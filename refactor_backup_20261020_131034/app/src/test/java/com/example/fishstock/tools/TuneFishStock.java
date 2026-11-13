package com.example.fishstock.tools;

import com.example.fishstock.arena.Arena;
import com.example.fishstock.arena.MatchResult;
import com.example.fishstock.agents.MinMax;
import com.example.fishstock.endgame.EndgameOracle;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.search.SearchLimits;
import com.example.fishstock.training.SelfPlay;
import com.example.fishstock.training.TexelTuner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The FishStock training run: self-play -> Texel tuning -> repeat with the new weights -> check the
 * result in real games against the untuned improved evaluation (MinMax+).
 *
 *   java ... com.example.fishstock.tools.TuneFishStock [rounds] [gamesPerRound] [depth] [threads] [minutesPerTune] [checkGames]
 *
 * Prints the final weights in EvalWeights.serialize() form: paste that into FishStock.TRAINED_WEIGHTS.
 * Plain main() so it doesn't run with the unit tests. More cores and more games = better weights.
 */
public final class TuneFishStock {
  public static void main(String[] args) throws Exception {
    int rounds = arg(args, 0, 2);
    int games = arg(args, 1, 1500);
    int depth = arg(args, 2, 1);
    int threads = arg(args, 3, Runtime.getRuntime().availableProcessors());
    int minutes = arg(args, 4, 15);
    int checkGames = arg(args, 5, 40);

    EvalWeights weights = EvalWeights.defaults();
    List<SelfPlay.Sample> pool = new ArrayList<>();
    long totalGames = 0;
    for (int round = 1; round <= rounds; round++) {
      long t0 = System.currentTimeMillis();
      final int r = round;
      List<SelfPlay.Sample> s = SelfPlay.play(weights, games, depth, threads, 1000L * round, (done, total, n) -> {
        if (done % 100 == 0) System.err.printf(Locale.US, "round %d: %d/%d games, %d positions%n", r, done, total, n);
      });
      totalGames += games;
      pool.addAll(s);
      System.err.printf(Locale.US, "round %d: self-play done in %ds, %d positions in total from %d games%n",
          round, (System.currentTimeMillis() - t0) / 1000, pool.size(), totalGames);
      TexelTuner tuner = new TexelTuner(pool, threads);
      tuner.fitK(weights, System.err::println);
      weights = tuner.tune(weights, 30, System.currentTimeMillis() + minutes * 60_000L, System.err::println);
      tuner.shutdown();
      System.err.println("round " + round + " weights: " + weights.serialize());
    }

    if (checkGames > 0) {
      final EvalWeights tuned = weights;
      MatchResult m = Arena.playMatch(
          () -> new MinMax("Tuned", tuned, SearchLimits.forDepth(3), EndgameOracle.NONE),
          () -> new MinMax("MinMax+", EvalWeights.defaults(), SearchLimits.forDepth(3), EndgameOracle.NONE),
          checkGames, Arena.DEFAULT_MAX_PLIES, true, null, null);
      System.err.println(m.summary());
    }
    System.out.println(weights.serialize());
  }

  private static int arg(String[] a, int i, int def) {
    return a.length > i ? Integer.parseInt(a[i]) : def;
  }
}
