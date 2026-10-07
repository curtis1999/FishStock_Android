package com.example.fishstock.eval;

import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * One value per {@link Term}. This object is the whole "personality" of an agent:
 * Simple, MinMax, FishStock and custom agents differ only in their weights (and search).
 *
 * Built for experimentation:
 *  - {@link #scaledBy} applies the per-category sliders,
 *  - {@link #serialize}/{@link #parse} store agents as plain text,
 *  - {@link #toVector}/{@link #fromVector} expose the weights as numbers for a training loop.
 */
public final class EvalWeights implements Serializable {
  private final double[] values;
  private transient Boolean usesV2;

  private EvalWeights(double[] values) {
    this.values = values;
  }

  /** The improved evaluation with its hand-set starting values (MinMax+, Agro, Timid, custom agents). */
  public static EvalWeights defaults() {
    Term[] terms = Term.values();
    double[] v = new double[terms.length];
    for (Term t : terms) v[t.ordinal()] = t.defaultValue;
    return new EvalWeights(v);
  }

  /** The original evaluation: every version-2 term switched off (Lazy, MinMax). */
  public static EvalWeights classic() {
    double[] v = defaults().values;
    for (Term t : Term.values()) if (t.version >= 2) v[t.ordinal()] = 0;
    return new EvalWeights(v);
  }

  /**
   * Just material and not leaving pieces en prise (the Simple agent): enough not to blunder a
   * piece in one move, but no idea about pawns, kings, outposts or anything else.
   */
  public static EvalWeights materialAndSafety() {
    double[] v = defaults().values;
    for (Term t : Term.values()) {
      boolean keep = t.category == Category.MATERIAL && t.version == 1
          || t == Term.EN_PRISE_FACTOR || t == Term.SECOND_THREAT_FACTOR;
      if (!keep) v[t.ordinal()] = t.kind == Term.Kind.MULTIPLY ? 1.0 : 0.0;
    }
    return new EvalWeights(v);
  }

  /** True when any version-2 term is switched on (the evaluator skips that work otherwise). */
  public boolean usesVersion2() {
    Boolean b = usesV2;
    if (b == null) {
      boolean any = false;
      for (Term t : Term.values()) if (t.version >= 2 && values[t.ordinal()] != 0) any = true;
      usesV2 = b = any;
    }
    return b;
  }

  public double get(Term t) {
    return values[t.ordinal()];
  }

  /** Returns a copy with one term changed (clamped to the term's range). */
  public EvalWeights with(Term t, double value) {
    double[] v = values.clone();
    v[t.ordinal()] = t.clamp(value);
    return new EvalWeights(v);
  }

  public EvalWeights copy() {
    return new EvalWeights(values.clone());
  }

  /**
   * Scales every term in each category. A factor of 1.0 leaves the category unchanged,
   * 0.0 switches it off and 2.0 doubles it. For MULTIPLY terms the distance from 1.0 is scaled
   * (so a 0.8 pin factor becomes 0.6 at 200% and 1.0 - i.e. ignored - at 0%).
   */
  public EvalWeights scaledBy(Map<Category, Double> factors) {
    double[] v = values.clone();
    for (Term t : Term.values()) {
      Double f = factors.get(t.category);
      if (f == null) continue;
      double x = v[t.ordinal()];
      if (t.kind == Term.Kind.MULTIPLY) {
        v[t.ordinal()] = Math.max(0.0, Math.min(1.0, 1.0 - (1.0 - x) * f));
      } else {
        v[t.ordinal()] = x * f;
      }
    }
    return new EvalWeights(v);
  }

  /** Convenience: all categories at 100%. */
  public static Map<Category, Double> neutralFactors() {
    Map<Category, Double> m = new EnumMap<>(Category.class);
    for (Category c : Category.values()) m.put(c, 1.0);
    return m;
  }

  // ---------------------------------------------------------------- training hooks

  /** Weights as a plain vector, in {@link Term} declaration order. */
  public double[] toVector() {
    return values.clone();
  }

  public static EvalWeights fromVector(double[] v) {
    if (v.length != Term.values().length) {
      throw new IllegalArgumentException("Expected " + Term.values().length + " weights, got " + v.length);
    }
    double[] c = v.clone();
    for (Term t : Term.values()) c[t.ordinal()] = t.clamp(c[t.ordinal()]);
    return new EvalWeights(c);
  }

  // ---------------------------------------------------------------- text form

  /** "PAWN_VALUE=1.0;KNIGHT_VALUE=3.05;..." */
  public String serialize() {
    StringBuilder sb = new StringBuilder();
    for (Term t : Term.values()) {
      if (sb.length() > 0) sb.append(';');
      sb.append(t.name()).append('=').append(String.format(Locale.US, "%.4f", values[t.ordinal()]));
    }
    return sb.toString();
  }

  /** Parses {@link #serialize} output. Unknown names are ignored, missing ones keep defaults. */
  public static EvalWeights parse(String text) {
    double[] v = defaults().values;
    if (text == null) return new EvalWeights(v);
    for (String part : text.split("[;\\n]")) {
      int eq = part.indexOf('=');
      if (eq < 0) continue;
      try {
        Term t = Term.valueOf(part.substring(0, eq).trim());
        v[t.ordinal()] = Double.parseDouble(part.substring(eq + 1).trim());
      } catch (IllegalArgumentException ignored) {
        // Term renamed or removed since this agent was saved: keep the default.
      }
    }
    return new EvalWeights(v);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof EvalWeights && Arrays.equals(values, ((EvalWeights) o).values);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(values);
  }

  @Override
  public String toString() {
    return serialize();
  }
}
