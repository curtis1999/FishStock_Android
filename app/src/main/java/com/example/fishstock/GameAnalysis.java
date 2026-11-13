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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.analysis.MoveReview;
import com.example.fishstock.analysis.PositionAnalyzer;
import com.example.fishstock.analysis.PositionProfile;
import com.example.fishstock.analysis.Training;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Piece;
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
 * Tap a piece and a square to try a move that wasn't played: the board becomes an exploration board
 * (evaluation and best lines for the new position) until BACK TO GAME.
 *
 * Every move also gets a verdict (Brilliant, Best, Okay, Mistake, Blunder, Miss) and a sentence on
 * why ({@link MoveReview}), shown under the lines for the move that led to the position on the board.
 *
 * When every move is reviewed, the two or three key moments (biggest blunders, mistakes and misses)
 * are listed; TRAIN turns them into puzzles, followed by collection puzzles on the same themes
 * ({@link Training}).
 *
 * Opened with {@link #EXTRA_FREE_BOARD} (the home screen's Analyze button) it is an analysis board
 * instead: no game, just the start position. Moves played for either side become the line on the
 * board (the arrows step through it; a different move part-way back replaces the rest), and the
 * engine's three best moves are shown for whatever is on the board.
 *
 * IMPORT GAME (on both) reads a pasted PGN or move list ({@link PgnImport}) and reopens this screen
 * on that game, reviewed from the chosen side: that side at the bottom, and its mistakes as the key
 * moments to train on. FLIP turns the board around. SAVE names the game and keeps it in
 * {@link SavedGamesActivity Saved Games}.
 */
public class GameAnalysis extends AppCompatActivity {
  public static final String EXTRA_START_FEN = "startFen";
  public static final String EXTRA_MOVES = "movesUci";
  /** Int: whose mistakes to train on: 1 = White, -1 = Black, 0 = both (default). */
  public static final String EXTRA_TRAIN_SIDE = "trainSide";
  /** Optional title, e.g. "Game 3: Agro vs Timid 1-0". */
  public static final String EXTRA_TITLE = "title";
  /** Boolean: an empty analysis board to play moves on (no game to review). */
  public static final String EXTRA_FREE_BOARD = "freeBoard";
  /** Boolean: Black at the bottom. */
  public static final String EXTRA_FLIPPED = "flipped";
  /** String: the Saved Games entry this game was opened from (SAVE then renames it). */
  public static final String EXTRA_SAVED_ID = "savedId";
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
  /** Per ply: "Best was 12. Nf3 (+0.45)" or "Also good: 12. Bd3 (+0.40)"; null if nothing to add. */
  private String[] bestMoveNotes;
  private TextView verdictText;
  private TextView explanationText;
  private TextView summaryText;
  private LinearLayout keyMomentsList;
  private Button trainButton;
  private List<Training.KeyMoment> keyMoments = new ArrayList<>();
  private final ExecutorService trainWorker = Executors.newSingleThreadExecutor();
  private final AtomicBoolean trainStop = new AtomicBoolean(false);
  private boolean destroyed;
  // Exploration: a line of moves not played in the game, starting at position variationBase.
  private final List<Move> variation = new ArrayList<>();
  private int variationBase = -1;
  private int variationView;
  private int selectedSquare = -1;
  private Button backToGame;
  /** Analysis board: moves played extend the game instead of starting an exploration line. */
  private boolean freeBoard;
  /** The Saved Games entry for this game, once saved (or when opened from Saved Games). */
  private String savedId;
  /** The moves as they were last saved, to tell whether an analysis-board line has grown since. */
  private String savedMoves;
  private Button saveButton;
  /**
   * Retrying a key moment: the engine lines are worked out but not shown (they'd give the answer
   * away) until a move is tried or the line text is tapped.
   */
  private boolean hideLines;
  private final String[][] hiddenLines = new String[3][];
  private static final String HIDDEN_LINES_TEXT = "Engine lines hidden. Tap to show";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_analysis);

    String fen = getIntent().getStringExtra(EXTRA_START_FEN);
    game = fen != null ? new Game(fen) : new Game();
    String moves = getIntent().getStringExtra(EXTRA_MOVES);
    freeBoard = getIntent().getBooleanExtra(EXTRA_FREE_BOARD, false) || moves == null || moves.trim().isEmpty();
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
    board.setFlipped(getIntent().getBooleanExtra(EXTRA_FLIPPED, false));
    evalText = findViewById(R.id.evalText);
    evalWhite = findViewById(R.id.evalBarWhite);
    evalContainer = findViewById(R.id.evaluationBarContainer);
    labels = new TextView[] {findViewById(R.id.move1Label), findViewById(R.id.move2Label), findViewById(R.id.move3Label)};
    lines = new TextView[] {findViewById(R.id.move1Line), findViewById(R.id.move2Line), findViewById(R.id.move3Line)};
    for (int i = 0; i < 3; i++) {
      lines[i].setOnClickListener(v -> revealLines());
      labels[i].setOnClickListener(v -> revealLines());
    }
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
    else if (freeBoard) setTitle("Analysis Board");
    findViewById(R.id.importGameButton).setOnClickListener(v -> new ImportGameDialog(this, this::openImported).show());
    findViewById(R.id.flipBoardButton).setOnClickListener(v -> flipBoard());
    saveButton = findViewById(R.id.saveGameButton);
    saveButton.setOnClickListener(v -> saveGame());
    savedId = getIntent().getStringExtra(EXTRA_SAVED_ID);
    if (savedId != null) {
      SavedGames.Entry saved = new SavedGames(this).get(savedId);
      if (saved == null) savedId = null;
      else savedMoves = saved.movesUci;
    }
    orientEvalBar();
    previous.setOnClickListener(v -> stepBack());
    next.setOnClickListener(v -> stepForward());
    backToGame = findViewById(R.id.backToGame);
    backToGame.setOnClickListener(v -> show(variationBase >= 0 ? variationBase : ply));
    board.setOnSquareClickListener(this::onSquareClicked);
    findViewById(R.id.viewLinesButton).setOnClickListener(v -> viewLines());
    graph.setOnPlySelectedListener(this::show);
    graph.setPlyCount(game.plyCount() + 1, Position.fromFen(game.startFen()).whiteToMove());
    graphScores = new double[game.plyCount() + 1];
    reviews = new MoveReview.Result[game.plyCount() + 1];
    bestMoveNotes = new String[game.plyCount() + 1];
    java.util.Arrays.fill(graphScores, Double.NaN);
    evalContainer.post(() -> show(ply));
    if (freeBoard) {
      // Nothing to review: no graph, verdicts, key moments or training.
      graph.setVisibility(View.GONE);
      graphCaption.setVisibility(View.GONE);
      summaryText.setVisibility(View.GONE);
      keyMomentsList.setVisibility(View.GONE);
      trainButton.setVisibility(View.GONE);
    } else {
      fillGraph();
    }
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
    show(target, false);
  }

  /** @param retry hide the engine lines (a key moment to solve again) */
  private void show(int target, boolean retry) {
    if (target < 0 || target > game.plyCount()) return;
    leaveVariation();
    hideLines = retry;
    ply = target;
    final Position pos = game.positionAt(ply);
    board.draw(pos);
    if (freeBoard) {
      showFreeBoardLine();
    } else {
      graph.setCurrentPly(ply);
      updateGraphCaption();
      updateVerdict();
    }
    refreshSaveButton();
    previous.setAlpha(ply > 0 ? 1f : 0.5f);
    next.setAlpha(ply < game.plyCount() ? 1f : 0.5f);
    analyze(pos, new ArrayList<>(game.positionHistory().subList(0, ply + 1)));
  }

  /** Evaluation bar and the three best lines for a position (game or exploration). */
  private void analyze(final Position pos, final List<Long> history) {
    for (int i = 0; i < 3; i++) {
      labels[i].setText("");
      lines[i].setText(i == 0 ? (hideLines ? HIDDEN_LINES_TEXT : "thinking...") : "");
      hiddenLines[i] = null;
    }
    // A mate already proven for this position (found here or on an earlier move of the same line)
    // stays on the bar: a shallower search later must not turn "#3" back into "+6.2".
    final Integer knownMate = knownMates.get(pos.hash());
    if (knownMate != null) setMate(knownMate);
    else setEval(WeightedEvaluator.withDefaults().evaluate(pos), null);

    currentStop.set(true);
    final AtomicBoolean stop = new AtomicBoolean(false);
    currentStop = stop;
    if (!MoveGenerator.hasLegalMove(pos)) {
      lines[0].setText(pos.inCheck() ? "Checkmate" : "Stalemate");
      if (pos.inCheck()) {
        setMate(pos.whiteToMove() ? -1 : 1); // the bar all one colour...
        evalText.setText(pos.whiteToMove() ? "0-1" : "1-0"); // ...and the result
      } else {
        setEval(0, "½-½");
      }
      return;
    }
    worker.execute(() -> {
      AlphaBetaSearch search = new AlphaBetaSearch(WeightedEvaluator.withDefaults());
      List<Move> excluded = new ArrayList<>();
      for (int i = 0; i < 3 && !stop.get(); i++) {
        SearchResult r = search.search(pos, history, SearchLimits.forTime(THINK_PER_LINE_MS), stop, excluded);
        if (r.bestMove == null || stop.get()) break;
        excluded.add(r.bestMove);
        if (i == 0) rememberMate(pos, r);
        final int index = i;
        final double whiteScore = pos.whiteToMove() ? r.score : -r.score;
        final String scoreText = formatScore(r, pos.whiteToMove());
        final String line = Notation.line(pos, r.principalVariation);
        final Integer whiteMate = r.isMateScore() ? (pos.whiteToMove() ? r.mateIn() : -r.mateIn()) : null;
        main.post(() -> {
          if (stop.get()) return;
          if (hideLines) {
            hiddenLines[index] = new String[] {scoreText, line};
          } else {
            labels[index].setText(scoreText);
            lines[index].setText(line);
          }
          if (index != 0) return;
          Integer mate = whiteMate != null ? whiteMate : knownMates.get(pos.hash());
          if (mate != null) setMate(mate);
          else setEval(whiteScore, scoreText);
        });
      }
    });
  }

  // ================================================================ forced mates

  /**
   * Positions with a proven forced mate: hash -> moves to mate, positive if White mates. Filled from
   * every mate score found (here and by the graph), along the whole mating line: if the root is mate
   * in 3, the position after the first two moves of the line is mate in 2, and so on. So once a mate
   * is found, stepping forward keeps counting down instead of a shorter search showing "+6.2" again.
   */
  private final java.util.Map<Long, Integer> knownMates = new java.util.concurrent.ConcurrentHashMap<>();

  private void rememberMate(Position root, SearchResult r) {
    if (r == null || !r.isMateScore() || r.principalVariation == null) return;
    int toMove = r.mateIn(); // signed for the side to move at the root
    if (toMove == 0) return;
    boolean rootMates = toMove > 0;
    boolean whiteMates = root.whiteToMove() == rootMates;
    int moves = Math.abs(toMove);
    Position p = root.copy();
    for (int k = 0; k <= r.principalVariation.size(); k++) {
      // Moves the mating side still needs after k plies of the line.
      int left = rootMates ? moves - (k + 1) / 2 : moves - k / 2;
      if (left <= 0) break;
      Integer old = knownMates.get(p.hash());
      if (old == null || Math.abs(old) > left) knownMates.put(p.hash(), whiteMates ? left : -left);
      if (k == r.principalVariation.size()) break;
      p.makeMove(r.principalVariation.get(k));
    }
  }

  /** Forced mate: the bar all one colour and "#3" (White mates) or "-#3" (Black mates). */
  private void setMate(int whiteMate) {
    int h = evalContainer.getHeight();
    if (h > 0) {
      ViewGroup.LayoutParams lp = evalWhite.getLayoutParams();
      lp.height = whiteMate > 0 ? h : 0;
      evalWhite.setLayoutParams(lp);
    }
    evalText.setText((whiteMate > 0 ? "#" : "-#") + Math.abs(whiteMate));
  }

  // ================================================================ exploring other moves

  /**
   * Tap a piece and a square to play a move on the analysis board. The game's own next move just
   * steps forward; any other move starts an exploration line from this position, evaluated and
   * searched like the game positions. The arrows then step through that line; BACK TO GAME returns.
   */
  private void onSquareClicked(int sq) {
    Position pos = displayed();
    if (selectedSquare >= 0) {
      for (Move m : MoveGenerator.legalMovesFrom(pos, selectedSquare)) {
        if (m.to != sq) continue;
        // Promotions on the analysis board always make a queen (pick another in the game itself).
        if (m.isPromotion() && m.promotion != Piece.QUEEN) continue;
        selectedSquare = -1;
        playExploreMove(m);
        return;
      }
    }
    boolean own = Piece.isColor(pos.pieceAt(sq), pos.whiteToMove());
    selectedSquare = own && sq != selectedSquare && !MoveGenerator.legalMovesFrom(pos, sq).isEmpty() ? sq : -1;
    board.draw(pos);
    if (selectedSquare >= 0) board.highlight(pos, selectedSquare, MoveGenerator.legalMovesFrom(pos, selectedSquare));
  }

  private void playExploreMove(Move m) {
    if (variationBase < 0 && ply < game.plyCount() && game.moves().get(ply).sameAs(m)) {
      show(ply + 1); // that's what was played: follow the game
      return;
    }
    if (freeBoard && variationBase < 0) {
      // Analysis board: the move becomes the line; anything after this point is replaced.
      while (game.plyCount() > ply) game.undo();
      if (!game.isOver()) {
        game.play(m);
        show(ply + 1);
        return;
      }
      // A draw by repetition or the 50-move rule ends the line here; keep going as a side line.
    }
    if (variationBase < 0) {
      variationBase = ply;
      variation.clear();
      variationView = 0;
    }
    while (variation.size() > variationView) variation.remove(variation.size() - 1);
    variation.add(m);
    variationView++;
    showVariation();
  }

  /** The position on the board: the game position, or the exploration line up to where we are. */
  private Position displayed() {
    if (variationBase < 0) return game.positionAt(ply);
    Position p = game.positionAt(variationBase);
    for (int i = 0; i < variationView; i++) p.makeMove(variation.get(i));
    return p;
  }

  /** Shows the engine lines that were held back while retrying a key moment. */
  private void revealLines() {
    if (!hideLines) return;
    hideLines = false;
    for (int i = 0; i < 3; i++) {
      labels[i].setText(hiddenLines[i] != null ? hiddenLines[i][0] : "");
      lines[i].setText(hiddenLines[i] != null ? hiddenLines[i][1] : i == 0 ? "thinking..." : "");
    }
  }

  private void showVariation() {
    hideLines = false; // a move was tried: no answer left to give away
    Position pos = displayed();
    board.draw(pos);
    backToGame.setVisibility(View.VISIBLE);
    previous.setAlpha(1f);
    next.setAlpha(variationView < variation.size() ? 1f : 0.5f);
    Position base = game.positionAt(variationBase);
    String line = variation.isEmpty() ? "" : Notation.line(base, variation.subList(0, Math.max(1, variationView)));
    verdictText.setTextColor(0xFF6200EE);
    verdictText.setText("EXPLORING (not played in the game)");
    explanationText.setText(variationView == 0 ? "Back at the game position. Play a move to explore."
        : line + "\nThe game went " + gameMoveText(variationBase) + " here.");
    List<Long> history = new ArrayList<>(game.positionHistory().subList(0, variationBase + 1));
    Position p = base.copy();
    for (int i = 0; i < variationView; i++) {
      p.makeMove(variation.get(i));
      history.add(p.hash());
    }
    analyze(pos, history);
  }

  private String gameMoveText(int at) {
    if (at >= game.plyCount()) return "no further (the game ended)";
    Position before = game.positionAt(at);
    return before.fullmoveNumber() + (before.whiteToMove() ? ". " : "... ") + Notation.toSan(before, game.moves().get(at));
  }

  private void stepBack() {
    if (variationBase >= 0) {
      if (variationView > 0) {
        variationView--;
        showVariation();
      } else {
        show(variationBase);
      }
      return;
    }
    show(ply - 1);
  }

  /** Analysis board: the moves so far, with the one that led here marked. */
  private void showFreeBoardLine() {
    verdictText.setText("");
    if (game.plyCount() == 0) {
      explanationText.setText("Play moves for either side. The engine's three best moves are shown above.");
      return;
    }
    Position start = Position.fromFen(game.startFen());
    String played = ply == 0 ? "" : Notation.line(start, game.moves().subList(0, ply));
    String rest = ply == game.plyCount() ? "" : "   (then " + Notation.line(game.positionAt(ply), game.moves().subList(ply, game.plyCount())) + ")";
    String over = ply == game.plyCount() && game.isOver() ? "\n" + gameOverText() : "";
    explanationText.setText((played.isEmpty() ? "Start position" : played) + rest + over);
  }

  private String gameOverText() {
    switch (game.result()) {
      case WHITE_CHECKMATES:
        return "Checkmate: White wins.";
      case BLACK_CHECKMATES:
        return "Checkmate: Black wins.";
      case STALEMATE:
        return "Stalemate.";
      case THREEFOLD_REPETITION:
        return "Draw by threefold repetition.";
      case FIFTY_MOVE_RULE:
        return "Draw by the 50-move rule.";
      case INSUFFICIENT_MATERIAL:
        return "Draw: insufficient material.";
      default:
        return "";
    }
  }

  // ================================================================ import and flip

  /** Reopens this screen on the pasted game, reviewed from the chosen side. */
  private void openImported(PgnImport.Result imported, boolean asWhite) {
    Intent intent = new Intent(this, GameAnalysis.class);
    intent.putExtra(EXTRA_START_FEN, imported.startFen);
    intent.putExtra(EXTRA_MOVES, imported.movesText());
    intent.putExtra(EXTRA_TITLE, imported.title);
    intent.putExtra(EXTRA_TRAIN_SIDE, asWhite ? 1 : -1);
    intent.putExtra(EXTRA_FLIPPED, !asWhite);
    startActivity(intent);
    finish(); // back from the imported game goes home, not to the board it was imported from
  }

  // ================================================================ saved games

  /** "e2e4 e7e5 ...": the game's moves as saved. */
  private String movesText() {
    StringBuilder sb = new StringBuilder();
    for (Move m : game.moves()) sb.append(m.toUci()).append(' ');
    return sb.toString().trim();
  }

  /**
   * SAVE: name the game and put it in Saved Games. Already saved: rename it, or (an analysis-board
   * line that has grown since) save the longer line into the same entry.
   */
  private void saveGame() {
    if (game.plyCount() == 0) {
      Toast.makeText(this, "Play some moves first.", Toast.LENGTH_SHORT).show();
      return;
    }
    final SavedGames store = new SavedGames(this);
    final String moves = movesText();
    boolean changed = savedId != null && !moves.equals(savedMoves);
    String suggested;
    if (savedId != null) {
      SavedGames.Entry e = store.get(savedId);
      suggested = e != null ? e.name : getTitle().toString();
    } else {
      String title = getIntent().getStringExtra(EXTRA_TITLE);
      String date = new java.text.SimpleDateFormat("MMM d, yyyy", Locale.US).format(new java.util.Date());
      suggested = title != null ? title : (freeBoard ? "Analysis line, " : "Game, ") + date;
    }
    String dialogTitle = savedId == null ? "Save game as" : changed ? "Save the new moves as" : "Rename game";
    SavedGamesActivity.askName(this, dialogTitle, suggested, name -> {
      if (savedId == null) {
        savedId = store.add(name, game.startFen(), moves, getIntent().getIntExtra(EXTRA_TRAIN_SIDE, 0));
      } else {
        store.rename(savedId, name);
        if (changed) store.updateMoves(savedId, moves);
      }
      savedMoves = moves;
      setTitle(name);
      refreshSaveButton();
      Toast.makeText(this, "Saved to Saved Games", Toast.LENGTH_SHORT).show();
    });
  }

  /** "SAVE", or "SAVED ✓" while Saved Games has exactly these moves (tap it to rename). */
  private void refreshSaveButton() {
    if (saveButton == null) return;
    boolean upToDate = savedId != null && movesText().equals(savedMoves);
    saveButton.setText(upToDate ? "SAVED ✓" : "SAVE");
  }

  private void flipBoard() {
    board.setFlipped(!board.isFlipped());
    selectedSquare = -1;
    board.draw(displayed());
    orientEvalBar();
  }

  /** The bar's white part sits on the side away from White's pieces, as before; flipping mirrors it. */
  private void orientEvalBar() {
    android.widget.FrameLayout.LayoutParams lp = (android.widget.FrameLayout.LayoutParams) evalWhite.getLayoutParams();
    lp.gravity = board.isFlipped() ? android.view.Gravity.BOTTOM : android.view.Gravity.TOP;
    evalWhite.setLayoutParams(lp);
  }

  private void stepForward() {
    if (variationBase >= 0) {
      if (variationView < variation.size()) {
        variationView++;
        showVariation();
      }
      return;
    }
    show(ply + 1);
  }

  private void leaveVariation() {
    variationBase = -1;
    variation.clear();
    variationView = 0;
    selectedSquare = -1;
    if (backToGame != null) backToGame.setVisibility(View.GONE);
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
      List<Long> previousHistory = null;
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
          rememberMate(p, r);
          double s = r.isMateScore() ? Math.signum(r.score) * PositionProfile.MATE_AS_PAWNS : r.score;
          white = p.whiteToMove() ? s : -s;
          // Proven earlier in the line but too deep for this short search: keep it a mate.
          Integer mate = knownMates.get(p.hash());
          if (!r.isMateScore() && mate != null) white = Math.signum(mate) * PositionProfile.MATE_AS_PAWNS;
        }
        MoveReview.Result review = null;
        String note = null;
        if (i > 0 && previous != null) {
          review = MoveReview.review(g.positionAt(i - 1), g.moves().get(i - 1), previous, r, evaluator);
          note = bestMoveNote(search, g.positionAt(i - 1), previousHistory, g.moves().get(i - 1), previous, review);
          if (graphStop.get()) return;
        }
        previous = r;
        previousHistory = history;
        final int index = i;
        final double score = white;
        final MoveReview.Result rev = review;
        final String bestNote = note;
        main.post(() -> {
          if (destroyed) return;
          graphScores[index] = score;
          graph.setScore(index, score);
          if (rev != null) {
            reviews[index] = rev;
            bestMoveNotes[index] = bestNote;
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
    if (variationBase >= 0) return; // exploring: the exploration text stays
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
      String note = bestMoveNotes[ply];
      explanationText.setText(note == null ? r.explanation : r.explanation + "\n" + note);
    }
  }

  // ================================================================ best move not played

  /** A second move counts as "also good" if it throws away under 3% winning chance against the best. */
  private static final double SIMILAR_WIN_CHANCE = 0.03;

  /**
   * What to tell the player about the engine's choice for the move that was played (worker thread).
   * Played something else: "Best was 12. Nf3 (+0.45)" (or "Engine's choice: ..." when the played move
   * was judged as good anyway). Played the engine's move: if a second move is about as good,
   * "Also good: 12. Bd3 (+0.40)", found with one extra search excluding the played move.
   */
  private String bestMoveNote(AlphaBetaSearch search, Position before, List<Long> history, Move played,
                              SearchResult best, MoveReview.Result review) {
    if (best == null || best.bestMove == null || history == null) return null;
    boolean white = before.whiteToMove();
    if (!played.sameAs(best.bestMove)) {
      boolean judgedBest = review != null && (review.verdict == MoveReview.Verdict.BRILLIANT
          || "best".equalsIgnoreCase(review.verdict.label));
      return (judgedBest ? "Engine's choice (about as good): " : "Best was ")
          + moveText(before, best.bestMove) + " (" + formatScore(best, white) + ")";
    }
    List<Move> excluded = new ArrayList<>();
    excluded.add(played);
    SearchResult alt = search.search(before, history, GRAPH_LIMITS, graphStop, excluded);
    if (graphStop.get() || alt == null || alt.bestMove == null) return null;
    if (winChance(best) - winChance(alt) >= SIMILAR_WIN_CHANCE) return null;
    return "Also good: " + moveText(before, alt.bestMove) + " (" + formatScore(alt, white) + ")";
  }

  /** "12. Nf3" or "12... Nf6". */
  private static String moveText(Position before, Move m) {
    return before.fullmoveNumber() + (before.whiteToMove() ? ". " : "... ") + Notation.toSan(before, m);
  }

  /** Winning chance (0..1) for the side to move, the same curve the move verdicts use. */
  private static double winChance(SearchResult r) {
    double pawns = r.isMateScore() ? Math.signum(r.score) * PositionProfile.MATE_AS_PAWNS : r.score;
    return winChance(pawns);
  }

  private static double winChance(double pawns) {
    return 1.0 / (1.0 + Math.exp(-0.368 * pawns));
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
    int trainSide = getIntent().getIntExtra(EXTRA_TRAIN_SIDE, 0);
    StringBuilder sb = new StringBuilder();
    for (int side = 0; side < 2; side++) {
      boolean isWhite = side == 0;
      String name = isWhite ? "White" : "Black";
      if (trainSide == (isWhite ? 1 : -1)) name += " (you)";
      sb.append(side == 0 ? "" : "\n\n").append(name).append(": ");
      MoveReview.Verdict[] all = MoveReview.Verdict.values();
      for (int k = 0; k < all.length; k++) {
        if (k > 0) sb.append(", ");
        sb.append(counts[side][k]).append(' ').append(all[k].label.toLowerCase(Locale.US));
      }
      sb.append('\n').append(ratingLine(counts[side], isWhite, whiteFirst));
    }
    summaryText.setText(sb.toString());
  }

  // ================================================================ rough rating estimate

  /**
   * "Accuracy 81%  ·  Plays like ≈ 1450 (rough guess)".
   *
   * Accuracy is Lichess's formula: each move scores 103.17·e^(−0.0435·Δ) − 3.17, where Δ is the
   * winning chance (in %) it gave away, averaged over the side's moves.
   *
   * The rating is a hand-calibrated guess from how often each verdict comes up, for fun only:
   *   rating ≈ 2400 − 6000·blunder rate − 3000·(mistake + miss) rate − 1000·okay rate,
   * pinned so roughly "15% blunders, 12% mistakes, 35% okay" lands near 800 and "2% blunders, 5%
   * mistakes, 25% okay" near 1900. Clamped to 400..2500 and rounded to 50. Under 8 moves: no guess.
   */
  private String ratingLine(int[] counts, boolean white, boolean whiteFirst) {
    int n = 0, okay = 0, mistakes = 0, blunders = 0;
    for (MoveReview.Verdict v : MoveReview.Verdict.values()) {
      int c = counts[v.ordinal()];
      n += c;
      if (v == MoveReview.Verdict.BLUNDER) blunders += c;
      else if (v == MoveReview.Verdict.MISTAKE || v == MoveReview.Verdict.MISS) mistakes += c;
      else if ("okay".equalsIgnoreCase(v.label)) okay += c;
    }
    double accuracy = accuracy(white, whiteFirst);
    String acc = Double.isNaN(accuracy) ? "" : String.format(Locale.US, "Accuracy %.0f%%  ·  ", accuracy);
    if (n < 8) return acc + "Too few moves to guess a rating";
    double elo = 2400 - 6000.0 * blunders / n - 3000.0 * mistakes / n - 1000.0 * okay / n;
    elo = Math.max(400, Math.min(2500, elo));
    long rounded = Math.round(elo / 50) * 50;
    return acc + "Plays like ≈ " + rounded + " (rough guess)";
  }

  /** Lichess-style accuracy (0..100) over one side's moves, from the graph scores; NaN if none. */
  private double accuracy(boolean white, boolean whiteFirst) {
    double sum = 0;
    int n = 0;
    for (int i = 1; i < graphScores.length; i++) {
      boolean whiteMoved = whiteFirst == (i % 2 == 1);
      if (whiteMoved != white) continue;
      double before = graphScores[i - 1], after = graphScores[i];
      if (Double.isNaN(before) || Double.isNaN(after)) continue;
      double sign = white ? 1 : -1;
      double lost = Math.max(0, 100 * (winChance(sign * before) - winChance(sign * after)));
      double a = 103.1668 * Math.exp(-0.04354 * lost) - 3.1669;
      sum += Math.max(0, Math.min(100, a));
      n++;
    }
    return n == 0 ? Double.NaN : sum / n;
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
      b.setOnClickListener(v -> retryKeyMoment(k));
      keyMomentsList.addView(b);
    }
    trainButton.setVisibility(keyMoments.isEmpty() ? View.GONE : View.VISIBLE);
    trainButton.setEnabled(true);
    trainButton.setText("TRAIN");
  }

  /**
   * Jumps to the position just before the key moment's move, with the engine lines hidden, so you
   * get another go at finding the better move. Playing the game's move (or ▶) shows the verdict as
   * before; any other move becomes an exploration line with its evaluation.
   */
  private void retryKeyMoment(Training.KeyMoment k) {
    if (k.ply < 1) {
      show(k.ply);
      return;
    }
    show(k.ply - 1, true);
    verdictText.setText("Your move here: " + k.review.headline());
    verdictText.setTextColor(k.review.verdict.colour);
    explanationText.setText("Can you find something better? Tap a piece to try a move, or ▶ to see what was played.");
    android.widget.ScrollView scroll = findViewById(R.id.analysisScroll);
    if (scroll != null) scroll.smoothScrollTo(0, 0); // the key moments are at the bottom; the board is up top
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
    final Position pos = displayed();
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
