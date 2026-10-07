package com.example.fishstock.agents;

import com.example.fishstock.endgame.EndgameOracle;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.search.SearchLimits;

/**
 * Level 6, the final boss: the same search as MinMax, driven by the improved evaluation with
 * weights tuned by self-play.
 *
 * {@link #TRAINED_WEIGHTS} come from a self-play training run that tuned every
 * {@link com.example.fishstock.eval.Term} of the improved evaluation (see ENGINE_NOTES.md).
 * To install a newer run, paste its serialized string here.
 */
public final class FishStock extends MinMax {
  public static final long THINK_MS = 7000;

  /**
   * The result of the self-play training run (tools/TuneFishStock): 2 rounds x 2,000 games of the
   * improved MinMax at depth 2, Texel-tuned on 300,000 positions, mop-up dials kept at their defaults.
   * Check games at depth 3: 56% over 80 games against the untuned improved evaluation (MinMax+),
   * 69% over 40 games against the original evaluation (MinMax).
   */
  public static final String TRAINED_WEIGHTS =
      "PAWN_VALUE=1.0000;KNIGHT_VALUE=2.8250;BISHOP_VALUE=3.6800;ROOK_VALUE=5.4200;QUEEN_VALUE=12.0000;" +
      "EN_PRISE_FACTOR=0.5000;SECOND_THREAT_FACTOR=0.4800;DEFENDED_PIECE_BONUS=0.0820;LOOSE_PIECE_PENALTY=0.0000;" +
      "KNIGHT_MOBILITY=0.0480;BISHOP_MOBILITY=0.0440;ROOK_MOBILITY=0.0420;QUEEN_MOBILITY=0.0160;" +
      "PINNED_TO_KING_FACTOR=0.8840;PINNED_TO_QUEEN_FACTOR=0.7600;REVEAL_CHECKER_BONUS=0.0000;" +
      "REVEAL_QUEEN_ATTACKER_BONUS=0.2600;PAWN_SHIELD_BONUS=0.0850;OPEN_FILE_NEAR_KING_PENALTY=0.2600;" +
      "KING_ZONE_ATTACK_PENALTY=0.0240;KING_FEW_ESCAPE_SQUARES_PENALTY=0.1340;XRAY_ON_KING_PENALTY=0.2880;" +
      "CASTLED_KING_BONUS=0.3700;LOST_CASTLING_PENALTY=0.0300;DOUBLED_PAWN_PENALTY=0.0000;" +
      "ISOLATED_PAWN_PENALTY=0.1240;BACKWARD_PAWN_PENALTY=0.0200;CONNECTED_PAWN_BONUS=0.0000;" +
      "CENTRAL_PAWN_BONUS=0.0750;PASSED_PAWN_BONUS=0.2700;PASSED_PAWN_ADVANCE_BONUS=1.2100;" +
      "PROTECTED_PASSED_PAWN_BONUS=0.1440;ONE_STEP_FROM_PROMOTION_BONUS=1.1700;UNSTOPPABLE_PAWN_BONUS=0.0000;" +
      "WINS_PAWN_RACE_BONUS=3.1600;KNIGHT_OUTPOST_BONUS=0.2200;BISHOP_OUTPOST_BONUS=0.3400;" +
      "ADVANCED_OUTPOST_BONUS=0.0000;BISHOP_PAIR_BONUS=0.5000;KNIGHT_CENTRAL_BONUS=0.0000;" +
      "KNIGHT_RIM_PENALTY=0.0000;KNIGHT_CORNER_PENALTY=0.3200;BISHOP_LONG_DIAGONAL_BONUS=0.1020;" +
      "ROOK_OPEN_FILE_BONUS=0.2180;ROOK_SEMI_OPEN_FILE_BONUS=0.2400;ROOK_SEVENTH_RANK_BONUS=0.2900;" +
      "CONNECTED_ROOKS_BONUS=0.1860;UNDEVELOPED_MINOR_PENALTY=0.3060;EARLY_QUEEN_PENALTY=0.1760;" +
      "SIDE_TO_MOVE_BONUS=0.0850;IN_CHECK_PENALTY=0.2000;KING_CENTRALISATION_BONUS=0.0840;" +
      "MOP_UP_EDGE_BONUS=0.1500;MOP_UP_KING_PROXIMITY_BONUS=0.0800;DRAWISH_SCALE=0.2300;PAWN_THREAT_BONUS=0.5000;" +
      "MINOR_THREAT_BONUS=0.5500;ROOK_THREAT_BONUS=1.0000;KING_DANGER_WEIGHT=1.2800;SAFE_CHECK_PENALTY=0.6000;" +
      "ROOK_BEHIND_PASSER_BONUS=0.1440;PASSER_FREE_PATH_BONUS=1.5000;PASSER_KING_PROXIMITY=0.3000;" +
      "KNIGHT_CENTRALITY=0.0740;BISHOP_CENTRALITY=0.0510;BAD_BISHOP_PENALTY=0.1300;TRAPPED_BISHOP_PENALTY=0.0000;" +
      "TRAPPED_ROOK_PENALTY=1.2000;SPACE_BONUS=0.0200;KNIGHT_PAWN_ADJUST=0.0540;ROOK_PAWN_ADJUST=0.1600;";

  public FishStock(EndgameOracle oracle) {
    this(oracle, SearchLimits.forTime(THINK_MS));
  }

  public FishStock(EndgameOracle oracle, SearchLimits limits) {
    super("FishStock", weights(), limits, oracle);
  }

  public static EvalWeights weights() {
    return TRAINED_WEIGHTS.isEmpty() ? EvalWeights.defaults() : EvalWeights.parse(TRAINED_WEIGHTS);
  }
}
