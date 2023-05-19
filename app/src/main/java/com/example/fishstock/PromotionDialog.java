package com.example.fishstock;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.fishstock.Pieces.*;



public class PromotionDialog extends Dialog {

  public interface OnPromotionMoveListener {
    void onPromotionMove() throws CloneNotSupportedException;
  }
  private Board board;
  private final Move move;
  boolean isWhite;
  private ImageButton promotionQueen, promotionRook, promotionBishop, promotionKnight;
  private OnPromotionMoveListener listener;

  public void setOnPromotionMoveListener(OnPromotionMoveListener listener) {
    this.listener = listener;
  }
  public PromotionDialog(Context context, Board board, Move move, boolean isWhite) {
    super(context);
    this.board = board;
    this.move = move;
    this.isWhite = isWhite;
    setContentView(R.layout.dialog_promotion);
    promotionQueen = findViewById(R.id.promotionQueen);
    promotionRook = findViewById(R.id.promotionRook);
    promotionBishop = findViewById(R.id.promotionBishop);
    promotionKnight = findViewById(R.id.promotionKnight);
    if (!isWhite) {
      promotionQueen.setImageResource(R.drawable.promotion_queen_black);
      promotionRook.setImageResource(R.drawable.promotion_rook_black);
      promotionBishop.setImageResource(R.drawable.promotion_bishop_black);
      promotionKnight.setImageResource(R.drawable.promotion_knight_black);
    }
    promotionQueen.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        move.setPromotion(new Queen(move.toCoord, isWhite));
        GameService.makeMove(board, move, isWhite);
        GameService.updateBoardMeta(board);
        if (listener != null) {
          try {
            listener.onPromotionMove();
          } catch (CloneNotSupportedException e) {
            e.printStackTrace();
          }
        }
        dismiss();
      }
    });

    promotionRook.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        move.setPromotion(new Rook(move.toCoord, isWhite));
        GameService.makeMove(board, move, isWhite);
        GameService.updateBoardMeta(board);
        if (listener != null) {
          try {
            listener.onPromotionMove();
          } catch (CloneNotSupportedException e) {
            e.printStackTrace();
          }
        }
        dismiss();
      }
    });

    promotionBishop.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        move.setPromotion(new Bishop(move.toCoord, isWhite));
        GameService.makeMove(board, move, isWhite);
        GameService.updateBoardMeta(board);
        if (listener != null) {
          try {
            listener.onPromotionMove();
          } catch (CloneNotSupportedException e) {
            e.printStackTrace();
          }
        }
        dismiss();
      }
    });

    promotionKnight.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        move.setPromotion(new Knight(move.toCoord, isWhite));
        GameService.makeMove(board, move, isWhite);
        GameService.updateBoardMeta(board);
        if (listener != null) {
          try {
            listener.onPromotionMove();
          } catch (CloneNotSupportedException e) {
            e.printStackTrace();
          }
        }
        dismiss();
      }
    });
  }
}
