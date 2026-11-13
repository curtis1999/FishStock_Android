package com.example.fishstock.nn;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monte Carlo Tree Search guided by the network, as in AlphaZero.
 *
 * Each simulation walks down the tree from the current position, at every step picking the move
 * with the best mix of "has scored well so far" (Q) and "the network likes it and it's been
 * tried little" (U, the PUCT formula). When it reaches a position it hasn't seen it asks the
 * network once: the value is the result of the simulation (no playing to the end), and the policy
 * becomes the new position's move priors. The value then flows back up, flipping sign each ply.
 *
 * After the simulations, the move tried most often is the choice, and the visit counts are the
 * "better policy" the network is trained towards.
 *
 * Game rules count exactly: checkmate is -1 for the side to move, stalemate, the 50-move rule,
 * insufficient material and a repeated position are 0. Pawns always promote to a queen.
 */
public final class Mcts {
  private final Network net;
  private final Random rng;
  /** Exploration constant: higher tries more moves, lower trusts the best ones sooner. */
  public float cPuct = 1.5f;
  /** Unvisited moves are assumed this much worse than the position's own value. */
  public float fpuReduction = 0.2f;
  /** Self-play: add Dirichlet noise at the root so it keeps trying new moves. */
  public boolean rootNoise;
  public double noiseAlpha = 0.3;
  public double noiseFraction = 0.25;

  public Mcts(Network net, Random rng) {
    this.net = net;
    this.rng = rng;
  }

  static final class Node {
    Move[] moves;
    int[] index;
    float[] prior;
    int[] visits;
    float[] valueSum;   // from this node's side-to-move point of view
    Node[] child;
    int total;
    float netValue;
    boolean terminal;
    float terminalValue;
  }

  /** What a search found. */
  public static final class Result {
    public final List<Move> moves;
    public final int[] visits;
    /** The network's move probabilities at the root (before noise), for display or analysis. */
    public final float[] prior;
    public final int bestIndex;
    /** Average result of the best move, -1..+1 for the side to move. */
    public final float value;
    public final int simulations;
    final int[] index;
    final boolean whiteToMove;

    Result(List<Move> moves, int[] visits, float[] prior, int[] index, int best, float value, int sims, boolean white) {
      this.moves = moves;
      this.visits = visits;
      this.prior = prior;
      this.index = index;
      this.bestIndex = best;
      this.value = value;
      this.simulations = sims;
      this.whiteToMove = white;
    }

    public Move best() {
      return bestIndex < 0 ? null : moves.get(bestIndex);
    }

    /** Visit counts as probabilities: the training target for the policy. */
    public float[] visitDistribution() {
      float[] d = new float[visits.length];
      int sum = 0;
      for (int v : visits) sum += v;
      for (int i = 0; i < d.length; i++) d[i] = sum == 0 ? 1f / d.length : (float) visits[i] / sum;
      return d;
    }

    /**
     * A move chosen in proportion to visits^(1/temperature): 1 = as often as it was searched,
     * near 0 = always the most-searched move.
     */
    public Move sample(double temperature, Random rng) {
      if (temperature < 0.05 || moves.isEmpty()) return best();
      double[] w = new double[visits.length];
      double sum = 0;
      for (int i = 0; i < w.length; i++) {
        w[i] = Math.pow(visits[i], 1.0 / temperature);
        sum += w[i];
      }
      if (sum <= 0) return best();
      double r = rng.nextDouble() * sum;
      for (int i = 0; i < w.length; i++) {
        r -= w[i];
        if (r <= 0) return moves.get(i);
      }
      return best();
    }
  }

  /**
   * Searches until {@code maxSimulations} or {@code timeMs} (0 = no limit) is reached or stop is set.
   *
   * @param history hashes of the game's earlier positions (for repetitions), the current one last; may be null
   */
  public Result search(Position root, List<Long> history, int maxSimulations, long timeMs, AtomicBoolean stop) {
    Position pos = root.copy();
    Set<Long> seen = new HashSet<>();
    if (history != null) seen.addAll(history);
    seen.add(pos.hash());

    Node node = new Node();
    expand(node, pos);
    if (node.terminal || node.moves.length == 0) {
      return new Result(new ArrayList<Move>(), new int[0], new float[0], new int[0], -1, node.terminalValue, 0,
          root.whiteToMove());
    }
    float[] cleanPrior = node.prior.clone();
    if (rootNoise) addNoise(node);

    long deadline = timeMs > 0 ? System.currentTimeMillis() + timeMs : Long.MAX_VALUE;
    int sims = 0;
    while (sims < maxSimulations) {
      simulate(node, pos, seen);
      sims++;
      if ((sims & 15) == 0) {
        if (stop != null && stop.get()) break;
        if (System.currentTimeMillis() >= deadline) break;
      }
      // Only one move: nothing to think about.
      if (node.moves.length == 1 && sims >= 2) break;
    }

    int best = 0;
    for (int i = 1; i < node.moves.length; i++) {
      if (node.visits[i] > node.visits[best]
          || (node.visits[i] == node.visits[best] && q(node, i) > q(node, best))) best = i;
    }
    List<Move> moves = new ArrayList<>();
    for (Move m : node.moves) moves.add(m);
    return new Result(moves, node.visits.clone(), cleanPrior, node.index, best,
        node.visits[best] > 0 ? q(node, best) : node.netValue, sims, root.whiteToMove());
  }

  private static float q(Node n, int i) {
    return n.visits[i] == 0 ? -2 : n.valueSum[i] / n.visits[i];
  }

  /** One simulation; returns the result from the point of view of the side to move at {@code node}. */
  private float simulate(Node node, Position pos, Set<Long> seen) {
    if (node.terminal) return node.terminalValue;

    // Pick the move with the best Q + U.
    float sqrtTotal = (float) Math.sqrt(node.total + 1);
    float fpu = node.netValue - fpuReduction;
    int pick = 0;
    float bestScore = Float.NEGATIVE_INFINITY;
    for (int i = 0; i < node.moves.length; i++) {
      float qv = node.visits[i] == 0 ? fpu : node.valueSum[i] / node.visits[i];
      float u = cPuct * node.prior[i] * sqrtTotal / (1 + node.visits[i]);
      float s = qv + u;
      if (s > bestScore) {
        bestScore = s;
        pick = i;
      }
    }

    Move m = node.moves[pick];
    pos.makeMove(m);
    long h = pos.hash();
    float value;
    if (node.child == null) node.child = new Node[node.moves.length];
    Node child = node.child[pick];
    if (child == null) {
      child = new Node();
      node.child[pick] = child;
      if (seen.contains(h)) {
        // Back to a position seen before (in the game or on this line): a draw.
        child.terminal = true;
        child.terminalValue = 0;
        value = 0;
      } else {
        expand(child, pos);
        value = child.terminal ? child.terminalValue : child.netValue;
      }
    } else {
      seen.add(h);
      value = simulate(child, pos, seen);
      seen.remove(h);
    }
    pos.unmakeMove(m);

    float mine = -value;
    node.visits[pick]++;
    node.valueSum[pick] += mine;
    node.total++;
    return mine;
  }

  /** First visit to a position: game over, or ask the network. */
  private void expand(Node node, Position pos) {
    List<Move> legal = searchMoves(pos);
    if (legal.isEmpty()) {
      node.terminal = true;
      node.terminalValue = pos.inCheck() ? -1 : 0;
      node.moves = new Move[0];
      return;
    }
    if (Rules.isFiftyMoveDraw(pos) || Rules.isInsufficientMaterial(pos)) {
      node.terminal = true;
      node.terminalValue = 0;
      node.moves = new Move[0];
      return;
    }
    int n = legal.size();
    node.moves = legal.toArray(new Move[0]);
    node.index = new int[n];
    boolean white = pos.whiteToMove();
    for (int i = 0; i < n; i++) node.index[i] = Encoder.moveIndex(node.moves[i], white);
    Network.Output out = net.forward(Encoder.features(pos), node.index);
    node.prior = Network.softmax(out.logits);
    node.netValue = out.value;
    node.visits = new int[n];
    node.valueSum = new float[n];
  }

  /** Legal moves, with promotions to a queen only. */
  public static List<Move> searchMoves(Position pos) {
    List<Move> legal = MoveGenerator.legalMoves(pos);
    List<Move> out = new ArrayList<>(legal.size());
    for (Move m : legal) {
      if (m.isPromotion() && m.promotion != Piece.QUEEN) continue;
      out.add(m);
    }
    return out;
  }

  private void addNoise(Node node) {
    int n = node.prior.length;
    double[] g = new double[n];
    double sum = 0;
    for (int i = 0; i < n; i++) {
      g[i] = gamma(noiseAlpha);
      sum += g[i];
    }
    if (sum <= 0) return;
    for (int i = 0; i < n; i++) {
      node.prior[i] = (float) ((1 - noiseFraction) * node.prior[i] + noiseFraction * g[i] / sum);
    }
  }

  /** Gamma(alpha, 1) sample (Marsaglia-Tsang, with the alpha < 1 boost). */
  private double gamma(double alpha) {
    if (alpha < 1) return gamma(alpha + 1) * Math.pow(rng.nextDouble(), 1 / alpha);
    double d = alpha - 1.0 / 3, c = 1 / Math.sqrt(9 * d);
    while (true) {
      double x = rng.nextGaussian();
      double v = 1 + c * x;
      if (v <= 0) continue;
      v = v * v * v;
      double u = rng.nextDouble();
      if (u < 1 - 0.0331 * x * x * x * x) return d * v;
      if (Math.log(u) < 0.5 * x * x + d * (1 - v + Math.log(v))) return d * v;
    }
  }
}
