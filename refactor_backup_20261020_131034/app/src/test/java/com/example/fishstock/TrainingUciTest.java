package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.analysis.MoveReview;
import com.example.fishstock.analysis.Training;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.puzzles.Puzzle;
import com.example.fishstock.puzzles.PuzzleBook;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.SearchLimits;
import com.example.fishstock.search.SearchResult;
import com.example.fishstock.uci.UciEngine;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** Key moments, training sets, and the UCI engine. */
public class TrainingUciTest {

  /** Reviews every move of a game the way the analysis screen does. */
  private static MoveReview.Result[] review(Game game) {
    WeightedEvaluator ev = WeightedEvaluator.withDefaults();
    AlphaBetaSearch s = new AlphaBetaSearch(ev);
    SearchResult[] r = new SearchResult[game.plyCount() + 1];
    MoveReview.Result[] out = new MoveReview.Result[game.plyCount() + 1];
    for (int i = 0; i <= game.plyCount(); i++) {
      Position p = game.positionAt(i);
      if (MoveGenerator.hasLegalMove(p)) {
        r[i] = s.search(p, new ArrayList<>(game.positionHistory().subList(0, i + 1)), SearchLimits.forDepth(3), null);
      }
      if (i > 0 && r[i - 1] != null) out[i] = MoveReview.review(game.positionAt(i - 1), game.moves().get(i - 1), r[i - 1], r[i], ev);
    }
    return out;
  }

  private static Game play(String... uci) {
    Game g = new Game();
    for (String u : uci) g.play(g.findUci(u));
    return g;
  }

  @Test
  public void hangingTheQueenIsAKeyMomentAndBecomesAPuzzle() {
    // 1.e4 e5 2.Nf3 Qg5?? 3.Nxg5: Black hangs the queen.
    Game g = play("e2e4", "e7e5", "g1f3", "d8g5", "f3g5", "b8c6");
    MoveReview.Result[] reviews = review(g);
    List<Training.KeyMoment> km = Training.keyMoments(reviews, -1, true);
    assertFalse(km.isEmpty());
    assertEquals(4, km.get(0).ply); // 2...Qg5 led to position 4
    List<Training.KeyMoment> whiteOnly = Training.keyMoments(reviews, 1, true);
    for (Training.KeyMoment k : whiteOnly) assertTrue(k.ply % 2 == 1);

    List<Puzzle> set = Training.buildSet(g, km, WeightedEvaluator.withDefaults(), null);
    assertFalse(set.isEmpty());
    Puzzle first = set.get(0);
    assertTrue(first.title.startsWith("Key moment"));
    // Anything but the blunder that keeps the balance should do; the blunder itself must not.
    Move blunder = g.moves().get(3);
    assertFalse(first.accepts(0, blunder));
    // Round trip through the format used to hand the set to the puzzle screen.
    Puzzle back = PuzzleBook.parse(PuzzleBook.format(first));
    assertNotNull(back);
    assertEquals(first.title, back.title);
    assertEquals(first.alsoAccepted.size(), back.alsoAccepted.size());
  }

  @Test
  public void keyMomentsAreCappedAndInGameOrder() {
    Game g = play("f2f3", "e7e5", "g2g4", "d8h4");
    MoveReview.Result[] reviews = review(g);
    List<Training.KeyMoment> km = Training.keyMoments(reviews, 0, true);
    assertTrue(km.size() <= Training.MAX_MOMENTS);
    for (int i = 1; i < km.size(); i++) assertTrue(km.get(i - 1).ply < km.get(i).ply);
  }

  // ================================================================ UCI

  private static String session(String... commands) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    UciEngine e = new UciEngine(new PrintStream(bytes, true));
    boolean searching = false;
    for (String c : commands) {
      e.handle(c);
      searching |= c.startsWith("go");
    }
    // Let the search finish on its own (it runs on its own thread) before reading the output.
    long deadline = System.currentTimeMillis() + 10_000;
    while (searching && !bytes.toString().contains("bestmove") && System.currentTimeMillis() < deadline) {
      try {
        Thread.sleep(20);
      } catch (InterruptedException ignored) {
        break;
      }
    }
    e.handle("stop");
    return bytes.toString();
  }

  @Test
  public void uciHandshake() {
    String out = session("uci", "isready");
    assertTrue(out.contains("id name FishStock"));
    assertTrue(out.contains("option name Agent"));
    assertTrue(out.contains("uciok"));
    assertTrue(out.contains("readyok"));
  }

  @Test
  public void uciPlaysALegalMove() {
    String out = session("setoption name Agent value Simple", "position startpos moves e2e4 e7e5 g1f3",
        "go movetime 300");
    String best = out.substring(out.lastIndexOf("bestmove ") + 9).trim().split("\\s+")[0];
    Game g = play("e2e4", "e7e5", "g1f3");
    assertNotNull("illegal bestmove " + best, g.findUci(best));
  }

  @Test
  public void uciFindsMateInOne() {
    String out = session("setoption name Agent value MinMax", "position fen 6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
        "go movetime 500");
    assertTrue(out, out.contains("bestmove d1d8"));
  }
}
