package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.engine.Position;
import com.example.fishstock.eval.Category;
import com.example.fishstock.eval.EvalBreakdown;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.Term;
import com.example.fishstock.eval.WeightedEvaluator;

import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

public class EvaluatorTest {
  private final WeightedEvaluator eval = WeightedEvaluator.withDefaults();

  /** Swap colours and flip the board vertically. */
  static String mirror(String fen) {
    String[] p = fen.split(" ");
    String[] rows = p[0].split("/");
    StringBuilder sb = new StringBuilder();
    for (int i = 7; i >= 0; i--) {
      for (char c : rows[i].toCharArray()) {
        sb.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
      }
      if (i > 0) sb.append('/');
    }
    String castle = "";
    for (char c : "KQkq".toCharArray()) {
      char swapped = Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c);
      if (p[2].indexOf(swapped) >= 0) castle += c;
    }
    return sb + (p[1].equals("w") ? " b " : " w ") + (castle.isEmpty() ? "-" : castle) + " - 0 1";
  }

  @Test
  public void evaluationIsColourSymmetric() {
    String[] fens = {
        Position.START_FEN,
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
        "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 5",
        "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"};
    for (String f : fens) {
      double a = eval.evaluate(Position.fromFen(f));
      double b = eval.evaluate(Position.fromFen(mirror(f)));
      assertEquals(f, a, -b, 1e-9);
    }
  }

  @Test
  public void extraQueenIsWinning() {
    assertTrue(eval.evaluate(Position.fromFen("4k3/8/8/8/8/8/8/3QK3 w - - 0 1")) > 8);
  }

  @Test
  public void hangingPieceCountsAgainstItsOwner() {
    // White knight on e4 attacked by a pawn, Black to move.
    Position p = Position.fromFen("rnbqkbnr/ppp2ppp/8/3pp3/4N3/8/PPPPPPPP/R1BQKBNR b KQkq - 0 3");
    EvalBreakdown b = eval.explain(p);
    assertTrue(b.contribution(Term.EN_PRISE_FACTOR) < -2.0);
  }

  @Test
  public void breakdownAddsUpToTotal() {
    Position p = Position.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
    EvalBreakdown b = eval.explain(p);
    double sum = 0;
    for (Term t : Term.values()) sum += b.contribution(t);
    assertEquals(b.total(), sum, 1e-9);
    assertEquals(eval.evaluate(p), b.total(), 1e-9);
  }

  @Test
  public void weightsSerializeAndScale() {
    EvalWeights w = EvalWeights.defaults().with(Term.KNIGHT_OUTPOST_BONUS, 1.2);
    assertEquals(w, EvalWeights.parse(w.serialize()));
    assertEquals(w, EvalWeights.fromVector(w.toVector()));

    Map<Category, Double> sliders = new EnumMap<>(Category.class);
    sliders.put(Category.KING_SAFETY, 2.0);
    sliders.put(Category.PINS_AND_REVEALS, 0.0);
    EvalWeights scaled = EvalWeights.defaults().scaledBy(sliders);
    assertEquals(2 * Term.PAWN_SHIELD_BONUS.defaultValue, scaled.get(Term.PAWN_SHIELD_BONUS), 1e-9);
    assertEquals(1.0, scaled.get(Term.PINNED_TO_KING_FACTOR), 1e-9);   // pins ignored
    assertEquals(Term.ROOK_VALUE.defaultValue, scaled.get(Term.ROOK_VALUE), 1e-9);
  }

  @Test
  public void customAgentSpecRoundTrip() {
    Map<Category, Double> sliders = new EnumMap<>(Category.class);
    sliders.put(Category.OUTPOSTS, 1.5);
    AgentSpec spec = AgentSpec.custom("Outpost Lover", sliders, 1500);
    AgentSpec back = AgentSpec.parse(spec.serialize());
    assertEquals("Outpost Lover", back.name);
    assertEquals(1500, back.thinkTimeMs);
    assertEquals(1.5, back.sliders.get(Category.OUTPOSTS), 1e-9);
    assertEquals(spec.weights(), back.weights());
  }
}
