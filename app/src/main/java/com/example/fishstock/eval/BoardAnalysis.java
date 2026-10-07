package com.example.fishstock.eval;

import com.example.fishstock.engine.Attacks;
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
 */
public final class BoardAnalysis {
  private static final int W = 0;
  private static final int B = 1;

  private final Position pos;
  private final int[] board = new int[64];
  private final double[] pieceValues; // indexed by piece type, king = 100 for exchange maths

  private final int[][] attackCount = new int[2][64];
  private final int[][] pawnAttackCount = new int[2][64];
  private final int[] mobility = new int[64];
  private final boolean[] pinnedToKing = new boolean[64];
  private final boolean[] pinnedToQueen = new boolean[64];
  private final boolean[] revealChecker = new boolean[64];
  private final boolean[] revealQueenAttacker = new boolean[64];
  private final int[][] pawnsOnFile = new int[2][8];
  private final int[] nonPawnMaterialCount = new int[2]; // knights+bishops+rooks+queens
  private final int phase;
  // Version 2: attacks by piece type, and how hard each king is attacked.
  private final int[][] knightAttack = new int[2][64];
  private final int[][] bishopAttack = new int[2][64];
  private final int[][] rookAttack = new int[2][64];
  private final int[][] queenAttack = new int[2][64];
  private final int[] kingAttackUnits = new int[2];   // against this side's king
  private final int[] kingAttackers = new int[2];

  public BoardAnalysis(Position pos, EvalWeights w) {
    this.pos = pos;
    for (int sq = 0; sq < 64; sq++) board[sq] = pos.pieceAt(sq);
    pieceValues = new double[] {0, w.get(Term.PAWN_VALUE), w.get(Term.KNIGHT_VALUE),
        w.get(Term.BISHOP_VALUE), w.get(Term.ROOK_VALUE), w.get(Term.QUEEN_VALUE), 100.0};

    int ph = 0;
    for (int sq = 0; sq < 64; sq++) {
      int p = board[sq];
      if (p == Piece.EMPTY) continue;
      int side = p > 0 ? W : B;
      switch (Piece.type(p)) {
        case Piece.PAWN:
          pawnsOnFile[side][Square.file(sq)]++;
          for (int t : Attacks.PAWN_ATTACKS[side][sq]) {
            pawnAttackCount[side][t]++;
            attackCount[side][t]++;
          }
          break;
        case Piece.KNIGHT:
        case Piece.BISHOP:
          ph += 1;
          nonPawnMaterialCount[side]++;
          break;
        case Piece.ROOK:
          ph += 2;
          nonPawnMaterialCount[side]++;
          break;
        case Piece.QUEEN:
          ph += 4;
          nonPawnMaterialCount[side]++;
          break;
        default:
          break;
      }
    }
    phase = Math.min(24, ph);

    // Piece attacks and mobility (mobility = squares not held by own pieces nor guarded by enemy pawns).
    for (int sq = 0; sq < 64; sq++) {
      int p = board[sq];
      int type = Piece.type(p);
      if (p == Piece.EMPTY || type == Piece.PAWN) continue;
      int side = p > 0 ? W : B;
      int enemy = 1 - side;
      int enemyKing = pos.kingSquare(enemy == W);
      int[][] typeAttack = type == Piece.KNIGHT ? knightAttack : type == Piece.BISHOP ? bishopAttack
          : type == Piece.ROOK ? rookAttack : type == Piece.QUEEN ? queenAttack : null;
      boolean hitsKingZone = false;
      int mob = 0;
      if (type == Piece.KNIGHT || type == Piece.KING) {
        int[] targets = type == Piece.KNIGHT ? Attacks.KNIGHT[sq] : Attacks.KING[sq];
        for (int t : targets) {
          attackCount[side][t]++;
          if (typeAttack != null) typeAttack[side][t]++;
          if (Square.distance(t, enemyKing) <= 1) hitsKingZone = true;
          if (!isOwn(board[t], side) && pawnAttackCount[enemy][t] == 0) mob++;
        }
      } else {
        for (int d = 0; d < 8; d++) {
          if (!Attacks.slidesAlong(type, d)) continue;
          for (int t : Attacks.RAYS[sq][d]) {
            attackCount[side][t]++;
            typeAttack[side][t]++;
            if (Square.distance(t, enemyKing) <= 1) hitsKingZone = true;
            if (!isOwn(board[t], side) && pawnAttackCount[enemy][t] == 0) mob++;
            if (board[t] != Piece.EMPTY) break;
          }
        }
      }
      mobility[sq] = mob;
      if (hitsKingZone && type != Piece.KING) {
        kingAttackers[enemy]++;
        kingAttackUnits[enemy] += type == Piece.QUEEN ? 5 : type == Piece.ROOK ? 3 : 2;
      }
    }

    for (int side = 0; side < 2; side++) {
      boolean white = side == W;
      findPins(pos.kingSquare(white), white, pinnedToKing, false);
      findReveals(pos.kingSquare(!white), white, revealChecker, false);
      for (int sq = 0; sq < 64; sq++) {
        if (board[sq] == Piece.make(Piece.QUEEN, white)) {
          findPins(sq, white, pinnedToQueen, true);
          findReveals(sq, !white, revealQueenAttacker, true);
        }
      }
    }
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
    return attackCount[idx(bySide)][sq];
  }

  /** How many enemy pawns attack this square. */
  public int pawnAttackers(int sq, boolean bySide) {
    return pawnAttackCount[idx(bySide)][sq];
  }

  /** Is the piece on sq defended by at least one friendly piece? */
  public boolean isDefended(int sq) {
    int p = board[sq];
    return p != Piece.EMPTY && attackCount[idx(p > 0)][sq] > 0;
  }

  /** Attacked by the enemy and not defended at all. */
  public boolean isHanging(int sq) {
    int p = board[sq];
    return p != Piece.EMPTY && attackers(sq, p < 0) > 0 && !isDefended(sq);
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
    if (attackers(sq, attackerSide) == 0) return 0;

    int[] b = board.clone();
    double[] gain = new double[34];
    int depth = 0;
    gain[0] = pieceValues[Piece.type(target)];
    boolean side = attackerSide;
    int from = leastValuableAttacker(b, sq, side);
    if (from < 0) return 0;
    while (from >= 0 && depth < 32) {
      int capturer = b[from];
      depth++;
      gain[depth] = pieceValues[Piece.type(capturer)] - gain[depth - 1];
      b[from] = Piece.EMPTY; // the capturer now stands on sq (sq itself is never scanned)
      side = !side;
      from = leastValuableAttacker(b, sq, side);
    }
    while (--depth > 0) gain[depth - 1] = -Math.max(-gain[depth - 1], gain[depth]);
    return Math.max(0, gain[0]);
  }

  /** Square of the cheapest piece of {@code side} attacking sq on board b, or -1. */
  private static int leastValuableAttacker(int[] b, int sq, boolean side) {
    int pawn = Piece.make(Piece.PAWN, side);
    for (int from : Attacks.PAWN_ATTACKS[side ? 1 : 0][sq]) if (b[from] == pawn) return from;
    int knight = Piece.make(Piece.KNIGHT, side);
    for (int from : Attacks.KNIGHT[sq]) if (b[from] == knight) return from;
    int bestSq = -1;
    int bestType = 99;
    for (int d = 0; d < 8; d++) {
      for (int s : Attacks.RAYS[sq][d]) {
        int p = b[s];
        if (p == Piece.EMPTY) continue;
        int t = Piece.type(p);
        if (Piece.isColor(p, side) && Attacks.slidesAlong(t, d) && t < bestType) {
          bestType = t;
          bestSq = s;
        }
        break;
      }
    }
    if (bestSq >= 0) return bestSq;
    int king = Piece.make(Piece.KING, side);
    for (int from : Attacks.KING[sq]) if (b[from] == king) return from;
    return -1;
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
    return pinnedToKing[sq];
  }

  /** The piece on sq shields its own queen from an enemy rook or bishop. */
  public boolean isPinnedToQueen(int sq) {
    return pinnedToQueen[sq];
  }

  /** Moving the piece on sq would give a discovered check. */
  public boolean isRevealChecker(int sq) {
    return revealChecker[sq];
  }

  /** Moving the piece on sq would uncover an attack on the enemy queen. */
  public boolean isRevealQueenAttacker(int sq) {
    return revealQueenAttacker[sq];
  }

  /**
   * From {@code anchor} (a king or queen of side {@code white}) look along every line:
   * own piece followed by an enemy slider on that line = pinned.
   * For queen pins only cheaper sliders (rook/bishop) count as pinners.
   */
  private void findPins(int anchor, boolean white, boolean[] out, boolean queenAnchor) {
    for (int d = 0; d < 8; d++) {
      int shield = -1;
      for (int s : Attacks.RAYS[anchor][d]) {
        int p = board[s];
        if (p == Piece.EMPTY) continue;
        if (shield < 0) {
          if (Piece.isColor(p, white) && Piece.type(p) != Piece.KING) {
            shield = s;
            continue;
          }
          break;
        }
        int t = Piece.type(p);
        if (Piece.isColor(p, !white) && Attacks.slidesAlong(t, d) && (!queenAnchor || t != Piece.QUEEN)) {
          out[shield] = true;
        }
        break;
      }
    }
  }

  /**
   * From the enemy {@code target} (king or queen) look along every line: a piece of side
   * {@code attackerWhite} followed by that side's slider = the first piece is a reveal piece.
   */
  private void findReveals(int target, boolean attackerWhite, boolean[] out, boolean queenTarget) {
    for (int d = 0; d < 8; d++) {
      int front = -1;
      for (int s : Attacks.RAYS[target][d]) {
        int p = board[s];
        if (p == Piece.EMPTY) continue;
        if (front < 0) {
          if (Piece.isColor(p, attackerWhite) && Piece.type(p) != Piece.KING) {
            front = s;
            continue;
          }
          break;
        }
        int t = Piece.type(p);
        if (Piece.isColor(p, attackerWhite) && Attacks.slidesAlong(t, d) && (!queenTarget || t != Piece.QUEEN)) {
          out[front] = true;
        }
        break;
      }
    }
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
    int k = pos.kingSquare(side);
    int enemy = idx(!side);
    int n = attackCount[enemy][k];
    for (int s : Attacks.KING[k]) n += attackCount[enemy][s];
    return n;
  }

  /** Squares the king could step to that are empty or enemy-held and not attacked. */
  public int kingEscapeSquares(boolean side) {
    int k = pos.kingSquare(side);
    int n = 0;
    for (int s : Attacks.KING[k]) {
      if (!isOwn(board[s], idx(side)) && attackCount[idx(!side)][s] == 0) n++;
    }
    return n;
  }

  /** Enemy rooks/bishops/queens lined up with the king with one or two pieces in between. */
  public int xRaysOnKing(boolean side) {
    int k = pos.kingSquare(side);
    int n = 0;
    for (int d = 0; d < 8; d++) {
      int blockers = 0;
      for (int s : Attacks.RAYS[k][d]) {
        int p = board[s];
        if (p == Piece.EMPTY) continue;
        if (Piece.isColor(p, !side) && Attacks.slidesAlong(Piece.type(p), d)) {
          if (blockers >= 1 && blockers <= 2) n++;
          break;
        }
        blockers++;
        if (blockers > 2) break;
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
    int p = board[sq];
    boolean white = p > 0;
    int f = Square.file(sq);
    int r = Square.rank(sq);
    int enemyPawn = Piece.make(Piece.PAWN, !white);
    for (int ff = f - 1; ff <= f + 1; ff++) {
      if (ff < 0 || ff > 7 || pawnsOnFile(!white, ff) == 0) continue;
      for (int rr = white ? r + 1 : r - 1; rr >= 0 && rr < 8; rr += white ? 1 : -1) {
        if (board[Square.of(ff, rr)] == enemyPawn) return false;
      }
    }
    return true;
  }

  /** No friendly pawns on the neighbouring files. */
  public boolean isIsolatedPawn(int sq) {
    boolean white = board[sq] > 0;
    int f = Square.file(sq);
    return pawnsOnFile(white, f - 1) == 0 && pawnsOnFile(white, f + 1) == 0;
  }

  /** Another friendly pawn stands ahead of this one on the same file. */
  public boolean isDoubledPawn(int sq) {
    int p = board[sq];
    boolean white = p > 0;
    int f = Square.file(sq);
    if (pawnsOnFile(white, f) < 2) return false;
    for (int rr = Square.rank(sq) + (white ? 1 : -1); rr >= 0 && rr < 8; rr += white ? 1 : -1) {
      if (board[Square.of(f, rr)] == p) return true;
    }
    return false;
  }

  /** A friendly pawn stands beside it or defends it. */
  public boolean isConnectedPawn(int sq) {
    int p = board[sq];
    boolean white = p > 0;
    int f = Square.file(sq);
    int r = Square.rank(sq);
    int behind = white ? r - 1 : r + 1;
    for (int ff = f - 1; ff <= f + 1; ff += 2) {
      if (ff < 0 || ff > 7) continue;
      if (board[Square.of(ff, r)] == p) return true;
      if (behind >= 0 && behind < 8 && board[Square.of(ff, behind)] == p) return true;
    }
    return false;
  }

  /**
   * Behind all friendly pawns on the neighbouring files (so none can ever support it)
   * and its next square is guarded by an enemy pawn.
   */
  public boolean isBackwardPawn(int sq) {
    int p = board[sq];
    boolean white = p > 0;
    int f = Square.file(sq);
    int r = Square.rank(sq);
    if (isIsolatedPawn(sq)) return false;
    for (int ff = f - 1; ff <= f + 1; ff += 2) {
      if (ff < 0 || ff > 7) continue;
      for (int rr = 0; rr < 8; rr++) {
        if (board[Square.of(ff, rr)] == p && (white ? rr <= r : rr >= r)) return false;
      }
    }
    int stop = sq + (white ? 8 : -8);
    return stop >= 0 && stop < 64 && pawnAttackCount[idx(!white)][stop] > 0;
  }

  /** Defended by a friendly pawn. */
  public boolean isPawnProtected(int sq) {
    int p = board[sq];
    return p != Piece.EMPTY && pawnAttackCount[idx(p > 0)][sq] > 0;
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
    int f = Square.file(sq);
    int promo = Square.of(f, white ? 7 : 0);
    for (int s = sq + (white ? 8 : -8); s >= 0 && s < 64; s += white ? 8 : -8) {
      if (board[s] != Piece.EMPTY) return false;
    }
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
    for (int sq = 0; sq < 64; sq++) {
      if (Piece.type(board[sq]) != Piece.PAWN) continue;
      int n = movesToPromoteUnstoppable(sq);
      if (n < 0) continue;
      if (board[sq] > 0) fw = Math.min(fw, n);
      else fb = Math.min(fb, n);
    }
    if (fw == Integer.MAX_VALUE && fb == Integer.MAX_VALUE) return 0;
    boolean whiteMoves = pos.whiteToMove();
    // Plies until each side's queen appears.
    long wPly = fw == Integer.MAX_VALUE ? Long.MAX_VALUE : 2L * fw - (whiteMoves ? 1 : 0);
    long bPly = fb == Integer.MAX_VALUE ? Long.MAX_VALUE : 2L * fb - (whiteMoves ? 0 : 1);
    if (wPly < bPly) return 1;
    if (bPly < wPly) return -1;
    return 0;
  }

  // ================================================================ outposts and placement

  /**
   * An outpost: on the 4th-6th rank (from the owner's side), defended by a friendly pawn,
   * and no enemy pawn on a neighbouring file can ever advance to attack it.
   */
  public boolean isOutpost(int sq) {
    int p = board[sq];
    boolean white = p > 0;
    int rel = Square.relativeRank(sq, white);
    if (rel < 3 || rel > 5) return false;
    if (!isPawnProtected(sq)) return false;
    int f = Square.file(sq);
    int r = Square.rank(sq);
    int enemyPawn = Piece.make(Piece.PAWN, !white);
    for (int ff = f - 1; ff <= f + 1; ff += 2) {
      if (ff < 0 || ff > 7) continue;
      for (int rr = white ? r + 1 : r - 1; rr >= 0 && rr < 8; rr += white ? 1 : -1) {
        if (board[Square.of(ff, rr)] == enemyPawn) return false;
      }
    }
    return true;
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
    int bishop = Piece.make(Piece.BISHOP, side);
    boolean light = false;
    boolean dark = false;
    for (int sq = 0; sq < 64; sq++) {
      if (board[sq] == bishop) {
        if (Square.isLight(sq)) light = true;
        else dark = true;
      }
    }
    return light && dark;
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
    int rook = Piece.make(Piece.ROOK, side);
    for (int sq = 0; sq < 64; sq++) {
      if (board[sq] != rook) continue;
      for (int d = 0; d < 4; d++) {
        for (int s : Attacks.RAYS[sq][d]) {
          if (board[s] == Piece.EMPTY) continue;
          if (board[s] == rook) return true;
          break;
        }
      }
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

  public int undevelopedMinors(boolean side) {
    int n = 0;
    for (int sq = 0; sq < 64; sq++) {
      if (Piece.isColor(board[sq], side) && isUndevelopedMinor(sq)) n++;
    }
    return n;
  }

  /** The queen has left d1/d8. */
  public boolean queenHasLeftHome(boolean side) {
    int queen = Piece.make(Piece.QUEEN, side);
    int home = Square.of(3, side ? 0 : 7);
    if (board[home] == queen) return false;
    for (int sq = 0; sq < 64; sq++) if (board[sq] == queen) return true;
    return false;
  }

  // ================================================================ version 2 detectors

  /** Enemy knights, bishops, rooks and queens attacked by our pawns. */
  public int threatsByPawn(boolean side) {
    int n = 0;
    for (int sq = 0; sq < 64; sq++) {
      int p = board[sq];
      if (Piece.isColor(p, !side) && Piece.type(p) != Piece.PAWN && Piece.type(p) != Piece.KING
          && pawnAttackCount[idx(side)][sq] > 0) n++;
    }
    return n;
  }

  /** Enemy rooks and queens attacked by our knights or bishops. */
  public int threatsByMinor(boolean side) {
    int n = 0;
    int s = idx(side);
    for (int sq = 0; sq < 64; sq++) {
      int p = board[sq];
      int t = Piece.type(p);
      if (Piece.isColor(p, !side) && (t == Piece.ROOK || t == Piece.QUEEN)
          && knightAttack[s][sq] + bishopAttack[s][sq] > 0) n++;
    }
    return n;
  }

  /** Enemy queens attacked by our rooks. */
  public int threatsByRook(boolean side) {
    int n = 0;
    for (int sq = 0; sq < 64; sq++) {
      if (board[sq] == Piece.make(Piece.QUEEN, !side) && rookAttack[idx(side)][sq] > 0) n++;
    }
    return n;
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
    int n = 0;
    for (int t : Attacks.KNIGHT[k]) {
      if (!isOwn(board[t], e) && knightAttack[e][t] > 0 && attackCount[s][t] == 0) n++;
    }
    for (int d = 0; d < 8; d++) {
      for (int t : Attacks.RAYS[k][d]) {
        if (isOwn(board[t], e)) break;
        int sliders = d < 4 ? rookAttack[e][t] : bishopAttack[e][t];
        sliders += queenAttack[e][t];
        if (sliders > 0 && attackCount[s][t] == 0) n++;
        if (board[t] != Piece.EMPTY) break;
      }
    }
    return n;
  }

  /** Squares on the c-f files, our 2nd-4th ranks, behind one of our pawns and not hit by enemy pawns. */
  public int space(boolean side) {
    int pawn = Piece.make(Piece.PAWN, side);
    int n = 0;
    for (int f = 2; f <= 5; f++) {
      for (int rel = 1; rel <= 3; rel++) {
        int sq = Square.of(f, side ? rel : 7 - rel);
        if (board[sq] == pawn || pawnAttackCount[idx(!side)][sq] > 0) continue;
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
    boolean light = Square.isLight(sq);
    int pawn = Piece.make(Piece.PAWN, white);
    int n = 0;
    for (int s = 0; s < 64; s++) {
      if (board[s] != pawn || Square.isLight(s) != light) continue;
      n++;
      int front = s + (white ? 8 : -8);
      if (front >= 0 && front < 64 && board[front] != Piece.EMPTY) n++;
    }
    return n;
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
    int rook = Piece.make(Piece.ROOK, white);
    for (int s = sq + (white ? -8 : 8); s >= 0 && s < 64; s += white ? -8 : 8) {
      if (board[s] == Piece.EMPTY) continue;
      return board[s] == rook;
    }
    return false;
  }

  /** Every square ahead of the pawn is empty and the next one isn't attacked by the enemy. */
  public boolean hasFreePath(int sq) {
    boolean white = board[sq] > 0;
    int next = sq + (white ? 8 : -8);
    if (next < 0 || next >= 64 || attackCount[idx(!white)][next] > 0) return false;
    for (int s = next; s >= 0 && s < 64; s += white ? 8 : -8) if (board[s] != Piece.EMPTY) return false;
    return true;
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
    int n = 0;
    for (int f = 0; f < 8; f++) n += pawnsOnFile[idx(side)][f];
    return n;
  }

  // ================================================================ helpers

  private static int idx(boolean white) {
    return white ? W : B;
  }

  private static boolean isOwn(int piece, int side) {
    return side == W ? piece > 0 : piece < 0;
  }
}
