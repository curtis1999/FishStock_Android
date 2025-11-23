package com.example.fishstock;

import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GameAnalysis extends AppCompatActivity {

  private Game gameToAnalyze;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // 1. Reuse the existing Game Manager layout
    setContentView(R.layout.activity_main);

    // 2. Retrieve the Game object passed from the previous activity
    // Note: The 'Game' class and its internal classes (Board, Move, etc.) must implement Serializable
    if (getIntent().hasExtra("GAME_DATA")) {
      gameToAnalyze = (Game) getIntent().getSerializableExtra("GAME_DATA");
    }

    // 3. Initialize the same buttons and views so they are active/visible
    initializeViews();
  }

  private void initializeViews() {
    // Bind Standard Game Controls
    Button undoBtn = findViewById(R.id.undo);
    Button resignBtn = findViewById(R.id.resign);
    Button drawBtn = findViewById(R.id.draw);

    // Bind Text info
    TextView welcomeMsg = findViewById(R.id.welcomeMessage);
    TextView player1 = findViewById(R.id.player1);
    TextView player2 = findViewById(R.id.player2);

    // Bind the Board
    GridLayout gridLayout = findViewById(R.id.gridlayout);

    // Iterate through grid to bind buttons (logic for displaying pieces would go here)
    for (int i = 0; i < gridLayout.getChildCount(); i++) {
      if (gridLayout.getChildAt(i) instanceof ImageButton) {
        ImageButton square = (ImageButton) gridLayout.getChildAt(i);
        // Setup click listeners or piece images here based on gameToAnalyze
      }
    }

    // Bind Captured Pieces Counts (Top)
    TextView topQueens = findViewById(R.id.numCapturedTopQueens);
    TextView topRooks = findViewById(R.id.numCapturedTopRooks);
    TextView topKnights = findViewById(R.id.numCapturedTopKnights);
    TextView topBishops = findViewById(R.id.numCapturedTopBishops);

    // Bind Captured Pieces Counts (Bottom)
    TextView botQueens = findViewById(R.id.numCapturedBottomQueens);
    TextView botRooks = findViewById(R.id.numCapturedBottomRooks);
    TextView botKnights = findViewById(R.id.numCapturedBottomKnights);
    TextView botBishops = findViewById(R.id.numCapturedBottomBishops);

    // Hide the Game Over/Promotion dialogs initially so the board is visible for analysis
    findViewById(R.id.game_over_layout).setVisibility(android.view.View.GONE);
    findViewById(R.id.promotion_layout).setVisibility(android.view.View.GONE);
  }
}