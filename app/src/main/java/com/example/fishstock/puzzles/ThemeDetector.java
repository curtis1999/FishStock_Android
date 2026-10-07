package com.example.fishstock.puzzles;

import com.example.fishstock.engine.Attacks;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Rules;
import com.example.fishstock.engine.Square;
import com.example.fishstock.eval.BoardAnalysis;
import com.example.fishstock.eval.EvalWeights;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Names the ideas behind a puzzle. Every theme has a plain, explicit definition below, applied to
 * the solution line (solver move, reply, solver move, ...) and the engine's continuation after it.
 *
 * Definitions ("solver" = the side to move in the puzzle, values 1/3/3/5/9, king = 100):
 *  - FORK: a solver move lands a piece that attacks two or more enemy pieces that matter (the
 *    king, a piece worth more than the forker, or an undefended piece worth 3+), and the forker
 *    can't simply be taken by something cheaper.
 *  - DOUBLE_CHECK: after a solver move two pieces give check.
 *  - DISCOVERED_CHECK: a reveal checker ({@link BoardAnalysis#isRevealChecker}) moves and check
 *    comes from a piece other than the one that moved.
 *  - DISCOVERED_ATTACK: a solver move uncovers an attack by another solver piece on an enemy
 *    queen/rook worth more than the attacker, or on an undefended piece worth 3+.
 *  - PIN: the moved piece pins an enemy knight/bishop/rook/queen to its king, or to a more
 *    valuable piece behind it.
 *  - SKEWER: the moved piece attacks the king (or a piece worth more than itself) with another
 *    enemy piece worth 3+ behind it on the same line.
 *  - REMOVE_THE_DEFENDER: the solver captures piece X, and later captures (or mates) on a square
 *    X was guarding.
 *  - DEFLECTION: after a solver check or capture, an enemy piece moves away, and the solver then
 *    captures (or mates) on a square that piece was guarding and no longer guards.
 *  - ATTRACTION: the enemy king captures a solver piece and the solver's next move is check.
 *  - SACRIFICE: at some point the solver is 2+ points down compared with the start, or a solver
 *    move leaves the moved piece losing 2+ pawns to an exchange (beyond what it captured).
 *  - PROMOTION / UNDERPROMOTION: a solver pawn promotes (to a queen / to anything else).
 *  - PAWN_RACE: kings and pawns only, the opponent has a passed pawn, and the solver queens
 *    first (promotes in the line, or wins the race by {@link BoardAnalysis#pawnRaceWinner}).
 *  - PAWN_BREAKTHROUGH: an endgame with at most a minor piece or rook left in total; the first
 *    move is a pawn move; a solver pawn is offered to an enemy pawn; and the line produces a new
 *    passed pawn or a promotion.
 *  - QUIET_MOVE: the first move is not a check, capture or promotion (and not mate in one).
 *  - MATE, BACK_RANK_MATE, SMOTHERED_MATE: the line ends in mate; on the back rank by a rook or
 *    queen with the king walled in by its own pieces; or by a knight against a king surrounded
 *    by its own pieces.
 *  - ENDGAME: game phase 6 or less (e.g. rook + minor each, or less).
 */
public final class ThemeDetector {
  private static final int KING_VALUE = 100;
  /** Printed in this order: most specific first. */
  private static final Theme[] ORDER = {
      Theme.SMOTHERED_MATE, Theme.BACK_RANK_MATE, Theme.DOUBLE_CHECK, Theme.DISCOVERED_CHECK,
      Theme.FORK, Theme.SKEWER, Theme.PIN, Theme.REMOVE_THE_DEFENDER, Theme.DEFLECTION,
      Theme.ATTRACTION, Theme.DISCOVERED_ATTACK, Theme.PAWN_BREAKTHROUGH, Theme.PAWN_RACE, Theme.SACRIFICE,
      Theme.UNDERPROMOTION, Theme.PROMOTION, Theme.QUIET_MOVE, Theme.MATE, Theme.ENDGAME};

  private static final EvalWeights WEIGHTS = EvalWeights.defaults();

  private ThemeDetector() {}

  /**
   * @param start        the puzzle position
   * @param line         solver move, reply, solver move, ... (must be legal from start)
   * @param continuation moves the engine expects after the line (may be empty)
   */
  public static List<Theme> detect(Position start, List<Move> line, List<Move> continuation) {
    List<Move> all = new ArrayList<>(line);
    if (continuation != null) all.addAll(continuation);
    // before[i] = position before all.get(i); before[all.size()] = final position
    List<Position> before = new ArrayList<>();
    Position p = start.copy();
    for (Move m : all) {
      before.add(p.copy());
      p.makeMove(m);
    }
    before.add(p.copy());
    boolean solver = start.whiteToMove();
    int lineLen = line.size();
    Set<Theme> t = EnumSet.noneOf(Theme.class);

    for (int i = 0; i < lineLen; i += 2) {
      Position b = before.get(i);
      Position a = before.get(i + 1);
      Move m = all.get(i);
      if (a.inCheck() && a.checkerCount() >= 2) t.add(Theme.DOUBLE_CHECK);
      if (isDiscoveredCheck(b, m, a)) t.add(Theme.DISCOVERED_CHECK);
      else if (isDiscoveredAttack(b, m, a)) t.add(Theme.DISCOVERED_ATTACK);
      if (isFork(m, a)) t.add(Theme.FORK);
      int lineTheme = pinOrSkewer(m, a);
      if (lineTheme == 1) t.add(Theme.PIN);
      if (lineTheme == 2) t.add(Theme.SKEWER);
      if (offersMaterial(m, a)) t.add(Theme.SACRIFICE);
      if (m.isPromotion() && m.promotion != Piece.QUEEN) t.add(Theme.UNDERPROMOTION);
    }
    for (int i = 0; i < all.size(); i += 2) if (all.get(i).isPromotion()) t.add(Theme.PROMOTION);

    if (removesDefender(all, before, lineLen)) t.add(Theme.REMOVE_THE_DEFENDER);
    if (deflects(all, before, lineLen)) t.add(Theme.DEFLECTION);
    if (attracts(all, before, lineLen)) t.add(Theme.ATTRACTION);
    if (losesMaterialOnTheWay(before, solver, lineLen)) t.add(Theme.SACRIFICE);

    Move first = line.get(0);
    Position afterFirst = before.get(1);
    boolean mateInOne = Rules.isCheckmate(afterFirst);
    if (!mateInOne && !first.isCapture() && !first.isPromotion() && !afterFirst.inCheck()) t.add(Theme.QUIET_MOVE);

    Position last = before.get(before.size() - 1);
    if (Rules.isCheckmate(last) && last.whiteToMove() != solver) {
      t.add(Theme.MATE);
      Move mating = all.get(all.size() - 1);
      if (isSmotheredMate(last, mating)) t.add(Theme.SMOTHERED_MATE);
      else if (isBackRankMate(last, mating)) t.add(Theme.BACK_RANK_MATE);
    }

    BoardAnalysis startAnalysis = new BoardAnalysis(start, WEIGHTS);
    if (startAnalysis.phase() <= 6) t.add(Theme.ENDGAME);
    if (isPawnBreakthrough(start, startAnalysis, all, before, lineLen)) t.add(Theme.PAWN_BREAKTHROUGH);
    if (isPawnRace(start, startAnalysis, all, before, lineLen)) t.add(Theme.PAWN_RACE);

    List<Theme> out = new ArrayList<>();
    for (Theme th : ORDER) if (t.contains(th)) out.add(th);
    return out;
  }

  // ================================================================ single-move patterns

  /** See the class comment: FORK. {@code a} is the position after the move. */
  public static boolean isFork(Move m, Position a) {
    int sq = m.to;
    int forker = a.pieceAt(sq);
    boolean white = forker > 0;
    int fv = value(forker);
    if (cheapestAttacker(a, sq, !white) < fv) return false;
    int targets = 0;
    for (int t : attacksFrom(a, sq)) {
      int target = a.pieceAt(t);
      if (!Piece.isColor(target, !white)) continue;
      int tv = value(target);
      boolean defended = a.isAttacked(t, !white);
      if (Piece.type(target) == Piece.KING || tv > fv || (!defended && tv >= 3)) targets++;
    }
    return targets >= 2;
  }

  /** The moved piece is not the (only) checker, and it was a reveal checker before moving. */
  static boolean isDiscoveredCheck(Position b, Move m, Position a) {
    if (!a.inCheck() || m.isCastle()) return false;
    if (!new BoardAnalysis(b, WEIGHTS).isRevealChecker(m.from)) return false;
    boolean movedPieceChecks = contains(attacksFrom(a, m.to), a.kingSquare(a.whiteToMove()));
    return !movedPieceChecks || a.checkerCount() >= 2;
  }

  /** Another solver slider newly attacks a big or loose enemy piece because m.from was vacated. */
  static boolean isDiscoveredAttack(Position b, Move m, Position a) {
    boolean white = m.isWhite();
    for (int s = 0; s < 64; s++) {
      int slider = a.pieceAt(s);
      if (s == m.to || !Piece.isColor(slider, white)) continue;
      int type = Piece.type(slider);
      if (type != Piece.BISHOP && type != Piece.ROOK && type != Piece.QUEEN) continue;
      int dir = Attacks.direction(s, m.from);
      if (dir < 0 || !Attacks.slidesAlong(type, dir)) continue;
      List<Integer> beforeTargets = attacksFrom(b, s);
      for (int t : attacksFrom(a, s)) {
        int target = a.pieceAt(t);
        if (!Piece.isColor(target, !white) || Piece.type(target) == Piece.KING) continue;
        if (contains(beforeTargets, t) || Attacks.direction(s, t) != dir) continue;
        int tv = value(target);
        boolean defended = a.isAttacked(t, !white);
        if ((tv >= 5 && tv > value(slider)) || (!defended && tv >= 3)) return true;
      }
    }
    return false;
  }

  /** 1 = the moved piece creates a pin, 2 = a skewer, 0 = neither. */
  static int pinOrSkewer(Move m, Position a) {
    int sq = m.to;
    int mover = a.pieceAt(sq);
    int type = Piece.type(mover);
    boolean white = mover > 0;
    if (type != Piece.BISHOP && type != Piece.ROOK && type != Piece.QUEEN) return 0;
    if (cheapestAttacker(a, sq, !white) < value(mover)) return 0;
    if (a.isAttacked(sq, !white) && !a.isAttacked(sq, white)) return 0; // just hangs there
    int found = 0;
    for (int d = 0; d < 8; d++) {
      if (!Attacks.slidesAlong(type, d)) continue;
      int first = -1;
      int second = -1;
      for (int s : Attacks.RAYS[sq][d]) {
        if (a.isEmpty(s)) continue;
        if (first < 0) first = s;
        else {
          second = s;
          break;
        }
      }
      if (first < 0 || second < 0) continue;
      int p1 = a.pieceAt(first);
      int p2 = a.pieceAt(second);
      if (!Piece.isColor(p1, !white) || !Piece.isColor(p2, !white)) continue;
      int v1 = value(p1);
      int v2 = value(p2);
      boolean p2Loose = !a.isAttacked(second, !white);
      if (Piece.type(p1) == Piece.KING || (v1 > v2 && v1 > value(mover))) {
        if (v2 >= 3 && (p2Loose || v2 > value(mover))) return 2;
      } else if (v1 >= 3 && (Piece.type(p2) == Piece.KING || (v2 > v1 && (v2 > value(mover) || p2Loose)))) {
        found = 1;
      }
    }
    return found;
  }

  /** The moved piece can be won by an exchange worth 2+ pawns more than what it captured. */
  public static boolean offersMaterial(Move m, Position a) {
    if (Rules.isCheckmate(a)) return false;
    BoardAnalysis an = new BoardAnalysis(a, WEIGHTS);
    double captured = m.isCapture() ? an.pieceValue(m.captured) : 0;
    return an.exchangeLoss(m.to) - captured >= 2.0;
  }

  // ================================================================ multi-move patterns

  private static boolean removesDefender(List<Move> all, List<Position> before, int lineLen) {
    for (int k = 0; k < lineLen; k += 2) {
      Move capture = all.get(k);
      if (!capture.isCapture() || capture.isEnPassant()) continue;
      Position b = before.get(k);
      // squares the captured piece was guarding, seen from where it stood
      List<Integer> guarded = attacksFrom(b, capture.to);
      for (int j = k + 2; j < all.size(); j += 2) {
        Move later = all.get(j);
        if (later.to == capture.to || !contains(guarded, later.to)) continue;
        if (isRealGain(later, before.get(j + 1))) return true;
      }
    }
    return false;
  }

  private static boolean deflects(List<Move> all, List<Position> before, int lineLen) {
    for (int r = 1; r < lineLen; r += 2) {
      Move forcing = all.get(r - 1);
      Position afterForcing = before.get(r);
      if (!forcing.isCapture() && !afterForcing.inCheck()) continue;
      Move reply = all.get(r);
      List<Integer> guardedBefore = attacksFrom(before.get(r), reply.from);
      List<Integer> guardedAfter = attacksFrom(before.get(r + 1), reply.to);
      for (int j = r + 1; j < all.size(); j += 2) {
        Move later = all.get(j);
        if (later.to == reply.to || !contains(guardedBefore, later.to) || contains(guardedAfter, later.to)) continue;
        if (isRealGain(later, before.get(j + 1))) return true;
      }
    }
    return false;
  }

  private static boolean attracts(List<Move> all, List<Position> before, int lineLen) {
    for (int r = 1; r + 1 < lineLen; r += 2) {
      Move reply = all.get(r);
      if (Piece.type(reply.piece) == Piece.KING && reply.isCapture() && before.get(r + 2).inCheck()) return true;
    }
    return false;
  }

  /** A capture of a knight or better, or checkmate. */
  private static boolean isRealGain(Move m, Position after) {
    return (m.isCapture() && Piece.displayValue(m.captured) >= 3) || Rules.isCheckmate(after);
  }

  private static boolean losesMaterialOnTheWay(List<Position> before, boolean solver, int lineLen) {
    int startBalance = balance(before.get(0), solver);
    for (int i = 2; i <= lineLen; i += 2) {
      // position after each reply
      if (balance(before.get(i), solver) <= startBalance - 2) return true;
    }
    return false;
  }

  private static boolean isPawnBreakthrough(Position start, BoardAnalysis sa, List<Move> all,
                                            List<Position> before, int lineLen) {
    if (sa.phase() > 2) return false;
    Move first = all.get(0);
    if (Piece.type(first.piece) != Piece.PAWN) return false;
    boolean solver = start.whiteToMove();
    boolean pawnOffered = false;
    for (int i = 0; i < lineLen; i += 2) {
      Move m = all.get(i);
      if (Piece.type(m.piece) != Piece.PAWN) continue;
      for (int from : Attacks.PAWN_ATTACKS[solver ? 0 : 1][m.to]) {
        // an enemy pawn standing where it attacks m.to
        if (before.get(i + 1).pieceAt(from) == Piece.make(Piece.PAWN, !solver)) pawnOffered = true;
      }
    }
    if (!pawnOffered) return false;
    for (int i = 0; i < all.size(); i += 2) if (all.get(i).isPromotion()) return true;
    return passedPawns(before.get(before.size() - 1), solver) > passedPawns(start, solver);
  }

  private static boolean isPawnRace(Position start, BoardAnalysis sa, List<Move> all, List<Position> before, int lineLen) {
    if (sa.phase() != 0) return false; // kings and pawns only
    boolean solver = start.whiteToMove();
    if (passedPawns(start, !solver) == 0) return false; // the opponent must have a runner too
    for (int i = 0; i < all.size(); i += 2) if (all.get(i).isPromotion()) return true;
    int winner = new BoardAnalysis(before.get(lineLen), WEIGHTS).pawnRaceWinner();
    return winner == (solver ? 1 : -1);
  }

  // ================================================================ mate patterns

  private static boolean isSmotheredMate(Position mated, Move mating) {
    if (Piece.type(mating.piece) != Piece.KNIGHT) return false;
    boolean white = mated.whiteToMove();
    int k = mated.kingSquare(white);
    for (int s : Attacks.KING[k]) if (!Piece.isColor(mated.pieceAt(s), white)) return false;
    return true;
  }

  private static boolean isBackRankMate(Position mated, Move mating) {
    int type = mating.promotion != 0 ? mating.promotion : Piece.type(mating.piece);
    if (type != Piece.ROOK && type != Piece.QUEEN) return false;
    boolean white = mated.whiteToMove();
    int k = mated.kingSquare(white);
    if (Square.relativeRank(k, white) != 0 || Square.rank(mating.to) != Square.rank(k)) return false;
    int forward = Square.rank(k) + (white ? 1 : -1);
    boolean ownPawnBlocks = false;
    for (int f = Square.file(k) - 1; f <= Square.file(k) + 1; f++) {
      if (f < 0 || f > 7) continue;
      int s = Square.of(f, forward);
      int p = mated.pieceAt(s);
      if (Piece.isColor(p, white)) {
        if (Piece.type(p) == Piece.PAWN) ownPawnBlocks = true;
      } else if (!mated.isAttacked(s, !white)) {
        return false;
      }
    }
    return ownPawnBlocks;
  }

  // ================================================================ helpers

  /** Squares attacked by the piece on sq (sliders stop at the first piece, which is included). */
  public static List<Integer> attacksFrom(Position p, int sq) {
    List<Integer> out = new ArrayList<>();
    int piece = p.pieceAt(sq);
    if (piece == Piece.EMPTY) return out;
    int type = Piece.type(piece);
    switch (type) {
      case Piece.PAWN:
        for (int t : Attacks.PAWN_ATTACKS[piece > 0 ? 0 : 1][sq]) out.add(t);
        break;
      case Piece.KNIGHT:
        for (int t : Attacks.KNIGHT[sq]) out.add(t);
        break;
      case Piece.KING:
        for (int t : Attacks.KING[sq]) out.add(t);
        break;
      default:
        for (int d = 0; d < 8; d++) {
          if (!Attacks.slidesAlong(type, d)) continue;
          for (int t : Attacks.RAYS[sq][d]) {
            out.add(t);
            if (!p.isEmpty(t)) break;
          }
        }
    }
    return out;
  }

  /** Value of the cheapest piece of side {@code bySide} attacking sq (Integer.MAX_VALUE if none). */
  static int cheapestAttacker(Position p, int sq, boolean bySide) {
    int best = Integer.MAX_VALUE;
    for (int s = 0; s < 64; s++) {
      int piece = p.pieceAt(s);
      if (!Piece.isColor(piece, bySide)) continue;
      if (value(piece) < best && contains(attacksFrom(p, s), sq)) best = value(piece);
    }
    return best;
  }

  private static int value(int piece) {
    return Piece.type(piece) == Piece.KING ? KING_VALUE : Piece.displayValue(piece);
  }

  /** Material (1/3/3/5/9) of side minus the other side. */
  private static int balance(Position p, boolean side) {
    int total = 0;
    for (int sq = 0; sq < 64; sq++) {
      int piece = p.pieceAt(sq);
      if (piece == Piece.EMPTY) continue;
      total += Piece.isColor(piece, side) ? Piece.displayValue(piece) : -Piece.displayValue(piece);
    }
    return total;
  }

  private static int passedPawns(Position p, boolean side) {
    BoardAnalysis a = new BoardAnalysis(p, WEIGHTS);
    int n = 0;
    for (int sq = 0; sq < 64; sq++) {
      if (p.pieceAt(sq) == Piece.make(Piece.PAWN, side) && a.isPassedPawn(sq)) n++;
    }
    return n;
  }

  private static boolean contains(List<Integer> list, int v) {
    for (int x : list) if (x == v) return true;
    return false;
  }
}
