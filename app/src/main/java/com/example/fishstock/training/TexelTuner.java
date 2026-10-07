package com.example.fishstock.training;

import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.Term;
import com.example.fishstock.eval.WeightedEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Texel tuning: find the weights whose evaluation best predicts game results.
 *
 * For every labelled position the evaluation e (pawns, White's view) is turned into a predicted
 * score 1 / (1 + 10^(-K e / 4)), and the error is the mean squared difference from the real result
 * (1, 0.5 or 0). K is fitted once so the scale matches; then each dial is nudged up and down in
 * turn, keeping any change that lowers the error, with smaller steps once nothing helps
 * (coordinate descent). PAWN_VALUE stays at 1.0 so a pawn is still a pawn.
 *
 * This learns from thousands of games' worth of positions at once, which is far cheaper than
 * judging each candidate set of weights by playing matches.
 */
public final class TexelTuner {
  public interface Log {
    void line(String s);
  }

  private final Position[] positions;
  private final double[] results;
  private final int threads;
  private final ExecutorService pool;
  private double k = 1.0;

  public TexelTuner(List<SelfPlay.Sample> samples, int threads) {
    positions = new Position[samples.size()];
    results = new double[samples.size()];
    for (int i = 0; i < samples.size(); i++) {
      positions[i] = Position.fromFen(samples.get(i).fen);
      results[i] = samples.get(i).result;
    }
    this.threads = Math.max(1, threads);
    pool = Executors.newFixedThreadPool(this.threads);
  }

  public void shutdown() {
    pool.shutdownNow();
  }

  public int size() {
    return positions.length;
  }

  /** Mean squared error of the predicted results for these weights. */
  public double error(EvalWeights w) throws Exception {
    final int n = positions.length;
    List<Future<Double>> parts = new ArrayList<>();
    for (int t = 0; t < threads; t++) {
      final int from = n * t / threads;
      final int to = n * (t + 1) / threads;
      parts.add(pool.submit(() -> {
        WeightedEvaluator ev = new WeightedEvaluator(w);
        double sum = 0;
        for (int i = from; i < to; i++) {
          double e = Math.max(-20, Math.min(20, ev.evaluate(positions[i])));
          double predicted = 1.0 / (1.0 + Math.pow(10, -k * e / 4.0));
          double d = results[i] - predicted;
          sum += d * d;
        }
        return sum;
      }));
    }
    double total = 0;
    for (Future<Double> f : parts) total += f.get();
    return total / n;
  }

  /** Finds the K that makes the starting weights' predictions fit best. */
  public double fitK(EvalWeights w, Log log) throws Exception {
    double best = error(w);
    double bestK = k;
    for (double step = 0.5; step > 0.01; step /= 2) {
      boolean improved = true;
      while (improved) {
        improved = false;
        for (double dir : new double[] {+1, -1}) {
          double old = k;
          k = Math.max(0.05, bestK + dir * step);
          double e = error(w);
          if (e < best) {
            best = e;
            bestK = k;
            improved = true;
          } else {
            k = old;
          }
        }
      }
    }
    k = bestK;
    if (log != null) log.line(String.format(java.util.Locale.US, "K = %.3f, error %.6f", k, best));
    return k;
  }

  /**
   * Coordinate descent from {@code start}. Stops after {@code maxPasses} passes or when every step
   * has shrunk below 1% of its term's range, or at {@code deadlineMs} (System.currentTimeMillis()).
   */
  public EvalWeights tune(EvalWeights start, int maxPasses, long deadlineMs, Log log) throws Exception {
    double[] v = start.toVector();
    Term[] terms = Term.values();
    double[] step = new double[terms.length];
    for (Term t : terms) step[t.ordinal()] = (t.max - t.min) * 0.08;
    double best = error(EvalWeights.fromVector(v));
    if (log != null) log.line(String.format(java.util.Locale.US, "start error %.6f on %d positions", best, size()));
    for (int pass = 1; pass <= maxPasses && System.currentTimeMillis() < deadlineMs; pass++) {
      int changed = 0;
      for (Term t : terms) {
        if (t == Term.PAWN_VALUE || System.currentTimeMillis() > deadlineMs) continue;
        int i = t.ordinal();
        if (step[i] < (t.max - t.min) * 0.01) continue;
        boolean moved = false;
        for (double dir : new double[] {+1, -1}) {
          double old = v[i];
          v[i] = t.clamp(old + dir * step[i]);
          if (v[i] == old) continue;
          double e = error(EvalWeights.fromVector(v));
          if (e < best) {
            best = e;
            moved = true;
            changed++;
            break;
          }
          v[i] = old;
        }
        if (!moved) step[i] /= 2;
      }
      if (log != null) log.line(String.format(java.util.Locale.US, "pass %d: error %.6f, %d dials moved", pass, best, changed));
      if (changed == 0) {
        boolean anyLeft = false;
        for (Term t : terms) if (step[t.ordinal()] >= (t.max - t.min) * 0.01) anyLeft = true;
        if (!anyLeft) break;
      }
    }
    return EvalWeights.fromVector(v);
  }
}
