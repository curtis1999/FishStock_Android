package com.example.fishstock.eval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** How much each {@link Term} contributed to an evaluation (pawns, White's point of view). */
public final class EvalBreakdown {
  private final double[] contributions;
  private final double total;

  EvalBreakdown(double[] contributions, double total) {
    this.contributions = contributions;
    this.total = total;
  }

  public double total() {
    return total;
  }

  public double contribution(Term t) {
    return contributions[t.ordinal()];
  }

  public double categoryTotal(Category c) {
    double s = 0;
    for (Term t : Term.values()) if (t.category == c) s += contributions[t.ordinal()];
    return s;
  }

  /** Non-zero terms, largest effect first. */
  public List<Term> significantTerms() {
    List<Term> out = new ArrayList<>();
    for (Term t : Term.values()) if (Math.abs(contributions[t.ordinal()]) > 1e-6) out.add(t);
    Collections.sort(out, (a, b) -> Double.compare(
        Math.abs(contributions[b.ordinal()]), Math.abs(contributions[a.ordinal()])));
    return out;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(String.format(Locale.US, "Total %+.2f%n", total));
    for (Term t : significantTerms()) {
      sb.append(String.format(Locale.US, "  %-32s %+.2f%n", t.name(), contributions[t.ordinal()]));
    }
    return sb.toString();
  }
}
