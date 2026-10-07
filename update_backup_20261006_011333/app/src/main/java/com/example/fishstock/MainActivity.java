package com.example.fishstock;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.ui.AgentStore;

/**
 * Opening screen: pick a colour, then an opponent. Opponents you have beaten turn green;
 * one that beat you last time turns red.
 *
 * Agents saved in "Make Your Own Agent" are listed under the built-in levels. Tap one to play
 * it; long-press it to edit it.
 */
public class MainActivity extends AppCompatActivity {
  private boolean isWhite = true;
  private boolean isHardMode = false;
  private boolean autoFlip = true;

  private Button playBlunder;
  private Button playRandy;
  private Button playSimple;
  private Button playMinMax;
  private Button playFishStock;
  private LinearLayout customAgentsLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Button whiteSelection = findViewById(R.id.WHITE);
    Button blackSelection = findViewById(R.id.BLACK);
    playBlunder = findViewById(R.id.playBlunder);
    playRandy = findViewById(R.id.PlayRandy);
    playSimple = findViewById(R.id.playSimple);
    playMinMax = findViewById(R.id.playMinMax);
    playFishStock = findViewById(R.id.PlayFishStock);
    customAgentsLayout = findViewById(R.id.customAgentsLayout);
    Button playHuman = findViewById(R.id.playHuman);
    Button puzzles = findViewById(R.id.puzzlesButton);
    Button makeAgent = findViewById(R.id.makeAgentButton);
    Button tournament = findViewById(R.id.tournamentButton);
    Switch hardMode = findViewById(R.id.hardModeSwitch);
    Switch autoFlipSwitch = findViewById(R.id.autoFlipSwitch);

    hardMode.setOnCheckedChangeListener((buttonView, isChecked) -> isHardMode = isChecked);
    autoFlipSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> autoFlip = isChecked);
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

    playBlunder.setOnClickListener(v -> play(AgentSpec.builtIn(AgentSpec.Kind.BLUNDER)));
    playRandy.setOnClickListener(v -> play(AgentSpec.builtIn(AgentSpec.Kind.RANDY)));
    playSimple.setOnClickListener(v -> play(AgentSpec.builtIn(AgentSpec.Kind.SIMPLE)));
    playMinMax.setOnClickListener(v -> play(AgentSpec.builtIn(AgentSpec.Kind.MINMAX)));
    playFishStock.setOnClickListener(v -> play(AgentSpec.builtIn(AgentSpec.Kind.FISHSTOCK)));
    playHuman.setOnClickListener(v -> play(AgentSpec.builtIn(AgentSpec.Kind.HUMAN)));

    puzzles.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, PuzzleActivity.class)));
    makeAgent.setOnClickListener(v -> openBuilder(null));
    tournament.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TournamentActivity.class)));
  }

  @Override
  protected void onResume() {
    super.onResume();
    AgentStore store = new AgentStore(this);
    colourByResult(playBlunder, "Blunder", store);
    colourByResult(playRandy, "Randy", store);
    colourByResult(playSimple, "Simple", store);
    colourByResult(playMinMax, "MinMax", store);
    colourByResult(playFishStock, "FishStock", store);
    showCustomAgents(store);
  }

  /** One row per saved agent, styled like the built-in level rows. */
  private void showCustomAgents(AgentStore store) {
    customAgentsLayout.removeAllViews();
    float density = getResources().getDisplayMetrics().density;
    for (final AgentSpec spec : store.customAgents()) {
      LinearLayout row = new LinearLayout(this);
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setGravity(Gravity.CENTER_VERTICAL);
      LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      rowParams.topMargin = Math.round(12 * density);

      TextView label = new TextView(this);
      label.setText("Custom");
      label.setTextSize(20);
      label.setGravity(Gravity.CENTER);
      row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

      Button button = new Button(this);
      button.setText(spec.name);
      button.setOnClickListener(v -> play(spec));
      button.setOnLongClickListener(v -> {
        openBuilder(spec.name);
        return true;
      });
      colourByResult(button, spec.name, store);
      row.addView(button, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

      customAgentsLayout.addView(row, rowParams);
    }
  }

  private void openBuilder(String editAgent) {
    Intent intent = new Intent(MainActivity.this, AgentBuilderActivity.class);
    intent.putExtra(GameManager.EXTRA_IS_WHITE, isWhite);
    intent.putExtra(GameManager.EXTRA_HARD_MODE, isHardMode);
    if (editAgent != null) intent.putExtra(AgentBuilderActivity.EXTRA_EDIT_AGENT, editAgent);
    startActivity(intent);
  }

  private void play(AgentSpec opponent) {
    Intent intent = GameManager.newGameIntent(this, opponent, isWhite, isHardMode);
    intent.putExtra(GameManager.EXTRA_AUTO_FLIP, autoFlip);
    startActivity(intent);
  }

  private static void colourByResult(Button button, String opponent, AgentStore store) {
    if (store.hasBeaten(opponent)) {
      button.setTextColor(Color.GREEN);
    } else if (store.lastResult(opponent) < 0) {
      button.setTextColor(Color.RED);
    }
  }
}
