package com.example.fishstock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.ui.AgentStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opening screen: choose AGENT or HUMAN, then (against an agent) your colour and the opponent,
 * and press PLAY. Two human players share the phone with White at the bottom; the board doesn't turn.
 *
 * The dropdown lists the built-in agents weakest first (Level -1 Blunder ... Level 5 FishStock),
 * then the agents saved in "Make Your Own Agent" (EDIT opens them in the builder). Opponents you
 * have beaten are green; one that beat you last time is red. The last choice is remembered.
 */
public class MainActivity extends AppCompatActivity {
  private static final String PREFS = "fishstock";
  private static final String KEY_LAST_OPPONENT = "last_opponent";

  private boolean isWhite = true;
  private boolean isHardMode = false;
  private boolean vsHuman = false;

  private Spinner opponentSpinner;
  private TextView opponentDescription;
  private Button editAgent;
  private final List<AgentSpec> opponents = new ArrayList<>();
  private AgentStore store;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Button whiteSelection = findViewById(R.id.WHITE);
    Button blackSelection = findViewById(R.id.BLACK);
    opponentSpinner = findViewById(R.id.opponentSpinner);
    opponentDescription = findViewById(R.id.opponentDescription);
    editAgent = findViewById(R.id.editAgent);
    Button play = findViewById(R.id.playButton);
    Button vsAgentButton = findViewById(R.id.vsAgent);
    Button vsHumanButton = findViewById(R.id.vsHuman);
    View agentOptions = findViewById(R.id.agentOptions);
    View humanDescription = findViewById(R.id.humanDescription);
    Button puzzles = findViewById(R.id.puzzlesButton);
    Button makeAgent = findViewById(R.id.makeAgentButton);
    Button tournament = findViewById(R.id.tournamentButton);
    Switch hardMode = findViewById(R.id.hardModeSwitch);

    hardMode.setOnCheckedChangeListener((buttonView, isChecked) -> isHardMode = isChecked);
    vsAgentButton.setOnClickListener(v -> {
      vsHuman = false;
      vsAgentButton.setTextColor(Color.WHITE);
      vsHumanButton.setTextColor(Color.BLACK);
      agentOptions.setVisibility(View.VISIBLE);
      humanDescription.setVisibility(View.GONE);
    });
    vsHumanButton.setOnClickListener(v -> {
      vsHuman = true;
      vsHumanButton.setTextColor(Color.WHITE);
      vsAgentButton.setTextColor(Color.BLACK);
      agentOptions.setVisibility(View.GONE);
      humanDescription.setVisibility(View.VISIBLE);
    });
    blackSelection.setOnClickListener(v -> {
      isWhite = false;
      blackSelection.setTextColor(Color.WHITE);
      whiteSelection.setTextColor(Color.BLACK);
    });
    whiteSelection.setOnClickListener(v -> {
      isWhite = true;
      blackSelection.setTextColor(Color.BLACK);
      whiteSelection.setTextColor(Color.WHITE);
    });

    opponentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        onOpponentSelected(position);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    play.setOnClickListener(v -> {
      if (vsHuman) {
        play(AgentSpec.builtIn(AgentSpec.Kind.HUMAN));
        return;
      }
      AgentSpec spec = selected();
      if (spec == null) return;
      prefs().edit().putString(KEY_LAST_OPPONENT, spec.name).apply();
      play(spec);
    });
    editAgent.setOnClickListener(v -> {
      AgentSpec spec = selected();
      if (spec != null && spec.kind == AgentSpec.Kind.CUSTOM) openBuilder(spec.name);
    });

    puzzles.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, PuzzleActivity.class)));
    makeAgent.setOnClickListener(v -> openBuilder(null));
    tournament.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TournamentActivity.class)));
  }

  @Override
  protected void onResume() {
    super.onResume();
    store = new AgentStore(this);
    fillOpponents();
  }

  /** Built-in agents weakest first, then saved agents; reselects the last opponent played. */
  private void fillOpponents() {
    String keep = selected() != null ? selected().name : prefs().getString(KEY_LAST_OPPONENT, "Simple");
    opponents.clear();
    for (AgentSpec.Kind k : AgentSpec.OPPONENTS) opponents.add(AgentSpec.builtIn(k));
    opponents.addAll(store.customAgents());
    List<String> labels = new ArrayList<>();
    int select = 0;
    for (int i = 0; i < opponents.size(); i++) {
      AgentSpec s = opponents.get(i);
      labels.add(label(s));
      if (s.name.equals(keep)) select = i;
    }
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        return colour(super.getView(position, convertView, parent), position);
      }

      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return colour(super.getDropDownView(position, convertView, parent), position);
      }
    };
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    opponentSpinner.setAdapter(adapter);
    opponentSpinner.setSelection(select);
    onOpponentSelected(select);
  }

  /** "Level 3  ·  Agro" or "Custom  ·  My Agent", with a tick once beaten. */
  private String label(AgentSpec s) {
    String level = s.kind == AgentSpec.Kind.CUSTOM ? "Custom" : String.format(Locale.US, "Level %d", s.level());
    String beaten = store != null && store.hasBeaten(s.name) ? "  ✓" : "";
    return level + "  ·  " + s.name + beaten;
  }

  /** Green once beaten, red if it won the last game against you; otherwise the theme's colour. */
  private View colour(View row, int position) {
    if (!(row instanceof TextView) || position >= opponents.size() || store == null) return row;
    String name = opponents.get(position).name;
    TextView t = (TextView) row;
    if (t.getTag() == null) t.setTag(t.getTextColors()); // rows are recycled: remember the normal colour
    if (store.hasBeaten(name)) t.setTextColor(0xFF12CC2B);
    else if (store.lastResult(name) < 0) t.setTextColor(0xFFE40D0D);
    else t.setTextColor((ColorStateList) t.getTag());
    return row;
  }

  private void onOpponentSelected(int position) {
    if (position < 0 || position >= opponents.size()) return;
    AgentSpec s = opponents.get(position);
    opponentDescription.setText(s.description());
    editAgent.setVisibility(s.kind == AgentSpec.Kind.CUSTOM ? View.VISIBLE : View.GONE);
  }

  private AgentSpec selected() {
    if (opponentSpinner == null || opponents.isEmpty()) return null;
    int i = opponentSpinner.getSelectedItemPosition();
    return i >= 0 && i < opponents.size() ? opponents.get(i) : null;
  }

  private SharedPreferences prefs() {
    return getSharedPreferences(PREFS, MODE_PRIVATE);
  }

  private void openBuilder(String editAgentName) {
    Intent intent = new Intent(MainActivity.this, AgentBuilderActivity.class);
    intent.putExtra(GameManager.EXTRA_IS_WHITE, isWhite);
    intent.putExtra(GameManager.EXTRA_HARD_MODE, isHardMode);
    if (editAgentName != null) intent.putExtra(AgentBuilderActivity.EXTRA_EDIT_AGENT, editAgentName);
    startActivity(intent);
  }

  private void play(AgentSpec opponent) {
    startActivity(GameManager.newGameIntent(this, opponent, isWhite, isHardMode));
  }
}
