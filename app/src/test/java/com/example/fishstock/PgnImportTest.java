package com.example.fishstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;

import org.junit.Test;

/** Pasting games into the analysis board: full PGNs, bare move lists, UCI, odd spellings. */
public class PgnImportTest {

  private static final String LICHESS_PGN =
      "[Event \"Rated Blitz game\"]\n"
          + "[Site \"https://lichess.org/abcdefgh\"]\n"
          + "[White \"Alice\"]\n"
          + "[Black \"Bob\"]\n"
          + "[Result \"1-0\"]\n"
          + "\n"
          + "1. e4 { [%clk 0:05:00] } 1... e5 { [%clk 0:05:00] } 2. Nf3 Nc6 (2... d6 3. d4) "
          + "3. Bc4 Nf6?! $6 4. Ng5 d5 5. exd5 Na5 6. Bb5+ c6 7. dxc6 bxc6 8. Qf3!? Rb8 "
          + "; a line comment\n"
          + "9. Bd3 h6 10. Ne4 Nd5 1-0\n";

  @Test
  public void readsLichessPgnWithCommentsVariationsAndNags() {
    PgnImport.Result r = PgnImport.parse(LICHESS_PGN);
    assertTrue(r.error, r.ok());
    assertEquals(20, r.movesUci.size());
    assertEquals("e2e4", r.movesUci.get(0));
    assertEquals("b8c6", r.movesUci.get(3)); // the (2... d6) variation is skipped
    assertEquals("Alice vs Bob  1-0", r.title);
    assertPlayable(r);
  }

  @Test
  public void readsBareMovesWithAndWithoutNumbers() {
    PgnImport.Result a = PgnImport.parse("e4 e5 Nf3 Nc6 Bb5 a6");
    PgnImport.Result b = PgnImport.parse("1.e4 e5 2.Nf3 Nc6 3.Bb5 a6");
    PgnImport.Result c = PgnImport.parse("1. e4 1... e5 2. Nf3 2... Nc6 3. Bb5 3...a6");
    assertTrue(a.error, a.ok());
    assertEquals(a.movesUci, b.movesUci);
    assertEquals(a.movesUci, c.movesUci);
    assertEquals("Imported game", a.title);
  }

  @Test
  public void readsUciAndLowerCaseAndZeroCastling() {
    PgnImport.Result uci = PgnImport.parse("e2e4 e7e5 g1f3 b8c6 f1c4 g8f6 e1g1");
    PgnImport.Result san = PgnImport.parse("e4 e5 nf3 Nc6 Bc4 Nf6 0-0");
    assertTrue(uci.error, uci.ok());
    assertTrue(san.error, san.ok());
    assertEquals(uci.movesUci, san.movesUci);
    assertEquals("e1g1", san.movesUci.get(6));
  }

  @Test
  public void readsPromotionSpellingsAndFenTag() {
    String fen = "8/P6k/8/8/8/8/8/K7 w - - 0 1";
    for (String move : new String[] {"a8=Q", "a8Q", "a8", "a7a8q", "a8=Q+"}) {
      PgnImport.Result r = PgnImport.parse("[FEN \"" + fen + "\"]\n[SetUp \"1\"]\n1. " + move);
      assertTrue(move + ": " + r.error, r.ok());
      assertEquals(fen, r.startFen);
      assertEquals("a7a8q", r.movesUci.get(0));
    }
    PgnImport.Result knight = PgnImport.parse("[FEN \"" + fen + "\"] a8=N");
    assertEquals("a7a8n", knight.movesUci.get(0));
  }

  @Test
  public void overDisambiguatedSanStillWorks() {
    PgnImport.Result r = PgnImport.parse("Ng1f3 d5 Ngh3");
    assertFalse(r.ok()); // the g1 knight is on f3 now, so there is no knight on the g-file
    PgnImport.Result ok = PgnImport.parse("Ng1f3 d5 Nbc3");
    assertTrue(ok.error, ok.ok());
    assertEquals("b1c3", ok.movesUci.get(2));
  }

  @Test
  public void explainsWhatWentWrong() {
    PgnImport.Result r = PgnImport.parse("1. e4 e5 2. Nf3 Nc6 3. Bb5 Ne4?? 4. Qh5");
    assertFalse(r.ok());
    assertTrue(r.error, r.error.contains("Ne4") && r.error.contains("move 3"));
    assertFalse(PgnImport.parse("").ok());
    assertFalse(PgnImport.parse("[Event \"x\"]").ok());
  }

  @Test
  public void stopsAtTheResult() {
    PgnImport.Result r = PgnImport.parse("1. f3 e5 2. g4 Qh4# 0-1");
    assertTrue(r.error, r.ok());
    assertEquals(4, r.movesUci.size());
  }

  private static void assertPlayable(PgnImport.Result r) {
    Game g = new Game(r.startFen);
    for (String uci : r.movesUci) {
      Move m = g.findUci(uci);
      assertNotNull(uci, m);
      g.play(m);
    }
  }
}
