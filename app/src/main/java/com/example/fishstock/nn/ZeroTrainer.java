package com.example.fishstock.nn;

import com.example.fishstock.agents.Agent;
import com.example.fishstock.agents.Aleph0;
import com.example.fishstock.arena.Arena;
import com.example.fishstock.arena.GameRecord;
import com.example.fishstock.arena.Openings;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Trains Aleph_0 from scratch by self-play, AlphaZero style. Each iteration (a "generation"):
 * <ol>
 *   <li>the current network plays itself {@code games} times (all CPU cores in parallel);</li>
 *   <li>the positions go into a replay buffer holding the most recent {@code window} examples;</li>
 *   <li>the network trains on random batches from the buffer: value towards who won, policy
 *       towards what the search preferred;</li>
 *   <li>the new network plays the previous one, Randy and Simple, and everything is logged to
 *       progress.csv. Like AlphaZero (not AlphaGo Zero), the new network is always kept.</li>
 * </ol>
 * Everything is saved after each generation (network, buffer, log), so it can be stopped at any
 * time (close the window) and continued later from where it left off.
 *
 * Run: java ... com.example.fishstock.nn.ZeroTrainer [key=value ...]
 * <pre>
 *   generations=5     how many generations to run this time (0 = until minutes runs out)
 *   minutes=0         stop after this long (0 = no limit), finishing the current generation
 *   games=60          self-play games per generation
 *   sims=100          simulations for a full search (fast searches use a quarter)
 *   threads=all       CPU threads
 *   batch=256  lr=0.001  window=60000  epochs=2  (training: each new example is seen ~epochs times)
 *   check=20          games against the previous generation, Randy and Simple (0 = skip)
 *   dir=aleph0_work   where the network, buffer and log are kept
 *   publish=path      also copy each new network here (e.g. into the app's resources)
 * </pre>
 */
public final class ZeroTrainer {

  private final File dir;
  private final File publish;
  private final int games;
  private final int threads;
  private final int batch;
  private final double lr;
  private final int window;
  private final double epochs;
  private final int checkGames;
  private final ZeroSelfPlay.Settings settings = new ZeroSelfPlay.Settings();
  private final Random rng = new Random();

  private Network net;
  private Adam adam;
  private final ArrayDeque<ZeroSelfPlay.Sample> buffer = new ArrayDeque<>();
  private final ExecutorService pool;

  public ZeroTrainer(File dir, File publish, int games, int sims, int threads, int batch, double lr, int window,
      double epochs, int checkGames) {
    this.dir = dir;
    this.publish = publish;
    this.games = games;
    this.threads = threads;
    this.batch = batch;
    this.lr = lr;
    this.window = window;
    this.epochs = epochs;
    this.checkGames = checkGames;
    settings.fullSimulations = sims;
    settings.fastSimulations = Math.max(8, sims / 4);
    pool = Executors.newFixedThreadPool(threads);
  }

  public static void main(String[] args) throws Exception {
    java.util.Map<String, String> a = new java.util.HashMap<>();
    for (String s : args) {
      int eq = s.indexOf('=');
      if (eq > 0) a.put(s.substring(0, eq).toLowerCase(Locale.US), s.substring(eq + 1));
    }
    int cores = Runtime.getRuntime().availableProcessors();
    String th = a.getOrDefault("threads", "all");
    int threads = th.equals("all") ? cores : Integer.parseInt(th);
    ZeroTrainer t = new ZeroTrainer(
        new File(a.getOrDefault("dir", "aleph0_work")),
        a.containsKey("publish") ? new File(a.get("publish")) : null,
        Integer.parseInt(a.getOrDefault("games", "60")),
        Integer.parseInt(a.getOrDefault("sims", "100")),
        Math.max(1, threads),
        Integer.parseInt(a.getOrDefault("batch", "256")),
        Double.parseDouble(a.getOrDefault("lr", "0.001")),
        Integer.parseInt(a.getOrDefault("window", "60000")),
        Double.parseDouble(a.getOrDefault("epochs", "2")),
        Integer.parseInt(a.getOrDefault("check", "20")));
    int generations = Integer.parseInt(a.getOrDefault("generations", "5"));
    double minutes = Double.parseDouble(a.getOrDefault("minutes", "0"));
    try {
      t.run(generations, minutes);
    } finally {
      t.pool.shutdownNow();
    }
  }

  // ================================================================ the loop

  public void run(int generations, double minutes) throws Exception {
    if (!dir.exists() && !dir.mkdirs()) throw new IOException("can't create " + dir);
    loadOrCreate();
    long deadline = minutes > 0 ? System.currentTimeMillis() + (long) (minutes * 60_000) : Long.MAX_VALUE;
    say(String.format(Locale.US, "Aleph_0 training: generation %d so far, %d games played, %d examples in the buffer, %d threads.",
        net.generation, net.gamesPlayed, buffer.size(), threads));
    for (int g = 0; generations <= 0 || g < generations; g++) {
      if (System.currentTimeMillis() >= deadline) break;
      generation();
    }
    say("Stopped. Network: " + new File(dir, "latest.bin").getPath());
  }

  private void generation() throws Exception {
    long t0 = System.currentTimeMillis();
    int gen = net.generation + 1;
    say("");
    say("Generation " + gen + ": playing " + games + " self-play games...");

    // 1. Self-play
    final Network player = net.copy();
    List<Future<ZeroSelfPlay.GameOutcome>> futures = new ArrayList<>();
    for (int i = 0; i < games; i++) {
      final long seed = rng.nextLong();
      futures.add(pool.submit(() -> ZeroSelfPlay.play(player, settings, new Random(seed))));
    }
    int white = 0, black = 0, draws = 0, limit = 0, newSamples = 0;
    long plies = 0;
    for (int i = 0; i < futures.size(); i++) {
      ZeroSelfPlay.GameOutcome o = futures.get(i).get();
      if (o.result > 0) white++;
      else if (o.result < 0) black++;
      else draws++;
      if (o.ending.equals("MOVE_LIMIT")) limit++;
      plies += o.plies;
      newSamples += o.samples.size();
      buffer.addAll(o.samples);
      if ((i + 1) % Math.max(10, games / 4) == 0 && i + 1 < games) say("  " + (i + 1) + "/" + games + " games");
    }
    while (buffer.size() > window) buffer.pollFirst();
    long t1 = System.currentTimeMillis();
    say(String.format(Locale.US, "  White won %d, Black won %d, drawn %d (%d hit the move limit); %.0f plies a game; %d new examples (%.0f s)",
        white, black, draws, limit, (double) plies / games, newSamples, (t1 - t0) / 1000.0));

    // 2. Training
    Network previous = net.copy();
    int steps = Math.max(20, (int) Math.ceil(newSamples * epochs / batch));
    double[] loss = train(steps);
    net.generation = gen;
    net.gamesPlayed += games;
    long t2 = System.currentTimeMillis();
    say(String.format(Locale.US, "  Trained %d steps of %d: value loss %.3f, policy loss %.3f (%.0f s)",
        steps, batch, loss[0], loss[1], (t2 - t1) / 1000.0));

    // 3. Save
    net.save(new File(dir, "latest.bin"));
    net.save(new File(dir, String.format(Locale.US, "gen%03d.bin", gen)));
    saveBuffer();
    if (publish != null) {
      File parent = publish.getParentFile();
      if (parent != null && !parent.exists()) parent.mkdirs();
      net.save(publish);
    }

    // 4. Check progress
    double vsPrevious = Double.NaN, vsRandy = Double.NaN, vsSimple = Double.NaN;
    if (checkGames > 0) {
      int checkSims = Math.max(16, settings.fullSimulations / 2);
      vsPrevious = match(net, previous, checkGames, checkSims);
      vsRandy = matchAgainst(net, com.example.fishstock.agents.AgentSpec.Kind.RANDY, checkGames, checkSims);
      vsSimple = matchAgainst(net, com.example.fishstock.agents.AgentSpec.Kind.SIMPLE, checkGames, checkSims);
      say(String.format(Locale.US, "  New network vs previous: %.0f%%   vs Randy: %.0f%%   vs Simple: %.0f%%",
          vsPrevious * 100, vsRandy * 100, vsSimple * 100));
    }
    long t3 = System.currentTimeMillis();
    log(gen, white, black, draws, limit, (double) plies / games, newSamples, loss, vsPrevious, vsRandy, vsSimple,
        (t3 - t0) / 1000.0);
  }

  // ================================================================ training

  /** {@code steps} batches from the buffer; returns the average value and policy loss. */
  double[] train(int steps) throws Exception {
    if (adam == null) adam = new Adam(net.params.length, lr, 1e-4);
    final ZeroSelfPlay.Sample[] all = buffer.toArray(new ZeroSelfPlay.Sample[0]);
    if (all.length == 0) return new double[] {0, 0};
    final int parts = Math.min(threads, batch);
    final float[][] grads = new float[parts][net.params.length];
    double valueSum = 0, policySum = 0;
    int counted = 0;
    for (int s = 0; s < steps; s++) {
      final int[] pick = new int[batch];
      for (int i = 0; i < batch; i++) pick[i] = rng.nextInt(all.length);
      List<Future<double[]>> fs = new ArrayList<>();
      for (int p = 0; p < parts; p++) {
        final int part = p;
        fs.add(pool.submit(() -> {
          float[] g = grads[part];
          java.util.Arrays.fill(g, 0f);
          double[] lp = new double[2];
          for (int i = part; i < batch; i += parts) {
            ZeroSelfPlay.Sample x = all[pick[i]];
            net.accumulateGradient(x.features, x.moves, x.target, x.z, g, lp);
          }
          return lp;
        }));
      }
      double[] lp = new double[2];
      for (Future<double[]> f : fs) {
        double[] r = f.get();
        lp[0] += r[0];
        lp[1] += r[1];
      }
      float[] total = grads[0];
      for (int p = 1; p < parts; p++) {
        float[] g = grads[p];
        for (int i = 0; i < total.length; i++) total[i] += g[i];
      }
      adam.step(net.params, total, batch);
      // Report the loss of the last half of the steps (the network as it ends up).
      if (s >= steps / 2) {
        valueSum += lp[0] / batch;
        policySum += lp[1] / batch;
        counted++;
      }
    }
    return new double[] {valueSum / Math.max(1, counted), policySum / Math.max(1, counted)};
  }

  // ================================================================ checking

  /** Score of {@code a} against {@code b} (1 = won everything), from varied openings. */
  double match(final Network a, final Network b, int n, final int sims) throws Exception {
    List<Future<Double>> fs = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      final int game = i;
      fs.add(pool.submit(() -> {
        Agent x = new Aleph0(a, 0, sims);
        Agent y = new Aleph0(b, 0, sims);
        boolean aWhite = game % 2 == 0;
        GameRecord r = aWhite ? Arena.playGame(x, y, Arena.DEFAULT_MAX_PLIES, null, Openings.forGame(game))
            : Arena.playGame(y, x, Arena.DEFAULT_MAX_PLIES, null, Openings.forGame(game));
        return (r.result.scoreFor(aWhite) + 1) / 2.0;
      }));
    }
    double sum = 0;
    for (Future<Double> f : fs) sum += f.get();
    return sum / n;
  }

  /** Score against a hand-written agent (Randy: random moves; Simple: counts material, one move deep). */
  double matchAgainst(final Network a, final com.example.fishstock.agents.AgentSpec.Kind kind, int n, final int sims)
      throws Exception {
    List<Future<Double>> fs = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      final int game = i;
      fs.add(pool.submit(() -> {
        Agent x = new Aleph0(a, 0, sims);
        Agent y = com.example.fishstock.agents.AgentFactory.create(
            com.example.fishstock.agents.AgentSpec.builtIn(kind), false);
        boolean aWhite = game % 2 == 0;
        GameRecord r = aWhite ? Arena.playGame(x, y, Arena.DEFAULT_MAX_PLIES, null, Openings.forGame(game))
            : Arena.playGame(y, x, Arena.DEFAULT_MAX_PLIES, null, Openings.forGame(game));
        return (r.result.scoreFor(aWhite) + 1) / 2.0;
      }));
    }
    double sum = 0;
    for (Future<Double> f : fs) sum += f.get();
    return sum / n;
  }

  // ================================================================ files

  private void loadOrCreate() throws IOException {
    File latest = new File(dir, "latest.bin");
    if (latest.exists()) {
      net = Network.load(latest);
      say("Continuing from " + latest.getPath());
    } else if (publish != null && publish.exists()) {
      // No work folder yet (e.g. a new PC): carry on from the network the app ships with.
      net = Network.load(publish);
      say("Continuing from the app's network " + publish.getPath() + " (generation " + net.generation + ")");
    } else {
      net = Network.random(rng.nextLong());
      say("Starting from scratch: a random network that knows nothing about chess.");
    }
    File b = new File(dir, "buffer.bin.gz");
    if (b.exists()) loadBuffer(b);
  }

  private void saveBuffer() throws IOException {
    File f = new File(dir, "buffer.bin.gz");
    File tmp = new File(dir, "buffer.bin.gz.tmp");
    try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(tmp))))) {
      out.writeInt(buffer.size());
      for (ZeroSelfPlay.Sample s : buffer) {
        out.writeByte(s.features.length);
        for (int x : s.features) out.writeShort(x);
        out.writeShort(s.moves.length);
        for (int i = 0; i < s.moves.length; i++) {
          out.writeShort(s.moves[i]);
          out.writeFloat(s.target[i]);
        }
        out.writeByte((int) s.z);
      }
    }
    if (f.exists() && !f.delete()) throw new IOException("can't replace " + f);
    if (!tmp.renameTo(f)) throw new IOException("can't rename " + tmp);
  }

  private void loadBuffer(File f) throws IOException {
    try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f))))) {
      int n = in.readInt();
      for (int k = 0; k < n; k++) {
        int[] features = new int[in.readUnsignedByte()];
        for (int i = 0; i < features.length; i++) features[i] = in.readUnsignedShort();
        int m = in.readUnsignedShort();
        int[] moves = new int[m];
        float[] target = new float[m];
        for (int i = 0; i < m; i++) {
          moves[i] = in.readUnsignedShort();
          target[i] = in.readFloat();
        }
        float z = in.readByte();
        buffer.add(new ZeroSelfPlay.Sample(features, moves, target, z));
      }
    }
  }

  private void log(int gen, int white, int black, int draws, int limit, double plies, int samples, double[] loss,
      double vsPrevious, double vsRandy, double vsSimple, double seconds) throws IOException {
    File f = new File(dir, "progress.csv");
    boolean header = !f.exists();
    try (PrintWriter w = new PrintWriter(new FileWriter(f, true))) {
      if (header) {
        w.println("generation,games_total,white_wins,black_wins,draws,move_limit,avg_plies,examples,value_loss,policy_loss,vs_previous,vs_randy,vs_simple,seconds");
      }
      w.println(String.format(Locale.US, "%d,%d,%d,%d,%d,%d,%.1f,%d,%.4f,%.4f,%.3f,%.3f,%.3f,%.0f",
          gen, net.gamesPlayed, white, black, draws, limit, plies, samples, loss[0], loss[1], vsPrevious, vsRandy,
          vsSimple, seconds));
    }
  }

  private static void say(String s) {
    System.out.println(s);
    System.out.flush();
  }

  /** For tests: the network being trained. */
  Network network() {
    return net;
  }
}
