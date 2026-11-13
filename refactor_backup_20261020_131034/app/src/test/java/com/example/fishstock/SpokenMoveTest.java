package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;

import org.junit.Test;

import java.util.Collections;

/**
 * Record Game's speech parser against the real move generator: what people say (and what the
 * recogniser writes for it) must become the right legal move, and talk must not become a move.
 */
public class SpokenMoveTest {

  /** expected: a UCI move, "AMBIG" (needs a file/rank), or null (not a move here). */
  private static void check(String fen, String moves, String phrase, String expected) {
    Game game = fen == null ? new Game() : new Game(fen);
    for (String uci : moves.trim().split("\\s+")) {
      if (uci.isEmpty()) continue;
      Move m = game.findUci(uci);
      assertTrue("setup move " + uci, m != null);
      game.play(m);
    }
    SpokenMoves.Heard h = SpokenMoves.find(game.position(), Collections.singletonList(phrase));
    String got = h.move != null ? h.move.toUci() : h.options.isEmpty() ? null : "AMBIG";
    assertEquals("\"" + phrase + "\" after " + moves, expected, got);
  }

  @Test
  public void spokenMovesBecomeLegalMoves() {
    check(null, "", "e4", "e2e4");
    check(null, "", "E4", "e2e4");
    check(null, "", "e four", "e2e4");
    check(null, "", "e for", "e2e4");
    check(null, "", "pawn to e4", "e2e4");
    check(null, "", "d4", "d2d4");
    check(null, "", "before", "b2b4");
    check(null, "", "b4", "b2b4");
    check(null, "", "knight f3", "g1f3");
    check(null, "", "Knight to F3", "g1f3");
    check(null, "", "night f3", "g1f3");
    check(null, "", "Nf3", "g1f3");
    check(null, "", "knight c3", "b1c3");
    check(null, "", "knight see three", "b1c3");
    check(null, "", "a3", "a2a3");
    check(null, "", "a four", "a2a4");
    check(null, "", "g1 f3", "g1f3");
    check(null, "", "e2e4", "e2e4");
    check(null, "", "hello how are you", null);
    check(null, "", "I want to be the best", null);
    check(null, "", "e5", null);
    check(null, "", "knight e5", null);
    check(null, "e2e4 d7d5", "e takes d5", "e4d5");
    check(null, "e2e4 d7d5", "exd5", "e4d5");
    check(null, "e2e4 d7d5", "pawn takes pawn", "e4d5");
    check(null, "e2e4 d7d5", "takes", null);
    check(null, "e2e4 d7d5", "takes d5", "e4d5");
    check(null, "e2e4 d7d5 e4d5", "queen takes d5", "d8d5");
    check(null, "e2e4 d7d5 e4d5", "queen takes", "d8d5");
    check(null, "e2e4 d7d5 e4d5", "queen takes pawn", "d8d5");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "castles", "e1g1");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "castle kingside", "e1g1");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "O-O", "e1g1");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "short castle", "e1g1");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "king g1", "e1g1");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "knight g5", "f3g5");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "knight takes e5", "f3e5");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "bishop takes f7", "c4f7");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "bishop takes pawn check", "c4f7");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "Bxf7+", "c4f7");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "bishop b5", "c4b5");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "d3", "d2d3");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "d4", "d2d4");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "rook f1", "h1f1");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "knight b d7", "b8d7");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "knight bd7", "b8d7");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "nbd7", "b8d7");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "knight d7", "AMBIG");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "d takes c4", "d5c4");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "dxc4", "d5c4");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "takes c4", "d5c4");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "bishop b4", "f8b4");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "bishop before", "f8b4");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "bb4", "f8b4");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "c5", "c7c5");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "see five", "c7c5");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "knight e4", "f6e4");
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "knight takes knight", null);
    check(null, "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3 f8b4 e2e3", "bishop takes knight", "b4c3");
    check(null, "e2e4 c7c5 b2b4 g8f6 b1c3 g7g6 f1d3 f8g7 g1e2 e8g8", "bxc5", "b4c5");
    check(null, "e2e4 c7c5 b2b4 g8f6 b1c3 g7g6 f1d3 f8g7 g1e2 e8g8", "b takes c5", "b4c5");
    check(null, "e2e4 c7c5 b2b4 g8f6 b1c3 g7g6 f1d3 f8g7 g1e2 e8g8", "b takes", "b4c5");
    check("8/4P1k1/8/8/8/8/6K1/8 w - - 0 1", "", "e8", "e7e8q");
    check("8/4P1k1/8/8/8/8/6K1/8 w - - 0 1", "", "e8 queen", "e7e8q");
    check("8/4P1k1/8/8/8/8/6K1/8 w - - 0 1", "", "e8 knight", "e7e8n");
    check("8/4P1k1/8/8/8/8/6K1/8 w - - 0 1", "", "e8 promotes to a rook", "e7e8r");
    check("8/4P1k1/8/8/8/8/6K1/8 w - - 0 1", "", "e8=N", "e7e8n");
    check("8/4P1k1/8/8/8/8/6K1/8 w - - 0 1", "", "e8 equals knight check", "e7e8n");
    check("8/4P1k1/8/8/8/8/6K1/8 w - - 0 1", "", "king f3", "g2f3");
    check("8/4P1k1/8/8/8/8/6K1/8 w - - 0 1", "", "king to h3", "g2h3");
    check("4k3/8/8/8/8/R7/8/R3K3 w - - 0 1", "", "rook 1 a2", "a1a2");
    check("4k3/8/8/8/8/R7/8/R3K3 w - - 0 1", "", "rook a2", "AMBIG");
    check("4k3/8/8/8/8/R7/8/R3K3 w - - 0 1", "", "rook three a2", "a3a2");
    check("4k3/8/8/8/8/R7/8/R3K3 w - - 0 1", "", "rook d1", "a1d1");
    check("4k3/8/8/8/8/R7/8/R3K3 w - - 0 1", "", "castles queenside", null);
    check("4k3/8/8/8/8/R7/8/R3K3 w Q - 0 1", "", "castles queenside", "e1c1");
    check("4k3/8/8/8/8/R7/8/R3K3 w Q - 0 1", "", "long castle", "e1c1");
    check("4k3/8/8/8/8/R7/8/R3K3 w Q - 0 1", "", "O-O-O", "e1c1");
    check("4k3/8/8/8/8/R7/8/R3K3 w Q - 0 1", "", "castles kingside", null);
    check("8/4P1k1/8/8/8/8/6K1/8 w - - 0 1", "", "e8=B", "e7e8b");
    check("8/4P1k1/8/8/8/8/6K1/8 w - - 0 1", "", "e8 bishop", "e7e8b");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5c6", "d takes c6", "d7c6");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5c6", "b takes c6", "b7c6");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5c6", "takes c6", "AMBIG");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5c6", "bxc6", "b7c6");
    check(null, "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5c6", "pawn takes bishop", "AMBIG");
    check(null, "e2e4 d7d5 e4e5 f7f5", "e takes f6", "e5f6");
    check(null, "e2e4 d7d5 e4e5 f7f5", "en passant", null);
    check(null, "e2e4 d7d5 e4e5 f7f5", "Undo", null);
    check(null, "", "a takes b5", null);
    check(null, "", "to be or not to be", null);
    check(null, "", "see you later", null);
    check(null, "", "go for it", null);
  }

  @Test
  public void theRecognisersSecondGuessIsUsedWhenTheFirstIsntAMove() {
    Game game = new Game();
    SpokenMoves.Heard h = SpokenMoves.find(game.position(), java.util.Arrays.asList("night of three", "knight f3"));
    assertEquals("g1f3", h.move.toUci());
  }

  @Test
  public void undoIsACommand() {
    assertEquals(SpokenMoveParser.Command.UNDO, SpokenMoveParser.command("undo"));
    assertEquals(SpokenMoveParser.Command.UNDO, SpokenMoveParser.command("take back"));
    assertEquals(SpokenMoveParser.Command.NONE, SpokenMoveParser.command("knight f3"));
  }

  @Test
  public void movesAreReadBackInWords() {
    assertEquals("knight takes F3, check", SpokenMoveParser.sanToWords("Nxf3+"));
    assertEquals("E takes D5", SpokenMoveParser.sanToWords("exd5"));
    assertEquals("castles queenside", SpokenMoveParser.sanToWords("O-O-O"));
    assertEquals("E8, promotes to queen, checkmate", SpokenMoveParser.sanToWords("e8=Q#"));
  }

  @Test
  public void thePositionIsDescribedPieceByPiece() {
    java.util.List<String> s = SpokenMoves.describe(new Game().position());
    assertEquals("White to move.", s.get(0));
    assertEquals("White pieces.", s.get(1));
    assertEquals("King on E1.", s.get(2));
    assertEquals("Rooks on A1 and H1.", s.get(4));
    assertEquals("Pawns on A2, B2, C2, D2, E2, F2, G2 and H2.", s.get(7));
    assertEquals("Black pieces.", s.get(8));
  }
}
