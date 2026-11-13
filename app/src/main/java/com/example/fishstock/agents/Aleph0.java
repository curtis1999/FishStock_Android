package com.example.fishstock.agents;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Position;
import com.example.fishstock.nn.Mcts;
import com.example.fishstock.nn.Network;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Aleph_0: a neural network that taught itself chess by playing itself (AlphaZero style), with
 * no hand-written knowledge at all: no piece values, no king safety, no opening book. It searches
 * with MCTS ({@link Mcts}) guided by the network ({@link Network}).
 *
 * It plays with the network shipped in the app (trained on a PC with train_aleph0.bat). If none
 * is shipped, it plays with an untrained network, which is barely better than random.
 */
public final class Aleph0 implements Agent {
  public static final long THINK_MS = 3000;

  private final Network net;
  private final long thinkMs;
  private final int maxSimulations;
  private Mcts.Result last;

  /**
   * Most simulations per move when playing by time. The search tree grows by one position per
   * simulation (about 1.5 KB each), so this keeps it to ~150 MB on a fast PC; a phone doesn't get
   * near it in 3 s.
   */
  public static final int MAX_SIMULATIONS = 100_000;

  /** Plays by time (the app and UCI). */
  public Aleph0(long thinkMs) {
    this(defaultNetwork(), thinkMs, MAX_SIMULATIONS);
  }

  /** @param maxSimulations limit by simulations (training matches), or Integer.MAX_VALUE */
  public Aleph0(Network net, long thinkMs, int maxSimulations) {
    this.net = net;
    this.thinkMs = thinkMs;
    this.maxSimulations = maxSimulations;
  }

  public static Network defaultNetwork() {
    Network n = Network.bundled();
    return n != null ? n : Network.random(1);
  }

  /** True when the app has a trained network. */
  public static boolean isTrained() {
    return Network.bundled() != null;
  }

  @Override
  public String name() {
    return "Aleph_0";
  }

  @Override
  public Move chooseMove(Position position, List<Long> history, AtomicBoolean stop) {
    Mcts mcts = new Mcts(net, new Random());
    last = mcts.search(position, history, maxSimulations, thinkMs, stop);
    return last.best();
  }

  /** The last search (moves, visits, value), for the UCI engine's info line. */
  public Mcts.Result lastResult() {
    return last;
  }
}
