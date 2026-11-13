package com.example.fishstock.nn;

import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.GameResult;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * One self-play game: the network plays itself with MCTS on every move, and each searched
 * position is saved with what the search preferred and (once the game ends) who won.
 *
 * Two tricks from AlphaZero and KataGo keep it learning on a laptop:
 *  - For the first {@link #randomPlies} plies, moves are picked in proportion to how much they
 *    were searched, and the root gets random noise, so games don't all repeat the same opening.
 *  - "Playout cap randomisation": only some moves get a full search and are kept for training;
 *    the rest get a quick search just to move the game along. Many more games for the same time.
 */
public final class ZeroSelfPlay {

  /** A training example: a position, the search's move preferences, and the final result. */
  public static final class Sample {
    public final int[] features;
    public final int[] moves;
    public final float[] target;
    /** Result for the side to move: +1 win, 0 draw, -1 loss. */
    public float z;

    public Sample(int[] features, int[] moves, float[] target, float z) {
      this.features = features;
      this.moves = moves;
      this.target = target;
      this.z = z;
    }
  }

  public static final class Settings {
    public int fullSimulations = 100;
    public int fastSimulations = 25;
    /** Share of moves that get a full search (and become training examples). */
    public double fullSearchShare = 0.3;
    public int randomPlies = 20;
    /** Games longer than this are called a draw. */
    public int maxPlies = 250;
  }

  public static final class GameOutcome {
    public final List<Sample> samples;
    /** +1 White won, 0 draw, -1 Black won. */
    public final int result;
    public final int plies;
    public final String ending;

    GameOutcome(List<Sample> samples, int result, int plies, String ending) {
      this.samples = samples;
      this.result = result;
      this.plies = plies;
      this.ending = ending;
    }
  }

  private ZeroSelfPlay() {}

  public static GameOutcome play(Network net, Settings s, Random rng) {
    Game game = new Game();
    Mcts mcts = new Mcts(net, rng);
    List<Sample> samples = new ArrayList<>();
    List<Boolean> sampleWhite = new ArrayList<>();

    while (!game.isOver() && game.plyCount() < s.maxPlies) {
      Position pos = game.position();
      boolean full = rng.nextDouble() < s.fullSearchShare;
      mcts.rootNoise = full;
      Mcts.Result r = mcts.search(pos, game.positionHistory(), full ? s.fullSimulations : s.fastSimulations, 0, null);
      if (r.moves.isEmpty()) break;
      if (full) {
        samples.add(new Sample(Encoder.features(pos), r.index.clone(), r.visitDistribution(), 0));
        sampleWhite.add(pos.whiteToMove());
      }
      Move m = game.plyCount() < s.randomPlies ? r.sample(1.0, rng) : r.best();
      game.play(m);
    }

    GameResult gr = game.result();
    int result = gr.isOver ? (gr.whiteWon() ? 1 : gr.blackWon() ? -1 : 0) : 0;
    String ending = gr.isOver ? gr.name() : "MOVE_LIMIT";
    for (int i = 0; i < samples.size(); i++) {
      samples.get(i).z = sampleWhite.get(i) ? result : -result;
    }
    return new GameOutcome(samples, result, game.plyCount(), ending);
  }
}
