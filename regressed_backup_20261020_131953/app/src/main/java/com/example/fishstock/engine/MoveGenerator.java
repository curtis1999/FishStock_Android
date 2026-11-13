package com.example.fishstock.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates moves for the side to move.
 *
 * "Pseudo-legal" moves follow piece movement rules but may leave the king in check;
 * {@link #legalMoves} filters those out by trying each move. This single filter replaces
 * the old separate code paths for check, double check and pins.
 */
public final class MoveGenerator {
  private MoveGenerator() {}

  /** All legal moves for the side to move. */
  public static List<Move> legalMoves(Position pos) {
    List<Move> pseudo = new ArrayList<>(48);
    generate(pos, pseudo, false);
    return filterLegal(pos, pseudo);
  }

  /** Legal captures and promotions only (used by the quiescence search). */
  public static List<Move> legalTacticalMoves(Position pos) {
    List<Move> pseudo = new ArrayList<>(16);
    generate(pos, pseudo, true);
    return filterLegal(pos, pseudo);
  }

  /** Legal moves of the piece standing on the given square. */
  public static List<Move> legalMovesFrom(Position pos, int from) {
    List<Move> out = new ArrayList<>();
    for (Move m : legalMoves(pos)) if (m.from == from) out.add(m);
    return out;
  }

  public static boolean hasLegalMove(Position pos) {
    List<Move> pseudo = new ArrayList<>(48);
    generate(pos, pseudo, false);
    boolean white = pos.whiteToMove();
    for (Move m : pseudo) {
      pos.makeMove(m);
      boolean ok = !pos.inCheck(white);
      pos.unmakeMove(m);
      if (ok) return true;
    }
    return false;
  }

  /** Does this (legal) move give check? */
  public static boolean givesCheck(Position pos, Move m) {
    pos.makeMove(m);
    boolean check = pos.inCheck();
    pos.unmakeMove(m);
    return check;
  }

  private static List<Move> filterLegal(Position pos, List<Move> pseudo) {
    boolean white = pos.whiteToMove();
    List<Move> legal = new ArrayList<>(pseudo.size());
    for (Move m : pseudo) {
      pos.makeMove(m);
      if (!pos.inCheck(white)) legal.add(m);
      pos.unmakeMove(m);
    }
    return legal;
  }

  /** Pseudo-legal move generation. */
  public static void generate(Position pos, List<Move> out, boolean tacticalOnly) {
    boolean white = pos.whiteToMove();
    for (int from = 0; from < 64; from++) {
      int piece = pos.pieceAt(from);
      if (!Piece.isColor(piece, white)) continue;
      switch (Piece.type(piece)) {
        case Piece.PAWN:
          pawnMoves(pos, from, piece, white, out, tacticalOnly);
          break;
        case Piece.KNIGHT:
          stepMoves(pos, from, piece, white, Attacks.KNIGHT[from], out, tacticalOnly);
          break;
        case Piece.KING:
          stepMoves(pos, from, piece, white, Attacks.KING[from], out, tacticalOnly);
          if (!tacticalOnly) castleMoves(pos, from, piece, white, out);
          break;
        default:
          slideMoves(pos, from, piece, white, out, tacticalOnly);
      }
    }
  }

  private static void pawnMoves(Position pos, int from, int piece, boolean white,
                                List<Move> out, boolean tacticalOnly) {
    int forward = white ? 8 : -8;
    int startRank = white ? 1 : 6;
    int lastRank = white ? 7 : 0;
    int one = from + forward;
    if (one >= 0 && one < 64 && pos.isEmpty(one)) {
      if (Square.rank(one) == lastRank) {
        addPromotions(from, one, piece, Piece.EMPTY, out);
      } else if (!tacticalOnly) {
        out.add(new Move(from, one, piece, Piece.EMPTY, 0, Move.FLAG_NONE));
        int two = one + forward;
        if (Square.rank(from) == startRank && pos.isEmpty(two)) {
          out.add(new Move(from, two, piece, Piece.EMPTY, 0, Move.FLAG_DOUBLE_PUSH));
        }
      }
    }
    for (int to : Attacks.PAWN_ATTACKS[white ? 0 : 1][from]) {
      int target = pos.pieceAt(to);
      if (target != Piece.EMPTY && Piece.isColor(target, !white)) {
        if (Square.rank(to) == lastRank) addPromotions(from, to, piece, target, out);
        else out.add(new Move(from, to, piece, target, 0, Move.FLAG_NONE));
      } else if (to == pos.enPassantSquare()) {
        out.add(new Move(from, to, piece, Piece.make(Piece.PAWN, !white), 0, Move.FLAG_EN_PASSANT));
      }
    }
  }

  private static void addPromotions(int from, int to, int piece, int captured, List<Move> out) {
    for (int type : Piece.PROMOTION_TYPES) out.add(new Move(from, to, piece, captured, type, Move.FLAG_NONE));
  }

  private static void stepMoves(Position pos, int from, int piece, boolean white, int[] targets,
                                List<Move> out, boolean tacticalOnly) {
    for (int to : targets) {
      int target = pos.pieceAt(to);
      if (target == Piece.EMPTY) {
        if (!tacticalOnly) out.add(new Move(from, to, piece, Piece.EMPTY, 0, Move.FLAG_NONE));
      } else if (Piece.isColor(target, !white)) {
        out.add(new Move(from, to, piece, target, 0, Move.FLAG_NONE));
      }
    }
  }

  private static void slideMoves(Position pos, int from, int piece, boolean white,
                                 List<Move> out, boolean tacticalOnly) {
    int type = Piece.type(piece);
    for (int d = 0; d < 8; d++) {
      if (!Attacks.slidesAlong(type, d)) continue;
      for (int to : Attacks.RAYS[from][d]) {
        int target = pos.pieceAt(to);
        if (target == Piece.EMPTY) {
          if (!tacticalOnly) out.add(new Move(from, to, piece, Piece.EMPTY, 0, Move.FLAG_NONE));
          continue;
        }
        if (Piece.isColor(target, !white)) out.add(new Move(from, to, piece, target, 0, Move.FLAG_NONE));
        break;
      }
    }
  }

  private static void castleMoves(Position pos, int from, int piece, boolean white, List<Move> out) {
    int home = white ? Square.parse("e1") : Square.parse("e8");
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
