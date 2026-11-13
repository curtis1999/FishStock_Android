package com.example.fishstock.agents;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Position;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** A person tapping the board. The game screen supplies the moves. */
public final class Human implements Agent {
  @Override
  public String name() {
    return "Human";
  }

  @Override
  public boolean isHuman() {
    return true;
  }

  @Override
  public Move chooseMove(Position position, List<Long> history, AtomicBoolean stop) {
    return null;
  }
}
