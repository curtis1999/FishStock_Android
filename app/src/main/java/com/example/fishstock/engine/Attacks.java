package com.example.fishstock.engine;

/**
 * Precomputed move tables. Built once; every lookup afterwards is an array access.
 */
public final class Attacks {
  /** Direction indices into {@link #RAYS}. 0-3 are straight lines, 4-7 are diagonals. */
  public static final int NORTH = 0, SOUTH = 1, EAST = 2, WEST = 3;
  public static final int NORTH_EAST = 4, NORTH_WEST = 5, SOUTH_EAST = 6, SOUTH_WEST = 7;

  private static final int[] DF = {0, 0, 1, -1, 1, -1, 1, -1};
  private static final int[] DR = {1, -1, 0, 0, 1, 1, -1, -1};

  /** KNIGHT[sq] = squares a knight on sq attacks. */
  public static final int[][] KNIGHT = new int[64][];
  /** KING[sq] = squares a king on sq attacks. */
  public static final int[][] KING = new int[64][];
  /** RAYS[sq][dir] = squares from sq outwards in direction dir, nearest first. */
  public static final int[][][] RAYS = new int[64][8][];
  /** PAWN_ATTACKS[0 = white, 1 = black][sq] = squares a pawn on sq attacks. */
  public static final int[][][] PAWN_ATTACKS = new int[2][64][];

  static {
    int[][] knightSteps = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};
    for (int sq = 0; sq < 64; sq++) {
      int f = Square.file(sq);
      int r = Square.rank(sq);
      KNIGHT[sq] = collect(f, r, knightSteps);
      int[][] kingSteps = new int[8][];
      for (int d = 0; d < 8; d++) kingSteps[d] = new int[] {DF[d], DR[d]};
      KING[sq] = collect(f, r, kingSteps);
      for (int d = 0; d < 8; d++) {
        int len = 0;
        int[] tmp = new int[7];
        int nf = f + DF[d];
        int nr = r + DR[d];
        while (Square.onBoard(nf, nr)) {
          tmp[len++] = Square.of(nf, nr);
          nf += DF[d];
          nr += DR[d];
        }
        int[] ray = new int[len];
        System.arraycopy(tmp, 0, ray, 0, len);
        RAYS[sq][d] = ray;
      }
      PAWN_ATTACKS[0][sq] = collect(f, r, new int[][] {{-1, 1}, {1, 1}});
      PAWN_ATTACKS[1][sq] = collect(f, r, new int[][] {{-1, -1}, {1, -1}});
    }
  }

  private Attacks() {}

  private static int[] collect(int f, int r, int[][] steps) {
    int[] tmp = new int[steps.length];
    int n = 0;
    for (int[] s : steps) {
      if (Square.onBoard(f + s[0], r + s[1])) tmp[n++] = Square.of(f + s[0], r + s[1]);
    }
    int[] out = new int[n];
    System.arraycopy(tmp, 0, out, 0, n);
    return out;
  }

  public static boolean isDiagonal(int dir) {
    return dir >= 4;
  }

  /** Can a piece of this type slide along this direction? */
  public static boolean slidesAlong(int pieceType, int dir) {
    if (pieceType == Piece.QUEEN) return true;
    if (pieceType == Piece.ROOK) return dir < 4;
    if (pieceType == Piece.BISHOP) return dir >= 4;
    return false;
  }

  /** The direction from a to b if they share a line or diagonal, otherwise -1. */
  public static int direction(int a, int b) {
    int df = Square.file(b) - Square.file(a);
    int dr = Square.rank(b) - Square.rank(a);
    if (a == b) return -1;
    if (df != 0 && dr != 0 && Math.abs(df) != Math.abs(dr)) return -1;
    int sf = Integer.signum(df);
    int sr = Integer.signum(dr);
    for (int d = 0; d < 8; d++) {
      if (DF[d] == sf && DR[d] == sr) return d;
    }
    return -1;
  }
}
