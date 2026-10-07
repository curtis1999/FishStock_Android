package com.example.fishstock.engine;

import java.util.List;

/** Standard algebraic notation (e4, Nxf7+, O-O, e8=Q#). */
public final class Notation {
  private Notation() {}

  /** SAN for a legal move in the given position. The position is left unchanged. */
  public static String toSan(Position pos, Move m) {
    StringBuilder sb = new StringBuilder();
    int type = Piece.type(m.piece);
    if (m.isCastle()) {
      sb.append(Square.file(m.to) == 6 ? "O-O" : "O-O-O");
    } else if (type == Piece.PAWN) {
      if (m.isCapture()) sb.append((char) ('a' + Square.file(m.from))).append('x');
      sb.append(Square.name(m.to));
      if (m.isPromotion()) sb.append('=').append(Piece.letter(m.promotion));
    } else {
      sb.append(Piece.letter(m.piece));
      sb.append(disambiguation(pos, m));
      if (m.isCapture()) sb.append('x');
      sb.append(Square.name(m.to));
    }
    pos.makeMove(m);
    if (pos.inCheck()) sb.append(MoveGenerator.hasLegalMove(pos) ? "+" : "#");
    pos.unmakeMove(m);
    return sb.toString();
  }

  private static String disambiguation(Position pos, Move m) {
    List<Move> legal = MoveGenerator.legalMoves(pos);
    boolean clash = false;
    boolean sameFile = false;
    boolean sameRank = false;
    for (Move o : legal) {
      if (o.piece == m.piece && o.to == m.to && o.from != m.from) {
        clash = true;
        if (Square.file(o.from) == Square.file(m.from)) sameFile = true;
        if (Square.rank(o.from) == Square.rank(m.from)) sameRank = true;
      }
    }
    if (!clash) return "";
    if (!sameFile) return "" + (char) ('a' + Square.file(m.from));
    if (!sameRank) return "" + (char) ('1' + Square.rank(m.from));
    return Square.name(m.from);
  }

  /** SAN for a sequence of moves starting from pos, e.g. "12. Nf3 d5 13. c4". */
  public static String line(Position start, List<Move> moves) {
    Position p = start.copy();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < moves.size(); i++) {
      Move m = moves.get(i);
      if (p.whiteToMove()) sb.append(p.fullmoveNumber()).append(". ");
      else if (i == 0) sb.append(p.fullmoveNumber()).append("... ");
      sb.append(toSan(p, m)).append(' ');
      p.makeMove(m);
    }
    return sb.toString().trim();
  }
}
