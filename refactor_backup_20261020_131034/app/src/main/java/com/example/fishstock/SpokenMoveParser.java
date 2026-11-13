package com.example.fishstock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns what the speech recogniser heard ("knight f3", "e takes d5", "bishop takes knight",
 * "castles kingside", "e8 promotes to queen", "g1 to f3") into the legal move(s) it could mean.
 *
 * The recogniser is not a chess player: it writes "before" for b4, "night" for knight, "for" for
 * four and "see" for c. So each word is mapped to what it could mean on a chess board, and the
 * result is checked against the legal moves of the position: only a move that is legal right now
 * can match, which throws out almost all mishearings.
 *
 * Pure Java with no engine or Android imports, so it is unit-tested on its own:
 * {@link RecordGameActivity} hands it the legal moves as {@link Candidate}s (see {@link SpokenMoves}).
 *
 * How a phrase is read:
 * <ol>
 *   <li>"castle(s)", "O-O": a castling move; "short" / "kingside" or "long" / "queenside" picks the side.</li>
 *   <li>The last square named is where the piece goes; a square named before it is where it comes
 *       from ("e2 e4", "knight g1 f3").</li>
 *   <li>The piece named before "takes" (or before the square) is the one moving; a piece named after
 *       "takes" is the one taken ("bishop takes knight" works without a square). No piece named means
 *       a pawn, as in written notation; if no pawn can do it, any piece that can.</li>
 *   <li>A lone file or rank before the square says which piece ("knight b d2", "rook 1 e1", "e takes d5").</li>
 *   <li>A piece named after the square is the promotion ("e8 queen", "e8 promotes to knight");
 *       a promotion with no piece named is a queen.</li>
 * </ol>
 */
public final class SpokenMoveParser {
  private SpokenMoveParser() {}

  /** One legal move, described in the terms people use to say it. */
  public static final class Candidate {
    /** 'P', 'N', 'B', 'R', 'Q' or 'K'. */
    public final char piece;
    public final int fromFile;
    public final int fromRank;
    public final int toFile;
    public final int toRank;
    public final boolean capture;
    /** The piece taken ('P'...'Q'), or 0. */
    public final char captured;
    /** 'N', 'B', 'R', 'Q', or 0 when not a promotion. */
    public final char promotion;
    /** 0 = not castling, 1 = kingside, 2 = queenside. */
    public final int castle;
    /** Whatever the caller wants back (the engine's Move). */
    public final Object move;

    public Candidate(char piece, int fromFile, int fromRank, int toFile, int toRank, boolean capture,
                     char captured, char promotion, int castle, Object move) {
      this.piece = piece;
      this.fromFile = fromFile;
      this.fromRank = fromRank;
      this.toFile = toFile;
      this.toRank = toRank;
      this.capture = capture;
      this.captured = captured;
      this.promotion = promotion;
      this.castle = castle;
      this.move = move;
    }
  }

  /** What a phrase is: a move to look for, "undo", or nothing chess-like at all. */
  public enum Command {
    NONE,
    UNDO
  }

  // ------------------------------------------------------------------ words

  private enum Kind {
    PIECE, FILE, RANK, SQUARE, CAPTURE, CASTLE, SIDE, PROMOTE
  }

  private static final class Token {
    final Kind kind;
    /** PIECE: the letter. FILE / RANK: 0-7. SQUARE: file * 8 + rank. SIDE: 1 king, 2 queen. */
    final int value;
    /** A word that is only a file or rank when its neighbours say so ("a", "be", "for", "to"). */
    final boolean weak;

    Token(Kind kind, int value, boolean weak) {
      this.kind = kind;
      this.value = value;
      this.weak = weak;
    }

    @Override
    public String toString() {
      return kind + ":" + value + (weak ? "?" : "");
    }
  }

  private static final String[][] PIECE_WORDS = {
      {"N", "n", "knight", "knights", "night", "nights", "nite", "horse", "kneight"},
      {"B", "bishop", "bishops", "bishops'", "bishup"},
      {"R", "r", "rook", "rooks", "rock", "rocks", "ruck", "brook", "rookie", "roof"},
      {"Q", "q", "queen", "queens", "quinn", "clean"},
      {"K", "k", "king", "kings", "kink"},
      {"P", "pawn", "pawns", "pon", "pond", "porn", "prawn", "palm"},
  };

  /** Words that are certainly a file. */
  private static final String[][] FILE_WORDS = {
      {"alpha", "alfa"},
      {"b", "bravo", "bee"},
      {"c", "charlie", "cee"},
      {"d", "dee", "delta"},
      {"e", "echo", "ee"},
      {"f", "ef", "eff", "foxtrot"},
      {"g", "gee", "golf", "jee"},
      {"h", "aitch", "hotel", "haitch"},
  };
  /** Words that may be a file, or may be ordinary English ("a", "be", "see", "age"). */
  private static final String[][] WEAK_FILE_WORDS = {
      {"a", "ay", "eh"},
      {"be"},
      {"see", "sea", "si"},
      {},
      {},
      {},
      {},
      {"age", "each", "eight's"},
  };
  private static final String[][] RANK_WORDS = {
      {"1", "one", "first"},
      {"2", "two", "second"},
      {"3", "three", "third"},
      {"4", "four", "fourth"},
      {"5", "five", "fifth"},
      {"6", "six", "sixth", "sicks"},
      {"7", "seven", "seventh"},
      {"8", "eight", "eighth"},
  };
  /** Only a rank straight after a file ("e for", "d to", "g won"). */
  private static final String[][] WEAK_RANK_WORDS = {
      {"won", "wan"},
      {"to", "too", "tu"},
      {"tree", "free"},
      {"for", "fore", "far", "full"},
      {"fife"},
      {"sex", "sax"},
      {},
      {"ate", "aid", "hate"},
  };
  private static final Set<String> CAPTURE_WORDS = set("takes", "take", "taking", "took", "x", "captures",
      "capture", "captured", "capturing", "eats", "by", "texas", "tax", "tikes", "passant");
  private static final Set<String> CASTLE_WORDS = set("castle", "castles", "castled", "castling", "kassel",
      "cassel", "oo", "ooo", "00", "000");
  private static final Set<String> PROMOTE_WORDS = set("promote", "promotes", "promoted", "promoting",
      "promotion", "equals", "equal", "becomes", "=");
  private static final Set<String> UNDO_WORDS = set("undo", "undue", "takeback", "take-back", "redo");

  /** SAN written as one word: "nf3", "exd5", "nbd2", "e8=q", "r1e1", "e2e4". */
  private static final Pattern COMPACT = Pattern.compile("([nbrqkp])?([a-h])?([1-8])?(x|:)?([a-h])([1-8])(?:=?([nbrq]))?");
  private static final Pattern TWO_SQUARES = Pattern.compile("([a-h])([1-8])-?([a-h])([1-8])([nbrq])?");

  // ------------------------------------------------------------------ public API

  /** UNDO for "undo" / "take back"; NONE otherwise (then try {@link #match}). */
  public static Command command(String heard) {
    String s = normalise(heard);
    if (s.contains("take back") || s.contains("takeback")) return Command.UNDO;
    for (String w : s.split(" ")) if (UNDO_WORDS.contains(w)) return Command.UNDO;
    return Command.NONE;
  }

  /**
   * Every legal move the phrase could mean (usually one). Empty if it names no legal move or isn't
   * a move at all; more than one if it is ambiguous ("knight d2" with two knights that can go there).
   */
  public static List<Candidate> match(String heard, List<Candidate> legal) {
    List<Candidate> out = matchTokens(tokenize(heard, false), legal);
    List<Token> other = tokenize(heard, true);
    if (!other.toString().equals(tokenize(heard, false).toString())) {
      for (Candidate c : matchTokens(other, legal)) if (!out.contains(c)) out.add(c);
    }
    return out;
  }

  private static List<Candidate> matchTokens(List<Token> tokens, List<Candidate> legal) {
    List<Candidate> out = new ArrayList<>();
    if (tokens.isEmpty()) return out;

    // 1. Castling.
    boolean castle = false;
    int side = 0;
    for (Token t : tokens) {
      if (t.kind == Kind.CASTLE) {
        castle = true;
        if (t.value != 0) side = t.value;
      }
      if (t.kind == Kind.SIDE) side = t.value;
    }
    if (castle) {
      for (Candidate c : legal) if (c.castle != 0 && (side == 0 || c.castle == side)) out.add(c);
      return out;
    }

    // 2. Squares: the last one is the destination, the one before it the starting square.
    int dest = -1;
    int origin = -1;
    for (int i = tokens.size() - 1; i >= 0; i--) {
      if (tokens.get(i).kind != Kind.SQUARE) continue;
      if (dest < 0) dest = i;
      else {
        origin = i;
        break;
      }
    }

    // 3. Pieces and hints, read in order.
    char mover = 0;
    char taken = 0;
    char promotion = 0;
    boolean capture = false;
    boolean promoteWord = false;
    int fileHint = -1;
    int rankHint = -1;
    for (int i = 0; i < tokens.size(); i++) {
      Token t = tokens.get(i);
      boolean afterDest = dest >= 0 && i > dest;
      switch (t.kind) {
        case CAPTURE:
          capture = true;
          break;
        case PROMOTE:
          promoteWord = true;
          break;
        case PIECE:
          char p = (char) t.value;
          if (afterDest || promoteWord) {
            if (promotion == 0) promotion = p;
          } else if (capture) {
            if (taken == 0) taken = p;
          } else if (mover == 0) {
            mover = p;
          }
          break;
        case FILE:
          if (promoteWord && t.value == 1) { // "e8 = B": the b-file letter is the bishop here
            if (promotion == 0) promotion = 'B';
            break;
          }
          if (!afterDest && i != dest && i != origin) fileHint = t.value;
          break;
        case RANK:
          if (!afterDest) rankHint = t.value;
          break;
        default:
          break;
      }
    }
    if (dest < 0 && !capture) return out;          // nothing that says where: not a move
    if (dest < 0 && mover == 0 && taken == 0 && fileHint < 0) return out; // "takes" alone

    int toFile = dest >= 0 ? tokens.get(dest).value / 8 : -1;
    int toRank = dest >= 0 ? tokens.get(dest).value % 8 : -1;
    int fromFile = origin >= 0 ? tokens.get(origin).value / 8 : fileHint;
    int fromRank = origin >= 0 ? tokens.get(origin).value % 8 : rankHint;

    // A promotion piece only means something on a promotion; "e8 queen" is how people say e8=Q.
    for (int pass = 0; pass < 2; pass++) {
      // Pass 0: no piece named = a pawn (as written). Pass 1: any piece, if no pawn could.
      if (pass == 1 && (mover != 0 || origin >= 0 || !out.isEmpty())) break;
      for (Candidate c : legal) {
        if (c.castle != 0) {
          // "g1" / "c1" for the king's castling move is fine when said as a king move or two squares.
          if (!(mover == 'K' || origin >= 0)) continue;
        }
        if (toFile >= 0 && (c.toFile != toFile || c.toRank != toRank)) continue;
        if (fromFile >= 0 && c.fromFile != fromFile) continue;
        if (fromRank >= 0 && c.fromRank != fromRank) continue;
        if (mover != 0 && c.piece != mover) continue;
        if (mover == 0 && origin < 0 && pass == 0 && c.piece != 'P') continue;
        if (capture && !c.capture) continue;
        if (taken != 0 && c.captured != taken) continue;
        if (c.promotion != 0) {
          char want = promotion == 0 ? 'Q' : promotion;
          if (c.promotion != want) continue;
        } else if (promotion != 0 && promoteWord) {
          continue;
        }
        out.add(c);
      }
    }
    return out;
  }

  /**
   * Tries each of the recogniser's guesses in turn (best first) and returns the first that names
   * exactly one legal move. If none does, returns the most promising ambiguous match (so the screen
   * can ask "Nbd2 or Nfd2?"), or an empty list.
   */
  public static List<Candidate> bestMatch(List<String> hypotheses, List<Candidate> legal) {
    List<Candidate> ambiguous = new ArrayList<>();
    for (String h : hypotheses) {
      List<Candidate> m = match(h, legal);
      if (m.size() == 1) return m;
      if (m.size() > 1 && ambiguous.isEmpty()) ambiguous = m;
    }
    return ambiguous;
  }

  /** "Nxf3+" → "knight takes f3, check": for reading a move back. */
  public static String sanToWords(String san) {
    if (san == null || san.isEmpty()) return "";
    String s = san.replace("!", "").replace("?", "");
    String suffix = "";
    if (s.endsWith("#")) {
      suffix = ", checkmate";
      s = s.substring(0, s.length() - 1);
    } else if (s.endsWith("+")) {
      suffix = ", check";
      s = s.substring(0, s.length() - 1);
    }
    if (s.equals("O-O") || s.equals("0-0")) return "castles kingside" + suffix;
    if (s.equals("O-O-O") || s.equals("0-0-0")) return "castles queenside" + suffix;
    StringBuilder out = new StringBuilder();
    int i = 0;
    boolean pawn = !(i < s.length() && "NBRQK".indexOf(s.charAt(i)) >= 0);
    if (!pawn) {
      out.append(pieceName(s.charAt(i)));
      i++;
    }
    String promo = "";
    int eq = s.indexOf('=');
    if (eq >= 0 && eq + 1 < s.length()) {
      promo = ", promotes to " + pieceName(s.charAt(eq + 1));
      s = s.substring(0, eq);
    }
    String body = s.substring(i);
    boolean takes = body.contains("x");
    body = body.replace("x", "");
    if (body.length() < 2) return san;
    String target = body.substring(body.length() - 2);
    String disamb = body.substring(0, body.length() - 2);
    // Pawns are said the way they're written: "E4", "E takes D5".
    if (!disamb.isEmpty()) out.append(out.length() > 0 ? " " : "").append(spaced(disamb));
    if (takes) out.append(out.length() > 0 ? " takes " : "takes ");
    else if (out.length() > 0) out.append(' ');
    out.append(squareWords(target));
    return out.toString() + promo + suffix;
  }

  /** "e4" → "E4", which speech engines say as "E four" (lower-case "a" can come out as "uh"). */
  public static String squareWords(String square) {
    return square.toUpperCase(Locale.US);
  }

  public static String pieceName(char letter) {
    switch (Character.toUpperCase(letter)) {
      case 'N':
        return "knight";
      case 'B':
        return "bishop";
      case 'R':
        return "rook";
      case 'Q':
        return "queen";
      case 'K':
        return "king";
      default:
        return "pawn";
    }
  }

  // ------------------------------------------------------------------ reading the words

  /** Lower case, chess symbols kept, everything else a space. */
  static String normalise(String heard) {
    if (heard == null) return "";
    String s = heard.toLowerCase(Locale.US)
        .replace("king's side", "kingside").replace("king side", "kingside")
        .replace("queen's side", "queenside").replace("queen side", "queenside")
        .replace("o-o-o", " ooo ").replace("0-0-0", " ooo ").replace("o - o - o", " ooo ")
        .replace("o-o", " oo ").replace("0-0", " oo ").replace("o - o", " oo ")
        .replace("take back", "takeback").replace("taken back", "takeback")
        .replace("×", " x ").replace("=", " = ");
    s = s.replaceAll("[^a-z0-9=:' -]", " ").replace("-", " ");
    return s.replaceAll("\\s+", " ").trim();
  }

  static List<Token> tokenize(String heard) {
    return tokenize(heard, false);
  }

  /**
   * @param bAsFile read a one-word move starting with "b" ("bxc4", "bc4") as the b-pawn rather
   *                than the bishop; {@link #match} tries both, since speech loses the capital letter
   */
  static List<Token> tokenize(String heard, boolean bAsFile) {
    String[] words = normalise(heard).split(" ");
    List<Token> raw = new ArrayList<>();
    for (String w : words) {
      if (w.isEmpty()) continue;
      addWord(w, raw, bAsFile);
    }
    // Pass 1: a file and the rank after it make a square ("e four", "e for", but not "a to").
    List<Token> joined = new ArrayList<>();
    for (int i = 0; i < raw.size(); i++) {
      Token t = raw.get(i);
      Token next = i + 1 < raw.size() ? raw.get(i + 1) : null;
      if (t.kind == Kind.FILE && next != null && next.kind == Kind.RANK && !(t.weak && next.weak)) {
        joined.add(new Token(Kind.SQUARE, t.value * 8 + next.value, false));
        i++;
        continue;
      }
      joined.add(t);
    }
    // Pass 2: what's left of the weak words is ordinary English, unless it is clearly part of a move.
    List<Token> out = new ArrayList<>();
    for (int i = 0; i < joined.size(); i++) {
      Token t = joined.get(i);
      if (t.kind == Kind.RANK && t.weak) continue; // "to", "for": not straight after a file
      if (t.kind == Kind.FILE && t.weak) {
        // "a takes b5", "rook a d1": kept. "takes a pawn", "to be": dropped.
        Token next = i + 1 < joined.size() ? joined.get(i + 1) : null;
        Token prev = out.isEmpty() ? null : out.get(out.size() - 1);
        boolean beforeMove = next != null && (next.kind == Kind.SQUARE || next.kind == Kind.CAPTURE);
        if (!beforeMove || !(prev == null || prev.kind == Kind.PIECE)) continue;
      }
      out.add(t);
    }
    return out;
  }

  private static void addWord(String w, List<Token> out, boolean bAsFile) {
    String word = w.replace("'", "");
    if (word.isEmpty()) return;
    if (word.equals("kingside") || word.equals("short")) {
      out.add(new Token(Kind.SIDE, 1, false));
      return;
    }
    if (word.equals("queenside") || word.equals("long")) {
      out.add(new Token(Kind.SIDE, 2, false));
      return;
    }
    if (CASTLE_WORDS.contains(word)) {
      out.add(new Token(Kind.CASTLE, word.equals("ooo") || word.equals("000") ? 2 : word.equals("oo") || word.equals("00") ? 1 : 0, false));
      return;
    }
    if (CAPTURE_WORDS.contains(word)) {
      out.add(new Token(Kind.CAPTURE, 0, false));
      return;
    }
    if (PROMOTE_WORDS.contains(word)) {
      out.add(new Token(Kind.PROMOTE, 0, false));
      return;
    }
    if (word.equals("before")) { // "b4"
      out.add(new Token(Kind.FILE, 1, false));
      out.add(new Token(Kind.RANK, 3, false));
      return;
    }
    for (String[] group : PIECE_WORDS) {
      for (int k = 1; k < group.length; k++) {
        if (group[k].equals(word)) {
          out.add(new Token(Kind.PIECE, group[0].charAt(0), false));
          return;
        }
      }
    }
    int f = indexIn(FILE_WORDS, word);
    if (f >= 0) {
      out.add(new Token(Kind.FILE, f, false));
      return;
    }
    f = indexIn(WEAK_FILE_WORDS, word);
    if (f >= 0) {
      out.add(new Token(Kind.FILE, f, true));
      return;
    }
    int r = indexIn(RANK_WORDS, word);
    if (r >= 0) {
      out.add(new Token(Kind.RANK, r, false));
      return;
    }
    r = indexIn(WEAK_RANK_WORDS, word);
    if (r >= 0) {
      out.add(new Token(Kind.RANK, r, true));
      return;
    }
    // Two squares in one word: "e2e4", "g1f3", "e7e8q".
    Matcher two = TWO_SQUARES.matcher(word);
    if (two.matches()) {
      out.add(new Token(Kind.SQUARE, (two.group(1).charAt(0) - 'a') * 8 + (two.group(2).charAt(0) - '1'), false));
      out.add(new Token(Kind.SQUARE, (two.group(3).charAt(0) - 'a') * 8 + (two.group(4).charAt(0) - '1'), false));
      if (two.group(5) != null) out.add(new Token(Kind.PIECE, Character.toUpperCase(two.group(5).charAt(0)), false));
      return;
    }
    // Written notation in one word: "nf3", "exd5", "nbd2", "e8q".
    Matcher m = COMPACT.matcher(word);
    if (m.matches()) {
      if (m.group(1) != null) {
        char letter = m.group(1).charAt(0);
        boolean afterPiece = !out.isEmpty() && out.get(out.size() - 1).kind == Kind.PIECE; // "knight bd7"
        if (letter == 'b' && (afterPiece || (bAsFile && m.group(2) == null))) out.add(new Token(Kind.FILE, 1, false));
        else out.add(new Token(Kind.PIECE, Character.toUpperCase(letter), false));
      }
      if (m.group(2) != null) out.add(new Token(Kind.FILE, m.group(2).charAt(0) - 'a', false));
      if (m.group(3) != null) out.add(new Token(Kind.RANK, m.group(3).charAt(0) - '1', false));
      if (m.group(4) != null) out.add(new Token(Kind.CAPTURE, 0, false));
      // Kept as a square even if the next word is a rank.
      out.add(new Token(Kind.SQUARE, (m.group(5).charAt(0) - 'a') * 8 + (m.group(6).charAt(0) - '1'), false));
      if (m.group(7) != null) {
        out.add(new Token(Kind.PROMOTE, 0, false));
        out.add(new Token(Kind.PIECE, Character.toUpperCase(m.group(7).charAt(0)), false));
      }
      return;
    }
    // "e4." "f3," etc. are cleaned already; a file letter glued to a rank word: "efour"? rare. Ignore.
    // Digits such as "84" (heard for "a4"/"e4") are too unreliable to guess at.
  }

  private static int indexIn(String[][] table, String word) {
    for (int i = 0; i < table.length; i++) for (String s : table[i]) if (s.equals(word)) return i;
    return -1;
  }

  private static Set<String> set(String... words) {
    Set<String> s = new LinkedHashSet<>();
    for (String w : words) s.add(w);
    return s;
  }

  private static String spaced(String disamb) {
    StringBuilder sb = new StringBuilder();
    for (char c : disamb.toCharArray()) {
      if (sb.length() > 0) sb.append(' ');
      sb.append(Character.toUpperCase(c));
    }
    return sb.toString();
  }
}
