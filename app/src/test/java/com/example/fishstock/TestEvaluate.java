package com.example.fishstock;

import static org.junit.Assert.assertEquals;

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
    Pawn whitePawn4;

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
    Pawn blackPawn4;

    Board board = new Board();
    Game game = new Game(board, AgentType.RANDY, AgentType.RANDY);


    public void initPawns(){
      //WHITE PIECES
      whiteKing = new King(new Coordinate(3,0), true);
      whitePawn1 = new Pawn(new Coordinate(2,1), true);
      whitePawn2 = new Pawn (new Coordinate( 3, 4), true); whitePawn2.growUp();
      whitePawn3 = new Pawn (new Coordinate( 4, 3), true); whitePawn3.growUp();
      whitePawn4 = new Pawn(new Coordinate(6,1), true);
      whitePieces.add(whiteKing); whitePieces.add(whitePawn1); whitePieces.add(whitePawn2); whitePieces.add(whitePawn3); whitePieces.add(whitePawn4);

      //BLACK PIECES
      blackKing = new King(new Coordinate(3,7), false);
      blackPawn1 = new Pawn (new Coordinate(0, 6), false); blackPawn1.growUp();
      blackPawn2 = new Pawn (new Coordinate(1, 6), false); blackPawn2.growUp();
      blackPawn3 = new Pawn (new Coordinate(2, 6), false); blackPawn3.growUp();
      blackPawn4 = new Pawn (new Coordinate(3, 7), false);
      blackPieces.add(blackKing); blackPieces.add(blackPawn1); blackPieces.add(blackPawn2); blackPieces.add(blackPawn3); blackPieces.add(blackPawn4);
      board = new Board(whitePieces, blackPieces);
      game = new Game(board, AgentType.RANDY, AgentType.RANDY);
    }
  @Test
  public void testPawnEval() {
      initPawns();
      GameService.updateBoardMeta(board);
      Board.printBoard(board, true);
      double eval1 = whitePawn1.evaluate(board);
      double eval2 = whitePawn2.evaluate(board);
      double eval3 = whitePawn3.evaluate(board);
      double eval4 = whitePawn4.evaluate(board);
      assertEquals(true, (eval1 < eval2 && eval2 > eval3));
      assertEquals(true, (eval4 > eval1));
      Pawn doublePawn = new Pawn(new Coordinate(3, 1), true);
      board.board[1][3].putPiece(doublePawn);
      GameService.updateBoardMeta(board);
      Board.printBoard(board, true);
      double updatedEval2 = whitePawn2.evaluate(board);
      assertEquals(true, (updatedEval2 < eval2));
    }
}
