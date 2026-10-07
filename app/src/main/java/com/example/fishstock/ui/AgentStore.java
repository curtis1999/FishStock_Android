package com.example.fishstock.ui;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.fishstock.agents.AgentSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Remembers custom agents and how you did against each opponent.
 * Stored in SharedPreferences as plain text (see {@link AgentSpec#serialize}).
 */
public final class AgentStore {
  private static final String PREFS = "fishstock";
  private static final String KEY_CUSTOM = "custom_agents";
  private static final String KEY_RESULT = "result_";
  private static final String KEY_BEATEN = "beaten_";

  private final SharedPreferences prefs;

  public AgentStore(Context context) {
    prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  public List<AgentSpec> customAgents() {
    List<AgentSpec> out = new ArrayList<>();
    String all = prefs.getString(KEY_CUSTOM, "");
    for (String line : all.split("\n")) {
      if (line.trim().isEmpty()) continue;
      try {
        out.add(AgentSpec.parse(line));
      } catch (RuntimeException ignored) {
        // skip a corrupt entry rather than crash
      }
    }
    return out;
  }

  public AgentSpec findCustom(String name) {
    for (AgentSpec s : customAgents()) if (s.name.equals(name)) return s;
    return null;
  }

  /** Saves (or replaces, by name) a custom agent. */
  public void saveCustom(AgentSpec spec) {
    List<AgentSpec> list = customAgents();
    for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).name.equals(spec.name)) list.remove(i);
    list.add(spec);
    write(list);
  }

  public void deleteCustom(String name) {
    List<AgentSpec> list = customAgents();
    for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).name.equals(name)) list.remove(i);
    write(list);
  }

  private void write(List<AgentSpec> list) {
    StringBuilder sb = new StringBuilder();
    for (AgentSpec s : list) sb.append(s.serialize()).append('\n');
    prefs.edit().putString(KEY_CUSTOM, sb.toString()).apply();
  }

  /** Records a finished game against an opponent: 1 win, 0 draw, -1 loss. */
  public void recordResult(String opponent, int result) {
    SharedPreferences.Editor e = prefs.edit().putInt(KEY_RESULT + opponent, result);
    if (result > 0) e.putBoolean(KEY_BEATEN + opponent, true);
    e.apply();
  }

  public boolean hasBeaten(String opponent) {
    return prefs.getBoolean(KEY_BEATEN + opponent, false);
  }

  /** Last result against this opponent (1, 0, -1), or 0 if never played. */
  public int lastResult(String opponent) {
    return prefs.getInt(KEY_RESULT + opponent, 0);
  }
}
