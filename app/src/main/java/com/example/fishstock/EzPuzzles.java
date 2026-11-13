package com.example.fishstock;

import com.example.fishstock.puzzles.Puzzle;
import com.example.fishstock.puzzles.PuzzleBook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "EZ to Visualize": puzzles with 6 pieces or fewer (kings included), for solving in your head.
 * On the puzzle screen, pick it under Select theme, then READ ALOUD and HIDE BOARD.
 *
 * Easiest first: two mates in one, then one-move tactics (skewer, double check, forks), then mates
 * in two and three, and a promotion that ends in a skewer.
 *
 * Found by running FishStock's puzzle generator ({@code puzzles/PuzzleGenerator}) over random
 * positions of 4-6 pieces: one move wins and no other does. Each was searched again two plies
 * deeper, and the mates were proved by trying every move.
 *
 * Same format as {@link PuzzleBook}: FEN | line (UCI, solver first) | continuation | best score |
 * second score | mate in. To add more, append lines in that format; any that don't replay legally
 * are skipped.
 */
public final class EzPuzzles {
  public static final String GROUP = "EZ to Visualize";

  static final String[] ENTRIES = {
      // 1... Ra3#  [Skewer, Checkmate, Endgame]  (5 pieces)
      "8/R7/8/K1k5/8/2r5/7n/8 b - - 0 1|c3a3||999.00|0.65|1",
      // 1... Rh6#  [Checkmate, Endgame]  (5 pieces)
      "6Rn/8/2r5/8/5k1K/8/8/8 b - - 0 1|c6h6||999.00|0.71|1",
      // 1... Qh8+ 2. Kg5 Qxh3  [Skewer]  (4 pieces)
      "4q3/8/7K/1k6/8/7Q/8/8 b - - 0 1|e8h8 h6g5 h8h3|g5f6 h3c3 f6f5|13.32|0.02|0",
      // 1... Na4+  [Double check, Discovered check, Fork, Endgame]  (5 pieces)
      "2r5/8/8/k1n5/8/2K5/1R6/8 b - - 0 1|c5a4|c3d4 a4b2 d4e4|10.02|0.66|0",
      // 1... Nf6+ 2. Ke6 Nxg4  [Fork, Endgame]  (5 pieces)
      "8/3n4/8/3K4/6R1/8/2p5/4k3 b - - 0 1|d7f6 d5e6 f6g4|e6f5 g4f2 f5g6|15.81|0.54|0",
      // 1. Nb6+ Kb7 2. Rxb3  [Endgame]  (5 pieces)
      "k1N5/8/8/1R6/8/1r6/8/5K2 w - - 0 1|c8b6 a8b7 b5b3|b7c7 b6c4 c7c6|9.98|0.60|0",
      // 1... Ra7+ 2. Qa6 Rxa6#  [Checkmate]  (5 pieces)
      "1r6/7r/2Q5/8/K2k4/8/8/8 b - - 0 1|h7a7 c6a6 a7a6||997.00|0.68|2",
      // 1... Qxc1+ 2. Ka2 Qb2#  [Remove the defender, Checkmate]  (5 pieces)
      "8/8/8/8/8/p7/2q4k/K1Q5 b - - 0 1|c2c1 a1a2 c1b2||997.00|0.00|2",
      // 1... h1=Q 2. g8=Q Qa8+ 3. Kc7 Qxg8  [Skewer, Pawn race, Promotion, Endgame]  (6 pieces)
      "3K4/6P1/1P6/8/8/4p3/7p/4k3 b - - 0 1|h2h1q g7g8q h1a8 d8c7 a8g8|b6b7 g8f7 c7b6|10.73|0.50|0",
      // 1. Qxh2 Kb1 2. Kc3 Ka1 3. Qb2#  [Remove the defender, Checkmate]  (4 pieces)
      "8/8/3Q4/8/3K4/8/7q/2k5 w - - 0 1|d6h2 c1b1 d4c3 b1a1 h2b2||995.00|0.00|3",
      // 1... Rxg4 2. Ke8 Rd4 3. Kf8 Rd8#  [Checkmate, Endgame]  (5 pieces)
      "5K2/1n6/5k2/6r1/6R1/8/8/8 b - - 0 1|g5g4 f8e8 g4d4 e8f8 d4d8||995.00|0.72|3",
      // 1. g7 Re6 2. g8=Q Rxe5 3. Qa8+  [Promotion, Quiet move, Endgame]  (5 pieces)
      "8/8/5rP1/4N3/k1K5/8/8/8 w - - 0 1|g6g7 f6e6 g7g8q e6e5 g8a8|e5a5|7.24|-0.07|0",
  };

  private static List<Puzzle> all;

  private EzPuzzles() {}

  public static synchronized List<Puzzle> all() {
    if (all == null) {
      List<Puzzle> list = new ArrayList<>();
      for (String e : ENTRIES) {
        Puzzle p = PuzzleBook.parse(e);
        if (p != null) list.add(p);
      }
      all = Collections.unmodifiableList(list);
    }
    return all;
  }
}
