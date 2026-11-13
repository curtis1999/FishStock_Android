package com.example.fishstock;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * SAVED GAMES (home screen): your saved games, newest first, then the famous games.
 * Tap a game to review it on the analysis screen; long-press to rename or delete it.
 * IMPORT GAME pastes a PGN, asks for a name, saves it and opens it.
 */
public class SavedGamesActivity extends AppCompatActivity {
  private SavedGames store;
  private LinearLayout list;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_saved_games);
    store = new SavedGames(this);
    list = findViewById(R.id.savedGamesList);
    findViewById(R.id.savedImportButton).setOnClickListener(v -> new ImportGameDialog(this, this::onImported).show());
  }

  @Override
  protected void onResume() {
    super.onResume();
    fill(); // names may have changed on the analysis screen
  }

  private void onImported(PgnImport.Result game, boolean asWhite) {
    askName(this, "Save game as", game.title, name -> {
      String id = store.add(name, game.startFen, game.movesText(), asWhite ? 1 : -1);
      open(store.get(id));
    });
  }

  private void fill() {
    list.removeAllViews();
    List<SavedGames.Entry> games = store.all();
    boolean anyOwn = false;
    for (SavedGames.Entry e : games) anyOwn |= !e.famous;

    list.addView(header("YOUR GAMES"));
    if (!anyOwn) {
      list.addView(hint("Nothing saved yet. Press SAVE on the analysis screen, or IMPORT GAME above."));
    }
    for (SavedGames.Entry e : games) if (!e.famous) list.addView(row(e));

    boolean anyFamous = false;
    for (SavedGames.Entry e : games) anyFamous |= e.famous;
    if (anyFamous) {
      list.addView(header("FAMOUS GAMES"));
      for (SavedGames.Entry e : games) if (e.famous) list.addView(row(e));
    }
    list.addView(hint("Long-press a game to rename or delete it."));
  }

  private TextView header(String text) {
    TextView t = new TextView(this);
    t.setText(text);
    t.setTextSize(20);
    t.setPadding(0, dp(20), 0, dp(4));
    return t;
  }

  private TextView hint(String text) {
    TextView t = new TextView(this);
    t.setText(text);
    t.setTextSize(14);
    t.setAlpha(0.7f);
    t.setPadding(0, dp(8), 0, dp(8));
    return t;
  }

  /** Name; "Paris 1858 · 1-0 · 17 moves"; and for famous games, one line on why to study it. */
  private View row(SavedGames.Entry e) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.VERTICAL);
    row.setPadding(dp(4), dp(10), dp(4), dp(10));
    TypedValue ripple = new TypedValue();
    getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
    row.setBackgroundResource(ripple.resourceId);

    TextView name = new TextView(this);
    name.setText(e.name);
    name.setTextSize(17);
    row.addView(name);

    StringBuilder details = new StringBuilder();
    if (!e.event.isEmpty()) details.append(e.event).append("  ·  ");
    if (!e.result.isEmpty()) details.append(e.result).append("  ·  ");
    details.append(e.moveCount()).append(e.moveCount() == 1 ? " move" : " moves");
    if (e.side != 0) details.append("  ·  as ").append(e.side > 0 ? "White" : "Black");
    TextView sub = new TextView(this);
    sub.setText(details);
    sub.setTextSize(13);
    sub.setAlpha(0.7f);
    row.addView(sub);

    if (!e.note.isEmpty()) {
      TextView note = new TextView(this);
      note.setText(e.note);
      note.setTextSize(14);
      note.setPadding(0, dp(2), 0, 0);
      row.addView(note);
    }

    row.setOnClickListener(v -> open(e));
    row.setOnLongClickListener(v -> {
      showOptions(e);
      return true;
    });
    return row;
  }

  private void showOptions(SavedGames.Entry e) {
    new AlertDialog.Builder(this)
        .setTitle(e.name)
        .setItems(new String[] {"Rename", "Delete"}, (d, which) -> {
          if (which == 0) {
            askName(this, "Rename game", e.name, name -> {
              store.rename(e.id, name);
              fill();
            });
          } else {
            new AlertDialog.Builder(this)
                .setMessage("Delete \"" + e.name + "\"?")
                .setPositiveButton("Delete", (d2, w2) -> {
                  store.delete(e.id);
                  fill();
                })
                .setNegativeButton("Cancel", null)
                .show();
          }
        })
        .show();
  }

  private void open(SavedGames.Entry e) {
    if (e == null) return;
    startActivity(analysisIntent(this, e));
  }

  /** Opens a saved game on the analysis screen, from the side it was saved for. */
  public static Intent analysisIntent(Context context, SavedGames.Entry e) {
    Intent intent = new Intent(context, GameAnalysis.class);
    if (e.startFen != null) intent.putExtra(GameAnalysis.EXTRA_START_FEN, e.startFen);
    intent.putExtra(GameAnalysis.EXTRA_MOVES, e.movesUci);
    intent.putExtra(GameAnalysis.EXTRA_TITLE, e.name);
    intent.putExtra(GameAnalysis.EXTRA_TRAIN_SIDE, e.side);
    intent.putExtra(GameAnalysis.EXTRA_FLIPPED, e.side < 0);
    intent.putExtra(GameAnalysis.EXTRA_SAVED_ID, e.id);
    return intent;
  }

  public interface NameListener {
    void onName(String name);
  }

  /** A small "name this game" box, prefilled and selected so it can be typed over. */
  public static void askName(Context context, String title, String suggested, NameListener listener) {
    final EditText input = new EditText(context);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    input.setSingleLine(true);
    input.setText(suggested == null ? "" : suggested);
    input.selectAll();
    FrameLayout box = new FrameLayout(context);
    int pad = Math.round(20 * context.getResources().getDisplayMetrics().density);
    box.setPadding(pad, pad / 2, pad, 0);
    box.addView(input);
    AlertDialog dialog = new AlertDialog.Builder(context)
        .setTitle(title)
        .setView(box)
        .setPositiveButton("Save", (d, w) -> listener.onName(input.getText().toString()))
        .setNegativeButton("Cancel", null)
        .create();
    dialog.setOnShowListener(d -> input.requestFocus());
    if (dialog.getWindow() != null) {
      dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
    dialog.show();
  }

  private int dp(int v) {
    return Math.round(v * getResources().getDisplayMetrics().density);
  }
}
