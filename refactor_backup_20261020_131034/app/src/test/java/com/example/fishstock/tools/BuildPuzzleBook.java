package com.example.fishstock.tools;

import com.example.fishstock.arena.Openings;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.puzzles.EndgameSampler;
import com.example.fishstock.puzzles.Puzzle;
import com.example.fishstock.puzzles.PuzzleBook;
import com.example.fishstock.puzzles.PuzzleGenerator;
import com.example.fishstock.puzzles.Theme;
import com.example.fishstock.search.MateFinder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Finds puzzles for {@link PuzzleBook}, one per category below, and prints them in book format.
 * Run on a desktop (it can take a while): it's a plain main() so it doesn't slow the unit tests.
 *
 *   java ... com.example.fishstock.tools.BuildPuzzleBook [seed] [minutes]
 *
 * To grow the collection, raise the counts in CATEGORIES and paste the output between the
 * GENERATED markers in PuzzleBook.
 */
public final class BuildPuzzleBook {
  /** Category -> how many puzzles to find. */
  private static final Map<String, Integer> CATEGORIES = new LinkedHashMap<>();

  static {
    // Second batch (20 puzzles). The first batch was one of each of the first ten.
    CATEGORIES.put("Mate in 1", 2);
    CATEGORIES.put("Mate in 2", 3);
    CATEGORIES.put("Mate in 3", 2);
    CATEGORIES.put("Mate in 4", 1);
    CATEGORIES.put(Theme.FORK.label, 2);
    CATEGORIES.put(Theme.SKEWER.label + "/" + Theme.PIN.label, 2);
    CATEGORIES.put(Theme.DISCOVERED_CHECK.label + "/" + Theme.DOUBLE_CHECK.label + "/" + Theme.DISCOVERED_ATTACK.label, 1);
    CATEGORIES.put(Theme.REMOVE_THE_DEFENDER.label + "/" + Theme.DEFLECTION.label, 2);
    CATEGORIES.put(Theme.PAWN_RACE.label, 1);
    CATEGORIES.put(Theme.PAWN_BREAKTHROUGH.label, 1);
    CATEGORIES.put(Theme.SACRIFICE.label, 1);
    CATEGORIES.put(Theme.PROMOTION.label, 1);
    CATEGORIES.put(Theme.QUIET_MOVE.label, 1);
  }

  public static void main(String[] args) {
    long seed = args.length > 0 ? Long.parseLong(args[0]) : 2026;
    long minutes = args.length > 1 ? Long.parseLong(args[1]) : 30;
    long deadline = System.currentTimeMillis() + minutes * 60_000;
    Random random = new Random(seed);
    PuzzleGenerator gen = new PuzzleGenerator(EvalWeights.defaults(), random);
    WeightedEvaluator ev = WeightedEvaluator.withDefaults();
    Map<String, List<Puzzle>> found = new LinkedHashMap<>();
    for (String c : CATEGORIES.keySet()) found.put(c, new ArrayList<Puzzle>());
    List<String> fens = new ArrayList<>();
    for (Puzzle old : PuzzleBook.all()) fens.add(old.fen); // don't repeat puzzles already in the book
    int checked = 0;

    while (System.currentTimeMillis() < deadline && !done(found)) {
      boolean endgame = random.nextInt(3) == 0;
      Game game;
      int first;
      int last;
      if (endgame) {
        game = new Game(EndgameSampler.next(random).toFen());
        first = 0;
        last = 8;
      } else {
        game = new Game();
        Openings.play(game, Openings.LINES[random.nextInt(Openings.LINES.length)]);
        first = 10;
        last = 90;
      }
      while (!game.isOver() && game.plyCount() < last && System.currentTimeMillis() < deadline) {
        if (game.plyCount() >= first) {
          checked++;
          Position pos = game.position();
          Puzzle p = null;
          MateFinder.Mate mate = MateFinder.find(pos, 4, 300, 300_000, null);
          if (mate != null && mate.movesToMate >= 2 && need(found, "Mate in " + mate.movesToMate)) {
            int d = 2 * mate.movesToMate;
            p = gen.check(pos, game.positionHistory(), game.lastMove(), null, d, d);
          } else if (mate == null || need(found, "Mate in 1")) {
            p = gen.check(pos, game.positionHistory(), game.lastMove(), null);
          }
          if (p != null && !fens.contains(p.fen)) {
            String cat = categoryFor(p, found);
            if (cat != null) {
              found.get(cat).add(p);
              fens.add(p.fen);
              System.err.println("[" + checked + " positions] " + cat + ": " + p + "  " + p.lineSan());
              // Printed straight away in book format, so stopping the run early loses nothing.
              System.out.println("      // " + cat + ": " + p.lineSan() + " [" + p.themeLabels() + "]");
              System.out.println("      \"" + PuzzleBook.format(p) + "\",");
              System.out.flush();
            }
          }
        }
        Move m = sloppy(game.position(), ev, random);
        if (m == null) break;
        game.play(m);
      }
    }
    System.out.println("      // " + checked + " positions searched, seed " + seed);
  }

  private static String categoryFor(Puzzle p, Map<String, List<Puzzle>> found) {
    if (p.mateIn > 0) return need(found, "Mate in " + p.mateIn) ? "Mate in " + p.mateIn : null;
    for (String c : CATEGORIES.keySet()) {
      if (c.startsWith("Mate") || !need(found, c)) continue;
      for (String label : c.split("/")) {
        for (Theme t : p.themes) if (t.label.equals(label)) return c;
      }
    }
    return null;
  }

  private static boolean need(Map<String, List<Puzzle>> found, String c) {
    return found.containsKey(c) && found.get(c).size() < CATEGORIES.get(c);
  }

  private static boolean done(Map<String, List<Puzzle>> found) {
    for (String c : CATEGORIES.keySet()) if (need(found, c)) return false;
    return true;
  }

  /** The same careless sampling as the puzzle generator: P(move) ~ exp(score / 0.35). */
  private static Move sloppy(Position position, WeightedEvaluator ev, Random random) {
    Position pos = position.copy();
    List<Move> moves = MoveGenerator.legalMoves(pos);
    if (moves.isEmpty()) return null;
    boolean us = pos.whiteToMove();
    double[] w = new double[moves.size()];
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < w.length; i++) {
      pos.makeMove(moves.get(i));
      w[i] = ev.evaluateFor(pos, us);
      pos.unmakeMove(moves.get(i));
      max = Math.max(max, w[i]);
    }
    double total = 0;
    for (int i = 0; i < w.length; i++) total += (w[i] = Math.exp((w[i] - max) / 0.35));
    double r = random.nextDouble() * total;
    for (int i = 0; i < w.length; i++) if ((r -= w[i]) <= 0) return moves.get(i);
    return moves.get(moves.size() - 1);
  }
}
