package com.example.fishstock.agents;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.Category;
import com.example.fishstock.eval.SideBias;
import com.example.fishstock.search.AlphaBetaSearch;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * An agent's temperament, from -1 (Timid) through 0 (neutral) to +1 (Agro).
 *
 * The weights ({@link com.example.fishstock.eval.EvalWeights}) say what an agent thinks a
 * position is worth. The style changes how it CHOOSES between moves, in three ways:
 *
 *  1. Move preferences: a bonus (Agro) or penalty (Timid) in pawns for captures and checks,
 *     added to OUR candidate moves only. Agro takes a piece rather than play a quiet move that
 *     scores up to {@link #captureBonus} pawns better; Timid only captures when not capturing
 *     would cost it more than that.
 *  2. Side bias ({@link SideBias}): Agro counts the ENEMY's king-safety and hanging-piece
 *     problems extra (it wants to attack them) and its own king safety and pawn structure less.
 *     Timid counts its OWN king safety, pawn structure and piece safety extra.
 *  3. Search: Agro spends {@link #mateSearchShare} of its thinking time on a checks-only hunt
 *     for forced mates ({@link com.example.fishstock.search.MateFinder}) and also looks at
 *     checks at the end of its normal search.
 *
 * All of this is derived from one number, so it can be a slider ("Aggression") on the
 * Make Your Own Agent screen and, later, one more parameter for a training run.
 */
public final class PlayStyle implements Serializable {
  // How strong each effect is at aggression +1 / -1 (the knobs to turn when tuning).
  private static final double CAPTURE_BONUS_AT_MAX = 0.35;
  private static final double CHECK_BONUS_AGRO = 0.25;
  private static final double CHECK_PENALTY_TIMID = 0.10;
  private static final double MATE_SEARCH_SHARE_AGRO = 0.35;
  private static final int MATE_SEARCH_MOVES = 4;

  public static final PlayStyle NEUTRAL = fromAggression(0);

  /** -1 = Timid, 0 = neutral, +1 = Agro. */
  public final double aggression;
  /** Added to the score of our capturing moves (pawns; negative = penalty). */
  public final double captureBonus;
  /** Added to the score of our checking moves (pawns; negative = penalty). */
  public final double checkBonus;
  /** How differently we count our own and the opponent's features. */
  public final SideBias bias;
  /** Share of thinking time spent hunting for forced mates (0 = none). */
  public final double mateSearchShare;
  /** Longest mate the hunt looks for, in moves. */
  public final int mateSearchMoves;
  /** Also search checking moves at the search horizon. */
  public final boolean quiescenceChecks;

  private PlayStyle(double aggression) {
    double a = Math.max(-1, Math.min(1, aggression));
    double agro = Math.max(0, a);
    double timid = Math.max(0, -a);
    this.aggression = a;
    this.captureBonus = CAPTURE_BONUS_AT_MAX * a;
    this.checkBonus = CHECK_BONUS_AGRO * agro - CHECK_PENALTY_TIMID * timid;
    this.bias = SideBias.neutral()
        // Agro: hunt the enemy king and loose enemy pieces; worry less about home.
        .withEnemy(Category.KING_SAFETY, 1 + 0.6 * agro - 0.2 * timid)
        .withEnemy(Category.PIECE_SAFETY, 1 + 0.3 * agro)
        .withOwn(Category.KING_SAFETY, 1 - 0.3 * agro + 0.6 * timid)
        .withOwn(Category.PAWN_STRUCTURE, 1 - 0.3 * agro + 0.5 * timid)
        // Timid: keep everything protected.
        .withOwn(Category.PIECE_SAFETY, 1 + 0.4 * timid);
    this.mateSearchShare = MATE_SEARCH_SHARE_AGRO * agro;
    this.mateSearchMoves = MATE_SEARCH_MOVES;
    this.quiescenceChecks = agro >= 0.5;
  }

  public static PlayStyle fromAggression(double aggression) {
    return new PlayStyle(aggression);
  }

  public boolean isNeutral() {
    return aggression == 0;
  }

  /** The move preference (pawns) for playing m from pos. */
  public double moveBonus(Position pos, Move m) {
    if (captureBonus == 0 && checkBonus == 0) return 0;
    double b = 0;
    if (m.isCapture()) b += captureBonus;
    if (checkBonus != 0 && MoveGenerator.givesCheck(pos, m)) b += checkBonus;
    return b;
  }

  /** The move preference in the form the search wants (null when there is none). */
  public AlphaBetaSearch.RootBonus rootBonus() {
    if (captureBonus == 0 && checkBonus == 0) return null;
    return this::moveBonus;
  }

  // ---------------------------------------------------------------- the two built-in personalities

  /** Agro's evaluation sliders: plays for activity, initiative and tactics. */
  public static Map<Category, Double> agroSliders() {
    Map<Category, Double> m = new EnumMap<>(Category.class);
    m.put(Category.INITIATIVE, 2.0);
    m.put(Category.MOBILITY, 1.5);
    m.put(Category.PINS_AND_REVEALS, 1.4);
    m.put(Category.OUTPOSTS, 1.2);
    m.put(Category.PIECE_PLACEMENT, 1.2);
    m.put(Category.PAWN_STRUCTURE, 0.7);
    return m;
  }

  /** Timid's evaluation sliders: solid pawns, safe king, nothing left hanging. */
  public static Map<Category, Double> timidSliders() {
    Map<Category, Double> m = new EnumMap<>(Category.class);
    m.put(Category.KING_SAFETY, 1.4);
    m.put(Category.PAWN_STRUCTURE, 1.4);
    m.put(Category.PIECE_SAFETY, 1.3);
    m.put(Category.MOBILITY, 0.8);
    m.put(Category.INITIATIVE, 0.6);
    return m;
  }
}
