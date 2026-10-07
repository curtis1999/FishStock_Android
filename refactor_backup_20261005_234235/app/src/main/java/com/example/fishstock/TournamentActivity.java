package com.example.fishstock;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.fishstock.Agents.*;
import com.example.fishstock.Pieces.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TournamentActivity extends AppCompatActivity {

  private Spinner agent1Spinner;
  private Spinner agent2Spinner;
  private Spinner numGamesSpinner;
  private Button startButton;
  private Button exitButton;
  private ProgressBar progressBar;
  private TextView progressText;
  private TextView resultsText;
  private Button exportButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_tournament);

    // Initialize UI elements
    agent1Spinner = findViewById(R.id.agent1Spinner);
    agent2Spinner = findViewById(R.id.agent2Spinner);
    numGamesSpinner = findViewById(R.id.numGamesSpinner);
    startButton = findViewById(R.id.startTournament);
    exitButton = findViewById(R.id.exitButton);
    progressBar = findViewById(R.id.progressBar);
    progressText = findViewById(R.id.progressText);
    resultsText = findViewById(R.id.results);
    exportButton = findViewById(R.id.exportResults);

    // Setup spinners
    setupSpinners();

    // Setup button listeners
    startButton.setOnClickListener(v -> startTournament());
    exitButton.setOnClickListener(v -> {
      Intent intent = new Intent(TournamentActivity.this, MainActivity.class);
      startActivity(intent);
      finish();
    });

    exportButton.setOnClickListener(v -> exportResults());
  }

  /**
   * Sets up the spinner dropdowns with agent names and game counts.
   */
  private void setupSpinners() {
    // Agent options
    String[] agents = {"Randy", "Simple", "MinMax", "FishStock"};
    ArrayAdapter<String> agentAdapter = new ArrayAdapter<>(
        this,
        android.R.layout.simple_spinner_item,
        agents
    );
    agentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    agent1Spinner.setAdapter(agentAdapter);
    agent2Spinner.setAdapter(agentAdapter);
    agent2Spinner.setSelection(1); // Default to second agent

    // Number of games options
    String[] gameOptions = {"10", "20", "50", "100", "200"};
    ArrayAdapter<String> gamesAdapter = new ArrayAdapter<>(
        this,
        android.R.layout.simple_spinner_item,
        gameOptions
    );
    gamesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    numGamesSpinner.setAdapter(gamesAdapter);
    numGamesSpinner.setSelection(1); // Default to 20 games
  }

  /**
   * Starts the tournament with selected agents and number of games.
   */
  private void startTournament() {
    String agent1Name = agent1Spinner.getSelectedItem().toString();
    String agent2Name = agent2Spinner.getSelectedItem().toString();
    int numGames = Integer.parseInt(numGamesSpinner.getSelectedItem().toString());

    // Check if same agent selected
    if (agent1Name.equals(agent2Name)) {
      resultsText.setText("Please select two different agents!");
      resultsText.setTextColor(0xFFFF0000); // Red color
      return;
    }

    // Disable controls during tournament
    startButton.setEnabled(false);
    agent1Spinner.setEnabled(false);
    agent2Spinner.setEnabled(false);
    numGamesSpinner.setEnabled(false);
    exportButton.setVisibility(View.GONE);
    progressBar.setVisibility(View.VISIBLE);
    progressBar.setProgress(0);
    resultsText.setText("Starting tournament...");
    resultsText.setTextColor(0xFF000000); // Black color

    // Run tournament in background thread
    new Thread(() -> runTournament(agent1Name, agent2Name, numGames)).start();
  }

  /**
   * Runs the tournament between two agents.
   */
  private void runTournament(String agent1Name, String agent2Name, int numGames) {
    int agent1Wins = 0;
    int agent2Wins = 0;
    int draws = 0;

    for (int game = 0; game < numGames; game++) {
      boolean agent1White = (game % 2 == 0); // Alternate colors
      int result = playGame(agent1Name, agent2Name, agent1White);

      if (result == 1) {
        agent1Wins++;
      } else if (result == -1) {
        agent2Wins++;
      } else {
        draws++;
      }

      // Update progress
      int finalGame = game + 1;
      int finalAgent1Wins = agent1Wins;
      int finalAgent2Wins = agent2Wins;
      int finalDraws = draws;

      runOnUiThread(() -> {
        progressBar.setProgress((finalGame * 100) / numGames);
        progressText.setText(String.format("Game %d/%d completed", finalGame, numGames));

        // Show running results
        displayRunningResults(agent1Name, agent2Name, finalAgent1Wins, finalAgent2Wins, finalDraws, finalGame);
      });
    }

    // Calculate final ELO and display results
    int finalAgent1Wins = agent1Wins;
    int finalAgent2Wins = agent2Wins;
    int finalDraws = draws;

    runOnUiThread(() -> {
      displayFinalResults(agent1Name, agent2Name, finalAgent1Wins, finalAgent2Wins, finalDraws, numGames);
      progressBar.setVisibility(View.GONE);
      progressText.setText("Tournament Complete!");
      startButton.setEnabled(true);
      agent1Spinner.setEnabled(true);
      agent2Spinner.setEnabled(true);
      numGamesSpinner.setEnabled(true);
      exportButton.setVisibility(View.VISIBLE);
    });
  }

  /**
   * Displays running results during the tournament.
   */
  private void displayRunningResults(String agent1, String agent2, int wins1, int wins2, int draws, int gamesPlayed) {
    StringBuilder sb = new StringBuilder();
    sb.append("TOURNAMENT IN PROGRESS\n");
    sb.append("======================\n\n");
    sb.append(String.format("%s vs %s\n\n", agent1, agent2));
    sb.append(String.format("Games Played: %d\n\n", gamesPlayed));
    sb.append(String.format("%s: %d wins\n", agent1, wins1));
    sb.append(String.format("%s: %d wins\n", agent2, wins2));
    sb.append(String.format("Draws: %d\n", draws));

    resultsText.setText(sb.toString());
    resultsText.setTextColor(0xFF000000); // Black color
  }

  /**
   * Displays final tournament results with ELO ratings.
   */
  private void displayFinalResults(String agent1, String agent2, int wins1, int wins2, int draws, int totalGames) {
    StringBuilder sb = new StringBuilder();
    sb.append("TOURNAMENT RESULTS\n");
    sb.append("==================\n\n");
    sb.append(String.format("%s vs %s\n", agent1, agent2));
    sb.append(String.format("Total Games: %d\n\n", totalGames));

    sb.append("SCORES:\n");
    sb.append("-------\n");
    sb.append(String.format("%s: %d wins, %d losses, %d draws\n",
        agent1, wins1, wins2, draws));
    sb.append(String.format("%s: %d wins, %d losses, %d draws\n\n",
        agent2, wins2, wins1, draws));

    // Calculate win percentages
    double agent1Score = wins1 + 0.5 * draws;
    double agent2Score = wins2 + 0.5 * draws;
    double agent1WinRate = (agent1Score / totalGames) * 100;
    double agent2WinRate = (agent2Score / totalGames) * 100;

    sb.append("WIN RATES:\n");
    sb.append("----------\n");
    sb.append(String.format("%s: %.1f%%\n", agent1, agent1WinRate));
    sb.append(String.format("%s: %.1f%%\n\n", agent2, agent2WinRate));

    // Calculate ELO ratings (simple performance-based)
    int baseELO = 1500;
    int agent1ELO = baseELO + (int)((agent1WinRate / 100.0 - 0.5) * 800);
    int agent2ELO = baseELO + (int)((agent2WinRate / 100.0 - 0.5) * 800);

    sb.append("ESTIMATED ELO:\n");
    sb.append("--------------\n");
    sb.append(String.format("%s: %d ELO\n", agent1, agent1ELO));
    sb.append(String.format("%s: %d ELO\n\n", agent2, agent2ELO));

    // Determine winner
    if (wins1 > wins2) {
      sb.append(String.format("WINNER: %s\n", agent1));
    } else if (wins2 > wins1) {
      sb.append(String.format("WINNER: %s\n", agent2));
    } else {
      sb.append("RESULT: TIE\n");
    }

    resultsText.setText(sb.toString());
    resultsText.setTextColor(0xFF000000); // Black color
  }

  /**
   * Plays a single game between two agents.
   * Returns: 1 if agent1 wins, -1 if agent2 wins, 0 for draw
   */
  private int playGame(String agent1Name, String agent2Name, boolean agent1White) {
    Board board = new Board();
    GameService.updateBoardMeta(board);

    Agent agent1 = GameManager.initializeAgent(agent1Name, agent1White);
    Agent agent2 = GameManager.initializeAgent(agent2Name, !agent1White);

    int moveCount = 0;
    int maxMoves = 200; // Prevent infinite games

    try {
      while (moveCount < maxMoves) {
        boolean isWhiteTurn = (moveCount % 2 == 0);
        Agent currentAgent = isWhiteTurn ?
            (agent1White ? agent1 : agent2) :
            (agent1White ? agent2 : agent1);

        ArrayList<Move> moves = GameService.generateMoves(board, isWhiteTurn);

        // Handle check/double check
        King king = isWhiteTurn ?
            (King) board.whitePieces.get(0) :
            (King) board.blackPieces.get(0);

        if (king.isDoubleChecked) {
          moves = GameService.generateMovesDoubleCheck(board, moves, isWhiteTurn);
        } else if (king.isChecked) {
          moves = GameService.generateMovesCheck(board, moves, isWhiteTurn);
        }

        // Check for checkmate or stalemate
        if (moves.isEmpty()) {
          if (king.isChecked || king.isDoubleChecked) {
            // Checkmate - determine winner
            boolean whiteWon = !isWhiteTurn;
            return (whiteWon == agent1White) ? 1 : -1;
          } else {
            // Stalemate
            return 0;
          }
        }

        // Check for insufficient material
        if (GameService.isDeadPosition(board.whitePieces, board.blackPieces)) {
          return 0; // Draw
        }

        // Get opponent's moves for evaluation
        ArrayList<Move> opponentMoves = GameService.generateMoves(board, !isWhiteTurn);

        // Make move
        Move move = currentAgent.getMove(board, moves, opponentMoves);
        if (move == null) {
          return 0; // Error, count as draw
        }

        GameService.makeMove(board, move, isWhiteTurn);
        GameService.updateBoardMeta(board);

        moveCount++;
      }
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return 0; // Error, count as draw
    }

    // Max moves reached - evaluate position
    return evaluateFinalPosition(board, agent1White);
  }

  /**
   * Evaluates the final position if max moves reached.
   */
  private int evaluateFinalPosition(Board board, boolean agent1White) {
    double materialBalance = 0;

    for (Piece piece : board.whitePieces) {
      materialBalance += getPieceValue(piece.getName());
    }
    for (Piece piece : board.blackPieces) {
      materialBalance -= getPieceValue(piece.getName());
    }

    // Determine winner based on material
    if (Math.abs(materialBalance) < 3) {
      return 0; // Draw if close
    }

    boolean whiteWinning = materialBalance > 0;
    return (whiteWinning == agent1White) ? 1 : -1;
  }

  /**
   * Gets the material value of a piece.
   */
  private int getPieceValue(String pieceName) {
    switch (pieceName) {
      case "Pawn": return 1;
      case "Knight": return 3;
      case "Bishop": return 3;
      case "Rook": return 5;
      case "Queen": return 9;
      default: return 0;
    }
  }

  /**
   * Exports tournament results to share.
   */
  private void exportResults() {
    String results = resultsText.getText().toString();

    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Chess Tournament Results");
    shareIntent.putExtra(Intent.EXTRA_TEXT, results);

    startActivity(Intent.createChooser(shareIntent, "Share Results"));
  }
}