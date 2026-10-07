package com.example.fishstock.eval;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Lets an evaluator care about the two sides differently ("my king" vs "your king").
 *
 * The plain evaluation is symmetric: an open file next to White's king costs White exactly what
 * the same file next to Black's king costs Black. A personality breaks that symmetry on purpose:
 *  - Agro scales the ENEMY's king-safety terms up (it loves attacking the king) and its OWN down
 *    (it doesn't worry much about its own),
 *  - Timid scales its OWN king safety, pawn structure and piece safety up.
 *
 * Each {@link Category} has an "own" and an "enemy" multiplier (1.0 = unchanged). Material is
 * always 1.0, so a pawn is a pawn for both sides. Which side is "own" is set on the evaluator
 * with {@link WeightedEvaluator#setPerspective} before each search.
 */
public final class SideBias implements Serializable {
  private final double[] own;
  private final double[] enemy;

  private SideBias(double[] own, double[] enemy) {
    this.own = own;
    this.enemy = enemy;
    own[Category.MATERIAL.ordinal()] = 1.0;
    enemy[Category.MATERIAL.ordinal()] = 1.0;
  }

  public static SideBias neutral() {
    double[] a = new double[Category.values().length];
    double[] b = new double[Category.values().length];
    Arrays.fill(a, 1.0);
    Arrays.fill(b, 1.0);
    return new SideBias(a, b);
  }

  /** Copy with the multiplier for our own side's terms in category c changed. */
  public SideBias withOwn(Category c, double factor) {
    double[] a = own.clone();
    a[c.ordinal()] = Math.max(0, factor);
    return new SideBias(a, enemy.clone());
  }

  /** Copy with the multiplier for the opponent's terms in category c changed. */
  public SideBias withEnemy(Category c, double factor) {
    double[] b = enemy.clone();
    b[c.ordinal()] = Math.max(0, factor);
    return new SideBias(own.clone(), b);
  }

  public double own(Category c) {
    return own[c.ordinal()];
  }

  public double enemy(Category c) {
    return enemy[c.ordinal()];
  }

  public boolean isNeutral() {
    for (int i = 0; i < own.length; i++) if (own[i] != 1.0 || enemy[i] != 1.0) return false;
    return true;
  }
}
