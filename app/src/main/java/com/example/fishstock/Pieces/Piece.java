package com.example.fishstock.Pieces;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.Move;

import java.util.*;


public interface Piece {
  public Coordinate getPos();
  public String getName();
  public ArrayList<Move> generateMoves(Coordinate coord2, Cell[][] board);
  public boolean getColor();
  public void setPos(Coordinate coord);
  public ArrayList<Piece> getProtectors();
  public ArrayList<Piece> getAttackers();
  public void addAttacker(Piece p);
  public void addProtector(Piece p);
  public void setRevealChecker(ArrayList<Coordinate> revealAve,Coordinate checkerLoc);
  public void setPin(ArrayList<Coordinate> pinAve, Coordinate pinnerLoc);
  public Coordinate getPinnerLoc();
  public void unPin();
  public void unReveal();
  public boolean getPin();
  public ArrayList<Coordinate> getPinAvenue();
  public ArrayList<Coordinate> getRevealAve();
  public ArrayList<Coordinate> generateAvenue(Coordinate c1, Coordinate c2);
  public Coordinate getCheckerLoc();
  public Coordinate getRevealCheckerLoc();
  public ArrayList<Move> getPossibleMoves();
  public void setPossibleMoves(ArrayList<Move> potentialMoves_2);

  public void reset();

  public Piece copyPiece();
  public char getSymbol();
}
