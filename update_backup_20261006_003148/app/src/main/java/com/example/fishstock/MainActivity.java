package com.example.fishstock;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.agents.AgentSpec;
import com.example.fishstock.ui.AgentStore;

/**
 * Opening screen: pick a colour, then an opponent. Opponents you have beaten turn green;
 * one that beat you last time turns red.
 */
public class MainActivity extends AppCompatActivity {
  private boolean isWhite = true;
  private boolean isHardMode = false;

  private Button playRandy;
  private Button playSimple;
  private Button playMinMax;
  private Button playFishStock;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Button whiteSelection = findViewById(R.id.WHITE);
    Button blackSelection = findViewById(R.id.BLACK);
    playRandy = findViewById(R.id.PlayRandy);
    playSimple = findViewById(R.id.playSimple);
    playMinMax = findViewById(R.id.playMinMax);
    playFishStock = findViewById(R.id.PlayFishStock);
    Button playHuman = findViewById(R.id.playHuman);
    Button makeAgent = findViewById(R.id.makeAgentButton);
    Button tournament = findViewById(R.id.tournamentButton);
    Switch hardMode = findViewById(R.id.hardModeSwitch);

    hardMode.setOnCheckedChangeListener((buttonView, isChecked) -> isHardMode = isChecked);
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

    playRandy.setOnClickListener(v -> play(AgentSpec.Kind.RANDY));
    playSimple.setOnClickListener(v -> play(AgentSpec.Kind.SIMPLE));
    playMinMax.setOnClickListener(v -> play(AgentSpec.Kind.MINMAX));
    playFishStock.setOnClickListener(v -> play(AgentSpec.Kind.FISHSTOCK));
    playHuman.setOnClickListener(v -> play(AgentSpec.Kind.HUMAN));

    makeAgent.setOnClickListener(v -> {
      Intent intent = new Intent(MainActivity.this, AgentBuilderActivity.class);
      intent.putExtra(GameManager.EXTRA_IS_WHITE, isWhite);
      intent.putExtra(GameManager.EXTRA_HARD_MODE, isHardMode);
      startActivity(intent);
    });
    tournament.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TournamentActivity.class)));
  }

  @Override
  protected void onResume() {
    super.onResume();
    AgentStore store = new AgentStore(this);
    colourByResult(playRandy, "Randy", store);
    colourByResult(playSimple, "Simple", store);
    colourByResult(playMinMax, "MinMax", store);
    colourByResult(playFishStock, "FishStock", store);
  }

  private void play(AgentSpec.Kind kind) {
    startActivity(GameManager.newGameIntent(this, AgentSpec.builtIn(kind), isWhite, isHardMode));
  }

  private static void colourByResult(Button button, String opponent, AgentStore store) {
    if (store.hasBeaten(opponent)) {
      button.setTextColor(Color.GREEN);
    } else if (store.lastResult(opponent) < 0) {
      button.setTextColor(Color.RED);
    }
  }
}
