package com.example.fishstock.agents;

import com.example.fishstock.endgame.EndgameOracle;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.Evaluator;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.SearchLimits;
import com.example.fishstock.search.SearchResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Level 2: Simple's evaluation function plus look-ahead. Runs alpha-beta MinMax
 * (see {@link AlphaBetaSearch}) as deep as it can in about {@link #DEFAULT_THINK_MS} ms,
 * exploring checks first, then captures.
 */
public class MinMax implements Agent {
  /** Target thinking time per move. Moves arrive in roughly 2-5 seconds on a phone. */
  public static final long DEFAULT_THINK_MS = 5000;

  private final String name;
  private final AlphaBetaSearch search;
  private final SearchLimits limits;
  private final EndgameOracle oracle;
  private volatile SearchResult lastResult;

  public MinMax() {
    this("MinMax", EvalWeights.defaults(), SearchLimits.forTime(DEFAULT_THINK_MS), EndgameOracle.NONE);
  }

  public MinMax(String name, EvalWeights weights, SearchLimits limits, EndgameOracle oracle) {
    this.name = name;
    this.search = new AlphaBetaSearch(new WeightedEvaluator(weights));
    this.limits = limits;
    this.oracle = oracle == null ? EndgameOracle.NONE : oracle;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Evaluator evaluator() {
    return search.evaluator();
  }

  /** Depth, score and principal variation of the last search (null before the first move). */
  public SearchResult lastResult() {
    return lastResult;
  }

  @Override
  public Move chooseMove(Position pos, List<Long> history, AtomicBoolean stop) {
    List<Move> moves = MoveGenerator.legalMoves(pos);
    if (moves.isEmpty()) return null;
    if (moves.size() == 1) return moves.get(0);
    if (oracle.covers(pos)) {
      Move perfect = oracle.bestMove(pos);
      if (perfect != null) return perfect;
    }
    SearchResult r = search.search(pos, history, limits, stop);
    lastResult = r;
    return r.bestMove != null ? r.bestMove : moves.get(0);
  }
}
