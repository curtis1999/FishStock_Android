package com.example.fishstock.training;

import com.example.fishstock.agents.MinMax;
import com.example.fishstock.arena.Openings;
import com.example.fishstock.endgame.EndgameOracle;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.GameResult;
import com.example.fishstock.engine.Move;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.search.SearchLimits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Self-play for training: many quick games of an agent against itself, each starting from a
 * different opening plus a few random moves, and every quiet position labelled with the game's
 * final result. These labelled positions are what {@link TexelTuner} learns from.
 *
 * Games use fixed-depth search (so results don't depend on the machine) and run on several threads.
 * Pure Java: run it on a desktop (see tools/TuneFishStock), not on the phone.
 */
public final class SelfPlay {
  /** One training example: a position (FEN) and how the game ended, 1 = White won, 0.5 draw, 0 Black won. */
  public static final class Sample {
    public final String fen;
    public final double result;

    public Sample(String fen, double result) {
      this.fen = fen;
      this.result = result;
    }
  }

  public interface Progress {
    void gamesDone(int done, int total, int samples);
  }

  private static final int MAX_PLIES = 220;
  private static final int SKIP_OPENING_PLIES = 8;
  private static final int RANDOM_MOVES = 4;

  private SelfPlay() {}

  /**
   * @param weights the evaluation both sides use
   * @param depth   search depth per move (1-2 is fast; quiescence and check extensions come on top)
   */
  public static List<Sample> play(EvalWeights weights, int games, int depth, int threads, long seed,
                                  Progress progress) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, threads));
    final List<Sample> all = Collections.synchronizedList(new ArrayList<Sample>());
    final AtomicInteger done = new AtomicInteger();
    List<Future<?>> jobs = new ArrayList<>();
    for (int i = 0; i < games; i++) {
      final int index = i;
      jobs.add(pool.submit(() -> {
        List<Sample> s = playOne(weights, depth, new Random(seed * 1_000_003L + index), index);
        all.addAll(s);
        int n = done.incrementAndGet();
        if (progress != null) progress.gamesDone(n, games, all.size());
      }));
    }
    for (Future<?> f : jobs) f.get();
    pool.shutdown();
    pool.awaitTermination(1, TimeUnit.MINUTES);
    return new ArrayList<>(all);
  }

  /** One game; returns its quiet positions labelled with the result. */
  static List<Sample> playOne(EvalWeights weights, int depth, Random random, int index) {
    Game game = new Game();
    Openings.play(game, Openings.LINES[index % Openings.LINES.length]);
    for (int i = 0; i < RANDOM_MOVES && !game.isOver(); i++) {
      List<Move> legal = game.legalMoves();
      game.play(legal.get(random.nextInt(legal.size())));
    }
    MinMax white = new MinMax("A", weights, SearchLimits.forDepth(depth), EndgameOracle.NONE);
    MinMax black = new MinMax("B", weights, SearchLimits.forDepth(depth), EndgameOracle.NONE);
    List<String> fens = new ArrayList<>();
    while (!game.isOver() && game.plyCount() < MAX_PLIES) {
      MinMax mover = game.whiteToMove() ? white : black;
      Move m = mover.chooseMove(game.position().copy(), new ArrayList<>(game.positionHistory()), null);
      if (m == null) break;
      game.play(m);
      // Quiet positions only: not in check, and the move that led here wasn't a capture.
      if (game.plyCount() > SKIP_OPENING_PLIES && !m.isCapture() && !game.position().inCheck() && !game.isOver()) {
        fens.add(game.position().toFen());
      }
    }
    double result;
    GameResult r = game.result();
    if (r.isOver) {
      result = r.score > 0 ? 1.0 : r.score < 0 ? 0.0 : 0.5;
    } else {
      // Move limit: score it on the board (big edge = win, otherwise a draw).
      double e = new WeightedEvaluator(weights).evaluate(game.position());
      result = e > 3 ? 1.0 : e < -3 ? 0.0 : 0.5;
    }
    List<Sample> out = new ArrayList<>(fens.size());
    for (String f : fens) out.add(new Sample(f, result));
    return out;
  }
}
