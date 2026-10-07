package com.example.fishstock.engine;

/**
 * Game-ending rules that depend only on the current position.
 * Repetition needs the game history, so it lives in {@link Game}.
 */
public final class Rules {
  private Rules() {}

  public static boolean isCheckmate(Position pos) {
    return pos.inCheck() && !MoveGenerator.hasLegalMove(pos);
  }

  public static boolean isStalemate(Position pos) {
    return !pos.inCheck() && !MoveGenerator.hasLegalMove(pos);
  }

  public static boolean isFiftyMoveDraw(Position pos) {
    return pos.halfmoveClock() >= 100;
  }

  /**
   * Neither side can possibly checkmate: K v K, K+minor v K, or K+B v K+B with bishops
   * on the same colour squares.
   */
  public static boolean isInsufficientMaterial(Position pos) {
    int minors = 0;
    int whiteBishopColour = -1;
    int blackBishopColour = -1;
    int bishops = 0;
    for (int sq = 0; sq < 64; sq++) {
      int p = pos.pieceAt(sq);
      switch (Piece.type(p)) {
        case Piece.PAWN:
        case Piece.ROOK:
        case Piece.QUEEN:
          return false;
        case Piece.KNIGHT:
          minors++;
          break;
        case Piece.BISHOP:
          minors++;
          bishops++;
          int colour = Square.isLight(sq) ? 1 : 0;
          if (p > 0) whiteBishopColour = colour;
          else blackBishopColour = colour;
          break;
        default:
          break;
      }
    }
    if (minors <= 1) return true;
    return minors == 2 && bishops == 2 && whiteBishopColour >= 0 && whiteBishopColour == blackBishopColour;
  }
}
