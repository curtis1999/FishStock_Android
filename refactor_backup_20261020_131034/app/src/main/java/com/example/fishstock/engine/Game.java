package com.example.fishstock.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A game in progress: the starting position, every move played, and the result.
 *
 * The UI, the Arena and the analysis board all go through this class, so there is exactly
 * one place that decides whether a move is legal and whether the game is over.
 */
public final class Game {
  private final String startFen;
  private final Position position;
  private final List<Move> moves = new ArrayList<>();
  private final List<Long> hashes = new ArrayList<>();
  private List<Move> legalMovesCache;
  private GameResult result = GameResult.ONGOING;

  public Game() {
    this(Position.START_FEN);
  }

  public Game(String fen) {
    this.startFen = fen;
    this.position = Position.fromFen(fen);
    hashes.add(position.hash());
    updateResult();
  }

  /** The live position. Treat as read-only: call {@link #play} to change it. */
  public Position position() {
    return position;
  }

  public String startFen() {
    return startFen;
  }

  public List<Move> moves() {
    return Collections.unmodifiableList(moves);
  }

  public int plyCount() {
    return moves.size();
  }

  public Move lastMove() {
    return moves.isEmpty() ? null : moves.get(moves.size() - 1);
  }

  public boolean whiteToMove() {
    return position.whiteToMove();
  }

  public GameResult result() {
    return result;
  }

  public boolean isOver() {
    return result.isOver;
  }

  /** Hashes of every position that has occurred, including the current one. */
  public List<Long> positionHistory() {
    return Collections.unmodifiableList(hashes);
  }

  public List<Move> legalMoves() {
    if (legalMovesCache == null) legalMovesCache = MoveGenerator.legalMoves(position);
    return legalMovesCache;
  }

  /** Finds the legal move matching the squares (and promotion type, 0 for none), or null. */
  public Move findMove(int from, int to, int promotion) {
    for (Move m : legalMoves()) {
      if (m.from == from && m.to == to && (m.promotion == promotion || (promotion == 0 && !m.isPromotion()))) {
        return m;
      }
    }
    return null;
  }

  /** True when moving from-to needs a promotion choice. */
  public boolean isPromotionMove(int from, int to) {
    for (Move m : legalMoves()) if (m.from == from && m.to == to && m.isPromotion()) return true;
    return false;
  }

  public Move findUci(String uci) {
    for (Move m : legalMoves()) if (m.toUci().equals(uci)) return m;
    return null;
  }

  /** Plays a move. Throws if the move is not legal here or the game is over. */
  public void play(Move move) {
    if (result.isOver) throw new IllegalStateException("Game is over: " + result);
    Move legal = null;
    for (Move m : legalMoves()) {
      if (m.sameAs(move)) {
        legal = m;
        break;
      }
    }
    if (legal == null) throw new IllegalArgumentException("Illegal move " + move + " in " + position.toFen());
    position.makeMove(legal);
    moves.add(legal);
    hashes.add(position.hash());
    legalMovesCache = null;
    updateResult();
  }

  /** Takes back the last move. Returns false if there is nothing to take back. */
  public boolean undo() {
    if (moves.isEmpty()) return false;
    Move last = moves.remove(moves.size() - 1);
    hashes.remove(hashes.size() - 1);
    position.unmakeMove(last);
    legalMovesCache = null;
    result = GameResult.ONGOING;
    updateResult();
    return true;
  }

  /** Ends the game by resignation or agreement. */
  public void end(GameResult forcedResult) {
    this.result = forcedResult;
  }

  /** How many times the current position has occurred (1 = first time). */
  public int repetitionCount() {
    long h = position.hash();
    int n = 0;
    for (long x : hashes) if (x == h) n++;
    return n;
  }

  /** A fresh copy of the position after the given number of plies (0 = start). */
  public Position positionAt(int ply) {
    Position p = Position.fromFen(startFen);
    for (int i = 0; i < ply && i < moves.size(); i++) p.makeMove(moves.get(i));
    return p;
  }

  /**
   * Pieces captured by each side during the first {@code ply} plies.
   * Returns counts[0 = captured by White][type] and counts[1 = captured by Black][type].
   * Deriving this from the move list keeps the counters right after undo or when
   * stepping back through the game with the arrows.
   */
  public int[][] capturedCounts(int ply) {
    int[][] counts = new int[2][7];
    for (int i = 0; i < ply && i < moves.size(); i++) {
      Move m = moves.get(i);
      if (m.isCapture()) counts[m.isWhite() ? 0 : 1][Piece.type(m.captured)]++;
    }
    return counts;
  }

  /** Material balance (1/3/3/5/9) from White's point of view after {@code ply} plies. */
  public int materialBalance(int ply) {
    Position p = positionAt(ply);
    int total = 0;
    for (int sq = 0; sq < 64; sq++) {
      int piece = p.pieceAt(sq);
      total += piece > 0 ? Piece.displayValue(piece) : -Piece.displayValue(piece);
    }
    return total;
  }

  /** Moves in standard algebraic notation, e.g. "1. e4 e5 2. Nf3". */
  public String toSanString() {
    StringBuilder sb = new StringBuilder();
    Position p = Position.fromFen(startFen);
    for (Move m : moves) {
      if (p.whiteToMove()) sb.append(p.fullmoveNumber()).append(". ");
      else if (sb.length() == 0) sb.append(p.fullmoveNumber()).append("... ");
      sb.append(Notation.toSan(p, m)).append(' ');
      p.makeMove(m);
    }
    return sb.toString().trim();
  }

  private void updateResult() {
    if (result.isOver) return;
    boolean anyMove = !legalMoves().isEmpty();
    if (!anyMove) {
      if (position.inCheck()) {
        result = position.whiteToMove() ? GameResult.BLACK_CHECKMATES : GameResult.WHITE_CHECKMATES;
      } else {
        result = GameResult.STALEMATE;
      }
    } else if (Rules.isInsufficientMaterial(position)) {
      result = GameResult.INSUFFICIENT_MATERIAL;
    } else if (repetitionCount() >= 3) {
      result = GameResult.THREEFOLD_REPETITION;
    } else if (Rules.isFiftyMoveDraw(position)) {
      result = GameResult.FIFTY_MOVE_RULE;
    } else {
      result = GameResult.ONGOING;
    }
  }
}
