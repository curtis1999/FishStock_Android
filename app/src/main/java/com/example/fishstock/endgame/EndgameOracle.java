package com.example.fishstock.endgame;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Position;

/**
 * Something that knows the perfect move in a solved endgame (a tablebase).
 * Agents ask the oracle first and fall back to their own thinking when it returns null.
 */
public interface EndgameOracle {

  /** True if this position is one the oracle can solve (e.g. at most 7 pieces). */
  boolean covers(Position pos);

  /** The best move, or null if unknown / unavailable (offline, timeout, ...). Blocking call. */
  Move bestMove(Position pos);

  /** An oracle that never knows anything. */
  EndgameOracle NONE = new EndgameOracle() {
    @Override
    public boolean covers(Position pos) {
      return false;
    }

    @Override
    public Move bestMove(Position pos) {
      return null;
    }
  };
}
