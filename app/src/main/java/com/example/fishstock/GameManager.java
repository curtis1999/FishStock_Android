package com.example.fishstock;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.fishstock.Agents.*;

public class GameManager extends AppCompatActivity {
  Game game;
  Board board;
  Agent whitePlayer;
  Agent agent;
  boolean hasStarted = false;
  boolean isWhite = true;
  private BoardFragment mBoardFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);
    this.board = new Board();
    this.agent = initializeAgent(getIntent().getStringExtra("agentType"));
    // Create the board fragment and set the bundle as its arguments
    // Create a new instance of the BoardFragment
    mBoardFragment = BoardFragment.newInstance(this.board, this.agent);


    // Set the bundle as its arguments
    if (mBoardFragment.getArguments() == null) {
      mBoardFragment.setArguments(new Bundle());
    }
    mBoardFragment.getArguments().putSerializable("board", new Board());


    TextView adversaryName = findViewById(R.id.player2);
    adversaryName.setText(agent.getName());
    Button white = findViewById(R.id.white);
    Button black = findViewById(R.id.black);
    Button resign = findViewById(R.id.resign);
    Button undo = findViewById(R.id.undo);
    Button draw = findViewById(R.id.draw);

    TextView message = findViewById(R.id.welcomeMessage);
    if (isWhite && hasStarted) {
      message.setText("PLEASE MAKE YOUR FIRST MOVE");
    } else if (!isWhite && hasStarted) {
      message.setText("It is " + agent.getName() + "'s Turn");
    }
/*
    if (!isFinishing() && !isDestroyed()) {
      getSupportFragmentManager().beginTransaction().add(R.id.container, mBoardFragment).commitAllowingStateLoss();
    }

 */
/*
    try {
      gameLoop();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
 */
  }

  private void gameLoop() throws CloneNotSupportedException {
    boolean gameOver = false;
    BoardFragment boardFragment = (BoardFragment) getSupportFragmentManager().findFragmentById(R.id.board);
    boardFragment.updateBoard(board);
    hasStarted = true;
    // Loop until the game is over
    while (!gameOver) {
        //1. Make the Player's move
        makePlayerMove();
        boardFragment.updateBoard(board);
        //2.  Check if the game is over
        if (isGameOver()) {
          //TODO:
        }
        //3. Make the agent's move.
        Move curMove = agent.getMove(board, GameController.generateMoves(board, false), GameController.generateMoves(board, true));
        GameController.makeMove(board, curMove, false);
        GameController.updateBoardMeta(board);
        boardFragment.updateBoard(board);
    }
  }

  private void makePlayerMove() {
    // Wait for the player to click on a button in the board fragment
    mBoardFragment.setMoveListener(new MoveListener() {
      @Override
      public void onMove(Move move) {
        // Player made a move, update the board and send move to agent
        GameController.makeMove(board, move, true);
        GameController.updateBoardMeta(board);
      }
    });
  }


  public static Agent initializeAgent(String agentName) {
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

  public boolean isGameOver() {
    return false; //TODO:
  }

}