package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.agents.AgentFactory;
import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.agents.Blunder;
import com.example.fishstock.agents.Randy;
import com.example.fishstock.arena.Arena;
import com.example.fishstock.arena.MatchResult;
import com.example.fishstock.arena.Openings;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.puzzles.Puzzle;
import com.example.fishstock.puzzles.PuzzleGenerator;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.SearchLimits;
import com.example.fishstock.search.SearchResult;

import org.junit.Test;

import java.util.Collections;
import java.util.Random;

/** Blunder (level -1), tournament openings and the puzzle generator. */
public class NewFeaturesTest {

  @Test
  public void blunderAvoidsMateInOne() {
    // Rd8 mates, so the worst-move agent must play anything else.
    Move m = new Blunder(new Random(1)).chooseMove(Position.fromFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1"), null, null);
    assertNotEquals("d1d8", m.toUci());
  }

  @Test
  public void blunderHangsItsQueen() {
    // The only queen move that loses her for nothing is Qd5 (into the e6 pawn), or similar.
    Position p = Position.fromFen("4k3/8/4p3/8/3Q4/8/8/4K3 w - - 0 1");
    Move m = new Blunder(new Random(1)).chooseMove(p.copy(), null, null);
    p.makeMove(m);
    assertTrue("Blunder should put the queen en prise, played " + m.toUci(), p.isAttacked(m.to, false));
  }

  @Test
  public void randyBeatsBlunder() {
    MatchResult r = Arena.playMatch(Randy::new, Blunder::new, 6, Arena.DEFAULT_MAX_PLIES, null, null);
    assertTrue("Randy should score well against Blunder, got " + r.scoreA(), r.scoreA() >= 0.6);
  }

  @Test
  public void blunderIsALevelAndSurvivesSaving() {
    assertEquals(AgentSpec.Kind.BLUNDER, AgentSpec.LEVELS[0]);
    assertEquals("Blunder", AgentSpec.byName("Blunder").name);
    assertEquals("Blunder", AgentFactory.create(AgentSpec.builtIn(AgentSpec.Kind.BLUNDER), false).name());
    assertTrue(AgentSpec.isBuiltInName("fishstock"));
    assertTrue(!AgentSpec.isBuiltInName("My Agent"));
  }

  @Test
  public void openingsAreLegal() {
    for (String line : Openings.LINES) {
      Game g = new Game();
      Openings.play(g, line);
      assertEquals(line, line.split(" ").length, g.plyCount());
    }
    assertEquals(Openings.forGame(0), Openings.forGame(1));
    assertNotEquals(Openings.forGame(1), Openings.forGame(2));
  }

  @Test
  public void matchWithOpeningsCountsEveryGame() {
    MatchResult r = Arena.playMatch(Randy::new, Blunder::new, 4, 120, true, null, null);
    assertEquals(4, r.gamesPlayed());
  }

  @Test
  public void generatedPuzzleHasExactlyOneWinningMove() {
    PuzzleGenerator g = new PuzzleGenerator(EvalWeights.defaults(), new Random(7));
    Puzzle p = g.find(null, null);
    assertNotNull(p);
    Position pos = p.position();
    boolean legal = false;
    for (Move m : MoveGenerator.legalMoves(pos)) legal |= m.sameAs(p.solution);
    assertTrue(legal);

    // Re-check independently: the solution wins, nothing else does.
    AlphaBetaSearch search = new AlphaBetaSearch(new com.example.fishstock.eval.WeightedEvaluator(EvalWeights.defaults()));
    SearchResult best = search.search(pos, null, SearchLimits.forDepth(4), null);
    SearchResult other = search.search(pos, null, SearchLimits.forDepth(4), null, Collections.singletonList(p.solution));
    assertTrue(best.bestMove.sameAs(p.solution));
    assertTrue(best.isMateScore() || best.score >= PuzzleGenerator.WIN);
    assertTrue(other.score < PuzzleGenerator.NOT_WINNING);
  }

  @Test
  public void knownForkIsAPuzzle() {
    // Pawn fork: ...f5+ hits the king on g4 and the knight on e4.
    Position pos = Position.fromFen("4kb1r/p4r1p/p2p1p2/4p3/4N1K1/PP6/2P2PPP/R6R b k - 2 22");
    Puzzle p = new PuzzleGenerator(EvalWeights.defaults(), new Random(1)).check(pos, null, null, null);
    assertNotNull(p);
    assertEquals("f6f5", p.solution.toUci());
  }

  @Test
  public void quietPositionIsNotAPuzzle() {
    Puzzle p = new PuzzleGenerator(EvalWeights.defaults(), new Random(1)).check(Position.startingPosition(), null, null, null);
    assertEquals(null, p);
  }
}
