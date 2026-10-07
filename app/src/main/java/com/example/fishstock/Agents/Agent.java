package com.example.fishstock.agents;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.Evaluator;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** A player. Agents are created fresh for each game. */
public interface Agent {

  String name();

  default boolean isHuman() {
    return false;
  }

  /**
   * Picks a move. Called on a background thread.
   *
   * @param position a private copy of the current position (safe to modify)
   * @param history  hashes of every position in the game so far, the current one last
   * @param stop     set to true when the answer is no longer wanted (undo, resign, screen closed)
   * @return a legal move, or null if there is none (or for humans)
   */
  Move chooseMove(Position position, List<Long> history, AtomicBoolean stop);

  /** The evaluation function this agent believes in, if it has one (used for draw offers). */
  default Evaluator evaluator() {
    return null;
  }
}
