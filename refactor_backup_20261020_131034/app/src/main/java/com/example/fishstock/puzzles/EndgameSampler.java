package com.example.fishstock.puzzles;

import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Square;

import java.util.Random;

/**
 * Makes up endgame positions for the puzzle generator. Sample games rarely last long enough to
 * reach an endgame, so endgame puzzles start from positions built here:
 *  - {@link #breakthrough}: the textbook pawn breakthrough (a quarter of the time: three pawns facing three, the
 *    attacker's on the 5th rank), shifted along the board, kings placed at random, sometimes with
 *    extra blocked pawns elsewhere. Whether it really works is left to the engine to confirm.
 *  - {@link #randomEnding}: kings and a handful of pawns per side, sometimes with a rook or a
 *    minor piece each.
 * Every position returned is legal (kings apart, side not to move not in check, no pawns on the
 * first or last rank).
 */
public final class EndgameSampler {
  private EndgameSampler() {}

  /** A breakthrough setup a quarter of the time, a random ending otherwise. */
  public static Position next(Random r) {
    for (int attempt = 0; attempt < 200; attempt++) {
      Position p = r.nextInt(4) == 0 ? breakthrough(r) : randomEnding(r);
      if (p != null) return p;
    }
    return Position.fromFen("8/5k2/3p4/1p1P4/1P6/5K2/8/8 w - - 0 1");
  }

  /** Three attacking pawns on the 5th rank against three defenders on the 7th; or null. */
  public static Position breakthrough(Random r) {
    int[] b = new int[64];
    boolean white = r.nextBoolean(); // the side that breaks through, and is to move
    int x = r.nextInt(6);            // pawns on files x, x+1, x+2
    for (int f = x; f < x + 3; f++) {
      b[sq(f, 4, white)] = Piece.make(Piece.PAWN, white);
      b[sq(f, 6, white)] = Piece.make(Piece.PAWN, !white);
    }
    // Kings on the other side of the board, attacker's a little closer to home.
    boolean right = x <= 2;
    int kf1 = right ? 5 + r.nextInt(3) : r.nextInt(3);
    int kf2 = right ? 4 + r.nextInt(4) : r.nextInt(4);
    b[sq(kf1, r.nextInt(3), white)] = Piece.make(Piece.KING, white);
    int defenderKing = sq(kf2, 2 + r.nextInt(4), white);
    if (b[defenderKing] != Piece.EMPTY) return null;
    b[defenderKing] = Piece.make(Piece.KING, !white);
    if (r.nextInt(3) == 0) {
      // A blocked pair on the king's wing, for realism.
      int f = right ? 5 + r.nextInt(3) : r.nextInt(3);
      int rr = 2 + r.nextInt(2);
      int a = sq(f, rr, white);
      int d = sq(f, rr + 1, white);
      if (b[a] == Piece.EMPTY && b[d] == Piece.EMPTY) {
        b[a] = Piece.make(Piece.PAWN, white);
        b[d] = Piece.make(Piece.PAWN, !white);
      }
    }
    return build(b, white);
  }

  /** Kings, 2-5 pawns each, and sometimes a rook or a minor piece each; or null. */
  public static Position randomEnding(Random r) {
    int[] b = new int[64];
    if (!place(b, Piece.KING, r, 0, 8) || !place(b, -Piece.KING, r, 0, 8)) return null;
    int whitePawns = 2 + r.nextInt(4);
    int blackPawns = Math.max(1, whitePawns + r.nextInt(3) - 1);
    for (int i = 0; i < whitePawns; i++) if (!place(b, Piece.PAWN, r, 1, 6)) return null;
    for (int i = 0; i < blackPawns; i++) if (!place(b, -Piece.PAWN, r, 2, 7)) return null;
    int extra = r.nextInt(4);
    if (extra == 1) {
      if (!place(b, Piece.ROOK, r, 0, 8) || !place(b, -Piece.ROOK, r, 0, 8)) return null;
    } else if (extra == 2) {
      int w = r.nextBoolean() ? Piece.KNIGHT : Piece.BISHOP;
      int k = r.nextBoolean() ? Piece.KNIGHT : Piece.BISHOP;
      if (!place(b, w, r, 0, 8) || !place(b, -k, r, 0, 8)) return null;
    }
    return build(b, r.nextBoolean());
  }

  // ---------------------------------------------------------------- helpers

  /** Square at file f and rank counted from {@code white}'s side. */
  private static int sq(int f, int relativeRank, boolean white) {
    return Square.of(f, white ? relativeRank : 7 - relativeRank);
  }

  /** Puts the piece on a random empty square with rank in [minRank, maxRank). */
  private static boolean place(int[] b, int piece, Random r, int minRank, int maxRank) {
    for (int attempt = 0; attempt < 50; attempt++) {
      int s = Square.of(r.nextInt(8), minRank + r.nextInt(maxRank - minRank));
      if (b[s] == Piece.EMPTY) {
        b[s] = piece;
        return true;
      }
    }
    return false;
  }

  private static Position build(int[] b, boolean whiteToMove) {
    int wk = -1;
    int bk = -1;
    for (int s = 0; s < 64; s++) {
      if (b[s] == Piece.KING) wk = s;
      if (b[s] == -Piece.KING) bk = s;
      if (Piece.type(b[s]) == Piece.PAWN && (Square.rank(s) == 0 || Square.rank(s) == 7)) return null;
    }
    if (wk < 0 || bk < 0 || Square.distance(wk, bk) < 2) return null;
    StringBuilder fen = new StringBuilder();
    for (int rank = 7; rank >= 0; rank--) {
      int empty = 0;
      for (int f = 0; f < 8; f++) {
        int p = b[Square.of(f, rank)];
        if (p == Piece.EMPTY) {
          empty++;
          continue;
        }
        if (empty > 0) fen.append(empty);
        empty = 0;
        fen.append(Piece.fenChar(p));
      }
      if (empty > 0) fen.append(empty);
      if (rank > 0) fen.append('/');
    }
    fen.append(whiteToMove ? " w - - 0 40" : " b - - 0 40");
    Position p = Position.fromFen(fen.toString());
    if (p.inCheck(!whiteToMove) || p.inCheck(whiteToMove)) return null;
    return p;
  }
}
