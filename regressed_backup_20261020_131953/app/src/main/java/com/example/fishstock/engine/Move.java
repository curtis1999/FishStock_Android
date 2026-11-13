package com.example.fishstock.engine;

import java.io.Serializable;

/**
 * An immutable chess move. Everything the board needs to make and unmake it is stored here,
 * so there are no half-filled flags to keep in sync (the source of many old bugs).
 */
public final class Move implements Serializable {
  public static final int FLAG_NONE = 0;
  public static final int FLAG_DOUBLE_PUSH = 1;
  public static final int FLAG_EN_PASSANT = 2;
  public static final int FLAG_CASTLE = 4;

  public final int from;
  public final int to;
  /** The moving piece (signed). */
  public final int piece;
  /** The captured piece (signed), or 0. For en passant this is the captured pawn. */
  public final int captured;
  /** Promotion piece type (unsigned, e.g. Piece.QUEEN), or 0. */
  public final int promotion;
  public final int flags;

  public Move(int from, int to, int piece, int captured, int promotion, int flags) {
    this.from = from;
    this.to = to;
    this.piece = piece;
    this.captured = captured;
    this.promotion = promotion;
    this.flags = flags;
  }

  public boolean isCapture() {
    return captured != Piece.EMPTY;
  }

  public boolean isPromotion() {
    return promotion != 0;
  }

  public boolean isCastle() {
    return (flags & FLAG_CASTLE) != 0;
  }

  public boolean isEnPassant() {
    return (flags & FLAG_EN_PASSANT) != 0;
  }

  public boolean isDoublePush() {
    return (flags & FLAG_DOUBLE_PUSH) != 0;
  }

  public boolean isWhite() {
    return piece > 0;
  }

  /** Long algebraic / UCI form, e.g. "e2e4" or "e7e8q". */
  public String toUci() {
    String s = Square.name(from) + Square.name(to);
    if (promotion != 0) s += Character.toLowerCase(Piece.letter(promotion));
    return s;
  }

  /** Same squares and same promotion. */
  public boolean sameAs(Move other) {
    return other != null && from == other.from && to == other.to && promotion == other.promotion;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Move)) return false;
    Move m = (Move) o;
    return from == m.from && to == m.to && piece == m.piece && captured == m.captured
        && promotion == m.promotion && flags == m.flags;
  }

  @Override
  public int hashCode() {
    return ((from * 64 + to) * 16 + promotion) * 31 + piece;
  }

  @Override
  public String toString() {
    return toUci();
  }
}
