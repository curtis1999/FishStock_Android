package com.example.fishstock.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Running totals of a match between agent A and agent B. */
public final class MatchResult {
  public final String nameA;
  public final String nameB;
  private int winsA;
  private int winsB;
  private int draws;
  private final List<GameRecord> games = new ArrayList<>();

  public MatchResult(String nameA, String nameB) {
    this.nameA = nameA;
    this.nameB = nameB;
  }

  synchronized void add(GameRecord g, boolean aWasWhite) {
    games.add(g);
    int s = aWasWhite ? g.result.score : -g.result.score;
    if (s > 0) winsA++;
    else if (s < 0) winsB++;
    else draws++;
  }

  /** A snapshot that won't change while the match goes on (safe to hand to the screen). */
  public synchronized MatchResult copy() {
    MatchResult c = new MatchResult(nameA, nameB);
    c.winsA = winsA;
    c.winsB = winsB;
    c.draws = draws;
    c.games.addAll(games);
    return c;
  }

  public int winsA() {
    return winsA;
  }

  public int winsB() {
    return winsB;
  }

  public int draws() {
    return draws;
  }

  public int gamesPlayed() {
    return games.size();
  }

  public List<GameRecord> games() {
    return Collections.unmodifiableList(games);
  }

  /** A's score as a fraction: wins count 1, draws 0.5. This is the number to maximise when training. */
  public double scoreA() {
    int n = gamesPlayed();
    return n == 0 ? 0.5 : (winsA + 0.5 * draws) / n;
  }

  /**
   * Rating difference (A minus B) implied by the score, using the standard Elo formula.
   * Capped at +/-800 for a clean sweep.
   */
  public double eloDifference() {
    double s = scoreA();
    if (s <= 0.0) return -800;
    if (s >= 1.0) return 800;
    return Math.max(-800, Math.min(800, -400 * Math.log10(1 / s - 1)));
  }

  public String summary() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format(Locale.US, "%s vs %s%n", nameA, nameB));
    sb.append(String.format(Locale.US, "Games Played: %d%n%n", gamesPlayed()));
    sb.append(String.format(Locale.US, "%s: %d wins, %d losses, %d draws%n", nameA, winsA, winsB, draws));
    sb.append(String.format(Locale.US, "%s: %d wins, %d losses, %d draws%n%n", nameB, winsB, winsA, draws));
    sb.append(String.format(Locale.US, "WIN RATES:%n----------%n"));
    sb.append(String.format(Locale.US, "%s: %.1f%%%n", nameA, 100 * scoreA()));
    sb.append(String.format(Locale.US, "%s: %.1f%%%n%n", nameB, 100 * (1 - scoreA())));
    sb.append(String.format(Locale.US, "ELO DIFFERENCE: %s is %+.0f%n", nameA, eloDifference()));
    return sb.toString();
  }

  /** CSV header matching {@link #toCsvLine}. */
  public static String csvHeader() {
    return "timestamp,agentA,agentB,games,winsA,winsB,draws,scoreA,eloDiff";
  }

  public String toCsvLine(long timestamp) {
    return String.format(Locale.US, "%d,%s,%s,%d,%d,%d,%d,%.4f,%.1f", timestamp,
        nameA.replace(",", " "), nameB.replace(",", " "), gamesPlayed(), winsA, winsB, draws, scoreA(), eloDifference());
  }
}
