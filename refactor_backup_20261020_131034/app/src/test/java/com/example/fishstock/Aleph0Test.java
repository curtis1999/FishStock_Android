package com.example.fishstock;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.Position;
import com.example.fishstock.nn.Adam;
import com.example.fishstock.nn.Encoder;
import com.example.fishstock.nn.Mcts;
import com.example.fishstock.nn.Network;
import com.example.fishstock.nn.ZeroSelfPlay;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Aleph_0: the encoder, the network's maths, the search and one self-play game. */
public class Aleph0Test {

  private static int[] moveIndices(Position p) {
    List<Move> moves = Mcts.searchMoves(p);
    int[] idx = new int[moves.size()];
    for (int i = 0; i < idx.length; i++) idx[i] = Encoder.moveIndex(moves.get(i), p.whiteToMove());
    return idx;
  }

  @Test
  public void encoderSeesTheBoardFromTheMoversSide() {
    // After 1.e4 e5 2.Nf3 Nc6 it's White to move; mirror the position (colours swapped, board
    // flipped) and Black to move should look exactly the same to the network.
    Position w = Position.fromFen("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3");
    Position b = Position.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
    int[] fw = Encoder.features(w);
    int[] fb = Encoder.features(b);
    Arrays.sort(fw);
    Arrays.sort(fb);
    assertArrayEquals(fw, fb);
    assertEquals(32 + 4, fw.length);
    int[] mw = moveIndices(w);
    int[] mb = moveIndices(b);
    Arrays.sort(mw);
    Arrays.sort(mb);
    assertArrayEquals(mw, mb);
  }

  @Test
  public void gradientsMatchNumericalDerivatives() {
    Network net = Network.random(7);
    Position p = Position.fromFen("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3");
    int[] f = Encoder.features(p);
    int[] m = moveIndices(p);
    float[] target = new float[m.length];
    target[0] = 0.7f;
    target[3] = 0.3f;
    float z = 0.5f;
    float[] grad = new float[net.params.length];
    net.accumulateGradient(f, m, target, z, grad, null);
    Random r = new Random(3);
    int checked = 0;
    for (int k = 0; k < 4000 && checked < 25; k++) {
      int i = r.nextInt(net.params.length);
      if (Math.abs(grad[i]) < 1e-4) continue;
      float old = net.params[i];
      double eps = 1e-3;
      net.params[i] = (float) (old + eps);
      double up = loss(net, f, m, target, z);
      net.params[i] = (float) (old - eps);
      double down = loss(net, f, m, target, z);
      net.params[i] = old;
      double numeric = (up - down) / (2 * eps);
      assertEquals("param " + i, numeric, grad[i], 0.02 * Math.max(1, Math.abs(numeric)));
      checked++;
    }
    assertTrue(checked > 10);
  }

  private static double loss(Network net, int[] f, int[] m, float[] target, float z) {
    float[] g = new float[net.params.length];
    return net.accumulateGradient(f, m, target, z, g, null);
  }

  @Test
  public void trainingLowersTheLoss() {
    Network net = Network.random(11);
    Adam adam = new Adam(net.params.length, 0.003, 0);
    Position p = Position.fromFen(Position.START_FEN);
    int[] f = Encoder.features(p);
    int[] m = moveIndices(p);
    float[] target = new float[m.length];
    target[5] = 1;
    double first = 0, last = 0;
    for (int step = 0; step < 60; step++) {
      float[] g = new float[net.params.length];
      double l = net.accumulateGradient(f, m, target, 1f, g, null);
      if (step == 0) first = l;
      last = l;
      adam.step(net.params, g, 1);
    }
    assertTrue(first + " -> " + last, last < first * 0.2);
  }

  @Test
  public void saveAndLoadGiveTheSameNetwork() throws Exception {
    Network net = Network.random(5);
    net.generation = 3;
    net.gamesPlayed = 120;
    Network back = Network.read(new ByteArrayInputStream(net.toBytes()));
    assertArrayEquals(net.params, back.params, 0f);
    assertEquals(3, back.generation);
    assertEquals(120, back.gamesPlayed);
  }

  @Test
  public void searchFindsMateEvenWithAnUntrainedNetwork() {
    // The rules are exact: checkmate scores -1, so the search finds Rd8# on its own.
    Mcts mcts = new Mcts(Network.random(1), new Random(1));
    Position p = Position.fromFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1");
    Mcts.Result r = mcts.search(p, null, 400, 0, null);
    assertEquals("d1d8", r.best().toUci());
    assertTrue(r.value > 0.9);
  }

  @Test
  public void searchAvoidsHangingMate() {
    // Black to move must stop Rd8#: the search should not allow it (e.g. ...h6 or ...g6 or ...Kf8).
    Mcts mcts = new Mcts(Network.random(2), new Random(2));
    Position p = Position.fromFen("6k1/5ppp/8/8/8/8/r4PPP/3R2K1 b - - 0 1");
    Mcts.Result r = mcts.search(p, null, 800, 0, null);
    Position after = p.copy();
    after.makeMove(r.best());
    Mcts.Result reply = new Mcts(Network.random(2), new Random(3)).search(after, null, 300, 0, null);
    assertTrue("allowed mate with " + r.best().toUci(), !reply.best().toUci().equals("d1d8") || reply.value < 0.9);
  }

  @Test
  public void aSelfPlayGameMakesTrainingExamples() {
    ZeroSelfPlay.Settings s = new ZeroSelfPlay.Settings();
    s.fullSimulations = 16;
    s.fastSimulations = 8;
    s.maxPlies = 60;
    ZeroSelfPlay.GameOutcome o = ZeroSelfPlay.play(Network.random(4), s, new Random(4));
    assertTrue(o.plies > 0);
    assertTrue(!o.samples.isEmpty());
    for (ZeroSelfPlay.Sample x : o.samples) {
      double sum = 0;
      for (float t : x.target) sum += t;
      assertEquals(1.0, sum, 1e-4);
      assertEquals(x.moves.length, x.target.length);
    }
  }

  @Test
  public void aleph0PlaysLegalMoves() {
    Game g = new Game();
    com.example.fishstock.agents.Aleph0 a = new com.example.fishstock.agents.Aleph0(Network.random(9), 0, 50);
    for (int i = 0; i < 6; i++) {
      Move m = a.chooseMove(g.position().copy(), g.positionHistory(), null);
      assertNotNull(g.findMove(m.from, m.to, m.promotion));
      g.play(m);
    }
  }
}
