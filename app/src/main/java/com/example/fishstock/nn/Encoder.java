package com.example.fishstock.nn;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;

/**
 * Turns a position into the network's inputs, and moves into the network's policy outputs.
 *
 * Everything is seen from the side to move ("me"): when Black is to move the board is mirrored
 * top to bottom, so the network only ever learns to play as if moving up the board. That halves
 * what it has to learn.
 *
 * Inputs: 12 x 64 on/off squares (my pawn on e2, their knight on f6, ...) plus 4 castling bits
 * (my king side, my queen side, theirs), 772 in all. A position has about 32 inputs switched on,
 * so they're passed as a list of indices instead of 772 numbers.
 *
 * Policy outputs: one per (from square, to square) pair, 64 x 64 = 4096, mirrored the same way.
 * Under-promotions share the queen promotion's output.
 */
public final class Encoder {
  private Encoder() {}

  public static final int INPUTS = 12 * 64 + 4;
  public static final int POLICY = 64 * 64;
  private static final int CASTLING = 12 * 64;

  /** The square as the side to move sees it (Black's moves are mirrored to White's side). */
  public static int relative(int sq, boolean whiteToMove) {
    return whiteToMove ? sq : sq ^ 56;
  }

  /** Indices of the inputs that are switched on. */
  public static int[] features(Position pos) {
    boolean white = pos.whiteToMove();
    int[] tmp = new int[36];
    int n = 0;
    for (int sq = 0; sq < 64; sq++) {
      int p = pos.pieceAt(sq);
      if (p == Piece.EMPTY) continue;
      int type = Piece.type(p) - 1;                     // 0 pawn .. 5 king
      int plane = Piece.isWhite(p) == white ? type : type + 6;
      if (n == tmp.length) tmp = java.util.Arrays.copyOf(tmp, n * 2);
      tmp[n++] = plane * 64 + relative(sq, white);
    }
    int myK = white ? Position.WHITE_KINGSIDE : Position.BLACK_KINGSIDE;
    int myQ = white ? Position.WHITE_QUEENSIDE : Position.BLACK_QUEENSIDE;
    int theirK = white ? Position.BLACK_KINGSIDE : Position.WHITE_KINGSIDE;
    int theirQ = white ? Position.BLACK_QUEENSIDE : Position.WHITE_QUEENSIDE;
    int[] rights = {myK, myQ, theirK, theirQ};
    for (int i = 0; i < 4; i++) {
      if (pos.canCastle(rights[i])) {
        if (n == tmp.length) tmp = java.util.Arrays.copyOf(tmp, n * 2);
        tmp[n++] = CASTLING + i;
      }
    }
    return java.util.Arrays.copyOf(tmp, n);
  }

  /** The policy output for a move, seen from the side making it. */
  public static int moveIndex(Move m, boolean whiteToMove) {
    return relative(m.from, whiteToMove) * 64 + relative(m.to, whiteToMove);
  }
}
