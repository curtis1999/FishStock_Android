package com.example.fishstock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
  Agent whitePlayer;
  Agent blackPlayer;
  Piece selectedPiece;
  boolean isWhite = true; //TODO;
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
    this.isWhite = getIntent().getBooleanExtra("isWhite", false);
    if (this.isWhite) {
      this.blackPlayer = initializeAgent(getIntent().getStringExtra("agentType"));
      this.whitePlayer = new Human(AgentType.HUMAN, true);
    } else {
      this.blackPlayer = new Human(AgentType.HUMAN, true);
      this.whitePlayer = initializeAgent(getIntent().getStringExtra("agentType"));
    }

    this.game = new Game(board, whitePlayer.type, blackPlayer.type);
    this.game.boardStates.add(GameService.copyBoard(this.board));
    updateBoard(board, isWhite);

    //2. Initialize the texts and buttons.
    TextView adversaryName = findViewById(R.id.player2);
    adversaryName.setText(blackPlayer.getName());
    Button resign = findViewById(R.id.resign);
    Button undo = findViewById(R.id.undo);
    Button draw = findViewById(R.id.draw);
    ImageButton promotionQueen = findViewById(R.id.promotionQueen);
    ImageButton promotionRook = findViewById(R.id.promotionRook);
    ImageButton promotionBishop = findViewById(R.id.promotionBishop);
    ImageButton promotionKnight = findViewById(R.id.promotionKnight);
    TextView capturedWhite = findViewById(R.id.CapturedPiecesWhite);
    TextView capturedBlack = findViewById(R.id.CapturedPiecesBlack);
    TextView checkStatusBlack = findViewById(R.id.checkStatusBlack);
    TextView checkStatusWhite = findViewById(R.id.checkStatusWhite);
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


    //3. Set the buttons.
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        ImageButton button = (ImageButton) getButonFromCoord(new Coordinate(col, row), isWhite);
        button.setOnClickListener(new View.OnClickListener() {

          @Override
          public void onClick(View v) {
            Coordinate coord = getCoordFromButton(button);
            Cell cell = board.board[coord.rank][coord.file];
            //CASE 1: Making a non-capturing move. (They clicked on an empty square with a selected piece.
            if (cell.PieceStatus == Status.EMPTY) {
              if (selectedPiece != null && isLegalMove(coord, board.board)) {
                Move move = new Move(selectedPiece.getPos(), coord, selectedPiece.getName(), false, true); //TODO: MAKE AN ISWHITE VARIABLE
                move = updateMove(move);
                if (move.isPromotion) {
                  PromotionDialog promotionDialog = new PromotionDialog(GameManager.this, board, move, isWhite);
                  promotionDialog.setOnPromotionMoveListener(GameManager.this);
                  promotionDialog.show();
                } else {
                  GameService.makeMove(board, move, true);
                  game.whitesMovesLog.add(move);
                  GameService.updateBoardMeta(board);
                  game.boardStates.add(GameService.copyBoard(board));
                  if (postMoveChecks(board, true, checkStatusBlack, checkStatusWhite, message)) {
                    return;
                  };
                  message.setText("BLACK TO MOVE");
                  try {
                    ArrayList<Move> playersMoves = GameService.generateMoves(board, true); //TODO: Should be unnecessary
                    Move adversaryMove = blackPlayer.getMove(board, blacksPotentialMoves, playersMoves);
                    if (adversaryMove.isCapture) {
                      capturedPiecesWhite.add(adversaryMove.capturablePiece);
                      capturedWhite.append(": " + adversaryMove.capturablePiece.getName());
                    }
                    GameService.makeMove(board, adversaryMove, false);
                    GameService.updateBoardMeta(board);
                    game.blacksMovesLog.add(adversaryMove);
                    game.boardStates.add(GameService.copyBoard(board));
                    updateBoard(board, isWhite);
                    postMoveChecks(board, false, checkStatusBlack, checkStatusWhite, message);
                    message.setText("WHITE TO MOVE");
                  } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                  }
                }
              }
              // CASE 2: Making a capturing move. (Clicked on an adversary piece with a piece selected.
            } else if (cell.PieceStatus == Status.BLACK) {
              if (selectedPiece != null && isLegalMove(coord, board.board)) {
                Move move = new Move(selectedPiece.getPos(), coord, selectedPiece.getName(), true, true); //TODO: MAKE AN ISWHITE VARIABLE
                move = updateMove(move);
                move.setCapture(board.board[coord.rank][coord.file].piece);
                capturedPiecesBlack.add(board.board[coord.rank][coord.file].piece);
                capturedBlack.append(": " + board.board[coord.rank][coord.file].piece.getName());
                if (move.isPromotion) {
                  PromotionDialog promotionDialog = new PromotionDialog(GameManager.this, board, move, isWhite);
                  promotionDialog.setOnPromotionMoveListener(GameManager.this);
                  promotionDialog.show(); //TODO: MAKE THE MOVE WITHIN HERE!!
                } else {
                  GameService.makeMove(board, move, true);
                  GameService.updateBoardMeta(board);
                  game.whitesMovesLog.add(move);
                  game.boardStates.add(GameService.copyBoard(board));
                  if (postMoveChecks(board, true, checkStatusBlack, checkStatusWhite, message)) {
                    return;
                  }
                  ;
                  message.setText("BLACK TO MOVE");
                  try {
                    ArrayList<Move> playersMoves = GameService.generateMoves(board, true);
                    Move adversaryMove = blackPlayer.getMove(board, blacksPotentialMoves, playersMoves);
                    if (adversaryMove.isCapture) {
                      capturedPiecesWhite.add(adversaryMove.capturablePiece);
                      capturedWhite.append(": " + adversaryMove.capturablePiece.getName());
                    }
                    GameService.makeMove(board, adversaryMove, false);
                    GameService.updateBoardMeta(board);
                    game.blacksMovesLog.add(adversaryMove);
                    game.boardStates.add(GameService.copyBoard(board));
                    updateBoard(board, isWhite);
                    postMoveChecks(board, false, checkStatusBlack, checkStatusWhite, message);
                    message.setText("WHITE TO MOVE");
                  } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                  }
                }
              }
              //CASE 3: The player clicked a white piece.
            } else {
              if (selectedPiece != null) {
                for (Move move : GameService.filterMoves(selectedPiece.generateMoves(selectedPiece.getPos(), board.board))) {
                  ImageButton button = (ImageButton) getButonFromCoord(move.toCoord, isWhite);
                  if (board.board[move.toCoord.rank][move.toCoord.file].isLight
                      && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.EMPTY) {
                    button.setImageResource(R.drawable.empty_light);
                  } else if (board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.EMPTY) {
                    button.setImageResource(R.drawable.empty_dark);
                  }
                }
              }
              if (!cell.piece.equals(selectedPiece)) {
                selectedPiece = board.board[coord.rank][coord.file].piece;
                ArrayList<Move> legalMoves = selectedPiece.generateMoves(coord, board.board);
                for (Move move : GameService.filterMoves(legalMoves)) {
                  ImageButton button = (ImageButton) getButonFromCoord(move.toCoord, isWhite);
                  if (board.board[move.toCoord.rank][move.toCoord.file].isLight
                      && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.EMPTY) {
                    button.setImageResource(R.drawable.white_empty_selected);
                  } else if (board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.EMPTY) {
                    button.setImageResource(R.drawable.black_empty_selected);
                  }
                }
              } else {
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
      move.setPromotion(new Queen(move.toCoord, move.piece.getColor())); //TODO: ASK THE USER FOR THE PROMOTION CHOICE!!
    } else if (move.piece.getName().equals("Pawn") && Math.abs(move.toCoord.file - move.fromCoord.file) == 1 &&
        board.board[move.toCoord.rank][move.toCoord.file].PieceStatus.equals(Status.EMPTY)) {
      move.setEnPassant();
    }
    return move;
  }

  public boolean postMoveChecks(Board board, boolean whiteMoved, TextView checkStatusBlack, TextView checkStatusWhite, TextView message) {

    //CHECK 1: Dead position.
    if (GameService.isDeadPosition(board.whitePieces, board.blackPieces)) {
      message.setText("DRAW BY INSUFFICIENT MATERIAL");
      GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 0);
      ggDialog.setOnGameOverListener(GameManager.this);
      ggDialog.show();
      return true;
    }
    //CHeck 2: Repetition.
    if (GameService.isRepetition(game.boardStates, board)) {
      message.setText("DRAW BY REPETITION");
      GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 0);
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
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 1);
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
          return true;
        }
      } else if (((King) board.blackPieces.get(0)).isChecked) {
        checkStatusBlack.setText("CHECK!");
        blacksPotentialMoves = GameService.generateMovesCheck(board, blacksPotentialMoves, false);
        if (blacksPotentialMoves.size() == 0) {
          message.setText("CHECKMATE!! PLAYER 1 WINS");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 1);
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
          return true;
        }
      } else {
        if (blacksPotentialMoves.size() == 0) {
          message.setText("STALEMATE. THE GAME ENDS IN A DRAW");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 0);
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
        whitesPotentialMoves = GameService.generateMoves(board, false);
      } catch (CloneNotSupportedException e) {
        e.printStackTrace();
      }
      if (((King) board.whitePieces.get(0)).isDoubleChecked) {
        checkStatusWhite.setText("DOUBLE CHECK!!");
        whitesPotentialMoves = GameService.generateMovesDoubleCheck(board, whitesPotentialMoves, true);
        if (whitesPotentialMoves.size() == 0) {
          message.setText("CHECKMATE!! PLAYER 1 LOSES");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, -1);
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
          return true;
        }
      } else if (((King) board.whitePieces.get(0)).isChecked) {
        checkStatusWhite.setText("CHECK!");
        whitesPotentialMoves = GameService.generateMovesCheck(board, whitesPotentialMoves, true);
        if (whitesPotentialMoves.size() == 0) {
          message.setText("CHECKMATE!! PLAYER 1 LOSES");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, -1);
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
          return true;
        }
      } else {
        if (whitesPotentialMoves.size() == 0) {
          message.setText("STALEMATE. THE GAME ENDS IN A DRAW");
          GameOverDialog ggDialog = new GameOverDialog(GameManager.this, 0);
          ggDialog.setOnGameOverListener(GameManager.this);
          ggDialog.show();
          return true;
        } else {
          checkStatusBlack.setText("");
          updateBoard(board, isWhite);
        }
      }

    }
    return false;
  }


  public static Agent initializeAgent(String agentName) {
    Agent agent;
    switch (agentName) {
      case "Randy":
        agent = new Randy(AgentType.RANDY, false); //TODO: ASK USER IF THEY WANT TO PLAY AS WHITE OR BLACK!
        break;
      case "Simple":
        agent = new Simple(AgentType.SIMPLE, false);
        break;
      case "MinMax":
        agent = new MinMax(AgentType.MINMAX, false);
        break;
      default:
        agent = new FishStock(AgentType.FISHSTOCK, false);
    }
    return agent;
  }


  public boolean isLegalMove(Coordinate coord, Cell[][] board) {
    if (selectedPiece == null) {
      return false;
    }
    List<Move> moves = selectedPiece.generateMoves(selectedPiece.getPos(), board);
    for (Move move : moves) {
      if (move.toCoord.file == coord.file && move.toCoord.rank == coord.rank) {
        return true;
      }
    }
    return false;
  }

  public Coordinate getCoordFromButton(View button) {
    String buttonId = getResources().getResourceEntryName(button.getId());
    int rank = 7 - (buttonId.charAt(6) - '0');
    int file = 7 - (buttonId.charAt(7) - '0');
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
      } else {
        cellImageView.setImageResource(R.drawable.empty_dark);
      }
    } else if (cell.isWhite) {
      if (cell.isKing) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_king_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_king_on_dark);
        }
      } else if (cell.isQueen) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_queen_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_queen_on_dark);
        }
      } else if (cell.isBishop) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_bishop_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_bishop_on_dark);
        }
      } else if (cell.isKnight) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_knight_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_knight_on_dark);
        }
      } else if (cell.isRook) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_rook_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_rook_on_dark);
        }
      } else if (cell.isPawn) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.white_pawn_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_pawn_on_dark);
        }
      }
    } else {
      if (cell.isKing) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_king_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_king_on_dark);
        }
      } else if (cell.isQueen) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_queen_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_queen_on_dark);
        }
      } else if (cell.isBishop) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_bishop_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_bishop_on_dark);
        }
      } else if (cell.isKnight) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_knight_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_knight_on_dark);
        }
      } else if (cell.isRook) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_rook_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_rook_on_dark);
        }
      } else if (cell.isPawn) {
        if (cell.isLight) {
          cellImageView.setImageResource(R.drawable.black_pawn_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_pawn_on_dark);
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
    Move adversaryMove = this.blackPlayer.getMove(board, blacksPotentialMoves, whitesPotentialMoves);
    GameService.makeMove(board, adversaryMove,false);
    GameService.updateBoardMeta(board);
    TextView checkStatusBlack = findViewById(R.id.checkStatusBlack);
    TextView checkStatusWhite = findViewById(R.id.checkStatusWhite);
    TextView message = findViewById(R.id.welcomeMessage);
    postMoveChecks(board, false, checkStatusBlack, checkStatusWhite, message);
  }

  @Override
  public void onGameOver() {

  }
}