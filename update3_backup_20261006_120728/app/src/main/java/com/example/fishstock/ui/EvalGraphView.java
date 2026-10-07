package com.example.fishstock.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Arrays;

/**
 * The evaluation through the whole game, like chess.com's graph: the top half is White's side,
 * the bottom half Black's. After each move there is a node a little further right, higher when
 * White is better and lower when Black is. Tap or drag on the graph to jump to that move.
 *
 * Big swings (a move that threw away 2+ pawns) get a red node; the move on the board gets a
 * purple node and a vertical marker. Scores are squashed so a mate doesn't flatten everything else.
 */
public final class EvalGraphView extends View {
  public interface OnPlySelectedListener {
    void onPlySelected(int ply);
  }

  /** Scores further from 0 than this barely move the line any more. */
  private static final double SCALE_PAWNS = 2.5;
  private static final double BLUNDER_SWING = 2.0;
  private static final int CURRENT = 0xFF6200EE; // the app's purple
  private static final int BLUNDER = 0xFFE40D0D; // the app's red

  private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint node = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path path = new Path();
  private final float density;

  private double[] scores = new double[0]; // White's view, NaN = not computed yet
  private int currentPly = -1;
  private boolean whiteMovesFirst = true;
  private OnPlySelectedListener listener;

  public EvalGraphView(Context context) {
    this(context, null);
  }

  public EvalGraphView(Context context, AttributeSet attrs) {
    super(context, attrs);
    density = context.getResources().getDisplayMetrics().density;
    line.setStyle(Paint.Style.STROKE);
    line.setStrokeWidth(2 * density);
    line.setColor(0xFF8A8A8A);
    border.setStyle(Paint.Style.STROKE);
    border.setStrokeWidth(density);
    border.setColor(0xFF808080);
  }

  public void setOnPlySelectedListener(OnPlySelectedListener l) {
    this.listener = l;
  }

  /** Number of positions in the game (moves + 1), and who made the first move. Scores start unknown. */
  public void setPlyCount(int positions, boolean whiteMovesFirst) {
    this.whiteMovesFirst = whiteMovesFirst;
    scores = new double[Math.max(1, positions)];
    Arrays.fill(scores, Double.NaN);
    invalidate();
  }

  /** Score of position {@code ply} in pawns from White's point of view. */
  public void setScore(int ply, double whiteScore) {
    if (ply < 0 || ply >= scores.length) return;
    scores[ply] = whiteScore;
    invalidate();
  }

  public void setCurrentPly(int ply) {
    currentPly = ply;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float w = getWidth();
    float h = getHeight();
    float mid = h / 2f;
    fill.setColor(Color.WHITE);
    canvas.drawRect(0, 0, w, mid, fill);
    fill.setColor(Color.BLACK);
    canvas.drawRect(0, mid, w, h, fill);
    canvas.drawRect(0, 0, w, h, border);

    int n = scores.length;
    if (n == 0) return;
    float pad = 6 * density;

    if (currentPly >= 0 && currentPly < n) {
      node.setColor(0x806200EE);
      float x = xFor(currentPly, n, w, pad);
      canvas.drawRect(x - density / 2, 0, x + density / 2, h, node);
    }

    path.reset();
    boolean started = false;
    for (int i = 0; i < n; i++) {
      if (Double.isNaN(scores[i])) {
        started = false;
        continue;
      }
      float x = xFor(i, n, w, pad);
      float y = yFor(scores[i], mid, pad);
      if (!started) path.moveTo(x, y);
      else path.lineTo(x, y);
      started = true;
    }
    canvas.drawPath(path, line);

    float r = n > 80 ? 1.8f * density : 2.6f * density;
    for (int i = 0; i < n; i++) {
      if (Double.isNaN(scores[i])) continue;
      float x = xFor(i, n, w, pad);
      float y = yFor(scores[i], mid, pad);
      boolean blunder = i > 0 && !Double.isNaN(scores[i - 1]) && isBlunder(i);
      if (i == currentPly) {
        node.setColor(CURRENT);
        canvas.drawCircle(x, y, r * 1.8f, node);
      } else if (blunder) {
        node.setColor(BLUNDER);
        canvas.drawCircle(x, y, r * 1.4f, node);
      } else {
        node.setColor(0xFF8A8A8A);
        canvas.drawCircle(x, y, r, node);
      }
    }
  }

  /** The move into position i lost 2+ pawns for the side that played it. */
  private boolean isBlunder(int i) {
    double before = clamp(scores[i - 1]);
    double after = clamp(scores[i]);
    boolean whiteMoved = whiteMovesFirst == (i % 2 == 1);
    double loss = whiteMoved ? before - after : after - before;
    return loss >= BLUNDER_SWING;
  }

  private static double clamp(double s) {
    return Math.max(-10, Math.min(10, s));
  }

  private static float xFor(int i, int n, float w, float pad) {
    if (n == 1) return w / 2f;
    return pad + (w - 2 * pad) * i / (float) (n - 1);
  }

  private static float yFor(double score, float mid, float pad) {
    double squashed = Math.signum(score) * (1 - Math.exp(-Math.abs(score) / SCALE_PAWNS));
    return (float) (mid - squashed * (mid - pad));
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    int action = e.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
      if (action == MotionEvent.ACTION_DOWN && getParent() != null) {
        getParent().requestDisallowInterceptTouchEvent(true); // dragging along the graph, not scrolling
      }
      int n = scores.length;
      if (n == 0) return true;
      float pad = 6 * density;
      float t = (e.getX() - pad) / Math.max(1f, getWidth() - 2 * pad);
      int ply = Math.max(0, Math.min(n - 1, Math.round(t * (n - 1))));
      if (ply != currentPly && listener != null) listener.onPlySelected(ply);
      return true;
    }
    if (action == MotionEvent.ACTION_UP) {
      performClick();
      return true;
    }
    return action == MotionEvent.ACTION_CANCEL || super.onTouchEvent(e);
  }

  @Override
  public boolean performClick() {
    return super.performClick();
  }
}
