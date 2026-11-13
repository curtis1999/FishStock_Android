package com.example.fishstock.eval;

/**
 * Groups of evaluation terms. Each category gets one slider on the "Make your own Agent" screen,
 * and scaling a category scales every {@link Term} in it at once.
 */
public enum Category {
  MATERIAL("Cares about material"),
  PIECE_SAFETY("Cares about hanging pieces"),
  MOBILITY("Cares about mobility"),
  PINS_AND_REVEALS("Cares about pins & discovered attacks"),
  KING_SAFETY("Cares about king safety"),
  PAWN_STRUCTURE("Cares about pawn structure"),
  OUTPOSTS("Cares about outposts"),
  PIECE_PLACEMENT("Cares about piece placement"),
  INITIATIVE("Cares about the initiative"),
  ENDGAME("Cares about endgame technique");

  public final String label;

  Category(String label) {
    this.label = label;
  }
}
