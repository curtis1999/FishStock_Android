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
    Bishop whiteBishop2;
    Knight whiteKnight1;
    Knight whiteKnight2;
    Pawn whitePawn1;
    Pawn whitePawn2;
    Pawn whitePawn3;
    Pawn whitePawn4;
    Pawn whitePawn5;
    //BLACK PIECES
    List<Piece> blackPieces = new ArrayList<>();
    King blackKing;
    Queen blackQueen;
    Rook blackRook1;
    Rook blackRook2;
    Bishop blackBishop1;
    Bishop blackBishop2;
    Knight blackKnight1;
    Knight blackKnight2;
    Pawn blackPawn1;
    Pawn blackPawn2;
    Pawn blackPawn3;
    Pawn blackPawn4;
    Board board;
    Game game;

  public void initAll(){
    //WHITE PIECES
    whiteKing = new King(new Coordinate(1,0), true);
    whitePawn1 = new Pawn(new Coordinate(0,1), true);
    whitePawn2 = new Pawn (new Coordinate( 1, 2), true); whitePawn2.growUp();
    whitePawn3 = new Pawn (new Coordinate( 2, 1), true);
    whitePawn4 = new Pawn(new Coordinate(6,1), true);
    whiteKnight1 = new Knight(new Coordinate(3, 2), true);
    whiteKnight2 = new Knight(new Coordinate(7, 7), true);
    whiteRook1 = new Rook(new Coordinate(3, 0), true);
    whiteRook2 = new Rook(new Coordinate(7, 0), true);
    whiteBishop1 = new Bishop (new Coordinate(1, 1), true);
    whiteBishop2 = new Bishop (new Coordinate(5, 0), true);
    whitePieces.add(whiteKing);
    whitePieces.add(whitePawn1); whitePieces.add(whitePawn2); whitePieces.add(whitePawn3); whitePieces.add(whitePawn4);
    whitePieces.add(whiteKnight1); whitePieces.add(whiteKnight2);
    whitePieces.add(whiteRook1); whitePieces.add(whiteRook2);
    whitePieces.add(whiteBishop1); whitePieces.add(whiteBishop2);

    //BLACK PIECES
    blackKing = new King(new Coordinate(3,7), false);
    blackKnight1 = new Knight(new Coordinate(0, 7), false);
    blackKnight2 = new Knight (new Coordinate(3, 4), false);
    blackBishop1 = new Bishop (new Coordinate(1, 6), false);
    blackBishop2 = new Bishop (new Coordinate(4, 2), false);
    blackRook1 = new Rook (new Coordinate(0, 3), false);
    blackRook2 = new Rook (new Coordinate(5, 1), false);
    blackPawn1 = new Pawn (new Coordinate(2, 5), false); blackPawn1.growUp();
    blackPawn2 = new Pawn (new Coordinate(4, 5), false);blackPawn2.growUp();
    blackPieces.add(blackKing);
    blackPieces.add(blackBishop1); blackPieces.add(blackBishop2);
    blackPieces.add(blackKnight1);blackPieces.add(blackKnight2);
    blackPieces.add(blackRook1); blackPieces.add(blackRook2);
    blackPieces.add(blackPawn1); blackPieces.add(blackPawn2);

    board = new Board(whitePieces, blackPieces);
    game = new Game(board, AgentType.RANDY, AgentType.RANDY);
  }
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

    public void initReveal() {
      whiteKing = new King(new Coordinate(3,0), true);
      whiteQueen = new Queen(new Coordinate(7, 5), true);
      whiteRook1 = new Rook(new Coordinate(3,1), true);
      whiteBishop1 = new Bishop (new Coordinate(7,1), true);
      whitePawn1 = new Pawn(new Coordinate(3,2), true);
      whitePawn2 = new Pawn(new Coordinate(6, 2), true);
      whitePawn3 = new Pawn (new Coordinate(6, 5), true);
      whitePieces.add(whiteKing); whitePieces.add(whiteRook1); whitePieces.add(whiteBishop1); whitePieces.add(whiteQueen);
      whitePieces.add(whitePawn1); whitePieces.add(whitePawn2); whitePieces.add(whitePawn3);

      blackKing = new King(new Coordinate(3,7), false);
      blackQueen = new Queen (new Coordinate (3, 5), false);
      blackPieces.add(blackKing); blackPieces.add(blackQueen);
      board = new Board(whitePieces, blackPieces);
      game = new Game(board, AgentType.RANDY, AgentType.RANDY);
    }

  public void initKingSafety() {
    whiteKing = new King(new Coordinate(1,0), true);
    whiteQueen = new Queen(new Coordinate(7, 5), true);
    whiteRook1 = new Rook(new Coordinate(3,1), true);
    whiteBishop1 = new Bishop (new Coordinate(7,1), true);
    whitePawn1 = new Pawn(new Coordinate(0,1), true);
    whitePawn2 = new Pawn(new Coordinate(1, 2), true); whitePawn2.growUp();
    whitePawn3 = new Pawn (new Coordinate(2, 1), true);
    whitePieces.add(whiteKing); whitePieces.add(whiteRook1); whitePieces.add(whiteBishop1); whitePieces.add(whiteQueen);
    whitePieces.add(whitePawn1); whitePieces.add(whitePawn2); whitePieces.add(whitePawn3);

    blackKing = new King(new Coordinate(3,7), false);
    blackQueen = new Queen (new Coordinate (3, 5), false);
    blackPieces.add(blackKing); blackPieces.add(blackQueen);
    board = new Board(whitePieces, blackPieces);
    game = new Game(board, AgentType.RANDY, AgentType.RANDY);
  }

  @Test
  public void testKingSafety() {
    initKingSafety();
    Board.printBoard(board, true);
    GameService.updateBoardMeta(board);
    double whiteKingSafety = whiteKing.evaluateSafety(board);
    double blackKingSafety = blackKing.evaluateSafety(board);
    assertEquals(true, whiteKingSafety > blackKingSafety);
    assertEquals(true, blackKing.xRayingPieces.size() == 1);
  }


    @Test
    public void testRevealQueen() {
      initReveal();
      GameService.updateBoardMeta(board);
      Board.printBoard(board, true);
      assertEquals(true, whitePawn1.isRevealQueenChecker());
      assertEquals(true, whitePawn2.isRevealQueenChecker());
      assertEquals(true, whitePawn3.isRevealQueenChecker());
      //Swap the white pawns for a black ones to test Pin.
      blackPawn1 = new Pawn(new Coordinate (3, 2), false);
      blackPawn2 = new Pawn(new Coordinate(6, 2), false);
      blackPawn3 = new Pawn(new Coordinate(6, 5), false);
      board.whitePieces.remove(whitePawn1);
      board.whitePieces.remove(whitePawn2);
      board.whitePieces.remove(whitePawn3);
      board.blackPieces.add(blackPawn1);
      board.blackPieces.add(blackPawn2);
      board.blackPieces.add(blackPawn3);
      board.board[2][3].empty();
      board.board[2][3].putPiece(blackPawn1);
      board.board[2][6].empty();
      board.board[2][6].putPiece(blackPawn2);
      board.board[5][6].empty();
      board.board[5][6].putPiece(blackPawn3);
      GameService.updateBoardMeta(board);
      Board.printBoard(board, true);
      assertEquals(true, blackPawn1.isPinnedToQueen());
      assertEquals(true, blackPawn2.isPinnedToQueen());
      assertEquals(true, blackPawn3.isPinnedToQueen());
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
      board.whitePieces.add(doublePawn);
      board.board[1][3].putPiece(doublePawn);
      GameService.updateBoardMeta(board);
      Board.printBoard(board, true);
      double updatedEval2 = whitePawn2.evaluate(board);
      assertEquals(true, (updatedEval2 < eval2));
    }

    @Test
  public void allPieces() {
    initAll();
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true);
    double whiteBishop1Eval = whiteBishop1.evaluate(board);
    double whiteBishop2Eval = whiteBishop2.evaluate(board);
    assertEquals(true, whiteBishop1Eval > whiteBishop2Eval);
    double whiteRook1Eval = whiteRook1.evaluate(board);
    double whiteRook2Eval = whiteRook2.evaluate(board);
    assertEquals(true, whiteRook2Eval > whiteRook1Eval);

    double whiteKnight1Eval = whiteKnight1.evaluate(board);
    double whiteKnight2Eval = whiteKnight2.evaluate(board);
    assertEquals(true, whiteKnight1Eval > whiteKnight2Eval);

    double blackRook1Eval = blackRook1.evaluate(board);
    double blackRook2Eval = blackRook2.evaluate(board);
    assertEquals(true, blackRook2Eval > blackRook1Eval);
  }

  @Test
  public void testSimple() {

  }
}
