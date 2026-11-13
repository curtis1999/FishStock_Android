package com.example.fishstock.agents;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/** Level 1: plays a random legal move. */
public final class Randy implements Agent {
  private final Random random;

  public Randy() {
    this(new Random());
  }

  public Randy(Random random) {
    this.random = random;
  }

  @Override
  public String name() {
    return "Randy";
  }

  @Override
  public Move chooseMove(Position position, List<Long> history, AtomicBoolean stop) {
    List<Move> moves = MoveGenerator.legalMoves(position);
    return moves.isEmpty() ? null : moves.get(random.nextInt(moves.size()));
  }
}
