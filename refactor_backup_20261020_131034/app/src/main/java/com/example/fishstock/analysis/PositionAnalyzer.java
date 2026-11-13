package com.example.fishstock.analysis;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.Evaluator;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.ScoredMove;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Works out a {@link PositionProfile}: scores every legal move, then walks a few plies down the
 * best line counting the good moves at each step. Pure Java; the analysis screen runs it on a
 * background thread and shows the results as they arrive.
 */
public final class PositionAnalyzer {
  /** Depth used to score every move of the position itself. */
  public static final int MOVE_DEPTH = 3;
  /** Depth used at each step down the best line (cheaper). */
  public static final int STEP_DEPTH = 2;
  /** How many plies down the best line to look. */
  public static final int STEPS = 4;

  public interface Listener {
    /** Every move scored (called once quickly at depth 1, then again at full depth). */
    void onMovesScored(List<ScoredMove> moves, int depth);

    /** The finished profile. */
    void onProfile(PositionProfile profile);
  }

  private final Evaluator evaluator;

  public PositionAnalyzer(Evaluator evaluator) {
    this.evaluator = evaluator;
  }

  /**
   * @param history hashes of the positions so far, the current one last (for repetitions)
   * @param timeMs  rough time budget for scoring the moves (the line steps add a little more)
   * @return the profile, or null if stopped or there are no legal moves
   */
  public PositionProfile analyze(Position pos, List<Long> history, long timeMs, AtomicBoolean stop, Listener listener) {
    AlphaBetaSearch search = new AlphaBetaSearch(evaluator);
    List<ScoredMove> quick = search.scoreAllMoves(pos, history, 1, 0, stop);
    if (quick.isEmpty() || stopped(stop)) return null;
    if (listener != null) listener.onMovesScored(quick, 1);

    List<ScoredMove> moves = search.scoreAllMoves(pos, history, MOVE_DEPTH, timeMs, stop);
    if (stopped(stop)) return null;
    if (moves.isEmpty()) moves = quick;
    if (listener != null && moves != quick) listener.onMovesScored(moves, moves.get(0).depth);

    List<PositionProfile.Step> steps = new ArrayList<>();
    Position p = pos.copy();
    List<Long> hist = history == null ? new ArrayList<Long>() : new ArrayList<>(history);
    List<ScoredMove> here = moves;
    for (int i = 0; i < STEPS && !here.isEmpty(); i++) {
      ScoredMove bestHere = here.get(0);
      steps.add(new PositionProfile.Step(san(p, bestHere.move), p.whiteToMove(),
          PositionProfile.countGood(here), here.size(), PositionProfile.isOnlyWin(here)));
      p.makeMove(bestHere.move);
      hist.add(p.hash());
      if (!MoveGenerator.hasLegalMove(p)) break;
      here = new AlphaBetaSearch(evaluator).scoreAllMoves(p, hist, STEP_DEPTH, 0, stop);
      if (stopped(stop)) return null;
    }

    PositionProfile profile = PositionProfile.of(moves, steps);
    if (listener != null) listener.onProfile(profile);
    return profile;
  }

  /** "Nf3" for White, "...e5" for Black. */
  private static String san(Position p, Move m) {
    String s = Notation.toSan(p, m);
    return p.whiteToMove() ? s : "..." + s;
  }

  private static boolean stopped(AtomicBoolean stop) {
    return stop != null && stop.get();
  }
}
