package com.example.fishstock.agents;

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
 * Level -1: Simple turned upside down. Looks one move ahead with the same evaluation function
 * and plays the move it likes LEAST. It never checkmates you if it has any other move.
 *
 * Handy as a sanity check: if the evaluation function is any good, Randy (level 0) should beat
 * Blunder almost every time.
 */
public final class Blunder implements Agent {
  private static final double TIE_MARGIN = 0.02;
  /** Mating is the best thing that can happen, so Blunder treats it as the worst choice. */
  private static final double MATE = 1000;

  private final Evaluator evaluator;
  private final Random random;

  public Blunder() {
    this(new Random());
  }

  public Blunder(Random random) {
    this.evaluator = new WeightedEvaluator(EvalWeights.classic());
    this.random = random;
  }

  @Override
  public String name() {
    return "Blunder";
  }

  @Override
  public Evaluator evaluator() {
    return evaluator;
  }

  @Override
  public Move chooseMove(Position pos, List<Long> history, AtomicBoolean stop) {
    List<Move> moves = MoveGenerator.legalMoves(pos);
    if (moves.isEmpty()) return null;
    boolean us = pos.whiteToMove();
    List<Move> worst = new ArrayList<>();
    double worstScore = Double.POSITIVE_INFINITY;
    for (Move m : moves) {
      if (stop != null && stop.get()) break;
      pos.makeMove(m);
      double score;
      if (Rules.isCheckmate(pos)) score = MATE;
      else if (Rules.isStalemate(pos) || Rules.isInsufficientMaterial(pos)) score = 0;
      else score = evaluator.evaluateFor(pos, us);
      pos.unmakeMove(m);

      if (score < worstScore - TIE_MARGIN) {
        worstScore = score;
        worst.clear();
        worst.add(m);
      } else if (score <= worstScore + TIE_MARGIN) {
        worst.add(m);
        worstScore = Math.min(worstScore, score);
      }
    }
    if (worst.isEmpty()) return moves.get(0);
    return worst.get(random.nextInt(worst.size()));
  }
}
