package com.example.fishstock;

import com.example.fishstock.Agents.AgentType;
import com.example.fishstock.Pieces.*;

import java.io.Serializable;
import java.util.*;
public class Game implements Serializable {
  Board ChessBoard = new Board();
  int moveNum;
  boolean isWhite;
  boolean isGameOver=false;
  boolean isEndGame;
  AgentType whitePlayer;
  AgentType blackPlayer;
  ArrayList<Move> whitesMovesLog = new ArrayList<>();
  ArrayList<Move> blacksMovesLog = new ArrayList<>();
  ArrayList<Board> boardStates = new ArrayList<>();

  public Game (Board ChessBoard, AgentType whitePlayer, AgentType blackPlayer) {
    this.ChessBoard = ChessBoard;
    this.moveNum = 1;
    this.isWhite = true;
    this.isGameOver = false;
    this.whitePlayer = whitePlayer;
    this.blackPlayer = blackPlayer;
    this.isEndGame = false;
  }

  public Board getPreviousBoard() {
    if(boardStates.size()>2) {
      Board prevBoardState = boardStates.get(boardStates.size()-3);
      boardStates.remove(boardStates.size()-1);
      boardStates.remove(boardStates.size()-2);
      whitesMovesLog.remove(whitesMovesLog.size()-1);
      blacksMovesLog.remove(blacksMovesLog.size()-1);
      return prevBoardState;
    } else {
      Board prevBoardState = boardStates.get(0);
      if (boardStates.size() == 2) {
        boardStates.remove(1);
        whitesMovesLog.remove(whitesMovesLog.size()-1);
      }
      return prevBoardState;
    }
  }
}
/*
TODOS 1: Add test to promotion checkmate (Not entering the Game Over Dialogue)
2: Factor draw potential into the pieces evaluation.  (AGENT WILL GET INTO A LOOP LEADING TO DRAW BY REPETITION WHEN IT IS WINNING)
3: The winning/losing message is inverted when you are playing as black
 */