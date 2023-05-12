package com.example.fishstock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    // Get a reference to the button
    Button playFishStock = findViewById(R.id.PlayFishStock);
    Button playRandy = findViewById(R.id.PlayRandy);
    Button playSimple = findViewById(R.id.playSimple);
    Button playMinMax = findViewById(R.id.playMinMax);
    // Set a click listener on the button
    playFishStock.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // Create an Intent to launch the Board activity
        Intent intent = new Intent(MainActivity.this, GameManager.class);
        intent.putExtra("agentType", "FishStock");
        // Start the activity
        startActivity(intent);
      }
    });
    playRandy.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(MainActivity.this, GameManager.class);
        intent.putExtra("agentType", "Randy");
        startActivity(intent);
      }
    });
    playSimple.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(MainActivity.this, GameManager.class);
        intent.putExtra("agentType", "Simple");
        startActivity(intent);
      }
    });
    playMinMax.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(MainActivity.this, GameManager.class);
        intent.putExtra("agentType", "MinMax");
        startActivity(intent);
      }
    });
  }
}