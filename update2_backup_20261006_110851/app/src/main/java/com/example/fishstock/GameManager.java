package com.example.fishstock;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.agents.Agent;
import com.example.fishstock.agents.AgentFactory;
import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.agents.Human;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.GameResult;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.Evaluator;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.ui.AgentStore;
import com.example.fishstock.ui.BoardRenderer;
import com.example.fishstock.ui.GameOverDialog;
import com.example.fishstock.ui.PromotionDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The game screen. Holds a {@link Game} (the rules) and two {@link Agent}s (the players) and
 * keeps the board, counters and messages in step with them.
 *
 * Agents think on a background thread, so the screen stays responsive and the board is
 * always drawn before the "game over" box appears.
 */
public class GameManager extends AppCompatActivity {
  public static final String EXTRA_AGENT_TYPE = "agentType";
  public static final String EXTRA_CUSTOM_AGENT = "customAgent";
  public static final String EXTRA_IS_WHITE = "isWhite";
  public static final String EXTRA_HARD_MODE = "isHardMode";
  /** Set to an agent name to watch agent vs agent (this agent plays White). */
  public static final String EXTRA_PLAYER1_TYPE = "player1Type";
  /** Human vs human: turn the board to face the side to move after each move (default true). */
  public static final String EXTRA_AUTO_FLIP = "autoFlip";

  /** Agents accept a draw offer when they think they are at least this many pawns worse. */
  private static final double ACCEPT_DRAW_BELOW = -1.5;
  /** Fast agents still wait this long so you can see their move coming. */
  private static final long MIN_AGENT_MOVE_MS = 600;
  private static final long FIRST_MOVE_DELAY_MS = 1000;

  private enum Mode { HUMAN_VS_AGENT, HUMAN_VS_HUMAN, AGENT_VS_AGENT }

  // ---- game state
  private final Game game = new Game();
  private Agent whitePlayer;
  private Agent blackPlayer;
  private Mode mode;
  private boolean humanIsWhite;
  private boolean hardMode;
  private String opponentName;
  private AgentSpec opponentSpec;

  // ---- interaction state
  private int viewPly;              // which position is on screen (arrows can step back)
  private int selectedSquare = -1;
  private boolean undoAvailable;
  private boolean autoFlip = true;  // human vs human: turn the board after each move (long-press FLIP to toggle)
  private boolean gameOverShown;

  // ---- thinking
  private final ExecutorService thinker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private AtomicBoolean currentStop;
  private boolean thinking;
  private boolean destroyed;

  // ---- views
  private BoardRenderer board;
  private TextView messageText;
  private TextView checkStatusTop;
  private TextView checkStatusBottom;
  private TextView topScore;
  private TextView bottomScore;
  private Button undoButton;
  private Button leftArrow;
  private Button rightArrow;

  private static final int[] COUNTER_TYPES = {Piece.PAWN, Piece.KNIGHT, Piece.BISHOP, Piece.ROOK, Piece.QUEEN};
  private TextView[] topCounters;
  private TextView[] bottomCounters;
  private ImageView[] topIcons;
  private ImageView[] bottomIcons;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);
    readPlayers();
    bindViews();
    board.setFlipped(mode == Mode.HUMAN_VS_AGENT && !humanIsWhite);
    board.setOnSquareClickListener(this::onSquareClicked);
    refresh();
    if (currentPlayer().isHuman()) return;
    main.postDelayed(new Runnable() {
      @Override
      public void run() {
        startAgentTurn();
      }
    }, FIRST_MOVE_DELAY_MS);
  }

  @Override
  protected void onDestroy() {
    destroyed = true;
    cancelThinking();
    thinker.shutdownNow();
    main.removeCallbacksAndMessages(null);
    super.onDestroy();
  }

  // ================================================================ setup

  private void readPlayers() {
    Intent intent = getIntent();
    hardMode = intent.getBooleanExtra(EXTRA_HARD_MODE, false);
    autoFlip = intent.getBooleanExtra(EXTRA_AUTO_FLIP, true);
    humanIsWhite = intent.getBooleanExtra(EXTRA_IS_WHITE, true);
    String player1 = intent.getStringExtra(EXTRA_PLAYER1_TYPE);
    String agentType = intent.getStringExtra(EXTRA_AGENT_TYPE);
    String custom = intent.getStringExtra(EXTRA_CUSTOM_AGENT);

    opponentSpec = custom != null ? AgentSpec.parse(custom) : AgentSpec.byName(agentType);
    opponentName = opponentSpec.name;
    Agent opponent = AgentFactory.create(opponentSpec, true);

    if (player1 != null && !player1.equals("Human")) {
      mode = Mode.AGENT_VS_AGENT;
      whitePlayer = AgentFactory.create(AgentSpec.byName(player1), true);
      blackPlayer = opponent;
      humanIsWhite = true;
    } else if (opponent.isHuman()) {
      mode = Mode.HUMAN_VS_HUMAN;
      whitePlayer = new Human();
      blackPlayer = new Human();
      humanIsWhite = true;
    } else {
      mode = Mode.HUMAN_VS_AGENT;
      whitePlayer = humanIsWhite ? new Human() : opponent;
      blackPlayer = humanIsWhite ? opponent : new Human();
    }
  }

  private void bindViews() {
    board = new BoardRenderer(this);
    messageText = findViewById(R.id.welcomeMessage);
    checkStatusTop = findViewById(R.id.checkStatusTop);
    checkStatusBottom = findViewById(R.id.checkStatusBottom);
    topScore = findViewById(R.id.topScore);
    bottomScore = findViewById(R.id.BottomScore);

    topCounters = new TextView[] {find(R.id.numCapturedTopPawns), find(R.id.numCapturedTopKnights),
        find(R.id.numCapturedTopBishops), find(R.id.numCapturedTopRooks), find(R.id.numCapturedTopQueens)};
    bottomCounters = new TextView[] {find(R.id.numCapturedBottomPawns), find(R.id.numCapturedBottomKnights),
        find(R.id.numCapturedBottomBishops), find(R.id.numCapturedBottomRooks), find(R.id.numCapturedBottomQueens)};
    topIcons = new ImageView[] {findViewById(R.id.topCapturedPawns), findViewById(R.id.topCapturedKnights),
        findViewById(R.id.topCapturedBishops), findViewById(R.id.topCapturedRooks), findViewById(R.id.topCapturedQueens)};
    bottomIcons = new ImageView[] {findViewById(R.id.bottomCapturedPawns), findViewById(R.id.bottomCapturedKnights),
        findViewById(R.id.bottomCapturedBishops), findViewById(R.id.bottomCapturedRooks), findViewById(R.id.bottomCapturedQueens)};

    TextView player1Name = findViewById(R.id.player1);
    TextView player2Name = findViewById(R.id.player2);
    switch (mode) {
      case AGENT_VS_AGENT:
        player1Name.setText(whitePlayer.name());
        player2Name.setText(blackPlayer.name());
        break;
      case HUMAN_VS_HUMAN:
        player1Name.setText("Player1");
        player2Name.setText("Player2");
        break;
      default:
        player1Name.setText("Player");
        player2Name.setText(opponentName);
    }

    Button resign = findViewById(R.id.resign);
    Button draw = findViewById(R.id.draw);
    Button flip = findViewById(R.id.flipBoard);
    undoButton = findViewById(R.id.undo);
    leftArrow = findViewById(R.id.leftarrow);
    rightArrow = findViewById(R.id.rightarrow);

    resign.setOnClickListener(v -> onResign());
    draw.setOnClickListener(v -> onDrawOffer());
    undoButton.setOnClickListener(v -> onUndo());
    flip.setOnClickListener(v -> onFlip());
    flip.setOnLongClickListener(v -> {
      if (mode != Mode.HUMAN_VS_HUMAN) return false;
      autoFlip = !autoFlip;
      if (autoFlip) {
        board.setFlipped(!game.whiteToMove());
        refresh();
      }
      Toast.makeText(this, autoFlip ? "Board turns after each move" : "Board stays put", Toast.LENGTH_SHORT).show();
      return true;
    });
    leftArrow.setOnClickListener(v -> stepView(-1));
    rightArrow.setOnClickListener(v -> stepView(+1));
    if (mode == Mode.AGENT_VS_AGENT) {
      flip.setVisibility(View.GONE);
      draw.setVisibility(View.GONE);
    }
  }

  private TextView find(int id) {
    return findViewById(id);
  }

  // ================================================================ turn handling

  private Agent currentPlayer() {
    return game.whiteToMove() ? whitePlayer : blackPlayer;
  }

  private void onSquareClicked(int sq) {
    if (game.isOver() || thinking || !currentPlayer().isHuman()) return;
    if (viewPly != game.plyCount()) {
      // Looking at an old position: a tap brings you back to the game.
      viewPly = game.plyCount();
      refresh();
      return;
    }
    Position pos = game.position();
    if (selectedSquare >= 0) {
      List<Move> targets = MoveGenerator.legalMovesFrom(pos, selectedSquare);
      for (Move m : targets) {
        if (m.to == sq) {
          final int from = selectedSquare;
          if (game.isPromotionMove(from, sq)) {
            askPromotion(from, sq);
          } else {
            playHumanMove(m);
          }
          return;
        }
      }
    }
    int piece = pos.pieceAt(sq);
    if (sq != selectedSquare && Piece.isColor(piece, pos.whiteToMove())
        && !MoveGenerator.legalMovesFrom(pos, sq).isEmpty()) {
      selectedSquare = sq;
    } else {
      selectedSquare = -1;
    }
    drawBoard();
  }

  private void askPromotion(final int from, final int to) {
    new PromotionDialog(this, game.whiteToMove(), type -> {
      if (type == 0) {
        selectedSquare = -1;
        drawBoard();
        return;
      }
      Move m = game.findMove(from, to, type);
      if (m != null) playHumanMove(m);
    }).show();
  }

  private void playHumanMove(Move m) {
    if (!hardMode && mode != Mode.AGENT_VS_AGENT) undoAvailable = true;
    playMove(m);
  }

  /** Plays a move, animates it, then hands the turn to the next player. */
  private void playMove(Move m) {
    Position before = game.position().copy();
    game.play(m);
    viewPly = game.plyCount();
    selectedSquare = -1;
    updateLabels();
    board.animateMove(before, m, game.position(), this::afterMove);
  }

  private void afterMove() {
    if (destroyed) return;
    refresh();
    if (game.isOver()) {
      showGameOver();
      return;
    }
    if (!currentPlayer().isHuman()) {
      startAgentTurn();
    } else if (mode == Mode.HUMAN_VS_HUMAN && autoFlip) {
      board.setFlipped(!game.whiteToMove());
      refresh();
    }
  }

  private void startAgentTurn() {
    if (destroyed || game.isOver() || thinking) return;
    final Agent agent = currentPlayer();
    if (agent.isHuman()) return;
    thinking = true;
    final AtomicBoolean stop = new AtomicBoolean(false);
    currentStop = stop;
    final Position snapshot = game.position().copy();
    final List<Long> history = new ArrayList<>(game.positionHistory());
    final long started = SystemClock.uptimeMillis();
    updateLabels();
    thinker.execute(() -> {
      Move chosen;
      try {
        chosen = agent.chooseMove(snapshot, history, stop);
      } catch (RuntimeException e) {
        chosen = null;
      }
      final Move move = chosen;
      long wait = Math.max(0, MIN_AGENT_MOVE_MS - (SystemClock.uptimeMillis() - started));
      main.postDelayed(() -> {
        if (destroyed || stop.get()) return;
        thinking = false;
        Move legal = move == null ? null : game.findMove(move.from, move.to, move.promotion);
        if (legal == null) {
          // Should never happen; fall back to any legal move rather than freezing the game.
          List<Move> moves = game.legalMoves();
          if (moves.isEmpty()) return;
          legal = moves.get(0);
        }
        playMove(legal);
      }, wait);
    });
  }

  private void cancelThinking() {
    if (currentStop != null) currentStop.set(true);
    thinking = false;
  }

  // ================================================================ buttons

  private void onUndo() {
    if (hardMode || !undoAvailable || mode == Mode.AGENT_VS_AGENT || game.isOver()) return;
    if (game.plyCount() == 0) return;
    if (mode == Mode.HUMAN_VS_AGENT) {
      boolean humanToMove = currentPlayer().isHuman();
      if (!humanToMove) {
        // The agent is about to reply (or thinking): take back just the human's move.
        cancelThinking();
        board.cancelAnimation();
        game.undo();
      } else if (game.plyCount() >= 2) {
        board.cancelAnimation();
        game.undo();                  // the agent's reply
        game.undo();                  // and the human's move
      } else {
        return;
      }
    } else {
      board.cancelAnimation();
      game.undo();
      if (autoFlip) board.setFlipped(!game.whiteToMove());
    }
    undoAvailable = false;            // one undo per move you make
    viewPly = game.plyCount();
    selectedSquare = -1;
    refresh();
  }

  private void onFlip() {
    board.setFlipped(!board.isFlipped());
    refresh();
  }

  private void onDrawOffer() {
    if (game.isOver()) return;
    if (mode == Mode.HUMAN_VS_HUMAN) {
      endGame(GameResult.DRAW_AGREED);
      return;
    }
    Agent agent = humanIsWhite ? blackPlayer : whitePlayer;
    Evaluator ev = agent.evaluator() != null ? agent.evaluator() : WeightedEvaluator.withDefaults();
    double agentView = ev.evaluateFor(game.position(), !humanIsWhite);
    if (agentView < ACCEPT_DRAW_BELOW) {
      messageText.setText("ACCEPTED");
      endGame(GameResult.DRAW_AGREED);
    } else {
      messageText.setText("DECLINED");
    }
  }

  private void onResign() {
    if (mode == Mode.AGENT_VS_AGENT || game.isOver()) {
      goToMainMenu();
      return;
    }
    boolean whiteResigns = mode == Mode.HUMAN_VS_HUMAN ? game.whiteToMove() : humanIsWhite;
    endGame(whiteResigns ? GameResult.WHITE_RESIGNS : GameResult.BLACK_RESIGNS);
  }

  private void endGame(GameResult result) {
    cancelThinking();
    board.cancelAnimation();
    game.end(result);
    viewPly = game.plyCount();
    refresh();
    showGameOver();
  }

  private void stepView(int delta) {
    int target = viewPly + delta;
    if (target < 0 || target > game.plyCount()) return;
    viewPly = target;
    selectedSquare = -1;
    refresh();
  }

  // ================================================================ game over

  private void showGameOver() {
    if (gameOverShown) return;
    gameOverShown = true;
    final GameResult r = game.result();
    if (mode == Mode.HUMAN_VS_AGENT) new AgentStore(this).recordResult(opponentName, r.scoreFor(humanIsWhite));
    // Let the final position appear on screen before the box covers it.
    main.postDelayed(() -> {
      if (destroyed) return;
      new GameOverDialog(GameManager.this, headline(r), new GameOverDialog.Listener() {
        @Override
        public void onExit() {
          goToMainMenu();
        }

        @Override
        public void onAnalyze() {
          openAnalysis();
        }

        @Override
        public void onPlayAgain() {
          playAgain();
        }
      }).show();
    }, 700);
  }

  private String headline(GameResult r) {
    if (r.isDraw()) return "DRAW :|";
    if (mode == Mode.HUMAN_VS_AGENT) return r.scoreFor(humanIsWhite) > 0 ? "YOU WIN!! :)" : "YOU LOSE  :(";
    return r.whiteWon() ? "WHITE WINS!!" : "BLACK WINS!!";
  }

  private void goToMainMenu() {
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    finish();
  }

  private void playAgain() {
    Intent intent = newGameIntent(this, opponentSpec, !humanIsWhite, hardMode);
    intent.putExtra(EXTRA_AUTO_FLIP, autoFlip);
    startActivity(intent);
    finish();
  }

  private void openAnalysis() {
    StringBuilder uci = new StringBuilder();
    for (Move m : game.moves()) uci.append(m.toUci()).append(' ');
    Intent intent = new Intent(this, GameAnalysis.class);
    intent.putExtra(GameAnalysis.EXTRA_START_FEN, game.startFen());
    intent.putExtra(GameAnalysis.EXTRA_MOVES, uci.toString().trim());
    startActivity(intent);
  }

  /** Intent that starts a game against the given opponent. */
  public static Intent newGameIntent(Context context, AgentSpec opponent, boolean playAsWhite, boolean hardMode) {
    Intent intent = new Intent(context, GameManager.class);
    if (opponent.kind == AgentSpec.Kind.CUSTOM) {
      intent.putExtra(EXTRA_AGENT_TYPE, "Custom");
      intent.putExtra(EXTRA_CUSTOM_AGENT, opponent.serialize());
    } else {
      intent.putExtra(EXTRA_AGENT_TYPE, opponent.name);
    }
    intent.putExtra(EXTRA_IS_WHITE, opponent.kind == AgentSpec.Kind.HUMAN || playAsWhite);
    intent.putExtra(EXTRA_HARD_MODE, hardMode);
    return intent;
  }

  // ================================================================ drawing

  /** Redraws the board and every label for the position being viewed. */
  private void refresh() {
    drawBoard();
    updateLabels();
  }

  private void drawBoard() {
    boolean live = viewPly == game.plyCount();
    Position pos = live ? game.position() : game.positionAt(viewPly);
    board.draw(pos);
    if (live && selectedSquare >= 0) {
      board.highlight(pos, selectedSquare, MoveGenerator.legalMovesFrom(pos, selectedSquare));
    }
  }

  private void updateLabels() {
    boolean live = viewPly == game.plyCount();
    Position pos = live ? game.position() : game.positionAt(viewPly);

    // Message line
    if (live && game.isOver()) messageText.setText(game.result().message);
    else messageText.setText(pos.whiteToMove() ? "WHITE TO MOVE" : "BLACK TO MOVE");

    // Who sits where: the bottom of the screen is White unless the board is flipped.
    boolean bottomIsWhite = !board.isFlipped();

    // Check labels
    String check = "";
    if (pos.inCheck()) check = pos.checkerCount() > 1 ? "DOUBLE CHECK!!" : "CHECK!";
    boolean checkedIsBottom = pos.whiteToMove() == bottomIsWhite;
    checkStatusBottom.setText(checkedIsBottom ? check : "");
    checkStatusTop.setText(checkedIsBottom ? "" : check);

    // Material score from each side's point of view
    int balance = game.materialBalance(viewPly);
    bottomScore.setText(String.valueOf(bottomIsWhite ? balance : -balance));
    topScore.setText(String.valueOf(bottomIsWhite ? -balance : balance));

    // Captured pieces: each player's row shows what they have taken.
    int[][] captured = game.capturedCounts(viewPly); // [0] taken by White, [1] taken by Black
    int[] bottomTook = captured[bottomIsWhite ? 0 : 1];
    int[] topTook = captured[bottomIsWhite ? 1 : 0];
    for (int i = 0; i < COUNTER_TYPES.length; i++) {
      int type = COUNTER_TYPES[i];
      bottomCounters[i].setText(String.valueOf(bottomTook[type]));
      topCounters[i].setText(String.valueOf(topTook[type]));
      bottomIcons[i].setImageResource(BoardRenderer.plainPieceDrawable(Piece.make(type, !bottomIsWhite)));
      topIcons[i].setImageResource(BoardRenderer.plainPieceDrawable(Piece.make(type, bottomIsWhite)));
    }

    // Buttons
    boolean canUndo = !hardMode && undoAvailable && mode != Mode.AGENT_VS_AGENT && !game.isOver();
    undoButton.setEnabled(canUndo);
    undoButton.setAlpha(canUndo ? 1.0f : 0.5f);
    leftArrow.setEnabled(viewPly > 0);
    leftArrow.setAlpha(viewPly > 0 ? 1.0f : 0.5f);
    rightArrow.setEnabled(viewPly < game.plyCount());
    rightArrow.setAlpha(viewPly < game.plyCount() ? 1.0f : 0.5f);
  }
}
