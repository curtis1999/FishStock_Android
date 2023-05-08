package com.example.fishstock;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.fishstock.Agents.*;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;

public class GameManager extends AppCompatActivity {
  Game game;
  AgentType whitePlayer;
  AgentType blackPlayer;
  boolean hasStarted = false;
  boolean isWhite;
  private BoardFragment mBoardFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);
    Agent agent = initilizeAgent(getIntent().getStringExtra("agentType"));
    TextView adversaryName = findViewById(R.id.player2);
    adversaryName.setText(agent.getName());
    Button white = findViewById(R.id.white);
    Button black = findViewById(R.id.black);
    Button resign = findViewById(R.id.resign);
    Button undo = findViewById(R.id.undo);
    Button draw = findViewById(R.id.draw);
    white.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        isWhite = true;
        hasStarted = true;
      }
    });
    black.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        isWhite = false;
        hasStarted = true;
      }
    });

    TextView message = findViewById(R.id.welcomeMessage);
    if (isWhite && hasStarted) {
      message.setText("PLEASE MAKE YOUR FIRST MOVE");
    } else if (!isWhite && hasStarted) {
      message.setText("It is " + agent.getName() + "'s Turn");
    }
    Board board = new Board();

    // Create the board fragment and set the bundle as its arguments
    BoardFragment boardFragment = (BoardFragment) getSupportFragmentManager().findFragmentById(R.id.board);
    boardFragment.updateBoard(board);
  }

  public static Agent initilizeAgent(String agentName) {
    Agent agent;
    switch (agentName) {
      case "Randy":
        agent = new Randy(AgentType.RANDY, false); //TODO: ASK USER IF THEY WANT TO PLAY AS WHITE OR BLACK!
        break;
      default:
        agent = new FishStock(AgentType.FISHSTOCK, false);
    }
    return agent;
  }
}