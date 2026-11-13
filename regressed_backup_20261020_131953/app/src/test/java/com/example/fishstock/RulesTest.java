package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.GameResult;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Rules;
import com.example.fishstock.engine.Square;

import org.junit.Test;

import java.util.List;

/** The rules of chess. Perft counts are the standard published node counts. */
public class RulesTest {

  private static long perft(Position p, int depth) {
    if (depth == 0) return 1;
    List<Move> moves = MoveGenerator.legalMoves(p);
    if (depth == 1) return moves.size();
    long n = 0;
    for (Move m : moves) {
      p.makeMove(m);
      n += perft(p, depth - 1);
      p.unmakeMove(m);
    }
    return n;
  }

  @Test
  public void perftStartingPosition() {
    assertEquals(8902, perft(Position.startingPosition(), 3));
    assertEquals(197281, perft(Position.startingPosition(), 4));
  }

  @Test
  public void perftKiwipeteCastlingEnPassantPromotion() {
    Position p = Position.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
    assertEquals(97862, perft(p, 3));
  }

  @Test
  public void perftEndgameAndPromotionPositions() {
    assertEquals(43238, perft(Position.fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"), 4));
    assertEquals(9467, perft(Position.fromFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"), 3));
    assertEquals(62379, perft(Position.fromFen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"), 3));
  }

  @Test
  public void makeUnmakeRestoresEverything() {
    Position p = Position.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
    String fen = p.toFen();
    long hash = p.hash();
    for (Move m : MoveGenerator.legalMoves(p)) {
      p.makeMove(m);
      p.unmakeMove(m);
      assertEquals(fen, p.toFen());
      assertEquals(hash, p.hash());
    }
  }

  @Test
  public void foolsMateIsCheckmate() {
    Game g = new Game();
    for (String uci : new String[] {"f2f3", "e7e5", "g2g4", "d8h4"}) g.play(g.findUci(uci));
    assertEquals(GameResult.BLACK_CHECKMATES, g.result());
    assertTrue(g.legalMoves().isEmpty());
  }

  @Test
  public void stalemateIsDetected() {
    Game g = new Game("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
    assertEquals(GameResult.STALEMATE, g.result());
  }

  @Test
  public void threefoldRepetition() {
    Game g = new Game();
    String[] shuffle = {"g1f3", "g8f6", "f3g1", "f6g8"};
    for (int i = 0; i < 2; i++) for (String uci : shuffle) g.play(g.findUci(uci));
    assertEquals(GameResult.THREEFOLD_REPETITION, g.result());
  }

  @Test
  public void fiftyMoveRule() {
    Game g = new Game("4k3/8/8/8/8/8/8/R3K3 w - - 99 80");
    g.play(g.findUci("a1a2"));
    assertEquals(GameResult.FIFTY_MOVE_RULE, g.result());
  }

  @Test
  public void insufficientMaterial() {
    assertTrue(Rules.isInsufficientMaterial(Position.fromFen("4k3/8/8/8/8/8/8/4KN2 w - - 0 1")));
    assertTrue(Rules.isInsufficientMaterial(Position.fromFen("4kb2/8/8/8/8/8/8/2B1K3 w - - 0 1")));
    assertFalse(Rules.isInsufficientMaterial(Position.fromFen("4k3/8/8/8/8/8/8/4KR2 w - - 0 1")));
    assertFalse(Rules.isInsufficientMaterial(Position.fromFen("4k3/8/8/8/8/8/8/3NKN2 w - - 0 1")));
  }

  @Test
  public void cannotCastleThroughCheck() {
    // Black rook on f8 covers f1.
    Game g = new Game("4kr2/8/8/8/8/8/8/4K2R w K - 0 1");
    assertNull(g.findUci("e1g1"));
    Game ok = new Game("4k3/8/8/8/8/8/8/4K2R w K - 0 1");
    Move castle = ok.findUci("e1g1");
    assertNotNull(castle);
    ok.play(castle);
    assertEquals(Piece.ROOK, ok.position().pieceAt(Square.parse("f1")));
  }

  @Test
  public void enPassantCapture() {
    Game g = new Game("4k3/3p4/8/4P3/8/8/8/4K3 b - - 0 1");
    g.play(g.findUci("d7d5"));
    Move ep = g.findUci("e5d6");
    assertNotNull(ep);
    assertTrue(ep.isEnPassant());
    g.play(ep);
    assertEquals(Piece.EMPTY, g.position().pieceAt(Square.parse("d5")));
  }

  @Test
  public void pinnedPieceCannotMove() {
    // Knight on e2 is pinned by the rook on e8.
    Game g = new Game("4r1k1/8/8/8/8/8/4N3/4K3 w - - 0 1");
    assertTrue(MoveGenerator.legalMovesFrom(g.position(), Square.parse("e2")).isEmpty());
  }

  @Test
  public void promotionNeedsChoiceAndCanCheckmate() {
    Game g = new Game("7k/4P3/6K1/8/8/8/8/8 w - - 0 1");
    assertTrue(g.isPromotionMove(Square.parse("e7"), Square.parse("e8")));
    g.play(g.findMove(Square.parse("e7"), Square.parse("e8"), Piece.QUEEN));
    assertEquals(GameResult.WHITE_CHECKMATES, g.result());
  }

  @Test
  public void undoRestoresGame() {
    Game g = new Game();
    g.play(g.findUci("e2e4"));
    g.play(g.findUci("e7e5"));
    assertTrue(g.undo());
    assertTrue(g.undo());
    assertEquals(Position.START_FEN, g.position().toFen());
    assertFalse(g.undo());
  }

  @Test
  public void capturedPiecesFollowTheMoveList() {
    Game g = new Game();
    for (String uci : new String[] {"e2e4", "d7d5", "e4d5", "d8d5"}) g.play(g.findUci(uci));
    assertEquals(1, g.capturedCounts(4)[0][Piece.PAWN]);
    assertEquals(1, g.capturedCounts(4)[1][Piece.PAWN]);
    assertEquals(1, g.capturedCounts(3)[0][Piece.PAWN]);
    assertEquals(0, g.capturedCounts(3)[1][Piece.PAWN]);
  }

  @Test
  public void algebraicNotation() {
    Game g = new Game();
    for (String uci : new String[] {"e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6", "e1g1"}) {
      g.play(g.findUci(uci));
    }
    assertEquals("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Bxc6 dxc6 5. O-O", g.toSanString());
    Position p = Position.fromFen("7k/8/6K1/8/8/8/8/R7 w - - 0 1");
    Move mate = null;
    for (Move m : MoveGenerator.legalMoves(p)) if (m.toUci().equals("a1a8")) mate = m;
    assertEquals("Ra8#", Notation.toSan(p, mate));
  }
}
