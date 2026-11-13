package com.example.fishstock.eval;

import com.example.fishstock.engine.Attacks;
import com.example.fishstock.engine.Bitboards;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Square;

/**
 * Facts about a position, one named method per idea. The evaluator only combines these
 * with weights; it never inspects the board itself. To add a new idea, add a detector here,
 * a {@link Term} for its weight, and one line in {@link WeightedEvaluator}.
 *
 * Detectors (grouped like the original design notes):
 *  1. Attackers / defenders: {@link #attackers}, {@link #isDefended}, {@link #isHanging},
 *     {@link #exchangeLoss} (what the opponent wins by capturing here).
 *  2. Mobility: {@link #mobility}, {@link #isOnRim}, {@link #isInCorner}, {@link #isCentral}.
 *  3. Pins and reveal checkers: {@link #isPinnedToKing}, {@link #isPinnedToQueen},
 *     {@link #isRevealChecker}, {@link #isRevealQueenAttacker}.
 *  4. King safety: {@link #pawnShield}, {@link #openFilesNearKing}, {@link #kingZoneAttacks},
 *     {@link #kingEscapeSquares}, {@link #xRaysOnKing}, {@link #isCastled}, {@link #hasLostCastling}.
 *  5. Pawn structure: {@link #isPassedPawn}, {@link #isIsolatedPawn}, {@link #isDoubledPawn},
 *     {@link #isConnectedPawn}, {@link #isBackwardPawn}, {@link #ranksToPromotion},
 *     {@link #isOneStepFromPromotion}, {@link #isUnstoppablePawn}, {@link #pawnRaceWinner}.
 *  Plus outposts and placement: {@link #isOutpost}, {@link #isOnLongDiagonal},
 *     {@link #isOnOpenFile}, {@link #isOnSemiOpenFile}, {@link #isOnSeventhRank},
 *     {@link #rooksConnected}, {@link #hasBishopPair}, and game {@link #phase}.
 *
 * All square arguments are 0..63 (a1 = 0). "side" booleans are true for White.
 *
 * Internally everything is bitboards ({@link Bitboards}): sets of squares as 64-bit numbers, so
 * "is any enemy pawn ahead on the neighbouring files" is one AND with a precomputed mask. How many
 * pieces attack each square is kept as a "bit-sliced counter": counts[k] holds bit k of every
 * square's count, so adding a piece's attacks to all 64 counters is a handful of operations.
 * The answers are exactly those of the original square-by-square version.
 */
public final class BoardAnalysis {
  private static final int W = 0;
  private static final int B = 1;

  // Precomputed masks, [side][sq].
  /** Squares ahead of sq on its file. */
  private static final long[][] FORWARD = new long[2][64];
  /** Squares ahead on the file and the two neighbouring files (passed pawn test). */
  private static final long[][] PASSED_SPAN = new long[2][64];
  /** Squares ahead on the two neighbouring files only (outpost test). */
  private static final long[][] ADJ_AHEAD = new long[2][64];
  /** Beside, or one rank behind on a neighbouring file (connected pawn test). */
  private static final long[][] CONNECT = new long[2][64];
  /** Neighbouring files, on the same rank or behind (backward pawn test). */
  private static final long[][] ADJ_BEHIND_OR_LEVEL = new long[2][64];
  private static final long[] ADJ_FILES = new long[8];

  static {
    for (int f = 0; f < 8; f++) {
      if (f > 0) ADJ_FILES[f] |= Bitboards.FILE[f - 1];
      if (f < 7) ADJ_FILES[f] |= Bitboards.FILE[f + 1];
    }
    for (int sq = 0; sq < 64; sq++) {
      int f = Square.file(sq);
      int r = Square.rank(sq);
      for (int side = 0; side < 2; side++) {
        boolean white = side == W;
        long ahead = 0;
        long levelOrBehind = 0;
        for (int rr = 0; rr < 8; rr++) {
          if (white ? rr > r : rr < r) ahead |= Bitboards.RANK[rr];
          if (white ? rr <= r : rr >= r) levelOrBehind |= Bitboards.RANK[rr];
        }
        FORWARD[side][sq] = ahead & Bitboards.FILE[f];
        ADJ_AHEAD[side][sq] = ahead & ADJ_FILES[f];
        PASSED_SPAN[side][sq] = ahead & (ADJ_FILES[f] | Bitboards.FILE[f]);
        ADJ_BEHIND_OR_LEVEL[side][sq] = levelOrBehind & ADJ_FILES[f];
        long c = Bitboards.RANK[r] & ADJ_FILES[f];
        int behind = white ? r - 1 : r + 1;
        if (behind >= 0 && behind < 8) c |= Bitboards.RANK[behind] & ADJ_FILES[f];
        CONNECT[side][sq] = c;
      }
    }
  }

  private final Position pos;
  private final int[] board = new int[64];
  private final double[] pieceValues; // indexed by piece type, king = 100 for exchange maths

  private final long occ;
  private final long[] occSide = new long[2];
  /** pawns[side], and every piece type: piecesOf[side][type]. */
  private final long[][] piecesOf = new long[2][7];

  /** Bit-sliced attack counters per side: bit k of the count on each square. */
  private final long[][] counts = new long[2][5];
  /** Squares attacked at least once. */
  private final long[] attacked = new long[2];
  /** Pawn attacks towards the left and right file (a square can be hit by one of each). */
  private final long[] pawnLeft = new long[2];
  private final long[] pawnRight = new long[2];
  private final long[] pawnAttacks = new long[2];
  private final int[] mobility = new int[64];
  private long pinnedToKing;
  private long pinnedToQueen;
  private long revealChecker;
  private long revealQueenAttacker;
  private final int[][] pawnsOnFile = new int[2][8];
  private final int[] nonPawnMaterialCount = new int[2]; // knights+bishops+rooks+queens
  private final int phase;
  // Version 2: squares attacked by each piece type, and how hard each king is attacked.
  private final long[] knightAttack = new long[2];
  private final long[] bishopAttack = new long[2];
  private final long[] rookAttack = new long[2];
  private final long[] queenAttack = new long[2];
  private final int[] kingAttackUnits = new int[2];   // against this side's king
  private final int[] kingAttackers = new int[2];

  public BoardAnalysis(Position pos, EvalWeights w) {
    this.pos = pos;
    for (int sq = 0; sq < 64; sq++) board[sq] = pos.pieceAt(sq);
    pieceValues = new double[] {0, w.get(Term.PAWN_VALUE), w.get(Term.KNIGHT_VALUE),
        w.get(Term.BISHOP_VALUE), w.get(Term.ROOK_VALUE), w.get(Term.QUEEN_VALUE), 100.0};

    occSide[W] = pos.occupancy(true);
    occSide[B] = pos.occupancy(false);
    occ = occSide[W] | occSide[B];
    for (int side = 0; side < 2; side++) {
      for (int t = Piece.PAWN; t <= Piece.KING; t++) piecesOf[side][t] = pos.pieces(t, side == W);
    }

    int ph = 0;
    for (int side = 0; side < 2; side++) {
      long[] p = piecesOf[side];
      int minors = Long.bitCount(p[Piece.KNIGHT]) + Long.bitCount(p[Piece.BISHOP]);
      int rooks = Long.bitCount(p[Piece.ROOK]);
      int queens = Long.bitCount(p[Piece.QUEEN]);
      ph += minors + 2 * rooks + 4 * queens;
      nonPawnMaterialCount[side] = minors + rooks + queens;
      for (int f = 0; f < 8; f++) pawnsOnFile[side][f] = Long.bitCount(p[Piece.PAWN] & Bitboards.FILE[f]);
    }
    phase = Math.min(24, ph);

    // Pawn attacks (a pawn on the a- or h-file attacks only one square).
    long notA = ~Bitboards.FILE[0];
    long notH = ~Bitboards.FILE[7];
    long wp = piecesOf[W][Piece.PAWN];
    long bp = piecesOf[B][Piece.PAWN];
    pawnLeft[W] = (wp & notA) << 7;
    pawnRight[W] = (wp & notH) << 9;
    pawnLeft[B] = (bp & notA) >>> 9;
    pawnRight[B] = (bp & notH) >>> 7;
    for (int side = 0; side < 2; side++) {
      pawnAttacks[side] = pawnLeft[side] | pawnRight[side];
      addAttacks(side, pawnLeft[side]);
      addAttacks(side, pawnRight[side]);
    }

    // Piece attacks and mobility (mobility = squares not held by own pieces nor guarded by enemy pawns).
    for (int side = 0; side < 2; side++) {
      int enemy = 1 - side;
      long enemyKingZone = Bitboards.KING_ZONE[pos.kingSquare(enemy == W)];
      long safe = ~occSide[side] & ~pawnAttacks[enemy];
      long pieces = occSide[side] & ~piecesOf[side][Piece.PAWN];
      while (pieces != 0) {
        int sq = Long.numberOfTrailingZeros(pieces);
        pieces &= pieces - 1;
        int type = Piece.type(board[sq]);
        long att;
        switch (type) {
          case Piece.KNIGHT:
            att = Bitboards.KNIGHT[sq];
            knightAttack[side] |= att;
            break;
          case Piece.BISHOP:
            att = Bitboards.bishopAttacks(sq, occ);
            bishopAttack[side] |= att;
            break;
          case Piece.ROOK:
            att = Bitboards.rookAttacks(sq, occ);
            rookAttack[side] |= att;
            break;
          case Piece.QUEEN:
            att = Bitboards.queenAttacks(sq, occ);
            queenAttack[side] |= att;
            break;
          default:
            att = Bitboards.KING[sq];
            break;
        }
        addAttacks(side, att);
        mobility[sq] = Long.bitCount(att & safe);
        if (type != Piece.KING && (att & enemyKingZone) != 0) {
          kingAttackers[enemy]++;
          kingAttackUnits[enemy] += type == Piece.QUEEN ? 5 : type == Piece.ROOK ? 3 : 2;
        }
      }
    }
    for (int side = 0; side < 2; side++) {
      long[] c = counts[side];
      attacked[side] = c[0] | c[1] | c[2] | c[3] | c[4];
    }

    for (int side = 0; side < 2; side++) {
      boolean white = side == W;
      pinnedToKing |= findPins(pos.kingSquare(white), white, false);
      revealChecker |= findReveals(pos.kingSquare(!white), white, false);
      long queens = piecesOf[side][Piece.QUEEN];
      while (queens != 0) {
        int sq = Long.numberOfTrailingZeros(queens);
        queens &= queens - 1;
        pinnedToQueen |= findPins(sq, white, true);
        revealQueenAttacker |= findReveals(sq, !white, true);
      }
    }
  }

  /** Adds 1 to the attack count of every square in {@code b} (bit-sliced binary addition). */
  private void addAttacks(int side, long b) {
    long[] c = counts[side];
    long carry = b;
    for (int k = 0; k < c.length && carry != 0; k++) {
      long next = c[k] & carry;
      c[k] ^= carry;
      carry = next;
    }
  }

  private int countAt(int side, int sq) {
    long[] c = counts[side];
    int n = 0;
    for (int k = 0; k < c.length; k++) n |= (int) ((c[k] >>> sq) & 1L) << k;
    return n;
  }

  /** Sum of the attack counts over a set of squares. */
  private int countOver(int side, long squares) {
    long[] c = counts[side];
    int n = 0;
    for (int k = 0; k < c.length; k++) n += Long.bitCount(c[k] & squares) << k;
    return n;
  }

  // ================================================================ general

  public Position position() {
    return pos;
  }

  /** 24 = all pieces on the board (opening/middlegame), 0 = only kings and pawns. */
  public int phase() {
    return phase;
  }

  /** 1.0 in the middlegame fading to 0.0 in a pawn endgame. */
  public double middlegameWeight() {
    return phase / 24.0;
  }

  public double endgameWeight() {
    return 1.0 - phase / 24.0;
  }

  public double pieceValue(int piece) {
    return pieceValues[Piece.type(piece)];
  }

  /** Number of knights, bishops, rooks and queens the side has. */
  public int nonPawnPieceCount(boolean side) {
    return nonPawnMaterialCount[idx(side)];
  }

  public int pawnsOnFile(boolean side, int file) {
    if (file < 0 || file > 7) return 0;
    return pawnsOnFile[idx(side)][file];
  }

  // ================================================================ 1. attackers / defenders

  /** How many pieces of the given side attack this square (direct attacks only). */
  public int attackers(int sq, boolean bySide) {
    return countAt(idx(bySide), sq);
  }

  /** How many pawns of the given side attack this square. */
  public int pawnAttackers(int sq, boolean bySide) {
    int s = idx(bySide);
    return (int) ((pawnLeft[s] >>> sq) & 1L) + (int) ((pawnRight[s] >>> sq) & 1L);
  }

  /** Is the piece on sq defended by at least one friendly piece? */
  public boolean isDefended(int sq) {
    int p = board[sq];
    return p != Piece.EMPTY && (attacked[idx(p > 0)] & (1L << sq)) != 0;
  }

  /** Attacked by the enemy and not defended at all. */
  public boolean isHanging(int sq) {
    int p = board[sq];
    return p != Piece.EMPTY && (attacked[idx(p < 0)] & (1L << sq)) != 0 && !isDefended(sq);
  }

  /**
   * Static exchange evaluation: the material (in pawns, never negative) the opponent wins if
   * it starts capturing on sq and both sides keep recapturing with their cheapest piece while
   * it pays. 0 means the piece is safe where it stands. This is how "if the piece will get
   * captured on the next move, it shouldn't be worth much" is measured.
   */
  public double exchangeLoss(int sq) {
    int target = board[sq];
    if (target == Piece.EMPTY || Piece.type(target) == Piece.KING) return 0;
    boolean attackerSide = target < 0;
    if ((attacked[idx(attackerSide)] & (1L << sq)) == 0) return 0;

    long o = occ; // pieces still on the board as the exchange goes on
    double[] gain = new double[34];
    int depth = 0;
    gain[0] = pieceValues[Piece.type(target)];
    boolean side = attackerSide;
    int from = leastValuableAttacker(o, sq, side);
    if (from < 0) return 0;
    while (from >= 0 && depth < 32) {
      int capturer = board[from];
      depth++;
      gain[depth] = pieceValues[Piece.type(capturer)] - gain[depth - 1];
      o &= ~(1L << from); // the capturer now stands on sq (sq itself is never looked at)
      side = !side;
      from = leastValuableAttacker(o, sq, side);
    }
    while (--depth > 0) gain[depth - 1] = -Math.max(-gain[depth - 1], gain[depth]);
    return Math.max(0, gain[0]);
  }

  /**
   * Square of the cheapest piece of {@code side} attacking sq with only the pieces in {@code o}
   * on the board, or -1. Ties go to the same piece as always (a lower direction number first),
   * so x-rays open up in a fixed order.
   */
  private int leastValuableAttacker(long o, int sq, boolean side) {
    int s = idx(side);
    long pawns = Bitboards.PAWN[side ? 1 : 0][sq] & piecesOf[s][Piece.PAWN] & o;
    if (pawns != 0) return Long.numberOfTrailingZeros(pawns);
    long knights = Bitboards.KNIGHT[sq] & piecesOf[s][Piece.KNIGHT] & o;
    // Which knight doesn't matter: a knight never stands on a line through sq, so taking it
    // off the board can't uncover a rook, bishop or queen behind it.
    if (knights != 0) return Long.numberOfTrailingZeros(knights);
    int bestSq = -1;
    int bestType = 99;
    for (int d = 0; d < 8; d++) {
      int b = Bitboards.firstBlocker(d, sq, o);
      if (b < 0) continue;
      int p = board[b];
      int t = Piece.type(p);
      if (Piece.isColor(p, side) && Attacks.slidesAlong(t, d) && t < bestType) {
        bestType = t;
        bestSq = b;
      }
    }
    if (bestSq >= 0) return bestSq;
    long king = Bitboards.KING[sq] & piecesOf[s][Piece.KING] & o;
    return king != 0 ? Long.numberOfTrailingZeros(king) : -1;
  }

  // ================================================================ 2. mobility and squares

  /** Safe squares the piece on sq can reach (not own-occupied, not guarded by enemy pawns). */
  public int mobility(int sq) {
    return mobility[sq];
  }

  public static boolean isOnRim(int sq) {
    int f = Square.file(sq);
    int r = Square.rank(sq);
    return f == 0 || f == 7 || r == 0 || r == 7;
  }

  public static boolean isInCorner(int sq) {
    int f = Square.file(sq);
    int r = Square.rank(sq);
    return (f == 0 || f == 7) && (r == 0 || r == 7);
  }

  /** d4, e4, d5 or e5. */
  public static boolean isCentral(int sq) {
    int f = Square.file(sq);
    int r = Square.rank(sq);
    return (f == 3 || f == 4) && (r == 3 || r == 4);
  }

  // ================================================================ 3. pins and reveal checkers

  /** The piece on sq cannot leave its line without exposing its own king. */
  public boolean isPinnedToKing(int sq) {
    return (pinnedToKing & (1L << sq)) != 0;
  }

  /** The piece on sq shields its own queen from an enemy rook or bishop. */
  public boolean isPinnedToQueen(int sq) {
    return (pinnedToQueen & (1L << sq)) != 0;
  }

  /** Moving the piece on sq would give a discovered check. */
  public boolean isRevealChecker(int sq) {
    return (revealChecker & (1L << sq)) != 0;
  }

  /** Moving the piece on sq would uncover an attack on the enemy queen. */
  public boolean isRevealQueenAttacker(int sq) {
    return (revealQueenAttacker & (1L << sq)) != 0;
  }

  /**
   * From {@code anchor} (a king or queen of side {@code white}) look along every line:
   * own piece followed by an enemy slider on that line = pinned.
   * For queen pins only cheaper sliders (rook/bishop) count as pinners.
   */
  private long findPins(int anchor, boolean white, boolean queenAnchor) {
    long out = 0;
    for (int d = 0; d < 8; d++) {
      int shield = Bitboards.firstBlocker(d, anchor, occ);
      if (shield < 0) continue;
      int p = board[shield];
      if (!Piece.isColor(p, white) || Piece.type(p) == Piece.KING) continue;
      int behind = Bitboards.firstBlocker(d, shield, occ);
      if (behind < 0) continue;
      int q = board[behind];
      int t = Piece.type(q);
      if (Piece.isColor(q, !white) && Attacks.slidesAlong(t, d) && (!queenAnchor || t != Piece.QUEEN)) {
        out |= 1L << shield;
      }
    }
    return out;
  }

  /**
   * From the enemy {@code target} (king or queen) look along every line: a piece of side
   * {@code attackerWhite} followed by that side's slider = the first piece is a reveal piece.
   */
  private long findReveals(int target, boolean attackerWhite, boolean queenTarget) {
    long out = 0;
    for (int d = 0; d < 8; d++) {
      int front = Bitboards.firstBlocker(d, target, occ);
      if (front < 0) continue;
      int p = board[front];
      if (!Piece.isColor(p, attackerWhite) || Piece.type(p) == Piece.KING) continue;
      int behind = Bitboards.firstBlocker(d, front, occ);
      if (behind < 0) continue;
      int q = board[behind];
      int t = Piece.type(q);
      if (Piece.isColor(q, attackerWhite) && Attacks.slidesAlong(t, d) && (!queenTarget || t != Piece.QUEEN)) {
        out |= 1L << front;
      }
    }
    return out;
  }

  // ================================================================ 4. king safety

  /** Friendly pawns in front of the king: 1 per pawn directly ahead, 0.5 two squares ahead. */
  public double pawnShield(boolean side) {
    int k = pos.kingSquare(side);
    int kf = Square.file(k);
    int kr = Square.rank(k);
    int dir = side ? 1 : -1;
    int pawn = Piece.make(Piece.PAWN, side);
    double shield = 0;
    for (int f = kf - 1; f <= kf + 1; f++) {
      if (f < 0 || f > 7) continue;
      if (Square.onBoard(f, kr + dir) && board[Square.of(f, kr + dir)] == pawn) shield += 1.0;
      else if (Square.onBoard(f, kr + 2 * dir) && board[Square.of(f, kr + 2 * dir)] == pawn) shield += 0.5;
    }
    return shield;
  }

  /** Files on and beside the king that have no friendly pawn. */
  public int openFilesNearKing(boolean side) {
    int kf = Square.file(pos.kingSquare(side));
    int n = 0;
    for (int f = kf - 1; f <= kf + 1; f++) {
      if (f >= 0 && f <= 7 && pawnsOnFile(side, f) == 0) n++;
    }
    return n;
  }

  /** Sum of enemy attacks on the king's square and the squares around it. */
  public int kingZoneAttacks(boolean side) {
    return countOver(idx(!side), Bitboards.KING_ZONE[pos.kingSquare(side)]);
  }

  /** Squares the king could step to that are empty or enemy-held and not attacked. */
  public int kingEscapeSquares(boolean side) {
    int k = pos.kingSquare(side);
    return Long.bitCount(Bitboards.KING[k] & ~occSide[idx(side)] & ~attacked[idx(!side)]);
  }

  /** Enemy rooks/bishops/queens lined up with the king with one or two pieces in between. */
  public int xRaysOnKing(boolean side) {
    int k = pos.kingSquare(side);
    int n = 0;
    for (int d = 0; d < 8; d++) {
      int blockers = 0;
      int s = Bitboards.firstBlocker(d, k, occ);
      while (s >= 0) {
        int p = board[s];
        if (Piece.isColor(p, !side) && Attacks.slidesAlong(Piece.type(p), d)) {
          if (blockers >= 1 && blockers <= 2) n++;
          break;
        }
        blockers++;
        if (blockers > 2) break;
        s = Bitboards.firstBlocker(d, s, occ);
      }
    }
    return n;
  }

  /** King on its back rank and on a wing (castled, or walked there). */
  public boolean isCastled(boolean side) {
    int k = pos.kingSquare(side);
    int f = Square.file(k);
    return Square.relativeRank(k, side) == 0 && (f >= 6 || f <= 2);
  }

  /** No castling rights left and the king is not already on a wing. */
  public boolean hasLostCastling(boolean side) {
    int rights = side ? (Position.WHITE_KINGSIDE | Position.WHITE_QUEENSIDE)
        : (Position.BLACK_KINGSIDE | Position.BLACK_QUEENSIDE);
    return (pos.castlingRights() & rights) == 0 && !isCastled(side);
  }

  // ================================================================ 5. pawn structure

  /** No enemy pawn ahead on this file or the neighbouring files. */
  public boolean isPassedPawn(int sq) {
    boolean white = board[sq] > 0;
    return (PASSED_SPAN[idx(white)][sq] & piecesOf[idx(!white)][Piece.PAWN]) == 0;
  }

  /** No friendly pawns on the neighbouring files. */
  public boolean isIsolatedPawn(int sq) {
    boolean white = board[sq] > 0;
    return (ADJ_FILES[Square.file(sq)] & piecesOf[idx(white)][Piece.PAWN]) == 0;
  }

  /** Another friendly pawn stands ahead of this one on the same file. */
  public boolean isDoubledPawn(int sq) {
    int p = board[sq];
    boolean white = p > 0;
    return (FORWARD[idx(white)][sq] & pos.pieces(p)) != 0;
  }

  /** A friendly pawn stands beside it or defends it. */
  public boolean isConnectedPawn(int sq) {
    int p = board[sq];
    boolean white = p > 0;
    return (CONNECT[idx(white)][sq] & pos.pieces(p)) != 0;
  }

  /**
   * Behind all friendly pawns on the neighbouring files (so none can ever support it)
   * and its next square is guarded by an enemy pawn.
   */
  public boolean isBackwardPawn(int sq) {
    int p = board[sq];
    boolean white = p > 0;
    if (isIsolatedPawn(sq)) return false;
    if ((ADJ_BEHIND_OR_LEVEL[idx(white)][sq] & pos.pieces(p)) != 0) return false;
    int stop = sq + (white ? 8 : -8);
    return stop >= 0 && stop < 64 && (pawnAttacks[idx(!white)] & (1L << stop)) != 0;
  }

  /** Defended by a friendly pawn. */
  public boolean isPawnProtected(int sq) {
    int p = board[sq];
    return p != Piece.EMPTY && (pawnAttacks[idx(p > 0)] & (1L << sq)) != 0;
  }

  /** Moves needed to reach the last rank (1 = on the 7th). */
  public int ranksToPromotion(int sq) {
    return 7 - Square.relativeRank(sq, board[sq] > 0);
  }

  /** On the 7th rank with the queening square empty. */
  public boolean isOneStepFromPromotion(int sq) {
    boolean white = board[sq] > 0;
    return ranksToPromotion(sq) == 1 && board[sq + (white ? 8 : -8)] == Piece.EMPTY;
  }

  /**
   * Rule of the square: the opponent has only king and pawns and its king cannot reach the
   * queening square in time. Our own pieces blocking the path make it not count.
   */
  public boolean isUnstoppablePawn(int sq) {
    boolean white = board[sq] > 0;
    if (nonPawnPieceCount(!white) > 0 || !isPassedPawn(sq)) return false;
    if ((FORWARD[idx(white)][sq] & occ) != 0) return false;
    int f = Square.file(sq);
    int promo = Square.of(f, white ? 7 : 0);
    int movesNeeded = ranksToPromotion(sq);
    if (Square.relativeRank(sq, white) == 1) movesNeeded--; // double step available
    int enemyKing = pos.kingSquare(!white);
    int kingMoves = Square.distance(enemyKing, promo);
    if (pos.whiteToMove() != white) kingMoves--; // defender moves first
    return kingMoves > movesNeeded;
  }

  /**
   * Moves the pawn on sq needs to promote if nothing can stop it ({@link #isUnstoppablePawn}),
   * counting the double step; -1 if it can be stopped.
   */
  public int movesToPromoteUnstoppable(int sq) {
    if (!isUnstoppablePawn(sq)) return -1;
    boolean white = board[sq] > 0;
    int moves = ranksToPromotion(sq);
    if (Square.relativeRank(sq, white) == 1) moves--;
    return moves;
  }

  /**
   * Pawn race: +1 if White's fastest unstoppable pawn queens before any Black pawn can,
   * -1 if Black's does, 0 if neither side has an unstoppable pawn that wins the race.
   * Whoever is to move gets there first on a tie.
   */
  public int pawnRaceWinner() {
    int fw = Integer.MAX_VALUE;
    int fb = Integer.MAX_VALUE;
    // Only a side facing a bare king (and pawns) can have an unstoppable pawn.
    if (nonPawnMaterialCount[B] == 0) fw = fastestUnstoppable(piecesOf[W][Piece.PAWN]);
    if (nonPawnMaterialCount[W] == 0) fb = fastestUnstoppable(piecesOf[B][Piece.PAWN]);
    if (fw == Integer.MAX_VALUE && fb == Integer.MAX_VALUE) return 0;
    boolean whiteMoves = pos.whiteToMove();
    // Plies until each side's queen appears.
    long wPly = fw == Integer.MAX_VALUE ? Long.MAX_VALUE : 2L * fw - (whiteMoves ? 1 : 0);
    long bPly = fb == Integer.MAX_VALUE ? Long.MAX_VALUE : 2L * fb - (whiteMoves ? 0 : 1);
    if (wPly < bPly) return 1;
    if (bPly < wPly) return -1;
    return 0;
  }

  private int fastestUnstoppable(long pawns) {
    int best = Integer.MAX_VALUE;
    while (pawns != 0) {
      int sq = Long.numberOfTrailingZeros(pawns);
      pawns &= pawns - 1;
      int n = movesToPromoteUnstoppable(sq);
      if (n >= 0) best = Math.min(best, n);
    }
    return best;
  }

  // ================================================================ outposts and placement

  /**
   * An outpost: on the 4th-6th rank (from the owner's side), defended by a friendly pawn,
   * and no enemy pawn on a neighbouring file can ever advance to attack it.
   */
  public boolean isOutpost(int sq) {
    boolean white = board[sq] > 0;
    int rel = Square.relativeRank(sq, white);
    if (rel < 3 || rel > 5) return false;
    if (!isPawnProtected(sq)) return false;
    return (ADJ_AHEAD[idx(white)][sq] & piecesOf[idx(!white)][Piece.PAWN]) == 0;
  }

  /** Outpost on the 6th rank (deep in enemy territory). */
  public boolean isAdvancedOutpost(int sq) {
    return isOutpost(sq) && Square.relativeRank(sq, board[sq] > 0) == 5;
  }

  /** On the a1-h8 or h1-a8 diagonal. */
  public static boolean isOnLongDiagonal(int sq) {
    int f = Square.file(sq);
    int r = Square.rank(sq);
    return f == r || f + r == 7;
  }

  public boolean hasBishopPair(boolean side) {
    long b = piecesOf[idx(side)][Piece.BISHOP];
    return (b & Bitboards.LIGHT_SQUARES) != 0 && (b & ~Bitboards.LIGHT_SQUARES) != 0;
  }

  /** No pawns of either colour on this piece's file. */
  public boolean isOnOpenFile(int sq) {
    int f = Square.file(sq);
    return pawnsOnFile[W][f] == 0 && pawnsOnFile[B][f] == 0;
  }

  /** No friendly pawns, but at least one enemy pawn, on this piece's file. */
  public boolean isOnSemiOpenFile(int sq) {
    boolean white = board[sq] > 0;
    int f = Square.file(sq);
    return pawnsOnFile(white, f) == 0 && pawnsOnFile(!white, f) > 0;
  }

  /** On the opponent's second rank. */
  public boolean isOnSeventhRank(int sq) {
    return Square.relativeRank(sq, board[sq] > 0) == 6;
  }

  /** Two rooks of this side see each other along a rank or file. */
  public boolean rooksConnected(boolean side) {
    long rooks = piecesOf[idx(side)][Piece.ROOK];
    if (Long.bitCount(rooks) < 2) return false;
    long r = rooks;
    while (r != 0) {
      int sq = Long.numberOfTrailingZeros(r);
      r &= r - 1;
      if ((Bitboards.rookAttacks(sq, occ) & rooks) != 0) return true;
    }
    return false;
  }

  /** A knight or bishop still on its original square. */
  public boolean isUndevelopedMinor(int sq) {
    int p = board[sq];
    boolean white = p > 0;
    int type = Piece.type(p);
    if (Square.relativeRank(sq, white) != 0) return false;
    int f = Square.file(sq);
    return (type == Piece.KNIGHT && (f == 1 || f == 6)) || (type == Piece.BISHOP && (f == 2 || f == 5));
  }

  private static final long KNIGHT_HOMES = (1L << 1) | (1L << 6) | (1L << 57) | (1L << 62);
  private static final long BISHOP_HOMES = (1L << 2) | (1L << 5) | (1L << 58) | (1L << 61);

  public int undevelopedMinors(boolean side) {
    int s = idx(side);
    long backRank = Bitboards.RANK[side ? 0 : 7];
    return Long.bitCount(piecesOf[s][Piece.KNIGHT] & KNIGHT_HOMES & backRank)
        + Long.bitCount(piecesOf[s][Piece.BISHOP] & BISHOP_HOMES & backRank);
  }

  /** The queen has left d1/d8. */
  public boolean queenHasLeftHome(boolean side) {
    long queens = piecesOf[idx(side)][Piece.QUEEN];
    int home = Square.of(3, side ? 0 : 7);
    return queens != 0 && (queens & (1L << home)) == 0;
  }

  // ================================================================ version 2 detectors

  /** Enemy knights, bishops, rooks and queens attacked by our pawns. */
  public int threatsByPawn(boolean side) {
    long[] e = piecesOf[idx(!side)];
    long targets = e[Piece.KNIGHT] | e[Piece.BISHOP] | e[Piece.ROOK] | e[Piece.QUEEN];
    return Long.bitCount(targets & pawnAttacks[idx(side)]);
  }

  /** Enemy rooks and queens attacked by our knights or bishops. */
  public int threatsByMinor(boolean side) {
    long[] e = piecesOf[idx(!side)];
    int s = idx(side);
    return Long.bitCount((e[Piece.ROOK] | e[Piece.QUEEN]) & (knightAttack[s] | bishopAttack[s]));
  }

  /** Enemy queens attacked by our rooks. */
  public int threatsByRook(boolean side) {
    return Long.bitCount(piecesOf[idx(!side)][Piece.QUEEN] & rookAttack[idx(side)]);
  }

  /**
   * Attack units on this side's king zone (king square and neighbours): knight/bishop 2, rook 3,
   * queen 5 for every enemy piece reaching it. 0 unless at least two pieces take part.
   */
  public int kingAttackUnits(boolean side) {
    return kingAttackers[idx(side)] >= 2 ? kingAttackUnits[idx(side)] : 0;
  }

  /**
   * Squares from which the enemy could give check to this side's king with a piece that can get
   * there, where we don't guard the square.
   */
  public int safeChecks(boolean side) {
    int k = pos.kingSquare(side);
    int s = idx(side);
    int e = 1 - s;
    long free = ~occSide[e] & ~attacked[s];
    int n = Long.bitCount(Bitboards.KNIGHT[k] & free & knightAttack[e]);
    n += Long.bitCount(Bitboards.rookAttacks(k, occ) & free & (rookAttack[e] | queenAttack[e]));
    n += Long.bitCount(Bitboards.bishopAttacks(k, occ) & free & (bishopAttack[e] | queenAttack[e]));
    return n;
  }

  /** Squares on the c-f files, our 2nd-4th ranks, behind one of our pawns and not hit by enemy pawns. */
  public int space(boolean side) {
    int pawn = Piece.make(Piece.PAWN, side);
    long enemyPawnAtt = pawnAttacks[idx(!side)];
    int n = 0;
    for (int f = 2; f <= 5; f++) {
      for (int rel = 1; rel <= 3; rel++) {
        int sq = Square.of(f, side ? rel : 7 - rel);
        if (board[sq] == pawn || (enemyPawnAtt & (1L << sq)) != 0) continue;
        for (int ahead = rel + 1; ahead <= rel + 3 && ahead <= 6; ahead++) {
          if (board[Square.of(f, side ? ahead : 7 - ahead)] == pawn) {
            n++;
            break;
          }
        }
      }
    }
    return n;
  }

  /** Own pawns on the bishop's square colour; blocked ones count twice. */
  public int badBishopPawns(int sq) {
    boolean white = board[sq] > 0;
    long colour = Square.isLight(sq) ? Bitboards.LIGHT_SQUARES : ~Bitboards.LIGHT_SQUARES;
    long pawns = piecesOf[idx(white)][Piece.PAWN] & colour;
    long blocked = pawns & (white ? occ >>> 8 : occ << 8);
    return Long.bitCount(pawns) + Long.bitCount(blocked);
  }

  /** A bishop on a7/h7 (a2/h2 for Black) with an enemy pawn on b6/g6 (b3/g3) shutting it in. */
  public boolean isTrappedBishop(int sq) {
    boolean white = board[sq] > 0;
    int f = Square.file(sq);
    int rel = Square.relativeRank(sq, white);
    if (rel != 6 || (f != 0 && f != 7)) return false;
    int pf = f == 0 ? 1 : 6;
    int pr = white ? 5 : 2;
    return board[Square.of(pf, pr)] == Piece.make(Piece.PAWN, !white);
  }

  /** A rook in the corner beside its uncastled king (no castling rights), hardly able to move. */
  public boolean isTrappedRook(int sq) {
    boolean white = board[sq] > 0;
    if (Square.relativeRank(sq, white) != 0 || mobility[sq] > 3) return false;
    int rights = white ? (Position.WHITE_KINGSIDE | Position.WHITE_QUEENSIDE)
        : (Position.BLACK_KINGSIDE | Position.BLACK_QUEENSIDE);
    if ((pos.castlingRights() & rights) != 0) return false;
    int k = pos.kingSquare(white);
    if (Square.rank(k) != Square.rank(sq)) return false;
    int kf = Square.file(k);
    int rf = Square.file(sq);
    return (kf >= 5 && rf > kf) || (kf <= 3 && rf < kf);
  }

  /** Our rook stands behind this passed pawn on its file with nothing in between. */
  public boolean isRookBehindPasser(int sq) {
    boolean white = board[sq] > 0;
    int s = Bitboards.firstBlocker(white ? Attacks.SOUTH : Attacks.NORTH, sq, occ);
    return s >= 0 && board[s] == Piece.make(Piece.ROOK, white);
  }

  /** Every square ahead of the pawn is empty and the next one isn't attacked by the enemy. */
  public boolean hasFreePath(int sq) {
    boolean white = board[sq] > 0;
    int next = sq + (white ? 8 : -8);
    if (next < 0 || next >= 64 || (attacked[idx(!white)] & (1L << next)) != 0) return false;
    return (FORWARD[idx(white)][sq] & occ) == 0;
  }

  /** Enemy king's distance to the pawn's next square minus ours (each capped at 5). */
  public int passerKingEdge(int sq) {
    boolean white = board[sq] > 0;
    int next = sq + (white ? 8 : -8);
    if (next < 0 || next >= 64) return 0;
    int theirs = Math.min(5, Square.distance(pos.kingSquare(!white), next));
    int ours = Math.min(5, Square.distance(pos.kingSquare(white), next));
    return theirs - ours;
  }

  /** Number of pawns the side has. */
  public int pawnCount(boolean side) {
    return Long.bitCount(piecesOf[idx(side)][Piece.PAWN]);
  }

  // ================================================================ helpers

  private static int idx(boolean white) {
    return white ? W : B;
  }
}
