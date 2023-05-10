package com.example.fishstock;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.fishstock.Pieces.*;

public class PromotionDialog extends Dialog {
  private final Move move;
  private ImageButton promotionQueen, promotionRook, promotionBishop, promotionKnight;

  public PromotionDialog(Context context, Move move) {
    super(context);
    this.move = move;
    setContentView(R.layout.dialog_promotion);
    promotionQueen = findViewById(R.id.promotionQueen);
    promotionRook = findViewById(R.id.promotionRook);
    promotionBishop = findViewById(R.id.promotionBishop);
    promotionKnight = findViewById(R.id.promotionKnight);

    promotionQueen.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        move.setPromotion(new Queen(move.toCoord, true));
        dismiss();
      }
    });

    promotionRook.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        move.setPromotion(new Rook(move.toCoord, true));
        dismiss();
      }
    });

    promotionBishop.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        move.setPromotion(new Bishop(move.toCoord, true));
        dismiss();
      }
    });

    promotionKnight.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        move.setPromotion(new Knight(move.toCoord, true));
        dismiss();
      }
    });
  }
}
