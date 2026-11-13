package com.example.fishstock.search;

import com.example.fishstock.engine.Move;

import java.util.Collections;
import java.util.List;

/** Outcome of a search. Scores are in pawns from the side to move's point of view. */
public final class SearchResult {
  public final Move bestMove;
  public final double score;
  public final int depth;
  public final long nodes;
  public final long timeMs;
  public final List<Move> principalVariation;

  public SearchResult(Move bestMove, double score, int depth, long nodes, long timeMs, List<Move> pv) {
    this.bestMove = bestMove;
    this.score = score;
    this.depth = depth;
    this.nodes = nodes;
    this.timeMs = timeMs;
    this.principalVariation = pv == null ? Collections.<Move>emptyList() : pv;
  }

  public boolean isMateScore() {
    return Math.abs(score) > AlphaBetaSearch.MATE_THRESHOLD;
  }

  /** Moves until mate (positive: side to move mates), or 0 if not a mate score. */
  public int mateIn() {
    if (!isMateScore()) return 0;
    int plies = (int) Math.round(AlphaBetaSearch.MATE - Math.abs(score));
    int moves = (plies + 1) / 2;
    return score > 0 ? moves : -moves;
  }
}
