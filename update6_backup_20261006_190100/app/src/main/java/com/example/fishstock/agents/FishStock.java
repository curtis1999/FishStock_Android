package com.example.fishstock.agents;

import com.example.fishstock.endgame.EndgameOracle;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.search.SearchLimits;

/**
 * Level 3: the same search as MinMax, driven by trained weights.
 *
 * The plan is for {@link #TRAINED_WEIGHTS} to be the output of a training run that plays
 * many Arena matches while tweaking each {@link com.example.fishstock.eval.Term}. Until that
 * run exists, it holds the hand-tuned defaults and FishStock simply thinks longer than MinMax.
 * To install trained weights, paste the serialized string here.
 */
public final class FishStock extends MinMax {
  public static final long THINK_MS = 7000;

  /** Paste the result of a training run here (EvalWeights.serialize() format). */
  public static final String TRAINED_WEIGHTS = "";

  public FishStock(EndgameOracle oracle) {
    this(oracle, SearchLimits.forTime(THINK_MS));
  }

  public FishStock(EndgameOracle oracle, SearchLimits limits) {
    super("FishStock", weights(), limits, oracle);
  }

  public static EvalWeights weights() {
    return TRAINED_WEIGHTS.isEmpty() ? EvalWeights.defaults() : EvalWeights.parse(TRAINED_WEIGHTS);
  }
}
