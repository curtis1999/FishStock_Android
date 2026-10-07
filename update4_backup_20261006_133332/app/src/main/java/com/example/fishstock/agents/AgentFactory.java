package com.example.fishstock.agents;

import com.example.fishstock.endgame.EndgameOracle;
import com.example.fishstock.endgame.LichessTablebase;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.search.SearchLimits;

/** Builds playing agents from {@link AgentSpec}s. */
public final class AgentFactory {
  private AgentFactory() {}

  /**
   * @param spec          what to build
   * @param useTablebase  ask the online tablebase in solved endgames (off for tournaments)
   * @param thinkTimeMs   override for searching agents' time per move, or 0 for their default
   */
  public static Agent create(AgentSpec spec, boolean useTablebase, long thinkTimeMs) {
    Agent agent = build(spec, useTablebase, thinkTimeMs);
    // Every thinking agent opens from the book that matches its temperament.
    OpeningBook book = OpeningBook.forAggression(spec.aggression);
    if (agent instanceof MinMax) ((MinMax) agent).setOpeningBook(book);
    if (agent instanceof Simple) ((Simple) agent).setOpeningBook(book);
    return agent;
  }

  private static Agent build(AgentSpec spec, boolean useTablebase, long thinkTimeMs) {
    EndgameOracle oracle = useTablebase ? new LichessTablebase() : EndgameOracle.NONE;
    switch (spec.kind) {
      case BLUNDER:
        return new Blunder();
      case RANDY:
        return new Randy();
      case SIMPLE:
        return new Simple("Simple", EvalWeights.defaults(), oracle);
      case MINMAX:
        return new MinMax("MinMax", EvalWeights.defaults(),
            SearchLimits.forTime(thinkTimeMs > 0 ? thinkTimeMs : MinMax.DEFAULT_THINK_MS), oracle);
      case AGRO:
      case TIMID:
        return new MinMax(spec.name, spec.weights(),
            SearchLimits.forTime(thinkTimeMs > 0 ? thinkTimeMs : MinMax.DEFAULT_THINK_MS), oracle, spec.style());
      case FISHSTOCK:
        return new FishStock(oracle, SearchLimits.forTime(thinkTimeMs > 0 ? thinkTimeMs : FishStock.THINK_MS));
      case CUSTOM:
        long t = thinkTimeMs > 0 && spec.thinkTimeMs > 0 ? thinkTimeMs : spec.thinkTimeMs;
        if (t <= 0) return new Simple(spec.name, spec.weights(), oracle, spec.style());
        return new MinMax(spec.name, spec.weights(), SearchLimits.forTime(t), oracle, spec.style());
      case HUMAN:
      default:
        return new Human();
    }
  }

  public static Agent create(AgentSpec spec, boolean useTablebase) {
    return create(spec, useTablebase, 0);
  }
}
