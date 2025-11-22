package com.example.fishstock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
  boolean isWhite = true;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    int result = getIntent().getIntExtra("Result", 0);
    String adversaryName = getIntent().getStringExtra("Adversary");
    // Get a reference to the button
    Button whiteSelection = findViewById(R.id.WHITE);
    Button blackSelection = findViewById(R.id.BLACK);
    Button playFishStock = findViewById(R.id.PlayFishStock);
    Button playRandy = findViewById(R.id.PlayRandy);
    Button playSimple = findViewById(R.id.playSimple);
    Button playMinMax = findViewById(R.id.playMinMax);
    Button playHuman = findViewById(R.id.playHuman);
    if (adversaryName != null) {
      switch (adversaryName) {
        case "Randy":
          if (result == 1) {
            playRandy.setTextColor(Color.GREEN);
          } else if (result == -1) {
            playRandy.setTextColor(Color.RED);
          }
          break;
        case "Simple":
          if (result == 1) {
            playSimple.setTextColor(Color.GREEN);
          } else if (result == -1) {
            playSimple.setTextColor(Color.RED);
          }
          break;
      }
    }
    blackSelection.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        isWhite = false;
        blackSelection.setTextColor(Color.WHITE);
        whiteSelection.setTextColor(Color.BLACK);
      }
    });
    whiteSelection.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        isWhite = true;
        blackSelection.setTextColor(Color.BLACK);
        whiteSelection.setTextColor(Color.WHITE);
      }
    });
    // Set a click listener on the button
    playFishStock.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // Create an Intent to launch the Board activity
        Intent intent = new Intent(MainActivity.this, GameManager.class);
        intent.putExtra("agentType", "FishStock");
        intent.putExtra("isWhite", isWhite);
        // Start the activity
        startActivity(intent);
      }
    });
    playRandy.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(MainActivity.this, GameManager.class);
        intent.putExtra("agentType", "Randy");
        intent.putExtra("isWhite", isWhite);
        startActivity(intent);
      }
    });
    playSimple.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(MainActivity.this, GameManager.class);
        intent.putExtra("agentType", "Simple");
        intent.putExtra("isWhite", isWhite);
        startActivity(intent);
      }
    });
    playMinMax.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(MainActivity.this, GameManager.class);
        intent.putExtra("agentType", "MinMax");
        intent.putExtra("isWhite", isWhite);
        startActivity(intent);
      }
    });
    playHuman.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(MainActivity.this, GameManager.class);
        intent.putExtra("agentType", "Human");
        intent.putExtra("isWhite", true);
        startActivity(intent);
      }
    });
    // Tournament buttom
    Button tournamentButton = findViewById(R.id.tournamentButton);
    tournamentButton.setOnClickListener(v -> {
      Intent intent = new Intent(MainActivity.this, TournamentActivity.class);
      startActivity(intent);
    });
  }
}