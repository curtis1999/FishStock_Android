package com.example.fishstock.arena;

import com.example.fishstock.engine.GameResult;

import java.util.Collections;
import java.util.List;

/** One finished Arena game. */
public final class GameRecord {
  public final String whiteName;
  public final String blackName;
  public final GameResult result;
  public final int plies;
  /** Moves in algebraic notation ("1. e4 e5 2. ..."), handy for reviewing training games. */
  public final String moves;
  /** Where the game started (FEN) and every move in UCI form, so it can be replayed and analysed. */
  public final String startFen;
  public final List<String> movesUci;

  public GameRecord(String whiteName, String blackName, GameResult result, int plies, String moves) {
    this(whiteName, blackName, result, plies, moves, null, Collections.<String>emptyList());
  }

  public GameRecord(String whiteName, String blackName, GameResult result, int plies, String moves,
                    String startFen, List<String> movesUci) {
    this.whiteName = whiteName;
    this.blackName = blackName;
    this.result = result;
    this.plies = plies;
    this.moves = moves;
    this.startFen = startFen;
    this.movesUci = movesUci == null ? Collections.<String>emptyList() : Collections.unmodifiableList(movesUci);
  }

  /** "e2e4 e7e5 ..." */
  public String movesUciText() {
    StringBuilder sb = new StringBuilder();
    for (String m : movesUci) sb.append(m).append(' ');
    return sb.toString().trim();
  }

  /** "1-0", "0-1" or "1/2-1/2". */
  public String scoreText() {
    return result.score > 0 ? "1-0" : result.score < 0 ? "0-1" : "1/2-1/2";
  }
}
