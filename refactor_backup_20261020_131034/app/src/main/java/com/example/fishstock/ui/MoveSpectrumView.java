package com.example.fishstock.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Every legal move as one bar, best on the left, worst on the right. Bars above the line are
 * moves after which the side to move is better, bars below are moves that leave it worse.
 * Green = good for the mover, red = bad, grey = roughly level. Moves within half a pawn of the best
 * move are drawn solid; the rest are paler, so the number of good moves stands out at a glance.
 *
 * The bars grow in from the line when new scores arrive. Tap a bar to select that move.
 */
public final class MoveSpectrumView extends View {
  public interface OnBarSelectedListener {
    void onBarSelected(int index);
  }

  private static final double SCALE_PAWNS = 2.5;
  private static final double EQUAL_BAND = 0.3;
  private static final double GOOD_MARGIN = 0.5;
  private static final int GREEN = 0xFF12CC2B; // colors.xml "green"
  private static final int RED = 0xFFE40D0D;   // colors.xml "red"
  private static final int GREY = 0xFF9E9E9E;

  private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final float density;

  private final List<String> names = new ArrayList<>();
  private double[] scores = new double[0];
  private int selected = -1;
  private float progress = 1f;
  private ValueAnimator animator;
  private OnBarSelectedListener listener;

  public MoveSpectrumView(Context context) {
    this(context, null);
  }

  public MoveSpectrumView(Context context, AttributeSet attrs) {
    super(context, attrs);
    density = context.getResources().getDisplayMetrics().density;
    axis.setColor(0xFF000000);
    axis.setStrokeWidth(density);
    text.setColor(0xFF000000);
    text.setTextSize(11 * context.getResources().getDisplayMetrics().scaledDensity);
    text.setTextAlign(Paint.Align.CENTER);
    outline.setStyle(Paint.Style.STROKE);
    outline.setStrokeWidth(2 * density);
    outline.setColor(0xFF000000);
  }

  public void setOnBarSelectedListener(OnBarSelectedListener l) {
    this.listener = l;
  }

  /**
   * @param moveNames move names (SAN), best first
   * @param moverScores scores from the side to move's point of view, same order (mates as +/-20)
   */
  public void setMoves(List<String> moveNames, double[] moverScores) {
    names.clear();
    names.addAll(moveNames);
    scores = moverScores.clone();
    selected = scores.length > 0 ? 0 : -1;
    animateIn();
  }

  public int selected() {
    return selected;
  }

  public void select(int index) {
    selected = index;
    invalidate();
  }

  private void animateIn() {
    if (animator != null) animator.cancel();
    animator = ValueAnimator.ofFloat(0f, 1f);
    animator.setDuration(650);
    animator.setInterpolator(new DecelerateInterpolator());
    animator.addUpdateListener(a -> {
      progress = (float) a.getAnimatedValue();
      invalidate();
    });
    animator.start();
  }

  @Override
  protected void onDetachedFromWindow() {
    if (animator != null) animator.cancel();
    super.onDetachedFromWindow();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float w = getWidth();
    float h = getHeight();
    float labelSpace = 16 * density;
    float top = labelSpace;
    float bottom = h - labelSpace;
    float mid = (top + bottom) / 2f;
    float half = (bottom - top) / 2f;
    int n = scores.length;
    canvas.drawLine(0, mid, w, mid, axis);
    if (n == 0) return;

    float pad = 4 * density;
    float slot = (w - 2 * pad) / n;
    float gap = slot > 6 * density ? 2 * density : slot > 3 * density ? density : 0;
    double best = scores[0];
    for (int i = 0; i < n; i++) {
      // Sweep left to right: each bar starts growing a little after the one before it.
      float local = Math.max(0f, Math.min(1f, progress * 1.6f - 0.6f * i / Math.max(1, n - 1)));
      double s = scores[i];
      float len = (float) (squash(s) * half) * local;
      int colour = s > EQUAL_BAND ? GREEN : s < -EQUAL_BAND ? RED : GREY;
      boolean good = s >= best - GOOD_MARGIN;
      bar.setColor(good ? colour : (colour & 0x00FFFFFF) | 0x66000000);
      float left = pad + i * slot + gap / 2;
      float right = pad + (i + 1) * slot - gap / 2;
      float minLen = density; // level moves still show as a sliver
      float barTop;
      float barBottom;
      if (len >= 0) {
        barTop = mid - Math.max(len, minLen);
        barBottom = mid;
      } else {
        barTop = mid;
        barBottom = mid + Math.max(-len, minLen);
      }
      canvas.drawRect(left, barTop, right, barBottom, bar);
      if (i == selected) canvas.drawRect(left, barTop - density, right, barBottom + density, outline);
    }

    // Name the best and the worst move at the ends.
    if (progress > 0.95f) {
      drawName(canvas, 0, pad, slot, mid, half);
      if (n > 1) drawName(canvas, n - 1, pad, slot, mid, half);
      if (selected > 0 && selected < n - 1) drawName(canvas, selected, pad, slot, mid, half);
    }
  }

  private void drawName(Canvas canvas, int i, float pad, float slot, float mid, float half) {
    if (i >= names.size()) return;
    float x = pad + (i + 0.5f) * slot;
    x = Math.max(text.getTextSize() * 1.5f, Math.min(getWidth() - text.getTextSize() * 1.5f, x));
    float len = (float) (squash(scores[i]) * half);
    float y = scores[i] >= 0 ? mid - len - 4 * density : mid - len + text.getTextSize() + 2 * density;
    canvas.drawText(names.get(i), x, y, text);
  }

  private static double squash(double s) {
    return Math.signum(s) * (1 - Math.exp(-Math.abs(s) / SCALE_PAWNS));
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    int action = e.getActionMasked();
    int n = scores.length;
    if (n > 0 && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
      if (action == MotionEvent.ACTION_DOWN && getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
      float pad = 4 * density;
      int i = (int) ((e.getX() - pad) / ((getWidth() - 2 * pad) / n));
      i = Math.max(0, Math.min(n - 1, i));
      if (i != selected) {
        selected = i;
        invalidate();
        if (listener != null) listener.onBarSelected(i);
      }
      return true;
    }
    if (action == MotionEvent.ACTION_UP) {
      performClick();
      return true;
    }
    return super.onTouchEvent(e);
  }

  @Override
  public boolean performClick() {
    return super.performClick();
  }
}
