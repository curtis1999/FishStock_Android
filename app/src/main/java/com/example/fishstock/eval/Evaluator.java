package com.example.fishstock.eval;

import com.example.fishstock.engine.Position;

/**
 * An evaluation function: looks at a position (without searching) and says who is better.
 *
 * Every implementation must expose the weight it gives each {@link Term}, so any evaluator
 * can be copied, tweaked with sliders, saved, or used as a starting point for training.
 */
public interface Evaluator {

  /** Score in pawns from White's point of view (positive = White is better). */
  double evaluate(Position pos);

  /** The relative value this evaluator puts on each aspect of the position. */
  EvalWeights weights();

  /** Score split into the contribution of each term, for debugging and the analysis screen. */
  EvalBreakdown explain(Position pos);

  /** Score from the point of view of the given side. */
  default double evaluateFor(Position pos, boolean white) {
    double s = evaluate(pos);
    return white ? s : -s;
  }
}
