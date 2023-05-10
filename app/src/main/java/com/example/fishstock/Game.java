package com.example.fishstock;

import com.example.fishstock.Agents.AgentType;
import com.example.fishstock.Pieces.*;
import java.util.*;
public class Game {
  Board ChessBoard = new Board();
  int moveNum;
  boolean isWhite;
  boolean isGameOver=false;
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
  }
  public void addWhiteMove(Move move) {
    this.whitesMovesLog.add(move);
  }
  public void addBlackMove(Move move) {
    this.blacksMovesLog.add(move);
  }
  public void addBoardState(Board board) {
    this.boardStates.add(board);
  }

  public Board getPreviousBoard() {
    if(boardStates.size()>1) {
      return boardStates.get(boardStates.size()-3);
    } else {
      return boardStates.get(0);
    }
  }
}