package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.agents.Agent;
import com.example.fishstock.agents.AgentFactory;
import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Odds;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Premoves;
import com.example.fishstock.engine.Square;

import org.junit.Test;

/** Piece odds and which squares a pre-move may aim at. */
public class OddsPremoveTest {

  private static int sq(String name) {
    return Square.parse(name);
  }

  @Test
  public void oddsRemoveTheAgentsPieces() {
    // Queen and f-pawn, agent playing Black.
    assertEquals("rnb1kbnr/ppppp1pp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", Odds.startFen("d1,f2", false));
    // The same odds for an agent playing White.
    assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPP1PP/RNB1KBNR w KQkq - 0 1", Odds.startFen("d1,f2", true));
    assertEquals(Position.START_FEN, Odds.startFen(Odds.NONE, true));
  }

  @Test
  public void castlingFollowsTheRooks() {
    assertEquals("rnbqkbn1/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQq - 0 1", Odds.startFen("h1", false));
    assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/1NBQKBNR w Kkq - 0 1", Odds.startFen("a1", true));
  }

  @Test
  public void theKingCantBeGivenAway() {
    assertTrue(Odds.parse("e1").isEmpty());
    assertEquals(Position.START_FEN, Odds.startFen("e1", false));
  }

  @Test
  public void oddsAreNamedLikeAChessPlayerWould() {
    // c1 is White's dark-squared bishop; for Black the same piece (c8) is the light-squared one.
    assertEquals("dark-squared bishop", Odds.describe("c1", true));
    assertEquals("light-squared bishop", Odds.describe("c1", false));
    assertEquals("queen, kingside knight and a7 pawn", Odds.describe("a2,g1,d1", false));
    assertEquals("", Odds.describe("", true));
  }

  @Test
  public void agentsPlayFromAnOddsPosition() {
    for (boolean agentWhite : new boolean[] {true, false}) {
      Game g = new Game(Odds.startFen("d1,a1,b2", agentWhite));
      if (!agentWhite) g.play(g.findUci("e2e4"));
      Agent a = AgentFactory.create(AgentSpec.builtIn(AgentSpec.Kind.MINMAX), false, 200);
      Move m = a.chooseMove(g.position().copy(), g.positionHistory(), null);
      assertNotNull(m);
      assertNotNull(g.findMove(m.from, m.to, m.promotion));
    }
  }

  @Test
  public void premoveCanRecaptureOnYourOwnPiecesSquare() {
    // After 1.e4 d5, squares White may pre-move to while Black thinks.
    Game g = new Game();
    g.play(g.findUci("e2e4"));
    g.play(g.findUci("d7d5"));
    Position p = g.position();
    assertTrue(Premoves.canTarget(p, sq("e4"), sq("d5")));   // pawn capture
    assertTrue(Premoves.canTarget(p, sq("d1"), sq("e2")));   // queen
    assertTrue(Premoves.canTarget(p, sq("f1"), sq("e2")));   // bishop
    // Own pieces' squares count (recapture if the agent takes there): Qd1xd2, Nb1xd2, Bc1xd2.
    assertTrue(Premoves.canTarget(p, sq("d1"), sq("d2")));
    assertTrue(Premoves.canTarget(p, sq("b1"), sq("d2")));
    assertTrue(Premoves.canTarget(p, sq("c1"), sq("d2")));
    // Pawns capture diagonally even onto an empty square (something may land there).
    assertTrue(Premoves.canTarget(p, sq("f2"), sq("g3")));
    assertTrue(Premoves.canTarget(p, sq("g2"), sq("g4")));   // double step from the start
    assertFalse(Premoves.canTarget(p, sq("e4"), sq("e6")));  // no double step later
    assertFalse(Premoves.canTarget(p, sq("e4"), sq("e3")));  // pawns don't go back
    assertFalse(Premoves.canTarget(p, sq("b1"), sq("b3")));  // not a knight move
    assertTrue(Premoves.canTarget(p, sq("e1"), sq("g1")));   // castling
  }

  @Test
  public void aQueuedRecaptureBecomesLegal() {
    // 1.e4 d5 2.Nc3: White queues Nxe4 while its own pawn is still on e4; after ...dxe4 it's legal.
    Game g = new Game();
    for (String u : new String[] {"e2e4", "d7d5", "b1c3"}) g.play(g.findUci(u));
    // Queued while Black thinks: the knight on c3 to e4 (White's own pawn is there now).
    assertTrue(Premoves.canTarget(g.position(), sq("c3"), sq("e4")));
    g.play(g.findUci("d5e4"));
    assertNotNull(g.findMove(sq("c3"), sq("e4"), 0));
  }
}
