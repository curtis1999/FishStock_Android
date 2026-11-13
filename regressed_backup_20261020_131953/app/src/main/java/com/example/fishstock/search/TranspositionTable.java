package com.example.fishstock.search;

import com.example.fishstock.engine.Move;

/** Fixed-size memory of searched positions, indexed by hash. Newer entries replace older ones. */
final class TranspositionTable {
  static final int EXACT = 0;
  static final int LOWER = 1; // score is at least this (beta cut-off)
  static final int UPPER = 2; // score is at most this (no move raised alpha)

  static final class Entry {
    long key;
    int depth;
    double score;
    int flag;
    Move move;
  }

  private final Entry[] entries;
  private final int mask;

  TranspositionTable(int size) {
    entries = new Entry[size];
    mask = size - 1;
  }

  Entry probe(long key) {
    Entry e = entries[(int) (key & mask)];
    return e != null && e.key == key ? e : null;
  }

  void store(long key, int depth, double score, int flag, Move move) {
    int i = (int) (key & mask);
    Entry e = entries[i];
    if (e == null) {
      e = new Entry();
      entries[i] = e;
    }
    boolean samePosition = e.key == key;
    if (samePosition && e.depth > depth) return; // keep the deeper result for this position
    e.key = key;
    e.depth = depth;
    e.score = score;
    e.flag = flag;
    if (move != null || !samePosition) e.move = move;
  }

  void clear() {
    java.util.Arrays.fill(entries, null);
  }
}
