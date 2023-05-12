package com.example.fishstock;

import com.example.fishstock.Agents.AgentType;
import com.example.fishstock.Pieces.Bishop;
import com.example.fishstock.Pieces.King;
import com.example.fishstock.Pieces.Knight;
import com.example.fishstock.Pieces.Pawn;
import com.example.fishstock.Pieces.Piece;
import com.example.fishstock.Pieces.Queen;
import com.example.fishstock.Pieces.Rook;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestEvaluate {
    //WHITE PIECES
    List<Piece> whitePieces = new ArrayList<>();
    King whiteKing;

    Queen whiteQueen;

    Rook whiteRook1;
    Rook whiteRook2;

    Bishop whiteBishop1;
    Knight whiteKnight1;
    Pawn whitePawn1;
    Pawn whitePawn2;
    Pawn whitePawn3;


    //BLACK PIECES
    List<Piece> blackPieces = new ArrayList<>();
    King blackKing;

    Queen blackQueen;

    Rook blackRook1;
    Rook blackRook2;

    Bishop blackBishop1;

    Knight blackKnight1;

    Pawn blackPawn1;
    Pawn blackPawn2;
    Pawn blackPawn3;

    Board board = new Board();
    Game game = new Game(board, AgentType.RANDY, AgentType.RANDY);


    public void init(){
      //WHITE PIECES
      whiteKing = new King(new Coordinate(3,0), true);
      whitePawn1 = new Pawn(new Coordinate(3,3), true);
      whiteKnight1 = new Knight(new Coordinate(4, 1), true);
      whiteRook1 = new Rook(new Coordinate(3, 7), true);
      whiteBishop1 = new Bishop (new Coordinate(7, 6), true);
      whitePawn2 = new Pawn(new Coordinate(1,1), true);
      whiteQueen = new Queen (new Coordinate(7, 1), true);
      whitePawn3 = new Pawn (new Coordinate( 6, 1), true);
      whitePieces.add(whiteKing);
      whitePieces.add(whitePawn1);
      whitePieces.add(whiteKnight1);
      whitePieces.add(whiteRook1);
      whitePieces.add(whiteBishop1);
      whitePieces.add(whitePawn2);
      whitePieces.add(whiteQueen);
      whitePieces.add(whitePawn3);

      //BLACK PIECES
      blackKing = new King(new Coordinate(3,4), false);
      blackKnight1 = new Knight(new Coordinate(4, 5), false);
      blackBishop1 = new Bishop (new Coordinate(3, 6), false);
      blackPawn1 = new Pawn (new Coordinate(2, 4), false);
      blackPawn1.growUp();
      ArrayList<Coordinate> pinAve = GameService.getCheckingAvenue(whiteRook1, whiteRook1.getPos(), blackKing.getPos());
      blackBishop1.setPin(pinAve, whiteRook1.getPos());
      blackPieces.add(blackKing);
      blackPieces.add(blackBishop1);
      blackPieces.add(blackKnight1);
      blackPieces.add(blackPawn1);
      board = new Board(whitePieces, blackPieces);
      game = new Game(board, AgentType.RANDY, AgentType.RANDY);
    }
  @Test
  public void testPawnEval() {
      init();
      Board.printBoard(board, true);
  }
}
