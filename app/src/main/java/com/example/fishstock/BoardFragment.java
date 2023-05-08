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
    ArrayList<ImageButton> squares = new ArrayList<>();
    button00 = rootView.findViewById(R.id.button00);
    button01 = rootView.findViewById(R.id.button01);
    button02 = rootView.findViewById(R.id.button02);
    button03 = rootView.findViewById(R.id.button03);
    button04 = rootView.findViewById(R.id.button04);
    button05 = rootView.findViewById(R.id.button05);
    button06 = rootView.findViewById(R.id.button06);
    button07 = rootView.findViewById(R.id.button07);
    button10 = rootView.findViewById(R.id.button10);
    button11 = rootView.findViewById(R.id.button11);
    button12 = rootView.findViewById(R.id.button12);
    button13 = rootView.findViewById(R.id.button13);
    button14 = rootView.findViewById(R.id.button14);
    button15 = rootView.findViewById(R.id.button15);
    button16 = rootView.findViewById(R.id.button16);
    button17 = rootView.findViewById(R.id.button17);
    squares.add(button00); squares.add(button01);
    button00.setImageResource(R.drawable.black_rook_on_light);
    button01.setImageResource(R.drawable.black_knight_on_dark);
    button02.setImageResource(R.drawable.black_bishop_on_light);
    button03.setImageResource(R.drawable.black_queen_on_dark);
    button04.setImageResource(R.drawable.black_king_on_light);
    button05.setImageResource(R.drawable.black_bishop_on_dark);
    button06.setImageResource(R.drawable.black_knight_on_light);
    button07.setImageResource(R.drawable.black_rook_on_dark);
    button10.setImageResource(R.drawable.black_pawn_on_dark);
    button11.setImageResource(R.drawable.black_pawn_on_light);
    button12.setImageResource(R.drawable.black_pawn_on_dark);
    button13.setImageResource(R.drawable.black_pawn_on_light);
    button14.setImageResource(R.drawable.black_pawn_on_dark);
    button15.setImageResource(R.drawable.black_pawn_on_light);
    button16.setImageResource(R.drawable.black_pawn_on_dark);
    button17.setImageResource(R.drawable.black_pawn_on_light);
    button10.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

      }
    });
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
    ImageView cellImageView = getCellImageView(row, col);
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

        } else {

        }
      } else if (cell.isRook) {
        if (cell.isLight){

        } else {

        }
      } else if (cell.isPawn) {
        if (cell.isLight){

        } else {

        }
      }
    } else {
      if (cell.isKing) {

      }
    }
  }
  public ImageView getCellImageView(int row, int col) {
    int resId = getResources().getIdentifier("cell_" + row + "_" + col, "id", getActivity().getPackageName());
    return rootView.findViewById(resId);
  }
  public static void defaultView(ArrayList<ImageButton> squares) {

  }

  //TODO:
  public Coordinate nameToCoord(String name) {
    return new Coordinate(-1, -1);
  }
}