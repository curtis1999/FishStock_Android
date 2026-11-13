package com.example.fishstock.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates moves for the side to move, with bitboards ({@link Bitboards}).
 *
 * Moves are made legal directly instead of by trying each one: before generating, it works out
 * which enemy pieces give check and which of our pieces are pinned to our king. Then
 *  - in double check only the king may move;
 *  - in single check a move must capture the checker or block the line to the king;
 *  - a pinned piece may only move along the line between its king and the pinner;
 *  - the king may not step onto an attacked square (looked at with the king lifted off the board,
 *    so it can't hide "behind itself" from a slider).
 * En passant is the one move checked by playing it, because it removes two pieces from a rank.
 */
public final class MoveGenerator {
  private MoveGenerator() {}

  /** All legal moves for the side to move. */
  public static List<Move> legalMoves(Position pos) {
    List<Move> out = new ArrayList<>(48);
    generateLegal(pos, out, false);
    return out;
  }

  /** Legal captures and promotions only (used by the quiescence search). */
  public static List<Move> legalTacticalMoves(Position pos) {
    List<Move> out = new ArrayList<>(16);
    generateLegal(pos, out, true);
    return out;
  }

  /** Legal moves of the piece standing on the given square. */
  public static List<Move> legalMovesFrom(Position pos, int from) {
    List<Move> out = new ArrayList<>();
    for (Move m : legalMoves(pos)) if (m.from == from) out.add(m);
    return out;
  }

  public static boolean hasLegalMove(Position pos) {
    return !legalMoves(pos).isEmpty();
  }

  /** Does this (legal) move give check? */
  public static boolean givesCheck(Position pos, Move m) {
    pos.makeMove(m);
    boolean check = pos.inCheck();
    pos.unmakeMove(m);
    return check;
  }

  /**
   * Pseudo-legal moves (may leave the king in check). Kept for tools that want them; the engine
   * itself uses {@link #legalMoves}.
   */
  public static void generate(Position pos, List<Move> out, boolean tacticalOnly) {
    generateAll(pos, out, tacticalOnly, -1L, 0L, false);
  }

  /** Legal moves straight away (see the class comment). */
  static void generateLegal(Position pos, List<Move> out, boolean tacticalOnly) {
    boolean white = pos.whiteToMove();
    int ksq = pos.kingSquare(white);
    long occ = pos.occupied();
    long checkers = pos.attackersTo(ksq, !white, occ);

    // Our pieces pinned to our king.
    long pinned = 0;
    long theirQueens = pos.pieces(Piece.QUEEN, !white);
    long snipers = (Bitboards.rookAttacks(ksq, 0) & (pos.pieces(Piece.ROOK, !white) | theirQueens))
        | (Bitboards.bishopAttacks(ksq, 0) & (pos.pieces(Piece.BISHOP, !white) | theirQueens));
    long own = pos.occupancy(white);
    while (snipers != 0) {
      int s = Long.numberOfTrailingZeros(snipers);
      snipers &= snipers - 1;
      long between = Bitboards.BETWEEN[ksq][s] & occ;
      if (between != 0 && (between & (between - 1)) == 0 && (between & own) != 0) pinned |= between;
    }

    // Squares a non-king move must land on.
    long target;
    int nChecks = Long.bitCount(checkers);
    if (nChecks == 0) {
      target = -1L;
    } else if (nChecks == 1) {
      int c = Long.numberOfTrailingZeros(checkers);
      target = checkers | Bitboards.BETWEEN[ksq][c];
    } else {
      target = 0; // double check: king moves only
    }
    generateAll(pos, out, tacticalOnly, target, pinned, true);
  }

  /**
   * @param target  squares non-king moves may go to (-1 = anywhere)
   * @param pinned  our pinned pieces (they keep to the line through the king)
   * @param legal   true: only legal moves (king safety, pins, en passant checked)
   */
  private static void generateAll(Position pos, List<Move> out, boolean tacticalOnly, long target, long pinned,
      boolean legal) {
    boolean white = pos.whiteToMove();
    int[] board = pos.boardArray();
    long own = pos.occupancy(white);
    long enemy = pos.occupancy(!white);
    long occ = own | enemy;
    int ksq = pos.kingSquare(white);
    long allowed = tacticalOnly ? enemy : ~own;

    // Every piece, from a1 upwards.
    long pieces = own;
    while (pieces != 0) {
      int from = Long.numberOfTrailingZeros(pieces);
      pieces &= pieces - 1;
      int piece = board[from];
      int type = Piece.type(piece);
      if (type == Piece.KING) {
        kingMoves(pos, board, from, piece, white, own, enemy, occ, out, tacticalOnly, legal);
        continue;
      }
      if (target == 0) continue;
      long lineMask = (pinned & (1L << from)) != 0 ? Bitboards.LINE[ksq][from] : -1L;
      if (type == Piece.PAWN) {
        pawnMoves(pos, board, from, piece, white, enemy, occ, target & lineMask, out, tacticalOnly, legal);
        continue;
      }
      long att;
      switch (type) {
        case Piece.KNIGHT: att = Bitboards.KNIGHT[from]; break;
        case Piece.BISHOP: att = Bitboards.bishopAttacks(from, occ); break;
        case Piece.ROOK: att = Bitboards.rookAttacks(from, occ); break;
        default: att = Bitboards.queenAttacks(from, occ); break;
      }
      att &= allowed & target & lineMask;
      while (att != 0) {
        int to = Long.numberOfTrailingZeros(att);
        att &= att - 1;
        out.add(new Move(from, to, piece, board[to], 0, Move.FLAG_NONE));
      }
    }
  }

  private static void kingMoves(Position pos, int[] board, int from, int piece, boolean white, long own, long enemy,
      long occ, List<Move> out, boolean tacticalOnly, boolean legal) {
    long att = Bitboards.KING[from] & (tacticalOnly ? enemy : ~own);
    long withoutKing = occ & ~(1L << from);
    while (att != 0) {
      int to = Long.numberOfTrailingZeros(att);
      att &= att - 1;
      if (legal && pos.attackersTo(to, !white, withoutKing) != 0) continue;
      out.add(new Move(from, to, piece, board[to], 0, Move.FLAG_NONE));
    }
    if (!tacticalOnly) castleMoves(pos, from, piece, white, out);
  }

  private static void pawnMoves(Position pos, int[] board, int from, int piece, boolean white, long enemy, long occ,
      long target, List<Move> out, boolean tacticalOnly, boolean legal) {
    int forward = white ? 8 : -8;
    int startRank = white ? 1 : 6;
    int lastRank = white ? 7 : 0;
    int one = from + forward;
    if (one >= 0 && one < 64 && (occ & (1L << one)) == 0) {
      if (Square.rank(one) == lastRank) {
        if ((target & (1L << one)) != 0) addPromotions(from, one, piece, Piece.EMPTY, out);
      } else if (!tacticalOnly) {
        if ((target & (1L << one)) != 0) out.add(new Move(from, one, piece, Piece.EMPTY, 0, Move.FLAG_NONE));
        int two = one + forward;
        if (Square.rank(from) == startRank && (occ & (1L << two)) == 0 && (target & (1L << two)) != 0) {
          out.add(new Move(from, two, piece, Piece.EMPTY, 0, Move.FLAG_DOUBLE_PUSH));
        }
      }
    }
    long caps = Bitboards.PAWN[white ? 0 : 1][from] & enemy & target;
    while (caps != 0) {
      int to = Long.numberOfTrailingZeros(caps);
      caps &= caps - 1;
      if (Square.rank(to) == lastRank) addPromotions(from, to, piece, board[to], out);
      else out.add(new Move(from, to, piece, board[to], 0, Move.FLAG_NONE));
    }
    int ep = pos.enPassantSquare();
    if (ep >= 0 && (Bitboards.PAWN[white ? 0 : 1][from] & (1L << ep)) != 0) {
      Move m = new Move(from, ep, piece, Piece.make(Piece.PAWN, !white), 0, Move.FLAG_EN_PASSANT);
      if (!legal) {
        out.add(m);
      } else {
        // Rare and tricky (two pieces leave one rank): just try it.
        pos.makeMove(m);
        boolean ok = !pos.inCheck(white);
        pos.unmakeMove(m);
        if (ok) out.add(m);
      }
    }
  }

  private static void addPromotions(int from, int to, int piece, int captured, List<Move> out) {
    for (int type : Piece.PROMOTION_TYPES) out.add(new Move(from, to, piece, captured, type, Move.FLAG_NONE));
  }

  private static void castleMoves(Position pos, int from, int piece, boolean white, List<Move> out) {
    int home = white ? 4 : 60;
    if (from != home) return;
    int kingSide = white ? Position.WHITE_KINGSIDE : Position.BLACK_KINGSIDE;
    int queenSide = white ? Position.WHITE_QUEENSIDE : Position.BLACK_QUEENSIDE;
    int rook = Piece.make(Piece.ROOK, white);
    if (!pos.canCastle(kingSide) && !pos.canCastle(queenSide)) return;
    if (pos.isAttacked(from, !white)) return;
    if (pos.canCastle(kingSide) && pos.pieceAt(from + 3) == rook
        && pos.isEmpty(from + 1) && pos.isEmpty(from + 2)
        && !pos.isAttacked(from + 1, !white) && !pos.isAttacked(from + 2, !white)) {
      out.add(new Move(from, from + 2, piece, Piece.EMPTY, 0, Move.FLAG_CASTLE));
    }
    if (pos.canCastle(queenSide) && pos.pieceAt(from - 4) == rook
        && pos.isEmpty(from - 1) && pos.isEmpty(from - 2) && pos.isEmpty(from - 3)
        && !pos.isAttacked(from - 1, !white) && !pos.isAttacked(from - 2, !white)) {
      out.add(new Move(from, from - 2, piece, Piece.EMPTY, 0, Move.FLAG_CASTLE));
    }
  }
}
