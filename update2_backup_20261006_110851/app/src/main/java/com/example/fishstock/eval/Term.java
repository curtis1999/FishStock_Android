package com.example.fishstock.eval;

/**
 * Every number the evaluation function uses, in one table.
 *
 * Units are pawns (1.0 = one pawn), the same scale as the original piece values.
 * Each term has:
 *  - a default (the hand-tuned first guess used by Simple and MinMax),
 *  - a min/max range (for the custom-agent sliders and any future training run),
 *  - a kind: ADD terms are added or subtracted when a feature is present;
 *    MULTIPLY terms scale a piece's value (e.g. a pinned piece is worth 0.8 of normal).
 *
 * To tweak the engine by hand, change a default here. To train it, search over
 * {@link EvalWeights#toVector()} - nothing else needs to change.
 */
public enum Term {
  // ---------------------------------------------------------------- material
  PAWN_VALUE(Category.MATERIAL, Kind.ADD, 1.00, 0.5, 2.0, "Value of a pawn"),
  KNIGHT_VALUE(Category.MATERIAL, Kind.ADD, 3.05, 2.0, 4.5, "Value of a knight"),
  BISHOP_VALUE(Category.MATERIAL, Kind.ADD, 3.33, 2.0, 4.5, "Value of a bishop"),
  ROOK_VALUE(Category.MATERIAL, Kind.ADD, 5.63, 4.0, 7.0, "Value of a rook"),
  QUEEN_VALUE(Category.MATERIAL, Kind.ADD, 9.50, 7.0, 12.0, "Value of a queen"),

  // ---------------------------------------------------------------- 1. attackers / defenders
  EN_PRISE_FACTOR(Category.PIECE_SAFETY, Kind.ADD, 0.80, 0.0, 1.0,
      "Share of the exchange loss counted when the opponent is to move and can win material"),
  SECOND_THREAT_FACTOR(Category.PIECE_SAFETY, Kind.ADD, 0.40, 0.0, 1.0,
      "Share counted for the 2nd-biggest threat against the side to move (it can only save one piece)"),
  DEFENDED_PIECE_BONUS(Category.PIECE_SAFETY, Kind.ADD, 0.03, 0.0, 0.2, "Per piece defended at least once"),
  LOOSE_PIECE_PENALTY(Category.PIECE_SAFETY, Kind.ADD, 0.05, 0.0, 0.3, "Per minor/major piece with no defender"),

  // ---------------------------------------------------------------- 2. mobility (small on purpose)
  KNIGHT_MOBILITY(Category.MOBILITY, Kind.ADD, 0.06, 0.0, 0.2, "Per safe knight move above/below 4"),
  BISHOP_MOBILITY(Category.MOBILITY, Kind.ADD, 0.05, 0.0, 0.2, "Per safe bishop move above/below 6"),
  ROOK_MOBILITY(Category.MOBILITY, Kind.ADD, 0.03, 0.0, 0.15, "Per safe rook move above/below 7"),
  QUEEN_MOBILITY(Category.MOBILITY, Kind.ADD, 0.015, 0.0, 0.1, "Per safe queen move above/below 13"),

  // ---------------------------------------------------------------- 3. pins and reveal checkers
  PINNED_TO_KING_FACTOR(Category.PINS_AND_REVEALS, Kind.MULTIPLY, 0.80, 0.3, 1.0,
      "Value multiplier for a piece pinned to its king"),
  PINNED_TO_QUEEN_FACTOR(Category.PINS_AND_REVEALS, Kind.MULTIPLY, 0.90, 0.3, 1.0,
      "Value multiplier for a piece pinned to its queen"),
  REVEAL_CHECKER_BONUS(Category.PINS_AND_REVEALS, Kind.ADD, 0.50, 0.0, 2.0,
      "Piece whose move would uncover check (discovered check)"),
  REVEAL_QUEEN_ATTACKER_BONUS(Category.PINS_AND_REVEALS, Kind.ADD, 0.20, 0.0, 1.0,
      "Piece whose move would uncover an attack on the enemy queen"),

  // ---------------------------------------------------------------- 4. king safety (fades out in the endgame)
  PAWN_SHIELD_BONUS(Category.KING_SAFETY, Kind.ADD, 0.12, 0.0, 0.5, "Per pawn sheltering the king"),
  OPEN_FILE_NEAR_KING_PENALTY(Category.KING_SAFETY, Kind.ADD, 0.25, 0.0, 1.0,
      "Per file next to the king with no friendly pawn"),
  KING_ZONE_ATTACK_PENALTY(Category.KING_SAFETY, Kind.ADD, 0.08, 0.0, 0.4,
      "Per enemy attack on the squares around the king"),
  KING_FEW_ESCAPE_SQUARES_PENALTY(Category.KING_SAFETY, Kind.ADD, 0.15, 0.0, 0.8,
      "King has fewer than 2 safe squares"),
  XRAY_ON_KING_PENALTY(Category.KING_SAFETY, Kind.ADD, 0.15, 0.0, 0.6,
      "Per enemy rook/bishop/queen aimed at the king through pieces"),
  CASTLED_KING_BONUS(Category.KING_SAFETY, Kind.ADD, 0.30, 0.0, 1.0, "King tucked away on the wing"),
  LOST_CASTLING_PENALTY(Category.KING_SAFETY, Kind.ADD, 0.25, 0.0, 1.0,
      "King stuck in the middle with no castling rights left"),

  // ---------------------------------------------------------------- 5. pawn structure
  DOUBLED_PAWN_PENALTY(Category.PAWN_STRUCTURE, Kind.ADD, 0.20, 0.0, 0.6, "Per pawn with a friendly pawn ahead of it"),
  ISOLATED_PAWN_PENALTY(Category.PAWN_STRUCTURE, Kind.ADD, 0.25, 0.0, 0.6, "No friendly pawns on neighbouring files"),
  BACKWARD_PAWN_PENALTY(Category.PAWN_STRUCTURE, Kind.ADD, 0.15, 0.0, 0.5, "Pawn left behind whose advance square is guarded"),
  CONNECTED_PAWN_BONUS(Category.PAWN_STRUCTURE, Kind.ADD, 0.10, 0.0, 0.4, "Pawn side by side with, or defended by, a pawn"),
  CENTRAL_PAWN_BONUS(Category.PAWN_STRUCTURE, Kind.ADD, 0.15, 0.0, 0.5, "Pawn on d4/e4/d5/e5"),
  PASSED_PAWN_BONUS(Category.PAWN_STRUCTURE, Kind.ADD, 0.30, 0.0, 1.0, "No enemy pawn can stop it"),
  PASSED_PAWN_ADVANCE_BONUS(Category.PAWN_STRUCTURE, Kind.ADD, 1.00, 0.0, 3.0,
      "Extra for a passed pawn, growing with the square of how far it has advanced"),
  PROTECTED_PASSED_PAWN_BONUS(Category.PAWN_STRUCTURE, Kind.ADD, 0.15, 0.0, 0.6, "Passed pawn defended by a pawn"),
  ONE_STEP_FROM_PROMOTION_BONUS(Category.PAWN_STRUCTURE, Kind.ADD, 0.30, 0.0, 1.5, "Pawn on the 7th with the queening square free"),
  UNSTOPPABLE_PAWN_BONUS(Category.PAWN_STRUCTURE, Kind.ADD, 3.00, 0.0, 6.0,
      "Passed pawn the enemy king cannot catch (opponent has only pawns)"),

  // ---------------------------------------------------------------- outposts
  KNIGHT_OUTPOST_BONUS(Category.OUTPOSTS, Kind.ADD, 0.40, 0.0, 1.5, "Knight on a pawn-protected square no enemy pawn can attack"),
  BISHOP_OUTPOST_BONUS(Category.OUTPOSTS, Kind.ADD, 0.20, 0.0, 1.0, "Bishop on an outpost"),
  ADVANCED_OUTPOST_BONUS(Category.OUTPOSTS, Kind.ADD, 0.15, 0.0, 0.8, "Extra for an outpost on the 6th rank"),

  // ---------------------------------------------------------------- piece placement
  BISHOP_PAIR_BONUS(Category.PIECE_PLACEMENT, Kind.ADD, 0.40, 0.0, 1.0, "Both bishops still on the board"),
  KNIGHT_CENTRAL_BONUS(Category.PIECE_PLACEMENT, Kind.ADD, 0.25, 0.0, 0.8, "Knight on d4/e4/d5/e5"),
  KNIGHT_RIM_PENALTY(Category.PIECE_PLACEMENT, Kind.ADD, 0.20, 0.0, 0.8, "A knight on the rim is dim"),
  KNIGHT_CORNER_PENALTY(Category.PIECE_PLACEMENT, Kind.ADD, 0.20, 0.0, 0.8, "Extra for a knight in the corner"),
  BISHOP_LONG_DIAGONAL_BONUS(Category.PIECE_PLACEMENT, Kind.ADD, 0.15, 0.0, 0.6, "Bishop on a1-h8 or h1-a8"),
  ROOK_OPEN_FILE_BONUS(Category.PIECE_PLACEMENT, Kind.ADD, 0.25, 0.0, 0.8, "Rook on a file with no pawns"),
  ROOK_SEMI_OPEN_FILE_BONUS(Category.PIECE_PLACEMENT, Kind.ADD, 0.12, 0.0, 0.5, "Rook on a file with only enemy pawns"),
  ROOK_SEVENTH_RANK_BONUS(Category.PIECE_PLACEMENT, Kind.ADD, 0.30, 0.0, 1.0, "Rook on the opponent's 2nd rank"),
  CONNECTED_ROOKS_BONUS(Category.PIECE_PLACEMENT, Kind.ADD, 0.15, 0.0, 0.6, "Rooks defending each other"),
  UNDEVELOPED_MINOR_PENALTY(Category.PIECE_PLACEMENT, Kind.ADD, 0.15, 0.0, 0.6,
      "Knight/bishop still on its starting square (opening only)"),
  EARLY_QUEEN_PENALTY(Category.PIECE_PLACEMENT, Kind.ADD, 0.20, 0.0, 0.8,
      "Queen out before the minor pieces (opening only)"),

  // ---------------------------------------------------------------- initiative
  SIDE_TO_MOVE_BONUS(Category.INITIATIVE, Kind.ADD, 0.10, 0.0, 0.5, "Having the move"),
  IN_CHECK_PENALTY(Category.INITIATIVE, Kind.ADD, 0.20, 0.0, 1.0, "Side to move is in check"),

  // ---------------------------------------------------------------- endgame
  KING_CENTRALISATION_BONUS(Category.ENDGAME, Kind.ADD, 0.10, 0.0, 0.4,
      "Per step the king is closer to the centre (endgame only)"),
  MOP_UP_EDGE_BONUS(Category.ENDGAME, Kind.ADD, 0.15, 0.0, 0.6,
      "When winning: per step the enemy king is pushed from the centre"),
  MOP_UP_KING_PROXIMITY_BONUS(Category.ENDGAME, Kind.ADD, 0.08, 0.0, 0.4,
      "When winning: per step our king is closer to the enemy king"),
  DRAWISH_SCALE(Category.ENDGAME, Kind.MULTIPLY, 0.25, 0.0, 1.0,
      "Multiplier when the stronger side has no pawns and too little extra to win (e.g. KR v KB)");

  public enum Kind { ADD, MULTIPLY }

  public final Category category;
  public final Kind kind;
  public final double defaultValue;
  public final double min;
  public final double max;
  public final String description;

  Term(Category category, Kind kind, double defaultValue, double min, double max, String description) {
    this.category = category;
    this.kind = kind;
    this.defaultValue = defaultValue;
    this.min = min;
    this.max = max;
    this.description = description;
  }

  public double clamp(double v) {
    return Math.max(min, Math.min(max, v));
  }
}
