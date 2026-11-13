package com.example.fishstock.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.example.fishstock.R;
import com.example.fishstock.engine.GameResult;

/**
 * "GAME OVER" box with PLAY AGAIN / ANALYZE / EXIT. The game screen decides what each button does.
 *
 * The window is ~90% of the screen width (a plain Dialog wraps its content, which is what used
 * to squash the text and buttons together on a phone).
 */
public final class GameOverDialog extends Dialog {
  private static final float WIDTH_SHARE = 0.9f;

  public interface Listener {
    void onExit();

    void onAnalyze();

    void onPlayAgain();
  }

  public GameOverDialog(Context context, String headline, Listener listener) {
    this(context, headline, "", listener);
  }

  /**
   * @param headline "YOU WIN!! :)", "DRAW :|", ...
   * @param reason   how it ended, e.g. "by checkmate" (see {@link #describe}); may be empty
   */
  public GameOverDialog(Context context, String headline, String reason, final Listener listener) {
    super(context);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.dialog_game_over);
    setCanceledOnTouchOutside(false); // a stray tap on the board shouldn't throw the box away
    ((TextView) findViewById(R.id.winnerMessage)).setText(headline);
    TextView reasonText = findViewById(R.id.gameOverReason);
    reasonText.setText(reason == null ? "" : reason);
    reasonText.setVisibility(reason == null || reason.isEmpty() ? View.GONE : View.VISIBLE);

    Button exit = findViewById(R.id.exit);
    Button analyze = findViewById(R.id.analyze);
    Button playAgain = findViewById(R.id.playAgain);
    exit.setOnClickListener(v -> {
      dismiss();
      listener.onExit();
    });
    analyze.setOnClickListener(v -> {
      dismiss();
      listener.onAnalyze();
    });
    playAgain.setOnClickListener(v -> {
      dismiss();
      listener.onPlayAgain();
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    Window window = getWindow();
    if (window == null) return;
    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // let the rounded card show
    DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
    window.setLayout(Math.round(metrics.widthPixels * WIDTH_SHARE), ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  /** "by checkmate", "by threefold repetition", "White resigned", ... */
  public static String describe(GameResult r) {
    switch (r) {
      case WHITE_CHECKMATES:
      case BLACK_CHECKMATES:
        return "by checkmate";
      case STALEMATE:
        return "by stalemate";
      case THREEFOLD_REPETITION:
        return "by threefold repetition";
      case FIFTY_MOVE_RULE:
        return "by the 50-move rule";
      case INSUFFICIENT_MATERIAL:
        return "by insufficient material";
      case DRAW_AGREED:
        return "by agreement";
      case WHITE_RESIGNS:
        return "White resigned";
      case BLACK_RESIGNS:
        return "Black resigned";
      case ADJUDICATED_WHITE:
      case ADJUDICATED_BLACK:
      case ADJUDICATED_DRAW:
        return "on adjudication";
      default:
        return "";
    }
  }
}
