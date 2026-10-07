package com.example.fishstock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.eval.Category;
import com.example.fishstock.ui.AgentStore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Make Your Own Agent": one slider per evaluation {@link Category} (0-200% of the default
 * weights) plus how long the agent thinks. Saved agents appear on the home screen (as an
 * opponent) and in the Tournament screen.
 *
 * Opened from the Tournament screen ({@link #EXTRA_FOR_TOURNAMENT}), the Play button becomes
 * "Use in Tournament": it saves the agent and goes back with it selected.
 */
public class AgentBuilderActivity extends AppCompatActivity {
  private static final int SLIDER_MAX = 200; // percent
  private static final long THINK_STEP_MS = 500;
  private static final String NEW_AGENT = "New agent";

  /** Boolean: opened from the Tournament screen. */
  public static final String EXTRA_FOR_TOURNAMENT = "forTournament";
  /** String: name of a saved agent to load when the screen opens. */
  public static final String EXTRA_EDIT_AGENT = "editAgent";
  /** Result string: name of the agent that was saved. */
  public static final String EXTRA_RESULT_AGENT = "savedAgent";

  private final Map<Category, SeekBar> sliders = new EnumMap<>(Category.class);
  private final Map<Category, TextView> sliderLabels = new EnumMap<>(Category.class);
  private AgentStore store;
  private EditText nameField;
  private SeekBar thinkSeek;
  private TextView thinkLabel;
  private Spinner savedSpinner;
  private boolean forTournament;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_agent_builder);
    store = new AgentStore(this);
    nameField = findViewById(R.id.agentName);
    thinkSeek = findViewById(R.id.thinkSeek);
    thinkLabel = findViewById(R.id.thinkLabel);
    savedSpinner = findViewById(R.id.savedAgentSpinner);
    forTournament = getIntent().getBooleanExtra(EXTRA_FOR_TOURNAMENT, false);

    buildSliders();
    thinkSeek.setOnSeekBarChangeListener(new SimpleSeekListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateThinkLabel();
      }
    });
    updateThinkLabel();
    refreshSavedList(getIntent().getStringExtra(EXTRA_EDIT_AGENT));

    savedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String name = (String) parent.getItemAtPosition(position);
        AgentSpec spec = NEW_AGENT.equals(name) ? null : store.findCustom(name);
        load(spec);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    ((Button) findViewById(R.id.saveAgent)).setOnClickListener(v -> {
      AgentSpec spec = currentSpec();
      if (spec == null) return;
      save(spec);
      Toast.makeText(this, forTournament ? "Saved " + spec.name
          : "Saved " + spec.name + ". It's on the home screen now.", Toast.LENGTH_SHORT).show();
    });
    Button play = findViewById(R.id.playAgent);
    if (forTournament) play.setText("Use in Tournament");
    play.setOnClickListener(v -> {
      AgentSpec spec = currentSpec();
      if (spec == null) return;
      save(spec);
      if (forTournament) {
        finish();
        return;
      }
      boolean white = getIntent().getBooleanExtra(GameManager.EXTRA_IS_WHITE, true);
      boolean hard = getIntent().getBooleanExtra(GameManager.EXTRA_HARD_MODE, false);
      startActivity(GameManager.newGameIntent(this, spec, white, hard));
    });
    ((Button) findViewById(R.id.deleteAgent)).setOnClickListener(v -> {
      String name = nameField.getText().toString().trim();
      if (store.findCustom(name) == null) return;
      store.deleteCustom(name);
      refreshSavedList(null);
      load(null);
      Toast.makeText(this, "Deleted " + name, Toast.LENGTH_SHORT).show();
    });
    ((Button) findViewById(R.id.exitBuilder)).setOnClickListener(v -> finish());
  }

  /** Saves the agent and, for the Tournament screen, reports it as the result. */
  private void save(AgentSpec spec) {
    store.saveCustom(spec);
    refreshSavedList(spec.name);
    setResult(RESULT_OK, new Intent().putExtra(EXTRA_RESULT_AGENT, spec.name));
  }

  private void buildSliders() {
    LinearLayout container = findViewById(R.id.sliderContainer);
    for (final Category c : Category.values()) {
      TextView label = new TextView(this);
      label.setTextColor(0xFF000000);
      label.setTextSize(16);
      label.setPadding(0, 16, 0, 0);
      SeekBar bar = new SeekBar(this);
      bar.setMax(SLIDER_MAX);
      bar.setProgress(100);
      bar.setMinimumHeight(96);
      bar.setOnSeekBarChangeListener(new SimpleSeekListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
          updateSliderLabel(c);
        }
      });
      container.addView(label);
      container.addView(bar);
      sliders.put(c, bar);
      sliderLabels.put(c, label);
      updateSliderLabel(c);
    }
  }

  private void updateSliderLabel(Category c) {
    sliderLabels.get(c).setText(String.format(Locale.US, "%s: %d%%", c.label, sliders.get(c).getProgress()));
  }

  private void updateThinkLabel() {
    int steps = thinkSeek.getProgress();
    if (steps == 0) {
      thinkLabel.setText("Thinking: looks one move ahead (like Simple)");
    } else {
      thinkLabel.setText(String.format(Locale.US, "Thinking: searches ahead for about %.1f s per move (like MinMax)",
          steps * THINK_STEP_MS / 1000.0));
    }
  }

  private void refreshSavedList(String select) {
    List<String> names = new ArrayList<>();
    names.add(NEW_AGENT);
    for (AgentSpec s : store.customAgents()) names.add(s.name);
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    savedSpinner.setAdapter(adapter);
    if (select != null) savedSpinner.setSelection(Math.max(0, names.indexOf(select)));
  }

  /** Fills the form from a saved agent, or resets it (null). */
  private void load(AgentSpec spec) {
    nameField.setText(spec == null ? "" : spec.name);
    for (Category c : Category.values()) {
      double f = spec == null ? 1.0 : spec.sliders.containsKey(c) ? spec.sliders.get(c) : 1.0;
      sliders.get(c).setProgress((int) Math.round(f * 100));
    }
    thinkSeek.setProgress(spec == null ? 0 : (int) Math.round(spec.thinkTimeMs / (double) THINK_STEP_MS));
    updateThinkLabel();
  }

  private AgentSpec currentSpec() {
    String name = nameField.getText().toString().trim();
    if (name.isEmpty()) {
      Toast.makeText(this, "Give your agent a name", Toast.LENGTH_SHORT).show();
      return null;
    }
    if (AgentSpec.isBuiltInName(name)) name = name + " 2";
    Map<Category, Double> factors = new EnumMap<>(Category.class);
    for (Category c : Category.values()) factors.put(c, sliders.get(c).getProgress() / 100.0);
    return AgentSpec.custom(name, factors, thinkSeek.getProgress() * THINK_STEP_MS);
  }

  /** SeekBar listener with only the method we need. */
  private abstract static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
  }
}
