package com.example.fishstock.puzzles;

import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Puzzles found ahead of time by {@code tools/BuildPuzzleBook} (run it on a desktop and paste its
 * output into {@link #ENTRIES}), grouped for the "Select theme" menu: Mate in 1 to Mate in 4 first,
 * then one group per {@link Theme}. A puzzle appears in every group it belongs to.
 *
 * Each entry: FEN | line (UCI, solver first) | continuation (UCI) | best score | second score | mate in.
 * Themes are worked out again when the book is loaded, so improving {@link ThemeDetector} updates them.
 */
public final class PuzzleBook {
  static final String[] ENTRIES = {
      // BEGIN GENERATED
      // 6042 positions searched, seed 2026
      // Mate in 1: 25... Qe1# [Back-rank mate, Checkmate]
      "4kbr1/4pp1p/1Qp5/p5p1/4P2q/2N2P2/PPR3PP/6K1 b - - 2 25|h4e1||999.00|-3.10|1",
      // Mate in 2: 31... Rf1+ 32. Kh2 R8f2# [Checkmate]
      "5r2/Rp1k2p1/2p1pr2/3p3N/1P1P4/R3P1PP/4P3/7K b - - 0 31|f6f1 h1h2 f8f2||997.00|-4.76|2",
      // Mate in 3: 24. Qg8+ Qf8 25. Qxf8+ Be8 26. Qxe8# [Back-rank mate, Fork, Skewer, Remove the defender, Checkmate]
      "r1k5/1ppb1Q2/3p3r/p3b2p/2P1Pq2/1K5P/PPN2PP1/3R2R1 w - - 1 24|f7g8 f4f8 g8f8 d7e8 f8e8||995.00|-1.11|3",
      // Mate in 4: 38... a1=Q+ 39. Rd1 Qxd1+ 40. Kg2 Qf1+ 41. Kh2 Rf2# [Deflection, Promotion, Checkmate, Endgame]
      "5rk1/P3R3/6pp/3R4/2p5/6PP/p1P5/7K b - - 4 38|a2a1q d5d1 a1d1 h1g2 d1f1 g2h2 f8f2||993.00|-1.70|4",
      // Fork: 18. Nc6+ Kf8 19. Nxa7 [Fork]
      "r1b3r1/q2nkppp/pp1pp3/8/B1PNPP1b/8/PP1N3P/R1BK3Q w - - 3 18|d4c6 e7f8 c6a7|a8a7 d2f3 h4f6|2.17|-2.01|0",
      // Skewer/Pin: 45... Rf1+ 46. Kd2 Rxa1 [Skewer]
      "8/2b1k3/p4rp1/2p1p1Np/2P1B2P/1P6/8/R2K4 b - - 2 45|f6f1 d1d2 f1a1|e4g6 a1h1 g6h5|2.73|-1.68|0",
      // Discovered check/Double check: 25... Rb1+ 26. Kg2 gxh6+ [Discovered check]
      "6rk/2p1npp1/R3p2P/2P5/3P4/2P1B2P/1r2PP2/5KR1 b - - 0 25|b2b1 f1g2 g7h6|g2f3 b1g1 e3h6|2.61|-3.04|0",
      // Remove the defender/Deflection: 29. Rd8+ Be8 30. Rxe8+ Kf7 31. Rxh8 [Fork, Deflection]
      "6kr/5bpp/3R4/pp6/4p3/1P3N2/P1P3PP/3n2K1 w - - 0 29|d6d8 f7e8 d8e8 g8f7 e8h8|e4f3 g2f3 f7g6|4.04|0.07|0",
      // Pawn race: 44. c7 Kxe7 45. c8=Q [Pawn race, Promotion, Quiet move, Endgame]
      "8/4Pk2/p1P2p2/8/8/8/4K3/8 w - - 1 44|c6c7 f7e7 c7c8q|a6a5 c8c7 e7e6|8.32|-1.19|0",
      // Pawn breakthrough: 42. h6 gxh6 43. gxf6 Kc5 44. f7 [Pawn breakthrough, Quiet move, Endgame]
      "8/6pp/5p2/5PPP/2k5/8/8/K7 w - - 0 42|h5h6 g7h6 g5f6 c4c5 f6f7|c5d4 f5f6 h6h5|8.54|-0.30|0",
      // END GENERATED
  };

  private static List<Puzzle> all;
  private static Map<String, List<Puzzle>> groups;

  private PuzzleBook() {}

  public static synchronized List<Puzzle> all() {
    if (all == null) {
      List<Puzzle> list = new ArrayList<>();
      for (String e : ENTRIES) {
        Puzzle p = parse(e);
        if (p != null) list.add(p);
      }
      all = Collections.unmodifiableList(list);
    }
    return all;
  }

  /** Group name -> puzzles, in menu order. */
  public static synchronized Map<String, List<Puzzle>> groups() {
    if (groups != null) return groups;
    Map<String, List<Puzzle>> g = new LinkedHashMap<>();
    for (int n = 1; n <= 4; n++) {
      List<Puzzle> l = new ArrayList<>();
      for (Puzzle p : all()) if (p.mateIn == n) l.add(p);
      if (!l.isEmpty()) g.put(mateGroup(n), l);
    }
    for (Theme t : Theme.values()) {
      if (t == Theme.MATE) continue;
      List<Puzzle> l = new ArrayList<>();
      for (Puzzle p : all()) if (p.hasTheme(t)) l.add(p);
      if (!l.isEmpty()) g.put(t.label, l);
    }
    groups = g;
    return g;
  }

  public static String mateGroup(int n) {
    return String.format(Locale.US, "Mate in %d", n);
  }

  /** One line of the book for a puzzle (what the builder prints). */
  public static String format(Puzzle p) {
    return p.fen + "|" + uci(p.line) + "|" + uci(p.continuation) + "|"
        + String.format(Locale.US, "%.2f|%.2f|%d", p.bestScore, p.secondScore, p.mateIn);
  }

  /** Parses one entry; null if it doesn't replay legally. */
  public static Puzzle parse(String entry) {
    try {
      String[] f = entry.split("\\|", -1);
      Game g = new Game(f[0]);
      List<Move> line = moves(g, f[1]);
      List<Move> cont = moves(g, f[2]);
      if (line == null || cont == null || line.isEmpty()) return null;
      List<Theme> themes = ThemeDetector.detect(new Game(f[0]).position(), line, cont);
      return new Puzzle(f[0], line, cont, Double.parseDouble(f[3]), Double.parseDouble(f[4]),
          Integer.parseInt(f[5]), themes);
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static List<Move> moves(Game g, String uci) {
    List<Move> out = new ArrayList<>();
    if (uci.trim().isEmpty()) return out;
    for (String u : uci.trim().split("\\s+")) {
      if (g.isOver()) return out;
      Move m = g.findUci(u);
      if (m == null) return null;
      out.add(m);
      g.play(m);
    }
    return out;
  }

  private static String uci(List<Move> moves) {
    StringBuilder sb = new StringBuilder();
    for (Move m : moves) sb.append(m.toUci()).append(' ');
    return sb.toString().trim();
  }
}
