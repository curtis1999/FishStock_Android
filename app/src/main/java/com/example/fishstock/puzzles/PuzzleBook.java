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
      // ---- second batch (20 puzzles, seeds 2027 and 2028)
      // 16. Rd8# [Back-rank mate, Pin, Checkmate]
      "1rb1kb1r/p4ppp/4P2n/1pp3B1/5P2/2NR4/PP4nP/2K3NR w k - 0 16|d3d8||999.00|-3.28|1",
      // 29. Rxb8+ Rf8 30. Rxf8# [Back-rank mate, Remove the defender, Checkmate]
      "1r5k/p5pp/4p3/2pppr2/P1b5/4P1PN/P6P/1RB1K3 w - - 0 29|b1b8 f5f8 b8f8||997.00|-8.18|2",
      // 40... Qa1+ 41. Qd1 Qxd1+ 42. Re1 Qxe1# [Back-rank mate, Remove the defender, Checkmate]
      "6r1/q4k2/4b1pp/1p3p2/1P1Q3P/2P1R3/5PPR/6K1 b - - 0 40|a7a1 d4d1 a1d1 e3e1 d1e1||995.00|-3.01|3",
      // 24. Bg5+ Kd6 25. Rd4+ Ke5 26. Bf6# [Deflection, Checkmate]
      "r3r3/p1bbk2p/2p1p1pB/1pn5/6RP/PPP3PB/2K5/1N3R2 w - - 9 24|h6g5 e7d6 g4d4 d6e5 g5f6||995.00|0.61|3",
      // 24... Rd1+ 25. Rc1 Rxc1# [Back-rank mate, Checkmate]
      "r2r2k1/p1p2ppp/Bp2p3/8/Q7/7P/PPR2PPb/K7 b - - 0 24|d8d1 c2c1 d1c1||997.00|-1.24|2",
      // 26. Nc6+ [Fork]
      "3r1b1r/4k1pp/6b1/pNp1N3/5P2/7P/PP4n1/2KR1R2 w - - 2 26|e5c6|e7e6 d1d8 a5a4|2.60|-1.96|0",
      // 34... Qe2+ 35. Kd4 Qxd1+ [Fork, Skewer]
      "7k/4B3/3p2rp/1Q6/2P1KP2/8/q7/3R4 b - - 0 34|a2e2 e4d4 e2d1|d4c3 d1e1 c3d4|6.20|0.42|0",
      // 39... Qg3+ 40. Ke4 Qxb3 [Skewer]
      "6k1/6q1/7p/8/2P2P2/1Q2K3/8/3R4 b - - 1 39|g7g3 e3e4 g3b3|d1d8 g8f7 d8d7|2.26|-7.96|0",
      // 30... Bh6+ 31. Kd3 Bxc1 [Skewer]
      "r7/p1P2kbp/Bp6/1P2p3/3nN3/4K3/P1P5/2R5 b - - 0 30|g7h6 e3d3 h6c1|a2a4 d4b5 a4b5|1.86|-2.25|0",
      // 28... b6+ [Discovered check]
      "q7/1p2p1k1/3p3p/B4Q2/2P1KP2/6r1/P7/R7 b - - 0 28|b7b6|f5d5 a8a5 f4f5|1.70|0.61|0",
      // 7... Nxd4 [Sacrifice]
      "r1bqkb1r/1ppppppp/2n5/pB2P3/Pn1P4/7N/1PP2PPP/RNBQK2R b KQkq - 0 7|c6d4|b1a3 b4c6 c1f4|1.54|-0.03|0",
      // 45... e1=Q+ [Fork, Promotion, Endgame]
      "8/k7/p1p1N3/8/1P5P/8/3Rp3/2K5 b - - 1 45|e2e1q|d2d1 e1c3 c1b1|3.50|-0.53|0",
      // 40. c6 dxc6 41. b6 cxb6 42. d6 [Pawn breakthrough, Sacrifice, Quiet move, Endgame]
      "8/1ppp4/8/1PPP4/6k1/8/8/7K w - - 0 40|c5c6 d7c6 b5b6 c7b6 d5d6|b6b5 d6d7 g4f3|2.75|-0.20|0",
      // 40. f6 Kd5 41. f7 c5 42. f8=Q [Pawn race, Promotion, Quiet move, Endgame]
      "8/2p5/8/5P2/2kp4/8/K5P1/8 w - - 0 40|f5f6 c4d5 f6f7 c7c5 f7f8q|c5c4 f8f5 d5c6|7.03|0.09|0",
      // 32... Ka7 [Quiet move, Endgame]
      "1k4R1/1pp2p2/p1p1r3/3n4/8/8/PPPK4/8 b - - 9 32|b8a7|g8g7 f7f5 d2c1|5.24|-0.92|0",
      // Mate in 1: 15. Qc8# [Back-rank mate, Skewer, Checkmate]
      "rn2k1r1/pp2ppbp/2p3p1/8/2P5/7Q/PP2PP1P/1qB1K2R w q - 0 15|h3c8||999.00|-11.50|1",
      // Mate in 2: 38... Rh8+ [Checkmate]
      "3r4/p1q3k1/8/3P4/P5P1/5P2/8/4RR1K b - - 0 38|d8h8|h1g1 c7g3|997.00|0.52|2",
      // Remove the defender/Deflection: 11... Nxd4 12. Nxd4 Qxg5 [Deflection, Sacrifice]
      "2kr3r/ppp1ppbp/2n1bnp1/1B3qB1/3P4/P1N2N2/1PP2PPP/R2Q2KR b - - 0 11|c6d4 f3d4 f5g5|d1d3 g5g4 a1d1|2.13|0.14|0",
      // Fork: 26. Qb7+ Nd7 27. Qxd7+ Re7 28. Bxe6+ Kf6 29. Bg5+ [Fork, Sacrifice]
      "rn2r3/5k1p/1Q2p1pB/5p2/2BP4/5P1P/6P1/4K2n w - - 7 26|b6b7 b8d7 b7d7 e8e7 c4e6 f7f6 h6g5|f6g5 d7e7 g5h6|5.57|-0.12|0",
      // Sacrifice: 35... Rb8 36. Nh7+ Kg7 37. Qxb8 Qxb8 [Sacrifice, Quiet move]
      "r3q3/pQ1b1p2/2p2k2/2Pp2N1/P2P1P2/6P1/8/2R3K1 b - - 0 35|a8b8 g5h7 f6g7 b7b8 e8b8|h7g5 b8b3 g1g2|1.51|0.00|0",
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

  /**
   * One line of the book for a puzzle (what the builder prints). Training puzzles add two more
   * fields: the other accepted first moves (UCI) and the title.
   */
  public static String format(Puzzle p) {
    String s = p.fen + "|" + uci(p.line) + "|" + uci(p.continuation) + "|"
        + String.format(Locale.US, "%.2f|%.2f|%d", p.bestScore, p.secondScore, p.mateIn);
    if (!p.alsoAccepted.isEmpty() || !p.title.isEmpty()) {
      s += "|" + uci(p.alsoAccepted) + "|" + p.title.replace("|", "/");
    }
    return s;
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
      List<Move> accepted = null;
      if (f.length > 6) {
        accepted = new ArrayList<>();
        Game start = new Game(f[0]);
        for (String u : f[6].trim().split("\\s+")) {
          Move m = u.isEmpty() ? null : start.findUci(u);
          if (m != null) accepted.add(m);
        }
      }
      String title = f.length > 7 ? f[7] : "";
      return new Puzzle(f[0], line, cont, Double.parseDouble(f[3]), Double.parseDouble(f[4]),
          Integer.parseInt(f[5]), themes, accepted, title);
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
