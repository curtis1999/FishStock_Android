package com.example.fishstock.search;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A dedicated hunt for forced checkmates in which every attacking move is a check.
 *
 * Because the attacker only considers checks (usually 0-5 per position) and the defender's
 * replies to a check are few, this sees much deeper mating attacks than the normal search for the
 * same time: mate in 4 is 7 plies, which the full search rarely reaches on a phone.
 *
 * Tries mate in 1, then mate in 2, ... up to {@code maxMoves}, so the shortest mate is found first.
 * Agro spends part of every move's thinking time here before its normal search.
 */
public final class MateFinder {
  private final long deadline;
  private final long maxNodes;
  private final AtomicBoolean stop;
  private long nodes;
  private boolean aborted;

  private MateFinder(long deadline, long maxNodes, AtomicBoolean stop) {
    this.deadline = deadline;
    this.maxNodes = maxNodes;
    this.stop = stop;
  }

  /** A mating line found by {@link #find}. */
  public static final class Mate {
    public final Move firstMove;
    public final int movesToMate;

    Mate(Move firstMove, int movesToMate) {
      this.firstMove = firstMove;
      this.movesToMate = movesToMate;
    }
  }

  /**
   * @param timeMs   give up after this long (0 or less = no time limit, only the node limit)
   * @param maxNodes give up after visiting this many positions (keeps fixed-depth games repeatable)
   * @return the first move of the shortest checks-only forced mate, or null if none was found
   */
  public static Mate find(Position root, int maxMoves, long timeMs, long maxNodes, AtomicBoolean stop) {
    long deadline = timeMs <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + timeMs;
    MateFinder f = new MateFinder(deadline, maxNodes, stop);
    Position pos = root.copy();
    for (int n = 1; n <= maxMoves; n++) {
      Move m = f.attack(pos, n);
      if (f.aborted) return null;
      if (m != null) return new Mate(m, n);
    }
    return null;
  }

  /** A checking move after which the side to move mates in at most n moves, or null. */
  private Move attack(Position pos, int n) {
    for (Move m : MoveGenerator.legalMoves(pos)) {
      if (tick()) return null;
      pos.makeMove(m);
      boolean mates = false;
      if (pos.inCheck()) {
        if (!MoveGenerator.hasLegalMove(pos)) mates = true;
        else if (n > 1) mates = everyReplyLoses(pos, n - 1);
      }
      pos.unmakeMove(m);
      if (aborted) return null;
      if (mates) return m;
    }
    return null;
  }

  /** The side to move (in check, with legal moves) is mated in n whatever it plays. */
  private boolean everyReplyLoses(Position pos, int n) {
    List<Move> replies = MoveGenerator.legalMoves(pos);
    for (Move r : replies) {
      pos.makeMove(r);
      boolean lost = attack(pos, n) != null;
      pos.unmakeMove(r);
      if (aborted || !lost) return false;
    }
    return !replies.isEmpty();
  }

  private boolean tick() {
    if (aborted) return true;
    nodes++;
    if (nodes > maxNodes) aborted = true;
    if ((nodes & 255) == 0 && (System.currentTimeMillis() > deadline || (stop != null && stop.get()))) {
      aborted = true;
    }
    return aborted;
  }
}
