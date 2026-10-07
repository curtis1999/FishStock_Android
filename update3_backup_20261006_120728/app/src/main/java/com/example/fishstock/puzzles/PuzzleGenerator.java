package com.example.fishstock.puzzles;

import com.example.fishstock.arena.Openings;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.Evaluator;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.SearchLimits;
import com.example.fishstock.search.SearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Finds puzzles: positions where one move wins and nothing else does, followed (where possible)
 * by more "only moves" for the same side, so most puzzles are 2-4 move combinations.
 *
 * How it works:
 *  1. Sample positions. Middlegames come from games played from a random opening where both
 *     sides pick moves a bit carelessly (careless moves create tactics). Endgames come from
 *     {@link EndgameSampler} (made-up pawn endings, including pawn-breakthrough setups).
 *  2. Quick test on every position: a shallow search for the best move and, with that move
 *     excluded, the second-best move. Most positions fail here.
 *  3. Confirm with a deeper search. The first move must
 *       - win: at least {@link #WIN} pawns, or a forced mate,
 *       - be the only winning move: the second best is below {@link #NOT_WINNING} (and not a mate),
 *       - not be a giveaway: not simply recapturing on the square just captured on, and not just
 *         taking an undefended piece (unless it mates).
 *  4. Extend the line: play the opponent's best reply, and if the solver again has exactly one
 *     move that keeps the win (the next best is at least {@link #UNIQUE_GAP} pawns worse, or the
 *     only mate), add it. Repeat up to {@link #MAX_SOLVER_MOVES} moves.
 *  5. Name the ideas in the line with {@link ThemeDetector}.
 *
 * One-move puzzles are kept only {@link #SINGLE_MOVE_SHARE} of the time, unless nothing longer
 * turns up within {@link #FALLBACK_WAIT_MS}.
 *
 * Pure Java, so it can be tested (and tuned) on a desktop.
 */
public final class PuzzleGenerator {
  /** The best move must be worth at least this much (pawns, solver's view). */
  public static final double WIN = 1.5;
  /** Every other first move must be worth less than this. */
  public static final double NOT_WINNING = 0.75;
  /** Later moves in the line: every alternative must be at least this much worse. */
  public static final double UNIQUE_GAP = 1.2;
  /** Longest line, counted in solver moves. */
  public static final int MAX_SOLVER_MOVES = 4;
  /** Share of one-move puzzles that are accepted straight away. */
  public static final double SINGLE_MOVE_SHARE = 0.1;
  /** Accept a one-move puzzle anyway if nothing longer is found in this time. */
  public static final long FALLBACK_WAIT_MS = 15_000;

  /** What kind of positions to look in. */
  public enum Mode {
    /** Mostly middlegame tactics from sample games, with some endgames mixed in. */
    MIXED,
    /** Endgames only (pawn breakthroughs, pawn races, rook and minor-piece endings). */
    ENDGAME
  }

  private static final int QUICK_DEPTH = 2;
  private static final int CONFIRM_DEPTH = 4;
  /** Endgames have few moves, so they can be searched deeper (pawn races need it). */
  private static final int ENDGAME_QUICK_DEPTH = 4;
  private static final int ENDGAME_CONFIRM_DEPTH = 6;
  private static final int ENDGAME_PIECES = 12;
  /** Very few pieces (kings and pawns, typically): deeper still, for pawn races. */
  private static final int BARE_ENDGAME_PIECES = 8;
  private static final int FIRST_PLY_TO_TEST = 10;
  private static final int MAX_GAME_PLIES = 90;
  private static final int ENDGAME_PLIES_PER_SAMPLE = 8;
  private static final double ENDGAME_SHARE_IN_MIXED = 0.15;
  private static final int CONTINUATION_PLIES = 3;
  /** Sampling noise in pawns: higher = sloppier play = more tactics, but stranger positions. */
  private static final double TEMPERATURE = 0.35;

  public interface Progress {
    void positionsChecked(int count);
  }

  private final Evaluator evaluator;
  private final Random random;

  public PuzzleGenerator(EvalWeights weights, Random random) {
    this.evaluator = new WeightedEvaluator(weights);
    this.random = random;
  }

  public Puzzle find(AtomicBoolean stop, Progress progress) {
    return find(stop, progress, Mode.MIXED);
  }

  /**
   * Keeps sampling positions until a puzzle turns up (or {@code stop} is set: returns null).
   * Typically takes a few seconds to a few tens of seconds on a phone.
   */
  public Puzzle find(AtomicBoolean stop, Progress progress, Mode mode) {
    int[] checked = {0};
    Puzzle[] fallback = {null};
    long[] fallbackSince = {0};
    while (stop == null || !stop.get()) {
      boolean endgame = mode == Mode.ENDGAME || random.nextDouble() < ENDGAME_SHARE_IN_MIXED;
      Game game;
      int firstPly;
      int lastPly;
      if (endgame) {
        game = new Game(EndgameSampler.next(random).toFen());
        firstPly = 0;
        lastPly = ENDGAME_PLIES_PER_SAMPLE;
      } else {
        game = new Game();
        Openings.play(game, Openings.LINES[random.nextInt(Openings.LINES.length)]);
        firstPly = FIRST_PLY_TO_TEST;
        lastPly = MAX_GAME_PLIES;
      }
      while (!game.isOver() && game.plyCount() < lastPly) {
        if (stop != null && stop.get()) return null;
        if (game.plyCount() >= firstPly) {
          checked[0]++;
          if (progress != null) progress.positionsChecked(checked[0]);
          Puzzle p = check(game.position(), game.positionHistory(), game.lastMove(), stop);
          if (p != null) {
            if (p.movesToFind() > 1 || random.nextDouble() < SINGLE_MOVE_SHARE) return p;
            if (fallback[0] == null) {
              fallback[0] = p;
              fallbackSince[0] = System.currentTimeMillis();
            }
          }
          if (fallback[0] != null && System.currentTimeMillis() - fallbackSince[0] > FALLBACK_WAIT_MS) {
            return fallback[0];
          }
        }
        Move m = sloppyMove(game.position());
        if (m == null) break;
        game.play(m);
      }
    }
    return null;
  }

  /** Is this position a puzzle? Returns it (with as long a line as holds up), or null. */
  public Puzzle check(Position position, List<Long> history, Move lastMove, AtomicBoolean stop) {
    Position pos = position.copy();
    int pieces = pos.pieceCount();
    int extra = pieces <= BARE_ENDGAME_PIECES ? 1 : 0;
    boolean endgame = pieces <= ENDGAME_PIECES;
    int quickDepth = (endgame ? ENDGAME_QUICK_DEPTH : QUICK_DEPTH) + extra;
    int confirmDepth = (endgame ? ENDGAME_CONFIRM_DEPTH : CONFIRM_DEPTH) + extra;

    // Quick test
    Candidate quick = bestTwo(pos, history, SearchLimits.forDepth(quickDepth), stop);
    if (quick == null || !quick.looksLikePuzzle()) return null;
    if (!quick.best.isMateScore() && isGiveaway(pos, quick.best.bestMove, lastMove)) return null;

    // Confirm, deeper
    Candidate deep = bestTwo(pos, history, SearchLimits.forDepth(confirmDepth), stop);
    if (deep == null || !deep.looksLikePuzzle()) return null;
    if (!deep.best.isMateScore() && isGiveaway(pos, deep.best.bestMove, lastMove)) return null;
    if (stop != null && stop.get()) return null;

    return buildLine(pos, history, deep, confirmDepth, stop);
  }

  /** Steps 4 and 5: extend the line while the solver keeps having exactly one good move. */
  private Puzzle buildLine(Position start, List<Long> history, Candidate first, int depth, AtomicBoolean stop) {
    List<Move> line = new ArrayList<>();
    line.add(first.best.bestMove);
    Position p = start.copy();
    p.makeMove(first.best.bestMove);
    List<Long> hist = history == null ? new ArrayList<Long>() : new ArrayList<>(history);
    hist.add(p.hash());

    while ((line.size() + 1) / 2 < MAX_SOLVER_MOVES && MoveGenerator.hasLegalMove(p)) {
      if (stop != null && stop.get()) return null;
      SearchResult reply = new AlphaBetaSearch(evaluator).search(p, hist, SearchLimits.forDepth(depth - 1), stop);
      if (reply.bestMove == null) break;
      Position q = p.copy();
      q.makeMove(reply.bestMove);
      if (!MoveGenerator.hasLegalMove(q)) break;
      List<Long> qHist = new ArrayList<>(hist);
      qHist.add(q.hash());
      Candidate next = bestTwo(q, qHist, SearchLimits.forDepth(depth), stop);
      if (next == null || !next.continuesLine()) break;
      line.add(reply.bestMove);
      line.add(next.best.bestMove);
      q.makeMove(next.best.bestMove);
      qHist.add(q.hash());
      p = q;
      hist = qHist;
    }
    if (stop != null && stop.get()) return null;

    List<Move> continuation = new ArrayList<>();
    if (MoveGenerator.hasLegalMove(p)) {
      SearchResult rest = new AlphaBetaSearch(evaluator).search(p, hist, SearchLimits.forDepth(3), stop);
      List<Move> pv = rest.principalVariation;
      for (int i = 0; i < pv.size() && i < CONTINUATION_PLIES; i++) continuation.add(pv.get(i));
    }

    SearchResult b = first.best;
    List<Theme> themes = ThemeDetector.detect(start, line, continuation);
    return new Puzzle(start.toFen(), line, continuation, b.score, first.second.score,
        b.isMateScore() ? b.mateIn() : 0, themes);
  }

  /** Best move and best alternative, each from the side to move's point of view. */
  private Candidate bestTwo(Position pos, List<Long> history, SearchLimits limits, AtomicBoolean stop) {
    AlphaBetaSearch search = new AlphaBetaSearch(evaluator);
    SearchResult best = search.search(pos, history, limits, stop);
    if (best.bestMove == null || (stop != null && stop.get())) return null;
    SearchResult second = search.search(pos, history, limits, stop, Collections.singletonList(best.bestMove));
    if (second.bestMove == null) return null; // only one legal move: not much of a puzzle
    return new Candidate(best, second);
  }

  private static final class Candidate {
    final SearchResult best;
    final SearchResult second;

    Candidate(SearchResult best, SearchResult second) {
      this.best = best;
      this.second = second;
    }

    boolean bestWins() {
      return best.isMateScore() ? best.mateIn() > 0 : best.score >= WIN;
    }

    /** First move: wins, and no other move wins. */
    boolean looksLikePuzzle() {
      boolean otherWins = second.isMateScore() ? second.mateIn() > 0 : second.score >= NOT_WINNING;
      return bestWins() && !otherWins;
    }

    /** Later moves: still winning, and clearly the only way to keep the win. */
    boolean continuesLine() {
      if (!bestWins()) return false;
      boolean secondMates = second.isMateScore() && second.mateIn() > 0;
      if (best.isMateScore()) return !secondMates || second.mateIn() > best.mateIn();
      return !secondMates && second.score <= best.score - UNIQUE_GAP;
    }
  }

  /** Recapturing, or picking up a piece nobody defends, is too easy to be a puzzle. */
  private static boolean isGiveaway(Position pos, Move best, Move lastMove) {
    if (!best.isCapture()) return false;
    if (lastMove != null && lastMove.isCapture() && lastMove.to == best.to) return true;
    boolean defenderWhite = !pos.whiteToMove();
    return !pos.isAttacked(best.to, defenderWhite);
  }

  /**
   * A careless but not random move: every move is scored one ply deep and one is drawn with
   * probability ~ exp(score / TEMPERATURE). Good moves are likely, small slips are common.
   */
  private Move sloppyMove(Position position) {
    Position pos = position.copy();
    List<Move> moves = MoveGenerator.legalMoves(pos);
    if (moves.isEmpty()) return null;
    boolean us = pos.whiteToMove();
    double[] scores = new double[moves.size()];
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < moves.size(); i++) {
      pos.makeMove(moves.get(i));
      scores[i] = evaluator.evaluateFor(pos, us);
      pos.unmakeMove(moves.get(i));
      max = Math.max(max, scores[i]);
    }
    double total = 0;
    for (int i = 0; i < scores.length; i++) {
      scores[i] = Math.exp((scores[i] - max) / TEMPERATURE);
      total += scores[i];
    }
    double r = random.nextDouble() * total;
    for (int i = 0; i < scores.length; i++) {
      r -= scores[i];
      if (r <= 0) return moves.get(i);
    }
    return moves.get(moves.size() - 1);
  }
}
