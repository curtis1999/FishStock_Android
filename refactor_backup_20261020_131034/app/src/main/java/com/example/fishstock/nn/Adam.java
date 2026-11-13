package com.example.fishstock.nn;

/**
 * The Adam optimiser with decoupled weight decay (AdamW): nudges every weight against its
 * gradient, with a step size that adapts per weight. The standard choice for training networks.
 */
public final class Adam {
  private final float[] m;
  private final float[] v;
  private int t;
  public double learningRate;
  public double weightDecay;
  private static final double B1 = 0.9;
  private static final double B2 = 0.999;
  private static final double EPS = 1e-8;

  public Adam(int size, double learningRate, double weightDecay) {
    m = new float[size];
    v = new float[size];
    this.learningRate = learningRate;
    this.weightDecay = weightDecay;
  }

  /** One step. {@code grad} is the batch's summed gradient; it is divided by {@code batch}. */
  public void step(float[] params, float[] grad, int batch) {
    t++;
    double c1 = 1 - Math.pow(B1, t);
    double c2 = 1 - Math.pow(B2, t);
    double lr = learningRate * Math.sqrt(c2) / c1;
    float inv = 1f / batch;
    float decay = (float) (1 - learningRate * weightDecay);
    for (int i = 0; i < params.length; i++) {
      float g = grad[i] * inv;
      m[i] = (float) (B1 * m[i] + (1 - B1) * g);
      v[i] = (float) (B2 * v[i] + (1 - B2) * g * g);
      params[i] = (float) (params[i] * decay - lr * m[i] / (Math.sqrt(v[i]) + EPS));
    }
  }
}
