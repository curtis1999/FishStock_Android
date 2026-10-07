package com.example.fishstock.engine;

/**
 * Pieces are stored as plain ints on the board: positive for White, negative for Black,
 * 0 for an empty square. The absolute value is the piece type.
 *
 * Using ints (instead of one object per piece) keeps copying and searching positions cheap,
 * which matters once MinMax starts looking several moves ahead on a phone.
 */
public final class Piece {
  public static final int EMPTY = 0;
  public static final int PAWN = 1;
  public static final int KNIGHT = 2;
  public static final int BISHOP = 3;
  public static final int ROOK = 4;
  public static final int QUEEN = 5;
  public static final int KING = 6;

  /** Piece types a pawn may promote to, best first. */
  public static final int[] PROMOTION_TYPES = {QUEEN, ROOK, BISHOP, KNIGHT};

  private static final String[] NAMES = {"", "Pawn", "Knight", "Bishop", "Rook", "Queen", "King"};
  private static final char[] SYMBOLS = {'.', 'P', 'N', 'B', 'R', 'Q', 'K'};

  private Piece() {}

  public static int type(int piece) {
    return piece < 0 ? -piece : piece;
  }

  public static boolean isWhite(int piece) {
    return piece > 0;
  }

  public static boolean isBlack(int piece) {
    return piece < 0;
  }

  /** True when the piece belongs to the given side. Empty squares belong to nobody. */
  public static boolean isColor(int piece, boolean white) {
    return white ? piece > 0 : piece < 0;
  }

  public static int make(int type, boolean white) {
    return white ? type : -type;
  }

  /** "Pawn", "Knight", ... for a piece type (sign ignored). */
  public static String name(int piece) {
    return NAMES[type(piece)];
  }

  /** FEN letter: upper case for White, lower case for Black. */
  public static char fenChar(int piece) {
    char c = SYMBOLS[type(piece)];
    return piece < 0 ? Character.toLowerCase(c) : c;
  }

  /** Upper-case letter used in algebraic notation (N, B, R, Q, K; P for pawns). */
  public static char letter(int piece) {
    return SYMBOLS[type(piece)];
  }

  public static int fromFenChar(char c) {
    boolean white = Character.isUpperCase(c);
    switch (Character.toUpperCase(c)) {
      case 'P': return make(PAWN, white);
      case 'N': return make(KNIGHT, white);
      case 'B': return make(BISHOP, white);
      case 'R': return make(ROOK, white);
      case 'Q': return make(QUEEN, white);
      case 'K': return make(KING, white);
      default: throw new IllegalArgumentException("Bad piece char: " + c);
    }
  }

  /** Conventional 1/3/3/5/9 values, used for the on-screen material score only. */
  public static int displayValue(int piece) {
    switch (type(piece)) {
      case PAWN: return 1;
      case KNIGHT:
      case BISHOP: return 3;
      case ROOK: return 5;
      case QUEEN: return 9;
      default: return 0;
    }
  }
}
