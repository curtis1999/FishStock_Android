package com.example.fishstock.agents;

import com.example.fishstock.endgame.EndgameOracle;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.Evaluator;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.MateFinder;
import com.example.fishstock.search.SearchLimits;
import com.example.fishstock.search.SearchResult;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Level 2: Simple's evaluation function plus look-ahead. Runs alpha-beta MinMax
 * (see {@link AlphaBetaSearch}) as deep as it can in about {@link #DEFAULT_THINK_MS} ms,
 * exploring checks first, then captures.
 *
 * With a {@link PlayStyle} (Agro, Timid, or a custom agent's Aggression slider) it also:
 * prefers or avoids captures and checks, weighs its own and the opponent's weaknesses
 * differently, and (Agro) hunts for forced mates before the normal search.
 */
public class MinMax implements Agent {
  /** Target thinking time per move. Moves arrive in roughly 2-5 seconds on a phone. */
  public static final long DEFAULT_THINK_MS = 5000;
  /** Node cap for the mate hunt, so fixed-depth (training) games stay repeatable. */
  private static final long MATE_SEARCH_MAX_NODES = 400_000;

  private final String name;
  private final WeightedEvaluator evaluator;
  private final AlphaBetaSearch search;
  private final SearchLimits limits;
  private final EndgameOracle oracle;
  private final PlayStyle style;
  private Boolean lastPerspective;
  private OpeningBook book;
  private final Random random = new Random();
  private volatile SearchResult lastResult;

  public MinMax() {
    this("MinMax", EvalWeights.defaults(), SearchLimits.forTime(DEFAULT_THINK_MS), EndgameOracle.NONE);
  }

  public MinMax(String name, EvalWeights weights, SearchLimits limits, EndgameOracle oracle) {
    this(name, weights, limits, oracle, PlayStyle.NEUTRAL);
  }

  public MinMax(String name, EvalWeights weights, SearchLimits limits, EndgameOracle oracle, PlayStyle style) {
    this.name = name;
    this.style = style == null ? PlayStyle.NEUTRAL : style;
    this.evaluator = new WeightedEvaluator(weights, this.style.bias);
    this.search = new AlphaBetaSearch(evaluator);
    this.search.setRootBonus(this.style.rootBonus());
    this.search.setQuiescenceChecks(this.style.quiescenceChecks);
    this.limits = limits;
    this.oracle = oracle == null ? EndgameOracle.NONE : oracle;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Evaluator evaluator() {
    return evaluator;
  }

  public PlayStyle style() {
    return style;
  }

  /** Depth, score and principal variation of the last search (null before the first move). */
  public SearchResult lastResult() {
    return lastResult;
  }

  /** Opening moves to play before thinking (null = none). See {@link OpeningBook}. */
  public void setOpeningBook(OpeningBook book) {
    this.book = book;
  }

  public OpeningBook openingBook() {
    return book;
  }

  @Override
  public Move chooseMove(Position pos, List<Long> history, AtomicBoolean stop) {
    List<Move> moves = MoveGenerator.legalMoves(pos);
    if (moves.isEmpty()) return null;
    if (moves.size() == 1) return moves.get(0);
    if (book != null) {
      Move fromBook = book.choose(pos, history, evaluator, random);
      if (fromBook != null) return fromBook;
    }
    if (oracle.covers(pos)) {
      Move perfect = oracle.bestMove(pos);
      if (perfect != null) return perfect;
    }

    // Our side is "own" for the side bias. The remembered positions were scored from the
    // other side's view if that changed (only happens when one agent analyses both sides).
    boolean us = pos.whiteToMove();
    evaluator.setPerspective(us);
    if (lastPerspective != null && lastPerspective != us && !style.isNeutral()) search.clear();
    lastPerspective = us;

    SearchLimits remaining = limits;
    if (style.mateSearchShare > 0) {
      long start = System.currentTimeMillis();
      boolean timed = limits.hardTimeMs < Long.MAX_VALUE / 8;
      long budget = timed ? (long) (limits.hardTimeMs * style.mateSearchShare) : 0;
      MateFinder.Mate mate = MateFinder.find(pos, style.mateSearchMoves, budget, MATE_SEARCH_MAX_NODES, stop);
      if (mate != null) return mate.firstMove;
      if (timed) {
        long used = System.currentTimeMillis() - start;
        remaining = new SearchLimits(limits.maxDepth, Math.max(1, limits.softTimeMs - used),
            Math.max(1, limits.hardTimeMs - used));
      }
    }

    SearchResult r = search.search(pos, history, remaining, stop);
    lastResult = r;
    return r.bestMove != null ? r.bestMove : moves.get(0);
  }
}
