package com.example.fishstock;

import static java.security.AccessController.getContext;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.Agents.Agent;
import com.example.fishstock.Board;
import com.example.fishstock.GameManager;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Pieces.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentActivity extends AppCompatActivity {

  private static final int GAMES_PER_MATCHUP = 1; // Number of games per match

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_tournament);

    Button startButton = findViewById(R.id.startTournament);
    TextView resultsText = findViewById(R.id.results);
    Button back = findViewById(R.id.back);

    startButton.setOnClickListener(v -> {
      runTournament(resultsText);
    });
    back.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v){
        Intent intent = new Intent(TournamentActivity.this,MainActivity.class);
        startActivity(intent);
      }
    });
  }

  private void runTournament(TextView resultsText) {
    String[] agents = {"Randy", "Simple"};
    Map<String, Integer> wins = new HashMap<>();
    Map<String, Integer> losses = new HashMap<>();
    Map<String, Integer> draws = new HashMap<>();

    // Initialize counters
    for (String agent : agents) {
      wins.put(agent, 0);
      losses.put(agent, 0);
      draws.put(agent, 0);
    }

    // Play all matchups
    for (int i = 0; i < agents.length; i++) {
      for (int j = i + 1; j < agents.length; j++) {
        String agent1 = agents[i];
        String agent2 = agents[j];

        // Play GAMES_PER_MATCHUP games
        for (int game = 0; game < GAMES_PER_MATCHUP; game++) {
          boolean agent1White = (game % 2 == 0); // Alternate colors
          int result = playGame(agent1, agent2, agent1White);

          if (result == 1) { // Agent1 wins
            wins.put(agent1, wins.get(agent1) + 1);
            losses.put(agent2, losses.get(agent2) + 1);
          } else if (result == -1) { // Agent2 wins
            wins.put(agent2, wins.get(agent2) + 1);
            losses.put(agent1, losses.get(agent1) + 1);
          } else { // Draw
            draws.put(agent1, draws.get(agent1) + 1);
            draws.put(agent2, draws.get(agent2) + 1);
          }

          // Update UI
          updateResults(resultsText, agent1, agent2, wins, losses, draws);
        }
      }
    }

    // Calculate and display ELO ratings
    Map<String, Integer> eloRatings = calculateELO(agents, wins, losses, draws);
    displayFinalResults(resultsText, eloRatings, wins, losses, draws);
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
            // Checkmate
            return isWhiteTurn ? -1 : 1; // Other player wins
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
   * Calculates ELO ratings using simple performance rating.
   */
  private Map<String, Integer> calculateELO(String[] agents,
                                            Map<String, Integer> wins,
                                            Map<String, Integer> losses,
                                            Map<String, Integer> draws) {
    Map<String, Integer> elo = new HashMap<>();
    int baseELO = 1500; // Starting ELO

    for (String agent : agents) {
      int totalGames = wins.get(agent) + losses.get(agent) + draws.get(agent);
      double score = wins.get(agent) + 0.5 * draws.get(agent);
      double winRate = score / totalGames;

      // Simple ELO calculation based on win rate
      int eloRating = baseELO + (int)((winRate - 0.5) * 400);
      elo.put(agent, eloRating);
    }

    return elo;
  }

  private void updateResults(TextView resultsText, String agent1, String agent2,
                             Map<String, Integer> wins, Map<String, Integer> losses,
                             Map<String, Integer> draws) {
    runOnUiThread(() -> {
      StringBuilder sb = new StringBuilder();
      sb.append("Current Results:\n\n");
      sb.append(agent1).append(" vs ").append(agent2).append("\n");
      sb.append(agent1).append(": W:").append(wins.get(agent1))
          .append(" L:").append(losses.get(agent1))
          .append(" D:").append(draws.get(agent1)).append("\n");
      resultsText.setText(sb.toString());
    });
  }

  private void displayFinalResults(TextView resultsText, Map<String, Integer> elo,
                                   Map<String, Integer> wins, Map<String, Integer> losses,
                                   Map<String, Integer> draws) {
    StringBuilder sb = new StringBuilder();
    sb.append("TOURNAMENT RESULTS\n");
    sb.append("==================\n\n");

    // Sort by ELO
    List<Map.Entry<String, Integer>> sortedElo = new ArrayList<>(elo.entrySet());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      sortedElo.sort((a, b) -> b.getValue().compareTo(a.getValue()));
    }

    for (Map.Entry<String, Integer> entry : sortedElo) {
      String agent = entry.getKey();
      sb.append(agent).append(": ").append(entry.getValue()).append(" ELO\n");
      sb.append("  Wins: ").append(wins.get(agent))
          .append(" | Losses: ").append(losses.get(agent))
          .append(" | Draws: ").append(draws.get(agent)).append("\n\n");
    }

    resultsText.setText(sb.toString());
  }
}