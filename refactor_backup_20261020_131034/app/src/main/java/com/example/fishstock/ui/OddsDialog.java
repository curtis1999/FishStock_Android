package com.example.fishstock.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.fishstock.engine.Odds;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Square;

import java.util.Set;

/**
 * Choose the agent's odds: its two starting ranks, drawn the way they'll sit on your board, and
 * you tap the pieces it should start without (tap again to put one back). The king stays.
 */
public final class OddsDialog {

  public interface Listener {
    /** The chosen odds (see {@link Odds}); {@link Odds#NONE} for a normal game. */
    void onOddsChosen(String odds);
  }

  private final Context context;
  private final boolean agentWhite;
  private final Set<Integer> removed; // White's side squares (a1..h2)
  private final ImageView[] cells = new ImageView[16];
  private final int[] cellSquares = new int[16]; // the agent's real squares
  private TextView summary;

  public OddsDialog(Context context, boolean agentWhite, String odds) {
    this.context = context;
    this.agentWhite = agentWhite;
    this.removed = Odds.parse(odds);
  }

  public void show(final Listener listener) {
    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setGravity(Gravity.CENTER_HORIZONTAL);
    int pad = dp(12);
    root.setPadding(pad, pad, pad, 0);

    TextView hint = new TextView(context);
    hint.setText("Tap the pieces the agent starts without.");
    hint.setTextSize(16);
    hint.setGravity(Gravity.CENTER);
    root.addView(hint);

    GridLayout grid = new GridLayout(context);
    grid.setColumnCount(8);
    grid.setRowCount(2);
    LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    gp.topMargin = dp(10);
    root.addView(grid, gp);

    // You see the agent's pieces across the board: White plays from the bottom, so an agent
    // playing Black has rank 8 on top (a..h); an agent playing White is seen from Black's side.
    int size = Math.min(dp(38), (context.getResources().getDisplayMetrics().widthPixels - dp(96)) / 8);
    Position start = Position.fromFen(Position.START_FEN);
    for (int row = 0; row < 2; row++) {
      for (int col = 0; col < 8; col++) {
        int rank = agentWhite ? row : 7 - row;
        int file = agentWhite ? 7 - col : col;
        final int sq = Square.of(file, rank);
        final int i = row * 8 + col;
        cellSquares[i] = sq;
        ImageView cell = new ImageView(context);
        cell.setScaleType(ImageView.ScaleType.FIT_XY);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = size;
        lp.height = size;
        cell.setLayoutParams(lp);
        cell.setOnClickListener(v -> toggle(i, start));
        cells[i] = cell;
        grid.addView(cell);
      }
    }

    summary = new TextView(context);
    summary.setTextSize(15);
    summary.setGravity(Gravity.CENTER);
    summary.setMinLines(2);
    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    sp.topMargin = dp(10);
    root.addView(summary, sp);

    redraw(start);

    final AlertDialog dialog = new AlertDialog.Builder(context)
        .setTitle("ODDS")
        .setView(root)
        .setPositiveButton("OK", (d, w) -> listener.onOddsChosen(Odds.format(removed)))
        .setNegativeButton("CANCEL", null)
        .setNeutralButton("RESET", null)
        .show();
    // RESET puts every piece back without closing the box.
    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
      removed.clear();
      redraw(start);
    });
  }

  private void toggle(int i, Position start) {
    int sq = cellSquares[i];
    if (Piece.type(start.pieceAt(sq)) == Piece.KING) {
      summary.setText("The king has to stay!");
      return;
    }
    int key = Odds.whiteSideSquare(sq);
    if (!removed.remove(key)) removed.add(key);
    redraw(start);
  }

  private void redraw(Position start) {
    for (int i = 0; i < 16; i++) {
      int sq = cellSquares[i];
      boolean gone = removed.contains(Odds.whiteSideSquare(sq));
      int piece = gone ? Piece.EMPTY : start.pieceAt(sq);
      cells[i].setImageResource(BoardRenderer.pieceDrawable(piece, Square.isLight(sq)));
    }
    String what = Odds.describe(Odds.format(removed), agentWhite);
    summary.setText(what.isEmpty() ? "No odds: a normal game." : "The agent starts without its " + what + ".");
  }

  private int dp(int v) {
    return Math.round(v * context.getResources().getDisplayMetrics().density);
  }
}
