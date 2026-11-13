package com.example.fishstock;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Saved Games folder: games you chose to keep (from the analysis screen or an import), plus the
 * famous games ({@link FamousGames}) added the first time the folder is opened.
 *
 * Stored as one JSON array in SharedPreferences ("saved_games"), newest first. Each game keeps what
 * the analysis screen needs to reopen it: start position, moves (UCI), whose side to review from.
 */
public final class SavedGames {
  private static final String PREFS = "saved_games";
  private static final String KEY_GAMES = "games";
  /** Set once the famous games have been added, so deleting one doesn't bring it back. */
  private static final String KEY_SEEDED = "famous_seeded_v1";

  public static final class Entry {
    public final String id;
    public String name;
    public final String startFen;
    public String movesUci;
    /** 1 = review as White, -1 = as Black, 0 = both. */
    public final int side;
    /** Famous games: where and when, the result, and why it's worth studying. Empty for your own. */
    public final String event;
    public final String result;
    public final String note;
    public final boolean famous;
    public final long savedAt;

    Entry(String id, String name, String startFen, String movesUci, int side, String event,
          String result, String note, boolean famous, long savedAt) {
      this.id = id;
      this.name = name;
      this.startFen = startFen;
      this.movesUci = movesUci;
      this.side = side;
      this.event = event;
      this.result = result;
      this.note = note;
      this.famous = famous;
      this.savedAt = savedAt;
    }

    /** Full moves (rounded up): 33 plies = 17 moves. */
    public int moveCount() {
      String m = movesUci.trim();
      int plies = m.isEmpty() ? 0 : m.split("\\s+").length;
      return (plies + 1) / 2;
    }

    JSONObject toJson() throws JSONException {
      JSONObject o = new JSONObject();
      o.put("id", id);
      o.put("name", name);
      o.put("fen", startFen);
      o.put("moves", movesUci);
      o.put("side", side);
      o.put("event", event);
      o.put("result", result);
      o.put("note", note);
      o.put("famous", famous);
      o.put("savedAt", savedAt);
      return o;
    }

    static Entry fromJson(JSONObject o) {
      return new Entry(o.optString("id"), o.optString("name", "Saved game"), o.optString("fen", null),
          o.optString("moves", ""), o.optInt("side", 0), o.optString("event", ""),
          o.optString("result", ""), o.optString("note", ""), o.optBoolean("famous", false),
          o.optLong("savedAt", 0));
    }
  }

  private final SharedPreferences prefs;

  public SavedGames(Context context) {
    prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  /** Every saved game, your own newest first, then the famous games in their original order. */
  public List<Entry> all() {
    seedFamousGames();
    return load();
  }

  public Entry get(String id) {
    if (id == null) return null;
    for (Entry e : all()) if (e.id.equals(id)) return e;
    return null;
  }

  /** Saves a game of your own and returns its id. */
  public String add(String name, String startFen, String movesUci, int side) {
    List<Entry> list = all();
    Entry e = new Entry(UUID.randomUUID().toString(), clean(name), startFen, movesUci, side, "", "", "",
        false, System.currentTimeMillis());
    list.add(0, e);
    store(list);
    return e.id;
  }

  public void rename(String id, String name) {
    List<Entry> list = all();
    for (Entry e : list) if (e.id.equals(id)) e.name = clean(name);
    store(list);
  }

  /** An analysis-board line saved earlier and then extended: keep the same entry, new moves. */
  public void updateMoves(String id, String movesUci) {
    List<Entry> list = all();
    for (Entry e : list) if (e.id.equals(id)) e.movesUci = movesUci;
    store(list);
  }

  public void delete(String id) {
    List<Entry> list = all();
    for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).id.equals(id)) list.remove(i);
    store(list);
  }

  private static String clean(String name) {
    String n = name == null ? "" : name.trim();
    return n.isEmpty() ? "Saved game" : n;
  }

  private void seedFamousGames() {
    if (prefs.getBoolean(KEY_SEEDED, false)) return;
    List<Entry> list = load();
    for (FamousGames.Game g : FamousGames.ALL) {
      list.add(new Entry("famous:" + g.id, g.name, null, g.movesUci, g.side, g.event, g.result, g.note,
          true, 0));
    }
    store(list);
    prefs.edit().putBoolean(KEY_SEEDED, true).apply();
  }

  private List<Entry> load() {
    List<Entry> list = new ArrayList<>();
    try {
      JSONArray a = new JSONArray(prefs.getString(KEY_GAMES, "[]"));
      for (int i = 0; i < a.length(); i++) list.add(Entry.fromJson(a.getJSONObject(i)));
    } catch (JSONException ignored) {
      // A damaged list reads as empty rather than crashing the screen.
    }
    return list;
  }

  private void store(List<Entry> list) {
    JSONArray a = new JSONArray();
    try {
      for (Entry e : list) a.put(e.toJson());
    } catch (JSONException ignored) {
      return;
    }
    prefs.edit().putString(KEY_GAMES, a.toString()).apply();
  }
}
