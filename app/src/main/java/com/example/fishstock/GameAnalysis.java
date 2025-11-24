package com.example.fishstock;

import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class GameAnalysis extends AppCompatActivity {

  private ArrayList<Move> whitesMovesLog;
  private ArrayList<Move> blacksMovesLog;
  private boolean isFlipped = false;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_analysis);
    whitesMovesLog = (ArrayList<Move>) getIntent().getSerializableExtra("WHITE_MOVES");
    blacksMovesLog = (ArrayList<Move>) getIntent().getSerializableExtra("BLACK_MOVES");
    // Setup UI


  }
}