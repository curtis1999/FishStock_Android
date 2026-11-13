package com.example.fishstock.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.fishstock.R;

/** "GAME OVER" box with EXIT / ANALYZE / PLAY AGAIN. The game screen decides what each button does. */
public final class GameOverDialog extends Dialog {

  public interface Listener {
    void onExit();

    void onAnalyze();

    void onPlayAgain();
  }

  public GameOverDialog(Context context, String headline, final Listener listener) {
    super(context);
    setContentView(R.layout.dialog_game_over);
    ((TextView) findViewById(R.id.winnerMessage)).setText(headline);
    Button exit = findViewById(R.id.exit);
    Button analyze = findViewById(R.id.analyze);
    Button playAgain = findViewById(R.id.playAgain);
    exit.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dismiss();
        listener.onExit();
      }
    });
    analyze.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dismiss();
        listener.onAnalyze();
      }
    });
    playAgain.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dismiss();
        listener.onPlayAgain();
      }
    });
  }
}
