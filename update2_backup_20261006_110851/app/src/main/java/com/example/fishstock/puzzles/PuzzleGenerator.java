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

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Finds positions with exactly one winning move.
 *
 * How it works:
 *  1. Sample positions: play a game from a random opening where both sides pick moves a bit
 *     carelessly (usually a decent move by the 1-ply evaluation, sometimes a worse one).
 *     Careless moves are what create tactics.
 *  2. Quick test on every position: a shallow search for the best move and, with that move
 *     excluded, the second-best move. Most positions fail here.
 *  3. Confirm with a deeper search. The position is a puzzle when
 *       - the best move wins: at least {@link #WIN} pawns, or a forced mate,
 *       - no other move wins: the second best is below {@link #NOT_WINNING} (and not a mate),
 *       - it isn't a giveaway: not simply recapturing on the square just captured on, and not
 *         just taking an undefended piece (unless it mates).
 *
 * Pure Java, so it can be tested (and tuned) on a desktop.
 */
public final class PuzzleGenerator {
  /** The best move must be worth at least this much (pawns, solver's view). */
  public static final double WIN = 1.5;
  /** Every other move must be worth less than this. */
  public static final double NOT_WINNING = 0.75;

  private static final int QUICK_DEPTH = 2;
  private static final int CONFIRM_DEPTH = 4;
  private static final int FIRST_PLY_TO_TEST = 10;
  private static final int MAX_GAME_PLIES = 90;
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

  /**
   * Keeps sampling positions until a puzzle turns up (or {@code stop} is set: returns null).
   * Typically takes a few seconds on a phone.
   */
  public Puzzle find(AtomicBoolean stop, Progress progress) {
    int checked = 0;
    while (stop == null || !stop.get()) {
      Game game = new Game();
      Openings.play(game, Openings.LINES[random.nextInt(Openings.LINES.length)]);
      while (!game.isOver() && game.plyCount() < MAX_GAME_PLIES) {
        if (stop != null && stop.get()) return null;
        if (game.plyCount() >= FIRST_PLY_TO_TEST) {
          checked++;
          if (progress != null) progress.positionsChecked(checked);
          Puzzle p = check(game.position(), game.positionHistory(), game.lastMove(), stop);
          if (p != null) return p;
        }
        Move m = sloppyMove(game.position());
        if (m == null) break;
        game.play(m);
      }
    }
    return null;
  }

  /** Is this position a puzzle? Returns it, or null. */
  public Puzzle check(Position position, List<Long> history, Move lastMove, AtomicBoolean stop) {
    Position pos = position.copy();
    // Quick test
    Candidate quick = bestTwo(pos, history, SearchLimits.forDepth(QUICK_DEPTH), stop);
    if (quick == null || !quick.looksLikePuzzle()) return null;
    if (!quick.best.isMateScore() && isGiveaway(pos, quick.best.bestMove, lastMove)) return null;

    // Confirm, deeper
    Candidate deep = bestTwo(pos, history, SearchLimits.forDepth(CONFIRM_DEPTH), stop);
    if (deep == null || !deep.looksLikePuzzle()) return null;
    if (!deep.best.isMateScore() && isGiveaway(pos, deep.best.bestMove, lastMove)) return null;
    if (stop != null && stop.get()) return null;

    SearchResult b = deep.best;
    return new Puzzle(pos.toFen(), b.bestMove, b.principalVariation, b.score, deep.second.score,
        b.isMateScore() ? b.mateIn() : 0);
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

    boolean looksLikePuzzle() {
      boolean bestWins = best.isMateScore() ? best.mateIn() > 0 : best.score >= WIN;
      boolean otherWins = second.isMateScore() ? second.mateIn() > 0 : second.score >= NOT_WINNING;
      return bestWins && !otherWins;
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
