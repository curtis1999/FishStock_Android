package com.example.fishstock;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BoardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BoardFragment extends Fragment {

  // TODO: Rename parameter arguments, choose names that match
  // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
  private Board board;
  private View rootView;
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

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   * @return A new instance of fragment BoardFragment.
   */
  // newInstance method with a Board argument
  public static BoardFragment newInstance(Board board) {
    BoardFragment fragment = new BoardFragment();
    Bundle args = new Bundle();
    args.putSerializable("board", (Serializable) board);
    fragment.setArguments(args);
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

    // initialize the onClick listeners for the buttons
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        ImageButton button = (ImageButton) getCellImageView(row, col);
        button.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {

          }
        });
      }
    }
    return rootView;
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
    ImageView cellImageView = getCellImageView(7-row, 7-col);
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

  public ImageView getCellImageView(int row, int col) {
    int resId = getResources().getIdentifier("button" + row + col, "id", getActivity().getPackageName());
    return rootView.findViewById(resId);
  }
  public static void defaultView(ArrayList<ImageButton> squares) {

  }

  //TODO:
  public Coordinate nameToCoord(String name) {
    return new Coordinate(-1, -1);
  }
}