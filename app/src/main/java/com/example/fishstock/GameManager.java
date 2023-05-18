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
    //Manually make the first move if the player is black.
    if (!isWhite) {
      try {
        Move adversaryMove = adversary.getMove(board, whitesPotentialMoves, blacksPotentialMoves);
        if (adversaryMove.isCapture) {
            capturedPiecesBlack.add(adversaryMove.capturablePiece);
            capturedBlack.append(": " + adversaryMove.capturablePiece.getName());
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
                  if (postMoveChecks(board, isWhite, checkStatusBlack, checkStatusWhite, message)) {
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
                        capturedWhite.append(": " + adversaryMove.capturablePiece.getName());
                      } else {
                        capturedPiecesBlack.add(adversaryMove.capturablePiece);
                        capturedBlack.append(": " + adversaryMove.capturablePiece.getName());
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
                    postMoveChecks(board, !isWhite, checkStatusBlack, checkStatusWhite, message);
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
                  capturedBlack.append(": " + board.board[coord.rank][coord.file].piece.getName());
                } else {
                  capturedPiecesWhite.add(board.board[coord.rank][coord.file].piece);
                  capturedWhite.append(": " + board.board[coord.rank][coord.file].piece.getName());
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
                  if (postMoveChecks(board, isWhite, checkStatusBlack, checkStatusWhite, message)) {
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
                    }if (adversaryMove.isCapture) {
                        capturedPiecesWhite.add(adversaryMove.capturablePiece);
                        capturedWhite.append(": " + adversaryMove.capturablePiece.getName());
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
                    postMoveChecks(board, !isWhite, checkStatusBlack, checkStatusWhite, message);
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
                  if (isLegalMove(move.toCoord, board)) {
                    ImageButton button = (ImageButton) getButonFromCoord(move.toCoord, isWhite);
                  if (board.board[move.toCoord.rank][move.toCoord.file].isLight
                      && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.EMPTY) {
                    button.setImageResource(R.drawable.white_empty_selected);
                  } else if (board.board[move.toCoord.rank][move.toCoord.file].PieceStatus == Status.EMPTY) {
                    button.setImageResource(R.drawable.black_empty_selected);
                  }
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
      move.setPromotion(new Queen(move.toCoord, move.piece.getColor()));
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
    return false;
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
    Move adversaryMove = this.adversary.getMove(board, blacksPotentialMoves, whitesPotentialMoves);
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