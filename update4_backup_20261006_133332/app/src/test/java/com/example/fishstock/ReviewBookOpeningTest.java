package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.agents.Agent;
import com.example.fishstock.agents.AgentFactory;
import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.agents.OpeningBook;
import com.example.fishstock.analysis.MoveReview;
import com.example.fishstock.arena.Arena;
import com.example.fishstock.arena.GameRecord;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.puzzles.Puzzle;
import com.example.fishstock.puzzles.PuzzleBook;
import com.example.fishstock.puzzles.Theme;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.SearchLimits;
import com.example.fishstock.search.SearchResult;

import org.junit.Test;

import java.util.List;
import java.util.Map;

/** Opening books, move review, the puzzle collection and the opponent order. */
public class ReviewBookOpeningTest {

  // ================================================================ openings

  @Test
  public void everyBookLineIsLegal() {
    for (OpeningBook.Repertoire r : OpeningBook.Repertoire.values()) {
      assertTrue(r + ": " + OpeningBook.of(r).badLines(), OpeningBook.of(r).badLines().isEmpty());
    }
  }

  @Test
  public void aggressionPicksTheRepertoire() {
    assertEquals(OpeningBook.Repertoire.KINGS_GAMBIT, OpeningBook.repertoireFor(1.0));
    assertEquals(OpeningBook.Repertoire.ATTACKING_ITALIAN, OpeningBook.repertoireFor(0.6));
    assertEquals(OpeningBook.Repertoire.ITALIAN, OpeningBook.repertoireFor(0.2));
    assertEquals(OpeningBook.Repertoire.MAIN_LINES, OpeningBook.repertoireFor(0.0));
    assertEquals(OpeningBook.Repertoire.FIANCHETTO, OpeningBook.repertoireFor(-1.0));
  }

  private static String play(AgentSpec.Kind kind, String... sanBefore) {
    Agent a = AgentFactory.create(AgentSpec.builtIn(kind), false, 200);
    Game g = new Game();
    for (String san : sanBefore) g.play(Notation.fromSan(g.position(), san));
    Move m = a.chooseMove(g.position().copy(), g.positionHistory(), null);
    return Notation.toSan(g.position(), m);
  }

  @Test
  public void agroPlaysTheKingsGambit() {
    assertEquals("e4", play(AgentSpec.Kind.AGRO));
    assertEquals("f4", play(AgentSpec.Kind.AGRO, "e4", "e5"));
  }

  @Test
  public void mainAgentsPlayTheRuyLopez() {
    assertEquals("e4", play(AgentSpec.Kind.MINMAX));
    assertEquals("Bb5", play(AgentSpec.Kind.MINMAX, "e4", "e5", "Nf3", "Nc6"));
    assertEquals("e5", play(AgentSpec.Kind.SIMPLE, "e4"));
  }

  @Test
  public void timidFianchettoes() {
    assertEquals("Nf3", play(AgentSpec.Kind.TIMID));
    // Out of book after 1.Nf3 a6: the set-up move g3 is still played (it's a perfectly good move).
    assertEquals("g3", play(AgentSpec.Kind.TIMID, "Nf3", "a6"));
    assertEquals("c6", play(AgentSpec.Kind.TIMID, "e4"));
  }

  @Test
  public void opponentsAreOrderedByStrength() {
    assertEquals(1, AgentSpec.builtIn(AgentSpec.Kind.SIMPLE).level());
    assertEquals(2, AgentSpec.builtIn(AgentSpec.Kind.TIMID).level());
    assertEquals(3, AgentSpec.builtIn(AgentSpec.Kind.AGRO).level());
    assertEquals(4, AgentSpec.builtIn(AgentSpec.Kind.MINMAX).level());
    assertEquals(5, AgentSpec.builtIn(AgentSpec.Kind.FISHSTOCK).level());
  }

  @Test
  public void tournamentGamesCanBeReplayed() {
    GameRecord r = Arena.playGame(AgentFactory.create(AgentSpec.builtIn(AgentSpec.Kind.RANDY), false),
        AgentFactory.create(AgentSpec.builtIn(AgentSpec.Kind.RANDY), false), 40, null);
    Game g = new Game(r.startFen);
    for (String u : r.movesUci) g.play(g.findUci(u));
    assertEquals(r.plies, g.plyCount());
  }

  // ================================================================ move review

  private static MoveReview.Result review(String fen, String san) {
    Position before = Position.fromFen(fen);
    Move m = Notation.fromSan(before, san);
    WeightedEvaluator ev = WeightedEvaluator.withDefaults();
    SearchResult b = new AlphaBetaSearch(ev).search(before, null, SearchLimits.forDepth(4), null);
    Position after = before.copy();
    after.makeMove(m);
    SearchResult a = new Game(after.toFen()).legalMoves().isEmpty() ? null
        : new AlphaBetaSearch(ev).search(after, null, SearchLimits.forDepth(4), null);
    return MoveReview.review(before, m, b, a, ev);
  }

  @Test
  public void hangingTheQueenIsABlunder() {
    MoveReview.Result r = review("rnb1kbnr/pppp1ppp/8/4p1q1/4P3/3P4/PPP2PPP/RNBQKBNR b KQkq - 0 3", "Qg4");
    assertEquals(r.explanation, MoveReview.Verdict.BLUNDER, r.verdict);
    assertTrue(r.explanation, r.explanation.contains("queen"));
  }

  @Test
  public void missingMateIsAMiss() {
    // Rd8 mates; Kf1 keeps an extra rook but misses it.
    MoveReview.Result r = review("6k1/5ppp/8/8/8/8/r4PPP/3R2K1 w - - 0 1", "Kf1");
    assertEquals(r.explanation, MoveReview.Verdict.MISS, r.verdict);
    assertTrue(r.explanation, r.explanation.contains("mate"));
  }

  @Test
  public void theEngineMoveIsBest() {
    MoveReview.Result r = review("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1", "Rd8#");
    assertTrue(r.verdict == MoveReview.Verdict.BEST || r.verdict == MoveReview.Verdict.BRILLIANT);
  }

  @Test
  public void allowingAForkIsExplained() {
    // ...Kf7?? walks into Ne5+ forking king and queen (d7)... any explanation must name a reason.
    MoveReview.Result r = review("r1b1kb1r/pppq1ppp/2n2n2/3p4/3P4/2N2N2/PPP1BPPP/R1BQK2R b KQkq - 0 6", "Ne4");
    assertNotNull(r.explanation);
    assertTrue(r.explanation.length() > 10);
  }

  // ================================================================ puzzle collection

  @Test
  public void puzzleCollectionLoadsWithThemes() {
    List<Puzzle> all = PuzzleBook.all();
    assertEquals(10, all.size());
    Map<String, List<Puzzle>> groups = PuzzleBook.groups();
    for (int n = 1; n <= 4; n++) assertTrue(groups.toString(), groups.containsKey(PuzzleBook.mateGroup(n)));
    assertTrue(groups.containsKey(Theme.FORK.label));
    assertTrue(groups.containsKey(Theme.PAWN_RACE.label));
    assertTrue(groups.containsKey(Theme.PAWN_BREAKTHROUGH.label));
    for (Puzzle p : all) {
      Game g = new Game(p.fen);
      for (Move m : p.line) g.play(m);
      assertEquals(1, p.line.size() % 2);
    }
  }
}
