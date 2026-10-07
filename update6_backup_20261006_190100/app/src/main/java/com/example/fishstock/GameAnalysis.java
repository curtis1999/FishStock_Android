package com.example.fishstock;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.analysis.MoveReview;
import com.example.fishstock.analysis.PositionAnalyzer;
import com.example.fishstock.analysis.PositionProfile;
import com.example.fishstock.analysis.Training;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.ScoredMove;
import com.example.fishstock.search.SearchLimits;
import com.example.fishstock.search.SearchResult;
import com.example.fishstock.ui.BoardRenderer;
import com.example.fishstock.ui.EvalGraphView;
import com.example.fishstock.ui.LinesDialog;
import com.example.fishstock.puzzles.Puzzle;
import com.example.fishstock.puzzles.PuzzleBook;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replays a finished game. For each position it shows the evaluation bar and the three best
 * lines found by a short MinMax search (default weights).
 *
 * Below the board, a graph of the evaluation after every move (filled in move by move in the
 * background); tap it to jump. VIEW LINES opens a chart of every legal move's evaluation and
 * labels the position (puzzle, sharp, calm, ...).
 *
 * Every move also gets a verdict (Brilliant, Best, Okay, Mistake, Blunder, Miss) and a sentence on
 * why ({@link MoveReview}), shown under the lines for the move that led to the position on the board.
 *
 * When every move is reviewed, the two or three key moments (biggest blunders, mistakes and misses)
 * are listed; TRAIN turns them into puzzles, followed by collection puzzles on the same themes
 * ({@link Training}).
 */
public class GameAnalysis extends AppCompatActivity {
  public static final String EXTRA_START_FEN = "startFen";
  public static final String EXTRA_MOVES = "movesUci";
  /** Int: whose mistakes to train on: 1 = White, -1 = Black, 0 = both (default). */
  public static final String EXTRA_TRAIN_SIDE = "trainSide";
  /** Optional title, e.g. "Game 3: Agro vs Timid 1-0". */
  public static final String EXTRA_TITLE = "title";
  private static final long THINK_PER_LINE_MS = 1000;
  /** Per position for the graph and the move verdicts: up to 4 plies, about half a second. */
  private static final SearchLimits GRAPH_LIMITS = new SearchLimits(4, 250, 700);
  private static final long VIEW_LINES_TIME_MS = 5000;

  private final Handler main = new Handler(Looper.getMainLooper());
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final ExecutorService graphWorker = Executors.newSingleThreadExecutor();
  private final ExecutorService linesWorker = Executors.newSingleThreadExecutor();
  private final AtomicBoolean graphStop = new AtomicBoolean(false);
  private AtomicBoolean currentStop = new AtomicBoolean(false);
  private AtomicBoolean linesStop = new AtomicBoolean(false);

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
  private EvalGraphView graph;
  private TextView graphCaption;
  private double[] graphScores;
  private MoveReview.Result[] reviews;
  private TextView verdictText;
  private TextView explanationText;
  private TextView summaryText;
  private LinearLayout keyMomentsList;
  private Button trainButton;
  private List<Training.KeyMoment> keyMoments = new ArrayList<>();
  private final ExecutorService trainWorker = Executors.newSingleThreadExecutor();
  private final AtomicBoolean trainStop = new AtomicBoolean(false);
  private boolean destroyed;

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
    ply = 0; // start from the beginning of the game; step forward with the arrows or the graph

    board = new BoardRenderer(this);
    evalText = findViewById(R.id.evalText);
    evalWhite = findViewById(R.id.evalBarWhite);
    evalContainer = findViewById(R.id.evaluationBarContainer);
    labels = new TextView[] {findViewById(R.id.move1Label), findViewById(R.id.move2Label), findViewById(R.id.move3Label)};
    lines = new TextView[] {findViewById(R.id.move1Line), findViewById(R.id.move2Line), findViewById(R.id.move3Line)};
    previous = findViewById(R.id.previousMoveButton);
    next = findViewById(R.id.nextMoveButton);
    graph = findViewById(R.id.evalGraph);
    graphCaption = findViewById(R.id.evalGraphCaption);
    verdictText = findViewById(R.id.moveVerdict);
    explanationText = findViewById(R.id.moveExplanation);
    summaryText = findViewById(R.id.reviewSummary);
    keyMomentsList = findViewById(R.id.keyMoments);
    trainButton = findViewById(R.id.trainButton);
    trainButton.setOnClickListener(v -> startTraining());
    String title = getIntent().getStringExtra(EXTRA_TITLE);
    if (title != null) setTitle(title);
    previous.setOnClickListener(v -> show(ply - 1));
    next.setOnClickListener(v -> show(ply + 1));
    findViewById(R.id.viewLinesButton).setOnClickListener(v -> viewLines());
    graph.setOnPlySelectedListener(this::show);
    graph.setPlyCount(game.plyCount() + 1, Position.fromFen(game.startFen()).whiteToMove());
    graphScores = new double[game.plyCount() + 1];
    reviews = new MoveReview.Result[game.plyCount() + 1];
    java.util.Arrays.fill(graphScores, Double.NaN);
    evalContainer.post(() -> show(ply));
    fillGraph();
  }

  @Override
  protected void onDestroy() {
    destroyed = true;
    currentStop.set(true);
    graphStop.set(true);
    linesStop.set(true);
    trainStop.set(true);
    trainWorker.shutdownNow();
    worker.shutdownNow();
    graphWorker.shutdownNow();
    linesWorker.shutdownNow();
    main.removeCallbacksAndMessages(null);
    super.onDestroy();
  }

  private void show(int target) {
    if (target < 0 || target > game.plyCount()) return;
    ply = target;
    final Position pos = game.positionAt(ply);
    board.draw(pos);
    graph.setCurrentPly(ply);
    updateGraphCaption();
    updateVerdict();
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

  // ================================================================ game graph

  /**
   * Scores every position of the game in the background, adding nodes to the graph as it goes,
   * and judges each move as soon as the positions before and after it are scored.
   */
  private void fillGraph() {
    final Game g = game;
    graphWorker.execute(() -> {
      WeightedEvaluator evaluator = WeightedEvaluator.withDefaults();
      AlphaBetaSearch search = new AlphaBetaSearch(evaluator);
      SearchResult previous = null;
      for (int i = 0; i <= g.plyCount() && !graphStop.get(); i++) {
        Position p = g.positionAt(i);
        List<Long> history = new ArrayList<>(g.positionHistory().subList(0, i + 1));
        double white;
        SearchResult r = null;
        if (!MoveGenerator.hasLegalMove(p)) {
          // Game over here: mate is the biggest possible swing, stalemate is level.
          white = p.inCheck() ? (p.whiteToMove() ? -PositionProfile.MATE_AS_PAWNS : PositionProfile.MATE_AS_PAWNS) : 0;
        } else {
          r = search.search(p, history, GRAPH_LIMITS, graphStop);
          if (graphStop.get()) return;
          double s = r.isMateScore() ? Math.signum(r.score) * PositionProfile.MATE_AS_PAWNS : r.score;
          white = p.whiteToMove() ? s : -s;
        }
        MoveReview.Result review = null;
        if (i > 0 && previous != null) {
          review = MoveReview.review(g.positionAt(i - 1), g.moves().get(i - 1), previous, r, evaluator);
        }
        previous = r;
        final int index = i;
        final double score = white;
        final MoveReview.Result rev = review;
        main.post(() -> {
          if (destroyed) return;
          graphScores[index] = score;
          graph.setScore(index, score);
          if (rev != null) {
            reviews[index] = rev;
            MoveReview.Verdict v = rev.verdict;
            boolean notable = v == MoveReview.Verdict.BRILLIANT || v == MoveReview.Verdict.MISTAKE
                || v == MoveReview.Verdict.BLUNDER || v == MoveReview.Verdict.MISS;
            graph.setNodeColour(index, notable ? v.colour : 0);
          }
          if (index == ply) {
            updateGraphCaption();
            updateVerdict();
          }
          if (index == g.plyCount()) {
            updateSummary();
            showKeyMoments();
          }
        });
      }
    });
  }

  /** Verdict and explanation for the move that led to the position on the board. */
  private void updateVerdict() {
    MoveReview.Result r = ply > 0 && ply < reviews.length ? reviews[ply] : null;
    if (ply == 0) {
      verdictText.setText("");
      explanationText.setText("");
    } else if (r == null) {
      verdictText.setText("");
      explanationText.setText("Reviewing the moves...");
    } else {
      verdictText.setText(r.headline());
      verdictText.setTextColor(r.verdict.colour);
      explanationText.setText(r.explanation);
    }
  }

  /** "White: 1 brilliant, 18 best, 9 okay, 2 mistakes, 1 blunder, 0 misses" and the same for Black. */
  private void updateSummary() {
    int[][] counts = new int[2][MoveReview.Verdict.values().length];
    boolean whiteFirst = Position.fromFen(game.startFen()).whiteToMove();
    for (int i = 1; i < reviews.length; i++) {
      if (reviews[i] == null) continue;
      boolean whiteMoved = whiteFirst == (i % 2 == 1);
      counts[whiteMoved ? 0 : 1][reviews[i].verdict.ordinal()]++;
    }
    StringBuilder sb = new StringBuilder();
    for (int side = 0; side < 2; side++) {
      sb.append(side == 0 ? "White: " : "\nBlack: ");
      MoveReview.Verdict[] all = MoveReview.Verdict.values();
      for (int k = 0; k < all.length; k++) {
        if (k > 0) sb.append(", ");
        sb.append(counts[side][k]).append(' ').append(all[k].label.toLowerCase(Locale.US));
      }
    }
    summaryText.setText(sb.toString());
  }

  /** "Move 23 (Black): +1.4" for the position on the board. */
  private void updateGraphCaption() {
    if (ply == 0) {
      graphCaption.setText("Start of the game. Tap the graph to jump to a move");
      return;
    }
    Position before = game.positionAt(ply - 1);
    Move m = game.moves().get(ply - 1);
    String move = (before.whiteToMove() ? before.fullmoveNumber() + ". " : before.fullmoveNumber() + "... ")
        + Notation.toSan(before, m);
    double s = graphScores[ply];
    String score = Double.isNaN(s) ? "..." : Math.abs(s) >= PositionProfile.MATE_AS_PAWNS
        ? (s > 0 ? "White mates" : "Black mates") : String.format(Locale.US, "%+.2f", s);
    graphCaption.setText("After " + move + ":  " + score);
  }

  // ================================================================ key moments and training

  /** One button per key moment (tap to jump there), then TRAIN. */
  private void showKeyMoments() {
    boolean whiteFirst = Position.fromFen(game.startFen()).whiteToMove();
    int side = getIntent().getIntExtra(EXTRA_TRAIN_SIDE, 0);
    keyMoments = Training.keyMoments(reviews, side, whiteFirst);
    keyMomentsList.removeAllViews();
    TextView title = new TextView(this);
    title.setTextSize(16);
    title.setText(keyMoments.isEmpty() ? "No big mistakes in this game." : "Key moments (tap to see):");
    keyMomentsList.addView(title);
    for (final Training.KeyMoment k : keyMoments) {
      Button b = new Button(this);
      b.setText(k.label());
      b.setTextColor(k.review.verdict.colour);
      b.setOnClickListener(v -> show(k.ply));
      keyMomentsList.addView(b);
    }
    trainButton.setVisibility(keyMoments.isEmpty() ? View.GONE : View.VISIBLE);
    trainButton.setEnabled(true);
    trainButton.setText("TRAIN");
  }

  /** Builds the training puzzles in the background, then opens them on the puzzle screen. */
  private void startTraining() {
    if (keyMoments.isEmpty()) return;
    trainButton.setEnabled(false);
    trainButton.setText("Preparing puzzles...");
    final List<Training.KeyMoment> moments = new ArrayList<>(keyMoments);
    final Game g = game;
    trainWorker.execute(() -> {
      List<Puzzle> set = Training.buildSet(g, moments, WeightedEvaluator.withDefaults(), trainStop);
      final String[] entries = new String[set.size()];
      for (int i = 0; i < set.size(); i++) entries[i] = PuzzleBook.format(set.get(i));
      main.post(() -> {
        if (destroyed) return;
        trainButton.setEnabled(true);
        trainButton.setText("TRAIN");
        if (entries.length == 0) return;
        Intent intent = new Intent(GameAnalysis.this, PuzzleActivity.class);
        intent.putExtra(PuzzleActivity.EXTRA_TRAINING, entries);
        startActivity(intent);
      });
    });
  }

  // ================================================================ view lines

  /** Opens the chart of every move's evaluation for the position on the board. */
  private void viewLines() {
    final Position pos = game.positionAt(ply);
    if (!MoveGenerator.hasLegalMove(pos)) return;
    final LinesDialog dialog = new LinesDialog(this, pos.whiteToMove());
    linesStop.set(true);
    final AtomicBoolean stop = new AtomicBoolean(false);
    linesStop = stop;
    dialog.setOnDismissListener(d -> stop.set(true));
    dialog.show();

    final List<Long> history = new ArrayList<>(game.positionHistory().subList(0, ply + 1));
    linesWorker.execute(() -> new PositionAnalyzer(WeightedEvaluator.withDefaults()).analyze(
        pos, history, VIEW_LINES_TIME_MS, stop, new PositionAnalyzer.Listener() {
          @Override
          public void onMovesScored(List<ScoredMove> moves, int depth) {
            final List<String> names = new ArrayList<>();
            for (ScoredMove m : moves) names.add(Notation.toSan(pos, m.move));
            final boolean done = depth >= PositionAnalyzer.MOVE_DEPTH;
            main.post(() -> {
              if (!stop.get() && dialog.isShowing()) dialog.showMoves(moves, names, depth, done);
            });
          }

          @Override
          public void onProfile(PositionProfile profile) {
            main.post(() -> {
              if (!stop.get() && dialog.isShowing()) dialog.showProfile(profile);
            });
          }
        }));
  }

  // ================================================================ helpers

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
