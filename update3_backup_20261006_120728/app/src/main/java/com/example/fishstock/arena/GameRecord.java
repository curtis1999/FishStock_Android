package com.example.fishstock.arena;

import com.example.fishstock.engine.GameResult;

/** One finished Arena game. */
public final class GameRecord {
  public final String whiteName;
  public final String blackName;
  public final GameResult result;
  public final int plies;
  /** Moves in algebraic notation ("1. e4 e5 2. ..."), handy for reviewing training games. */
  public final String moves;

  public GameRecord(String whiteName, String blackName, GameResult result, int plies, String moves) {
    this.whiteName = whiteName;
    this.blackName = blackName;
    this.result = result;
    this.plies = plies;
    this.moves = moves;
  }

  /** "1-0", "0-1" or "1/2-1/2". */
  public String scoreText() {
    return result.score > 0 ? "1-0" : result.score < 0 ? "0-1" : "1/2-1/2";
  }
}
