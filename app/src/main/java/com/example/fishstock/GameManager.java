package com.example.fishstock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.fishstock.Agents.*;
import com.example.fishstock.Pieces.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GameManager extends AppCompatActivity
    implements PromotionDialog.OnPromotionMoveListener, GameOverDialog.OnGameOverMoveListener {

  // Game state
  private Game game;
  private Board board;
  private Agent player1;
  private Agent adversary;
  private Piece selectedPiece;
  private boolean isWhite;
  private boolean boardFlipped = false; // Tracks current board orientation

  // Move tracking
  private ArrayList<Piece> capturedPiecesWhite = new ArrayList<>();
  private ArrayList<Piece> capturedPiecesBlack = new ArrayList<>();
  private ArrayList<Move> blacksPotentialMoves = new ArrayList<>();
  private ArrayList<Move> whitesPotentialMoves = new ArrayList<>();

  // UI elements
  private TextView messageText;
  private TextView checkStatusBlack;
  private TextView checkStatusWhite;
  private TextView whiteScore;
  private TextView blackScore;
  private Button flipBoardButton;

  // Captured piece counters
  private Map<String, TextView> whiteCapturedCounters = new HashMap<>();
  private Map<String, TextView> blackCapturedCounters = new HashMap<>();

  // Piece values for scoring
  private static final Map<String, Integer> PIECE_VALUES = new HashMap<String, Integer>() {{
    put("Pawn", 1);
    put("Knight", 3);
    put("Bishop", 3);
    put("Rook", 5);
    put("Queen", 9);
  }};

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);

    initializeGame();
    initializeUI();
    setupButtonListeners();

    // If player is black and adversary is not human, show initial position then make adversary move
    if (!isWhite && !adversary.getName().equals("Human")) {
      // Show initial board state
      updateBoard(board, isWhite);

      // Wait 1 second then make adversary's first move
      new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
        @Override
        public void run() {
          makeAdversaryMove();
        }
      }, 1000);
    } else if (bothAgents()) {
      // Agent vs Agent game - start the game loop
      updateBoard(board, isWhite);
      new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
        @Override
        public void run() {
          startAgentVsAgentGame();
        }
      }, 1000);
    }

    setupBoardClickListeners();
  }

  /**
   * Checks if both players are agents (not human)
   */
  private boolean bothAgents() {
    return !player1.getName().equals("Human") && !adversary.getName().equals("Human");
  }

  /**
   * Starts the agent vs agent game loop
   */
  private void startAgentVsAgentGame() {
    if (game.isGameOver) {
      return;
    }

    try {
      // Determine whose turn it is
      boolean whiteTurn = game.whitesMovesLog.size() == game.blacksMovesLog.size();
      Agent currentAgent = whiteTurn ?
          (isWhite ? player1 : adversary) :
          (isWhite ? adversary : player1);

      ArrayList<Move> currentMoves = whiteTurn ? whitesPotentialMoves : blacksPotentialMoves;
      ArrayList<Move> opponentMoves = whiteTurn ? blacksPotentialMoves : whitesPotentialMoves;

      Move agentMove = currentAgent.getMove(board, currentMoves, opponentMoves);

      // Handle capture
      if (agentMove.isCapture) {
        updateCaptureUI(agentMove.capturablePiece, whiteTurn);
      }

      GameService.makeMove(board, agentMove, whiteTurn);
      GameService.updateBoardMeta(board);

      // Log move
      if (whiteTurn) {
        game.whitesMovesLog.add(agentMove);
      } else {
        game.blacksMovesLog.add(agentMove);
      }

      game.boardStates.add(GameService.copyBoard(board));
      updateBoard(board, boardFlipped);

      // Check for game over
      if (postMoveChecks(board, whiteTurn)) {
        game.isGameOver = true;
        return;
      }

      messageText.setText(whiteTurn ? "BLACK TO MOVE" : "WHITE TO MOVE");

      // Continue the game loop
      new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
        @Override
        public void run() {
          startAgentVsAgentGame();
        }
      }, 1000);

    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Initializes the game board and agents.
   */
  private void initializeGame() {
    this.board = new Board();
    GameService.updateBoardMeta(board);

    try {
      whitesPotentialMoves = GameService.generateMoves(board, true);
      blacksPotentialMoves = GameService.generateMoves(board, false);
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    this.isWhite = getIntent().getBooleanExtra("isWhite", false);
    String player1Type = getIntent().getStringExtra("player1Type");
    String adversaryType = getIntent().getStringExtra("agentType");

    // Handle agent vs agent
    if (player1Type != null && !player1Type.equals("Human")) {
      this.player1 = initializeAgent(player1Type, true);
      this.adversary = initializeAgent(adversaryType, false);
    } else {
      this.adversary = initializeAgent(adversaryType, !isWhite);
      this.player1 = new Human(AgentType.HUMAN, isWhite);
    }

    if (isWhite) {
      this.game = new Game(board, player1.type, adversary.type);
    } else {
      this.game = new Game(board, adversary.type, player1.type);
    }

    this.game.boardStates.add(GameService.copyBoard(this.board));
    this.boardFlipped = !isWhite;
    updateBoard(board, boardFlipped);
  }

  /**
   * Initializes all UI elements and sets up captured piece displays.
   */
  private void initializeUI() {
    messageText = findViewById(R.id.welcomeMessage);

    // Setup player names
    TextView adversaryName = findViewById(R.id.player2);
    TextView playerName = findViewById(R.id.player1);

    if (bothAgents()) {
      playerName.setText(player1.getName());
      adversaryName.setText(adversary.getName());
    } else if (adversary.getName().equals("Human")) {
      adversaryName.setText("Player2");
      playerName.setText("Player1");
    } else {
      adversaryName.setText(adversary.getName());
      playerName.setText("Player");
    }

    // Setup score and check status displays based on perspective
    setupPerspectiveBasedUI();

    // Setup captured piece counters
    setupCapturedPieceCounters();
  }

  /**
   * Sets up UI elements based on player perspective (white or black).
   */
  private void setupPerspectiveBasedUI() {
    if (!boardFlipped) {
      checkStatusBlack = findViewById(R.id.checkStatusTop);
      checkStatusWhite = findViewById(R.id.checkStatusBottom);
      whiteScore = findViewById(R.id.BottomScore);
      blackScore = findViewById(R.id.topScore);
    } else {
      // Flip the perspective for black player
      checkStatusBlack = findViewById(R.id.checkStatusBottom);
      checkStatusWhite = findViewById(R.id.checkStatusTop);
      whiteScore = findViewById(R.id.topScore);
      blackScore = findViewById(R.id.BottomScore);

      // Update captured piece images
      updateCapturedPieceImages();
    }
  }

  /**
   * Updates captured piece icons when playing from black's perspective.
   */
  private void updateCapturedPieceImages() {
    // Black pieces on top
    ((ImageView) findViewById(R.id.topCapturedPawns)).setImageResource(R.drawable.black_pawn);
    ((ImageView) findViewById(R.id.topCapturedBishops)).setImageResource(R.drawable.black_bishop);
    ((ImageView) findViewById(R.id.topCapturedKnights)).setImageResource(R.drawable.black_knight);
    ((ImageView) findViewById(R.id.topCapturedRooks)).setImageResource(R.drawable.black_rook);
    ((ImageView) findViewById(R.id.topCapturedQueens)).setImageResource(R.drawable.black_queen);

    // White pieces on bottom
    ((ImageView) findViewById(R.id.bottomCapturedPawns)).setImageResource(R.drawable.white_pawn);
    ((ImageView) findViewById(R.id.bottomCapturedBishops)).setImageResource(R.drawable.white_bishop);
    ((ImageView) findViewById(R.id.bottomCapturedKnights)).setImageResource(R.drawable.white_knight);
    ((ImageView) findViewById(R.id.bottomCapturedRooks)).setImageResource(R.drawable.whie_rook);
    ((ImageView) findViewById(R.id.bottomCapturedQueens)).setImageResource(R.drawable.white_queen);
  }

  /**
   * Sets up maps for captured piece counters based on perspective.
   */
  private void setupCapturedPieceCounters() {
    String topPrefix = !boardFlipped ? "numCapturedTop" : "numCapturedBottom";
    String bottomPrefix = !boardFlipped ? "numCapturedBottom" : "numCapturedTop";

    // Black captured pieces (displayed on top for white, bottom for black)
    blackCapturedCounters.put("Pawn", findViewById(getResources().getIdentifier(bottomPrefix + "Pawns", "id", getPackageName())));
    blackCapturedCounters.put("Rook", findViewById(getResources().getIdentifier(bottomPrefix + "Rooks", "id", getPackageName())));
    blackCapturedCounters.put("Knight", findViewById(getResources().getIdentifier(bottomPrefix + "Knights", "id", getPackageName())));
    blackCapturedCounters.put("Bishop", findViewById(getResources().getIdentifier(bottomPrefix + "Bishops", "id", getPackageName())));
    blackCapturedCounters.put("Queen", findViewById(getResources().getIdentifier(bottomPrefix + "Queens", "id", getPackageName())));

    // White captured pieces
    whiteCapturedCounters.put("Pawn", findViewById(getResources().getIdentifier(topPrefix + "Pawns", "id", getPackageName())));
    whiteCapturedCounters.put("Rook", findViewById(getResources().getIdentifier(topPrefix + "Rooks", "id", getPackageName())));
    whiteCapturedCounters.put("Knight", findViewById(getResources().getIdentifier(topPrefix + "Knights", "id", getPackageName())));
    whiteCapturedCounters.put("Bishop", findViewById(getResources().getIdentifier(topPrefix + "Bishops", "id", getPackageName())));
    whiteCapturedCounters.put("Queen", findViewById(getResources().getIdentifier(topPrefix + "Queens", "id", getPackageName())));
  }

  /**
   * Sets up button click listeners for game controls.
   */
  private void setupButtonListeners() {
    Button resign = findViewById(R.id.resign);
    Button undo = findViewById(R.id.undo);
    Button draw = findViewById(R.id.draw);
    flipBoardButton = findViewById(R.id.flipBoard);

    resign.setOnClickListener(v -> {
      Intent intent = new Intent(GameManager.this, MainActivity.class);
      startActivity(intent);
    });

    undo.setOnClickListener(v -> {
      board = game.getPreviousBoard();
      GameService.updateBoardMeta(board);
      updateBoard(board, boardFlipped);
    });

    draw.setOnClickListener(v -> handleDrawOffer());

    flipBoardButton.setOnClickListener(v -> flipBoard());

    // Hide flip button for agent vs agent games
    if (bothAgents()) {
      flipBoardButton.setVisibility(View.GONE);
    }
  }

  /**
   * Flips the board orientation
   */
  private void flipBoard() {
    boardFlipped = !boardFlipped;

    // Clear any selected piece
    if (selectedPiece != null) {
      clearPieceHighlights();
      selectedPiece = null;
    }

    // Re-setup UI elements for new orientation
    setupPerspectiveBasedUI();
    setupCapturedPieceCounters();

    // Update captured piece images if needed
    if (boardFlipped) {
      updateCapturedPieceImages();
    } else {
      // Reset to normal orientation images
      ((ImageView) findViewById(R.id.topCapturedPawns)).setImageResource(R.drawable.black_pawn);
      ((ImageView) findViewById(R.id.topCapturedBishops)).setImageResource(R.drawable.black_bishop);
      ((ImageView) findViewById(R.id.topCapturedKnights)).setImageResource(R.drawable.black_knight);
      ((ImageView) findViewById(R.id.topCapturedRooks)).setImageResource(R.drawable.black_rook);
      ((ImageView) findViewById(R.id.topCapturedQueens)).setImageResource(R.drawable.black_queen);

      ((ImageView) findViewById(R.id.bottomCapturedPawns)).setImageResource(R.drawable.white_pawn);
      ((ImageView) findViewById(R.id.bottomCapturedBishops)).setImageResource(R.drawable.white_bishop);
      ((ImageView) findViewById(R.id.bottomCapturedKnights)).setImageResource(R.drawable.white_knight);
      ((ImageView) findViewById(R.id.bottomCapturedRooks)).setImageResource(R.drawable.whie_rook);
      ((ImageView) findViewById(R.id.bottomCapturedQueens)).setImageResource(R.drawable.white_queen);
    }

    // Redraw the board
    updateBoard(board, boardFlipped);

    // Update captured counters
    updateAllCapturedCounters();
  }

  /**
   * Updates all captured piece counters after board flip
   */
  private void updateAllCapturedCounters() {
    // Reset all counters to 0
    for (TextView counter : whiteCapturedCounters.values()) {
      if (counter != null) counter.setText("0");
    }
    for (TextView counter : blackCapturedCounters.values()) {
      if (counter != null) counter.setText("0");
    }

    // Re-count captured pieces
    for (Piece piece : capturedPiecesWhite) {
      TextView counter = whiteCapturedCounters.get(piece.getName());
      if (counter != null) {
        int count = Integer.parseInt(counter.getText().toString());
        counter.setText(String.valueOf(count + 1));
      }
    }

    for (Piece piece : capturedPiecesBlack) {
      TextView counter = blackCapturedCounters.get(piece.getName());
      if (counter != null) {
        int count = Integer.parseInt(counter.getText().toString());
        counter.setText(String.valueOf(count + 1));
      }
    }
  }

  /**
   * Handles draw offer logic.
   */
  private void handleDrawOffer() {
    int playerScore = isWhite ?
        Integer.parseInt(whiteScore.getText().toString()) :
        Integer.parseInt(blackScore.getText().toString());

    if (playerScore >= 0) {
      messageText.setText("DECLINED");
    } else {
      messageText.setText("ACCEPTED");
      GameOverDialog ggDialog = new GameOverDialog(this, 0, isWhite, adversary.getName(), game);
      ggDialog.setOnGameOverListener(this);
      ggDialog.show();
    }
  }

  /**
   * Sets up click listeners for all board squares.
   */
  private void setupBoardClickListeners() {
    // Disable clicks for agent vs agent games
    if (bothAgents()) {
      return;
    }

    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        ImageButton button = (ImageButton) getButtonFromCoord(new Coordinate(col, row), boardFlipped);
        button.setOnClickListener(v -> handleSquareClick(button));
      }
    }
  }

  /**
   * Main handler for board square clicks.
   */
  private void handleSquareClick(ImageButton button) {
    Coordinate coord = getCoordFromButton(button, boardFlipped);
    Cell cell = board.board[coord.rank][coord.file];

    // Case 1: Empty square - making a non-capturing move
    if (cell.PieceStatus == Status.EMPTY) {
      handleEmptySquareClick(coord);
    }
    // Case 2: Opponent's piece - making a capturing move
    else if ((cell.PieceStatus == Status.BLACK && isWhite) ||
        (cell.PieceStatus == Status.WHITE && !isWhite)) {
      handleCaptureSquareClick(coord);
    }
    // Case 3: Own piece - selecting/deselecting
    else {
      handleOwnPieceClick(coord, cell);
    }
  }

  /**
   * Handles clicking on an empty square.
   */
  private void handleEmptySquareClick(Coordinate coord) {
    if (selectedPiece != null && isLegalMove(coord, board)) {
      Move move = new Move(selectedPiece.getPos(), coord, selectedPiece.getName(), false, isWhite);
      executePlayerMove(move);
    }
  }

  /**
   * Handles clicking on an opponent's piece (capture).
   */
  private void handleCaptureSquareClick(Coordinate coord) {
    if (selectedPiece != null && isLegalMove(coord, board)) {
      Piece capturedPiece = board.board[coord.rank][coord.file].piece;
      Move move = new Move(selectedPiece.getPos(), coord, selectedPiece.getName(), true, isWhite);
      move.setCapture(capturedPiece);

      updateCaptureUI(capturedPiece, isWhite);
      executePlayerMove(move);
    }
  }

  /**
   * Handles clicking on player's own piece (select/deselect).
   */
  private void handleOwnPieceClick(Coordinate coord, Cell cell) {
    // Deselect previous piece
    if (selectedPiece != null) {
      clearPieceHighlights();
    }

    // Select new piece if different from current
    if (selectedPiece == null || !cell.piece.equals(selectedPiece)) {
      selectPiece(coord, cell);
    } else {
      selectedPiece = null;
    }
  }

  /**
   * Selects a piece and highlights its legal moves.
   */
  private void selectPiece(Coordinate coord, Cell cell) {
    // Check if piece is pinned
    if (isWhite) {
      int index = Board.getIndex(board.whitePieces, coord);
      if (index >= 0) {
        Piece piece = board.whitePieces.get(index);
        if (piece.isPinned()) {
          return; // Can't select pinned piece
        }
      }
    }

    selectedPiece = cell.piece;

    // Highlight selected piece
    ImageButton pieceButton = (ImageButton) getButtonFromCoord(selectedPiece.getPos(), boardFlipped);
    pieceButton.setColorFilter(Color.YELLOW, PorterDuff.Mode.OVERLAY);

    // Highlight legal moves
    ArrayList<Move> legalMoves = selectedPiece.generateMoves(coord, board.board);
    for (Move move : GameService.filterMoves(legalMoves)) {
      if (isLegalMove(move.toCoord, board)) {
        highlightLegalMove(move);
      }
    }
  }

  /**
   * Highlights a legal move destination square.
   */
  private void highlightLegalMove(Move move) {
    ImageButton button = (ImageButton) getButtonFromCoord(move.toCoord, boardFlipped);
    Cell targetCell = board.board[move.toCoord.rank][move.toCoord.file];

    if (targetCell.PieceStatus == Status.EMPTY) {
      // Highlight empty squares
      if (targetCell.isLight) {
        button.setImageResource(R.drawable.white_empty_selected);
      } else {
        button.setImageResource(R.drawable.black_empty_selected);
      }
    } else if ((isWhite && targetCell.PieceStatus == Status.BLACK) ||
        (!isWhite && targetCell.PieceStatus == Status.WHITE)) {
      // Highlight capturable pieces
      button.setColorFilter(Color.RED, PorterDuff.Mode.OVERLAY);
    }
  }

  /**
   * Clears all piece and move highlights.
   */
  private void clearPieceHighlights() {
    if (selectedPiece == null) return;

    // Clear selected piece highlight
    ImageButton pieceButton = (ImageButton) getButtonFromCoord(selectedPiece.getPos(), boardFlipped);
    pieceButton.setColorFilter(null);

    // Clear move highlights
    for (Move move : GameService.filterMoves(selectedPiece.generateMoves(selectedPiece.getPos(), board.board))) {
      ImageButton button = (ImageButton) getButtonFromCoord(move.toCoord, boardFlipped);
      Cell cell = board.board[move.toCoord.rank][move.toCoord.file];

      if (cell.PieceStatus == Status.EMPTY) {
        button.setImageResource(cell.isLight ? R.drawable.empty_light : R.drawable.empty_dark);
      } else {
        button.setColorFilter(null);
      }
    }
  }

  private void executePlayerMove(Move move) {
    move = updateMove(move);

    if (move.isPromotion) {
      PromotionDialog promotionDialog = new PromotionDialog(this, board, move, isWhite);
      promotionDialog.setOnPromotionMoveListener(this);
      promotionDialog.show();
    } else {
      makeMoveAndContinue(move);
    }
  }

  /**
   * Makes a move and continues the game flow.
   */
  private void makeMoveAndContinue(Move move) {
    GameService.makeMove(board, move, isWhite);
    GameService.updateBoardMeta(board);

    // Log move
    if (isWhite) {
      game.whitesMovesLog.add(move);
    } else {
      game.blacksMovesLog.add(move);
    }

    game.boardStates.add(GameService.copyBoard(board));

    // Check for game over
    if (postMoveChecks(board, isWhite)) {
      return;
    }

    // Update turn message
    messageText.setText(isWhite ? "BLACK TO MOVE" : "WHITE TO MOVE");

    // Adversary's turn
    if (!adversary.getName().equals("Human")) {
      makeAdversaryMove();
    } else {
      // Switch perspective for human vs human
      isWhite = !isWhite;
      flipBoard();
    }
  }

  /**
   * Makes the adversary's move.
   */
  private void makeAdversaryMove() {
    try {
      ArrayList<Move> playersMoves = GameService.generateMoves(board, isWhite);
      Move adversaryMove;

      if (isWhite) {
        adversaryMove = adversary.getMove(board, blacksPotentialMoves, playersMoves);
      } else {
        adversaryMove = adversary.getMove(board, whitesPotentialMoves, playersMoves);
      }

      // Handle capture
      if (adversaryMove.isCapture) {
        updateCaptureUI(adversaryMove.capturablePiece, !isWhite);
      }

      GameService.makeMove(board, adversaryMove, !isWhite);
      GameService.updateBoardMeta(board);

      // Log move
      if (isWhite) {
        game.blacksMovesLog.add(adversaryMove);
      } else {
        game.whitesMovesLog.add(adversaryMove);
      }

      game.boardStates.add(GameService.copyBoard(board));
      updateBoard(board, boardFlipped);
      postMoveChecks(board, !isWhite);

      messageText.setText(isWhite ? "WHITE TO MOVE" : "BLACK TO MOVE");

    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Updates UI when a piece is captured.
   */
  private void updateCaptureUI(Piece capturedPiece, boolean capturedByWhite) {
    String pieceName = capturedPiece.getName();
    int pieceValue = 0;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
      pieceValue = PIECE_VALUES.getOrDefault(pieceName, 0);
    }

    // Add to captured pieces list
    if (capturedByWhite) {
      capturedPiecesBlack.add(capturedPiece);
      incrementCapturedCounter(blackCapturedCounters.get(pieceName));
      updateScores(-pieceValue, pieceValue);
    } else {
      capturedPiecesWhite.add(capturedPiece);
      incrementCapturedCounter(whiteCapturedCounters.get(pieceName));
      updateScores(pieceValue, -pieceValue);
    }
  }

  /**
   * Increments a captured piece counter.
   */
  private void incrementCapturedCounter(TextView counter) {
    if (counter != null) {
      int currentCount = Integer.parseInt(counter.getText().toString());
      counter.setText(String.valueOf(currentCount + 1));
    }
  }

  /**
   * Updates material score displays.
   */
  private void updateScores(int whiteChange, int blackChange) {
    int currentWhiteScore = Integer.parseInt(whiteScore.getText().toString());
    int currentBlackScore = Integer.parseInt(blackScore.getText().toString());

    whiteScore.setText(String.valueOf(currentWhiteScore + whiteChange));
    blackScore.setText(String.valueOf(currentBlackScore + blackChange));
  }

  /**
   * Updates move metadata for special moves (castling, en passant, promotion).
   */
  private Move updateMove(Move move) {
    // Castling
    if (move.piece.getName().equals("King") &&
        Math.abs(move.toCoord.file - move.fromCoord.file) == 2) {
      move.setCastle();
    }
    // Promotion
    else if (move.piece.getName().equals("Pawn") &&
        ((isWhite && move.toCoord.rank == 7) || (!isWhite && move.toCoord.rank == 0))) {
      move.setPromotion(new Queen(move.toCoord, move.piece.getColor()));
    }
    // En passant
    else if (move.piece.getName().equals("Pawn") &&
        Math.abs(move.toCoord.file - move.fromCoord.file) == 1 &&
        board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.EMPTY) {
      move.setEnPassant();
      int captureRank = isWhite ? move.toCoord.rank - 1 : move.toCoord.rank + 1;
      move.setCapture(board.board[captureRank][move.toCoord.file].piece);
    }

    return move;
  }

  /**
   * Performs post-move checks for game-ending conditions.
   * Returns true if game is over.
   */
  private boolean postMoveChecks(Board board, boolean whiteMoved) {
    // Check 1: Insufficient material
    if (GameService.isDeadPosition(board.whitePieces, board.blackPieces)) {
      showGameOver("DRAW BY INSUFFICIENT MATERIAL", 0);
      return true;
    }

    // Check 2: Threefold repetition
    if (GameService.isRepetition(game.boardStates, board)) {
      showGameOver("DRAW BY REPETITION", 0);
      return true;
    }

    // Check 3: Checkmate, stalemate
    if (whiteMoved) {
      return checkBlackStatus();
    } else {
      return checkWhiteStatus();
    }
  }

  /**
   * Checks black's status after white moves.
   */
  private boolean checkBlackStatus() {
    try {
      blacksPotentialMoves = GameService.generateMoves(board, false);
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    King blackKing = (King) board.blackPieces.get(0);

    if (blackKing.isDoubleChecked) {
      checkStatusBlack.setText("DOUBLE CHECK!!");
      blacksPotentialMoves = GameService.generateMovesDoubleCheck(board, blacksPotentialMoves, false);
      if (blacksPotentialMoves.isEmpty()) {
        showGameOver("CHECKMATE!! PLAYER 1 WINS", 1);
        return true;
      }
    } else if (blackKing.isChecked) {
      checkStatusBlack.setText("CHECK!");
      blacksPotentialMoves = GameService.generateMovesCheck(board, blacksPotentialMoves, false);
      if (blacksPotentialMoves.isEmpty()) {
        showGameOver("CHECKMATE!! PLAYER 1 WINS", 1);
        return true;
      }
    } else {
      checkStatusWhite.setText("");
      if (blacksPotentialMoves.isEmpty()) {
        showGameOver("STALEMATE. THE GAME ENDS IN A DRAW", 0);
        return true;
      }
      updateBoard(board, boardFlipped);
      selectedPiece = null;
    }

    updateEndgameStatus();
    return false;
  }

  /**
   * Checks white's status after black moves.
   */
  private boolean checkWhiteStatus() {
    try {
      whitesPotentialMoves = GameService.generateMoves(board, true);
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    King whiteKing = (King) board.whitePieces.get(0);

    if (whiteKing.isDoubleChecked) {
      checkStatusWhite.setText("DOUBLE CHECK!!");
      whitesPotentialMoves = GameService.generateMovesDoubleCheck(board, whitesPotentialMoves, true);
      if (whitesPotentialMoves.isEmpty()) {
        showGameOver("CHECKMATE!! PLAYER 1 LOSES", -1);
        return true;
      }
    } else if (whiteKing.isChecked) {
      checkStatusWhite.setText("CHECK!");
      whitesPotentialMoves = GameService.generateMovesCheck(board, whitesPotentialMoves, true);
      if (whitesPotentialMoves.isEmpty()) {
        showGameOver("CHECKMATE!! PLAYER 1 LOSES", -1);
        return true;
      }
    } else {
      checkStatusBlack.setText("");
      if (whitesPotentialMoves.isEmpty()) {
        showGameOver("STALEMATE. THE GAME ENDS IN A DRAW", 0);
        return true;
      }
      updateBoard(board, boardFlipped);
    }

    updateEndgameStatus();
    return false;
  }

  /**
   * Shows game over dialog.
   */
  private void showGameOver(String message, int result) {
    messageText.setText(message);
    GameOverDialog dialog = new GameOverDialog(this, result, isWhite, adversary.getName(), game);
    dialog.setOnGameOverListener(this);
    dialog.show();
  }

  /**
   * Updates endgame flag if entering endgame.
   */
  private void updateEndgameStatus() {
    if (!game.isEndGame && isEndGame(board)) {
      game.isEndGame = true;
    }
  }

  /**
   * Determines if the position is an endgame.
   */
  public static boolean isEndGame(Board board) {
    // No queens = endgame
    boolean hasWhiteQueen = false;
    boolean hasBlackQueen = false;

    for (Piece piece : board.whitePieces) {
      if (piece.getName().equals("Queen")) {
        hasWhiteQueen = true;
        break;
      }
    }

    for (Piece piece : board.blackPieces) {
      if (piece.getName().equals("Queen")) {
        hasBlackQueen = true;
        break;
      }
    }

    if (!hasWhiteQueen && !hasBlackQueen) {
      return true;
    }

    // Limited minor and major pieces = endgame
    int whiteMinorMajor = Simple.countByType(board.whitePieces, "Rook")
        + Simple.countByType(board.whitePieces, "Bishop")
        + Simple.countByType(board.whitePieces, "Knight");

    int blackMinorMajor = Simple.countByType(board.blackPieces, "Rook")
        + Simple.countByType(board.blackPieces, "Bishop")
        + Simple.countByType(board.blackPieces, "Knight");

    return whiteMinorMajor < 5 || blackMinorMajor < 5;
  }

  /**
   * Creates an agent based on the agent type string.
   */
  public static Agent initializeAgent(String agentName, boolean isWhite) {
    switch (agentName) {
      case "Randy":
        return new Randy(AgentType.RANDY, isWhite);
      case "Simple":
        return new Simple(AgentType.SIMPLE, isWhite);
      case "MinMax":
        return new MinMax(AgentType.MINMAX, isWhite);
      case "FishStock":
        return new FishStock(AgentType.FISHSTOCK, isWhite);
      default:
        return new Human(AgentType.HUMAN, isWhite);
    }
  }

  /**
   * Checks if a move to the given coordinate is legal.
   */
  private boolean isLegalMove(Coordinate coord, Board board) {
    if (selectedPiece == null) {
      return false;
    }

    King ourKing = isWhite ?
        (King) board.whitePieces.get(0) :
        (King) board.blackPieces.get(0);

    ArrayList<Move> legalMoves;

    // Handle check situations
    if (ourKing.isDoubleChecked) {
      legalMoves = isWhite ?
          GameService.generateMovesDoubleCheck(board, whitesPotentialMoves, true) :
          GameService.generateMovesDoubleCheck(board, blacksPotentialMoves, false);
    } else if (ourKing.isChecked) {
      legalMoves = isWhite ?
          GameService.generateMovesCheck(board, whitesPotentialMoves, true) :
          GameService.generateMovesCheck(board, blacksPotentialMoves, false);
    } else {
      legalMoves = GameService.filterMoves(selectedPiece.generateMoves(selectedPiece.getPos(), board.board));
    }

    // Check if the coordinate matches any legal move
    for (Move move : legalMoves) {
      if (Coordinate.compareCoords(move.toCoord, coord) &&
          move.piece.getName().equals(selectedPiece.getName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Converts a coordinate to the corresponding button view.
   */
  private ImageView getButtonFromCoord(Coordinate coord, boolean flipped) {
    int resId;
    if (!flipped) {
      resId = getResources().getIdentifier(
          "button" + (7 - coord.rank) + (7 - coord.file),
          "id",
          getPackageName());
    } else {
      resId = getResources().getIdentifier(
          "button" + coord.rank + coord.file,
          "id",
          getPackageName());
    }
    return findViewById(resId);
  }

  /**
   * Extracts coordinate from a button's resource ID.
   */
  private Coordinate getCoordFromButton(View button, boolean flipped) {
    String buttonId = getResources().getResourceEntryName(button.getId());
    int rank;
    int file;

    if (!flipped) {
      rank = 7 - (buttonId.charAt(6) - '0');
      file = 7 - (buttonId.charAt(7) - '0');
    } else {
      rank = buttonId.charAt(6) - '0';
      file = buttonId.charAt(7) - '0';
    }

    return new Coordinate(file, rank);
  }

  /**
   * Updates the entire board display.
   */
  public void updateBoard(Board board, boolean flipped) {
    if (!flipped) {
      for (int rank = 0; rank < 8; rank++) {
        for (int file = 0; file < 8; file++) {
          updateCellImage(board.board[rank][file], rank, file, flipped);
        }
      }
    } else {
      for (int rank = 7; rank >= 0; rank--) {
        for (int file = 7; file >= 0; file--) {
          updateCellImage(board.board[rank][file], rank, file, flipped);
        }
      }
    }
  }

  /**
   * Updates a single cell's image on the board.
   */
  private void updateCellImage(Cell cell, int rank, int file, boolean flipped) {
    ImageView cellImageView = getButtonFromCoord(new Coordinate(file, rank), flipped);
    cellImageView.setColorFilter(null);

    if (cell.isEmpty) {
      cellImageView.setImageResource(
          cell.isLight ? R.drawable.empty_light : R.drawable.empty_dark);
      return;
    }

    // Get appropriate piece image
    int resourceId = getPieceImageResource(cell);
    cellImageView.setImageResource(resourceId);
  }

  /**
   * Gets the appropriate drawable resource for a piece on a cell.
   */
  private int getPieceImageResource(Cell cell) {
    String color = cell.isWhite ? "white" : "black";
    String square = cell.isLight ? "light" : "dark";
    String piece = getPieceName(cell);

    String resourceName = color + "_" + piece + "_on_" + square;
    return getResources().getIdentifier(resourceName, "drawable", getPackageName());
  }

  /**
   * Gets the piece name from a cell.
   */
  private String getPieceName(Cell cell) {
    if (cell.isKing) return "king";
    if (cell.isQueen) return "queen";
    if (cell.isRook) return "rook";
    if (cell.isBishop) return "bishop";
    if (cell.isKnight) return "knight";
    if (cell.isPawn) return "pawn";
    return "";
  }

  // Promotion dialog callback
  @Override
  public void onPromotionMove() throws CloneNotSupportedException {
    updateBoard(board, boardFlipped);

    // Adversary responds to promotion
    if (!adversary.getName().equals("Human")) {
      ArrayList<Move> blackMoves = GameService.generateMoves(board, false);
      King blackKing = (King) board.blackPieces.get(0);

      if (blackKing.isDoubleChecked) {
        blackMoves = GameService.generateMovesDoubleCheck(board, blackMoves, false);
      } else if (blackKing.isChecked) {
        blackMoves = GameService.generateMovesCheck(board, blackMoves, false);
      }

      Move adversaryMove = adversary.getMove(board, blackMoves, whitesPotentialMoves);
      GameService.makeMove(board, adversaryMove, false);
      GameService.updateBoardMeta(board);

      postMoveChecks(board, false);
    }
  }

  // Game over dialog callback
  @Override
  public void onGameOver() {
    // Handle game over actions if needed
  }
}