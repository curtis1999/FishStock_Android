package com.example.fishstock.agents;

import com.example.fishstock.endgame.EndgameOracle;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Rules;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.Evaluator;
import com.example.fishstock.eval.WeightedEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Level 2: looks one move ahead. Tries every legal move and keeps the one whose resulting
 * position the evaluation function likes best. All of its "knowledge" is in the
 * {@link EvalWeights} it is given (the defaults, unless it is a custom agent).
 *
 * Steps for each move:
 *  1. In a solved endgame, ask the tablebase (if one is available) and play its move.
 *  2. A move that checkmates is played at once.
 *  3. Stalemate, threefold repetition and dead positions score as a draw (0).
 *  4. Otherwise score = evaluation of the new position from our side.
 *  5. Safety net: a move that lets the opponent mate next move scores as lost.
 * Near-equal moves are chosen between at random so games vary.
 */
public class Simple implements Agent {
  private static final double LOSS = -1000;
  private static final double TIE_MARGIN = 0.02;

  private final String name;
  private final Evaluator evaluator;
  private final EndgameOracle oracle;
  private final Random random = new Random();

  public Simple() {
    this("Simple", EvalWeights.defaults(), EndgameOracle.NONE);
  }

  public Simple(String name, EvalWeights weights, EndgameOracle oracle) {
    this.name = name;
    this.evaluator = new WeightedEvaluator(weights);
    this.oracle = oracle == null ? EndgameOracle.NONE : oracle;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Evaluator evaluator() {
    return evaluator;
  }

  @Override
  public Move chooseMove(Position pos, List<Long> history, AtomicBoolean stop) {
    List<Move> moves = MoveGenerator.legalMoves(pos);
    if (moves.isEmpty()) return null;
    if (moves.size() == 1) return moves.get(0);

    if (oracle.covers(pos)) {
      Move perfect = oracle.bestMove(pos);
      if (perfect != null) return perfect;
    }

    boolean us = pos.whiteToMove();
    List<Move> best = new ArrayList<>();
    double bestScore = Double.NEGATIVE_INFINITY;
    for (Move m : moves) {
      if (stop != null && stop.get()) break;
      pos.makeMove(m);
      double score;
      if (Rules.isCheckmate(pos)) {
        pos.unmakeMove(m);
        return m;
      } else if (Rules.isStalemate(pos) || Rules.isInsufficientMaterial(pos)
          || repeatsTwice(pos.hash(), history)) {
        score = 0;
      } else if (allowsMateInOne(pos)) {
        score = LOSS;
      } else {
        score = evaluator.evaluateFor(pos, us);
      }
      pos.unmakeMove(m);

      if (score > bestScore + TIE_MARGIN) {
        bestScore = score;
        best.clear();
        best.add(m);
      } else if (score >= bestScore - TIE_MARGIN) {
        best.add(m);
        bestScore = Math.max(bestScore, score);
      }
    }
    if (best.isEmpty()) return moves.get(0);
    return best.get(random.nextInt(best.size()));
  }

  /** Would this position appear for the third time? */
  private static boolean repeatsTwice(long hash, List<Long> history) {
    if (history == null) return false;
    int n = 0;
    for (long h : history) if (h == hash) n++;
    return n >= 2;
  }

  /** Can the side to move (the opponent) checkmate immediately? */
  private static boolean allowsMateInOne(Position pos) {
    for (Move reply : MoveGenerator.legalMoves(pos)) {
      pos.makeMove(reply);
      boolean mate = pos.inCheck() && !MoveGenerator.hasLegalMove(pos);
      pos.unmakeMove(reply);
      if (mate) return true;
    }
    return false;
  }
}
