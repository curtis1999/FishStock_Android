package com.example.fishstock;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.SearchLimits;
import com.example.fishstock.search.SearchResult;
import com.example.fishstock.ui.BoardRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replays a finished game. For each position it shows the evaluation bar and the three best
 * lines found by a short MinMax search (default weights).
 */
public class GameAnalysis extends AppCompatActivity {
  public static final String EXTRA_START_FEN = "startFen";
  public static final String EXTRA_MOVES = "movesUci";
  private static final long THINK_PER_LINE_MS = 1000;

  private final Handler main = new Handler(Looper.getMainLooper());
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private AtomicBoolean currentStop = new AtomicBoolean(false);

  private Game game;
  private int ply;
  private BoardRenderer board;
  private TextView evalText;
  private View evalWhite;
  private View evalContainer;
  private TextView[] labels;
  private TextView[] lines;
  private Button previous;
  private Button next;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_analysis);

    String fen = getIntent().getStringExtra(EXTRA_START_FEN);
    game = fen != null ? new Game(fen) : new Game();
    String moves = getIntent().getStringExtra(EXTRA_MOVES);
    if (moves != null) {
      for (String uci : moves.trim().split("\\s+")) {
        if (uci.isEmpty() || game.isOver()) break;
        Move m = game.findUci(uci);
        if (m == null) break;
        game.play(m);
      }
    }
    ply = game.plyCount();

    board = new BoardRenderer(this);
    evalText = findViewById(R.id.evalText);
    evalWhite = findViewById(R.id.evalBarWhite);
    evalContainer = findViewById(R.id.evaluationBarContainer);
    labels = new TextView[] {findViewById(R.id.move1Label), findViewById(R.id.move2Label), findViewById(R.id.move3Label)};
    lines = new TextView[] {findViewById(R.id.move1Line), findViewById(R.id.move2Line), findViewById(R.id.move3Line)};
    previous = findViewById(R.id.previousMoveButton);
    next = findViewById(R.id.nextMoveButton);
    previous.setOnClickListener(v -> show(ply - 1));
    next.setOnClickListener(v -> show(ply + 1));
    evalContainer.post(() -> show(ply));
  }

  @Override
  protected void onDestroy() {
    currentStop.set(true);
    worker.shutdownNow();
    main.removeCallbacksAndMessages(null);
    super.onDestroy();
  }

  private void show(int target) {
    if (target < 0 || target > game.plyCount()) return;
    ply = target;
    final Position pos = game.positionAt(ply);
    board.draw(pos);
    previous.setAlpha(ply > 0 ? 1f : 0.5f);
    next.setAlpha(ply < game.plyCount() ? 1f : 0.5f);
    for (int i = 0; i < 3; i++) {
      labels[i].setText("");
      lines[i].setText(i == 0 ? "thinking..." : "");
    }
    setEval(WeightedEvaluator.withDefaults().evaluate(pos), null);

    currentStop.set(true);
    final AtomicBoolean stop = new AtomicBoolean(false);
    currentStop = stop;
    final List<Long> history = new ArrayList<>(game.positionHistory().subList(0, ply + 1));
    worker.execute(() -> {
      AlphaBetaSearch search = new AlphaBetaSearch(WeightedEvaluator.withDefaults());
      List<Move> excluded = new ArrayList<>();
      for (int i = 0; i < 3 && !stop.get(); i++) {
        SearchResult r = search.search(pos, history, SearchLimits.forTime(THINK_PER_LINE_MS), stop, excluded);
        if (r.bestMove == null || stop.get()) break;
        excluded.add(r.bestMove);
        final int index = i;
        final double whiteScore = pos.whiteToMove() ? r.score : -r.score;
        final String scoreText = formatScore(r, pos.whiteToMove());
        final String line = Notation.line(pos, r.principalVariation);
        main.post(() -> {
          if (stop.get()) return;
          labels[index].setText(scoreText);
          lines[index].setText(line);
          if (index == 0) setEval(whiteScore, scoreText);
        });
      }
    });
  }

  private static String formatScore(SearchResult r, boolean whiteToMove) {
    if (r.isMateScore()) {
      int mate = whiteToMove ? r.mateIn() : -r.mateIn();
      return (mate > 0 ? "#" : "-#") + Math.abs(mate);
    }
    double s = whiteToMove ? r.score : -r.score;
    return String.format(Locale.US, "%+.2f", s);
  }

  /** White share of the bar follows a smooth curve: +2 pawns is roughly 75% white. */
  private void setEval(double whiteScore, String text) {
    double clamped = Math.max(-20, Math.min(20, whiteScore));
    double whiteShare = 1.0 / (1.0 + Math.exp(-clamped / 1.8));
    int h = evalContainer.getHeight();
    if (h > 0) {
      ViewGroup.LayoutParams lp = evalWhite.getLayoutParams();
      lp.height = (int) Math.round(h * whiteShare);
      evalWhite.setLayoutParams(lp);
    }
    evalText.setText(text != null ? text : String.format(Locale.US, "%+.1f", whiteScore));
  }
}
