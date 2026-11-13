package com.example.fishstock.engine;

/**
 * Bitboards: a set of squares stored as the 64 bits of a {@code long} (bit 0 = a1, bit 63 = h8).
 *
 * "Where are White's knights?" is one number, and questions about many squares at once become a
 * single CPU instruction: {@code knights & centre} (which knights are central), {@code attacks &
 * ~ownPieces} (where can this piece go), {@code Long.bitCount(x)} (how many). That's what makes
 * move generation and the evaluation several times faster than looping over 64 squares.
 *
 * Sliding pieces use the classic "ray" method: the attack ray in each direction is a lookup, and
 * the first piece blocking it is found with one bit-scan instruction.
 */
public final class Bitboards {
  private Bitboards() {}

  public static final long FILE_A = 0x0101010101010101L;
  public static final long RANK_1 = 0xFFL;
  public static final long LIGHT_SQUARES = 0x55AA55AA55AA55AAL;

  /** KNIGHT[sq], KING[sq]: squares attacked from sq. */
  public static final long[] KNIGHT = new long[64];
  public static final long[] KING = new long[64];
  /** PAWN[0 = White, 1 = Black][sq]: squares a pawn of that colour on sq attacks. */
  public static final long[][] PAWN = new long[2][64];
  /** RAY[dir][sq]: every square from sq outwards in direction dir (Attacks.NORTH ... SOUTH_WEST). */
  public static final long[][] RAY = new long[8][64];
  /** BETWEEN[a][b]: squares strictly between a and b on a shared line, else 0. */
  public static final long[][] BETWEEN = new long[64][64];
  /** LINE[a][b]: the whole line (edge to edge) through a and b, else 0. */
  public static final long[][] LINE = new long[64][64];
  /** FILE[f], RANK[r]. */
  public static final long[] FILE = new long[8];
  public static final long[] RANK = new long[8];
  /** The king's square and the squares around it. */
  public static final long[] KING_ZONE = new long[64];

  // Directions that run towards higher square numbers (the first blocker is the lowest bit).
  private static final boolean[] POSITIVE = {true, false, true, false, true, true, false, false};
  /** N<->S, E<->W, NE<->SW, NW<->SE (direction numbers as in {@link Attacks}). */
  private static final int[] OPPOSITE = {1, 0, 3, 2, 7, 6, 5, 4};

  static {
    for (int i = 0; i < 8; i++) {
      FILE[i] = FILE_A << i;
      RANK[i] = RANK_1 << (8 * i);
    }
    for (int sq = 0; sq < 64; sq++) {
      for (int t : Attacks.KNIGHT[sq]) KNIGHT[sq] |= bit(t);
      for (int t : Attacks.KING[sq]) KING[sq] |= bit(t);
      for (int t : Attacks.PAWN_ATTACKS[0][sq]) PAWN[0][sq] |= bit(t);
      for (int t : Attacks.PAWN_ATTACKS[1][sq]) PAWN[1][sq] |= bit(t);
      for (int d = 0; d < 8; d++) for (int t : Attacks.RAYS[sq][d]) RAY[d][sq] |= bit(t);
      KING_ZONE[sq] = KING[sq] | bit(sq);
    }
    for (int a = 0; a < 64; a++) {
      for (int d = 0; d < 8; d++) {
        long between = 0;
        for (int t : Attacks.RAYS[a][d]) {
          BETWEEN[a][t] = between;
          LINE[a][t] = RAY[d][a] | RAY[opposite(d)][a] | bit(a);
          between |= bit(t);
        }
      }
    }
  }

  private static int opposite(int d) {
    return OPPOSITE[d];
  }

  public static long bit(int sq) {
    return 1L << sq;
  }

  /** Lowest set square (call only when b != 0). */
  public static int first(long b) {
    return Long.numberOfTrailingZeros(b);
  }

  public static int count(long b) {
    return Long.bitCount(b);
  }

  /** Squares a slider attacks along one direction, stopping at (and including) the first piece. */
  public static long rayAttacks(int dir, int sq, long occupied) {
    long ray = RAY[dir][sq];
    long blockers = ray & occupied;
    if (blockers != 0) {
      int b = POSITIVE[dir] ? Long.numberOfTrailingZeros(blockers) : 63 - Long.numberOfLeadingZeros(blockers);
      ray ^= RAY[dir][b];
    }
    return ray;
  }

  public static long rookAttacks(int sq, long occupied) {
    return rayAttacks(0, sq, occupied) | rayAttacks(1, sq, occupied)
        | rayAttacks(2, sq, occupied) | rayAttacks(3, sq, occupied);
  }

  public static long bishopAttacks(int sq, long occupied) {
    return rayAttacks(4, sq, occupied) | rayAttacks(5, sq, occupied)
        | rayAttacks(6, sq, occupied) | rayAttacks(7, sq, occupied);
  }

  public static long queenAttacks(int sq, long occupied) {
    return rookAttacks(sq, occupied) | bishopAttacks(sq, occupied);
  }

  /** The first piece met from sq in direction dir, or -1. */
  public static int firstBlocker(int dir, int sq, long occupied) {
    long blockers = RAY[dir][sq] & occupied;
    if (blockers == 0) return -1;
    return POSITIVE[dir] ? Long.numberOfTrailingZeros(blockers) : 63 - Long.numberOfLeadingZeros(blockers);
  }

  /** Every square on a file ahead of the given squares (towards the 8th rank for White). */
  public static long fillForward(long b, boolean white) {
    if (white) {
      b |= b << 8;
      b |= b << 16;
      b |= b << 32;
    } else {
      b |= b >>> 8;
      b |= b >>> 16;
      b |= b >>> 32;
    }
    return b;
  }
}
