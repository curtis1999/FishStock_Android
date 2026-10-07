package com.example.fishstock.endgame;

import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Position;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Asks the free Lichess Syzygy tablebase server (tablebase.lichess.ovh) for the perfect move in
 * positions with at most 7 pieces. Needs internet; when offline it quietly returns null and the
 * agent thinks for itself. After a failure it waits a minute before trying the network again,
 * so being offline never slows the game down more than once.
 *
 * Must be called off the UI thread (agents always are).
 */
public final class LichessTablebase implements EndgameOracle {
  private static final String ENDPOINT = "https://tablebase.lichess.ovh/standard?fen=";
  private static final int CONNECT_TIMEOUT_MS = 1500;
  private static final int READ_TIMEOUT_MS = 2500;
  private static final long RETRY_AFTER_FAILURE_MS = 60_000;

  private static final Map<String, String> CACHE = Collections.synchronizedMap(
      new LinkedHashMap<String, String>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
          return size() > 512;
        }
      });
  private static volatile long lastFailure = 0;

  @Override
  public boolean covers(Position pos) {
    return SolvedEndgames.isSolved(pos);
  }

  @Override
  public Move bestMove(Position pos) {
    if (!covers(pos)) return null;
    if (System.currentTimeMillis() - lastFailure < RETRY_AFTER_FAILURE_MS) return null;
    String fen = pos.toFen();
    String uci = CACHE.get(fen);
    if (uci == null) {
      uci = fetchBestUci(fen);
      if (uci == null) {
        lastFailure = System.currentTimeMillis();
        return null;
      }
      CACHE.put(fen, uci);
    }
    for (Move m : MoveGenerator.legalMoves(pos)) if (m.toUci().equals(uci)) return m;
    return null;
  }

  private static String fetchBestUci(String fen) {
    HttpURLConnection conn = null;
    try {
      URL url = new URL(ENDPOINT + URLEncoder.encode(fen, "UTF-8").replace("+", "%20"));
      conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
      conn.setReadTimeout(READ_TIMEOUT_MS);
      conn.setRequestProperty("User-Agent", "FishStock chess app");
      if (conn.getResponseCode() != 200) return null;
      StringBuilder sb = new StringBuilder();
      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
      String line;
      while ((line = in.readLine()) != null) sb.append(line);
      in.close();
      return parseBestUci(sb.toString());
    } catch (Exception e) {
      return null;
    } finally {
      if (conn != null) conn.disconnect();
    }
  }

  /**
   * The server lists moves best-first: {"category":"win", ..., "moves":[{"uci":"h1h8",...}, ...]}.
   * A tiny hand parser keeps this class free of JSON libraries (so it also runs in plain JUnit).
   */
  static String parseBestUci(String json) {
    int moves = json.indexOf("\"moves\"");
    if (moves < 0) return null;
    int key = json.indexOf("\"uci\"", moves);
    if (key < 0) return null;
    int start = json.indexOf('"', json.indexOf(':', key) + 1);
    int end = json.indexOf('"', start + 1);
    if (start < 0 || end < 0) return null;
    return json.substring(start + 1, end);
  }

  /** Convenience for the UI: is the game's current position one the tablebase would answer? */
  public static boolean isSolved(Game game) {
    return SolvedEndgames.isSolved(game.position());
  }
}
