package com.example.fishstock.engine;

import java.util.Random;

/**
 * A complete chess position: piece placement, side to move, castling rights, en passant square
 * and move clocks. Moves are applied with {@link #makeMove} and taken back with
 * {@link #unmakeMove}, which lets the search explore millions of positions without copying.
 *
 * This class only knows the rules of chess; it has no opinion on who is winning.
 */
public final class Position {
  public static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  public static final int WHITE_KINGSIDE = 1;
  public static final int WHITE_QUEENSIDE = 2;
  public static final int BLACK_KINGSIDE = 4;
  public static final int BLACK_QUEENSIDE = 8;

  /** Castling rights that survive a move touching each square (e.g. moving from h1 loses K). */
  private static final int[] CASTLE_MASK = new int[64];

  // Zobrist keys for hashing (used for repetition detection and the search's memory).
  private static final long[][] PIECE_KEYS = new long[13][64];
  private static final long[] CASTLE_KEYS = new long[16];
  private static final long[] EP_KEYS = new long[8];
  private static final long SIDE_KEY;

  static {
    for (int i = 0; i < 64; i++) CASTLE_MASK[i] = 15;
    CASTLE_MASK[Square.parse("e1")] = 15 & ~(WHITE_KINGSIDE | WHITE_QUEENSIDE);
    CASTLE_MASK[Square.parse("h1")] = 15 & ~WHITE_KINGSIDE;
    CASTLE_MASK[Square.parse("a1")] = 15 & ~WHITE_QUEENSIDE;
    CASTLE_MASK[Square.parse("e8")] = 15 & ~(BLACK_KINGSIDE | BLACK_QUEENSIDE);
    CASTLE_MASK[Square.parse("h8")] = 15 & ~BLACK_KINGSIDE;
    CASTLE_MASK[Square.parse("a8")] = 15 & ~BLACK_QUEENSIDE;

    Random rnd = new Random(20230507L);
    for (int p = 0; p < 13; p++) for (int s = 0; s < 64; s++) PIECE_KEYS[p][s] = rnd.nextLong();
    for (int i = 0; i < 16; i++) CASTLE_KEYS[i] = rnd.nextLong();
    for (int i = 0; i < 8; i++) EP_KEYS[i] = rnd.nextLong();
    SIDE_KEY = rnd.nextLong();
  }

  private final int[] board = new int[64];
  /** Bitboards (see {@link Bitboards}): bb[piece + 6] = squares holding that piece; plus occupancy. */
  private final long[] bb = new long[13];
  private long whiteOcc;
  private long blackOcc;
  private boolean whiteToMove;
  private int castling;
  private int epSquare = Square.NONE;
  private int halfmoveClock;
  private int fullmoveNumber = 1;
  private int whiteKing = Square.NONE;
  private int blackKing = Square.NONE;
  private long hash;

  // Undo stack (state that a Move alone cannot restore).
  private int[] undoCastling = new int[64];
  private int[] undoEp = new int[64];
  private int[] undoHalfmove = new int[64];
  private long[] undoHash = new long[64];
  private int undoTop;

  private Position() {}

  public static Position startingPosition() {
    return fromFen(START_FEN);
  }

  // ---------------------------------------------------------------- accessors

  public int pieceAt(int sq) {
    return board[sq];
  }

  /** The mailbox board (read-only use by the move generator). */
  int[] boardArray() {
    return board;
  }

  public boolean isEmpty(int sq) {
    return board[sq] == Piece.EMPTY;
  }

  public boolean whiteToMove() {
    return whiteToMove;
  }

  public int castlingRights() {
    return castling;
  }

  public boolean canCastle(int right) {
    return (castling & right) != 0;
  }

  public int enPassantSquare() {
    return epSquare;
  }

  public int halfmoveClock() {
    return halfmoveClock;
  }

  public int fullmoveNumber() {
    return fullmoveNumber;
  }

  public int kingSquare(boolean white) {
    return white ? whiteKing : blackKing;
  }

  public long hash() {
    return hash;
  }

  /** Number of plies made on this object that can still be unmade. */
  public int undoDepth() {
    return undoTop;
  }

  /** Count of pieces of the given signed code on the board. */
  public int count(int piece) {
    return Long.bitCount(bb[piece + 6]);
  }

  /** Total number of pieces on the board, kings included. */
  public int pieceCount() {
    return Long.bitCount(whiteOcc | blackOcc);
  }

  /** Squares holding the given signed piece (e.g. Piece.make(Piece.KNIGHT, true)), as a bitboard. */
  public long pieces(int piece) {
    return bb[piece + 6];
  }

  public long pieces(int type, boolean white) {
    return bb[(white ? type : -type) + 6];
  }

  /** Squares holding the given side's pieces. */
  public long occupancy(boolean white) {
    return white ? whiteOcc : blackOcc;
  }

  /** Squares holding any piece. */
  public long occupied() {
    return whiteOcc | blackOcc;
  }

  /** Pieces of the given side that attack sq, with the board occupancy given (for x-rays). */
  public long attackersTo(int sq, boolean bySide, long occupied) {
    long b = Bitboards.PAWN[bySide ? 1 : 0][sq] & pieces(Piece.PAWN, bySide);
    b |= Bitboards.KNIGHT[sq] & pieces(Piece.KNIGHT, bySide);
    b |= Bitboards.KING[sq] & pieces(Piece.KING, bySide);
    long queens = pieces(Piece.QUEEN, bySide);
    long diag = pieces(Piece.BISHOP, bySide) | queens;
    long straight = pieces(Piece.ROOK, bySide) | queens;
    if (diag != 0) b |= Bitboards.bishopAttacks(sq, occupied) & diag;
    if (straight != 0) b |= Bitboards.rookAttacks(sq, occupied) & straight;
    return b & occupied;
  }

  private void put(int piece, int sq) {
    board[sq] = piece;
    long m = 1L << sq;
    bb[piece + 6] |= m;
    if (piece > 0) whiteOcc |= m;
    else blackOcc |= m;
  }

  private void remove(int sq) {
    int piece = board[sq];
    if (piece == Piece.EMPTY) return;
    board[sq] = Piece.EMPTY;
    long m = ~(1L << sq);
    bb[piece + 6] &= m;
    if (piece > 0) whiteOcc &= m;
    else blackOcc &= m;
  }

  public Position copy() {
    Position p = new Position();
    System.arraycopy(board, 0, p.board, 0, 64);
    System.arraycopy(bb, 0, p.bb, 0, 13);
    p.whiteOcc = whiteOcc;
    p.blackOcc = blackOcc;
    p.whiteToMove = whiteToMove;
    p.castling = castling;
    p.epSquare = epSquare;
    p.halfmoveClock = halfmoveClock;
    p.fullmoveNumber = fullmoveNumber;
    p.whiteKing = whiteKing;
    p.blackKing = blackKing;
    p.hash = hash;
    return p;
  }

  // ---------------------------------------------------------------- attacks

  /** Is square sq attacked by any piece of the given side? */
  public boolean isAttacked(int sq, boolean byWhite) {
    if ((Bitboards.PAWN[byWhite ? 1 : 0][sq] & pieces(Piece.PAWN, byWhite)) != 0) return true;
    if ((Bitboards.KNIGHT[sq] & pieces(Piece.KNIGHT, byWhite)) != 0) return true;
    if ((Bitboards.KING[sq] & pieces(Piece.KING, byWhite)) != 0) return true;
    long occ = whiteOcc | blackOcc;
    long queens = pieces(Piece.QUEEN, byWhite);
    long diag = pieces(Piece.BISHOP, byWhite) | queens;
    if (diag != 0 && (Bitboards.bishopAttacks(sq, occ) & diag) != 0) return true;
    long straight = pieces(Piece.ROOK, byWhite) | queens;
    return straight != 0 && (Bitboards.rookAttacks(sq, occ) & straight) != 0;
  }

  /** Same as {@link #isAttacked(int, boolean)} but on an arbitrary board array. */
  public static boolean isAttacked(int[] b, int sq, boolean byWhite) {
    // Pawns: look from the target square backwards along the pawn capture direction.
    int pawn = Piece.make(Piece.PAWN, byWhite);
    for (int from : Attacks.PAWN_ATTACKS[byWhite ? 1 : 0][sq]) {
      if (b[from] == pawn) return true;
    }
    int knight = Piece.make(Piece.KNIGHT, byWhite);
    for (int from : Attacks.KNIGHT[sq]) {
      if (b[from] == knight) return true;
    }
    int king = Piece.make(Piece.KING, byWhite);
    for (int from : Attacks.KING[sq]) {
      if (b[from] == king) return true;
    }
    for (int d = 0; d < 8; d++) {
      for (int s : Attacks.RAYS[sq][d]) {
        int p = b[s];
        if (p == Piece.EMPTY) continue;
        if (Piece.isColor(p, byWhite) && Attacks.slidesAlong(Piece.type(p), d)) return true;
        break;
      }
    }
    return false;
  }

  /** Is the side to move in check? */
  public boolean inCheck() {
    return isAttacked(kingSquare(whiteToMove), !whiteToMove);
  }

  /** Is the given side's king attacked? */
  public boolean inCheck(boolean white) {
    return isAttacked(kingSquare(white), !white);
  }

  /** Number of enemy pieces giving check to the side to move (2 = double check). */
  public int checkerCount() {
    return Long.bitCount(checkers());
  }

  /** The enemy pieces giving check to the side to move, as a bitboard. */
  public long checkers() {
    return attackersTo(kingSquare(whiteToMove), !whiteToMove, whiteOcc | blackOcc);
  }

  // ---------------------------------------------------------------- make / unmake

  public void makeMove(Move m) {
    if (undoTop == undoHash.length) growUndo();
    undoCastling[undoTop] = castling;
    undoEp[undoTop] = epSquare;
    undoHalfmove[undoTop] = halfmoveClock;
    undoHash[undoTop] = hash;
    undoTop++;

    boolean white = m.piece > 0;
    long h = hash;
    if (epSquare != Square.NONE) h ^= EP_KEYS[Square.file(epSquare)];
    h ^= CASTLE_KEYS[castling];

    // Remove the captured piece.
    if (m.isEnPassant()) {
      int capSq = m.to + (white ? -8 : 8);
      h ^= key(board[capSq], capSq);
      remove(capSq);
    } else if (m.captured != Piece.EMPTY) {
      h ^= key(board[m.to], m.to);
      remove(m.to);
    }

    // Move the piece (promoting if needed).
    h ^= key(m.piece, m.from);
    remove(m.from);
    int placed = m.promotion != 0 ? Piece.make(m.promotion, white) : m.piece;
    put(placed, m.to);
    h ^= key(placed, m.to);

    if (m.isCastle()) {
      int rookFrom;
      int rookTo;
      if (Square.file(m.to) == 6) {
        rookFrom = m.to + 1;
        rookTo = m.to - 1;
      } else {
        rookFrom = m.to - 2;
        rookTo = m.to + 1;
      }
      int rook = board[rookFrom];
      remove(rookFrom);
      put(rook, rookTo);
      h ^= key(rook, rookFrom) ^ key(rook, rookTo);
    }

    if (Piece.type(m.piece) == Piece.KING) {
      if (white) whiteKing = m.to;
      else blackKing = m.to;
    }

    castling &= CASTLE_MASK[m.from] & CASTLE_MASK[m.to];
    h ^= CASTLE_KEYS[castling];

    epSquare = Square.NONE;
    if (m.isDoublePush()) {
      int ep = (m.from + m.to) / 2;
      if (enemyPawnCanTake(m.to, white)) {
        epSquare = ep;
        h ^= EP_KEYS[Square.file(ep)];
      }
    }

    if (Piece.type(m.piece) == Piece.PAWN || m.captured != Piece.EMPTY) halfmoveClock = 0;
    else halfmoveClock++;
    if (!white) fullmoveNumber++;

    whiteToMove = !whiteToMove;
    h ^= SIDE_KEY;
    hash = h;
  }

  public void unmakeMove(Move m) {
    undoTop--;
    castling = undoCastling[undoTop];
    epSquare = undoEp[undoTop];
    halfmoveClock = undoHalfmove[undoTop];
    hash = undoHash[undoTop];
    whiteToMove = !whiteToMove;
    boolean white = m.piece > 0;
    if (!white) fullmoveNumber--;

    remove(m.to);
    put(m.piece, m.from);
    if (m.isEnPassant()) {
      put(m.captured, m.to + (white ? -8 : 8));
    } else if (m.captured != Piece.EMPTY) {
      put(m.captured, m.to);
    }

    if (m.isCastle()) {
      int rookFrom;
      int rookTo;
      if (Square.file(m.to) == 6) {
        rookFrom = m.to + 1;
        rookTo = m.to - 1;
      } else {
        rookFrom = m.to - 2;
        rookTo = m.to + 1;
      }
      int rook = board[rookTo];
      remove(rookTo);
      put(rook, rookFrom);
    }

    if (Piece.type(m.piece) == Piece.KING) {
      if (white) whiteKing = m.from;
      else blackKing = m.from;
    }
  }

  private boolean enemyPawnCanTake(int to, boolean moverWhite) {
    // Enemy pawns beside the pawn that just made a double step.
    long beside = ((1L << to) << 1 & ~Bitboards.FILE_A) | ((1L << to) >>> 1 & ~(Bitboards.FILE_A << 7));
    return (beside & pieces(Piece.PAWN, !moverWhite)) != 0;
  }

  private void growUndo() {
    int n = undoHash.length * 2;
    undoCastling = java.util.Arrays.copyOf(undoCastling, n);
    undoEp = java.util.Arrays.copyOf(undoEp, n);
    undoHalfmove = java.util.Arrays.copyOf(undoHalfmove, n);
    undoHash = java.util.Arrays.copyOf(undoHash, n);
  }

  private static long key(int piece, int sq) {
    return PIECE_KEYS[piece + 6][sq];
  }

  private long computeHash() {
    long h = 0;
    for (int s = 0; s < 64; s++) if (board[s] != Piece.EMPTY) h ^= key(board[s], s);
    h ^= CASTLE_KEYS[castling];
    if (epSquare != Square.NONE) h ^= EP_KEYS[Square.file(epSquare)];
    if (!whiteToMove) h ^= SIDE_KEY;
    return h;
  }

  // ---------------------------------------------------------------- FEN

  public static Position fromFen(String fen) {
    String[] parts = fen.trim().split("\\s+");
    Position p = new Position();
    int rank = 7;
    int file = 0;
    for (char c : parts[0].toCharArray()) {
      if (c == '/') {
        rank--;
        file = 0;
      } else if (Character.isDigit(c)) {
        file += c - '0';
      } else {
        int piece = Piece.fromFenChar(c);
        int sq = Square.of(file, rank);
        p.put(piece, sq);
        if (piece == Piece.KING) p.whiteKing = sq;
        if (piece == -Piece.KING) p.blackKing = sq;
        file++;
      }
    }
    p.whiteToMove = parts.length < 2 || parts[1].equals("w");
    p.castling = 0;
    if (parts.length > 2) {
      for (char c : parts[2].toCharArray()) {
        if (c == 'K') p.castling |= WHITE_KINGSIDE;
        if (c == 'Q') p.castling |= WHITE_QUEENSIDE;
        if (c == 'k') p.castling |= BLACK_KINGSIDE;
        if (c == 'q') p.castling |= BLACK_QUEENSIDE;
      }
    }
    p.epSquare = parts.length > 3 ? Square.parse(parts[3]) : Square.NONE;
    p.halfmoveClock = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
    p.fullmoveNumber = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;
    if (p.whiteKing == Square.NONE || p.blackKing == Square.NONE) {
      throw new IllegalArgumentException("Both kings are required: " + fen);
    }
    p.hash = p.computeHash();
    return p;
  }

  public String toFen() {
    StringBuilder sb = new StringBuilder();
    for (int r = 7; r >= 0; r--) {
      int empty = 0;
      for (int f = 0; f < 8; f++) {
        int piece = board[Square.of(f, r)];
        if (piece == Piece.EMPTY) {
          empty++;
        } else {
          if (empty > 0) sb.append(empty);
          empty = 0;
          sb.append(Piece.fenChar(piece));
        }
      }
      if (empty > 0) sb.append(empty);
      if (r > 0) sb.append('/');
    }
    sb.append(whiteToMove ? " w " : " b ");
    String c = "";
    if (canCastle(WHITE_KINGSIDE)) c += "K";
    if (canCastle(WHITE_QUEENSIDE)) c += "Q";
    if (canCastle(BLACK_KINGSIDE)) c += "k";
    if (canCastle(BLACK_QUEENSIDE)) c += "q";
    sb.append(c.isEmpty() ? "-" : c);
    sb.append(' ').append(Square.name(epSquare));
    sb.append(' ').append(halfmoveClock).append(' ').append(fullmoveNumber);
    return sb.toString();
  }

  /** Text diagram, White at the bottom. Handy when debugging tests. */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int r = 7; r >= 0; r--) {
      for (int f = 0; f < 8; f++) sb.append(Piece.fenChar(board[Square.of(f, r)])).append(' ');
      sb.append('\n');
    }
    return sb.append(toFen()).toString();
  }
}
