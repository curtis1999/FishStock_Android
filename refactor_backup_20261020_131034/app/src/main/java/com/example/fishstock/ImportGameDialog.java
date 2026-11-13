package com.example.fishstock;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * IMPORT GAME: paste a PGN or a plain move list, choose whose side to review it from, ANALYZE.
 * The text is read by {@link PgnImport}; if a move can't be read the box stays open and says
 * which one, so it can be fixed in place.
 */
public final class ImportGameDialog extends Dialog {
  private static final float WIDTH_SHARE = 0.92f;

  public interface Listener {
    /** @param asWhite true to review from White's side, false from Black's */
    void onImport(PgnImport.Result game, boolean asWhite);
  }

  private boolean asWhite = true;

  public ImportGameDialog(Context context, final Listener listener) {
    super(context);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.dialog_import_game);

    final EditText text = findViewById(R.id.importText);
    final TextView error = findViewById(R.id.importError);
    final Button white = findViewById(R.id.importWhite);
    final Button black = findViewById(R.id.importBlack);

    white.setOnClickListener(v -> {
      asWhite = true;
      white.setTextColor(Color.WHITE);
      black.setTextColor(Color.BLACK);
    });
    black.setOnClickListener(v -> {
      asWhite = false;
      black.setTextColor(Color.WHITE);
      white.setTextColor(Color.BLACK);
    });

    findViewById(R.id.importPaste).setOnClickListener(v -> {
      ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      ClipData clip = cm == null ? null : cm.getPrimaryClip();
      if (clip == null || clip.getItemCount() == 0) {
        showError(error, "Nothing to paste: copy the game first.");
        return;
      }
      CharSequence pasted = clip.getItemAt(0).coerceToText(context);
      text.setText(pasted);
      text.setSelection(0);
      error.setVisibility(View.GONE);
    });

    findViewById(R.id.importCancel).setOnClickListener(v -> dismiss());
    findViewById(R.id.importAnalyze).setOnClickListener(v -> {
      PgnImport.Result r = PgnImport.parse(text.getText().toString());
      if (!r.ok()) {
        showError(error, r.error);
        return;
      }
      dismiss();
      listener.onImport(r, asWhite);
    });
  }

  private static void showError(TextView error, String message) {
    error.setText(message);
    error.setVisibility(View.VISIBLE);
  }

  @Override
  protected void onStart() {
    super.onStart();
    Window window = getWindow();
    if (window == null) return;
    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // let the rounded card show
    DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
    window.setLayout(Math.round(metrics.widthPixels * WIDTH_SHARE), ViewGroup.LayoutParams.WRAP_CONTENT);
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  }
}
