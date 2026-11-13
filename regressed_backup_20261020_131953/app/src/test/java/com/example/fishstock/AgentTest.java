package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.agents.MinMax;
import com.example.fishstock.agents.Randy;
import com.example.fishstock.agents.Simple;
import com.example.fishstock.arena.Arena;
import com.example.fishstock.arena.MatchResult;
import com.example.fishstock.endgame.EndgameOracle;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.search.SearchLimits;

import org.junit.Test;

public class AgentTest {

  private static String simpleMove(String fen) {
    Move m = new Simple().chooseMove(Position.fromFen(fen), null, null);
    return m.toUci();
  }

  private static String minMaxMove(String fen, int depth) {
    MinMax agent = new MinMax("MinMax", EvalWeights.defaults(), SearchLimits.forDepth(depth), EndgameOracle.NONE);
    return agent.chooseMove(Position.fromFen(fen), null, null).toUci();
  }

  @Test
  public void simplePlaysMateInOne() {
    assertEquals("d1d8", simpleMove("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1"));
    assertEquals("h5f7", simpleMove("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4"));
  }

  @Test
  public void simpleTakesAFreeQueen() {
    assertEquals("b1b2", simpleMove("2r3k1/5ppp/8/8/8/8/1q3PPP/1R1R2K1 w - - 0 1"));
  }

  @Test
  public void simpleDoesNotHangItsQueen() {
    // Qd5 would walk into the e6 pawn.
    String m = simpleMove("4k3/8/4p3/8/3Q4/8/8/4K3 w - - 0 1");
    assertTrue(!m.equals("d4d5"));
  }

  @Test
  public void minMaxFindsMateInTwo() {
    // Back rank: 1. Rb8! Rxb8 2. Rxb8#
    assertEquals("b2b8", minMaxMove("2r3k1/5ppp/8/8/8/8/1R6/1R4K1 w - - 0 1", 4));
  }

  @Test
  public void minMaxWinsMaterialTwoMovesDeep() {
    // Knight fork: Nc7+ wins the rook on a8.
    assertEquals("b5c7", minMaxMove("r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1", 3));
  }

  @Test
  public void arenaRecordsWinRates() {
    MatchResult r = Arena.playMatch(Simple::new, Randy::new, 4, Arena.DEFAULT_MAX_PLIES, null, null);
    assertEquals(4, r.gamesPlayed());
    assertEquals(4, r.winsA() + r.winsB() + r.draws());
    assertTrue("Simple should beat Randy", r.scoreA() >= 0.75);
    assertTrue(r.toCsvLine(0).startsWith("0,Simple,Randy,4,"));
  }
}
