package com.example.fishstock.agents;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Square;
import com.example.fishstock.eval.Evaluator;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.ScoredMove;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Hard-coded openings, chosen to match an agent's temperament ({@link PlayStyle#aggression}):
 *
 * | aggression   | as White                                 | as Black                          |
 * |--------------|------------------------------------------|-----------------------------------|
 * | 0.8 and up   | King's Gambit (Smith-Morra vs Sicilian)  | Najdorf / Sveshnikov, King's Indian|
 * | 0.4 to 0.8   | Evans Gambit and the Fried Liver         | Two Knights, Schliemann, Albin    |
 * | 0.1 to 0.4   | Italian (Giuoco Piano)                   | Italian, Queen's Gambit Declined  |
 * | -0.1 to 0.1  | main lines: Ruy Lopez, Open Sicilian ... | main lines: Ruy Lopez, QGD, Nimzo |
 * | below -0.1   | kingside fianchetto (King's Indian Attack)| Caro-Kann, King's Indian setup   |
 *
 * Simple, MinMax and FishStock (aggression 0) play the main lines for as long as the opponent
 * follows them. The book is looked up by position, so transpositions are found too.
 *
 * Book lines are played without question (a gambit "loses" a pawn by the evaluation, which is
 * the point). The low-aggression book also has a "system": set-up moves (Nf3, g3, Bg2, O-O, d3...)
 * that are played in the first ten moves after the book runs out, but only if a quick 2-ply check
 * says they are within 0.6 pawns of the best move.
 *
 * To add an opening, add a line below: "W" lines are played by the agent as White, "B" lines as
 * Black. Moves are in normal notation. {@code OpeningBookTest} checks that every line is legal.
 */
public final class OpeningBook {
  public enum Repertoire {
    KINGS_GAMBIT("King's Gambit"),
    ATTACKING_ITALIAN("Evans Gambit and Fried Liver"),
    ITALIAN("Italian"),
    MAIN_LINES("Main lines"),
    FIANCHETTO("Kingside fianchetto");

    public final String label;

    Repertoire(String label) {
      this.label = label;
    }
  }

  // ---------------------------------------------------------------- the lines

  private static final String[] SMITH_MORRA_AND_ADVANCE = {
      "W e4 c5 d4 cxd4 c3 dxc3 Nxc3 Nc6 Nf3 d6 Bc4 e6 O-O Nf6 Qe2 Be7 Rd1",
      "W e4 c5 d4 cxd4 c3 Nf6 e5 Nd5 Nf3 Nc6 Bc4",
      "W e4 c5 d4 cxd4 c3 d3 Bxd3 Nc6 Nf3",
      "W e4 e6 d4 d5 e5 c5 c3 Nc6 Nf3 Qb6 a3",
      "W e4 c6 d4 d5 e5 Bf5 Nc3 e6 g4 Bg6 Nge2",
      "W e4 d5 exd5 Qxd5 Nc3 Qa5 d4 Nf6 Nf3 c6 Bc4",
      "W e4 Nf6 e5 Nd5 d4 d6 c4 Nb6 f4",
      "W e4 d6 d4 Nf6 Nc3 g6 f4 Bg7 Nf3",
  };

  private static final String[] KINGS_GAMBIT = {
      "W e4 e5 f4 exf4 Nf3 g5 h4 g4 Ne5 Nf6 Bc4 d5 exd5 Bd6 d4",
      "W e4 e5 f4 exf4 Nf3 d5 exd5 Nf6 Bb5+ c6 dxc6 bxc6 Bc4 Nd5",
      "W e4 e5 f4 exf4 Nf3 Nf6 e5 Nh5 d4 d6 Qe2",
      "W e4 e5 f4 exf4 Nf3 d6 d4 g5 h4 g4 Ng1",
      "W e4 e5 f4 Bc5 Nf3 d6 c3 Nf6 d4 exd4 cxd4 Bb4+ Bd2 Bxd2+ Nbxd2",
      "W e4 e5 f4 d5 exd5 exf4 Nf3 Nf6 Bc4",
      "W e4 e5 f4 Nc6 Nf3 exf4 d4",
      "B e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Be3 e5 Nb3 Be6 f3 Be7 Qd2 O-O O-O-O Nbd7 g4 b5",
      "B e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Bg5 e6 f4 Qb6",
      "B e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 e5 Ndb5 d6 Bg5 a6 Na3 b5",
      "B e4 c5 Nc3 Nc6 g3 g6 Bg2 Bg7 d3 d6",
      "B e4 c5 c3 Nf6 e5 Nd5 d4 cxd4 Nf3",
      "B d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e5 O-O Nc6 d5 Ne7 Ne1 Nd7 f3 f5",
      "B d4 Nf6 Nf3 g6 c4 Bg7 Nc3 O-O e4 d6",
      "B d4 Nf6 c4 g6 g3 Bg7 Bg2 O-O Nf3 d6 O-O",
      "B c4 e5 Nc3 Nf6 g3 d5 cxd5 Nxd5 Bg2 Nb6",
      "B Nf3 Nf6 c4 g6 Nc3 Bg7 e4 d6 d4 O-O",
  };

  private static final String[] ATTACKING_ITALIAN = {
      "W e4 e5 Nf3 Nc6 Bc4 Bc5 b4 Bxb4 c3 Ba5 d4 exd4 O-O Nge7 cxd4 d5 exd5 Nxd5 Ba3",
      "W e4 e5 Nf3 Nc6 Bc4 Bc5 b4 Bxb4 c3 Be7 d4 Na5 Be2 exd4 Qxd4",
      "W e4 e5 Nf3 Nc6 Bc4 Bc5 b4 Bb6 a4 a6 Nc3 Nf6 Nd5",
      "W e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 d5 exd5 Nxd5 Nxf7 Kxf7 Qf3+ Ke6 Nc3 Nb4 a3 Nxc2+ Kd1 Nxa1 Nxd5",
      "W e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 d5 exd5 Na5 Bb5+ c6 dxc6 bxc6 Be2 h6 Nf3 e4 Ne5 Bd6 d4",
      "W e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 d5 exd5 Nd4 c3 b5 Bf1 Nxd5 cxd4 Qxg5 Bxb5+ Kd8",
      "W e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 Bc5 Nxf7 Bxf2+ Kxf2 Nxe4+ Kg1",
      "W e4 e5 Nf3 Nc6 Bc4 Be7 d4 d6 d5 Nb8",
      "W e4 e5 Nf3 d6 d4 exd4 Nxd4 Nf6 Nc3 Be7 Bf4",
      "W e4 e5 Nf3 Nf6 Nxe5 d6 Nf3 Nxe4 d4 d5 Bd3",
      "B e4 e5 Nf3 Nc6 Bc4 Nf6 d3 Be7 O-O O-O",
      "B e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 d5 exd5 Na5 Bb5+ c6 dxc6 bxc6 Be2 h6",
      "B e4 e5 Nf3 Nc6 Bb5 f5 Nc3 fxe4 Nxe4 d5 Nxe5 dxe4 Nxc6 Qg5",
      "B e4 e5 Nf3 Nc6 d4 exd4 Nxd4 Nf6 Nxc6 bxc6 e5 Qe7",
      "B e4 e5 Nc3 Nf6 f4 d5 fxe5 Nxe4",
      "B d4 d5 c4 e5 dxe5 d4 Nf3 Nc6 g3 Be6",
      "B d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O",
      "B c4 e5 Nc3 Nf6 g3 d5 cxd5 Nxd5 Bg2 Nb6",
      "B Nf3 d5 g3 Bg4 Bg2 Nd7",
  };

  private static final String[] ITALIAN = {
      "W e4 e5 Nf3 Nc6 Bc4 Bc5 c3 Nf6 d3 d6 O-O O-O Re1 a6 Bb3 Ba7 h3 h6 Nbd2",
      "W e4 e5 Nf3 Nc6 Bc4 Nf6 d3 Be7 O-O O-O Re1 d6 c3 Na5 Bb5 a6 Ba4 b5 Bc2",
      "W e4 e5 Nf3 Nc6 Bc4 Nf6 d3 Bc5 c3 d6 O-O O-O",
      "W e4 e5 Nf3 Nc6 Bc4 Be7 d4 exd4 Nxd4",
      "W e4 e5 Nf3 d6 d4 exd4 Nxd4 Nf6 Nc3 Be7",
      "W e4 e5 Nf3 Nf6 Nxe5 d6 Nf3 Nxe4 d4 d5 Bd3",
      "W e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Be2 e5 Nb3 Be7 O-O O-O",
      "W e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 e5 Ndb5 d6",
      "W e4 e6 d4 d5 Nc3 Nf6 Bg5 Be7 e5 Nfd7 Bxe7 Qxe7 f4",
      "W e4 c6 d4 d5 Nc3 dxe4 Nxe4 Bf5 Ng3 Bg6 h4 h6 Nf3 Nd7 h5 Bh7 Bd3 Bxd3 Qxd3",
      "B e4 e5 Nf3 Nc6 Bc4 Bc5 c3 Nf6 d3 d6 O-O O-O",
      "B e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b5 Bb3 d6 c3 O-O h3",
      "B e4 e5 Nf3 Nc6 d4 exd4 Nxd4 Bc5",
      "B d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 h6 Bh4 b6",
      "B d4 d5 c4 e6 Nf3 Nf6 Nc3 Be7 Bf4 O-O e3 c5",
      "B d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O",
      "B c4 e5 Nc3 Nf6 Nf3 Nc6 g3 d5 cxd5 Nxd5 Bg2 Nb6",
      "B Nf3 d5 d4 Nf6 c4 e6 Nc3 Be7",
  };

  private static final String[] MAIN_LINES = {
      // Ruy Lopez: Closed, Breyer, Chigorin, Marshall, Berlin, Open
      "W e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b5 Bb3 d6 c3 O-O h3 Nb8 d4 Nbd7 Nbd2 Bb7 Bc2 Re8 Nf1 Bf8 Ng3 g6",
      "W e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b5 Bb3 d6 c3 O-O h3 Na5 Bc2 c5 d4 Qc7 Nbd2",
      "W e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b5 Bb3 O-O c3 d5 exd5 Nxd5 Nxe5 Nxe5 Rxe5 c6 d4 Bd6 Re1 Qh4 g3 Qh3",
      "W e4 e5 Nf3 Nc6 Bb5 Nf6 O-O Nxe4 d4 Nd6 Bxc6 dxc6 dxe5 Nf5 Qxd8+ Kxd8",
      "W e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Nxe4 d4 b5 Bb3 d5 dxe5 Be6",
      "W e4 e5 Nf3 Nc6 Bb5 a6 Bxc6 dxc6 O-O f6 d4",
      "W e4 e5 Nf3 Nc6 Bb5 d6 d4 Bd7 Nc3 Nf6 O-O Be7",
      "W e4 e5 Nf3 Nc6 Bb5 Bc5 c3 Nf6 d4",
      "W e4 e5 Nf3 Nf6 Nxe5 d6 Nf3 Nxe4 d4 d5 Bd3 Nc6 O-O Be7 c4 Nb4 Be2 O-O Nc3",
      "W e4 e5 Nf3 d6 d4 exd4 Nxd4 Nf6 Nc3 Be7 Be2 O-O O-O",
      // Open Sicilian: Najdorf, Sveshnikov, Taimanov, Dragon
      "W e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Be3 e5 Nb3 Be6 f3 Be7 Qd2 O-O O-O-O Nbd7 g4 b5 g5 b4",
      "W e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 e5 Ndb5 d6 Bg5 a6 Na3 b5 Bxf6 gxf6 Nd5 f5",
      "W e4 c5 Nf3 e6 d4 cxd4 Nxd4 Nc6 Nc3 Qc7 Be3 a6 Qd2 Nf6 O-O-O",
      "W e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Be3 Bg7 f3 O-O Qd2 Nc6 Bc4",
      // French, Caro-Kann, Scandinavian, Alekhine, Pirc, Modern
      "W e4 e6 d4 d5 Nc3 Nf6 Bg5 Be7 e5 Nfd7 Bxe7 Qxe7 f4 O-O Nf3 c5",
      "W e4 e6 d4 d5 Nc3 Bb4 e5 c5 a3 Bxc3+ bxc3 Ne7 Qg4 Qc7",
      "W e4 e6 d4 d5 Nc3 dxe4 Nxe4 Nd7 Nf3 Ngf6",
      "W e4 c6 d4 d5 Nc3 dxe4 Nxe4 Bf5 Ng3 Bg6 h4 h6 Nf3 Nd7 h5 Bh7 Bd3 Bxd3 Qxd3 e6 Bd2 Ngf6 O-O-O",
      "W e4 d5 exd5 Qxd5 Nc3 Qa5 d4 Nf6 Nf3 c6 Bc4 Bf5 Bd2 e6",
      "W e4 d5 exd5 Nf6 d4 Nxd5 Nf3",
      "W e4 Nf6 e5 Nd5 d4 d6 Nf3 Bg4 Be2 e6 O-O Be7",
      "W e4 d6 d4 Nf6 Nc3 g6 Be3 Bg7 Qd2 c6",
      "W e4 g6 d4 Bg7 Nc3 d6 Be3",
      "W e4 Nc6 d4 d5 Nc3",
      // As Black: 1.e4 e5 main lines
      "B e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b5 Bb3 d6 c3 O-O h3 Nb8 d4 Nbd7",
      "B e4 e5 Nf3 Nc6 Bc4 Bc5 c3 Nf6 d3 d6 O-O O-O",
      "B e4 e5 Nf3 Nc6 d4 exd4 Nxd4 Nf6 Nxc6 bxc6 e5 Qe7",
      "B e4 e5 Nf3 Nc6 Nc3 Nf6 d4 exd4 Nxd4 Bb4",
      "B e4 e5 Nc3 Nf6 Nf3 Nc6",
      "B e4 e5 f4 exf4 Nf3 d6 d4 g5",
      "B e4 e5 d4 exd4 Qxd4 Nc6 Qe3 Nf6",
      "B e4 e5 Bc4 Nf6 d3 c6",
      // As Black against 1.d4, 1.c4, 1.Nf3 and the rest
      "B d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 h6 Bh4 b6 cxd5 Nxd5 Bxe7 Qxe7 Nxd5 exd5",
      "B d4 d5 c4 e6 Nf3 Nf6 Nc3 Be7 Bf4 O-O e3 c5 dxc5 Bxc5",
      "B d4 d5 c4 e6 Nc3 Nf6 cxd5 exd5 Bg5 Be7 e3 O-O Bd3 Nbd7",
      "B d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Bd3 d5 Nf3 c5 O-O",
      "B d4 Nf6 c4 e6 Nf3 d5 Nc3 Be7",
      "B d4 Nf6 c4 e6 g3 d5 Bg2 Be7 Nf3 O-O",
      "B d4 Nf6 Nf3 d5 c4 e6 Nc3 Be7",
      "B d4 d5 Nf3 Nf6 c4 e6 Nc3 Be7 Bg5 O-O",
      "B d4 d5 Bf4 Nf6 e3 c5 c3 Nc6 Nd2",
      "B d4 d5 e3 Nf6 Bd3 c5 c3 Nc6",
      "B c4 e5 Nc3 Nf6 Nf3 Nc6 g3 d5 cxd5 Nxd5 Bg2 Nb6 O-O Be7",
      "B Nf3 d5 d4 Nf6 c4 e6 Nc3 Be7 Bg5 O-O",
      "B Nf3 d5 g3 Nf6 Bg2 e6 O-O Be7 d3 O-O",
      "B f4 d5 Nf3 Nf6 e3 g6",
      "B b3 e5 Bb2 Nc6 e3 d5",
      "B g3 d5 Bg2 Nf6 Nf3",
      "B Nc3 d5 e4 d4",
      "B e3 e5 d4 exd4 exd4 d5",
      "B d3 e5 e4 Nf6",
      "B a3 e5 e4 Nf6",
      "B h3 e5 e4 Nf6",
      "B a4 e5 e4 Nf6",
      "B h4 e5 e4 Nf6",
      "B g4 d5 Bg2 Bxg4 c4 c6",
      "B b4 e5 Bb2 Bxb4 Bxe5 Nf6",
      "B c3 e5 d4 exd4 cxd4 d5",
      "B f3 e5 e4 Nf6",
      "B Na3 e5 e4 Nf6",
      "B Nh3 d5 d4 Nf6",
  };

  private static final String[] FIANCHETTO = {
      "W Nf3 d5 g3 Nf6 Bg2 e6 O-O Be7 d3 O-O Nbd2 c5 e4 Nc6 Re1",
      "W Nf3 d5 g3 Nf6 Bg2 c6 O-O Bg4 d3 Nbd7 Nbd2 e5 e4",
      "W Nf3 d5 g3 c5 Bg2 Nc6 O-O e5 d3 Nf6 Nbd2 Be7 e4",
      "W Nf3 Nf6 g3 g6 Bg2 Bg7 O-O O-O d3 d6 e4 e5 Nc3",
      "W Nf3 c5 g3 Nc6 Bg2 g6 O-O Bg7 c3 Nf6 d4",
      "W Nf3 e6 g3 d5 Bg2 Nf6 O-O Be7 d3 O-O Nbd2 c5 e4",
      "W Nf3 d6 g3 e5 d3 Nf6 Bg2 Be7 O-O O-O e4",
      "B e4 c6 d4 d5 Nc3 dxe4 Nxe4 Bf5 Ng3 Bg6 h4 h6 Nf3 Nd7 h5 Bh7 Bd3 Bxd3 Qxd3 e6",
      "B e4 c6 d4 d5 e5 Bf5 Nf3 e6 Be2 c5 Be3",
      "B e4 c6 d4 d5 exd5 cxd5 Bd3 Nc6 c3 Nf6 Bf4 Bg4",
      "B e4 c6 Nf3 d5 Nc3 Bg4 h3 Bxf3 Qxf3 e6",
      "B e4 c6 d4 d5 Nd2 dxe4 Nxe4 Bf5 Ng3 Bg6",
      "B e4 c6 c4 d5 exd5 cxd5 cxd5 Nf6",
      "B e4 c6 d4 d5 Nc3 dxe4 Nxe4 Bf5 Ng3 Bg6 Nf3 Nd7",
      "B d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e5 O-O Nc6",
      "B d4 Nf6 Nf3 g6 g3 Bg7 Bg2 O-O O-O d6",
      "B d4 Nf6 c4 g6 g3 Bg7 Bg2 O-O Nc3 d6 Nf3",
      "B d4 Nf6 Bf4 g6 e3 Bg7 Nf3 O-O Be2 d6",
      "B d4 Nf6 c4 g6 Nf3 Bg7 Nc3 O-O",
      "B c4 Nf6 Nc3 g6 g3 Bg7 Bg2 O-O",
      "B Nf3 Nf6 g3 g6 Bg2 Bg7 O-O O-O",
  };

  /** Set-up moves for the fianchetto repertoire once the book runs out (checked before playing). */
  private static final String[] FIANCHETTO_SYSTEM_WHITE = {"Nf3", "g3", "Bg2", "O-O", "d3", "Nbd2", "e4", "Re1"};
  private static final String[] FIANCHETTO_SYSTEM_BLACK = {"Nf6", "g6", "Bg7", "O-O", "d6"};
  private static final int SYSTEM_MOVE_LIMIT = 10;
  private static final double SYSTEM_TOLERANCE = 0.6;

  private static final Map<Repertoire, OpeningBook> CACHE = new EnumMap<>(Repertoire.class);

  // ---------------------------------------------------------------- book

  public final Repertoire repertoire;
  /** Position hash -> book moves for the side to move (repeats = more likely). */
  private final Map<Long, List<Move>> whiteMoves = new HashMap<>();
  private final Map<Long, List<Move>> blackMoves = new HashMap<>();
  private final List<String> badLines = new ArrayList<>();
  private final String[] systemWhite;
  private final String[] systemBlack;

  private OpeningBook(Repertoire repertoire, String[] systemWhite, String[] systemBlack, String[]... lineSets) {
    this.repertoire = repertoire;
    this.systemWhite = systemWhite;
    this.systemBlack = systemBlack;
    for (String[] set : lineSets) for (String line : set) add(line);
  }

  /** The repertoire for an aggression level (see the class comment). */
  public static Repertoire repertoireFor(double aggression) {
    if (aggression >= 0.8) return Repertoire.KINGS_GAMBIT;
    if (aggression >= 0.4) return Repertoire.ATTACKING_ITALIAN;
    if (aggression >= 0.1) return Repertoire.ITALIAN;
    if (aggression > -0.1) return Repertoire.MAIN_LINES;
    return Repertoire.FIANCHETTO;
  }

  public static OpeningBook forAggression(double aggression) {
    return of(repertoireFor(aggression));
  }

  public static synchronized OpeningBook of(Repertoire r) {
    OpeningBook b = CACHE.get(r);
    if (b != null) return b;
    switch (r) {
      case KINGS_GAMBIT:
        b = new OpeningBook(r, null, null, KINGS_GAMBIT, SMITH_MORRA_AND_ADVANCE, mainLinesBlackOnly());
        break;
      case ATTACKING_ITALIAN:
        b = new OpeningBook(r, null, null, ATTACKING_ITALIAN, SMITH_MORRA_AND_ADVANCE, mainLinesBlackOnly());
        break;
      case ITALIAN:
        b = new OpeningBook(r, null, null, ITALIAN, mainLinesBlackOnly());
        break;
      case FIANCHETTO:
        b = new OpeningBook(r, FIANCHETTO_SYSTEM_WHITE, FIANCHETTO_SYSTEM_BLACK, FIANCHETTO);
        break;
      case MAIN_LINES:
      default:
        b = new OpeningBook(r, null, null, MAIN_LINES);
    }
    CACHE.put(r, b);
    return b;
  }

  /** Main-line answers as Black, used to fill gaps in the more specialised repertoires. */
  private static String[] mainLinesBlackOnly() {
    List<String> out = new ArrayList<>();
    for (String l : MAIN_LINES) if (l.startsWith("B ")) out.add(l);
    return out.toArray(new String[0]);
  }

  /** Every line in every repertoire (for tests). */
  public static List<String> allLines() {
    List<String> out = new ArrayList<>();
    for (String[] set : new String[][] {SMITH_MORRA_AND_ADVANCE, KINGS_GAMBIT, ATTACKING_ITALIAN, ITALIAN, MAIN_LINES, FIANCHETTO}) {
      for (String l : set) out.add(l);
    }
    return out;
  }

  /** Lines that contained a move that isn't legal (should always be empty). */
  public List<String> badLines() {
    return badLines;
  }

  private void add(String line) {
    String[] tokens = line.trim().split("\\s+");
    boolean forWhite = tokens[0].equals("W");
    Position p = Position.startingPosition();
    for (int i = 1; i < tokens.length; i++) {
      Move m = Notation.fromSan(p, tokens[i]);
      if (m == null) {
        badLines.add(line + "  (at " + tokens[i] + ")");
        return;
      }
      if (p.whiteToMove() == forWhite) {
        Map<Long, List<Move>> map = forWhite ? whiteMoves : blackMoves;
        List<Move> list = map.get(p.hash());
        if (list == null) {
          list = new ArrayList<>();
          map.put(p.hash(), list);
        }
        list.add(m);
      }
      p.makeMove(m);
    }
  }

  /** A book move for the side to move, or null when out of book. */
  public Move bookMove(Position pos, Random random) {
    Map<Long, List<Move>> map = pos.whiteToMove() ? whiteMoves : blackMoves;
    List<Move> options = map.get(pos.hash());
    if (options == null || options.isEmpty()) return null;
    Move pick = options.get(random.nextInt(options.size()));
    // Hash collisions are astronomically unlikely, but never play an illegal move.
    Move legal = Notation.fromSan(pos, Notation.toSan(pos, pick));
    return legal != null && legal.sameAs(pick) ? legal : null;
  }

  /**
   * Book move, or (fianchetto repertoire only) the next set-up move if it is legal, not already
   * played, and a quick check says it is within {@link #SYSTEM_TOLERANCE} of the best move.
   */
  public Move choose(Position pos, List<Long> history, Evaluator evaluator, Random random) {
    Move m = bookMove(pos, random);
    if (m != null) return m;
    String[] system = pos.whiteToMove() ? systemWhite : systemBlack;
    if (system == null || pos.fullmoveNumber() > SYSTEM_MOVE_LIMIT || evaluator == null) return null;
    List<ScoredMove> scored = null;
    for (String san : system) {
      Move candidate = Notation.fromSan(pos, san);
      if (candidate == null || alreadyDone(pos, san)) continue;
      if (scored == null) scored = new AlphaBetaSearch(evaluator).scoreAllMoves(pos, history, 2, 0, null);
      if (scored.isEmpty()) return null;
      double best = scored.get(0).score;
      for (ScoredMove s : scored) {
        if (s.move.sameAs(candidate)) return s.score >= best - SYSTEM_TOLERANCE ? candidate : null;
      }
      return null;
    }
    return null;
  }

  /** "Bg2" is done if our bishop already stands on g2 (or g7 for Black's "Bg7"), and so on. */
  private static boolean alreadyDone(Position pos, String san) {
    boolean white = pos.whiteToMove();
    if (san.startsWith("O-O")) return false; // only legal while not yet castled
    String target = san.replaceAll("[^a-h1-8]", "");
    if (target.length() < 2) return false;
    int sq = Square.parse(target.substring(target.length() - 2));
    int type = Character.isUpperCase(san.charAt(0)) ? Piece.fromFenChar(san.charAt(0)) : Piece.PAWN;
    return pos.pieceAt(sq) == Piece.make(Piece.type(type), white);
  }
}
