package com.example.fishstock.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ImageButton;

import com.example.fishstock.R;
import com.example.fishstock.engine.Piece;

/** Asks which piece a pawn promotes to. It only reports the choice; the game screen plays the move. */
public final class PromotionDialog extends Dialog {

  public interface Listener {
    /** @param pieceType Piece.QUEEN, ROOK, BISHOP or KNIGHT, or 0 if the dialog was dismissed */
    void onPromotionChosen(int pieceType);
  }

  private boolean answered;

  public PromotionDialog(Context context, boolean white, final Listener listener) {
    super(context);
    setContentView(R.layout.dialog_promotion);
    ImageButton queen = findViewById(R.id.promotionQueen);
    ImageButton rook = findViewById(R.id.promotionRook);
    ImageButton bishop = findViewById(R.id.promotionBishop);
    ImageButton knight = findViewById(R.id.promotionKnight);
    if (!white) {
      queen.setImageResource(R.drawable.promotion_queen_black);
      rook.setImageResource(R.drawable.promotion_rook_black);
      bishop.setImageResource(R.drawable.promotion_bishop_black);
      knight.setImageResource(R.drawable.promotion_knight_black);
    }
    bind(queen, Piece.QUEEN, listener);
    bind(rook, Piece.ROOK, listener);
    bind(bishop, Piece.BISHOP, listener);
    bind(knight, Piece.KNIGHT, listener);
    setOnDismissListener(new DialogInterface.OnDismissListener() {
      @Override
      public void onDismiss(DialogInterface d) {
        if (!answered) {
          answered = true;
          listener.onPromotionChosen(0);
        }
      }
    });
  }

  private void bind(ImageButton button, final int type, final Listener listener) {
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (answered) return;
        answered = true;
        listener.onPromotionChosen(type);
        dismiss();
      }
    });
  }
}
