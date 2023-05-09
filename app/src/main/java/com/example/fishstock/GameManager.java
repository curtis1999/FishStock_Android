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

public class GameManager extends AppCompatActivity {
  Game game;
  Board board;
  Agent whitePlayer;
  Agent blackPlayer;
  Piece selectedPiece;
  boolean hasStarted = false;
  boolean isWhite = true;
  ArrayList<Piece> capturedPiecesWhite = new ArrayList<>();
  ArrayList<Piece> capturedPiecesBlack = new ArrayList<>();
  /**
   * The Game Loop.  Initializes the board and the buttons.
   * @param savedInstanceState
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);
    //1. Initialize the board, agent and game
    this.board = new Board();
    this.blackPlayer = initializeAgent(getIntent().getStringExtra("agentType"));
    this.whitePlayer = new Human(AgentType.HUMAN, true);
    this.game = new Game(board, whitePlayer.type, blackPlayer.type);
    updateBoard(board);

    //2. Initialize the texts and buttons.
    TextView adversaryName = findViewById(R.id.player2);
    adversaryName.setText(blackPlayer.getName());
    Button resign = findViewById(R.id.resign);
    Button undo = findViewById(R.id.undo);
    Button draw = findViewById(R.id.draw);
    TextView capturedWhite = findViewById(R.id.CapturedPiecesWhite);
    TextView capturedBlack = findViewById(R.id.CapturedPiecesBlack);
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
      }
    });
    TextView message = findViewById(R.id.welcomeMessage);
    if (isWhite && hasStarted) {
      message.setText("PLEASE MAKE YOUR FIRST MOVE");
    } else if (!isWhite && hasStarted) {
      message.setText("It is " + blackPlayer.getName() + "'s Turn");
    }
    //3. Set the buttons.
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        ImageButton button = (ImageButton) getButonFromCoord(new Coordinate(col, row));
        button.setOnClickListener(new View.OnClickListener() {

          @Override
          public void onClick(View v) {
            Coordinate coord = getCoordFromButton(button);
            Cell cell = board.board[coord.rank][coord.file];
            if (selectedPiece != null && cell.PieceStatus == Status.EMPTY) {
              if (isLegalMove(coord, board.board)){
                Move move = new Move(selectedPiece.getPos(), coord, selectedPiece.getName(), false, true); //TODO: MAKE AN ISWHITE VARIABLE
                GameService.makeMove(board, move, true);
                GameService.updateBoardMeta(board);
                updateBoard(board);
                selectedPiece = null;
                try {
                  ArrayList<Move> adversaryMoves = GameService.generateMoves(board, false);
                  ArrayList<Move> playersMoves = GameService.generateMoves(board, true);
                  Move adversaryMove = blackPlayer.getMove(board, adversaryMoves, playersMoves);
                  if (adversaryMove.isCapture) {
                    capturedPiecesWhite.add(adversaryMove.capturablePiece);
                    capturedWhite.append(": " + adversaryMove.capturablePiece.getName());
                  }
                  GameService.makeMove(board, adversaryMove, false);
                  GameService.updateBoardMeta(board);
                  updateBoard(board);
                } catch (CloneNotSupportedException e) {
                  e.printStackTrace();
                }

              }
            } else if (cell.PieceStatus == Status.BLACK){
              if (selectedPiece != null) {
                Move move = new Move(selectedPiece.getPos(), coord, selectedPiece.getName(), true, true); //TODO: MAKE AN ISWHITE VARIABLE
                move.setCapture(board.board[coord.rank][coord.file].piece);
                capturedPiecesBlack.add(board.board[coord.rank][coord.file].piece);
                capturedBlack.append(": " + board.board[coord.rank][coord.file].piece.getName());
                GameService.makeMove(board, move, true);
                GameService.updateBoardMeta(board);
                updateBoard(board);
                selectedPiece = null;
                if (isGameOver(board, true)) {

                }
                try {
                  ArrayList<Move> adversaryMoves = GameService.generateMoves(board, false);
                  ArrayList<Move> playersMoves = GameService.generateMoves(board, true);
                  Move adversaryMove = blackPlayer.getMove(board, adversaryMoves, playersMoves);
                  if (adversaryMove.isCapture) {
                    capturedPiecesWhite.add(adversaryMove.capturablePiece);
                    capturedWhite.append(": " + adversaryMove.capturablePiece.getName());
                  }
                  GameService.makeMove(board, adversaryMove, false);
                  GameService.updateBoardMeta(board);
                  updateBoard(board);
                  if (isGameOver(board, false)){

                  }
                } catch (CloneNotSupportedException e) {
                  e.printStackTrace();
                }
              }
            } else {
              if (selectedPiece != null) {
                for (Move move : GameService.filterMoves(selectedPiece.generateMoves(selectedPiece.getPos(), board.board))) {
                  ImageButton button = (ImageButton) getButonFromCoord(move.toCoord);
                  if (board.board[move.toCoord.rank][move.toCoord.file].isLight
                      && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus==Status.EMPTY) {
                    button.setImageResource(R.drawable.empty_light);
                  } else if (board.board[move.toCoord.rank][move.toCoord.file].PieceStatus==Status.EMPTY){
                    button.setImageResource(R.drawable.empty_dark);
                  }
                }
              } if (!cell.piece.equals(selectedPiece)) {
                selectedPiece = board.board[coord.rank][coord.file].piece;
                ArrayList<Move> legalMoves = selectedPiece.generateMoves(coord, board.board);
                for (Move move : GameService.filterMoves(legalMoves)) {
                  ImageButton button = (ImageButton) getButonFromCoord(move.toCoord);
                  if (board.board[move.toCoord.rank][move.toCoord.file].isLight
                      && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus==Status.EMPTY) {
                    button.setImageResource(R.drawable.white_empty_selected);
                  } else if (board.board[move.toCoord.rank][move.toCoord.file].PieceStatus==Status.EMPTY){
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


  public static Agent initializeAgent(String agentName) {
    Agent agent;
    switch (agentName) {
      case "Randy":
        agent = new Randy(AgentType.RANDY, false); //TODO: ASK USER IF THEY WANT TO PLAY AS WHITE OR BLACK!
        break;
      default:
        agent = new FishStock(AgentType.FISHSTOCK, false);
    }
    return agent;
  }

  public boolean isGameOver(Board board, boolean isWhite) {
    return false; //TODO:
  }
  public boolean isLegalMove(Coordinate coord, Cell[][] board) {
    if (selectedPiece == null){
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
  public ImageView getButonFromCoord(Coordinate coord) {
    int resId = getResources().getIdentifier("button" + (7-coord.rank) + (7-coord.file), "id", this.getPackageName());
    return findViewById(resId);
  }

  public static void defaultView(ArrayList<ImageButton> squares) {
  }
  public void updateBoard(Board board) {
    GridLayout grid = findViewById(R.id.gridlayout);

    for (int row=0; row<8; row++) {
      for (int col=0; col<8; col++) {
        Cell cell = board.board[row][col];
        updateCellImage(cell, row, col);
      }
    }
  }


  public void updateCellImage(Cell cell, int row, int col) {
    ImageView cellImageView = getButonFromCoord(new Coordinate(col, row));
    if (cell.isEmpty) {
      if (cell.isLight) {
        cellImageView.setImageResource(R.drawable.empty_light);
      } else {
        cellImageView.setImageResource(R.drawable.empty_dark);
      }
    } else if (cell.isWhite) {
      if (cell.isKing) {
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.white_king_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_king_on_dark);
        }
      } else if(cell.isQueen){
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.white_queen_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_queen_on_dark);
        }
      }else if (cell.isBishop) {
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.white_bishop_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_bishop_on_dark);
        }
      } else if (cell.isKnight){
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.white_knight_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_knight_on_dark);
        }
      } else if (cell.isRook) {
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.white_rook_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_rook_on_dark);
        }
      } else if (cell.isPawn) {
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.white_pawn_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.white_pawn_on_dark);
        }
      }
    } else {
      if (cell.isKing) {
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.black_king_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_king_on_dark);
        }
      } else if(cell.isQueen){
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.black_queen_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_queen_on_dark);
        }
      }else if (cell.isBishop) {
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.black_bishop_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_bishop_on_dark);
        }
      } else if (cell.isKnight){
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.black_knight_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_knight_on_dark);
        }
      } else if (cell.isRook) {
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.black_rook_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_rook_on_dark);
        }
      } else if (cell.isPawn) {
        if (cell.isLight){
          cellImageView.setImageResource(R.drawable.black_pawn_on_light);
        } else {
          cellImageView.setImageResource(R.drawable.black_pawn_on_dark);
        }
      }
    }
  }

}