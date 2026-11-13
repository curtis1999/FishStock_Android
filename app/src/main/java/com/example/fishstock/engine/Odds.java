package com.example.fishstock.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Piece odds: the agent starts without some of its pieces (down the a7 pawn, the dark-squared
 * bishop, the queen...).
 *
 * Odds are written from White's side, as the squares a1..h2 that are emptied ("d1,f2"), so the
 * same odds work whichever colour the agent plays: for Black they are mirrored to rank 8 / rank 7
 * (d1 becomes d8: the queen is still the queen, the f-pawn still the f-pawn). The king can't be
 * removed.
 */
public final class Odds {
  private Odds() {}

  public static final String NONE = "";

  /** The squares in an odds string (White's side, a1..h2). Unknown or king squares are ignored. */
  public static Set<Integer> parse(String odds) {
    Set<Integer> squares = new TreeSet<>();
    if (odds == null) return squares;
    for (String token : odds.split(",")) {
      int sq = Square.parse(token.trim());
      if (sq >= 0 && Square.rank(sq) <= 1 && sq != Square.parse("e1")) squares.add(sq);
    }
    return squares;
  }

  public static String format(Set<Integer> whiteSideSquares) {
    StringBuilder sb = new StringBuilder();
    for (int sq : new TreeSet<>(whiteSideSquares)) {
      if (sb.length() > 0) sb.append(',');
      sb.append(Square.name(sq));
    }
    return sb.toString();
  }

  public static boolean isNone(String odds) {
    return parse(odds).isEmpty();
  }

  /** The real square for the agent's colour (mirrors White's side to Black's). */
  public static int actualSquare(int whiteSideSquare, boolean agentWhite) {
    if (agentWhite) return whiteSideSquare;
    return Square.of(Square.file(whiteSideSquare), 7 - Square.rank(whiteSideSquare));
  }

  /** The reverse of {@link #actualSquare}. */
  public static int whiteSideSquare(int actualSquare) {
    int r = Square.rank(actualSquare);
    return r <= 1 ? actualSquare : Square.of(Square.file(actualSquare), 7 - r);
  }

  /** Starting position with the agent's odds pieces removed (castling rights follow the rooks). */
  public static String startFen(String odds, boolean agentWhite) {
    Set<Integer> gone = new TreeSet<>();
    for (int sq : parse(odds)) gone.add(actualSquare(sq, agentWhite));
    if (gone.isEmpty()) return Position.START_FEN;

    Position start = Position.fromFen(Position.START_FEN);
    StringBuilder placement = new StringBuilder();
    for (int rank = 7; rank >= 0; rank--) {
      int empty = 0;
      for (int file = 0; file < 8; file++) {
        int sq = Square.of(file, rank);
        int piece = gone.contains(sq) ? Piece.EMPTY : start.pieceAt(sq);
        if (piece == Piece.EMPTY) {
          empty++;
        } else {
          if (empty > 0) placement.append(empty);
          empty = 0;
          placement.append(Piece.fenChar(piece));
        }
      }
      if (empty > 0) placement.append(empty);
      if (rank > 0) placement.append('/');
    }
    StringBuilder castling = new StringBuilder();
    if (!gone.contains(Square.parse("h1"))) castling.append('K');
    if (!gone.contains(Square.parse("a1"))) castling.append('Q');
    if (!gone.contains(Square.parse("h8"))) castling.append('k');
    if (!gone.contains(Square.parse("a8"))) castling.append('q');
    if (castling.length() == 0) castling.append('-');
    return placement + " w " + castling + " - 0 1";
  }

  /** "queen, dark-squared bishop and f7 pawn" (for the agent's real colour), or "" for no odds. */
  public static String describe(String odds, boolean agentWhite) {
    List<String> names = new ArrayList<>();
    Position start = Position.fromFen(Position.START_FEN);
    // Biggest pieces first.
    int[] order = {Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT, Piece.PAWN};
    Set<Integer> squares = parse(odds);
    for (int type : order) {
      for (int sq : squares) {
        int actual = actualSquare(sq, agentWhite);
        int piece = start.pieceAt(actual);
        if (Piece.type(piece) == type) names.add(pieceName(piece, actual));
      }
    }
    if (names.isEmpty()) return "";
    if (names.size() == 1) return names.get(0);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < names.size(); i++) {
      if (i > 0) sb.append(i == names.size() - 1 ? " and " : ", ");
      sb.append(names.get(i));
    }
    return sb.toString();
  }

  /** How a chess player would name the piece on its starting square. */
  public static String pieceName(int piece, int square) {
    String side = Square.file(square) < 4 ? "queenside" : "kingside";
    switch (Piece.type(piece)) {
      case Piece.PAWN: return Square.name(square) + " pawn";
      case Piece.KNIGHT: return side + " knight";
      case Piece.BISHOP: return (Square.isLight(square) ? "light" : "dark") + "-squared bishop";
      case Piece.ROOK: return side + " rook";
      case Piece.QUEEN: return "queen";
      default: return "king";
    }
  }
}
