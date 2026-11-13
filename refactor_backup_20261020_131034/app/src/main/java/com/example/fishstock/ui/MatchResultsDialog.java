package com.example.fishstock.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.fishstock.R;
import com.example.fishstock.arena.GameRecord;
import com.example.fishstock.arena.MatchResult;

import java.util.List;
import java.util.Locale;

/**
 * VIEW RESULTS on the Tournament screen: every game of the match (who had White, the result and
 * how it ended), each with VIEW GAME. SHARE AS TEXT sends the summary and history elsewhere.
 */
public final class MatchResultsDialog extends Dialog {
  public interface Listener {
    void onViewGame(GameRecord game, int number);

    void onShare();
  }

  public MatchResultsDialog(Context context, MatchResult match, final Listener listener) {
    super(context);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.dialog_match_results);
    float density = context.getResources().getDisplayMetrics().density;

    ((TextView) findViewById(R.id.matchSummary)).setText(String.format(Locale.US,
        "%s %d  -  %d %s   (%d draw%s)", match.nameA, match.winsA(), match.winsB(), match.nameB,
        match.draws(), match.draws() == 1 ? "" : "s"));
    LinearLayout list = findViewById(R.id.matchGames);
    List<GameRecord> games = match.games();
    for (int i = 0; i < games.size(); i++) {
      final GameRecord g = games.get(i);
      final int number = i + 1;
      LinearLayout row = new LinearLayout(context);
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setGravity(Gravity.CENTER_VERTICAL);
      row.setPadding(0, Math.round(4 * density), 0, Math.round(4 * density));

      TextView text = new TextView(context);
      text.setTextColor(0xFF000000);
      text.setTextSize(15);
      text.setText(describe(g, number));
      row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

      Button view = new Button(context);
      view.setText("VIEW GAME");
      view.setTextSize(13);
      view.setEnabled(!g.movesUci.isEmpty());
      view.setOnClickListener(v -> listener.onViewGame(g, number));
      row.addView(view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT));
      list.addView(row);
    }
    if (games.isEmpty()) {
      TextView none = new TextView(context);
      none.setTextColor(0xFF000000);
      none.setText("No games finished yet.");
      list.addView(none);
    }
    findViewById(R.id.shareResults).setOnClickListener(v -> listener.onShare());
    findViewById(R.id.closeResults).setOnClickListener(v -> dismiss());
  }

  /** "Game 3: Agro (White) 1-0 Timid\n checkmate, 54 moves" */
  private static String describe(GameRecord g, int number) {
    String how = GameOverDialog.describe(g.result);
    return String.format(Locale.US, "Game %d: %s (White) %s %s\n%s%d moves", number, g.whiteName, g.scoreText(),
        g.blackName, how.isEmpty() ? "" : how + ", ", (g.plies + 1) / 2);
  }

  @Override
  protected void onStart() {
    super.onStart();
    Window window = getWindow();
    if (window == null) return;
    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    DisplayMetrics m = getContext().getResources().getDisplayMetrics();
    window.setLayout(Math.round(m.widthPixels * 0.94f), Math.round(m.heightPixels * 0.8f));
  }
}
