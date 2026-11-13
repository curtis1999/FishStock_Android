package com.example.fishstock.engine;

/**
 * Which squares a pre-move may aim at. A pre-move is chosen before the opponent has moved, so it
 * can't be checked against the legal moves yet: any square the piece could reach on an empty
 * board counts, including squares your own pieces stand on now (to recapture there if the
 * opponent takes) and empty diagonals for pawns (in case a piece lands there). Whether it is
 * really legal is checked when your turn comes.
 */
public final class Premoves {
  private Premoves() {}

  private static final int[][] KNIGHT = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};

  public static boolean canTarget(Position pos, int from, int to) {
    if (from < 0 || to < 0 || from == to) return false;
    int piece = pos.pieceAt(from);
    if (piece == Piece.EMPTY) return false;
    boolean white = Piece.isWhite(piece);
    int df = Square.file(to) - Square.file(from);
    int dr = Square.rank(to) - Square.rank(from);
    int adf = Math.abs(df);
    int adr = Math.abs(dr);
    switch (Piece.type(piece)) {
      case Piece.PAWN: {
        int forward = white ? dr : -dr;
        if (adf == 1 && forward == 1) return true;                    // captures
        if (df != 0) return false;
        if (forward == 1) return true;
        return forward == 2 && Square.relativeRank(from, white) == 1; // double step from the start
      }
      case Piece.KNIGHT:
        for (int[] k : KNIGHT) if (df == k[0] && dr == k[1]) return true;
        return false;
      case Piece.BISHOP:
        return adf == adr;
      case Piece.ROOK:
        return df == 0 || dr == 0;
      case Piece.QUEEN:
        return adf == adr || df == 0 || dr == 0;
      case Piece.KING:
        if (adf <= 1 && adr <= 1) return true;
        // Castling: two squares sideways from the king's starting square.
        return dr == 0 && adf == 2 && from == (white ? Square.parse("e1") : Square.parse("e8"));
      default:
        return false;
    }
  }
}
