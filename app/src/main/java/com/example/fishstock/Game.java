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
  ArrayList<Piece> pinPiecesWhite = new ArrayList<>();
  ArrayList<Piece> pinPiecesBlack = new ArrayList<>();
  ArrayList<Piece> revealPiecesWhite = new ArrayList<>();
  ArrayList<Piece> revealPiecesBlack = new ArrayList<>();

  public Game (Board ChessBoard, AgentType whitePlayer, AgentType blackPlayer) {
    this.ChessBoard = ChessBoard;
    this.moveNum = 1;
    this.isWhite = true;
    this.isGameOver = false;
    this.whitePlayer = whitePlayer;
    this.blackPlayer = blackPlayer;
  }

}