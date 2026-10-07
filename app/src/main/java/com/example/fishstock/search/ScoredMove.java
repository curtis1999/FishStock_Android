package com.example.fishstock.search;

import com.example.fishstock.engine.Move;

/** A root move with its score (pawns, from the point of view of the side playing it). */
public final class ScoredMove {
  public final Move move;
  public final double score;
  /** Search depth the score comes from. */
  public final int depth;

  public ScoredMove(Move move, double score, int depth) {
    this.move = move;
    this.score = score;
    this.depth = depth;
  }

  public boolean isMate() {
    return Math.abs(score) > AlphaBetaSearch.MATE_THRESHOLD;
  }

  /** Moves until mate (positive: the mover mates), or 0. */
  public int mateIn() {
    if (!isMate()) return 0;
    int plies = (int) Math.round(AlphaBetaSearch.MATE - Math.abs(score));
    int moves = (plies + 1) / 2;
    return score > 0 ? moves : -moves;
  }

  @Override
  public String toString() {
    return move.toUci() + " " + String.format(java.util.Locale.US, "%+.2f", score);
  }
}
