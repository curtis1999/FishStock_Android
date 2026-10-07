package com.example.fishstock.puzzles;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Position;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A position with exactly one winning move, according to the evaluation function.
 * Scores are in pawns from the solver's point of view (the side to move).
 */
public final class Puzzle {
  public final String fen;
  /** The only winning move. */
  public final Move solution;
  /** The engine's expected continuation, starting with {@link #solution}. */
  public final List<Move> line;
  /** Score after the solution. */
  public final double bestScore;
  /** Score after the best of the other moves. */
  public final double secondScore;
  /** Mate in this many moves, or 0 if the solution wins material instead. */
  public final int mateIn;

  public Puzzle(String fen, Move solution, List<Move> line, double bestScore, double secondScore, int mateIn) {
    this.fen = fen;
    this.solution = solution;
    this.line = line == null ? Collections.singletonList(solution) : line;
    this.bestScore = bestScore;
    this.secondScore = secondScore;
    this.mateIn = mateIn;
  }

  public Position position() {
    return Position.fromFen(fen);
  }

  public boolean whiteToMove() {
    return position().whiteToMove();
  }

  /** "Find mate in 2" or "Win material". */
  public String goal() {
    if (mateIn > 0) return mateIn == 1 ? "Find mate in 1" : String.format(Locale.US, "Find mate in %d", mateIn);
    return "Find the winning move";
  }

  /** The solution in normal notation, e.g. "Nxf7+". */
  public String solutionSan() {
    return Notation.toSan(position(), solution);
  }

  /** e.g. "+3.20 vs +0.10 for the next best move" (or a mate). */
  public String scoreSummary() {
    String best = mateIn > 0 ? "mate in " + mateIn : String.format(Locale.US, "%+.2f", bestScore);
    return String.format(Locale.US, "%s, next best move %+.2f", best, secondScore);
  }

  @Override
  public String toString() {
    return fen + "  " + solution.toUci() + "  (" + scoreSummary() + ")";
  }
}
