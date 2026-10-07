package com.example.fishstock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.agents.AgentFactory;
import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.arena.Arena;
import com.example.fishstock.arena.MatchResult;
import com.example.fishstock.ui.AgentStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent vs agent matches. Runs the {@link Arena} on a background thread, shows the running
 * score, and appends every finished match to tournament_results.csv in the app's files folder
 * so win rates are kept between sessions.
 *
 * MAKE YOUR OWN AGENT opens the agent builder; the agent you save there comes straight back
 * as Agent 1, ready to be tested against a built-in agent (or an earlier version of itself).
 */
public class TournamentActivity extends AppCompatActivity {
  private static final String RESULTS_FILE = "tournament_results.csv";
  private static final String[] GAME_OPTIONS = {"10", "20", "50", "100", "200"};
  private static final String[] TIME_OPTIONS = {"0.1 s", "0.25 s", "0.5 s", "1 s", "3 s"};
  private static final long[] TIME_VALUES_MS = {100, 250, 500, 1000, 3000};

  private Spinner agent1Spinner;
  private Spinner agent2Spinner;
  private Spinner numGamesSpinner;
  private Spinner thinkTimeSpinner;
  private Button startButton;
  private Button exportButton;
  private Button makeAgentButton;
  private CheckBox openingsCheck;
  private ProgressBar progressBar;
  private TextView progressText;
  private TextView resultsText;

  private final List<AgentSpec> agents = new ArrayList<>();
  private AtomicBoolean stop = new AtomicBoolean(false);
  private volatile boolean running;

  /** Opens the agent builder and selects whatever agent it saved. */
  private final ActivityResultLauncher<Intent> builderLauncher = registerForActivityResult(
      new ActivityResultContracts.StartActivityForResult(), result -> {
        String saved = result.getData() == null ? null
            : result.getData().getStringExtra(AgentBuilderActivity.EXTRA_RESULT_AGENT);
        setupSpinners(saved);
        if (saved != null) {
          resultsText.setText("Agent 1 is now " + saved + ".\nPick an opponent and press Start Tournament.");
          resultsText.setTextColor(0xFF000000);
        }
      });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_tournament);
    agent1Spinner = findViewById(R.id.agent1Spinner);
    agent2Spinner = findViewById(R.id.agent2Spinner);
    numGamesSpinner = findViewById(R.id.numGamesSpinner);
    thinkTimeSpinner = findViewById(R.id.thinkTimeSpinner);
    startButton = findViewById(R.id.startTournament);
    exportButton = findViewById(R.id.exportResults);
    makeAgentButton = findViewById(R.id.tournamentMakeAgent);
    openingsCheck = findViewById(R.id.varyOpenings);
    progressBar = findViewById(R.id.progressBar);
    progressText = findViewById(R.id.progressText);
    resultsText = findViewById(R.id.results);
    Button exitButton = findViewById(R.id.exitButton);

    setupSpinners(null);
    makeAgentButton.setOnClickListener(v -> {
      Intent intent = new Intent(TournamentActivity.this, AgentBuilderActivity.class);
      intent.putExtra(AgentBuilderActivity.EXTRA_FOR_TOURNAMENT, true);
      AgentSpec current = agents.get(agent1Spinner.getSelectedItemPosition());
      if (current.kind == AgentSpec.Kind.CUSTOM) intent.putExtra(AgentBuilderActivity.EXTRA_EDIT_AGENT, current.name);
      builderLauncher.launch(intent);
    });
    startButton.setOnClickListener(v -> startTournament());
    exitButton.setOnClickListener(v -> {
      stop.set(true);
      Intent intent = new Intent(TournamentActivity.this, MainActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      finish();
    });
    exportButton.setOnClickListener(v -> exportResults());
    progressText.setText(String.format(Locale.US, "Ready to start (%d matches recorded)", historyLines().size()));
  }

  @Override
  protected void onDestroy() {
    stop.set(true);
    super.onDestroy();
  }

  /**
   * Fills the agent lists (built-in agents, then your saved ones). Keeps the current choices,
   * and puts {@code selectAsAgent1} (a newly saved agent) in the Agent 1 slot.
   */
  private void setupSpinners(String selectAsAgent1) {
    boolean first = agents.isEmpty();
    String keep1 = first ? null : agents.get(agent1Spinner.getSelectedItemPosition()).name;
    String keep2 = first ? null : agents.get(agent2Spinner.getSelectedItemPosition()).name;
    agents.clear();
    for (AgentSpec.Kind k : AgentSpec.LEVELS) agents.add(AgentSpec.builtIn(k));
    agents.addAll(new AgentStore(this).customAgents());
    List<String> names = new ArrayList<>();
    for (AgentSpec s : agents) names.add(s.name);

    agent1Spinner.setAdapter(adapter(names.toArray(new String[0])));
    agent2Spinner.setAdapter(adapter(names.toArray(new String[0])));
    String want1 = selectAsAgent1 != null ? selectAsAgent1 : keep1 != null ? keep1 : "Simple";
    String want2 = keep2 != null && !keep2.equals(want1) ? keep2 : want1.equals("MinMax") ? "Simple" : "MinMax";
    agent1Spinner.setSelection(Math.max(0, names.indexOf(want1)));
    agent2Spinner.setSelection(Math.max(0, names.indexOf(want2)));
    if (first) {
      numGamesSpinner.setAdapter(adapter(GAME_OPTIONS));
      numGamesSpinner.setSelection(1);
      thinkTimeSpinner.setAdapter(adapter(TIME_OPTIONS));
      thinkTimeSpinner.setSelection(1);
    }
  }

  private ArrayAdapter<String> adapter(String[] items) {
    ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    return a;
  }

  private void startTournament() {
    if (running) return;
    final AgentSpec a = agents.get(agent1Spinner.getSelectedItemPosition());
    final AgentSpec b = agents.get(agent2Spinner.getSelectedItemPosition());
    final int games = Integer.parseInt(GAME_OPTIONS[numGamesSpinner.getSelectedItemPosition()]);
    final long thinkMs = TIME_VALUES_MS[thinkTimeSpinner.getSelectedItemPosition()];
    final boolean varyOpenings = openingsCheck.isChecked();
    if (a.name.equals(b.name)) {
      resultsText.setText("Please select two different agents!");
      resultsText.setTextColor(0xFFFF0000);
      return;
    }
    setControlsEnabled(false);
    exportButton.setVisibility(View.GONE);
    progressBar.setVisibility(View.VISIBLE);
    progressBar.setProgress(0);
    resultsText.setText("Starting tournament...");
    resultsText.setTextColor(0xFF000000);
    stop = new AtomicBoolean(false);
    final AtomicBoolean myStop = stop;
    running = true;

    new Thread(() -> {
      MatchResult result = Arena.playMatch(
          () -> AgentFactory.create(a, false, thinkMs),
          () -> AgentFactory.create(b, false, thinkMs),
          games, Arena.DEFAULT_MAX_PLIES, varyOpenings,
          (done, total, game, soFar) -> {
            final String text = "TOURNAMENT IN PROGRESS\n======================\n\n" + soFar.summary();
            runOnUiThread(() -> {
              progressBar.setProgress(done * 100 / total);
              progressText.setText(String.format(Locale.US, "Game %d/%d completed", done, total));
              resultsText.setText(text);
            });
          },
          myStop);
      if (!myStop.get() && result.gamesPlayed() > 0) appendToHistory(result);
      final MatchResult finalResult = result;
      runOnUiThread(() -> {
        running = false;
        if (isFinishing()) return;
        showFinal(finalResult);
        progressBar.setVisibility(View.GONE);
        progressText.setText(String.format(Locale.US, "Tournament Complete! (%d matches recorded)", historyLines().size()));
        setControlsEnabled(true);
        exportButton.setVisibility(View.VISIBLE);
      });
    }).start();
  }

  private void showFinal(MatchResult r) {
    StringBuilder sb = new StringBuilder("TOURNAMENT RESULTS\n==================\n\n");
    sb.append(r.summary()).append('\n');
    if (r.winsA() > r.winsB()) sb.append("WINNER: ").append(r.nameA).append('\n');
    else if (r.winsB() > r.winsA()) sb.append("WINNER: ").append(r.nameB).append('\n');
    else sb.append("RESULT: TIE\n");
    resultsText.setText(sb.toString());
    resultsText.setTextColor(0xFF000000);
  }

  private void setControlsEnabled(boolean enabled) {
    startButton.setEnabled(enabled);
    agent1Spinner.setEnabled(enabled);
    agent2Spinner.setEnabled(enabled);
    numGamesSpinner.setEnabled(enabled);
    thinkTimeSpinner.setEnabled(enabled);
    makeAgentButton.setEnabled(enabled);
    openingsCheck.setEnabled(enabled);
  }

  // ---------------------------------------------------------------- results history

  private File historyFile() {
    return new File(getFilesDir(), RESULTS_FILE);
  }

  private synchronized void appendToHistory(MatchResult r) {
    File f = historyFile();
    boolean isNew = !f.exists();
    try (FileWriter w = new FileWriter(f, true)) {
      if (isNew) w.write(MatchResult.csvHeader() + "\n");
      w.write(r.toCsvLine(System.currentTimeMillis()) + "\n");
    } catch (IOException ignored) {
      // History is a nice-to-have; the result is still on screen.
    }
  }

  private List<String> historyLines() {
    List<String> lines = new ArrayList<>();
    File f = historyFile();
    if (!f.exists()) return lines;
    try (BufferedReader r = new BufferedReader(new FileReader(f))) {
      String line;
      boolean header = true;
      while ((line = r.readLine()) != null) {
        if (header) {
          header = false;
          continue;
        }
        if (!line.trim().isEmpty()) lines.add(line);
      }
    } catch (IOException ignored) {
      // treat as empty
    }
    return lines;
  }

  private void exportResults() {
    StringBuilder text = new StringBuilder(resultsText.getText());
    text.append("\n\nALL RECORDED MATCHES (CSV)\n").append(MatchResult.csvHeader()).append('\n');
    for (String line : historyLines()) text.append(line).append('\n');
    Intent share = new Intent(Intent.ACTION_SEND);
    share.setType("text/plain");
    share.putExtra(Intent.EXTRA_SUBJECT, "Chess Tournament Results");
    share.putExtra(Intent.EXTRA_TEXT, text.toString());
    startActivity(Intent.createChooser(share, "Share Results"));
  }
}
