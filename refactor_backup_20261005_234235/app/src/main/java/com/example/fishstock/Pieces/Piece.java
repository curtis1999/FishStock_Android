package com.example.fishstock.Pieces;
import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.Move;

import java.io.Serializable;
import java.util.*;


public interface Piece extends Serializable{
  public Coordinate getPos();
  public Coordinate getFromPos();
  public String getName();
  public ArrayList<Move> generateMoves(Coordinate coord2, Cell[][] board);
  public boolean getColor();
  public void setPos(Coordinate coord);
  public ArrayList<Piece> getProtectors();
  public ArrayList<Piece> getAttackers();
  public void addAttacker(Piece p);
  public void addProtector(Piece p);
  public void setRevealChecker(ArrayList<Coordinate> revealAve,Coordinate checkerLoc);
  public void setReveal();
  public void setRevealQueen();
  public void setPin(ArrayList<Coordinate> pinAve, Coordinate pinnerLoc);
  public void setQueenPin();
  public Coordinate getPinnerLoc();
  public boolean isPinned();
  public boolean isPinnedToQueen();
  public ArrayList<Coordinate> generateAvenue(Coordinate c1, Coordinate c2);
  public Coordinate getCheckerLoc();
  public Coordinate getRevealCheckerLoc();
  public ArrayList<Move> getPossibleMoves();
  public void setPossibleMoves(ArrayList<Move> potentialMoves_2);

  void setRevealChecker();

  public boolean isRevealChecker();

  void setRevealQueenChecker();

  public boolean isRevealQueenChecker();
  public void reset();
  public Piece copyPiece();
  public char getSymbol();
  public void addCriticalAttack(Piece piece);
  public void addCriticalDefenence(Piece piece);
  public void clearCriticalLabels();
  public void addOverloadValue(int value);
  public void addForkValue(int value);
  double evaluate(Board board);
  double evaluateSimple(Board board);
  int getValue();
}
