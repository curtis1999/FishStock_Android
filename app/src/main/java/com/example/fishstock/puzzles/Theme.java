package com.example.fishstock.puzzles;

/**
 * Tactical and strategic ideas a puzzle can be about. Each one has an explicit definition in
 * {@link ThemeDetector}; the text here is what the puzzle screen shows.
 */
public enum Theme {
  MATE("Checkmate", "Look for checkmate", "The line ends in checkmate."),
  BACK_RANK_MATE("Back-rank mate", "Look at the back rank",
      "Mate by a rook or queen on the king's back rank, its own pawns blocking the escape."),
  SMOTHERED_MATE("Smothered mate", "Look for a knight check",
      "Mate by a knight against a king boxed in by its own pieces."),
  DOUBLE_CHECK("Double check", "Look for a double check",
      "Two pieces give check at once, so the king has to move."),
  DISCOVERED_CHECK("Discovered check", "Look for a discovered check",
      "Moving one piece uncovers check from another (a reveal checker moves)."),
  DISCOVERED_ATTACK("Discovered attack", "Look for a discovered attack",
      "Moving one piece uncovers an attack by another on a valuable target."),
  FORK("Fork", "Look for a fork", "One piece attacks two or more enemy pieces at the same time."),
  PIN("Pin", "Look for a pin",
      "A piece can't move without exposing a more valuable piece (or the king) behind it."),
  SKEWER("Skewer", "Look for a skewer",
      "A valuable piece is attacked and, when it moves, the piece behind it falls."),
  REMOVE_THE_DEFENDER("Remove the defender", "Which piece is doing all the defending?",
      "A critical defender is captured or traded off, and what it guarded is then won."),
  DEFLECTION("Deflection", "Can you drag a defender away?",
      "A defender is forced to move away from what it was guarding."),
  ATTRACTION("Attraction", "Lure the king out",
      "A sacrifice forces the king onto a square where it can be attacked."),
  SACRIFICE("Sacrifice", "Don't be afraid to give material",
      "Material is given up on purpose because the follow-up wins more."),
  PROMOTION("Promotion", "Think about promoting", "A pawn promotes."),
  UNDERPROMOTION("Underpromotion", "A queen isn't always best", "A pawn promotes to something other than a queen."),
  PAWN_RACE("Pawn race", "Count who queens first",
      "Both sides have passed pawns in a pawn ending, and yours queens first."),
  PAWN_BREAKTHROUGH("Pawn breakthrough", "Break through with a pawn",
      "Pawns are sacrificed to create a passed pawn that can't be stopped."),
  QUIET_MOVE("Quiet move", "The best move isn't a check or a capture",
      "The key move is neither a check nor a capture."),
  ENDGAME("Endgame", "Count the moves", "Few pieces are left on the board.");

  public final String label;
  /** A nudge for the HINT button that doesn't give the move away. */
  public final String hint;
  public final String description;

  Theme(String label, String hint, String description) {
    this.label = label;
    this.hint = hint;
    this.description = description;
  }
}
