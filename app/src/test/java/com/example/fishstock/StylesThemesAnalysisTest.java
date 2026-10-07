package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.agents.Agent;
import com.example.fishstock.agents.AgentFactory;
import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.agents.MinMax;
import com.example.fishstock.agents.PlayStyle;
import com.example.fishstock.analysis.PositionAnalyzer;
import com.example.fishstock.analysis.PositionProfile;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.Category;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.puzzles.Puzzle;
import com.example.fishstock.puzzles.PuzzleGenerator;
import com.example.fishstock.puzzles.Theme;
import com.example.fishstock.puzzles.ThemeDetector;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.MateFinder;
import com.example.fishstock.search.ScoredMove;
import com.example.fishstock.search.SearchLimits;
import com.example.fishstock.search.SearchResult;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Agro and Timid, puzzle themes and multi-move puzzles, and the position profile. */
public class StylesThemesAnalysisTest {

  // ================================================================ Agro / Timid

  @Test
  public void agroAndTimidAreBuiltInOpponents() {
    AgentSpec agro = AgentSpec.byName("Agro");
    AgentSpec timid = AgentSpec.byName("timid");
    assertEquals(AgentSpec.Kind.AGRO, agro.kind);
    assertEquals(AgentSpec.Kind.TIMID, timid.kind);
    assertTrue(agro.style().captureBonus > 0 && agro.style().checkBonus > 0);
    assertTrue(timid.style().captureBonus < 0);
    assertTrue(agro.style().mateSearchShare > 0);
    assertTrue(AgentSpec.isBuiltInName("AGRO"));
    Agent a = AgentFactory.create(agro, false, 300);
    assertEquals("Agro", a.name());
  }

  @Test
  public void aggressionSurvivesSaving() {
    Map<Category, Double> sliders = new EnumMap<>(Category.class);
    sliders.put(Category.KING_SAFETY, 1.5);
    AgentSpec s = AgentSpec.custom("Spicy", sliders, 1000, 0.6);
    AgentSpec back = AgentSpec.parse(s.serialize());
    assertEquals(0.6, back.aggression, 1e-9);
    assertEquals(1.5, back.sliders.get(Category.KING_SAFETY), 1e-9);
    // Lines saved before the Aggression slider existed still load (as neutral).
    AgentSpec old = AgentSpec.parse("CUSTOM|Old|0|KING_SAFETY=1.20");
    assertEquals(0.0, old.aggression, 1e-9);
  }

  @Test
  public void sideBiasMakesAgroCareAboutTheEnemyKing() {
    // Same position except Black's king shelter: Agro (White) should value the difference more.
    Position open = Position.fromFen("rnbq1rk1/ppppp2p/8/8/8/8/PPPPPPPP/RNBQKBNR w KQ - 0 1");
    Position safe = Position.fromFen("rnbq1rk1/ppppppp1/8/8/8/8/PPPPPPPP/RNBQKBNR w KQ - 0 1");
    WeightedEvaluator neutral = new WeightedEvaluator(EvalWeights.defaults());
    WeightedEvaluator agro = new WeightedEvaluator(EvalWeights.defaults(), PlayStyle.fromAggression(1).bias);
    agro.setPerspective(true);
    double neutralGap = neutral.evaluate(open) - neutral.evaluate(safe);
    double agroGap = agro.evaluate(open) - agro.evaluate(safe);
    assertTrue(agroGap + " vs " + neutralGap, agroGap > neutralGap + 0.05);
    // Without a perspective the biased evaluator is still symmetric.
    WeightedEvaluator unset = new WeightedEvaluator(EvalWeights.defaults(), PlayStyle.fromAggression(1).bias);
    assertEquals(neutral.evaluate(open), unset.evaluate(open), 1e-9);
  }

  @Test
  public void rootBonusChangesTheChoice() {
    // Equal-ish position: a huge bonus on one quiet move must make the search pick it.
    Position p = Position.startingPosition();
    AlphaBetaSearch s = new AlphaBetaSearch(WeightedEvaluator.withDefaults());
    s.setRootBonus((pos, m) -> m.toUci().equals("a2a3") ? 5.0 : 0.0);
    SearchResult r = s.search(p, null, SearchLimits.forDepth(2), null);
    assertEquals("a2a3", r.bestMove.toUci());
  }

  @Test
  public void timidDeclinesASmallCaptureThatAgroTakes() {
    // White can win a pawn with Nxe5, which loosens things a little. Agro grabs material it
    // likes; Timid, with a capture penalty, should prefer a quiet move when the gain is small.
    Position p = Position.fromFen("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3");
    PlayStyle agro = PlayStyle.fromAggression(1);
    PlayStyle timid = PlayStyle.fromAggression(-1);
    Move nxe5 = new Game(p.toFen()).findUci("f3e5");
    assertTrue(agro.moveBonus(p, nxe5) > 0);
    assertTrue(timid.moveBonus(p, nxe5) < 0);
  }

  @Test
  public void mateFinderSeesChecksOnlyMates() {
    MateFinder.Mate m = MateFinder.find(
        Position.fromFen("r5k1/5ppp/8/8/8/8/3R1PPP/3R2K1 w - - 0 1"), 3, 0, 1_000_000, null);
    assertNotNull(m);
    assertEquals(2, m.movesToMate);
    assertTrue(m.firstMove.toUci().endsWith("d8"));
    assertEquals(null, MateFinder.find(Position.startingPosition(), 2, 0, 100_000, null));
  }

  @Test
  public void agroPlaysTheMateItFinds() {
    MinMax agro = (MinMax) AgentFactory.create(AgentSpec.builtIn(AgentSpec.Kind.AGRO), false, 500);
    Move m = agro.chooseMove(Position.fromFen("r5k1/5ppp/8/8/8/8/3R1PPP/3R2K1 w - - 0 1"), null, null);
    assertTrue(m.toUci().endsWith("d8"));
  }

  // ================================================================ themes

  private static List<Move> line(Position start, String... uci) {
    Game g = new Game(start.toFen());
    List<Move> out = new ArrayList<>();
    for (String u : uci) {
      Move m = g.findUci(u);
      assertNotNull("illegal " + u, m);
      out.add(m);
      g.play(m);
    }
    return out;
  }

  private static List<Theme> themes(String fen, String... uci) {
    Position p = Position.fromFen(fen);
    return ThemeDetector.detect(p, line(p, uci), Collections.<Move>emptyList());
  }

  @Test
  public void forkIsRecognised() {
    List<Theme> t = themes("4kb1r/p4r1p/p2p1p2/4p3/4N1K1/PP6/2P2PPP/R6R b k - 2 22", "f6f5");
    assertTrue(t.toString(), t.contains(Theme.FORK));
  }

  @Test
  public void discoveredAndDoubleCheck() {
    String fen = "4k3/8/8/8/4B3/8/8/4R2K w - - 0 1";
    List<Theme> discovered = themes(fen, "e4h7");
    assertTrue(discovered.toString(), discovered.contains(Theme.DISCOVERED_CHECK));
    assertFalse(discovered.contains(Theme.DOUBLE_CHECK));
    List<Theme> dbl = themes(fen, "e4g6");
    assertTrue(dbl.toString(), dbl.contains(Theme.DOUBLE_CHECK));
  }

  @Test
  public void mateThemes() {
    List<Theme> backRank = themes("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1", "d1d8");
    assertTrue(backRank.toString(), backRank.contains(Theme.BACK_RANK_MATE) && backRank.contains(Theme.MATE));
    List<Theme> smothered = themes("6rk/6pp/7N/8/8/8/8/6K1 w - - 0 1", "h6f7");
    assertTrue(smothered.toString(), smothered.contains(Theme.SMOTHERED_MATE));
  }

  @Test
  public void skewerIsRecognised() {
    List<Theme> t = themes("1r6/p5B1/P7/5kp1/4p3/3b2P1/2N2P2/3K3R b - - 3 44", "b8b1", "d1d2", "b1h1");
    assertTrue(t.toString(), t.contains(Theme.SKEWER));
  }

  @Test
  public void removeTheDefender() {
    // Bxf6 takes the knight that guards h7; after ...gxf6 Qxh7 is mate.
    List<Theme> t = themes("r1b2rk1/ppp2ppp/5n2/6BQ/8/3B4/PPP2PPP/R3K2R w KQ - 0 1", "g5f6", "g7f6", "h5h7");
    assertTrue(t.toString(), t.contains(Theme.REMOVE_THE_DEFENDER));
    assertTrue(t.toString(), t.contains(Theme.MATE));
  }

  @Test
  public void textbookPawnBreakthrough() {
    // b6! axb6 c6! bxc6 a6 and the a-pawn queens.
    Position p = Position.fromFen("6k1/ppp5/8/PPP5/8/8/8/6K1 w - - 0 1");
    Puzzle puzzle = new PuzzleGenerator(EvalWeights.defaults(), new Random(1)).check(p, null, null, null);
    assertNotNull(puzzle);
    assertEquals("b5b6", puzzle.solution.toUci());
    assertTrue(puzzle.movesToFind() >= 2);
    assertTrue(puzzle.toString(), puzzle.hasTheme(Theme.PAWN_BREAKTHROUGH));
    assertTrue(puzzle.hasTheme(Theme.ENDGAME));
  }

  // ================================================================ multi-move puzzles

  @Test
  public void mostPuzzlesAreSequences() {
    PuzzleGenerator g = new PuzzleGenerator(EvalWeights.defaults(), new Random(11));
    int multi = 0;
    for (int i = 0; i < 4; i++) {
      Puzzle p = g.find(null, null);
      assertNotNull(p);
      // The line is legal and alternates solver / reply, ending with a solver move.
      Game game = new Game(p.fen);
      for (Move m : p.line) game.play(m);
      assertEquals(1, p.line.size() % 2);
      if (p.movesToFind() > 1) multi++;
    }
    assertTrue("only " + multi + " of 4 puzzles had more than one move", multi >= 2);
  }

  @Test
  public void endgameModeGivesEndgames() {
    Puzzle p = new PuzzleGenerator(EvalWeights.defaults(), new Random(5)).find(null, null, PuzzleGenerator.Mode.ENDGAME);
    assertNotNull(p);
    assertTrue(p.position().pieceCount() <= 12);
  }

  // ================================================================ position profile

  @Test
  public void startingPositionIsCalm() {
    PositionProfile prof = new PositionAnalyzer(WeightedEvaluator.withDefaults())
        .analyze(Position.startingPosition(), null, 0, null, null);
    assertEquals(20, prof.moveCount);
    assertTrue(prof.labelText(), prof.has(PositionProfile.Label.CALM));
    assertFalse(prof.has(PositionProfile.Label.PUZZLE));
  }

  @Test
  public void forkPositionIsAPuzzle() {
    PositionProfile prof = new PositionAnalyzer(WeightedEvaluator.withDefaults())
        .analyze(Position.fromFen("4kb1r/p4r1p/p2p1p2/4p3/4N1K1/PP6/2P2PPP/R6R b k - 2 22"), null, 0, null, null);
    assertTrue(prof.labelText(), prof.has(PositionProfile.Label.PUZZLE));
    assertEquals(1, prof.goodMoves);
    assertTrue(prof.negativeMoves > prof.positiveMoves);
  }

  @Test
  public void everyMoveGetsAScore() {
    Position p = Position.fromFen("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4");
    List<ScoredMove> all = new AlphaBetaSearch(WeightedEvaluator.withDefaults()).scoreAllMoves(p, null, 2, 0, null);
    assertEquals(new Game(p.toFen()).legalMoves().size(), all.size());
    assertEquals("h5f7", all.get(0).move.toUci()); // Qxf7#
    assertEquals(1, all.get(0).mateIn());
    for (int i = 1; i < all.size(); i++) assertTrue(all.get(i - 1).score >= all.get(i).score);
  }
}
