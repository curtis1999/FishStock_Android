package com.example.fishstock.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.fishstock.R;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Square;

import java.util.List;

/**
 * Draws a {@link Position} onto the 8x8 grid of ImageButtons (ids button00..button77, row then
 * column, top-left first) used by the game and analysis layouts, and turns taps back into squares.
 *
 * Not flipped: White at the bottom (row 0 = rank 8, column 0 = a-file).
 * Flipped: Black at the bottom.
 */
public final class BoardRenderer {
  public interface SquareClickListener {
    void onSquareClicked(int square);
  }

  private static final long ANIMATION_MS = 180;

  private final Activity activity;
  private final ImageButton[][] buttons = new ImageButton[8][8];
  private boolean flipped;
  private ImageView animatingPiece;

  public BoardRenderer(Activity activity) {
    this.activity = activity;
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        int id = activity.getResources().getIdentifier("button" + row + col, "id", activity.getPackageName());
        buttons[row][col] = activity.findViewById(id);
      }
    }
  }

  public void setFlipped(boolean flipped) {
    this.flipped = flipped;
  }

  public boolean isFlipped() {
    return flipped;
  }

  public void setOnSquareClickListener(final SquareClickListener listener) {
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        final int r = row;
        final int c = col;
        buttons[row][col].setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            listener.onSquareClicked(squareAt(r, c));
          }
        });
      }
    }
  }

  /** Board square shown at a grid row/column with the current orientation. */
  public int squareAt(int row, int col) {
    return flipped ? Square.of(7 - col, row) : Square.of(col, 7 - row);
  }

  public ImageButton buttonFor(int square) {
    int f = Square.file(square);
    int r = Square.rank(square);
    return flipped ? buttons[r][7 - f] : buttons[7 - r][f];
  }

  /** Redraws every square and clears highlights. */
  public void draw(Position pos) {
    for (int sq = 0; sq < 64; sq++) drawSquare(pos, sq);
  }

  private void drawSquare(Position pos, int sq) {
    ImageButton b = buttonFor(sq);
    b.setColorFilter(null);
    b.setImageResource(pieceDrawable(pos.pieceAt(sq), Square.isLight(sq)));
  }

  /** Yellow tint on the selected piece, dots on empty targets, red tint on capturable pieces. */
  public void highlight(Position pos, int selected, List<Move> targets) {
    buttonFor(selected).setColorFilter(Color.YELLOW, PorterDuff.Mode.OVERLAY);
    for (Move m : targets) {
      ImageButton b = buttonFor(m.to);
      if (pos.isEmpty(m.to)) {
        b.setImageResource(Square.isLight(m.to) ? R.drawable.white_empty_selected : R.drawable.black_empty_selected);
      } else {
        b.setColorFilter(Color.RED, PorterDuff.Mode.OVERLAY);
      }
    }
  }

  /**
   * Slides the moving piece from its square to its destination, then draws {@code after}.
   * {@code before} is the position before the move.
   */
  public void animateMove(final Position before, final Move move, final Position after, final Runnable onDone) {
    cancelAnimation();
    final ImageButton from = buttonFor(move.from);
    final ImageButton to = buttonFor(move.to);
    final ViewGroup root = (ViewGroup) activity.findViewById(R.id.gridlayout).getParent();
    if (from.getWidth() == 0 || root == null) {
      draw(after);
      if (onDone != null) onDone.run();
      return;
    }
    draw(before);
    from.setImageResource(Square.isLight(move.from) ? R.drawable.empty_light : R.drawable.empty_dark);

    int[] rootLoc = new int[2];
    int[] fromLoc = new int[2];
    int[] toLoc = new int[2];
    root.getLocationInWindow(rootLoc);
    from.getLocationInWindow(fromLoc);
    to.getLocationInWindow(toLoc);

    final ImageView ghost = new ImageView(activity);
    ghost.setImageResource(animationDrawable(move.piece));
    ghost.setScaleType(ImageView.ScaleType.FIT_XY);
    root.addView(ghost, new ViewGroup.LayoutParams(from.getWidth(), from.getHeight()));
    ghost.setTranslationX(fromLoc[0] - rootLoc[0]);
    ghost.setTranslationY(fromLoc[1] - rootLoc[1]);
    ghost.setElevation(8f);
    animatingPiece = ghost;
    ghost.animate()
        .translationX(toLoc[0] - rootLoc[0])
        .translationY(toLoc[1] - rootLoc[1])
        .setDuration(ANIMATION_MS)
        .withEndAction(new Runnable() {
          @Override
          public void run() {
            root.removeView(ghost);
            if (animatingPiece == ghost) animatingPiece = null;
            draw(after);
            if (onDone != null) onDone.run();
          }
        })
        .start();
  }

  public void cancelAnimation() {
    if (animatingPiece != null) {
      animatingPiece.animate().cancel();
      ViewGroup parent = (ViewGroup) animatingPiece.getParent();
      if (parent != null) parent.removeView(animatingPiece);
      animatingPiece = null;
    }
  }

  // ---------------------------------------------------------------- drawables

  /** Square image with the piece on it, e.g. white_knight_on_dark. */
  public static int pieceDrawable(int piece, boolean light) {
    if (piece == Piece.EMPTY) return light ? R.drawable.empty_light : R.drawable.empty_dark;
    boolean white = piece > 0;
    switch (Piece.type(piece)) {
      case Piece.PAWN:
        return white ? (light ? R.drawable.white_pawn_on_light : R.drawable.white_pawn_on_dark)
            : (light ? R.drawable.black_pawn_on_light : R.drawable.black_pawn_on_dark);
      case Piece.KNIGHT:
        return white ? (light ? R.drawable.white_knight_on_light : R.drawable.white_knight_on_dark)
            : (light ? R.drawable.black_knight_on_light : R.drawable.black_knight_on_dark);
      case Piece.BISHOP:
        return white ? (light ? R.drawable.white_bishop_on_light : R.drawable.white_bishop_on_dark)
            : (light ? R.drawable.black_bishop_on_light : R.drawable.black_bishop_on_dark);
      case Piece.ROOK:
        return white ? (light ? R.drawable.white_rook_on_light : R.drawable.white_rook_on_dark)
            : (light ? R.drawable.black_rook_on_light : R.drawable.black_rook_on_dark);
      case Piece.QUEEN:
        return white ? (light ? R.drawable.white_queen_on_light : R.drawable.white_queen_on_dark)
            : (light ? R.drawable.black_queen_on_light : R.drawable.black_queen_on_dark);
      default:
        return white ? (light ? R.drawable.white_king_on_light : R.drawable.white_king_on_dark)
            : (light ? R.drawable.black_king_on_light : R.drawable.black_king_on_dark);
    }
  }

  /** Transparent cut-out of the board piece (same artwork as the squares), used while sliding. */
  public static int animationDrawable(int piece) {
    boolean white = piece > 0;
    switch (Piece.type(piece)) {
      case Piece.PAWN: return white ? R.drawable.anim_white_pawn : R.drawable.anim_black_pawn;
      case Piece.KNIGHT: return white ? R.drawable.anim_white_knight : R.drawable.anim_black_knight;
      case Piece.BISHOP: return white ? R.drawable.anim_white_bishop : R.drawable.anim_black_bishop;
      case Piece.ROOK: return white ? R.drawable.anim_white_rook : R.drawable.anim_black_rook;
      case Piece.QUEEN: return white ? R.drawable.anim_white_queen : R.drawable.anim_black_queen;
      default: return white ? R.drawable.anim_white_king : R.drawable.anim_black_king;
    }
  }

  /** Small piece icon on a white background (used by the captured-piece counters). */
  public static int plainPieceDrawable(int piece) {
    boolean white = piece > 0;
    switch (Piece.type(piece)) {
      case Piece.PAWN: return white ? R.drawable.white_pawn : R.drawable.black_pawn;
      case Piece.KNIGHT: return white ? R.drawable.white_knight : R.drawable.black_knight;
      case Piece.BISHOP: return white ? R.drawable.white_bishop : R.drawable.black_bishop;
      case Piece.ROOK: return white ? R.drawable.whie_rook : R.drawable.black_rook;
      case Piece.QUEEN: return white ? R.drawable.white_queen : R.drawable.black_queen;
      default: return white ? R.drawable.white_king : R.drawable.black_king;
    }
  }
}
