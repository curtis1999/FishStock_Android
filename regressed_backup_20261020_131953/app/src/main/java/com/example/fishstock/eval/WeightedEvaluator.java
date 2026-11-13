package com.example.fishstock.eval;

import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Square;

/**
 * The evaluation function: for every feature found by {@link BoardAnalysis}, add (or multiply by)
 * the matching {@link Term} weight. Reading this class top to bottom gives the full list of
 * what the engine cares about and how much.
 *
 * The same class serves every agent; only the {@link EvalWeights} differ.
 */
public final class WeightedEvaluator implements Evaluator {
  private static final int KNIGHT_MOBILITY_BASELINE = 4;
  private static final int BISHOP_MOBILITY_BASELINE = 6;
  private static final int ROOK_MOBILITY_BASELINE = 7;
  private static final int QUEEN_MOBILITY_BASELINE = 13;
  /** Mop-up and "drawish" rules switch on around a rook's worth of extra material. */
  private static final double WINNING_MATERIAL_EDGE = 4.0;

  private final EvalWeights w;

  public WeightedEvaluator(EvalWeights weights) {
    this.w = weights;
  }

  /** Evaluator with the hand-tuned default weights (the Simple agent's brain). */
  public static WeightedEvaluator withDefaults() {
    return new WeightedEvaluator(EvalWeights.defaults());
  }

  @Override
  public EvalWeights weights() {
    return w;
  }

  @Override
  public double evaluate(Position pos) {
    return compute(pos, new double[Term.values().length]);
  }

  @Override
  public EvalBreakdown explain(Position pos) {
    double[] c = new double[Term.values().length];
    double total = compute(pos, c);
    return new EvalBreakdown(c, total);
  }

  /** Fills c (per-term contributions, White positive) and returns the total. */
  private double compute(Position pos, double[] c) {
    BoardAnalysis a = new BoardAnalysis(pos, w);
    double mg = a.middlegameWeight();
    double eg = a.endgameWeight();

    double[] material = new double[2];      // non-king material, [0] White, [1] Black
    boolean[] hasPawns = new boolean[2];
    boolean[] hasQueen = new boolean[2];
    boolean[] onlyKnights = {true, true};
    double[] biggestThreat = new double[2];
    double[] secondThreat = new double[2];

    for (int sq = 0; sq < 64; sq++) {
      int p = pos.pieceAt(sq);
      if (p == Piece.EMPTY) continue;
      int type = Piece.type(p);
      if (type == Piece.KING) continue;
      boolean white = p > 0;
      int side = white ? 0 : 1;
      double value = a.pieceValue(p);

      // ---- material
      material[side] += value;
      add(c, materialTerm(type), white, value);
      if (type == Piece.PAWN) hasPawns[side] = true;
      if (type == Piece.QUEEN) hasQueen[side] = true;
      if (type != Piece.KNIGHT) onlyKnights[side] = false;

      // ---- 3. pins and reveal checkers
      if (a.isPinnedToKing(sq)) {
        add(c, Term.PINNED_TO_KING_FACTOR, white, value * (w.get(Term.PINNED_TO_KING_FACTOR) - 1.0));
      } else if (a.isPinnedToQueen(sq)) {
        add(c, Term.PINNED_TO_QUEEN_FACTOR, white, value * (w.get(Term.PINNED_TO_QUEEN_FACTOR) - 1.0));
      }
      if (a.isRevealChecker(sq)) add(c, Term.REVEAL_CHECKER_BONUS, white, w.get(Term.REVEAL_CHECKER_BONUS));
      if (a.isRevealQueenAttacker(sq)) {
        add(c, Term.REVEAL_QUEEN_ATTACKER_BONUS, white, w.get(Term.REVEAL_QUEEN_ATTACKER_BONUS));
      }

      // ---- 1. attackers / defenders: remember what each side stands to lose
      if (a.attackers(sq, !white) > 0) {
        double loss = a.exchangeLoss(sq);
        if (loss > biggestThreat[side]) {
          secondThreat[side] = biggestThreat[side];
          biggestThreat[side] = loss;
        } else if (loss > secondThreat[side]) {
          secondThreat[side] = loss;
        }
      }
      if (type != Piece.PAWN) {
        if (a.isDefended(sq)) add(c, Term.DEFENDED_PIECE_BONUS, white, w.get(Term.DEFENDED_PIECE_BONUS));
        else add(c, Term.LOOSE_PIECE_PENALTY, white, -w.get(Term.LOOSE_PIECE_PENALTY));
      }

      // ---- 2. mobility and per-piece placement
      switch (type) {
        case Piece.KNIGHT:
          add(c, Term.KNIGHT_MOBILITY, white, w.get(Term.KNIGHT_MOBILITY) * (a.mobility(sq) - KNIGHT_MOBILITY_BASELINE));
          if (BoardAnalysis.isCentral(sq)) add(c, Term.KNIGHT_CENTRAL_BONUS, white, w.get(Term.KNIGHT_CENTRAL_BONUS));
          if (BoardAnalysis.isOnRim(sq)) add(c, Term.KNIGHT_RIM_PENALTY, white, -w.get(Term.KNIGHT_RIM_PENALTY));
          if (BoardAnalysis.isInCorner(sq)) add(c, Term.KNIGHT_CORNER_PENALTY, white, -w.get(Term.KNIGHT_CORNER_PENALTY));
          if (a.isOutpost(sq)) {
            add(c, Term.KNIGHT_OUTPOST_BONUS, white, w.get(Term.KNIGHT_OUTPOST_BONUS));
            if (a.isAdvancedOutpost(sq)) add(c, Term.ADVANCED_OUTPOST_BONUS, white, w.get(Term.ADVANCED_OUTPOST_BONUS));
          }
          if (a.isUndevelopedMinor(sq)) add(c, Term.UNDEVELOPED_MINOR_PENALTY, white, -w.get(Term.UNDEVELOPED_MINOR_PENALTY) * mg);
          break;
        case Piece.BISHOP:
          add(c, Term.BISHOP_MOBILITY, white, w.get(Term.BISHOP_MOBILITY) * (a.mobility(sq) - BISHOP_MOBILITY_BASELINE));
          if (BoardAnalysis.isOnLongDiagonal(sq)) {
            add(c, Term.BISHOP_LONG_DIAGONAL_BONUS, white, w.get(Term.BISHOP_LONG_DIAGONAL_BONUS));
          }
          if (a.isOutpost(sq)) {
            add(c, Term.BISHOP_OUTPOST_BONUS, white, w.get(Term.BISHOP_OUTPOST_BONUS));
            if (a.isAdvancedOutpost(sq)) add(c, Term.ADVANCED_OUTPOST_BONUS, white, w.get(Term.ADVANCED_OUTPOST_BONUS));
          }
          if (a.isUndevelopedMinor(sq)) add(c, Term.UNDEVELOPED_MINOR_PENALTY, white, -w.get(Term.UNDEVELOPED_MINOR_PENALTY) * mg);
          break;
        case Piece.ROOK:
          add(c, Term.ROOK_MOBILITY, white, w.get(Term.ROOK_MOBILITY) * (a.mobility(sq) - ROOK_MOBILITY_BASELINE));
          if (a.isOnOpenFile(sq)) add(c, Term.ROOK_OPEN_FILE_BONUS, white, w.get(Term.ROOK_OPEN_FILE_BONUS));
          else if (a.isOnSemiOpenFile(sq)) add(c, Term.ROOK_SEMI_OPEN_FILE_BONUS, white, w.get(Term.ROOK_SEMI_OPEN_FILE_BONUS));
          if (a.isOnSeventhRank(sq)) add(c, Term.ROOK_SEVENTH_RANK_BONUS, white, w.get(Term.ROOK_SEVENTH_RANK_BONUS));
          break;
        case Piece.QUEEN:
          add(c, Term.QUEEN_MOBILITY, white, w.get(Term.QUEEN_MOBILITY) * (a.mobility(sq) - QUEEN_MOBILITY_BASELINE));
          break;
        case Piece.PAWN:
          addPawnStructure(c, a, sq, white);
          break;
        default:
          break;
      }
    }

    // ---- per-side terms
    for (int side = 0; side < 2; side++) {
      boolean white = side == 0;
      if (a.hasBishopPair(white)) add(c, Term.BISHOP_PAIR_BONUS, white, w.get(Term.BISHOP_PAIR_BONUS));
      if (a.rooksConnected(white)) add(c, Term.CONNECTED_ROOKS_BONUS, white, w.get(Term.CONNECTED_ROOKS_BONUS));
      if (a.queenHasLeftHome(white) && a.undevelopedMinors(white) >= 2) {
        add(c, Term.EARLY_QUEEN_PENALTY, white, -w.get(Term.EARLY_QUEEN_PENALTY) * mg);
      }
      addKingSafety(c, a, white, mg);
      int kingSq = pos.kingSquare(white);
      add(c, Term.KING_CENTRALISATION_BONUS, white,
          w.get(Term.KING_CENTRALISATION_BONUS) * (3 - Square.centreDistance(kingSq)) * eg);
    }

    // ---- 1. hanging pieces: the side to move will cash in its best capture;
    //         it can only rescue one of its own attacked pieces.
    boolean moverWhite = pos.whiteToMove();
    int mover = moverWhite ? 0 : 1;
    int waiter = 1 - mover;
    add(c, Term.EN_PRISE_FACTOR, !moverWhite, -w.get(Term.EN_PRISE_FACTOR) * biggestThreat[waiter]);
    add(c, Term.SECOND_THREAT_FACTOR, moverWhite, -w.get(Term.SECOND_THREAT_FACTOR) * secondThreat[mover]);

    // ---- initiative
    add(c, Term.SIDE_TO_MOVE_BONUS, moverWhite, w.get(Term.SIDE_TO_MOVE_BONUS));
    if (pos.inCheck()) add(c, Term.IN_CHECK_PENALTY, moverWhite, -w.get(Term.IN_CHECK_PENALTY));

    // ---- endgame: drive a lone/defenceless king to the edge
    int strong = material[0] >= material[1] ? 0 : 1;
    int weak = 1 - strong;
    double edge = material[strong] - material[weak];
    if (edge >= WINNING_MATERIAL_EDGE && !hasPawns[weak]) {
      boolean strongWhite = strong == 0;
      int weakKing = pos.kingSquare(!strongWhite);
      int strongKing = pos.kingSquare(strongWhite);
      add(c, Term.MOP_UP_EDGE_BONUS, strongWhite, w.get(Term.MOP_UP_EDGE_BONUS) * Square.centreDistance(weakKing));
      add(c, Term.MOP_UP_KING_PROXIMITY_BONUS, strongWhite,
          w.get(Term.MOP_UP_KING_PROXIMITY_BONUS) * (7 - Square.distance(weakKing, strongKing)));
    }

    double total = 0;
    for (double x : c) total += x;

    // ---- endgame: not enough to win without pawns (KR v KB, KNN v K, ...) - scale towards a draw
    boolean drawish = !hasPawns[strong]
        && ((!hasQueen[strong] && edge < WINNING_MATERIAL_EDGE)
            || (onlyKnights[strong] && material[weak] == 0 && edge <= 2 * w.get(Term.KNIGHT_VALUE) + 0.01));
    if (drawish) {
      double scaled = total * w.get(Term.DRAWISH_SCALE);
      c[Term.DRAWISH_SCALE.ordinal()] += scaled - total;
      total = scaled;
    }
    return total;
  }

  /** 5. Pawn structure terms for the pawn on sq. */
  private void addPawnStructure(double[] c, BoardAnalysis a, int sq, boolean white) {
    if (a.isDoubledPawn(sq)) add(c, Term.DOUBLED_PAWN_PENALTY, white, -w.get(Term.DOUBLED_PAWN_PENALTY));
    if (a.isIsolatedPawn(sq)) add(c, Term.ISOLATED_PAWN_PENALTY, white, -w.get(Term.ISOLATED_PAWN_PENALTY));
    else if (a.isBackwardPawn(sq)) add(c, Term.BACKWARD_PAWN_PENALTY, white, -w.get(Term.BACKWARD_PAWN_PENALTY));
    if (a.isConnectedPawn(sq)) add(c, Term.CONNECTED_PAWN_BONUS, white, w.get(Term.CONNECTED_PAWN_BONUS));
    if (BoardAnalysis.isCentral(sq)) add(c, Term.CENTRAL_PAWN_BONUS, white, w.get(Term.CENTRAL_PAWN_BONUS));
    if (a.isPassedPawn(sq)) {
      int advanced = Square.relativeRank(sq, white) - 1; // 0 on its starting rank, 5 on the 7th
      add(c, Term.PASSED_PAWN_BONUS, white, w.get(Term.PASSED_PAWN_BONUS));
      add(c, Term.PASSED_PAWN_ADVANCE_BONUS, white, w.get(Term.PASSED_PAWN_ADVANCE_BONUS) * advanced * advanced / 25.0);
      if (a.isPawnProtected(sq)) add(c, Term.PROTECTED_PASSED_PAWN_BONUS, white, w.get(Term.PROTECTED_PASSED_PAWN_BONUS));
      if (a.isOneStepFromPromotion(sq)) {
        add(c, Term.ONE_STEP_FROM_PROMOTION_BONUS, white, w.get(Term.ONE_STEP_FROM_PROMOTION_BONUS));
      }
      if (a.isUnstoppablePawn(sq)) add(c, Term.UNSTOPPABLE_PAWN_BONUS, white, w.get(Term.UNSTOPPABLE_PAWN_BONUS));
    }
  }

  /** 4. King safety terms. These matter while pieces are on the board, so they fade with mg. */
  private void addKingSafety(double[] c, BoardAnalysis a, boolean white, double mg) {
    if (mg <= 0) return;
    add(c, Term.PAWN_SHIELD_BONUS, white, w.get(Term.PAWN_SHIELD_BONUS) * a.pawnShield(white) * mg);
    add(c, Term.OPEN_FILE_NEAR_KING_PENALTY, white, -w.get(Term.OPEN_FILE_NEAR_KING_PENALTY) * a.openFilesNearKing(white) * mg);
    add(c, Term.KING_ZONE_ATTACK_PENALTY, white, -w.get(Term.KING_ZONE_ATTACK_PENALTY) * a.kingZoneAttacks(white) * mg);
    if (a.kingEscapeSquares(white) < 2) {
      add(c, Term.KING_FEW_ESCAPE_SQUARES_PENALTY, white, -w.get(Term.KING_FEW_ESCAPE_SQUARES_PENALTY) * mg);
    }
    add(c, Term.XRAY_ON_KING_PENALTY, white, -w.get(Term.XRAY_ON_KING_PENALTY) * a.xRaysOnKing(white) * mg);
    if (a.isCastled(white)) add(c, Term.CASTLED_KING_BONUS, white, w.get(Term.CASTLED_KING_BONUS) * mg);
    if (a.hasLostCastling(white)) add(c, Term.LOST_CASTLING_PENALTY, white, -w.get(Term.LOST_CASTLING_PENALTY) * mg);
  }

  private static Term materialTerm(int type) {
    switch (type) {
      case Piece.PAWN: return Term.PAWN_VALUE;
      case Piece.KNIGHT: return Term.KNIGHT_VALUE;
      case Piece.BISHOP: return Term.BISHOP_VALUE;
      case Piece.ROOK: return Term.ROOK_VALUE;
      default: return Term.QUEEN_VALUE;
    }
  }

  private static void add(double[] c, Term t, boolean white, double amount) {
    c[t.ordinal()] += white ? amount : -amount;
  }
}
