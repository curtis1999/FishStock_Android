package com.example.fishstock;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Square;

import java.util.ArrayList;
import java.util.List;

/**
 * The bridge between the engine and speech, both ways:
 * <ul>
 *   <li>{@link #find}: what the recogniser heard → the legal move it means (via {@link SpokenMoveParser});</li>
 *   <li>{@link #say}: a move → words to read back ("knight takes F3, check");</li>
 *   <li>{@link #describe}: a whole position → sentences to read slowly, for visualisation training.</li>
 * </ul>
 * Pure Java (no Android).
 */
public final class SpokenMoves {
  private SpokenMoves() {}

  /** Every legal move in pos, described for the parser. */
  public static List<SpokenMoveParser.Candidate> candidates(Position pos) {
    boolean white = pos.whiteToMove();
    List<SpokenMoveParser.Candidate> out = new ArrayList<>();
    for (Move m : MoveGenerator.legalMoves(pos)) {
      int type = Piece.type(m.piece);
      int target = pos.pieceAt(m.to);
      boolean enemy = Piece.isColor(target, !white);
      // A pawn changing file onto an empty square can only be taking en passant.
      boolean enPassant = type == Piece.PAWN && Square.file(m.from) != Square.file(m.to) && !enemy;
      char captured = enemy ? letter(Piece.type(target)) : enPassant ? 'P' : 0;
      char promotion = m.isPromotion() ? letter(m.promotion) : 0;
      int castle = m.isCastle() ? (Square.file(m.to) == 6 ? 1 : 2) : 0;
      out.add(new SpokenMoveParser.Candidate(letter(type), Square.file(m.from), Square.rank(m.from),
          Square.file(m.to), Square.rank(m.to), enemy || enPassant, captured, promotion, castle, m));
    }
    return out;
  }

  /** What came of a phrase: one move, several it could be, or none. */
  public static final class Heard {
    /** The move, when exactly one fits. */
    public final Move move;
    /** When more than one fits: their SAN, e.g. ["Nbd2", "Nfd2"]. Empty otherwise. */
    public final List<String> options;

    Heard(Move move, List<String> options) {
      this.move = move;
      this.options = options;
    }
  }

  /** The recogniser's guesses (best first) → the move they mean in pos. */
  public static Heard find(Position pos, List<String> hypotheses) {
    List<SpokenMoveParser.Candidate> m = SpokenMoveParser.bestMatch(hypotheses, candidates(pos));
    if (m.size() == 1) return new Heard((Move) m.get(0).move, new ArrayList<>());
    List<String> options = new ArrayList<>();
    for (SpokenMoveParser.Candidate c : m) options.add(Notation.toSan(pos, (Move) c.move));
    return new Heard(null, options);
  }

  /** "knight takes F3, check" for a move about to be played in pos. */
  public static String say(Position pos, Move m) {
    return SpokenMoveParser.sanToWords(Notation.toSan(pos, m));
  }

  private static final int[] ORDER = {Piece.KING, Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT, Piece.PAWN};

  /**
   * The position as short sentences, side to move first: "White to move.", "White pieces.",
   * "King on G1.", "Rooks on A1 and F1.", "Pawns on A2, B2, F2 and G2.", then Black the same way.
   * Squares go file by file (a to h), so a pawn chain is heard in order.
   */
  public static List<String> describe(Position pos) {
    List<String> out = new ArrayList<>();
    out.add(pos.whiteToMove() ? "White to move." : "Black to move.");
    boolean[] sides = {pos.whiteToMove(), !pos.whiteToMove()};
    for (boolean white : sides) {
      out.add(white ? "White pieces." : "Black pieces.");
      for (int type : ORDER) {
        List<String> squares = new ArrayList<>();
        for (int file = 0; file < 8; file++) {
          for (int rank = 0; rank < 8; rank++) {
            int sq = Square.parse("" + (char) ('a' + file) + (char) ('1' + rank));
            int p = pos.pieceAt(sq);
            if (Piece.isColor(p, white) && Piece.type(p) == type) squares.add(SpokenMoveParser.squareWords(Square.name(sq)));
          }
        }
        if (squares.isEmpty()) continue;
        String name = SpokenMoveParser.pieceName(letter(type));
        out.add(capitalise(squares.size() == 1 ? name : name + "s") + " on " + list(squares) + ".");
      }
    }
    return out;
  }

  private static String list(List<String> items) {
    if (items.size() == 1) return items.get(0);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) sb.append(i == items.size() - 1 ? " and " : ", ");
      sb.append(items.get(i));
    }
    return sb.toString();
  }

  private static String capitalise(String s) {
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private static char letter(int type) {
    if (type == Piece.KNIGHT) return 'N';
    if (type == Piece.BISHOP) return 'B';
    if (type == Piece.ROOK) return 'R';
    if (type == Piece.QUEEN) return 'Q';
    if (type == Piece.KING) return 'K';
    return 'P';
  }
}
