package com.example.fishstock.engine;

/** How a game stands, and if it is over, why. */
public enum GameResult {
  ONGOING(0, "", false),
  WHITE_CHECKMATES(1, "CHECKMATE!!", true),
  BLACK_CHECKMATES(-1, "CHECKMATE!!", true),
  STALEMATE(0, "STALEMATE. THE GAME ENDS IN A DRAW", true),
  THREEFOLD_REPETITION(0, "DRAW BY REPETITION", true),
  FIFTY_MOVE_RULE(0, "DRAW BY THE 50 MOVE RULE", true),
  INSUFFICIENT_MATERIAL(0, "DRAW BY INSUFFICIENT MATERIAL", true),
  DRAW_AGREED(0, "DRAW AGREED", true),
  WHITE_RESIGNS(-1, "WHITE RESIGNS", true),
  BLACK_RESIGNS(1, "BLACK RESIGNS", true),
  /** Arena only: game hit the move limit and was scored on the board. */
  ADJUDICATED_WHITE(1, "WHITE WINS ON ADJUDICATION", true),
  ADJUDICATED_BLACK(-1, "BLACK WINS ON ADJUDICATION", true),
  ADJUDICATED_DRAW(0, "DRAW ON ADJUDICATION", true);

  /** +1 White won, -1 Black won, 0 draw (or ongoing). */
  public final int score;
  public final String message;
  public final boolean isOver;

  GameResult(int score, String message, boolean isOver) {
    this.score = score;
    this.message = message;
    this.isOver = isOver;
  }

  public boolean whiteWon() {
    return isOver && score > 0;
  }

  public boolean blackWon() {
    return isOver && score < 0;
  }

  public boolean isDraw() {
    return isOver && score == 0;
  }

  /** 1 if the given side won, -1 if it lost, 0 for a draw. */
  public int scoreFor(boolean white) {
    return white ? score : -score;
  }
}
