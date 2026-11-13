package com.example.fishstock.nn;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Aleph_0's brain: a small neural network with two answers for a position.
 *
 * <pre>
 *   772 inputs (Encoder) -> 256 -> 128 ---> 32 -> 1     value: who's winning, -1..+1 (tanh)
 *                                     \--> 4096          policy: how good each move looks
 * </pre>
 * Hidden layers use ReLU. All weights live in one float array ({@link #params}) so training,
 * saving and loading treat them alike. Pure Java with no libraries, so the same code trains on a
 * PC and plays on the phone.
 *
 * It's small on purpose: a forward pass is about 45,000 multiplications (only the ~32 inputs that
 * are on, and only the policy outputs of legal moves, are computed), so the search can look at
 * thousands of positions a second even on a phone.
 */
public final class Network {
  public static final int MAGIC = 0xA1E90001;

  public final int h1;
  public final int h2;
  public final int hv;

  /** All weights and biases, back to back. */
  public final float[] params;
  /** Training bookkeeping, saved with the weights. */
  public int generation;
  public long gamesPlayed;

  // Offsets into params.
  final int oW1, oB1, oW2, oB2, oWv1, oBv1, oWv2, oBv2, oWp, oBp;

  public Network(int h1, int h2, int hv) {
    this.h1 = h1;
    this.h2 = h2;
    this.hv = hv;
    int o = 0;
    oW1 = o; o += Encoder.INPUTS * h1;
    oB1 = o; o += h1;
    oW2 = o; o += h1 * h2;
    oB2 = o; o += h2;
    oWv1 = o; o += h2 * hv;
    oBv1 = o; o += hv;
    oWv2 = o; o += hv;
    oBv2 = o; o += 1;
    oWp = o; o += Encoder.POLICY * h2;
    oBp = o; o += Encoder.POLICY;
    params = new float[o];
  }

  /** A fresh, untrained network (it knows nothing, not even what a pawn is worth). */
  public static Network random(long seed) {
    Network n = new Network(256, 128, 32);
    n.initialise(new Random(seed));
    return n;
  }

  void initialise(Random r) {
    fill(r, oW1, Encoder.INPUTS * h1, 0.1);             // ~32 inputs are on at a time
    fill(r, oW2, h1 * h2, Math.sqrt(2.0 / h1));
    fill(r, oWv1, h2 * hv, Math.sqrt(2.0 / h2));
    fill(r, oWv2, hv, Math.sqrt(1.0 / hv));
    fill(r, oWp, Encoder.POLICY * h2, 0.01);            // start with every move equally likely
  }

  private void fill(Random r, int off, int len, double std) {
    for (int i = 0; i < len; i++) params[off + i] = (float) (r.nextGaussian() * std);
  }

  public Network copy() {
    Network n = new Network(h1, h2, hv);
    System.arraycopy(params, 0, n.params, 0, params.length);
    n.generation = generation;
    n.gamesPlayed = gamesPlayed;
    return n;
  }

  // ================================================================ forward

  /** What a forward pass leaves behind (needed again for training). */
  public static final class Output {
    public float value;
    public float[] logits;
    float[] a1;
    float[] a2;
    float[] av;
  }

  /**
   * @param features active inputs (Encoder.features)
   * @param moves    policy outputs wanted (Encoder.moveIndex of each legal move); may be empty
   */
  public Output forward(int[] features, int[] moves) {
    final float[] p = params;
    float[] a1 = new float[h1];
    System.arraycopy(p, oB1, a1, 0, h1);
    for (int f : features) {
      int row = oW1 + f * h1;
      for (int i = 0; i < h1; i++) a1[i] += p[row + i];
    }
    for (int i = 0; i < h1; i++) if (a1[i] < 0) a1[i] = 0;

    float[] a2 = new float[h2];
    System.arraycopy(p, oB2, a2, 0, h2);
    for (int i = 0; i < h1; i++) {
      float x = a1[i];
      if (x == 0) continue;
      int row = oW2 + i * h2;
      for (int j = 0; j < h2; j++) a2[j] += x * p[row + j];
    }
    for (int j = 0; j < h2; j++) if (a2[j] < 0) a2[j] = 0;

    float[] av = new float[hv];
    System.arraycopy(p, oBv1, av, 0, hv);
    for (int j = 0; j < h2; j++) {
      float x = a2[j];
      if (x == 0) continue;
      int row = oWv1 + j * hv;
      for (int k = 0; k < hv; k++) av[k] += x * p[row + k];
    }
    float v = p[oBv2];
    for (int k = 0; k < hv; k++) {
      if (av[k] < 0) av[k] = 0;
      v += av[k] * p[oWv2 + k];
    }

    float[] logits = new float[moves.length];
    for (int m = 0; m < moves.length; m++) {
      int row = oWp + moves[m] * h2;
      float s = p[oBp + moves[m]];
      for (int j = 0; j < h2; j++) s += a2[j] * p[row + j];
      logits[m] = s;
    }

    Output out = new Output();
    out.value = (float) Math.tanh(v);
    out.logits = logits;
    out.a1 = a1;
    out.a2 = a2;
    out.av = av;
    return out;
  }

  /** Softmax of the logits: the network's move probabilities. */
  public static float[] softmax(float[] logits) {
    float[] pr = new float[logits.length];
    if (logits.length == 0) return pr;
    float max = Float.NEGATIVE_INFINITY;
    for (float l : logits) max = Math.max(max, l);
    double sum = 0;
    for (int i = 0; i < logits.length; i++) {
      pr[i] = (float) Math.exp(logits[i] - max);
      sum += pr[i];
    }
    for (int i = 0; i < pr.length; i++) pr[i] /= sum;
    return pr;
  }

  // ================================================================ training

  /**
   * Adds this example's gradient into {@code grad} and returns its loss. The loss is
   * (value - z)^2 for the value plus the cross-entropy between the search's move preferences
   * {@code target} and the network's policy.
   */
  public double accumulateGradient(int[] features, int[] moves, float[] target, float z, float[] grad,
      double[] lossParts) {
    final float[] p = params;
    Output o = forward(features, moves);
    float v = o.value;
    double valueLoss = (v - z) * (v - z);
    float dPreV = 2 * (v - z) * (1 - v * v);

    float[] da2 = new float[h2];

    // Policy head (only the legal moves' rows)
    double policyLoss = 0;
    if (moves.length > 0) {
      float[] pr = softmax(o.logits);
      for (int m = 0; m < moves.length; m++) {
        if (target[m] > 0) policyLoss -= target[m] * Math.log(Math.max(pr[m], 1e-8));
        float dl = pr[m] - target[m];
        if (dl == 0) continue;
        int row = oWp + moves[m] * h2;
        grad[oBp + moves[m]] += dl;
        for (int j = 0; j < h2; j++) {
          grad[row + j] += dl * o.a2[j];
          da2[j] += dl * p[row + j];
        }
      }
    }

    // Value head
    grad[oBv2] += dPreV;
    for (int k = 0; k < hv; k++) {
      grad[oWv2 + k] += dPreV * o.av[k];
      if (o.av[k] <= 0) continue;
      float dav = dPreV * p[oWv2 + k];
      grad[oBv1 + k] += dav;
      for (int j = 0; j < h2; j++) {
        grad[oWv1 + j * hv + k] += dav * o.a2[j];
        da2[j] += dav * p[oWv1 + j * hv + k];
      }
    }

    // Second hidden layer
    float[] da1 = new float[h1];
    for (int j = 0; j < h2; j++) {
      if (o.a2[j] <= 0) da2[j] = 0;
      grad[oB2 + j] += da2[j];
    }
    for (int i = 0; i < h1; i++) {
      float x = o.a1[i];
      if (x <= 0) continue;
      int row = oW2 + i * h2;
      float s = 0;
      for (int j = 0; j < h2; j++) {
        grad[row + j] += x * da2[j];
        s += p[row + j] * da2[j];
      }
      da1[i] = s;
    }

    // First hidden layer (only the rows of inputs that are on)
    for (int i = 0; i < h1; i++) grad[oB1 + i] += da1[i];
    for (int f : features) {
      int row = oW1 + f * h1;
      for (int i = 0; i < h1; i++) grad[row + i] += da1[i];
    }

    if (lossParts != null) {
      lossParts[0] += valueLoss;
      lossParts[1] += policyLoss;
    }
    return valueLoss + policyLoss;
  }

  // ================================================================ saving

  public void save(File file) throws IOException {
    File tmp = new File(file.getPath() + ".tmp");
    try (OutputStream out = new FileOutputStream(tmp)) {
      write(out);
    }
    if (file.exists() && !file.delete()) throw new IOException("can't replace " + file);
    if (!tmp.renameTo(file)) throw new IOException("can't rename " + tmp);
  }

  public void write(OutputStream os) throws IOException {
    DataOutputStream out = new DataOutputStream(os);
    out.writeInt(MAGIC);
    out.writeInt(h1);
    out.writeInt(h2);
    out.writeInt(hv);
    out.writeInt(generation);
    out.writeLong(gamesPlayed);
    out.writeInt(params.length);
    ByteBuffer bb = ByteBuffer.allocate(params.length * 4).order(ByteOrder.BIG_ENDIAN);
    bb.asFloatBuffer().put(params);
    out.write(bb.array());
    out.flush();
  }

  public static Network load(File file) throws IOException {
    try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
      return read(in);
    }
  }

  public static Network read(InputStream is) throws IOException {
    DataInputStream in = new DataInputStream(is);
    if (in.readInt() != MAGIC) throw new IOException("not an Aleph_0 network file");
    Network n = new Network(in.readInt(), in.readInt(), in.readInt());
    n.generation = in.readInt();
    n.gamesPlayed = in.readLong();
    int count = in.readInt();
    if (count != n.params.length) throw new IOException("network size doesn't match");
    byte[] bytes = new byte[count * 4];
    in.readFully(bytes);
    ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).asFloatBuffer().get(n.params);
    return n;
  }

  /** The trained network shipped with the app (inside the APK / jar), or null if there is none. */
  public static final String RESOURCE = "/com/example/fishstock/nn/aleph0.bin";

  private static Network bundled;
  private static boolean bundledLoaded;

  public static synchronized Network bundled() {
    if (!bundledLoaded) {
      bundledLoaded = true;
      try (InputStream in = Network.class.getResourceAsStream(RESOURCE)) {
        if (in != null) bundled = read(new BufferedInputStream(in));
      } catch (IOException e) {
        bundled = null;
      }
    }
    return bundled;
  }

  /** Bytes of a saved network (for tests). */
  public byte[] toBytes() {
    try {
      ByteArrayOutputStream b = new ByteArrayOutputStream();
      write(b);
      return b.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
