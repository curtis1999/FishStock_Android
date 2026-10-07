package com.example.fishstock;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.agents.FishStock;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Rules;
import com.example.fishstock.puzzles.Puzzle;
import com.example.fishstock.puzzles.PuzzleBook;
import com.example.fishstock.puzzles.PuzzleGenerator;
import com.example.fishstock.puzzles.Theme;
import com.example.fishstock.ui.BoardRenderer;
import com.example.fishstock.ui.PromotionDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Puzzles: GENERATE PUZZLE searches sample positions until it finds one where exactly one move
 * wins, according to FishStock's evaluation (see {@link PuzzleGenerator}). Most puzzles are a
 * sequence: play your move, the opponent's reply is played for you, then find the next one.
 *
 * HINT first names the idea ("Look for a fork", from the puzzle's themes), then on a second press
 * shows which piece to move. SOLUTION plays the rest of the line. The themes are shown once the
 * puzzle is solved.
 *
 * "Select theme" chooses where puzzles come from: a newly generated one (any, or endgames only),
 * or the ready-made collection in {@link PuzzleBook} grouped by theme (Mate in 1-4, Fork, Pawn race...).
 * For a collection group the button becomes NEXT PUZZLE and steps through the group.
 */
public class PuzzleActivity extends AppCompatActivity {
  private static final long REPLY_DELAY_MS = 600;
  private static final long WRONG_MOVE_RESET_MS = 900;

  private final Handler main = new Handler(Looper.getMainLooper());
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final PuzzleGenerator generator = new PuzzleGenerator(FishStock.weights(), new Random());
  private AtomicBoolean currentStop = new AtomicBoolean(false);

  private BoardRenderer board;
  private TextView goalText;
  private TextView feedbackText;
  private TextView progressText;
  private Button generateButton;
  private Button hintButton;
  private Button solutionButton;
  private Spinner themeSpinner;
  /** Menu entries: the two "new puzzle" choices, then the collection's groups. */
  private final List<String> themeChoices = new ArrayList<>();
  private int bookIndex = -1;
  private static final String NEW_ANY = "New puzzle (any)";
  private static final String NEW_ENDGAME = "New endgame puzzle";

  private Puzzle puzzle;
  private Position shown;          // what is on the board now
  private int step;                // which of the solver's moves we are waiting for (0-based)
  private int hintLevel;           // 0 = none yet, 1 = idea shown, 2 = piece shown
  private int selectedSquare = -1;
  private boolean finished;        // solved or solution shown: the board no longer takes moves
  private boolean busy;            // a move is animating or about to be undone
  private boolean destroyed;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_puzzle);
    board = new BoardRenderer(this);
    goalText = findViewById(R.id.puzzleGoal);
    feedbackText = findViewById(R.id.puzzleFeedback);
    progressText = findViewById(R.id.puzzleProgress);
    generateButton = findViewById(R.id.generatePuzzle);
    hintButton = findViewById(R.id.puzzleHint);
    solutionButton = findViewById(R.id.puzzleSolution);
    themeSpinner = findViewById(R.id.themeSpinner);
    setUpThemes();

    board.setOnSquareClickListener(this::onSquareClicked);
    generateButton.setOnClickListener(v -> nextPuzzle());
    hintButton.setOnClickListener(v -> showHint());
    solutionButton.setOnClickListener(v -> showSolution());

    board.draw(Position.startingPosition());
    setPuzzleButtonsEnabled(false);
    generate();
  }

  @Override
  protected void onDestroy() {
    destroyed = true;
    currentStop.set(true);
    worker.shutdownNow();
    main.removeCallbacksAndMessages(null);
    board.cancelAnimation();
    super.onDestroy();
  }

  // ================================================================ choosing

  private void setUpThemes() {
    themeChoices.add(NEW_ANY);
    themeChoices.add(NEW_ENDGAME);
    List<String> labels = new ArrayList<>(themeChoices);
    for (Map.Entry<String, List<Puzzle>> e : PuzzleBook.groups().entrySet()) {
      themeChoices.add(e.getKey());
      labels.add(String.format(Locale.US, "%s (%d)", e.getKey(), e.getValue().size()));
    }
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    themeSpinner.setAdapter(adapter);
    themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      private boolean first = true;

      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (first) { // fired once when the menu is set up; onCreate already starts a puzzle
          first = false;
          return;
        }
        bookIndex = -1;
        nextPuzzle();
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  private String selectedTheme() {
    int i = themeSpinner.getSelectedItemPosition();
    return i >= 0 && i < themeChoices.size() ? themeChoices.get(i) : NEW_ANY;
  }

  /** GENERATE PUZZLE / NEXT PUZZLE: from the generator, or the next one in the chosen group. */
  private void nextPuzzle() {
    String theme = selectedTheme();
    if (theme.equals(NEW_ANY) || theme.equals(NEW_ENDGAME)) {
      generateButton.setText("Generate Puzzle");
      generate();
      return;
    }
    List<Puzzle> group = PuzzleBook.groups().get(theme);
    if (group == null || group.isEmpty()) return;
    currentStop.set(true);
    main.removeCallbacksAndMessages(null);
    board.cancelAnimation();
    bookIndex = (bookIndex + 1) % group.size();
    generateButton.setText(group.size() > 1 ? "Next Puzzle" : "Show Again");
    show(group.get(bookIndex));
    progressText.setText(String.format(Locale.US, "%s: puzzle %d of %d", theme, bookIndex + 1, group.size()));
  }

  // ================================================================ generating

  private void generate() {
    currentStop.set(true);
    final AtomicBoolean stop = new AtomicBoolean(false);
    currentStop = stop;
    main.removeCallbacksAndMessages(null);
    board.cancelAnimation();
    puzzle = null;
    finished = true;
    busy = false;
    selectedSquare = -1;
    generateButton.setEnabled(false);
    setPuzzleButtonsEnabled(false);
    final PuzzleGenerator.Mode mode = selectedTheme().equals(NEW_ENDGAME) ? PuzzleGenerator.Mode.ENDGAME : PuzzleGenerator.Mode.MIXED;
    goalText.setText(mode == PuzzleGenerator.Mode.ENDGAME ? "Looking for an endgame puzzle..." : "Looking for a puzzle...");
    feedbackText.setText("");
    progressText.setText("Searching");

    worker.execute(() -> {
      Puzzle found = generator.find(stop, count -> {
        if (count % 10 != 0) return;
        main.post(() -> {
          if (!stop.get()) progressText.setText(String.format(Locale.US, "Searching... %d positions checked", count));
        });
      }, mode);
      main.post(() -> {
        if (destroyed || stop.get() || found == null) return;
        show(found);
      });
    });
  }

  private void show(Puzzle p) {
    puzzle = p;
    shown = p.position();
    step = 0;
    hintLevel = 0;
    finished = false;
    busy = false;
    selectedSquare = -1;
    board.setFlipped(!shown.whiteToMove());   // the solver plays up the board
    board.draw(shown);
    updateGoal();
    feedbackText.setText("");
    progressText.setText("");
    generateButton.setEnabled(true);
    setPuzzleButtonsEnabled(true);
  }

  /** "WHITE TO MOVE: Find the winning sequence (3 moves)" plus "move 2 of 3" once under way. */
  private void updateGoal() {
    String side = puzzle.whiteToMove() ? "WHITE" : "BLACK";
    String text = side + " TO MOVE: " + puzzle.goal();
    if (puzzle.movesToFind() > 1 && !finished) {
      text += String.format(Locale.US, "\nMove %d of %d", step + 1, puzzle.movesToFind());
    }
    goalText.setText(text);
  }

  // ================================================================ solving

  private void onSquareClicked(int sq) {
    if (puzzle == null || finished || busy) return;
    if (selectedSquare >= 0) {
      for (Move m : MoveGenerator.legalMovesFrom(shown, selectedSquare)) {
        if (m.to != sq) continue;
        if (m.isPromotion()) {
          askPromotion(selectedSquare, sq);
        } else {
          tryMove(m);
        }
        return;
      }
    }
    int piece = shown.pieceAt(sq);
    boolean ours = Piece.isColor(piece, shown.whiteToMove());
    selectedSquare = sq != selectedSquare && ours && !MoveGenerator.legalMovesFrom(shown, sq).isEmpty() ? sq : -1;
    redraw();
  }

  private void askPromotion(final int from, final int to) {
    new PromotionDialog(this, shown.whiteToMove(), type -> {
      if (type == 0) {
        selectedSquare = -1;
        redraw();
        return;
      }
      for (Move m : MoveGenerator.legalMovesFrom(shown, from)) {
        if (m.to == to && m.promotion == type) {
          tryMove(m);
          return;
        }
      }
    }).show();
  }

  private void tryMove(Move m) {
    selectedSquare = -1;
    final Position start = shown.copy();
    Move expected = puzzle.solverMove(step);
    boolean mates = isMate(start, m);
    if (m.sameAs(expected) || mates) {
      boolean last = step == puzzle.movesToFind() - 1 || (mates && !m.sameAs(expected));
      if (last) {
        finish(start, m);
        return;
      }
      // Right, and there is more: play it, then the opponent's reply, then wait for the next move.
      busy = true;
      hintLevel = 0;
      feedbackText.setText(Notation.toSan(start, m) + " is right. Keep going!");
      final Move reply = puzzle.replyTo(step);
      animate(start, m, () -> main.postDelayed(() -> {
        if (destroyed || puzzle == null || finished) return;
        animate(shown.copy(), reply, () -> {
          step++;
          busy = false;
          updateGoal();
        });
      }, REPLY_DELAY_MS));
      return;
    }
    // Wrong: show the move, say so, then put the pieces back.
    busy = true;
    feedbackText.setText(Notation.toSan(start, m) + " isn't it. Try again!");
    animate(start, m, () -> main.postDelayed(() -> {
      if (destroyed || puzzle == null || finished) return;
      shown = start;
      busy = false;
      board.draw(shown);
    }, WRONG_MOVE_RESET_MS));
  }

  /** First press: the idea behind the puzzle. Second press: the piece to move. */
  private void showHint() {
    if (puzzle == null || finished || busy) return;
    String idea = hintIdea();
    if (hintLevel == 0 && idea != null && step == 0) {
      hintLevel = 1;
      feedbackText.setText("Hint: " + idea + ".");
      return;
    }
    hintLevel = 2;
    selectedSquare = puzzle.solverMove(step).from;
    redraw();
    feedbackText.setText("Hint: move this piece.");
  }

  /** The most specific theme's nudge, skipping ones that would give away too little. */
  private String hintIdea() {
    for (Theme t : puzzle.themes) {
      if (t == Theme.ENDGAME || t == Theme.MATE) continue; // the goal already says so
      return t.hint;
    }
    return null;
  }

  private void showSolution() {
    if (puzzle == null || finished || busy) return;
    List<Move> rest = new ArrayList<>(puzzle.line.subList(step * 2, puzzle.line.size()));
    finished = true;
    setPuzzleButtonsEnabled(false);
    updateGoal();
    feedbackText.setText("Solution: " + explanation());
    playMoves(shown.copy(), rest, () -> playMoves(shown.copy(), puzzle.continuation, null));
  }

  /** Solved: play the last move, show what the puzzle was about, then the expected follow-up. */
  private void finish(Position start, Move m) {
    finished = true;
    setPuzzleButtonsEnabled(false);
    updateGoal();
    feedbackText.setText("Correct! " + explanation());
    busy = true;
    animate(start, m, () -> {
      if (Rules.isCheckmate(shown)) {
        busy = false;
        return;
      }
      main.postDelayed(() -> playMoves(shown.copy(), puzzle.continuation, null), REPLY_DELAY_MS);
    });
  }

  /** "1. Nxf7+ Kg8 2. Nh6+ (mate in 2, next best move -0.20)\nThemes: Fork, Discovered check" */
  private String explanation() {
    String themes = puzzle.themeLabels();
    return puzzle.lineSan() + "  (" + puzzle.scoreSummary() + ")"
        + (themes.isEmpty() ? "" : "\nThemes: " + themes);
  }

  /** Animates moves one after another with a short pause between them. */
  private void playMoves(final Position from, final List<Move> moves, final Runnable onDone) {
    if (moves == null || moves.isEmpty() || destroyed || puzzle == null) {
      busy = false;
      if (onDone != null) onDone.run();
      return;
    }
    busy = true;
    final Move first = moves.get(0);
    final List<Move> rest = moves.subList(1, moves.size());
    animate(from, first, () -> main.postDelayed(() -> {
      if (destroyed || puzzle == null) return;
      playMoves(shown.copy(), rest, onDone);
    }, REPLY_DELAY_MS));
  }

  /** Slides a move on the board and updates {@link #shown}. */
  private void animate(Position before, Move m, Runnable onDone) {
    Position after = before.copy();
    after.makeMove(m);
    shown = after;
    board.animateMove(before, m, after, onDone);
  }

  private static boolean isMate(Position pos, Move m) {
    Position p = pos.copy();
    p.makeMove(m);
    return Rules.isCheckmate(p);
  }

  private void redraw() {
    board.draw(shown);
    if (selectedSquare >= 0) board.highlight(shown, selectedSquare, MoveGenerator.legalMovesFrom(shown, selectedSquare));
  }

  private void setPuzzleButtonsEnabled(boolean enabled) {
    hintButton.setEnabled(enabled);
    solutionButton.setEnabled(enabled);
    hintButton.setAlpha(enabled ? 1f : 0.5f);
    solutionButton.setAlpha(enabled ? 1f : 0.5f);
  }
}
