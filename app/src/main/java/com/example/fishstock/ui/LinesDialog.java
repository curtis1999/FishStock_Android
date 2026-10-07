package com.example.fishstock.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.example.fishstock.R;
import com.example.fishstock.analysis.PositionProfile;
import com.example.fishstock.search.ScoredMove;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * VIEW LINES on the analysis screen: the evaluation of every legal move as a bar chart
 * ({@link MoveSpectrumView}), how many moves are good / level / bad, and what kind of position
 * that makes it ({@link PositionProfile}: puzzle, sharp, calm, ...).
 *
 * Everything here is from the point of view of the side to move: bars above the line are moves
 * that leave it better off. The screen fills it in as the background analysis finishes.
 */
public final class LinesDialog extends Dialog {
  private static final float WIDTH_SHARE = 0.94f;

  private final boolean whiteToMove;
  private final String side;
  private final MoveSpectrumView spectrum;
  private final TextView subtitle;
  private final TextView caption;
  private final TextView counts;
  private final TextView label;
  private final TextView labelDescription;
  private final TextView stats;
  private final TextView steps;
  private final List<String> names = new ArrayList<>();
  private List<ScoredMove> moves = new ArrayList<>();

  public LinesDialog(Context context, boolean whiteToMove) {
    super(context);
    this.whiteToMove = whiteToMove;
    this.side = whiteToMove ? "White" : "Black";
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.dialog_view_lines);
    spectrum = findViewById(R.id.moveSpectrum);
    subtitle = findViewById(R.id.linesSubtitle);
    caption = findViewById(R.id.spectrumCaption);
    counts = findViewById(R.id.linesCounts);
    label = findViewById(R.id.positionLabel);
    labelDescription = findViewById(R.id.positionLabelDescription);
    stats = findViewById(R.id.linesStats);
    steps = findViewById(R.id.linesSteps);
    findViewById(R.id.closeLines).setOnClickListener(v -> dismiss());
    spectrum.setOnBarSelectedListener(this::describeMove);
    subtitle.setText(side + " to move. Scoring every move...");
  }

  @Override
  protected void onStart() {
    super.onStart();
    Window window = getWindow();
    if (window == null) return;
    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
    window.setLayout(Math.round(metrics.widthPixels * WIDTH_SHARE), ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  /** New scores for every move (best first) with their names; redraws the chart. */
  public void showMoves(List<ScoredMove> scored, List<String> moveNames, int depth, boolean done) {
    moves = new ArrayList<>(scored);
    names.clear();
    names.addAll(moveNames);
    double[] s = new double[moves.size()];
    int good = 0;
    int level = 0;
    int bad = 0;
    for (int i = 0; i < s.length; i++) {
      ScoredMove m = moves.get(i);
      s[i] = m.isMate() ? Math.signum(m.score) * PositionProfile.MATE_AS_PAWNS : m.score;
      if (s[i] > PositionProfile.EQUAL_BAND) good++;
      else if (s[i] < -PositionProfile.EQUAL_BAND) bad++;
      else level++;
    }
    spectrum.setMoves(names, s);
    subtitle.setText(String.format(Locale.US, "%s to move. %d legal moves, searched %d ply%s deep%s.\nBars above the line are good for %s.",
        side, s.length, depth, depth == 1 ? "" : "s", done ? "" : " (refining...)", side));
    counts.setText(String.format(Locale.US, "%d move%s keep %s better, %d %s about level, %d leave %s worse.",
        good, good == 1 ? "" : "s", side, level, level == 1 ? "is" : "are", bad, side));
    describeMove(0);
  }

  /** The finished profile: labels, statistics and the good-move counts down the best line. */
  public void showProfile(PositionProfile p) {
    label.setText(p.labelText());
    StringBuilder d = new StringBuilder();
    for (PositionProfile.Label l : p.labels) {
      if (d.length() > 0) d.append('\n');
      d.append(l.description);
    }
    labelDescription.setText(d.toString());
    stats.setText(String.format(Locale.US,
        "Best %s, next best %s, average %+.2f.\nSpread (standard deviation) %.2f pawns: %s\n%d of %d moves are within half a pawn of the best.",
        pawns(p.best), pawns(p.second), p.mean, p.stdDev, spreadWords(p.stdDev), p.goodMoves, p.moveCount));
    steps.setText(p.stepsText());
  }

  private void describeMove(int i) {
    if (i < 0 || i >= moves.size() || i >= names.size()) return;
    spectrum.select(i);
    ScoredMove m = moves.get(i);
    String score;
    if (m.isMate()) {
      int n = Math.abs(m.mateIn());
      score = m.mateIn() > 0 ? side + " mates in " + n : side + " gets mated in " + n;
    } else {
      score = String.format(Locale.US, "%+.2f for %s", m.score, side);
    }
    String rank = i == 0 ? "best" : String.format(Locale.US, "#%d of %d", i + 1, moves.size());
    caption.setText(names.get(i) + "   " + score + "   (" + rank + ")");
  }

  private static String pawns(double v) {
    if (Math.abs(v) >= PositionProfile.MATE_AS_PAWNS) return v > 0 ? "mate" : "mated";
    return String.format(Locale.US, "%+.2f", v);
  }

  private static String spreadWords(double sd) {
    if (sd < 0.7) return "most moves are much alike.";
    if (sd < 2.0) return "choices matter.";
    return "mistakes are costly here.";
  }

  public boolean whiteToMove() {
    return whiteToMove;
  }
}
