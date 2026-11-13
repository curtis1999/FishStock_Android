package com.example.fishstock.engine;

/**
 * Squares are numbered 0..63: a1 = 0, b1 = 1, ..., h1 = 7, a2 = 8, ..., h8 = 63.
 * file 0 is the a-file, rank 0 is White's back rank.
 */
public final class Square {
  public static final int NONE = -1;

  private Square() {}

  public static int of(int file, int rank) {
    return rank * 8 + file;
  }

  public static int file(int sq) {
    return sq & 7;
  }

  public static int rank(int sq) {
    return sq >> 3;
  }

  public static boolean onBoard(int file, int rank) {
    return file >= 0 && file < 8 && rank >= 0 && rank < 8;
  }

  /** a1 is a dark square. */
  public static boolean isLight(int sq) {
    return ((file(sq) + rank(sq)) & 1) == 1;
  }

  /** Rank counted from the given side's back rank (0 = own back rank, 7 = promotion rank). */
  public static int relativeRank(int sq, boolean white) {
    return white ? rank(sq) : 7 - rank(sq);
  }

  /** Chebyshev (king-move) distance. */
  public static int distance(int a, int b) {
    return Math.max(Math.abs(file(a) - file(b)), Math.abs(rank(a) - rank(b)));
  }

  /** King steps from the nearest of the four centre squares (0 in the centre, 3 in a corner). */
  public static int centreDistance(int sq) {
    int f = file(sq);
    int r = rank(sq);
    int df = f < 4 ? 3 - f : f - 4;
    int dr = r < 4 ? 3 - r : r - 4;
    return Math.max(df, dr);
  }

  public static String name(int sq) {
    if (sq < 0) return "-";
    return "" + (char) ('a' + file(sq)) + (char) ('1' + rank(sq));
  }

  public static int parse(String name) {
    if (name == null || name.length() != 2 || name.equals("-")) return NONE;
    int f = name.charAt(0) - 'a';
    int r = name.charAt(1) - '1';
    if (!onBoard(f, r)) throw new IllegalArgumentException("Bad square: " + name);
    return of(f, r);
  }
}
