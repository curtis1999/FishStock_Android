package com.example.fishstock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.fishstock.Agents.*;
import com.example.fishstock.Pieces.*;

import java.util.ArrayList;
import java.util.List;

public class GameManager extends AppCompatActivity implements PromotionDialog.OnPromotionMoveListener, GameOverDialog.OnGameOverMoveListener {
  Game game;
  Board board;
  Agent player1;
  Agent adversary;
  Piece selectedPiece;
  boolean isWhite = true;
  ArrayList<Piece> capturedPiecesWhite = new ArrayList<>();
  ArrayList<Piece> capturedPiecesBlack = new ArrayList<>();
  ArrayList<Move> blacksPotentialMoves = new ArrayList<>();
  ArrayList<Move> whitesPotentialMoves = new ArrayList<>();

  /**
   * The Game Loop.  Initializes the board and the buttons.
   *
   * @param savedInstanceState
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);
    //1. Initialize the board, agent and game
    this.board = new Board();
    GameService.updateBoardMeta(board);
    try {
      whitesPotentialMoves = GameService.generateMoves(board, true);
      blacksPotentialMoves = GameService.generateMoves(board, false);
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    this.isWhite = getIntent().getBooleanExtra("isWhite", false);
    this.adversary = initializeAgent(getIntent().getStringExtra("agentType"), isWhite);
    this.player1 = new Human(AgentType.HUMAN, true);

    if (isWhite) {
      this.game = new Game(board, player1.type, adversary.type);
    } else {
      this.game = new Game(board, adversary.type, player1.type);
    }
    this.game.boardStates.add(GameService.copyBoard(this.board));
    updateBoard(board, isWhite);

    //2. Initialize the texts and buttons.
    TextView adversaryName = findViewById(R.id.player2);
    adversaryName.setText(adversary.getName());
    Button resign = findViewById(R.id.resign);
    Button undo = findViewById(R.id.undo);
    Button draw = findViewById(R.id.draw);
    ImageButton promotionQueen = findViewById(R.id.promotionQueen);
    ImageButton promotionRook = findViewById(R.id.promotionRook);
    ImageButton promotionBishop = findViewById(R.id.promotionBishop);
    ImageButton promotionKnight = findViewById(R.id.promotionKnight);
    TextView numCapturedBlackPawns = findViewById(R.id.numCapturedBottomPawns);
    TextView numCapturedBlackRooks = findViewById(R.id.numCapturedBottomRooks);
    TextView numCapturedBlackKnights = findViewById(R.id.numCapturedBottomKnights);
    TextView numCapturedBlackBishops = findViewById(R.id.numCapturedBottomBishops);
    TextView numCapturedBlackQueens = findViewById(R.id.numCapturedBottomQueens);
    TextView numCapturedWhitePawns = findViewById(R.id.numCapturedTopPawns);
    TextView numCapturedWhiteRooks = findViewById(R.id.numCapturedTopRooks);
    TextView numCapturedWhiteKnights = findViewById(R.id.numCapturedTopKnights);
    TextView numCapturedWhiteBishops = findViewById(R.id.numCapturedTopBishops);
    TextView numCapturedWhiteQueens = findViewById(R.id.numCapturedTopQueens);
    TextView whiteScore = findViewById(R.id.topScore);
    TextView blackScore = findViewById(R.id.BottomScore);
    TextView checkStatusBlack = findViewById(R.id.checkStatusTop);
    TextView checkStatusWhite = findViewById(R.id.checkStatusBottom);

    if (!isWhite) {
      ImageView capturedBlackPawnView  = findViewById(R.id.topCapturedPawns);
      capturedBlackPawnView.setImageResource(R.drawable.black_pawn);
      ImageView capturedBlackBishopView  = findViewById(R.id.topCapturedBishops);
      capturedBlackBishopView.setImageResource(R.drawable.black_bishop);
      ImageView capturedBlackKnightView  = findViewById(R.id.topCapturedKnights);
      capturedBlackKnightView.setImageResource(R.drawable.black_knight);
      ImageView capturedBlackRookView  = findViewById(R.id.topCapturedRooks);
      capturedBlackRookView.setImageResource(R.drawable.black_rook);
      ImageView capturedBlackQueenView  = findViewById(R.id.topCapturedQueens);
      capturedBlackQueenView.setImageResource(R.drawable.black_queen);

      ImageView capturedWhitePawnView = findViewById(R.id.bottomCapturedPawns);
      capturedWhitePawnView.setImageResource(R.drawable.white_pawn);
      ImageView capturedWhiteBishopView  = findViewById(R.id.bottomCapturedBishops);
      capturedWhiteBishopView.setImageResource(R.drawable.white_bishop);
      ImageView capturedWhiteKnightView  = findViewById(R.id.bottomCapturedKnights);
      capturedWhiteKnightView.setImageResource(R.drawable.white_knight);
      ImageView capturedWhiteRookView  = findViewById(R.id.bottomCapturedRooks);
      capturedWhiteRookView.setImageResource(R.drawable.whie_rook);
      ImageView capturedWhiteQueenView  = findViewById(R.id.bottomCapturedQueens);
      capturedWhiteQueenView.setImageResource(R.drawable.white_queen);

      numCapturedBlackPawns = findViewById(R.id.numCapturedTopPawns);
      numCapturedBlackRooks = findViewById(R.id.numCapturedTopRooks);
      numCapturedBlackKnights = findViewById(R.id.numCapturedTopKnights);
      numCapturedBlackBishops = findViewById(R.id.numCapturedTopBishops);
      numCapturedBlackQueens = findViewById(R.id.numCapturedTopQueens);
      numCapturedWhitePawns = findViewById(R.id.numCapturedBottomPawns);
      numCapturedWhiteRooks = findViewById(R.id.numCapturedBottomRooks);
      numCapturedWhiteKnights = findViewById(R.id.numCapturedBottomKnights);
      numCapturedWhiteBishops = findViewById(R.id.numCapturedBottomBishops);
      numCapturedWhiteQueens = findViewById(R.id.numCapturedBottomQueens);
      whiteScore = findViewById(R.id.BottomScore);
      blackScore = findViewById(R.id.topScore);
      checkStatusBlack = findViewById(R.id.checkStatusBottom);
      checkStatusWhite = findViewById(R.id.checkStatusTop);
    }
    TextView message = findViewById(R.id.welcomeMessage);
    resign.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(GameManager.this, MainActivity.class);
        startActivity(intent);
      }
    });
    undo.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        board = game.getPreviousBoard();
        GameService.updateBoardMeta(board);
        updateBoard(board, isWhite);
      }
    });
    TextView finalWhiteScore1 = whiteScore;
    TextView finalBlackScore1 = blackScore;
    draw.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (isWhite && Integer.valueOf((String) finalWhiteScore1.getText()) >= 0
        || !isWhite && Integer.valueOf((String) finalBlackScore1.getText()) >=0 ) {
          message.setText("DECLINED");
        } else {
          message.setText("ACCEPT");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 0, isWhite, adversary.getName());
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
        }
      }
    });
    //Manually make the first move if the player is black.
    if (!isWhite) {
      try {
        Move adversaryMove = adversary.getMove(board, whitesPotentialMoves, blacksPotentialMoves);
        if (adversaryMove.isCapture) {
            capturedPiecesBlack.add(adversaryMove.capturablePiece);
        }
        GameService.makeMove(board, adversaryMove, true);
        GameService.updateBoardMeta(board);
        game.whitesMovesLog.add(adversaryMove);
        game.boardStates.add(GameService.copyBoard(board));
        updateBoard(board, false);
        postMoveChecks(board, true, checkStatusBlack, checkStatusWhite, message);
          message.setText("BLACK TO MOVE");
      } catch (CloneNotSupportedException e) {
        e.printStackTrace();
      }
    }

    //3. Set the buttons.
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        ImageButton button = (ImageButton) getButonFromCoord(new Coordinate(col, row), isWhite);
        TextView finalCheckStatusBlack = checkStatusBlack;
        TextView finalCheckStatusWhite = checkStatusWhite;
        TextView finalNumCapturedWhitePawns = numCapturedWhitePawns;
        TextView finalWhiteScore = whiteScore;
        TextView finalBlackScore = blackScore;
        TextView finalNumCapturedWhiteRooks = numCapturedWhiteRooks;
        TextView finalNumCapturedWhiteKnights = numCapturedWhiteKnights;
        TextView finalNumCapturedWhiteQueens = numCapturedWhiteQueens;
        TextView finalNumCapturedBlackPawns = numCapturedBlackPawns;
        TextView finalNumCapturedBlackRooks = numCapturedBlackRooks;
        TextView finalNumCapturedBlackKnights = numCapturedBlackKnights;
        TextView finalNumCapturedBlackBishops = numCapturedBlackBishops;
        TextView finalNumCapturedBlackQueens = numCapturedBlackQueens;
        TextView finalNumCapturedWhiteBishops = numCapturedWhiteBishops;
        button.setOnClickListener(new View.OnClickListener() {

          @Override
          public void onClick(View v) {
            Coordinate coord = getCoordFromButton(button, isWhite);
            Cell cell = board.board[coord.rank][coord.file];
            //CASE 1: Making a non-capturing move. (They clicked on an empty square with a selected piece.
            if (cell.PieceStatus == Status.EMPTY) {
              if (selectedPiece != null && isLegalMove(coord, board)) {
                Move move = new Move(selectedPiece.getPos(), coord, selectedPiece.getName(), false, isWhite);
                move = updateMove(move);
                if (move.isPromotion) {
                  PromotionDialog promotionDialog = new PromotionDialog(GameManager.this, board, move, isWhite);
                  promotionDialog.setOnPromotionMoveListener(GameManager.this);
                  promotionDialog.show();
                } else {
                  GameService.makeMove(board, move, isWhite);
                  if (isWhite) {
                    game.whitesMovesLog.add(move);
                  } else {
                    game.blacksMovesLog.add(move);
                  }
                  GameService.updateBoardMeta(board);
                  game.boardStates.add(GameService.copyBoard(board));
                  if (postMoveChecks(board, isWhite, finalCheckStatusBlack, finalCheckStatusWhite, message)) {
                    return;
                  };
                  if (isWhite) {
                    message.setText("BLACK TO MOVE");
                  } else {
                    message.setText("WHITE TO MOVE");
                  }
                  try {
                    ArrayList<Move> playersMoves = GameService.generateMoves(board, isWhite);
                    Move adversaryMove;
                    if (isWhite) {
                      adversaryMove = adversary.getMove(board, blacksPotentialMoves, playersMoves);
                    } else {
                      adversaryMove = adversary.getMove(board, whitesPotentialMoves, playersMoves);
                    }
                    if (adversaryMove.isCapture) {
                      if (isWhite) {
                        capturedPiecesWhite.add(adversaryMove.capturablePiece);
                        switch(adversaryMove.capturablePiece.getName()) {
                          case "Pawn":
                            finalNumCapturedWhitePawns.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhitePawns.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 1));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 1));
                            break;
                          case "Rook":
                            finalNumCapturedWhiteRooks.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteRooks.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 5));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 5));
                            break;
                          case "Knight":
                            finalNumCapturedWhiteKnights.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteKnights.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 3));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 3));
                            break;
                          case "Bishop":
                            finalNumCapturedWhiteBishops.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteBishops.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 3));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 3));
                            break;
                          case "Queen":
                            finalNumCapturedWhiteQueens.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteQueens.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 9));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 9));
                            break;
                        }
                      } else {
                        capturedPiecesBlack.add(adversaryMove.capturablePiece);
                        switch(adversaryMove.capturablePiece.getName()) {
                          case "Pawn":
                            finalNumCapturedBlackPawns.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackPawns.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 1));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 1));
                            break;
                          case "Rook":
                            finalNumCapturedBlackRooks.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackRooks.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 5));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 5));
                            break;
                          case "Knight":
                            finalNumCapturedBlackKnights.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackKnights.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 3));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 3));
                            break;
                          case "Bishop":
                            finalNumCapturedBlackBishops.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackBishops.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 3));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 3));
                            break;
                          case "Queen":
                            finalNumCapturedBlackQueens.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackQueens.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 9));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 9));
                            break;
                        }
                      }
                    }
                    GameService.makeMove(board, adversaryMove, !isWhite);
                    GameService.updateBoardMeta(board);
                    if (isWhite) {
                      game.blacksMovesLog.add(adversaryMove);
                    } else {
                      game.whitesMovesLog.add(adversaryMove);
                    }
                    game.boardStates.add(GameService.copyBoard(board));
                    updateBoard(board, isWhite);
                    postMoveChecks(board, !isWhite, finalCheckStatusBlack, finalCheckStatusWhite, message);
                    if (isWhite) {
                      message.setText("WHITE TO MOVE");
                    } else {
                      message.setText("BLACK TO MOVE");
                    }
                  } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                  }
                }
              }
              // CASE 2: Making a capturing move. (Clicked on an adversary piece with a piece selected.
            } else if ((cell.PieceStatus == Status.BLACK && isWhite) || cell.PieceStatus == Status.WHITE && !isWhite) {
              if (selectedPiece != null && isLegalMove(coord, board)) {
                Move move = new Move(selectedPiece.getPos(), coord, selectedPiece.getName(), true, isWhite);
                move = updateMove(move);
                move.setCapture(board.board[coord.rank][coord.file].piece);
                if (isWhite) {
                  capturedPiecesBlack.add(board.board[coord.rank][coord.file].piece);
                  switch(board.board[coord.rank][coord.file].piece.getName()) {
                    case "Pawn":
                      finalNumCapturedBlackPawns.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackPawns.getText()) + 1));
                      finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 1));
                      finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 1));
                      break;
                    case "Rook":
                      finalNumCapturedBlackRooks.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackRooks.getText()) + 1));
                      finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 5));
                      finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 5));
                      break;
                    case "Knight":
                      finalNumCapturedBlackKnights.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackKnights.getText()) + 1));
                      finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 3));
                      finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 3));
                      break;
                    case "Bishop":
                      finalNumCapturedBlackBishops.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackBishops.getText()) + 1));
                      finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 3));
                      finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 3));
                      break;
                    case "Queen":
                      finalNumCapturedBlackQueens.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackQueens.getText()) + 1));
                      finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 9));
                      finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 9));
                      break;
                  }
                } else {
                  capturedPiecesWhite.add(board.board[coord.rank][coord.file].piece);
                  switch(board.board[coord.rank][coord.file].piece.getName()) {
                    case "Pawn":
                      finalNumCapturedWhitePawns.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhitePawns.getText()) + 1));
                      finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 1));
                      finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 1));
                      break;
                    case "Rook":
                      finalNumCapturedWhiteRooks.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteRooks.getText()) + 1));
                      finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 5));
                      finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 5));
                      break;
                    case "Knight":
                      finalNumCapturedWhiteKnights.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteKnights.getText()) + 1));
                      finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 3));
                      finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 3));
                      break;
                    case "Bishop":
                      finalNumCapturedWhiteBishops.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteBishops.getText()) + 1));
                      finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 3));
                      finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 3));
                      break;
                    case "Queen":
                      finalNumCapturedWhiteQueens.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteQueens.getText()) + 1));
                      finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 9));
                      finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 9));
                      break;
                  }
                }
                if (move.isPromotion) {
                  PromotionDialog promotionDialog = new PromotionDialog(GameManager.this, board, move, isWhite);
                  promotionDialog.setOnPromotionMoveListener(GameManager.this);
                  promotionDialog.show();
                } else {
                  GameService.makeMove(board, move, isWhite);
                  GameService.updateBoardMeta(board);
                  if (isWhite) {
                    game.whitesMovesLog.add(move);
                  } else {
                    game.blacksMovesLog.add(move);
                  }
                  game.boardStates.add(GameService.copyBoard(board));
                  if (postMoveChecks(board, isWhite, finalCheckStatusBlack, finalCheckStatusWhite, message)) {
                    return;
                  }
                  if (isWhite) {
                    message.setText("BLACK TO MOVE");
                  } else {
                    message.setText("WHITE TO MOVE");
                  }
                  try {
                    ArrayList<Move> playersMoves = GameService.generateMoves(board, isWhite);
                    Move adversaryMove;
                    if (isWhite) {
                      adversaryMove = adversary.getMove(board, blacksPotentialMoves, playersMoves);
                    } else {
                      adversaryMove = adversary.getMove(board, whitesPotentialMoves, playersMoves);
                    } if (adversaryMove.isCapture) {
                      if (isWhite) {
                        capturedPiecesWhite.add(adversaryMove.capturablePiece);
                        switch(board.board[coord.rank][coord.file].piece.getName()) {
                          case "Pawn":
                            finalNumCapturedWhitePawns.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhitePawns.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 1));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 1));
                            break;
                          case "Rook":
                            finalNumCapturedWhiteRooks.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteRooks.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 5));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 5));
                            break;
                          case "Knight":
                            finalNumCapturedWhiteKnights.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteKnights.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 3));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 3));
                            break;
                          case "Bishop":
                            finalNumCapturedWhiteBishops.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteBishops.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 3));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 3));
                            break;
                          case "Queen":
                            finalNumCapturedWhiteQueens.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedWhiteQueens.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) + 9));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) - 9));
                            break;
                        }
                      } else {
                        capturedPiecesBlack.add(adversaryMove.capturablePiece);
                        switch(board.board[coord.rank][coord.file].piece.getName()) {
                          case "Pawn":
                            finalNumCapturedBlackPawns.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackPawns.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 1));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 1));
                            break;
                          case "Rook":
                            finalNumCapturedBlackRooks.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackRooks.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 5));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 5));
                            break;
                          case "Knight":
                            finalNumCapturedBlackKnights.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackKnights.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 3));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 3));
                            break;
                          case "Bishop":
                            finalNumCapturedBlackBishops.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackBishops.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 3));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 3));
                            break;
                          case "Queen":
                            finalNumCapturedBlackQueens.setText( String.valueOf(Integer.valueOf((String) finalNumCapturedBlackQueens.getText()) + 1));
                            finalWhiteScore.setText(String.valueOf(Integer.valueOf((String) finalWhiteScore.getText()) - 9));
                            finalBlackScore.setText(String.valueOf(Integer.valueOf((String) finalBlackScore.getText()) + 9));
                            break;
                        }
                      }
                    }
                    GameService.makeMove(board, adversaryMove, !isWhite);
                    GameService.updateBoardMeta(board);
                    if (isWhite) {
                      game.blacksMovesLog.add(adversaryMove);
                    } else {
                      game.whitesMovesLog.add(adversaryMove);
                    }
                    game.boardStates.add(GameService.copyBoard(board));
                    updateBoard(board, isWhite);
                    postMoveChecks(board, !isWhite, finalCheckStatusBlack, finalCheckStatusWhite, message);
                    if (isWhite) {
                      message.setText("WHITE TO MOVE");
                    } else {
                      message.setText("BLACK TO MOVE");
                    }
                  } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                  }
                }
              }
              //CASE 3: The player clicked on one of their pieces.
            } else {
              if (selectedPiece != null) {
                for (Move move : GameService.filterMoves(selectedPiece.generateMoves(selectedPiece.getPos(), board.board))) {
                  ImageButton curPieceButton = (ImageButton) getButonFromCoord(selectedPiece.getPos(), isWhite);
                  curPieceButton.setColorFilter(null);
                  ImageButton button = (ImageButton) getButonFromCoord(move.toCoord, isWhite);
                  if (board.board[move.toCoord.rank][move.toCoord.file].isLight
                      && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.EMPTY) {
                    button.setImageResource(R.drawable.empty_light);
                  } else if (board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.EMPTY) {
                    button.setImageResource(R.drawable.empty_dark);
                  } else if ((isWhite && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.BLACK) ||
                      (!isWhite && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.WHITE)){
                    button.setColorFilter(null);
                  }
                }
              }
              if (!cell.piece.equals(selectedPiece)) {
                selectedPiece = board.board[coord.rank][coord.file].piece;
                ImageButton curPiece = (ImageButton) getButonFromCoord(selectedPiece.getPos(), isWhite);
                curPiece.setColorFilter(Color.YELLOW, PorterDuff.Mode.OVERLAY);
                ArrayList<Move> legalMoves = selectedPiece.generateMoves(coord, board.board);
                for (Move move : GameService.filterMoves(legalMoves)) {
                  if (isLegalMove(move.toCoord, board)) {
                    ImageButton button = (ImageButton) getButonFromCoord(move.toCoord, isWhite);
                  if (board.board[move.toCoord.rank][move.toCoord.file].isLight
                      && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.EMPTY) {
                    button.setImageResource(R.drawable.white_empty_selected);
                  } else if (board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.EMPTY) {
                    button.setImageResource(R.drawable.black_empty_selected);
                  }
                  else if ((isWhite && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.BLACK) ||
                      (!isWhite && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.WHITE)){
                    button.setColorFilter(Color.RED, PorterDuff.Mode.OVERLAY);
                  }
                }
                }
              } else {
                ImageButton curPieceButton = (ImageButton) getButonFromCoord(selectedPiece.getPos(), isWhite);
                curPieceButton.setColorFilter(null);
                selectedPiece = null;
              }
            }
          }
        });
      }
    }
  }

  /**
   * Updates the move if it is is a castle, enPassant or Promotion.
   *
   * @param move
   * @return
   */
  public Move updateMove(Move move) {
    if (move.piece.getName().equals("King") && Math.abs(move.toCoord.file - move.fromCoord.file) == 2) {
      move.setCastle();
    } else if (move.piece.getName().equals("Pawn") && ((this.isWhite && move.toCoord.rank == 7) || (!this.isWhite && move.toCoord.rank == 0))) {
      move.setPromotion(new Queen(move.toCoord, move.piece.getColor()));
    } else if (move.piece.getName().equals("Pawn") && Math.abs(move.toCoord.file - move.fromCoord.file) == 1 &&
        board.board[move.toCoord.rank][move.toCoord.file].PieceStatus.equals(Status.EMPTY)) {
      move.setEnPassant();
      if (isWhite) {
        move.setCapture(board.board[move.toCoord.rank - 1][move.toCoord.file].piece);
      } else {
        move.setCapture(board.board[move.toCoord.rank + 1][move.toCoord.file].piece);
      }
    }
    return move;
  }

  //Checks if the game is over.
  public boolean postMoveChecks(Board board, boolean whiteMoved, TextView checkStatusBlack, TextView checkStatusWhite, TextView message) {

    //CHECK 1: Dead position.
    if (GameService.isDeadPosition(board.whitePieces, board.blackPieces)) {
      message.setText("DRAW BY INSUFFICIENT MATERIAL");
      GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 0, isWhite, this.adversary.getName());
      ggDialog.setOnGameOverListener(GameManager.this);
      ggDialog.show();
      return true;
    }
    //CHeck 2: Repetition.
    if (GameService.isRepetition(game.boardStates, board)) {
      message.setText("DRAW BY REPETITION");
      GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 0, isWhite, adversary.getName());
      ggDialog.setOnGameOverListener(GameManager.this);
      ggDialog.show();
      return true;
    }
    //Check 3: Checks, mates and stalemates.
    if (whiteMoved) {
      try {
        blacksPotentialMoves = GameService.generateMoves(board, false);
      } catch (CloneNotSupportedException e) {
        e.printStackTrace();
      }
      if (((King) board.blackPieces.get(0)).isDoubleChecked) {
        checkStatusBlack.setText("DOUBLE CHECK!!");
        blacksPotentialMoves = GameService.generateMovesDoubleCheck(board, blacksPotentialMoves, false);
        if (blacksPotentialMoves.size() == 0) {
          message.setText("CHECKMATE!! PLAYER 1 WINS");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 1, isWhite, adversary.getName());
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
          return true;
        }
      } else if (((King) board.blackPieces.get(0)).isChecked) {
        checkStatusBlack.setText("CHECK!");
        blacksPotentialMoves = GameService.generateMovesCheck(board, blacksPotentialMoves, false);
        if (blacksPotentialMoves.size() == 0) {
          message.setText("CHECKMATE!! PLAYER 1 WINS");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 1, isWhite, adversary.getName());
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
          return true;
        }
      } else {
        if (blacksPotentialMoves.size() == 0) {
          message.setText("STALEMATE. THE GAME ENDS IN A DRAW");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 0, isWhite, adversary.getName());
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
          return true;
        } else {
          updateBoard(board, isWhite);
          selectedPiece = null;
          checkStatusWhite.setText("");
        }
      }
    } else {
      try {
        whitesPotentialMoves = GameService.generateMoves(board, true);
      } catch (CloneNotSupportedException e) {
        e.printStackTrace();
      }
      if (((King) board.whitePieces.get(0)).isDoubleChecked) {
        checkStatusWhite.setText("DOUBLE CHECK!!");
        whitesPotentialMoves = GameService.generateMovesDoubleCheck(board, whitesPotentialMoves, true);
        if (whitesPotentialMoves.size() == 0) {
          message.setText("CHECKMATE!! PLAYER 1 LOSES");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, -1, isWhite, adversary.getName());
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
          return true;
        }
      } else if (((King) board.whitePieces.get(0)).isChecked) {
        checkStatusWhite.setText("CHECK!");
        whitesPotentialMoves = GameService.generateMovesCheck(board, whitesPotentialMoves, true);
        if (whitesPotentialMoves.size() == 0) {
          message.setText("CHECKMATE!! PLAYER 1 LOSES");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, -1, isWhite, adversary.getName());
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
          return true;
        }
      } else {
        if (whitesPotentialMoves.size() == 0) {
          message.setText("STALEMATE. THE GAME ENDS IN A DRAW");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 0, isWhite, adversary.getName());
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
          return true;
        } else {
          checkStatusBlack.setText("");
          updateBoard(board, isWhite);
        }
      }
    }
    if (!game.isEndGame && isEndGame(board)) {
      game.isEndGame = true;
    }
    return false;
  }

  public static boolean isEndGame(Board board) {
    for (Piece piece : board.whitePieces) {
      if (piece.getName().equals("Queen")) {
        return false;
      }
    }
    for (Piece piece : board.blackPieces) {
      if (piece.getName().equals("Queen")) {
        return false;
      }
    }
    if (board.whitePieces.size() < 9 && board.blackPieces.size() < 9) {
      return true;
    } else {
      return false;
    }
  }
  public static Agent initializeAgent(String agentName, boolean isWhite) {
    Agent agent;
    switch (agentName) {
      case "Randy":
        agent = new Randy(AgentType.RANDY, !isWhite);
        break;
      case "Simple":
        agent = new Simple(AgentType.SIMPLE, !isWhite);
        break;
      case "MinMax":
        agent = new MinMax(AgentType.MINMAX, !isWhite);
        break;
      default:
        agent = new FishStock(AgentType.FISHSTOCK, !isWhite);
    }
    return agent;
  }


  public boolean isLegalMove(Coordinate coord, Board board) {
    if (selectedPiece == null) {
      return false;
    }
    if ((isWhite && ((King)board.whitePieces.get(0)).isChecked)
       || (!isWhite && ((King)board.blackPieces.get(0)).isChecked)) {
      List<Move> checkMoves;
      if (isWhite) {
        checkMoves = GameService.generateMovesCheck(board, whitesPotentialMoves, true);
      } else {
        checkMoves = GameService.generateMovesCheck(board, blacksPotentialMoves, false);
      }
      for (Move move: checkMoves) {
        if (Coordinate.compareCoords(move.toCoord, coord) && move.piece.getName().equals(selectedPiece.getName())) {
          return true;
        }
      }
      return false;
    }
    if (((King)board.whitePieces.get(0)).isDoubleChecked) {
      List<Move> doubleCheckMoves;
      if (isWhite) {
        doubleCheckMoves = GameService.generateMovesDoubleCheck(board, whitesPotentialMoves, true);
      } else {
        doubleCheckMoves = GameService.generateMovesDoubleCheck(board, blacksPotentialMoves, false);
      }
      for (Move move: doubleCheckMoves) {
        if (Coordinate.compareCoords(move.toCoord, coord) && move.piece.getName().equals(selectedPiece.getName())) {
          return true;
        }
      }
      return false;
    }

    List<Move> moves = GameService.filterMoves(selectedPiece.generateMoves(selectedPiece.getPos(), board.board));
    for (Move move : moves) {
      if (move.toCoord.file == coord.file && move.toCoord.rank == coord.rank) {
        return true;
      }
    }
    return false;
  }

  public Coordinate getCoordFromButton(View button, boolean isWhite) {
    String buttonId = getResources().getResourceEntryName(button.getId());
    int rank;
    int file;
    if (isWhite) {
      rank = 7 - (buttonId.charAt(6) - '0');
      file = 7 - (buttonId.charAt(7) - '0');
    } else {
      rank = buttonId.charAt(6) - '0';
      file = buttonId.charAt(7) - '0';
    }
    return new Coordinate(file, rank);
  }

  public ImageView getButonFromCoord(Coordinate coord, boolean isWhite) {
    int resId;
    if (isWhite) {
      resId = getResources().getIdentifier("button" + (7 - coord.rank) + (7 - coord.file), "id", this.getPackageName());
    } else {
      resId = getResources().getIdentifier("button" + (coord.rank) + (coord.file), "id", this.getPackageName());
    }
    return findViewById(resId);
  }

  public static void defaultView(ArrayList<ImageButton> squares) {
  }

  public void updateBoard(Board board, boolean isWhite) {
    GridLayout grid = findViewById(R.id.gridlayout);
    if (isWhite) {
      for (int row = 0; row < 8; row++) {
        for (int col = 0; col < 8; col++) {
          Cell cell = board.board[row][col];
          updateCellImage(cell, row, col);
        }
      }
    }else {
      for (int row = 7; row >= 0; row --) {
        for (int col = 7; col >=0; col--) {
          Cell cell = board.board[row][col];
          updateCellImage(cell, row, col);
        }
      }
    }
  }


  public void updateCellImage(Cell cell, int row, int col) {
    ImageView cellImageView = getButonFromCoord(new Coordinate(col, row), isWhite);
    if (cell.isEmpty) {
      if (cell.isLight) {
        cellImageView.setImageResource(R.drawable.empty_light);
        cellImageView.setColorFilter(null);
      } else {
        cellImageView.setImageResource(R.drawable.empty_dark);
        cellImageView.setColorFilter(null);
      }
    } else if (cell.isWhite) {
      if (cell.isKing) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_king_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.white_king_on_dark);
          cellImageView.setColorFilter(null);
        }
      } else if (cell.isQueen) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_queen_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.white_queen_on_dark);
          cellImageView.setColorFilter(null);
        }
      } else if (cell.isBishop) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_bishop_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.white_bishop_on_dark);
          cellImageView.setColorFilter(null);
        }
      } else if (cell.isKnight) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_knight_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.white_knight_on_dark);
          cellImageView.setColorFilter(null);
        }
      } else if (cell.isRook) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_rook_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.white_rook_on_dark);
          cellImageView.setColorFilter(null);
        }
      } else if (cell.isPawn) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_pawn_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.white_pawn_on_dark);
          cellImageView.setColorFilter(null);
        }
      }
    } else {
      if (cell.isKing) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_king_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.black_king_on_dark);
          cellImageView.setColorFilter(null);
        }
      } else if (cell.isQueen) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_queen_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.black_queen_on_dark);
          cellImageView.setColorFilter(null);
        }
      } else if (cell.isBishop) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_bishop_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.black_bishop_on_dark);
          cellImageView.setColorFilter(null);
        }
      } else if (cell.isKnight) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_knight_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.black_knight_on_dark);
          cellImageView.setColorFilter(null);
        }
      } else if (cell.isRook) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_rook_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.black_rook_on_dark);
          cellImageView.setColorFilter(null);
        }
      } else if (cell.isPawn) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_pawn_on_light);
          cellImageView.setColorFilter(null);
        } else {
          cellImageView.setImageResource(R.drawable.black_pawn_on_dark);
          cellImageView.setColorFilter(null);
        }
      }
    }
  }
  @Override
  public void onPromotionMove() throws CloneNotSupportedException {
    updateBoard(board, isWhite);
    ArrayList<Move> blacksPotentialMoves = GameService.generateMoves(board, false);
    if (((King)board.blackPieces.get(0)).isDoubleChecked) {
      blacksPotentialMoves = GameService.generateMovesDoubleCheck(board, blacksPotentialMoves, false);
    } else if (((King)board.blackPieces.get(0)).isChecked) {
      blacksPotentialMoves = GameService.generateMovesCheck(board, blacksPotentialMoves, false);
    }
    Move adversaryMove = this.adversary.getMove(board, blacksPotentialMoves, whitesPotentialMoves);
    GameService.makeMove(board, adversaryMove,false);
    GameService.updateBoardMeta(board);
    TextView checkStatusBlack = findViewById(R.id.checkStatusTop);
    TextView checkStatusWhite = findViewById(R.id.checkStatusBottom);
    TextView message = findViewById(R.id.welcomeMessage);
    postMoveChecks(board, false, checkStatusBlack, checkStatusWhite, message);
  }

  @Override
  public void onGameOver() {

  }
}