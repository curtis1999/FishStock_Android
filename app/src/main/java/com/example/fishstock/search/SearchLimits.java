package com.example.fishstock.search;

/**
 * How long / how deep to search.
 *
 * Iterative deepening searches depth 1, 2, 3, ... and stops when time runs out, so the depth
 * adapts to the phone's speed instead of being hard-coded.
 *  - {@code softTimeMs}: don't start a new depth after this much time (the next depth usually
 *    takes several times longer than the last one).
 *  - {@code hardTimeMs}: abandon the current depth and play the best move found so far.
 * With the MinMax defaults (1.5 s / 5 s) moves typically arrive in 2-5 seconds.
 */
public final class SearchLimits {
  public final int maxDepth;
  public final long softTimeMs;
  public final long hardTimeMs;

  public SearchLimits(int maxDepth, long softTimeMs, long hardTimeMs) {
    this.maxDepth = maxDepth;
    this.softTimeMs = softTimeMs;
    this.hardTimeMs = hardTimeMs;
  }

  /** Think for roughly the given time per move. */
  public static SearchLimits forTime(long targetMs) {
    return new SearchLimits(64, Math.max(1, targetMs * 3 / 10), Math.max(1, targetMs));
  }

  /** Fixed depth, no time limit (deterministic: handy for tests and training runs). */
  public static SearchLimits forDepth(int depth) {
    return new SearchLimits(depth, Long.MAX_VALUE / 4, Long.MAX_VALUE / 4);
  }
}
