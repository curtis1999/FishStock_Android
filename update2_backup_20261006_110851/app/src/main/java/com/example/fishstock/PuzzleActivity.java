package com.example.fishstock;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.agents.FishStock;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.puzzles.Puzzle;
import com.example.fishstock.puzzles.PuzzleGenerator;
import com.example.fishstock.ui.BoardRenderer;
import com.example.fishstock.ui.PromotionDialog;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Puzzles: GENERATE PUZZLE searches sample positions until it finds one where exactly one move
 * wins, according to FishStock's evaluation (see {@link PuzzleGenerator}). You play the move on
 * the board. HINT shows which piece to move; SOLUTION plays the answer.
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

  private Puzzle puzzle;
  private Position shown;          // what is on the board now
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

    board.setOnSquareClickListener(this::onSquareClicked);
    generateButton.setOnClickListener(v -> generate());
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
    goalText.setText("Looking for a puzzle...");
    feedbackText.setText("");
    progressText.setText("Searching");

    worker.execute(() -> {
      Puzzle found = generator.find(stop, count -> {
        if (count % 10 != 0) return;
        main.post(() -> {
          if (!stop.get()) progressText.setText(String.format(Locale.US, "Searching... %d positions checked", count));
        });
      });
      main.post(() -> {
        if (destroyed || stop.get() || found == null) return;
        show(found);
      });
    });
  }

  private void show(Puzzle p) {
    puzzle = p;
    shown = p.position();
    finished = false;
    busy = false;
    selectedSquare = -1;
    board.setFlipped(!shown.whiteToMove());   // the solver plays up the board
    board.draw(shown);
    goalText.setText((shown.whiteToMove() ? "WHITE" : "BLACK") + " TO MOVE: " + p.goal());
    feedbackText.setText("");
    progressText.setText("");
    generateButton.setEnabled(true);
    setPuzzleButtonsEnabled(true);
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
    if (m.sameAs(puzzle.solution)) {
      finished = true;
      setPuzzleButtonsEnabled(false);
      feedbackText.setText("Correct! " + explanation());
      playLine(start);
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

  /** Highlights the piece that should move. */
  private void showHint() {
    if (puzzle == null || finished || busy) return;
    selectedSquare = puzzle.solution.from;
    redraw();
    feedbackText.setText("Hint: move this piece.");
  }

  private void showSolution() {
    if (puzzle == null || finished || busy) return;
    finished = true;
    setPuzzleButtonsEnabled(false);
    feedbackText.setText("Solution: " + explanation());
    playLine(shown.copy());
  }

  /** "Rxe1 (+2.92, next best move -3.54). Line: 28... Rxe1 29. Rxe1" */
  private String explanation() {
    Position start = puzzle.position();
    List<Move> line = puzzle.line.size() > 4 ? puzzle.line.subList(0, 4) : puzzle.line;
    return puzzle.solutionSan() + " (" + puzzle.scoreSummary() + ")\n" + Notation.line(start, line);
  }

  /** Plays the solution, then the opponent's expected reply. */
  private void playLine(final Position start) {
    busy = true;
    animate(start, puzzle.solution, () -> {
      if (puzzle == null || puzzle.line.size() < 2) {
        busy = false;
        return;
      }
      main.postDelayed(() -> {
        if (destroyed || puzzle == null) return;
        Move reply = puzzle.line.get(1);
        animate(shown.copy(), reply, () -> busy = false);
      }, REPLY_DELAY_MS);
    });
  }

  /** Slides a move on the board and updates {@link #shown}. */
  private void animate(Position before, Move m, Runnable onDone) {
    Position after = before.copy();
    after.makeMove(m);
    shown = after;
    board.animateMove(before, m, after, onDone);
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
