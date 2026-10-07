package com.example.fishstock.arena;

import com.example.fishstock.agents.Agent;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.GameResult;
import com.example.fishstock.engine.Move;
import com.example.fishstock.eval.WeightedEvaluator;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Plays agents against each other with no screen involved. Used by the Tournament screen, and
 * designed to be the inner loop of a future training run, e.g.:
 *
 * <pre>
 *   MatchResult r = Arena.playMatch(
 *       () -> new MinMax("candidate", candidateWeights, SearchLimits.forDepth(2), EndgameOracle.NONE),
 *       () -> new MinMax("baseline", EvalWeights.defaults(), SearchLimits.forDepth(2), EndgameOracle.NONE),
 *       100, Arena.DEFAULT_MAX_PLIES, null, null);
 *   double fitness = r.scoreA();
 * </pre>
 *
 * Colours alternate every game so neither agent gets the white pieces more often.
 * Pure Java: runs on the phone or on a desktop JVM (much faster for training).
 */
public final class Arena {
  public static final int DEFAULT_MAX_PLIES = 300;
  /** At the move limit, an evaluation beyond this many pawns counts as a win. */
  public static final double ADJUDICATION_MARGIN = 3.0;

  public interface AgentSupplier {
    Agent get();
  }

  public interface Listener {
    void onGameFinished(int gamesDone, int totalGames, GameRecord game, MatchResult soFar);
  }

  private Arena() {}

  /**
   * @param agentA   creates agent A (a fresh one per game)
   * @param agentB   creates agent B
   * @param games    number of games
   * @param maxPlies games longer than this are adjudicated
   * @param listener progress callback, may be null (called on the Arena's thread)
   * @param stop     set to true to end the match early, may be null
   */
  public static MatchResult playMatch(AgentSupplier agentA, AgentSupplier agentB, int games, int maxPlies,
                                      Listener listener, AtomicBoolean stop) {
    MatchResult result = null;
    for (int i = 0; i < games; i++) {
      if (stop != null && stop.get()) break;
      Agent a = agentA.get();
      Agent b = agentB.get();
      if (result == null) result = new MatchResult(a.name(), b.name());
      boolean aWhite = i % 2 == 0;
      GameRecord g = aWhite ? playGame(a, b, maxPlies, stop) : playGame(b, a, maxPlies, stop);
      if (stop != null && stop.get() && !g.result.isOver) break;
      result.add(g, aWhite);
      if (listener != null) listener.onGameFinished(i + 1, games, g, result);
    }
    if (result == null) result = new MatchResult(agentA.get().name(), agentB.get().name());
    return result;
  }

  /** Plays one game to the end (or to the move limit). */
  public static GameRecord playGame(Agent white, Agent black, int maxPlies, AtomicBoolean stop) {
    Game game = new Game();
    while (!game.isOver() && game.plyCount() < maxPlies) {
      if (stop != null && stop.get()) break;
      Agent mover = game.whiteToMove() ? white : black;
      Move m = mover.chooseMove(game.position().copy(), new ArrayList<>(game.positionHistory()), stop);
      if (m == null || game.findMove(m.from, m.to, m.promotion) == null) {
        // An agent that cannot produce a legal move forfeits.
        game.end(game.whiteToMove() ? GameResult.WHITE_RESIGNS : GameResult.BLACK_RESIGNS);
        break;
      }
      game.play(m);
    }
    if (!game.isOver() && game.plyCount() >= maxPlies) game.end(adjudicate(game));
    return new GameRecord(white.name(), black.name(), game.result(), game.plyCount(), game.toSanString());
  }

  /** Scores an unfinished game using the default evaluation. */
  static GameResult adjudicate(Game game) {
    double eval = WeightedEvaluator.withDefaults().evaluate(game.position());
    if (eval >= ADJUDICATION_MARGIN) return GameResult.ADJUDICATED_WHITE;
    if (eval <= -ADJUDICATION_MARGIN) return GameResult.ADJUDICATED_BLACK;
    return GameResult.ADJUDICATED_DRAW;
  }
}
