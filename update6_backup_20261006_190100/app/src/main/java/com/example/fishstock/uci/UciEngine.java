package com.example.fishstock.uci;

import com.example.fishstock.agents.Agent;
import com.example.fishstock.agents.AgentFactory;
import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.agents.MinMax;
import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.search.SearchResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FishStock as a UCI engine, the standard protocol chess programs use to talk to engines. With it,
 * any FishStock agent can play on Lichess through lichess-bot, or in engine matches with
 * cutechess-cli / fastchess (e.g. against Stockfish at a limited Elo, to estimate a rating).
 *
 * Build it on a PC with {@code build_uci_engine.bat} (makes fishstock-uci.jar and fishstock-uci.bat)
 * and point the chess program at fishstock-uci.bat. Pure Java, no Android.
 *
 * Options:
 *  - Agent: Simple, MinMax, Agro, Timid, FishStock, Blunder or Randy (default FishStock).
 *  - CustomAgent: a saved agent's line (AgentSpec.serialize()); overrides Agent when set.
 *  - OwnBook: use the agent's opening book (default true).
 *  - MoveOverheadMs: time kept back for network lag (default 100).
 * Time: uses movetime if given, else about 1/30 of the remaining clock plus most of the increment.
 */
public final class UciEngine {
  private static final String NAME = "FishStock";

  private final PrintStream out;
  private AgentSpec spec = AgentSpec.builtIn(AgentSpec.Kind.FISHSTOCK);
  private boolean ownBook = true;
  private long moveOverheadMs = 100;
  private Game game = new Game();
  private Thread searchThread;
  private AtomicBoolean stop = new AtomicBoolean(false);

  public UciEngine(PrintStream out) {
    this.out = out;
  }

  public static void main(String[] args) throws Exception {
    UciEngine engine = new UciEngine(System.out);
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String line;
    while ((line = in.readLine()) != null) {
      if (!engine.handle(line.trim())) break;
    }
    engine.stopSearch();
  }

  /** Handles one command; returns false on "quit". */
  public boolean handle(String line) {
    if (line.isEmpty()) return true;
    String[] t = line.split("\\s+");
    switch (t[0]) {
      case "uci":
        send("id name " + NAME);
        send("id author FishStock");
        send("option name Agent type combo default FishStock var Simple var MinMax var Agro var Timid var FishStock var Blunder var Randy");
        send("option name CustomAgent type string default <empty>");
        send("option name OwnBook type check default true");
        send("option name MoveOverheadMs type spin default 100 min 0 max 5000");
        send("uciok");
        break;
      case "isready":
        send("readyok");
        break;
      case "setoption":
        setOption(line);
        break;
      case "ucinewgame":
        stopSearch();
        game = new Game();
        break;
      case "position":
        stopSearch();
        position(t);
        break;
      case "go":
        go(t);
        break;
      case "stop":
        stopSearch();
        break;
      case "quit":
        return false;
      default:
        break; // unknown commands are ignored, as the protocol asks
    }
    return true;
  }

  private void setOption(String line) {
    String lower = line.toLowerCase(Locale.US);
    int n = lower.indexOf(" name ");
    int v = lower.indexOf(" value ");
    if (n < 0) return;
    String name = (v > n ? line.substring(n + 6, v) : line.substring(n + 6)).trim();
    String value = v > 0 ? line.substring(v + 7).trim() : "";
    if (name.equalsIgnoreCase("Agent")) {
      AgentSpec s = AgentSpec.byName(value);
      if (s.kind != AgentSpec.Kind.HUMAN) spec = s;
    } else if (name.equalsIgnoreCase("CustomAgent")) {
      if (!value.isEmpty() && !value.equals("<empty>")) {
        try {
          spec = AgentSpec.parse(value);
        } catch (RuntimeException e) {
          send("info string could not read CustomAgent: " + e.getMessage());
        }
      }
    } else if (name.equalsIgnoreCase("OwnBook")) {
      ownBook = value.equalsIgnoreCase("true");
    } else if (name.equalsIgnoreCase("MoveOverheadMs")) {
      try {
        moveOverheadMs = Math.max(0, Long.parseLong(value));
      } catch (NumberFormatException ignored) {
        // keep the old value
      }
    }
  }

  /** position startpos [moves ...] | position fen <6 fields> [moves ...] */
  private void position(String[] t) {
    int i = 1;
    if (i < t.length && t[i].equals("startpos")) {
      game = new Game();
      i++;
    } else if (i < t.length && t[i].equals("fen")) {
      StringBuilder fen = new StringBuilder();
      i++;
      while (i < t.length && !t[i].equals("moves")) fen.append(t[i++]).append(' ');
      game = new Game(fen.toString().trim());
    }
    if (i < t.length && t[i].equals("moves")) {
      for (i++; i < t.length; i++) {
        Move m = game.findUci(t[i]);
        if (m == null || game.isOver()) break;
        game.play(m);
      }
    }
  }

  private void go(String[] t) {
    stopSearch();
    long wtime = -1, btime = -1, winc = 0, binc = 0, movetime = -1;
    int movestogo = 0;
    boolean infinite = false;
    for (int i = 1; i < t.length; i++) {
      long val = i + 1 < t.length ? parse(t[i + 1]) : 0;
      switch (t[i]) {
        case "wtime": wtime = val; i++; break;
        case "btime": btime = val; i++; break;
        case "winc": winc = val; i++; break;
        case "binc": binc = val; i++; break;
        case "movetime": movetime = val; i++; break;
        case "movestogo": movestogo = (int) val; i++; break;
        case "infinite": infinite = true; break;
        default: break;
      }
    }
    boolean white = game.whiteToMove();
    long remaining = white ? wtime : btime;
    long inc = white ? winc : binc;
    long think;
    if (movetime > 0) {
      think = movetime - moveOverheadMs;
    } else if (remaining > 0) {
      int moves = movestogo > 0 ? movestogo : 30;
      think = remaining / moves + inc * 3 / 4 - moveOverheadMs;
      think = Math.min(think, remaining / 4); // never risk the clock
    } else {
      think = infinite ? 60_000 : 5_000;
    }
    final long thinkMs = Math.max(20, think);

    final AtomicBoolean myStop = new AtomicBoolean(false);
    stop = myStop;
    final Game snapshot = game;
    searchThread = new Thread(() -> {
      // Timid would take 1.2x the time: scale it back so the clock is respected. (Agro keeps its
      // 0.4x: thinking fast is its character.)
      long base = thinkMs;
      if (spec.kind == AgentSpec.Kind.TIMID) base = thinkMs * MinMax.DEFAULT_THINK_MS / AgentFactory.TIMID_THINK_MS;
      Agent agent = AgentFactory.create(spec, false, Math.max(1, base));
      if (!ownBook) {
        if (agent instanceof MinMax) ((MinMax) agent).setOpeningBook(null);
        if (agent instanceof com.example.fishstock.agents.Simple) ((com.example.fishstock.agents.Simple) agent).setOpeningBook(null);
      }
      Move best = agent.chooseMove(snapshot.position().copy(), new ArrayList<>(snapshot.positionHistory()), myStop);
      if (agent instanceof MinMax) {
        SearchResult r = ((MinMax) agent).lastResult();
        if (r != null && r.bestMove != null && best != null && r.bestMove.sameAs(best)) {
          String score = r.isMateScore() ? "mate " + r.mateIn() : "cp " + Math.round(r.score * 100);
          StringBuilder pv = new StringBuilder();
          for (Move m : r.principalVariation) pv.append(' ').append(m.toUci());
          send("info depth " + r.depth + " score " + score + " nodes " + r.nodes + " time " + r.timeMs + " pv" + pv);
        }
      }
      if (best == null && !snapshot.legalMoves().isEmpty()) best = snapshot.legalMoves().get(0);
      send("bestmove " + (best == null ? "0000" : best.toUci()));
    }, "fishstock-search");
    searchThread.start();
  }

  private void stopSearch() {
    stop.set(true);
    Thread th = searchThread;
    if (th != null) {
      try {
        th.join(10_000);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }
    searchThread = null;
  }

  private static long parse(String s) {
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private synchronized void send(String s) {
    out.println(s);
    out.flush();
  }
}
