package com.example.fishstock.analysis;

import com.example.fishstock.puzzles.PuzzleGenerator;
import com.example.fishstock.search.ScoredMove;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * What kind of position this is, judged from the spread of scores of ALL legal moves
 * (and, optionally, how many good moves there are at each step of the best line).
 *
 * Scores are from the side to move's point of view. Mates count as +/-{@link #MATE_AS_PAWNS}
 * for the statistics so one mate doesn't swamp the average.
 *
 * Labels (a position can have several):
 *  - PUZZLE: one move wins (+{@link PuzzleGenerator#WIN} or mate) and no other move does
 *    (all below +{@link PuzzleGenerator#NOT_WINNING}) - the same test the puzzle generator uses.
 *    If the side to move then has another "only winning move" after the best reply, it's a
 *    combination of 2 or 3 moves.
 *  - ONLY_MOVE: not a winning puzzle, but every other move is at least {@link #ONLY_MOVE_GAP}
 *    worse than the best (usually: find the one defence or you lose).
 *  - SHARP: at each step of the best line only one or two moves are good (within
 *    {@link #GOOD_MARGIN} of the best), so both sides must be precise.
 *  - CALM: many moves are about equally good and the spread (standard deviation) is small.
 *  - WINNING / LOSING: even after the best move the side to move is 3+ pawns up / down.
 *  - BALANCED: the best move leaves things within half a pawn.
 */
public final class PositionProfile {
  /** A move within this many pawns of the best one counts as "good". */
  public static final double GOOD_MARGIN = 0.5;
  /** Scores between -this and +this count as roughly equal. */
  public static final double EQUAL_BAND = 0.3;
  public static final double ONLY_MOVE_GAP = 1.5;
  public static final double MATE_AS_PAWNS = 20.0;
  private static final double DECISIVE = 3.0;
  private static final double SHARP_AVG_GOOD = 2.0;
  private static final double CALM_GOOD_SHARE = 0.35;
  private static final double CALM_STD_DEV = 1.0;

  public enum Label {
    PUZZLE("Puzzle", "One move wins and nothing else does."),
    ONLY_MOVE("Only move", "Every other move is clearly worse."),
    SHARP("Sharp", "Only one or two good moves at each step: precision needed."),
    CALM("Calm", "Many moves are about as good as each other."),
    WINNING("Winning", "Even the best reply can't save the other side much."),
    LOSING("Losing", "Even the best move leaves the side to move well behind."),
    BALANCED("Balanced", "The best move keeps things level.");

    public final String label;
    public final String description;

    Label(String label, String description) {
      this.label = label;
      this.description = description;
    }
  }

  /** One position along the best line: how many of its moves were good. */
  public static final class Step {
    public final String moveSan;
    public final boolean whiteMoved;
    public final int goodMoves;
    public final int legalMoves;
    /** The best move here was the only winning one (puzzle-style). */
    public final boolean onlyWinningMove;

    public Step(String moveSan, boolean whiteMoved, int goodMoves, int legalMoves, boolean onlyWinningMove) {
      this.moveSan = moveSan;
      this.whiteMoved = whiteMoved;
      this.goodMoves = goodMoves;
      this.legalMoves = legalMoves;
      this.onlyWinningMove = onlyWinningMove;
    }
  }

  public final int moveCount;
  public final double best;
  public final double second;
  public final double worst;
  public final double mean;
  public final double stdDev;
  public final int goodMoves;
  /** Moves after which the side to move is better (above +{@link #EQUAL_BAND}). */
  public final int positiveMoves;
  /** Moves after which it is roughly equal. */
  public final int equalMoves;
  /** Moves after which the side to move is worse. */
  public final int negativeMoves;
  /** How many moves in a row the side to move has an only winning move (1 = a one-move puzzle). */
  public final int combinationLength;
  public final List<Step> steps;
  public final List<Label> labels;

  private PositionProfile(List<ScoredMove> moves, List<Step> steps) {
    this.steps = steps == null ? Collections.<Step>emptyList() : steps;
    moveCount = moves.size();
    double[] s = new double[moveCount];
    for (int i = 0; i < moveCount; i++) s[i] = forStats(moves.get(i));
    best = moveCount > 0 ? s[0] : 0;
    second = moveCount > 1 ? s[1] : best;
    worst = moveCount > 0 ? s[moveCount - 1] : 0;
    double sum = 0;
    int good = 0;
    int pos = 0;
    int neg = 0;
    for (double x : s) {
      sum += x;
      if (x >= best - GOOD_MARGIN) good++;
      if (x > EQUAL_BAND) pos++;
      else if (x < -EQUAL_BAND) neg++;
    }
    mean = moveCount > 0 ? sum / moveCount : 0;
    double var = 0;
    for (double x : s) var += (x - mean) * (x - mean);
    stdDev = moveCount > 1 ? Math.sqrt(var / moveCount) : 0;
    goodMoves = good;
    positiveMoves = pos;
    negativeMoves = neg;
    equalMoves = moveCount - pos - neg;

    boolean puzzle = moveCount > 1 && isOnlyWin(moves);
    int combo = 0;
    if (puzzle) {
      combo = 1;
      // steps alternate: [0] side to move now, [1] opponent, [2] side to move again, ...
      for (int i = 2; i < this.steps.size() && this.steps.get(i).onlyWinningMove; i += 2) combo++;
    }
    combinationLength = combo;

    List<Label> l = new ArrayList<>();
    if (puzzle) l.add(Label.PUZZLE);
    else if (moveCount > 1 && best - second >= ONLY_MOVE_GAP) l.add(Label.ONLY_MOVE);
    if (isSharp()) l.add(Label.SHARP);
    if (moveCount > 1 && !puzzle && goodMoves >= CALM_GOOD_SHARE * moveCount && stdDev < CALM_STD_DEV) {
      l.add(Label.CALM);
    }
    if (best >= DECISIVE && !puzzle) l.add(Label.WINNING);
    if (best <= -DECISIVE) l.add(Label.LOSING);
    if (Math.abs(best) < GOOD_MARGIN && !l.contains(Label.SHARP) && !l.contains(Label.ONLY_MOVE)) {
      l.add(Label.BALANCED);
    }
    labels = Collections.unmodifiableList(l);
  }

  /**
   * @param moves every legal move with its score, best first (as {@code scoreAllMoves} returns)
   * @param steps good-move counts along the best line, starting with this position (may be null)
   */
  public static PositionProfile of(List<ScoredMove> moves, List<Step> steps) {
    return new PositionProfile(moves, steps);
  }

  /** The puzzle test on a list of scored moves (best first). */
  public static boolean isOnlyWin(List<ScoredMove> moves) {
    if (moves.size() < 2) return false;
    ScoredMove b = moves.get(0);
    ScoredMove s = moves.get(1);
    boolean bestWins = b.isMate() ? b.mateIn() > 0 : b.score >= PuzzleGenerator.WIN;
    boolean otherWins = s.isMate() ? s.mateIn() > 0 : s.score >= PuzzleGenerator.NOT_WINNING;
    return bestWins && !otherWins;
  }

  /** Number of moves within {@link #GOOD_MARGIN} of the best (best first list). */
  public static int countGood(List<ScoredMove> moves) {
    if (moves.isEmpty()) return 0;
    double best = forStats(moves.get(0));
    int n = 0;
    for (ScoredMove m : moves) if (forStats(m) >= best - GOOD_MARGIN) n++;
    return n;
  }

  public boolean has(Label l) {
    return labels.contains(l);
  }

  /** "Puzzle (2-move combination), Sharp" */
  public String labelText() {
    StringBuilder sb = new StringBuilder();
    for (Label l : labels) {
      if (sb.length() > 0) sb.append(", ");
      sb.append(l.label);
      if (l == Label.PUZZLE && combinationLength > 1) {
        sb.append(String.format(Locale.US, " (%d-move combination)", combinationLength));
      }
    }
    return sb.toString();
  }

  /** "Good moves at each step: Nf3 1/32, ...e5 2/29, ..." */
  public String stepsText() {
    if (steps.isEmpty()) return "";
    StringBuilder sb = new StringBuilder("Good moves at each step of the best line:\n");
    for (int i = 0; i < steps.size(); i++) {
      Step st = steps.get(i);
      if (i > 0) sb.append("   ");
      sb.append(st.moveSan).append(' ').append(st.goodMoves).append('/').append(st.legalMoves);
    }
    return sb.toString();
  }

  private boolean isSharp() {
    if (moveCount < 4) return false;
    if (!steps.isEmpty()) {
      double total = 0;
      int narrow = 0;
      int counted = 0;
      for (Step st : steps) {
        if (st.legalMoves < 2) continue;
        total += st.goodMoves;
        if (st.goodMoves <= 2) narrow++;
        counted++;
      }
      return counted >= 2 && total / counted <= SHARP_AVG_GOOD && narrow * 3 >= counted * 2;
    }
    // No line information: judge from this position alone.
    return goodMoves <= 2 && stdDev >= 1.5;
  }

  private static double forStats(ScoredMove m) {
    if (m.isMate()) return m.score > 0 ? MATE_AS_PAWNS : -MATE_AS_PAWNS;
    return Math.max(-MATE_AS_PAWNS, Math.min(MATE_AS_PAWNS, m.score));
  }
}
