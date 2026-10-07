package com.example.fishstock.endgame;

import com.example.fishstock.engine.Position;

/** Recognises positions that endgame tablebases have solved exactly. */
public final class SolvedEndgames {
  /** Syzygy tablebases cover every position with this many pieces or fewer (kings included). */
  public static final int MAX_TABLEBASE_PIECES = 7;

  private SolvedEndgames() {}

  /** At most 7 pieces and no castling rights left: the tablebase knows the perfect move. */
  public static boolean isSolved(Position pos) {
    return pos.pieceCount() <= MAX_TABLEBASE_PIECES && pos.castlingRights() == 0;
  }
}
