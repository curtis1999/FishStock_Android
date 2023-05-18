package com.example.fishstock;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import com.example.fishstock.Agents.AgentType;
import com.example.fishstock.Pieces.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class TestGameService {
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

  public void init2() {
    whiteKing = new King(new Coordinate(5, 1), true);
    whiteRook1 = new Rook(new Coordinate(6, 0), true);
    whitePawn1 = new Pawn(new Coordinate(1, 1), true);
    whitePawn2 = new Pawn(new Coordinate(6, 1), true);
    whitePawn3 = new Pawn(new Coordinate(6, 4), true);
    whitePawn3.growUp();
    whitePieces.add(whiteKing);
    whitePieces.add(whiteRook1);
    whitePieces.add(whitePawn1);
    whitePieces.add(whitePawn2);
    whitePieces.add(whitePawn3);

    blackKing = new King(new Coordinate(6, 5), false);
    blackRook1 = new Rook(new Coordinate(6, 7), false);
    blackPawn1 = new Pawn(new Coordinate(2, 3), false);
    blackPawn1.growUp();
    blackPawn2 = new Pawn(new Coordinate(7, 3), false);
    blackPawn2.growUp();
    blackPawn3 = new Pawn(new Coordinate(7, 6), false);
    blackPieces.add(blackKing);
    blackPieces.add(blackRook1);
    blackPieces.add(blackPawn1);
    blackPieces.add(blackPawn2);
    blackPieces.add(blackPawn3);
    board = new Board(whitePieces, blackPieces);
    game = new Game(board, AgentType.RANDY, AgentType.RANDY);
  }
  public void initCheck() {
    whiteKing = new King(new Coordinate(0,0), true);
    whitePawn1 = new Pawn(new Coordinate(0,1), true);
    whiteKnight1 = new Knight(new Coordinate(1, 1), true);
    whiteBishop1 = new Bishop(new Coordinate(4, 3), true);
    whitePieces.add(whiteKing);
    whitePieces.add(whitePawn1);
    whitePieces.add(whiteKnight1);
    whitePieces.add(whiteBishop1);

    //BLACK PIECES
    blackKing = new King(new Coordinate(3,7), false);
    blackRook1 = new Rook (new Coordinate(2, 1), false);
    blackBishop1 = new Bishop (new Coordinate(4, 4), false);
    blackPieces.add(blackKing);
    blackPieces.add(blackBishop1);
    blackPieces.add(blackRook1);
    board = new Board(whitePieces, blackPieces);
    game = new Game(board, AgentType.RANDY, AgentType.RANDY);
  }

  public void initOpposition() {
    whiteKing = new King(new Coordinate(3, 3), true);
    whiteRook1 = new Rook(new Coordinate(7, 5), true);
    whiteBishop1 = new Bishop(new Coordinate(6, 3), true);
    whitePieces.add(whiteKing); whitePieces.add(whiteRook1); whitePieces.add(whiteBishop1);
    blackKing = new King(new Coordinate(3, 5), false);
    blackRook1 = new Rook(new Coordinate(7, 3), false);
    blackBishop1 = new Bishop(new Coordinate(6, 5), false);
    blackPieces.add(blackKing); blackPieces.add(blackRook1); blackPieces.add(blackBishop1);
    board = new Board(whitePieces, blackPieces);
    game = new Game(board, AgentType.RANDY, AgentType.RANDY);
  }
  public void initCastle() {
    GameService.clearBoard(board);
    whiteKing = new King(new Coordinate(3, 0), true);
    whiteQueen = new Queen(new Coordinate(4, 0), true);
    whiteRook1 = new Rook(new Coordinate(0, 0), true);
    whiteRook2 = new Rook(new Coordinate (7, 0), true);
    whiteBishop1 = new Bishop(new Coordinate(2, 4), true);
    whitePieces.add(whiteKing); whitePieces.add(whiteQueen); whitePieces.add(whiteRook1); whitePieces.add(whiteRook2); whitePieces.add(whiteBishop1);

    blackKing = new King(new Coordinate(3, 7), false);
    blackQueen = new Queen (new Coordinate(4, 7), false);
    blackRook1 = new Rook (new Coordinate(0, 7), false);
    blackRook2 = new Rook (new Coordinate(7, 7), false);
    blackBishop1 = new Bishop(new Coordinate(2, 3), false);
    blackPieces.add(blackKing);blackPieces.add(blackQueen);blackPieces.add(blackRook1);blackPieces.add(blackRook2);blackPieces.add(blackBishop1);
    board = new Board(whitePieces, blackPieces);
    game = new Game(board, AgentType.RANDY, AgentType.RANDY);
  }
  public void initCastle2() {
    GameService.clearBoard(board);
    whiteKing = new King(new Coordinate(3, 0), true);
    whiteRook1 = new Rook(new Coordinate(0, 0), true);
    whitePieces.add(whiteKing); whitePieces.add(whiteRook1);
    blackKing = new King(new Coordinate(3, 7), false);
    blackRook1 = new Rook (new Coordinate(7, 7), false);
    blackKnight1 = new Knight(new Coordinate(6, 7), false);
    blackPieces.add(blackKing);blackPieces.add(blackRook1);blackPieces.add(blackKnight1);
    board = new Board(whitePieces, blackPieces);
    game = new Game(board, AgentType.RANDY, AgentType.RANDY);
  }

  public void initPromotion() {
    whiteKing = new King(new Coordinate(3, 0), true);
    whitePawn1 = new Pawn(new Coordinate(0, 6), true);
    whitePawn1.growUp();
    whitePieces.add(whiteKing); whitePieces.add(whitePawn1);

    blackKing = new King(new Coordinate(5, 7), false);
    blackPawn1 = new Pawn(new Coordinate(1, 1), false);
    blackPawn1.growUp();
    blackRook1 = new Rook (new Coordinate(4, 7), false);
    blackPieces.add(blackKing);blackPieces.add(blackPawn1);blackPieces.add(blackRook1);
    board = new Board(whitePieces, blackPieces);
    game = new Game(board, AgentType.RANDY, AgentType.RANDY);
  }
  @Test
  public void testCastle3(){
    initCastle2();
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true);
    List<Move> kingMoves = board.blackPieces.get(0).generateMoves(board.blackPieces.get(0).getPos(), board.board);
    assertEquals(5, kingMoves.size());

    Move rookMove = new Move(whiteRook1.getPos(), new Coordinate(0,7), "Rook", false, true);
    GameService.makeMove(board, rookMove, true);
    GameService.updateBoardMeta(board);
    ArrayList<Move> rawKingMoves = board.blackPieces.get(0).generateMoves(board.blackPieces.get(0).getPos(), board.board);
    kingMoves = GameService.generateMovesDoubleCheck(board, rawKingMoves, false);
    Board.printBoard(board, true);
    assertEquals(3, kingMoves.size());
  }


  @Test
  public void testChecks() throws CloneNotSupportedException {
    initCheck();
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true);
    assertEquals(false, whiteKing.isChecked);
    assertEquals(true, whiteKnight1.isPinned());
    //Test 1: Giving a check with ROok
    Move move1 = new Move(blackRook1.getPos(), new Coordinate(2,0), "Rook", false, false);
    GameService.makeMove(board, move1, false);
    GameService.updateBoardMeta(board);
    assertEquals(true, whiteKing.isChecked);
    assertEquals(false, whiteKing.isDoubleChecked);
    List<Move> whitesMoves = GameService.generateMoves(board, true);
    whitesMoves = GameService.generateMovesCheck(board, (ArrayList<Move>) whitesMoves, true);
    assertEquals(1, whitesMoves.size());

  }
  @Test
  public void testPromotion() throws CloneNotSupportedException {
    initPromotion();
    GameService.updateBoardMeta(board);
    List<Move> whitesMoves = GameService.generateMoves(board, true);
    List<Move> blacksMoves = GameService.generateMoves(board, false);
    assertEquals(6, whitesMoves.size());
    assertEquals(19, blacksMoves.size());

    //TEST 1: White Promotion.
    Move whitePromotion = new Move(whitePawn1.getPos(), new Coordinate(0,7), "Pawn", false, true);
    whitePromotion.setPromotion(new Queen(whitePromotion.toCoord, true));
    GameService.makeMove(board, whitePromotion, true);
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true);
    whitesMoves = GameService.generateMoves(board, true);
    assertEquals(20, whitesMoves.size());

    //Test 2: Capturing the Promoted Pawn.
  }

  @Test
  public void testGenerateMoves() throws CloneNotSupportedException {
    init();
    GameService.updateBoardMeta(board);
    ArrayList<Move> blackMoves = GameService.generateMoves(board, false);
    assertEquals(11, blackMoves.size());
    ArrayList<Move> whitesMoves = GameService.generateMoves(board, true);
    assertEquals(41, whitesMoves.size());
  }
  @Test
  public void testUpdateBoardMeta() {
    init();
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true);
    assertEquals(2, board.board[4][4].whiteAttackers.size());
    assertEquals(3, board.board[3][3].blackAttackers.size());
    assertEquals(1, board.board[4][2].whiteAttackers.size());
    assertEquals(0, board.board[2][4].whiteAttackers.size());
    assertEquals(0, board.board[2][7].blackAttackers.size());
    assertEquals(1, board.board[6][3].piece.getAttackers().size());
  }

  @Test
  public void testOpposition() throws CloneNotSupportedException {
    initOpposition();
    Board.printBoard(board, true); System.out.println("|||||||||||||||||");
    GameService.updateBoardMeta(board);
    List<Move> whitesKingMoves = whiteKing.generateMoves(whiteKing.getPos(), board.board);
    List<Move> blacksKingMoves = blackKing.generateMoves(blackKing.getPos(), board.board);
    assertEquals(8, whitesKingMoves.size());
    assertEquals(8, blacksKingMoves.size());
    whitesKingMoves = GameService.filterMoves((ArrayList<Move>) whitesKingMoves);
    blacksKingMoves = GameService.filterMoves((ArrayList<Move>) blacksKingMoves);
    assertEquals(3, whitesKingMoves.size());
    assertEquals(3, blacksKingMoves.size());
  }

  @Test
  public void testCastling() {
    initCastle();
    Board.printBoard(board, true); System.out.println("|||||||||||||||||");
    GameService.updateBoardMeta(board);

    //Test 1: White's short castle.
    List<Move> whiteKingMoves = whiteKing.generateMoves(whiteKing.getPos(), board.board);
    assertEquals(6, whiteKingMoves.size());
    Move shortCastleMove = new Move(whiteKing.getPos(), new Coordinate(1, 0), "King", false, true);
    shortCastleMove.setCastle();
    GameService.makeMove(board, shortCastleMove, true);
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true);  System.out.println("|||||||||||||||||");

    //Test 2: Black's castle blocked.
    List<Move> blacksKingMoves = blackKing.generateMoves(blackKing.getPos(), board.board);
    assertEquals(6, blacksKingMoves.size());
    //Test moving the bishop into the checking avenue
    Move bishopMove = new Move(whiteBishop1.getPos(), new Coordinate(0,6), "Bishop", false, true);
    GameService.makeMove(board, bishopMove, true);
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true);  System.out.println("|||||||||||||||||");
    blacksKingMoves = blackKing.generateMoves(blackKing.getPos(), board.board);
    assertEquals(5, blacksKingMoves.size());

    //Test Long Castle for black.  First set up the long castle by clearing the path.
    Move queenCapture = new Move(blackQueen.getPos(), new Coordinate(4, 0), "Queen", true, false);
    queenCapture.setCapture(whiteQueen);
    GameService.makeMove(board, queenCapture, false);
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true);  System.out.println("|||||||||||||||||");
    blacksKingMoves = blackKing.generateMoves(blackKing.getPos(), board.board);
    assertEquals(6, blacksKingMoves.size());
    Move longCastle = new Move(blackKing.getPos(), new Coordinate(5, 7), "King", false, false);
    longCastle.setCastle();
    GameService.makeMove(board, longCastle, false);
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true);  System.out.println("|||||||||||||||||");

  }

  @Test
  public void testCastling2() {
    initCastle();
    Board.printBoard(board, true); System.out.println("|||||||||||||||||");
    GameService.updateBoardMeta(board);

    List<Move> whiteKingMoves = whiteKing.generateMoves(whiteKing.getPos(), board.board);
    assertEquals(6, whiteKingMoves.size());
    List<Move> blacksKingMoves = blackKing.generateMoves(blackKing.getPos(), board.board);
    assertEquals(6, blacksKingMoves.size());

    //Test 1: Moving the whiteRook to block blacks castling rights.
    Move rookMove = new Move(whiteRook1.getPos(), new Coordinate(1, 0), "Rook", false, true);
    GameService.makeMove(board, rookMove, true);
    GameService.updateBoardMeta(board);
    whiteKingMoves = whiteKing.generateMoves(whiteKing.getPos(), board.board);
    assertEquals(5, whiteKingMoves.size());
    blacksKingMoves = blackKing.generateMoves(blackKing.getPos(), board.board);
    assertEquals(5, blacksKingMoves.size());
    Board.printBoard(board, true); System.out.println("|||||||||||||||||");
    //Moving the Rook back.
    rookMove = new Move(whiteRook1.getPos(), new Coordinate(0, 0), "Rook", false, true);
    GameService.makeMove(board, rookMove, true);
    GameService.updateBoardMeta(board);
    whiteKingMoves = whiteKing.generateMoves(whiteKing.getPos(), board.board);
    assertEquals(5, whiteKingMoves.size());
    blacksKingMoves = blackKing.generateMoves(blackKing.getPos(), board.board);
    assertEquals(6, blacksKingMoves.size());
    Board.printBoard(board, true); System.out.println("|||||||||||||||||");
  }

  @Test
  public void testEnPassant() throws CloneNotSupportedException {
    init2();
    Board.printBoard(board, true);
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true); System.out.println("||||||||||||||||||||||");

    //TEST 1: Not en-passantable
    Move move1 = new Move(whitePawn1.getPos(), new Coordinate(1, 2), "Pawn", false, true);
    GameService.makeMove(board, move1, true);
    GameService.updateBoardMeta(board);
    assertEquals(false, ((Pawn)board.whitePieces.get(Board.getIndex(board.whitePieces, whitePawn1.getPos()))).enPassantable);
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||");

    move1 = new Move(whitePawn1.getPos(), new Coordinate(1, 3), "Pawn", false, true);
    GameService.makeMove(board, move1, true);
    GameService.updateBoardMeta(board);
    assertEquals(false, ((Pawn)board.whitePieces.get(Board.getIndex(board.whitePieces, whitePawn1.getPos()))).enPassantable);
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

    //TEST 2: En-passantable.
    Move move2 = new Move(whitePawn2.getPos(), new Coordinate(6, 3), "Pawn", false, true);
    GameService.makeMove(board, move2, true);
    GameService.updateBoardMeta(board);
    assertEquals(true, ((Pawn)board.whitePieces.get(Board.getIndex(board.whitePieces, whitePawn2.getPos()))).enPassantable);
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
    ArrayList<Move> blacksMoves = GameService.generateMoves(board, false);
    ArrayList<Move> pawnMoves = blackPawn2.generateMoves(blackPawn2.getPos(), board.board);
    move2 = new Move(blackPawn2.getPos(), new Coordinate(6, 2), "Pawn", true, false);
    move2.setCapture(whitePawn2);
    move2.setEnPassant();
    GameService.makeMove(board, move2, false);
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
    assertEquals(true, ((King) board.whitePieces.get(0)).isChecked);
    ArrayList<Move> whitesMoves = GameService.generateMoves(board, true);
    whitesMoves = GameService.generateMovesCheck(board, whitesMoves, true);
    assertEquals(8, whitesMoves.size());

    //TEST 3: Capturing the Rook to get out of check and setting up the en-passant reveal check
    Move move3 = new Move(whiteRook1.getPos(), new Coordinate(6, 2), "Rook", true, true);
    move3.setCapture(blackPawn2);
    GameService.makeMove(board, move3, true);
    GameService.updateBoardMeta(board);

    //TEST 4: Testing EnPassanting black
    Move move4 = new Move(blackPawn3.getPos(), new Coordinate(7, 4), "Pawn", false, false);
    GameService.makeMove(board, move4, false);
    GameService.updateBoardMeta(board);
    assertEquals(true, ((Pawn)board.blackPieces.get(Board.getIndex(board.blackPieces, blackPawn3.getPos()))).enPassantable);
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
    whitesMoves = GameService.generateMoves(board, true);
    pawnMoves = whitePawn3.generateMoves(whitePawn3.getPos(), board.board);
    move4 = new Move(whitePawn3.getPos(), new Coordinate(7, 5), "Pawn", true, true);
    move4.setCapture(blackPawn3);
    move4.setEnPassant();
    GameService.makeMove(board, move4, true);
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
    assertEquals(true, blackKing.isChecked);

    //TEST PROMOTING TO CHECKMATE
    Move move5 = new Move(blackKing.getPos(), new Coordinate(7, 4), "King", false, false);
    GameService.makeMove(board, move5, false);
    GameService.updateBoardMeta(board);
    move5 = new Move(whitePawn3.getPos(), new Coordinate(7, 6), "Pawn", false, true);
    GameService.makeMove(board, move5, true);
    GameService.updateBoardMeta(board);
    move5 = new Move(whitePawn3.getPos(), new Coordinate(7, 7), "Pawn", false, true);
    move5.setPromotion(new Queen(move5.toCoord, true));
    GameService.makeMove(board, move5, true);
    GameService.updateBoardMeta(board);
    blacksMoves = GameService.generateMoves(board, false);
    assertEquals(true, blackKing.isChecked);
    blacksMoves = GameService.generateMovesCheck(board, blacksMoves, false);
    assertEquals(1, blacksMoves.size());
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");


  }
@Test
  public void testMoves() throws CloneNotSupportedException {
    init();
    GameService.updateBoardMeta(board);
    //Test 1: Creating a pin.
    Move move1 = new Move(whiteBishop1.getPos(), new Coordinate (6, 7), "Bishop", false, true);
    GameService.makeMove(board, move1, true);
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
    assertEquals( false, blackKnight1.isPinned());
    GameService.updateBoardMeta(board);
    assertEquals(true, blackKnight1.isPinned());
    assertEquals(3, board.board[3][3].blackAttackers.size());
    ArrayList<Move> blacksMoves = GameService.generateMoves(board, false);
    assertEquals(5, blacksMoves.size());
    ArrayList<Move> whitesMoves = GameService.generateMoves(board, true);
    assertEquals(37, whitesMoves.size());

    //Test 2: Pawn move by 2.
    Move move2 = new Move(whitePawn2.getPos(), new Coordinate(1,3), "Pawn", false, true);
    GameService.makeMove(board, move2, true);
    GameService.updateBoardMeta(board);
    blacksMoves = GameService.generateMoves(board, false);
    assertEquals(6, blacksMoves.size());
    whitesMoves = GameService.generateMoves(board, true);
    assertEquals(37, whitesMoves.size());
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

    //TEST 3: Pawn capture.
    Move move3 = new Move(blackPawn1.getPos(), new Coordinate(3, 3), "Pawn", true, false);
    move3.setCapture(board.board[3][3].piece);
    GameService.makeMove(board, move3, false);
    GameService.updateBoardMeta(board);
    blacksMoves = GameService.generateMoves(board, false);
    assertEquals(4, blacksMoves.size());
    whitesMoves = GameService.generateMoves(board, true);
    assertEquals(36, whitesMoves.size());
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

    //TEST 4 STEPPING OUT OF A PIN
  Move move4 = new Move(whiteRook1.getPos(), new Coordinate(0, 7), "Rook", false, true);
  GameService.makeMove(board, move4, true);
  GameService.updateBoardMeta(board);
  blacksMoves = GameService.generateMoves(board, false);
  assertEquals(9, blacksMoves.size()); //TODO: IF A PINNING PIECE STEPS OUT OF THE PINNING AVE
  whitesMoves = GameService.generateMoves(board, true);
  assertEquals(42, whitesMoves.size());
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

  //TEST 5: SETTING UP A REVEAL.
  Move move5 = new Move(whiteQueen.getPos(), new Coordinate(7,0), "Queen", false, true);
  GameService.makeMove(board, move5, true);
  GameService.updateBoardMeta(board);
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

  //TEST 6: CAPTURING CHECK.
  Move move6 = new Move (whiteBishop1.getPos(), new Coordinate(4, 5), "Bishop", true, true);
  move6.setCapture(board.board[5][4].piece);
  GameService.makeMove(board, move6, true);
  GameService.updateBoardMeta(board);
  assertEquals(true, ((King)board.blackPieces.get(0)).isChecked);
  blacksMoves = GameService.generateMoves(board, false);
  blacksMoves = GameService.generateMovesCheck(board, blacksMoves, false);
  assertEquals(6, blacksMoves.size());
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
  //TEST 7: USING THE REVEAL.
  Move move7 = new Move(whitePawn3.getPos(), new Coordinate(6,3), "Pawn", false, true);
  GameService.makeMove(board, move7, true);
  GameService.updateBoardMeta(board);
  whitesMoves = GameService.generateMoves(board, true);
  blacksMoves = GameService.generateMoves(board, false);
  assertEquals(true, ((King)board.blackPieces.get(0)).isChecked);
  assertEquals(true, ((King)board.blackPieces.get(0)).isDoubleChecked);
  blacksMoves = GameService.generateMovesDoubleCheck(board, blacksMoves, false);
  assertEquals(3, blacksMoves.size());

  //TEST 8: Capturing the bishop with the king
  Move move8 = new Move(blackKing.getPos(), new Coordinate (4, 5), "King", true, false);
  move8.setCapture(board.board[5][4].piece);
  GameService.makeMove(board, move8, false);
  GameService.updateBoardMeta(board);
  whitesMoves = GameService.generateMoves(board, true);
  blacksMoves = GameService.generateMoves(board, false);
  assertEquals(false, ((King) board.blackPieces.get(0)).isChecked);
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

  //TEST 9: Queen Check
  Move move9 = new Move(whiteQueen.getPos(), new Coordinate(2, 5), "Queen", false, true);
  GameService.makeMove(board, move9, true);
  GameService.updateBoardMeta(board);
  blacksMoves = GameService.generateMoves(board, false);
  assertEquals(true, ((King) board.blackPieces.get(0)).isChecked);
  blacksMoves = GameService.generateMovesCheck(board, blacksMoves, false);
  assertEquals(4, blacksMoves.size());
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

  //TEST 10: Bishop capture
  Move move10 = new Move(blackBishop1.getPos(), new Coordinate(2, 5), "Bishop", true, false);
  move10.setCapture(board.board[5][2].piece);
  GameService.makeMove(board, move10, false);
  GameService.updateBoardMeta(board);
  blacksMoves = GameService.generateMoves(board, false);
  assertEquals(19, blacksMoves.size());
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

  //TEST 11: SETTING A PIN:
  Move move11 = new Move(whiteRook1.getPos(), new Coordinate(0,5), "Rook", false, true);
  GameService.makeMove(board, move11, true);
  GameService.updateBoardMeta(board);
  blacksMoves = GameService.generateMoves(board, false);
  assertEquals(8, blacksMoves.size());
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

  //TEST 12: PUSHING THE PAWN FORAWRD
  Move move12 = new Move(blackPawn1.getPos(), new Coordinate(3,2),"Pawn", false, false);
  GameService.makeMove(board, move12, false);
  GameService.updateBoardMeta(board);
  move12 = new Move(blackPawn1.getPos(), new Coordinate(4,1),"Pawn", true, false);
  move12.setCapture(board.board[1][4].piece);
  GameService.makeMove(board, move12, false);
  GameService.updateBoardMeta(board);
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
  assertEquals(true, ((King)board.whitePieces.get(0)).isChecked);
  whitesMoves = GameService.generateMoves(board, true);
  whitesMoves = GameService.generateMovesCheck(board, whitesMoves, true);
  board = GameService.clearBoard(board);
  assertEquals(5, whitesMoves.size());
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

  //TEST Promotion
  Move move13 = new Move(whiteKing.getPos(), new Coordinate(3,1), "King", false, true);
  GameService.makeMove(board, move13, true);
  GameService.updateBoardMeta(board);
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
  blacksMoves = GameService.generateMoves(board, false);
  assertEquals(11, blacksMoves.size());
  move13 = new Move(blackPawn1.getPos(), new Coordinate(4,0), "Queen", false, false);
  move13.setPromotion(new Queen(move13.toCoord, false));
  GameService.makeMove(board, move13, false);
  GameService.updateBoardMeta(board);
  blacksMoves = GameService.generateMoves(board, false);
  assertEquals(22, blacksMoves.size());
  assertEquals(true, ((King) board.whitePieces.get(0)).isChecked);

  //TEST 14: King capturing the recently promoted Queen
  Move move14 = new Move(whiteKing.getPos(), new Coordinate(4, 0),"King", true, true);
  move14.setCapture(board.board[0][4].piece);
  GameService.makeMove(board, move14, true);
  GameService.updateBoardMeta(board);
  blacksMoves = GameService.generateMoves(board, false);
  assertEquals(7, blacksMoves.size());
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
  //TEST 15: Whites Promotion.
  Move move15 = new Move(whitePawn3.getPos(), new Coordinate(6, 6), "Pawn", false, true);
  GameService.makeMove(board, move15, true);
  GameService.updateBoardMeta(board);
  whitesMoves = GameService.generateMoves(board, true);
  assertEquals(19, whitesMoves.size());
  move15 = new Move(whitePawn3.getPos(), new Coordinate(6, 7), "Knight", false, true);
  move15.setPromotion(new Knight(move15.toCoord, true));
  GameService.makeMove(board, move15, true);
  GameService.updateBoardMeta(board);
  whitesMoves = GameService.generateMoves(board, true);
  assertEquals(18, whitesMoves.size());
  assertEquals(false, ((King) board.blackPieces.get(0)).isChecked);
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
  //MOVING THE KING AWAY FROM THE PIN
  Move move16 = new Move(blackKing.getPos(), new Coordinate(3, 6), "King", false, false);
  GameService.makeMove(board, move16, false);
  assertTrue(board.blackPieces.get(1).isPinned());
  Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
  GameService.updateBoardMeta(board);
  assertTrue(!board.blackPieces.get(1).isPinned());
  Board.printBoard(board, true);
  Move move17 = new Move(whitePawn2.getPos(), new Coordinate(1, 6), "Pawn", false, true);
  GameService.makeMove(board, move17, true);
  GameService.updateBoardMeta(board);
  move17 = new Move(whitePawn2.getPos(), new Coordinate(1, 7), "Pawn", false, true);
  move17.setPromotion(new Knight(move17.toCoord, true));
  GameService.makeMove(board, move17, true);
  GameService.updateBoardMeta(board);
  assertTrue(((King) board.blackPieces.get(0)).isChecked);
  Board.printBoard(board, true);
  }

  @Test
  public void testDeadPosition () {
    initOpposition();
    GameService.updateBoardMeta(board);
    Board.printBoard(board, true);

    Move move1 = new Move(whiteRook1.getPos(), new Coordinate(7, 3), "Rook", true, true);
    move1.setCapture(blackRook1);
    GameService.makeMove(board, move1, true);
    GameService.updateBoardMeta(board);
    move1 = new Move(whiteRook1.getPos(), new Coordinate(7, 4), "Rook", false, true);
    GameService.makeMove(board, move1, true);
    GameService.updateBoardMeta(board);

    assertEquals(false, GameService.isDeadPosition(board.whitePieces, board.blackPieces));
    Move move2 = new Move(blackBishop1.getPos(), new Coordinate(7, 4), "Bishop", true, false);
    move2.setCapture(whiteRook1);
    GameService.makeMove(board, move2, false);
    GameService.updateBoardMeta(board);
    assertEquals(false, GameService.isDeadPosition(board.whitePieces, board.blackPieces));
    move2 = new Move(whiteBishop1.getPos(), new Coordinate(7, 4), "Bishop", true, true);
    move2.setCapture(blackBishop1);
    GameService.makeMove(board, move2, true);
    GameService.updateBoardMeta(board);
    assertEquals(true, GameService.isDeadPosition(board.whitePieces, board.blackPieces));
  }

  //TODO:
  @Test
  public void testDrawByRepetition() {
    List<Board> boardStates = new ArrayList<>();
    init();
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
    GameService.updateBoardMeta(board);
    boardStates.add(GameService.copyBoard(board));

    Move whiteMove = new Move(whiteQueen.getPos(), new Coordinate(7,5), "Queen", false, true);
    GameService.makeMove(board, whiteMove, true);
    GameService.updateBoardMeta(board);
    boardStates.add(GameService.copyBoard(board));
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

    Move blackMove = new Move(blackKnight1.getPos(), new Coordinate(6,6), "Knight", false, false);
    GameService.makeMove(board, blackMove, false);
    GameService.updateBoardMeta(board);
    boardStates.add(GameService.copyBoard(board));
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

    whiteMove = new Move(whiteQueen.getPos(), new Coordinate(7,1), "Queen", false, true);
    GameService.makeMove(board, whiteMove, true);
    GameService.updateBoardMeta(board);
    boardStates.add(GameService.copyBoard(board));
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

    blackMove = new Move(blackKnight1.getPos(), new Coordinate(4,5), "Knight", false, false);
    GameService.makeMove(board, blackMove, false);
    GameService.updateBoardMeta(board);
    boardStates.add(GameService.copyBoard(board));
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

    whiteMove = new Move(whiteQueen.getPos(), new Coordinate(7,5), "Queen", false, true);
    GameService.makeMove(board, whiteMove, true);
    GameService.updateBoardMeta(board);
    boardStates.add(GameService.copyBoard(board));
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

    blackMove = new Move(blackKnight1.getPos(), new Coordinate(6,6), "Knight", false, false);
    GameService.makeMove(board, blackMove, false);
    GameService.updateBoardMeta(board);
    boardStates.add(GameService.copyBoard(board));
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
    assertEquals(false, GameService.isRepetition(boardStates, board));

    whiteMove = new Move(whiteQueen.getPos(), new Coordinate(7,1), "Queen", false, true);
    GameService.makeMove(board, whiteMove, true);
    GameService.updateBoardMeta(board);
    boardStates.add(GameService.copyBoard(board));
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");

    assertEquals(false, GameService.isRepetition(boardStates, board));
    blackMove = new Move(blackKnight1.getPos(), new Coordinate(4,5), "Knight", false, false);
    GameService.makeMove(board, blackMove, false);
    GameService.updateBoardMeta(board);
    boardStates.add(GameService.copyBoard(board));
    Board.printBoard(board, true); System.out.println("|||||||||||||||||||||||||");
    //TODO: FIX THIS!
    //assertEquals(true, GameService.isRepetition(boardStates, board));

  }

}
