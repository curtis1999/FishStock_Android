package com.example.fishstock.analysis;

import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.Evaluator;
import com.example.fishstock.puzzles.Puzzle;
import com.example.fishstock.puzzles.PuzzleBook;
import com.example.fishstock.puzzles.PuzzleGenerator;
import com.example.fishstock.puzzles.Theme;
import com.example.fishstock.puzzles.ThemeDetector;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.ScoredMove;
import com.example.fishstock.search.SearchLimits;
import com.example.fishstock.search.SearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Learning from a finished game.
 *
 * 1. {@link #keyMoments}: the two or three moves that changed the game most - the blunders,
 *    mistakes and misses that threw away the most winning chance ({@link MoveReview}).
 * 2. {@link #buildSet}: a training set. First each key moment as a puzzle (the position before
 *    the bad move; find what should have been played), then for each one a few puzzles from the
 *    collection ({@link PuzzleBook}) on the same theme: miss a mate in 2 and you get mate-in-2
 *    puzzles, allow a fork and you get fork puzzles.
 *
 * A key-moment position isn't always a clean "only one move" puzzle (sometimes the point is
 * just not to blunder), so any move within {@link #ACCEPT_MARGIN} pawns of the best one counts.
 */
public final class Training {
  /** At most this many key moments per game. */
  public static final int MAX_MOMENTS = 3;
  /** Collection puzzles added per key moment. */
  public static final int SIMILAR_PER_MOMENT = 3;
  /** In a key-moment puzzle, moves this close to the best one are also accepted. */
  public static final double ACCEPT_MARGIN = 0.3;

  /** A turning point of the game. */
  public static final class KeyMoment {
    /** Position number after the move (the move is game.moves().get(ply - 1)). */
    public final int ply;
    public final MoveReview.Result review;

    KeyMoment(int ply, MoveReview.Result review) {
      this.ply = ply;
      this.review = review;
    }

    /** The move that was actually played. */
    public Move reviewMove(Game game) {
      return game.moves().get(ply - 1);
    }

    /** "23. g3 (Miss)" */
    public String label() {
      return review.moveText + review.verdict.symbol + " (" + review.verdict.label + ")";
    }
  }

  private Training() {}

  /**
   * @param reviews  reviews[i] judges the move that led to position i (index 0 unused)
   * @param side     +1 = only White's moves, -1 = only Black's, 0 = both
   * @param whiteFirst who made the first move of the game
   */
  public static List<KeyMoment> keyMoments(MoveReview.Result[] reviews, int side, boolean whiteFirst) {
    List<KeyMoment> all = new ArrayList<>();
    for (int i = 1; i < reviews.length; i++) {
      MoveReview.Result r = reviews[i];
      if (r == null) continue;
      boolean whiteMoved = whiteFirst == (i % 2 == 1);
      if (side > 0 && !whiteMoved || side < 0 && whiteMoved) continue;
      boolean bad = r.verdict == MoveReview.Verdict.BLUNDER || r.verdict == MoveReview.Verdict.MISTAKE
          || r.verdict == MoveReview.Verdict.MISS;
      if (bad) all.add(new KeyMoment(i, r));
    }
    // Biggest swing first, keep the top few, then show them in game order.
    Collections.sort(all, (a, b) -> Double.compare(swing(b), swing(a)));
    List<KeyMoment> top = new ArrayList<>(all.subList(0, Math.min(MAX_MOMENTS, all.size())));
    Collections.sort(top, (a, b) -> Integer.compare(a.ply, b.ply));
    return top;
  }

  /** How much the move changed the game: winning chance lost, with misses counted by score dropped. */
  private static double swing(KeyMoment k) {
    double lost = k.review.winChanceLost;
    if (k.review.verdict == MoveReview.Verdict.MISS) {
      lost = Math.max(lost, MoveReview.winChance(k.review.bestScore) - MoveReview.winChance(k.review.playedScore));
    }
    return lost;
  }

  /** The training set: each key moment as a puzzle, followed by collection puzzles on its theme. */
  public static List<Puzzle> buildSet(Game game, List<KeyMoment> moments, Evaluator evaluator, AtomicBoolean stop) {
    List<Puzzle> out = new ArrayList<>();
    List<String> used = new ArrayList<>();
    for (KeyMoment k : moments) {
      if (stop != null && stop.get()) return out;
      Puzzle p = puzzleFor(game, k, evaluator, stop);
      if (p == null) continue;
      out.add(p);
      used.add(p.fen);
    }
    for (KeyMoment k : moments) {
      String group = groupFor(k, out);
      if (group == null) continue;
      List<Puzzle> pool = PuzzleBook.groups().get(group);
      if (pool == null) continue;
      int added = 0;
      for (Puzzle q : pool) {
        if (added >= SIMILAR_PER_MOMENT || used.contains(q.fen)) continue;
        out.add(q.withTitle("Practice: " + group));
        used.add(q.fen);
        added++;
      }
    }
    return out;
  }

  /** The position before the key move, as a puzzle: find the move that should have been played. */
  public static Puzzle puzzleFor(Game game, KeyMoment k, Evaluator evaluator, AtomicBoolean stop) {
    int before = k.ply - 1;
    Position pos = game.positionAt(before);
    if (!MoveGenerator.hasLegalMove(pos)) return null;
    List<Long> history = new ArrayList<>(game.positionHistory().subList(0, before + 1));
    String title = "Key moment: " + k.label() + ". Find the better move.";

    // A real puzzle (one winning line) if there is one...
    Puzzle real = new PuzzleGenerator(EvalWeights.defaults(), new Random(k.ply)).check(pos, history, null, stop);
    if (real != null) return real.withTitle(title);

    // ...otherwise accept any move about as good as the best.
    AlphaBetaSearch search = new AlphaBetaSearch(evaluator);
    List<ScoredMove> scored = search.scoreAllMoves(pos, history, 3, 4000, stop);
    if (scored.isEmpty() || (stop != null && stop.get())) return null;
    ScoredMove best = scored.get(0);
    List<Move> accepted = new ArrayList<>();
    for (int i = 1; i < scored.size(); i++) {
      ScoredMove s = scored.get(i);
      boolean ok = best.isMate() ? s.isMate() && s.mateIn() > 0 : s.score >= best.score - ACCEPT_MARGIN;
      if (ok && !s.move.sameAs(k.reviewMove(game))) accepted.add(s.move);
    }
    Position after = pos.copy();
    after.makeMove(best.move);
    List<Move> continuation = new ArrayList<>();
    if (MoveGenerator.hasLegalMove(after)) {
      SearchResult r = new AlphaBetaSearch(evaluator).search(after, null, SearchLimits.forDepth(3), stop);
      for (int i = 0; i < r.principalVariation.size() && i < 3; i++) continuation.add(r.principalVariation.get(i));
    }
    List<Move> line = Collections.singletonList(best.move);
    List<Theme> themes = ThemeDetector.detect(pos, line, continuation);
    double second = scored.size() > 1 ? scored.get(1).score : best.score;
    return new Puzzle(pos.toFen(), line, continuation, best.score, second, best.isMate() ? best.mateIn() : 0,
        themes, accepted, title);
  }

  /** Which collection group matches a key moment: "Mate in N" for mates, else its main tactic. */
  static String groupFor(KeyMoment k, List<Puzzle> keyPuzzles) {
    Map<String, List<Puzzle>> groups = PuzzleBook.groups();
    int mate = 0;
    if (k.review.verdict == MoveReview.Verdict.MISS && k.review.bestScore > AlphaBetaSearch.MATE_THRESHOLD) {
      mate = (int) ((Math.round(AlphaBetaSearch.MATE - k.review.bestScore) + 1) / 2);
    }
    for (Puzzle p : keyPuzzles) if (p.title.contains(k.review.moveText) && p.mateIn > 0) mate = p.mateIn;
    if (mate > 0) {
      String g = PuzzleBook.mateGroup(Math.min(4, mate));
      if (groups.containsKey(g)) return g;
    }
    List<Theme> candidates = new ArrayList<>(k.review.tactics);
    for (Puzzle p : keyPuzzles) if (p.title.contains(k.review.moveText)) candidates.addAll(p.themes);
    for (Theme t : candidates) {
      if (t == Theme.ENDGAME || t == Theme.QUIET_MOVE || t == Theme.MATE) continue;
      if (groups.containsKey(t.label)) return t.label;
    }
    return null;
  }
}
