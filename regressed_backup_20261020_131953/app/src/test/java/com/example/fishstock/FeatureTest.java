package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Square;
import com.example.fishstock.eval.BoardAnalysis;
import com.example.fishstock.eval.EvalWeights;

import org.junit.Test;

/** One test per feature detector, so each idea in the evaluation can be checked on its own. */
public class FeatureTest {

  private static BoardAnalysis analyse(String fen) {
    return new BoardAnalysis(Position.fromFen(fen), EvalWeights.defaults());
  }

  private static int sq(String name) {
    return Square.parse(name);
  }

  // ---- 1. attackers / defenders

  @Test
  public void exchangeLossOfHangingAndDefendedPieces() {
    BoardAnalysis a = analyse("4k3/8/3p4/4n3/8/5N2/8/4K3 w - - 0 1");
    assertEquals(0.0, a.exchangeLoss(sq("e5")), 1e-9);        // knight defended by a pawn
    assertEquals(3.05, a.exchangeLoss(sq("f3")), 1e-9);       // white knight undefended, attacked by Ne5
    assertTrue(a.isHanging(sq("f3")));
    assertFalse(a.isHanging(sq("e5")));
  }

  @Test
  public void exchangeLossWhenAttackedByCheaperPiece() {
    // Queen defended once but attacked by a pawn: loses queen for pawn.
    BoardAnalysis a = analyse("4k3/8/8/4p3/3Q4/8/8/3RK3 w - - 0 1");
    assertEquals(9.5 - 1.0, a.exchangeLoss(sq("d4")), 1e-9);
  }

  @Test
  public void attackerAndDefenderCounts() {
    BoardAnalysis a = analyse("4k3/8/8/3p4/4P3/5N2/8/4K3 w - - 0 1");
    assertEquals(1, a.attackers(sq("d5"), true));   // e4 pawn
    assertEquals(1, a.attackers(sq("e4"), false));  // d5 pawn
    assertFalse(a.isDefended(sq("e4")));
  }

  // ---- 2. mobility

  @Test
  public void knightInCornerHasLessMobilityThanInCentre() {
    BoardAnalysis corner = analyse("4k3/8/8/8/8/8/8/N3K3 w - - 0 1");
    BoardAnalysis centre = analyse("4k3/8/8/8/3N4/8/8/4K3 w - - 0 1");
    assertEquals(2, corner.mobility(sq("a1")));
    assertEquals(8, centre.mobility(sq("d4")));
    assertTrue(BoardAnalysis.isInCorner(sq("a1")));
    assertTrue(BoardAnalysis.isCentral(sq("d4")));
  }

  // ---- 3. pins and reveal checkers

  @Test
  public void pinToKingAndToQueen() {
    BoardAnalysis a = analyse("r1bqkbnr/ppp2ppp/2np4/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 4");
    assertTrue(a.isPinnedToKing(sq("c6")));
    BoardAnalysis q = analyse("3qk3/8/8/3n4/8/8/8/3RK3 w - - 0 1");
    assertTrue(q.isPinnedToQueen(sq("d5")));
    assertFalse(q.isPinnedToKing(sq("d5")));
  }

  @Test
  public void revealChecker() {
    // White bishop on d3 stands between the white rook on d1 and the black king on d8.
    BoardAnalysis a = analyse("3k4/8/8/8/8/3B4/8/3RK3 w - - 0 1");
    assertTrue(a.isRevealChecker(sq("d3")));
  }

  // ---- 4. king safety

  @Test
  public void kingShelterAndOpenFiles() {
    BoardAnalysis castled = analyse("4k3/8/8/8/8/8/5PPP/6K1 w - - 0 1");
    BoardAnalysis exposed = analyse("4k3/8/8/8/8/8/8/6K1 w - - 0 1");
    assertEquals(3.0, castled.pawnShield(true), 1e-9);
    assertEquals(0, castled.openFilesNearKing(true));
    assertEquals(3, exposed.openFilesNearKing(true));
    assertTrue(castled.isCastled(true));
  }

  @Test
  public void xRayOnKing() {
    BoardAnalysis a = analyse("4r1k1/8/8/8/8/8/4N3/4K3 w - - 0 1");
    assertEquals(1, a.xRaysOnKing(true));
  }

  // ---- 5. pawn structure

  @Test
  public void passedIsolatedDoubledConnected() {
    BoardAnalysis a = analyse("4k3/p7/8/8/3P4/2P5/2P4P/4K3 w - - 0 1");
    assertTrue(a.isPassedPawn(sq("d4")));
    assertTrue(a.isPassedPawn(sq("a7")));                // passed for Black too
    assertTrue(a.isDoubledPawn(sq("c2")));
    assertFalse(a.isDoubledPawn(sq("c3")));
    assertTrue(a.isConnectedPawn(sq("d4")));             // defended by c3
    assertTrue(a.isIsolatedPawn(sq("h2")));
  }

  @Test
  public void promotionDistanceAndUnstoppablePawn() {
    BoardAnalysis a = analyse("k7/6P1/8/8/8/8/8/4K3 w - - 0 1");
    assertEquals(1, a.ranksToPromotion(sq("g7")));
    assertTrue(a.isOneStepFromPromotion(sq("g7")));
    assertTrue(a.isUnstoppablePawn(sq("g7")));
    BoardAnalysis caught = analyse("6k1/8/8/8/8/8/6P1/4K3 w - - 0 1");
    assertFalse(caught.isUnstoppablePawn(sq("g2")));
  }

  @Test
  public void backwardPawn() {
    // d6 pawn is behind its neighbours on c5/e5 and d5 is covered by the white e4 pawn.
    BoardAnalysis a = analyse("4k3/8/3p4/2p1p3/4P3/8/8/4K3 b - - 0 1");
    assertTrue(a.isBackwardPawn(sq("d6")));
  }

  // ---- outposts and placement

  @Test
  public void outpost() {
    BoardAnalysis a = analyse("4k3/8/8/3N4/2P1P3/8/8/4K3 w - - 0 1");
    assertTrue(a.isOutpost(sq("d5")));
    BoardAnalysis attackable = analyse("4k3/2p5/8/3N4/2P1P3/8/8/4K3 w - - 0 1");
    assertFalse(attackable.isOutpost(sq("d5")));
  }

  @Test
  public void rookFilesSeventhRankAndBishopPair() {
    BoardAnalysis a = analyse("4k3/R6p/8/8/8/8/P7/2B1KB1R w K - 0 1");
    assertTrue(a.isOnSeventhRank(sq("a7")));
    assertTrue(a.isOnSemiOpenFile(sq("h1")));
    assertTrue(a.hasBishopPair(true));
    assertFalse(a.hasBishopPair(false));
  }
}
