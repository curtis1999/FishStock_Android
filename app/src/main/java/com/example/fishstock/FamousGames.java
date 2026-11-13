package com.example.fishstock;

import java.util.Arrays;
import java.util.List;

/**
 * Ten famous and instructive games, put into Saved Games the first time it opens (after that they
 * are ordinary saved games: rename or delete them as you like).
 *
 * Moves are UCI, from the standard start position. Every game was checked move by move with a chess
 * library (python-chess) before being added; the five that end in mate end in checkmate.
 * Sources: Wikipedia (Opera, Immortal, Evergreen, Game of the Century, Rotlewi-Rubinstein,
 * Carlsen-Nepomniachtchi game 6), and the Caissly / ChessBase / Lichess game pages for the rest,
 * each compared with a second source where one was available.
 *
 * "side" is whose point of view to review from: 1 = White, -1 = Black (that side at the bottom, and
 * its moves are the ones the key moments look at).
 */
public final class FamousGames {
  private FamousGames() {}

  public static final class Game {
    public final String id;
    public final String name;
    public final String event;
    public final String result;
    public final int side;
    public final String note;
    public final String movesUci;

    Game(String id, String name, String event, String result, int side, String note, String movesUci) {
      this.id = id;
      this.name = name;
      this.event = event;
      this.result = result;
      this.side = side;
      this.note = note;
      this.movesUci = movesUci;
    }
  }

  private static Game game(String id, String name, String event, String result, int side, String note, String moves) {
    return new Game(id, name, event, result, side, note, moves);
  }

  public static final List<Game> ALL = Arrays.asList(
      game("opera", "Opera Game: Morphy vs Duke Karl & Count Isouard",
          "Paris 1858", "1-0", 1,
          "Rapid development and open lines beat a king left in the centre. Ends with a queen sacrifice and mate on the back rank.",
          "e2e4 e7e5 g1f3 d7d6 d2d4 c8g4 d4e5 g4f3 d1f3 d6e5 f1c4 g8f6 f3b3 d8e7 b1c3 c7c6 "
            + "c1g5 b7b5 c3b5 c6b5 c4b5 b8d7 e1c1 a8d8 d1d7 d8d7 h1d1 e7e6 b5d7 f6d7 b3b8 d7b8 "
            + "d1d8"),
      game("immortal", "The Immortal Game: Anderssen vs Kieseritzky",
          "London 1851", "1-0", 1,
          "Romantic-era King's Gambit: White gives up both rooks, a bishop and the queen, and mates with three minor pieces.",
          "e2e4 e7e5 f2f4 e5f4 f1c4 d8h4 e1f1 b7b5 c4b5 g8f6 g1f3 h4h6 d2d3 f6h5 f3h4 h6g5 "
            + "h4f5 c7c6 g2g4 h5f6 h1g1 c6b5 h2h4 g5g6 h4h5 g6g5 d1f3 f6g8 c1f4 g5f6 b1c3 f8c5 "
            + "c3d5 f6b2 f4d6 c5g1 e4e5 b2a1 f1e2 b8a6 f5g7 e8d8 f3f6 g8f6 d6e7"),
      game("evergreen", "The Evergreen Game: Anderssen vs Dufresne",
          "Berlin 1852", "1-0", 1,
          "Evans Gambit. A lead in development turned into a mating attack, finished by a queen sacrifice and a double check.",
          "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 b2b4 c5b4 c2c3 b4a5 d2d4 e5d4 e1g1 d4d3 d1b3 d8f6 "
            + "e4e5 f6g6 f1e1 g8e7 c1a3 b7b5 b3b5 a8b8 b5a4 a5b6 b1d2 c8b7 d2e4 g6f5 c4d3 f5h5 "
            + "e4f6 g7f6 e5f6 h8g8 a1d1 h5f3 e1e7 c6e7 a4d7 e8d7 d3f5 d7e8 f5d7 e8f8 a3e7"),
      game("rubinstein", "Rubinstein's Immortal: Rotlewi vs Rubinstein",
          "Lodz 1907", "0-1", -1,
          "Black's two bishops rake the long diagonals; a chain of sacrifices opens the white king. Review it as Black.",
          "d2d4 d7d5 g1f3 e7e6 e2e3 c7c5 c2c4 b8c6 b1c3 g8f6 d4c5 f8c5 a2a3 a7a6 b2b4 c5d6 "
            + "c1b2 e8g8 d1d2 d8e7 f1d3 d5c4 d3c4 b7b5 c4d3 f8d8 d2e2 c8b7 e1g1 c6e5 f3e5 d6e5 "
            + "f2f4 e5c7 e3e4 a8c8 e4e5 c7b6 g1h1 f6g4 d3e4 e7h4 g2g3 c8c3 g3h4 d8d2 e2d2 b7e4 "
            + "d2g2 c3h3"),
      game("capablanca", "Capablanca vs Tartakower: the rook endgame",
          "New York 1924", "1-0", 1,
          "Textbook endgame technique: rook to the 7th, king marches in, pawns are given up for activity (35.Kg3!!).",
          "d2d4 e7e6 g1f3 f7f5 c2c4 g8f6 c1g5 f8e7 b1c3 e8g8 e2e3 b7b6 f1d3 c8b7 e1g1 d8e8 "
            + "d1e2 f6e4 g5e7 e4c3 b2c3 e8e7 a2a4 b7f3 e2f3 b8c6 f1b1 a8e8 f3h3 f8f6 f2f4 c6a5 "
            + "h3f3 d7d6 b1e1 e7d7 e3e4 f5e4 f3e4 g7g6 g2g3 g8f8 g1g2 f6f7 h2h4 d6d5 c4d5 e6d5 "
            + "e4e8 d7e8 e1e8 f8e8 h4h5 f7f6 h5g6 h7g6 a1h1 e8f8 h1h7 f6c6 g3g4 a5c4 g4g5 c4e3 "
            + "g2f3 e3f5 d3f5 g6f5 f3g3 c6c3 g3h4 c3f3 g5g6 f3f4 h4g5 f4e4 g5f6 f8g8 h7g7 g8h8 "
            + "g7c7 e4e8 f6f5 e8e4 f5f6 e4f4 f6e5 f4g4 g6g7 h8g8 c7a7 g4g1 e5d5 g1c1 d5d6 c1c2 "
            + "d4d5 c2c1 a7c7 c1a1 d6c6 a1a4 d5d6"),
      game("century", "The Game of the Century: D. Byrne vs Fischer",
          "New York 1956", "0-1", -1,
          "13-year-old Fischer gives up his queen (17...Be6!!) for a crushing attack and a windmill of checks. Review it as Black.",
          "g1f3 g8f6 c2c4 g7g6 b1c3 f8g7 d2d4 e8g8 c1f4 d7d5 d1b3 d5c4 b3c4 c7c6 e2e4 b8d7 "
            + "a1d1 d7b6 c4c5 c8g4 f4g5 b6a4 c5a3 a4c3 b2c3 f6e4 g5e7 d8b6 f1c4 e4c3 e7c5 f8e8 "
            + "e1f1 g4e6 c5b6 e6c4 f1g1 c3e2 g1f1 e2d4 f1g1 d4e2 g1f1 e2c3 f1g1 a7b6 a3b4 a8a4 "
            + "b4b6 c3d1 h2h3 a4a2 g1h2 d1f2 h1e1 e8e1 b6d8 g7f8 f3e1 c4d5 e1f3 f2e4 d8b8 b7b5 "
            + "h3h4 h7h5 f3e5 g8g7 h2g1 f8c5 g1f1 e4g3 f1e1 c5b4 e1d1 d5b3 d1c1 g3e2 c1b1 e2c3 "
            + "b1c1 a2c2"),
      game("fischer6", "Fischer vs Spassky, World Championship game 6",
          "Reykjavik 1972", "1-0", 1,
          "Fischer opens 1.c4 and wins a pure positional game: hanging pawns fixed, f-file pressure, then the break e5-e6. Spassky applauded.",
          "c2c4 e7e6 g1f3 d7d5 d2d4 g8f6 b1c3 f8e7 c1g5 e8g8 e2e3 h7h6 g5h4 b7b6 c4d5 f6d5 "
            + "h4e7 d8e7 c3d5 e6d5 a1c1 c8e6 d1a4 c7c5 a4a3 f8c8 f1b5 a7a6 d4c5 b6c5 e1g1 a8a7 "
            + "b5e2 b8d7 f3d4 e7f8 d4e6 f7e6 e3e4 d5d4 f2f4 f8e7 e4e5 c8b8 e2c4 g8h8 a3h3 d7f8 "
            + "b2b3 a6a5 f4f5 e6f5 f1f5 f8h7 c1f1 e7d8 h3g3 a7e7 h2h4 b8b7 e5e6 b7c7 g3e5 d8e8 "
            + "a2a4 e8d8 f1f2 d8e8 f2f3 e8d8 c4d3 d8e8 e5e4 h7f6 f5f6 g7f6 f3f6 h8g8 d3c4 g8h8 "
            + "e4f4"),
      game("octopus", "Karpov vs Kasparov, World Championship game 16",
          "Moscow 1985", "0-1", -1,
          "Kasparov's 'octopus' knight on d3 paralyses White for twenty moves: a model of a dominant outpost. Review it as Black.",
          "e2e4 c7c5 g1f3 e7e6 d2d4 c5d4 f3d4 b8c6 d4b5 d7d6 c2c4 g8f6 b1c3 a7a6 b5a3 d6d5 "
            + "c4d5 e6d5 e4d5 c6b4 f1e2 f8c5 e1g1 e8g8 e2f3 c8f5 c1g5 f8e8 d1d2 b7b5 a1d1 b4d3 "
            + "a3b1 h7h6 g5h4 b5b4 c3a4 c5d6 h4g3 a8c8 b2b3 g7g5 g3d6 d8d6 g2g3 f6d7 f3g2 d6f6 "
            + "a2a3 a6a5 a3b4 a5b4 d2a2 f5g6 d5d6 g5g4 a2d2 g8g7 f2f3 f6d6 f3g4 d6d4 g1h1 d7f6 "
            + "f1f4 f6e4 d2d3 e4f2 f4f2 g6d3 f2d2 d4e3 d2d3 c8c1 a4b2 e3f2 b1d2 c1d1 b2d1 e8e1"),
      game("kasparov", "Kasparov's Immortal: Kasparov vs Topalov",
          "Wijk aan Zee 1999", "1-0", 1,
          "24.Rxd4!! starts a rook sacrifice and a king hunt that drags Black's king all the way to d1.",
          "e2e4 d7d6 d2d4 g8f6 b1c3 g7g6 c1e3 f8g7 d1d2 c7c6 f2f3 b7b5 g1e2 b8d7 e3h6 g7h6 "
            + "d2h6 c8b7 a2a3 e7e5 e1c1 d8e7 c1b1 a7a6 e2c1 e8c8 c1b3 e5d4 d1d4 c6c5 d4d1 d7b6 "
            + "g2g3 c8b8 b3a5 b7a8 f1h3 d6d5 h6f4 b8a7 h1e1 d5d4 c3d5 b6d5 e4d5 e7d6 d1d4 c5d4 "
            + "e1e7 a7b6 f4d4 b6a5 b2b4 a5a4 d4c3 d6d5 e7a7 a8b7 a7b7 d5c4 c3f6 a4a3 f6a6 a3b4 "
            + "c2c3 b4c3 a6a1 c3d2 a1b2 d2d1 h3f1 d8d2 b7d7 d2d7 f1c4 b5c4 b2h8 d7d3 h8a8 c4c3 "
            + "a8a4 d1e1 f3f4 f7f5 b1c1 d3d2 a4a7"),
      game("carlsen6", "Carlsen vs Nepomniachtchi, World Championship game 6",
          "Dubai 2021", "1-0", 1,
          "The longest World Championship game ever (136 moves). Rook and knight grind down a queen: patience in a near-equal endgame.",
          "d2d4 g8f6 g1f3 d7d5 g2g3 e7e6 f1g2 f8e7 e1g1 e8g8 b2b3 c7c5 d4c5 e7c5 c2c4 d5c4 "
            + "d1c2 d8e7 b1d2 b8c6 d2c4 b7b5 c4e5 c6b4 c2b2 c8b7 a2a3 b4c6 e5d3 c5b6 c1g5 f8d8 "
            + "g5f6 g7f6 a1c1 c6d4 f3d4 b6d4 b2a2 b7g2 g1g2 e7b7 g2g1 b7e4 a2c2 a7a5 f1d1 g8g7 "
            + "d1d2 a8c8 c2c8 d8c8 c1c8 e4d5 b3b4 a5a4 e2e3 d4e5 h2h4 h7h5 g1h2 e5b2 c8c5 d5d6 "
            + "d2d1 b2a3 c5b5 d6d7 b5c5 e6e5 c5c2 d7d5 d1d2 d5b3 c2a2 e5e4 d3c5 b3b4 c5e4 b4b3 "
            + "a2c2 a3f8 e4c5 b3b5 c5d3 a4a3 d3f4 b5a5 c2a2 f8b4 d2d3 g7h6 d3d1 a5a4 d1a1 b4d6 "
            + "h2g1 a4b3 f4e2 b3d3 e2d4 h6h7 g1h2 d3e4 a2a3 e4h4 h2g1 h4e4 a3a4 d6e5 d4e2 e4c2 "
            + "a1a2 c2b3 g1g2 b3d5 f2f3 d5d1 f3f4 e5c7 g2f2 c7b6 a2a1 d1b3 a4e4 h7g7 e4e8 f6f5 "
            + "a1a8 b3b4 a8c8 b6a5 c8c1 a5b6 e8e5 b4b3 e5e8 b3d5 c1c8 d5h1 c8c1 h1d5 c1b1 b6a7 "
            + "e8e7 a7c5 e7e5 d5d3 b1b7 d3c2 b7b5 c5a7 b5a5 a7b6 a5b5 b6a7 e5f5 c2d3 f5f7 g7f7 "
            + "b5b7 f7g6 b7a7 d3d5 a7a6 g6h7 a6a1 h7g6 e2d4 d5b7 a1a2 b7h1 a2a6 g6f7 d4f3 h1b1 "
            + "a6d6 f7g7 d6d5 b1a2 d5d2 a2b1 d2e2 b1b6 e2c2 b6b1 f3d4 b1h1 c2c7 g7f6 c7c6 f6f7 "
            + "d4f3 h1b1 f3g5 f7g7 g5e6 g7f7 e6d4 b1h1 c6c7 f7f6 d4f3 h1b1 c7d7 b1b2 d7d2 b2b1 "
            + "f3g1 b1b4 d2d1 b4b3 d1d6 f6g7 d6d4 b3b2 g1e2 b2b1 e3e4 b1h1 d4d7 g7g8 d7d4 h1h2 "
            + "f2e3 h5h4 g3h4 h2h3 e3d2 h3h4 d4d3 g8f8 d3f3 h4d8 d2e3 d8a5 e3f2 a5a7 f3e3 a7d7 "
            + "e2g3 d7d2 f2f3 d2d1 e3e2 d1b3 f3g2 b3b7 e2d2 b7b3 d2d5 f8e7 d5e5 e7f7 e5f5 f7e8 "
            + "e4e5 b3a2 g2h3 a2e6 h3h4 e6h6 g3h5 h6h7 e5e6 h7g6 f5f7 e8d8 f4f5 g6g1 h5g7"));
}
