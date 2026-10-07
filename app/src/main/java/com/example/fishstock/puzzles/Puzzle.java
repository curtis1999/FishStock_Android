package com.example.fishstock.puzzles;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A puzzle: a position and a line where every one of the solver's moves is the only good one.
 *
 * {@link #line} alternates solver move, opponent reply, solver move, ... and always ends with a
 * solver move. The solver has to find {@link #movesToFind()} moves; the replies are played for
 * them. Scores are in pawns from the solver's point of view.
 */
public final class Puzzle {
  public final String fen;
  /** The first move to find (same as line.get(0)). */
  public final Move solution;
  /** Solver move, reply, solver move, ..., solver move. */
  public final List<Move> line;
  /** What the engine expects after the line (played automatically at the end; may be empty). */
  public final List<Move> continuation;
  /** Score after the solution. */
  public final double bestScore;
  /** Score after the best of the other first moves. */
  public final double secondScore;
  /** Mate in this many moves, or 0 if the solution wins material instead. */
  public final int mateIn;
  /** What the puzzle is about, most specific first (see {@link ThemeDetector}). */
  public final List<Theme> themes;
  /**
   * Other first moves that also count as solving it (training positions from your own games often
   * have more than one good answer). Empty for normal puzzles.
   */
  public final List<Move> alsoAccepted;
  /** Shown above the puzzle in training ("Key moment: 23. g3, you missed a fork"); may be empty. */
  public final String title;

  public Puzzle(String fen, List<Move> line, List<Move> continuation, double bestScore, double secondScore,
                int mateIn, List<Theme> themes) {
    this(fen, line, continuation, bestScore, secondScore, mateIn, themes, null, "");
  }

  public Puzzle(String fen, List<Move> line, List<Move> continuation, double bestScore, double secondScore,
                int mateIn, List<Theme> themes, List<Move> alsoAccepted, String title) {
    if (line == null || line.isEmpty() || line.size() % 2 == 0) {
      throw new IllegalArgumentException("A puzzle line must end with a solver move");
    }
    this.fen = fen;
    this.line = Collections.unmodifiableList(new ArrayList<>(line));
    this.solution = line.get(0);
    this.continuation = continuation == null ? Collections.<Move>emptyList()
        : Collections.unmodifiableList(new ArrayList<>(continuation));
    this.bestScore = bestScore;
    this.secondScore = secondScore;
    this.mateIn = mateIn;
    this.themes = themes == null ? Collections.<Theme>emptyList() : Collections.unmodifiableList(new ArrayList<>(themes));
    this.alsoAccepted = alsoAccepted == null ? Collections.<Move>emptyList()
        : Collections.unmodifiableList(new ArrayList<>(alsoAccepted));
    this.title = title == null ? "" : title;
  }

  /** Same puzzle with a title (for training sets). */
  public Puzzle withTitle(String newTitle) {
    return new Puzzle(fen, line, continuation, bestScore, secondScore, mateIn, themes, alsoAccepted, newTitle);
  }

  /** Is m an acceptable answer at solver move {@code step}? */
  public boolean accepts(int step, Move m) {
    if (solverMove(step).sameAs(m)) return true;
    if (step == 0) for (Move a : alsoAccepted) if (a.sameAs(m)) return true;
    return false;
  }

  public Position position() {
    return Position.fromFen(fen);
  }

  public boolean whiteToMove() {
    return position().whiteToMove();
  }

  /** How many moves the solver has to find. */
  public int movesToFind() {
    return (line.size() + 1) / 2;
  }

  /** The solver's move number {@code step} (0-based). */
  public Move solverMove(int step) {
    return line.get(step * 2);
  }

  /** The opponent's reply to solver move {@code step}, or null after the last one. */
  public Move replyTo(int step) {
    int i = step * 2 + 1;
    return i < line.size() ? line.get(i) : null;
  }

  public boolean hasTheme(Theme t) {
    return themes.contains(t);
  }

  /** "Find mate in 2", "Find the winning move" or "Find the winning sequence (3 moves)". */
  public String goal() {
    if (mateIn > 0) return mateIn == 1 ? "Find mate in 1" : String.format(Locale.US, "Find mate in %d", mateIn);
    if (bestScore < PuzzleGenerator.WIN) return "Find the best move"; // training positions from your games
    if (movesToFind() == 1) return "Find the winning move";
    return String.format(Locale.US, "Find the winning sequence (%d moves)", movesToFind());
  }

  /** "Fork, Discovered check" (empty if no theme was recognised). */
  public String themeLabels() {
    StringBuilder sb = new StringBuilder();
    for (Theme t : themes) {
      if (sb.length() > 0) sb.append(", ");
      sb.append(t.label);
    }
    return sb.toString();
  }

  /** The first move in normal notation, e.g. "Nxf7+". */
  public String solutionSan() {
    return Notation.toSan(position(), solution);
  }

  /** The whole line in normal notation, e.g. "28... Rxe1+ 29. Kh2 Qe5+". */
  public String lineSan() {
    return Notation.line(position(), line);
  }

  /** e.g. "+3.20, next best move +0.10" (or a mate). */
  public String scoreSummary() {
    String best = mateIn > 0 ? "mate in " + mateIn : String.format(Locale.US, "%+.2f", bestScore);
    return String.format(Locale.US, "%s, next best move %+.2f", best, secondScore);
  }

  @Override
  public String toString() {
    StringBuilder uci = new StringBuilder();
    for (Move m : line) uci.append(m.toUci()).append(' ');
    return fen + "  " + uci.toString().trim() + "  (" + scoreSummary() + ") [" + themeLabels() + "]";
  }
}
