package com.example.fishstock;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.fishstock.Agents.Agent;
import com.example.fishstock.Agents.AgentType;
import com.example.fishstock.Agents.Randy;
import com.example.fishstock.Pieces.Piece;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BoardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BoardFragment extends Fragment {

  // TODO: Rename parameter arguments, choose names that match
  // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
  private Board board;
  Agent adversary;
  Piece selectedPiece;
  boolean isWhite = true;
  boolean isGameOver = false;
  private View rootView;
  private ImageButton selectedButon;
  private MoveListener moveListener;
  private ImageButton button00;
  private ImageButton button01;
  private ImageButton button02;
  private ImageButton button03;
  private ImageButton button04;
  private ImageButton button05;
  private ImageButton button06;
  private ImageButton button07;
  private ImageButton button10;
  private ImageButton button11;
  private ImageButton button12;
  private ImageButton button13;
  private ImageButton button14;
  private ImageButton button15;
  private ImageButton button16;
  private ImageButton button17;
  private ImageButton button18;


  public BoardFragment() {
  }

  public void setMoveListener(MoveListener  moveListener) {
    this.moveListener = moveListener;
  }
  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   * @return A new instance of fragment BoardFragment.
   */
  // newInstance method with a Board argument
  public static BoardFragment newInstance(Board board, Agent adversary) {
    BoardFragment fragment = new BoardFragment();
    Bundle args = new Bundle();
    args.putSerializable("board", (Serializable) board);
    fragment.setArguments(args);
    fragment.board = board;
    fragment.adversary = adversary;
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Get the board object from the bundle
    Bundle bundle = getArguments();
    if (bundle != null) {
      board = (Board) bundle.getSerializable("board");
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    rootView = inflater.inflate(R.layout.fragment_board, container, false);
    if (adversary == null) {
      adversary = new Randy(AgentType.RANDY, false);
    }
    if (board == null) {
      Board board = new Board();
      this.board = board;
    }
    updateBoard(board);
    // initialize the onClick listeners for the buttons
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        ImageButton button = (ImageButton) getButonFromCoord(new Coordinate(col, row));
        button.setOnClickListener(new View.OnClickListener() {

          @Override
          public void onClick(View v) {
            Coordinate coord = getCoordFromButton(button);
            Cell cell = board.board[coord.rank][coord.file];
            if (isWhite && cell.PieceStatus == Status.EMPTY) {
              if (isLegalMove(coord, board.board)){
                Move move = new Move(selectedPiece.getPos(), coord, selectedPiece.getName(), false, true); //TODO: MAKE AN ISWHITE VARIABLE
                GameController.makeMove(board, move, true);
                GameController.updateBoardMeta(board);
                updateBoard(board);
                try {
                  ArrayList<Move> adversaryMoves = GameController.generateMoves(board, false);
                  ArrayList<Move> playersMoves = GameController.generateMoves(board, true);
                  Move adversaryMove = adversary.getMove(board, adversaryMoves, playersMoves);
                  GameController.makeMove(board, adversaryMove, false);
                  GameController.updateBoardMeta(board);
                  updateBoard(board);
                } catch (CloneNotSupportedException e) {
                  e.printStackTrace();
                }

              }
            } else if (isWhite && cell.PieceStatus == Status.BLACK){
              Move move = new Move(selectedPiece.getPos(), coord, selectedPiece.getName(), true, true); //TODO: MAKE AN ISWHITE VARIABLE
              move.setCapture(selectedPiece);
              GameController.makeMove(board, move, true);
            } else if (isWhite){
              if (selectedPiece != null) {
                for (Move move : GameController.filterMoves(selectedPiece.generateMoves(selectedPiece.getPos(), board.board))) {
                  ImageButton button = (ImageButton) getButonFromCoord(move.toCoord);
                  if (board.board[move.toCoord.rank][move.toCoord.file].isLight
                      && board.board[move.toCoord.rank][move.toCoord.file].PieceStatus==Status.EMPTY) {
                    button.setImageResource(R.drawable.empty_light);
                  } else {
                    button.setImageResource(R.drawable.empty_dark);
                  }
                }
              } if (!cell.piece.equals(selectedPiece)) {
                selectedPiece = board.board[coord.rank][coord.file].piece;
                ArrayList<Move> legalMoves = selectedPiece.generateMoves(coord, board.board);
                for (Move move : GameController.filterMoves(legalMoves)) {
                  ImageButton button = (ImageButton) getButonFromCoord(move.toCoord);
                  if (board.board[move.toCoord.rank][move.toCoord.file].isLight) {
                    button.setImageResource(R.drawable.white_empty_selected);
                  } else {
                    button.setImageResource(R.drawable.black_empty_selected);
                  }
                }
              }
            }
          }
        });
      }
    }
    return rootView;
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
    int resId = getResources().getIdentifier("button" + (7-coord.rank) + (7-coord.file), "id", getActivity().getPackageName());
    return rootView.findViewById(resId);
  }

  public static void defaultView(ArrayList<ImageButton> squares) {
  }
  public void updateBoard(Board board) {
    GridLayout grid = rootView.findViewById(R.id.gridlayout);

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