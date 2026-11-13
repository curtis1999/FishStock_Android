package com.example.fishstock.search;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Rules;
import com.example.fishstock.eval.Evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MinMax with alpha-beta pruning (written in the compact "negamax" form: each side maximises
 * its own score, and a child's score is negated for the parent).
 *
 * On top of plain alpha-beta:
 *  - Move ordering: checks first, then captures (most valuable victim, least valuable attacker),
 *    then quiet moves that caused cut-offs before. Good moves first = more pruning = deeper search.
 *  - Iterative deepening with a time limit, so the depth fits the device.
 *  - Quiescence search: at the horizon, keep looking at captures so the search never stops
 *    in the middle of an exchange.
 *  - Check extension: positions in check are searched one ply deeper.
 *  - Repetitions and the 50-move rule score as a draw (0), so a winning side avoids them.
 *  - A small transposition table remembers positions reached by different move orders.
 */
public final class AlphaBetaSearch {
  public static final double MATE = 1000.0;
  /** Scores beyond this are "mate in N" (evaluations never get near it). */
  public static final double MATE_THRESHOLD = MATE - 200;
  private static final double INF = 1e9;
  private static final int MAX_PLY = 96;
  private static final int MAX_QUIESCENCE_PLY = 10;

  private static final int ORDER_HASH_MOVE = 1_000_000_000;
  private static final int ORDER_CHECK = 100_000_000;
  private static final int ORDER_CAPTURE = 10_000_000;
  private static final int ORDER_KILLER = 1_000_000;

  private final Evaluator evaluator;
  private final TranspositionTable table = new TranspositionTable(1 << 16);
  private final Move[][] killers = new Move[MAX_PLY + 1][2];
  private final int[][] history = new int[64][64];

  // Per-search state
  private long[] pathHashes = new long[MAX_PLY + 1];
  private Set<Long> gameHistory = new HashSet<>();
  private long startTime;
  private long hardDeadline;
  private AtomicBoolean stopFlag;
  private boolean aborted;
  private long nodes;

  public AlphaBetaSearch(Evaluator evaluator) {
    this.evaluator = evaluator;
  }

  public Evaluator evaluator() {
    return evaluator;
  }

  /** Forget everything learned (call between unrelated games). */
  public void clear() {
    table.clear();
    for (Move[] k : killers) k[0] = k[1] = null;
    for (int[] h : history) java.util.Arrays.fill(h, 0);
  }

  public SearchResult search(Position root, List<Long> previousPositions, SearchLimits limits, AtomicBoolean stop) {
    return search(root, previousPositions, limits, stop, Collections.<Move>emptyList());
  }

  /**
   * @param root              position to search (left unchanged)
   * @param previousPositions hashes of positions that already occurred in the game (for repetitions)
   * @param excluded          root moves to ignore (used to find 2nd/3rd best lines for analysis)
   */
  public SearchResult search(Position root, List<Long> previousPositions, SearchLimits limits,
                             AtomicBoolean stop, List<Move> excluded) {
    Position pos = root.copy();
    startTime = System.currentTimeMillis();
    hardDeadline = startTime + limits.hardTimeMs;
    stopFlag = stop;
    aborted = false;
    nodes = 0;
    pathHashes[0] = pos.hash();
    gameHistory = new HashSet<>();
    // A position seen before in the game counts as a draw if reached again inside the search.
    if (previousPositions != null) {
      for (int i = 0; i < previousPositions.size() - 1; i++) gameHistory.add(previousPositions.get(i));
    }

    List<Move> rootMoves = new ArrayList<>();
    for (Move m : MoveGenerator.legalMoves(pos)) {
      boolean skip = false;
      for (Move e : excluded) if (e.sameAs(m)) skip = true;
      if (!skip) rootMoves.add(m);
    }
    if (rootMoves.isEmpty()) {
      double s = pos.inCheck() ? -MATE : 0;
      return new SearchResult(null, s, 0, 0, 0, null);
    }

    Move best = rootMoves.get(0);
    double bestScore = -INF;
    int completedDepth = 0;
    if (rootMoves.size() == 1 && excluded.isEmpty()) {
      return new SearchResult(best, evaluator.evaluateFor(pos, pos.whiteToMove()), 0, 0, 0,
          Collections.singletonList(best));
    }

    for (int depth = 1; depth <= limits.maxDepth; depth++) {
      orderMoves(pos, rootMoves, best, 0);
      double alpha = -INF;
      Move iterationBest = null;
      double iterationScore = -INF;
      for (Move m : rootMoves) {
        pos.makeMove(m);
        pathHashes[1] = pos.hash();
        double score = -negamax(pos, depth - 1, -INF, -alpha, 1);
        pos.unmakeMove(m);
        if (aborted) break;
        if (score > iterationScore) {
          iterationScore = score;
          iterationBest = m;
        }
        if (score > alpha) alpha = score;
      }
      if (iterationBest != null && (!aborted || iterationScore > bestScore || iterationBest.sameAs(best))) {
        // A partly finished depth is still usable once the previous best move has been re-checked.
        best = iterationBest;
        bestScore = iterationScore;
        if (!aborted) completedDepth = depth;
      }
      if (aborted) break;
      // Bring the best move to the front for the next iteration.
      rootMoves.remove(best);
      rootMoves.add(0, best);
      if (Math.abs(bestScore) > MATE_THRESHOLD) break; // forced mate found
      if (System.currentTimeMillis() - startTime > limits.softTimeMs) break;
    }

    List<Move> pv = extractPv(pos, best);
    return new SearchResult(best, bestScore, completedDepth, nodes, System.currentTimeMillis() - startTime, pv);
  }

  private double negamax(Position pos, int depth, double alpha, double beta, int ply) {
    if ((++nodes & 1023) == 0) checkTime();
    if (aborted) return 0;

    if (isDraw(pos, ply)) return 0;

    boolean inCheck = pos.inCheck();
    if (inCheck && ply < MAX_PLY / 2) depth++;
    if (depth <= 0 || ply >= MAX_PLY) return quiescence(pos, alpha, beta, ply, 0);

    double originalAlpha = alpha;
    long key = pos.hash();
    TranspositionTable.Entry entry = table.probe(key);
    Move hashMove = null;
    if (entry != null) {
      hashMove = entry.move;
      if (entry.depth >= depth) {
        double s = fromTable(entry.score, ply);
        if (entry.flag == TranspositionTable.EXACT) return s;
        if (entry.flag == TranspositionTable.LOWER && s >= beta) return s;
        if (entry.flag == TranspositionTable.UPPER && s <= alpha) return s;
      }
    }

    List<Move> moves = MoveGenerator.legalMoves(pos);
    if (moves.isEmpty()) return inCheck ? -(MATE - ply) : 0;
    orderMoves(pos, moves, hashMove, ply);

    double best = -INF;
    Move bestMove = null;
    for (Move m : moves) {
      pos.makeMove(m);
      pathHashes[ply + 1] = pos.hash();
      double score = -negamax(pos, depth - 1, -beta, -alpha, ply + 1);
      pos.unmakeMove(m);
      if (aborted) return 0;
      if (score > best) {
        best = score;
        bestMove = m;
      }
      if (score > alpha) alpha = score;
      if (alpha >= beta) {
        if (!m.isCapture() && !m.isPromotion()) {
          if (!m.sameAs(killers[ply][0])) {
            killers[ply][1] = killers[ply][0];
            killers[ply][0] = m;
          }
          history[m.from][m.to] += depth * depth;
        }
        break;
      }
    }

    int flag = best <= originalAlpha ? TranspositionTable.UPPER
        : best >= beta ? TranspositionTable.LOWER : TranspositionTable.EXACT;
    table.store(key, depth, toTable(best, ply), flag, bestMove);
    return best;
  }

  /** Only captures and promotions (all moves when in check), until the position is quiet. */
  private double quiescence(Position pos, double alpha, double beta, int ply, int qply) {
    if ((++nodes & 1023) == 0) checkTime();
    if (aborted) return 0;

    boolean inCheck = pos.inCheck();
    double standPat = -INF;
    if (!inCheck) {
      standPat = evaluator.evaluateFor(pos, pos.whiteToMove());
      if (standPat >= beta || qply >= MAX_QUIESCENCE_PLY) return standPat;
      if (standPat > alpha) alpha = standPat;
    }

    List<Move> moves = inCheck ? MoveGenerator.legalMoves(pos) : MoveGenerator.legalTacticalMoves(pos);
    if (inCheck && moves.isEmpty()) return -(MATE - ply);
    if (inCheck && qply >= MAX_QUIESCENCE_PLY) return evaluator.evaluateFor(pos, pos.whiteToMove());
    orderCaptures(moves);

    double best = standPat;
    for (Move m : moves) {
      pos.makeMove(m);
      double score = -quiescence(pos, -beta, -alpha, ply + 1, qply + 1);
      pos.unmakeMove(m);
      if (aborted) return 0;
      if (score > best) best = score;
      if (score > alpha) alpha = score;
      if (alpha >= beta) break;
    }
    return best;
  }

  private boolean isDraw(Position pos, int ply) {
    if (pos.halfmoveClock() >= 100) return true;
    long h = pos.hash();
    // Repetition inside the search path (same side to move => step 2) or earlier in the game.
    for (int i = ply - 2; i >= 0 && i >= ply - pos.halfmoveClock(); i -= 2) {
      if (pathHashes[i] == h) return true;
    }
    if (gameHistory.contains(h)) return true;
    return pos.pieceCount() <= 4 && Rules.isInsufficientMaterial(pos);
  }

  private void checkTime() {
    if (System.currentTimeMillis() > hardDeadline || (stopFlag != null && stopFlag.get())) aborted = true;
  }

  // ---------------------------------------------------------------- move ordering

  /** Hash move, then checks, then captures (MVV-LVA), then killers, then history. */
  private void orderMoves(Position pos, List<Move> moves, Move hashMove, int ply) {
    final int n = moves.size();
    final int[] keys = new int[n];
    for (int i = 0; i < n; i++) {
      Move m = moves.get(i);
      int k;
      if (m.sameAs(hashMove)) {
        k = ORDER_HASH_MOVE;
      } else {
        k = history[m.from][m.to];
        if (k > ORDER_KILLER - 1) k = ORDER_KILLER - 1;
        if (m.sameAs(killers[ply][0]) || m.sameAs(killers[ply][1])) k = ORDER_KILLER;
        if (m.isCapture() || m.isPromotion()) k = ORDER_CAPTURE + mvvLva(m);
        if (MoveGenerator.givesCheck(pos, m)) k = ORDER_CHECK + (m.isCapture() ? mvvLva(m) : 0);
      }
      keys[i] = k;
    }
    sortByKeys(moves, keys);
  }

  private static void orderCaptures(List<Move> moves) {
    int[] keys = new int[moves.size()];
    for (int i = 0; i < keys.length; i++) keys[i] = mvvLva(moves.get(i));
    sortByKeys(moves, keys);
  }

  private static int mvvLva(Move m) {
    int victim = Piece.type(m.captured);
    int promo = m.promotion == Piece.QUEEN ? 8 : 0;
    return (victim + promo) * 10 - Piece.type(m.piece);
  }

  /** Insertion sort, descending by key (lists are short). */
  private static void sortByKeys(List<Move> moves, int[] keys) {
    for (int i = 1; i < keys.length; i++) {
      int k = keys[i];
      Move m = moves.get(i);
      int j = i - 1;
      while (j >= 0 && keys[j] < k) {
        keys[j + 1] = keys[j];
        moves.set(j + 1, moves.get(j));
        j--;
      }
      keys[j + 1] = k;
      moves.set(j + 1, m);
    }
  }

  // ---------------------------------------------------------------- helpers

  private List<Move> extractPv(Position root, Move first) {
    List<Move> pv = new ArrayList<>();
    if (first == null) return pv;
    Position p = root.copy();
    Set<Long> seen = new HashSet<>();
    Move m = first;
    while (m != null && pv.size() < 12) {
      Move legal = null;
      for (Move x : MoveGenerator.legalMoves(p)) if (x.sameAs(m)) legal = x;
      if (legal == null) break;
      pv.add(legal);
      p.makeMove(legal);
      if (!seen.add(p.hash())) break;
      TranspositionTable.Entry e = table.probe(p.hash());
      m = e == null ? null : e.move;
    }
    return pv;
  }

  /** Mate scores are stored relative to the node so they stay correct at other depths. */
  private static double toTable(double score, int ply) {
    if (score > MATE_THRESHOLD) return score + ply;
    if (score < -MATE_THRESHOLD) return score - ply;
    return score;
  }

  private static double fromTable(double score, int ply) {
    if (score > MATE_THRESHOLD) return score - ply;
    if (score < -MATE_THRESHOLD) return score + ply;
    return score;
  }

  public long nodes() {
    return nodes;
  }
}
