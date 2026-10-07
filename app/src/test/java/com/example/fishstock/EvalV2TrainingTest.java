package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.agents.AgentFactory;
import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.agents.FishStock;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.BoardAnalysis;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.Term;
import com.example.fishstock.eval.WeightedEvaluator;
import com.example.fishstock.training.SelfPlay;
import com.example.fishstock.training.TexelTuner;

import org.junit.Test;

import java.util.List;

/** The improved evaluation, the agent line-up and the training pieces. */
public class EvalV2TrainingTest {

  private static BoardAnalysis an(String fen) {
    return new BoardAnalysis(Position.fromFen(fen), EvalWeights.defaults());
  }

  @Test
  public void classicSwitchesTheNewTermsOff() {
    assertFalse(EvalWeights.classic().usesVersion2());
    assertTrue(EvalWeights.defaults().usesVersion2());
    for (Term t : Term.values()) if (t.version >= 2) assertEquals(0.0, EvalWeights.classic().get(t), 0);
    // Material-only Simple keeps piece values and en-prise awareness, nothing else.
    EvalWeights simple = EvalWeights.materialAndSafety();
    assertEquals(3.05, simple.get(Term.KNIGHT_VALUE), 1e-9);
    assertEquals(0.0, simple.get(Term.PAWN_SHIELD_BONUS), 0);
    assertTrue(simple.get(Term.EN_PRISE_FACTOR) > 0);
  }

  @Test
  public void threatDetectors() {
    // White pawn on e5 attacks the knight on f6 (and d6 is empty).
    BoardAnalysis a = an("4k3/8/5n2/4P3/8/8/8/4K3 w - - 0 1");
    assertEquals(1, a.threatsByPawn(true));
    // Knight on d5 attacks the rook on e7... (d5 knight hits e7, c7, b6, b4, c3, e3, f4, f6).
    BoardAnalysis b = an("4k3/4r3/8/3N4/8/8/8/4K3 w - - 0 1");
    assertEquals(1, b.threatsByMinor(true));
  }

  @Test
  public void kingDangerAndTrappedPieces() {
    // Queen and rook both bearing on Black's king zone.
    BoardAnalysis a = an("6k1/5ppp/8/8/8/2B5/6Q1/4R1K1 w - - 0 1");
    assertTrue(a.kingAttackUnits(false) >= 7); // queen on g2 (g-file) and bishop on c3 (long diagonal)
    // White bishop on a7 shut in by the pawn on b6.
    BoardAnalysis b = an("4k3/B7/1p6/8/8/8/8/4K3 w - - 0 1");
    assertTrue(b.isTrappedBishop(48)); // a7
  }

  @Test
  public void improvedEvaluationIsColourSymmetric() {
    String[] fens = {
        "r1bq1rk1/pp2bppp/2n1pn2/3p4/2PP4/2N1PN2/PP2BPPP/R2QKB1R w KQ - 0 8",
        "4k3/B7/1p6/8/8/8/8/4K3 w - - 0 1",
        "8/5pk1/6p1/3P4/8/6P1/5PK1/R7 w - - 0 1"};
    WeightedEvaluator ev = WeightedEvaluator.withDefaults();
    for (String fen : fens) {
      Position p = Position.fromFen(fen);
      Position m = Position.fromFen(mirror(fen));
      assertEquals(fen, ev.evaluate(p), -ev.evaluate(m), 1e-9);
    }
  }

  /** Colour-flipped FEN (ranks reversed, case swapped, side to move swapped, no castling/ep). */
  private static String mirror(String fen) {
    String[] parts = fen.split(" ");
    String[] ranks = parts[0].split("/");
    StringBuilder sb = new StringBuilder();
    for (int i = ranks.length - 1; i >= 0; i--) {
      for (char c : ranks[i].toCharArray()) {
        sb.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
      }
      if (i > 0) sb.append('/');
    }
    String castle = parts[2].equals("-") ? "-" : swapCase(parts[2]);
    return sb + (parts[1].equals("w") ? " b " : " w ") + castle + " - 0 1";
  }

  private static String swapCase(String s) {
    StringBuilder b = new StringBuilder();
    for (char c : s.toCharArray()) b.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
    return b.toString();
  }

  @Test
  public void lineUpUsesTheRightEvaluations() {
    assertEquals("Simple", AgentFactory.create(AgentSpec.builtIn(AgentSpec.Kind.SIMPLE), false).name());
    assertEquals("Lazy", AgentFactory.create(AgentSpec.builtIn(AgentSpec.Kind.LAZY), false).name());
    assertFalse(AgentFactory.create(AgentSpec.builtIn(AgentSpec.Kind.MINMAX), false).evaluator().weights().usesVersion2());
    assertTrue(AgentFactory.create(AgentSpec.builtIn(AgentSpec.Kind.MINMAX_PLUS), false).evaluator().weights().usesVersion2());
    assertTrue(FishStock.weights().usesVersion2());
  }

  @Test
  public void selfPlayAndTuningRun() throws Exception {
    List<SelfPlay.Sample> samples = SelfPlay.play(EvalWeights.defaults(), 4, 1, 2, 7, null);
    assertFalse(samples.isEmpty());
    TexelTuner t = new TexelTuner(samples, 2);
    double before = t.error(EvalWeights.defaults());
    EvalWeights tuned = t.tune(EvalWeights.defaults(), 1, System.currentTimeMillis() + 20_000, null);
    assertTrue(t.error(tuned) <= before);
    t.shutdown();
  }
}
