package com.example.fishstock.arena;

import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;

/**
 * Short, well-known opening lines used to start tournament games.
 *
 * Two searching agents left to themselves tend to play the same game over and over, so a
 * 100-game match would really be 2 games repeated 50 times. Instead, each pair of games starts
 * from the next opening below, played once with each agent as White. That way a win rate
 * reflects the agents, not one lucky line.
 */
public final class Openings {
  private Openings() {}

  /** Moves in UCI form, 2-4 moves for each side. */
  public static final String[] LINES = {
      "e2e4 e7e5 g1f3 b8c6 f1b5",            // Ruy Lopez
      "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5",       // Italian
      "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4",       // Sicilian
      "e2e4 e7e6 d2d4 d7d5",                 // French
      "e2e4 c7c6 d2d4 d7d5",                 // Caro-Kann
      "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6",       // Queen's Gambit Declined
      "d2d4 d7d5 c2c4 d5c4",                 // Queen's Gambit Accepted
      "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7",       // King's Indian
      "d2d4 g8f6 c2c4 e7e6 b1c3 f8b4",       // Nimzo-Indian
      "c2c4 e7e5 b1c3 g8f6",                 // English
      "g1f3 d7d5 g2g3 g8f6 f1g2",            // Reti
      "e2e4 d7d5 e4d5 d8d5 b1c3",            // Scandinavian
      "e2e4 e7e5 f2f4",                      // King's Gambit
      "d2d4 f7f5 g2g3 g8f6 f1g2",            // Dutch
      "e2e4 g8f6 e4e5 f6d5 d2d4",            // Alekhine
      "e2e4 d7d6 d2d4 g8f6 b1c3 g7g6",       // Pirc
  };

  /** The opening for a given game: games 0 and 1 share line 0, games 2 and 3 line 1, ... */
  public static String forGame(int gameIndex) {
    return LINES[(gameIndex / 2) % LINES.length];
  }

  /** Plays the opening moves into the game. Stops quietly at a move that doesn't fit. */
  public static void play(Game game, String uciLine) {
    if (uciLine == null) return;
    for (String uci : uciLine.trim().split("\\s+")) {
      if (uci.isEmpty() || game.isOver()) return;
      Move m = game.findUci(uci);
      if (m == null) return;
      game.play(m);
    }
  }
}
