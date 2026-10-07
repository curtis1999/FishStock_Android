# FishStock – how the code is organised

All chess logic is plain Java (no Android imports), so it runs in unit tests and on a desktop JVM.
The screens only draw what the engine says.

```
com.example.fishstock
├── engine/     the rules: Position, Move, MoveGenerator, Game, Rules, Notation
├── eval/       the evaluation function
│   ├── Term.java            every tunable number, with default, range, description
│   ├── Category.java        groups of terms (one slider each on "Make Your Own Agent")
│   ├── EvalWeights.java     one value per Term: an agent's "personality"
│   ├── BoardAnalysis.java   one named detector per idea (isPinnedToKing, isOutpost, ...)
│   ├── Evaluator.java       interface: evaluate(), weights(), explain()
│   └── WeightedEvaluator.java   detectors × weights = score
├── search/     AlphaBetaSearch (MinMax with alpha-beta, checks → captures first), MateFinder
├── endgame/    solved-endgame detection + Lichess tablebase lookup (≤ 7 pieces, needs internet)
├── agents/     Blunder, Randy, Simple, MinMax, FishStock, Human, PlayStyle, OpeningBook, AgentSpec, AgentFactory
├── arena/      Arena: agent vs agent for N games, win rates, Elo difference; Openings
├── puzzles/    PuzzleGenerator (multi-move puzzles), ThemeDetector + Theme, EndgameSampler, PuzzleBook
├── analysis/   PositionAnalyzer + PositionProfile (labels a position), MoveReview (judges a move)
└── ui/ + activities   MainActivity, GameManager, TournamentActivity, AgentBuilderActivity,
                       GameAnalysis, PuzzleActivity
```

## The agents

| Level | Agent | Brain | Search | Openings |
|---|---|---|---|---|
| -1 | Blunder | `EvalWeights.defaults()` | 1 move ahead, plays the move it likes *least* | none |
| 0 | Randy | none | random legal move | none |
| 1 | Simple | `EvalWeights.defaults()` | 1 move ahead, plus a "don't allow mate in one" check | main lines |
| 2 | Timid | defaults × `PlayStyle.timidSliders()`, aggression −1 | MinMax search | kingside fianchetto / Caro-Kann |
| 3 | Agro | defaults × `PlayStyle.agroSliders()`, aggression +1 | MinMax search; ~35% of its time hunting checks-only mates | King's Gambit / Najdorf, King's Indian |
| 4 | MinMax | same weights as Simple | alpha-beta, about 2–5 s per move | main lines |
| 5 | FishStock | `FishStock.TRAINED_WEIGHTS` (still the defaults, for now) | same search as MinMax, 7 s per move | main lines |
| Custom | Custom | defaults scaled by your sliders, plus Aggression | 1-ply, or MinMax for the chosen time | by aggression |

The order of Timid, Agro and MinMax comes from 12-game matches at 0.2 s a move: Timid beat Simple
12–0, Agro beat Timid 79%, MinMax beat Agro 71%. The home screen lists them in this order
(`AgentSpec.OPPONENTS`) in a dropdown.

In solved endgames (≤ 7 pieces), Simple, MinMax and FishStock ask the Lichess tablebase for the
perfect move when the phone is online. Tournaments never use it.

## What the evaluation looks at

Each feature has its own detector in `BoardAnalysis` and its own weight in `Term`:

1. **Attackers / defenders**: `exchangeLoss(sq)` is a static exchange evaluation. A piece that will be
   lost on the next move counts as worth much less (`EN_PRISE_FACTOR`, `SECOND_THREAT_FACTOR`).
   Also `DEFENDED_PIECE_BONUS` and `LOOSE_PIECE_PENALTY`.
2. **Mobility** (deliberately small): safe moves for each piece compared with a baseline.
   Knight centre, rim and corner bonuses sit under piece placement.
3. **Pins and reveal checkers**: a pinned piece's value is multiplied by `PINNED_TO_KING_FACTOR` or
   `PINNED_TO_QUEEN_FACTOR`. Discovered-check pieces get `REVEAL_CHECKER_BONUS`.
4. **King safety** (fades out as pieces come off): pawn shield, open files near the king,
   attacks on the king zone, escape squares, x-rays, castled or stuck in the centre.
5. **Pawn structure**: doubled, isolated, backward, connected, central, passed (growing as it advances),
   protected passed, one step from promotion, unstoppable (rule of the square).
   Pawn race (`pawnRaceWinner`, `WINS_PAWN_RACE_BONUS`): in a pawn ending, an unstoppable pawn that
   queens before any enemy pawn is worth nearly a queen straight away. Without this the engine only
   saw breakthroughs like b6! once the new queen was on the board (depth 8 instead of 5).
6. **Outposts and placement**: knight and bishop outposts, bishop pair, long diagonals, rooks on open
   files and the 7th rank, connected rooks, development, early queen moves.
7. **Endgame**: king centralisation, mop-up (drive the lone king to the edge), drawish-material scaling.

`Evaluator.explain(position)` prints each term's contribution, which helps when tuning.

## Personalities: Agro and Timid

`PlayStyle` turns one number, aggression from −1 (Timid) to +1 (Agro), into three things:

1. **Move preferences.** A bonus added to our capturing moves (+0.35 pawns for Agro, −0.35 for
   Timid) and checking moves (+0.25 / −0.10). It is applied only to the agent's own candidate moves
   at the root of the search (`AlphaBetaSearch.setRootBonus`), so Agro takes a piece rather than play
   a quiet move that scores up to 0.35 better, and Timid only captures when not capturing would cost
   it more than that.
2. **Side bias** (`eval/SideBias`). The normal evaluation is symmetric. With a side bias each category
   has an "own" and an "enemy" multiplier: Agro counts the enemy's king-safety problems ×1.6 and loose
   enemy pieces ×1.3, and its own king safety and pawn structure ×0.7. Timid counts its own king safety
   ×1.6, pawn structure ×1.5 and piece safety ×1.4. `WeightedEvaluator.setPerspective(white)` says
   which side is "own"; the agents call it before every search.
3. **Search.** Agro spends about a third of its thinking time in `MateFinder`, a checks-only hunt for
   forced mates up to mate in 4 (7 plies, far deeper than the full search reaches in that time), and
   also searches checks at the end of its normal search.

On top of that, Agro and Timid use different category sliders (`PlayStyle.agroSliders()`: initiative
×2, mobility ×1.5, pins ×1.4...; `timidSliders()`: king safety, pawn structure and piece safety up,
initiative down).

In a 6-game test at depth 3, when a capture was available Agro took 38% of the time and Timid 27%;
Agro gave half as many checks again; Agro scored +2 overall.

The same aggression number is a slider on Make Your Own Agent (saved as a 5th field in the agent's line;
older saved agents load as neutral), so it can also be part of a future training run.

## Openings

`OpeningBook` holds hard-coded lines, written in normal notation and looked up by position (so
transpositions work). Which repertoire an agent uses depends on its aggression:

| aggression | as White | as Black |
|---|---|---|
| 0.8 and up | King's Gambit (Smith-Morra vs the Sicilian) | Najdorf / Sveshnikov, King's Indian |
| 0.4 to 0.8 | Evans Gambit and the Fried Liver | Two Knights, Schliemann, Albin Countergambit |
| 0.1 to 0.4 | Italian (Giuoco Piano) | Italian, Queen's Gambit Declined |
| −0.1 to 0.1 | main lines: Ruy Lopez (Closed, Marshall, Berlin), Open Sicilian, French, Caro-Kann | Ruy Lopez, QGD, Nimzo-Indian |
| below −0.1 | kingside fianchetto (King's Indian Attack) | Caro-Kann, King's Indian set-up |

Book moves are played without question (a gambit loses a pawn by the evaluation; that's the point).
The fianchetto repertoire also has a "system" (Nf3, g3, Bg2, O-O, d3...) played in the first ten moves
after the book runs out, if a 2-ply check says the move is within 0.6 pawns of the best. `AgentFactory`
gives every thinking agent its book; agents built directly in code (training) have none.
Add a line to grow a repertoire: "W ..." lines are played as White, "B ..." as Black;
`ReviewBookOpeningTest.everyBookLineIsLegal` catches typos.

## Custom agents

Saved agents (Make Your Own Agent → Save) are stored as one line of text each in SharedPreferences
(`AgentStore`). They show up:

- on the home screen, under FishStock, labelled "Custom" (tap to play, long-press to edit),
- in both agent lists on the Tournament screen.

The Tournament screen has its own MAKE YOUR OWN AGENT button. It opens the builder (loaded with
Agent 1 if that is a custom agent); USE IN TOURNAMENT saves the agent and returns with it as Agent 1.

## Tournaments and openings

Two searching agents left alone play the same game again and again, so a 100-game match would really
be two games. With "Start games from varied openings" ticked (the default), each pair of games starts
from the next line in `arena/Openings.java`, once with each agent as White. Add lines there to widen
the test set. `Arena.playMatch(..., useOpenings = true, ...)` does the same from code.

For judging a change, play the new agent against the agent it was copied from, 50+ games, short
think time. A score of 55% over 50 games is still within noise; look for 60%+ or play more games.

## Puzzles

`PuzzleGenerator` (pure Java, uses FishStock's weights):

1. **Sample positions.** Middlegames: games from a random opening where both sides pick moves
   "sloppily" (every move scored one ply deep, one drawn with probability ~ exp(score / 0.35)).
   Endgames (15% of the time, or always with "New endgame puzzle" chosen under Select theme): `EndgameSampler` builds
   kings + pawns (sometimes a rook or minor piece each), and a quarter of the time the textbook
   breakthrough setup (three pawns on the 5th facing three on the 7th) at a random place on the board.
2. **Quick test**: best move and second-best move at depth 2 (endgames 4, bare pawn endings 5).
3. **Confirm** at depth 4 (endgames 6, bare pawn endings 7). The first move must win (≥ +1.5 or a forced
   mate), every other move must be < +0.75, and it can't be a giveaway (a recapture or grabbing a loose
   piece, unless it mates).
4. **Extend the line.** Play the opponent's best reply; if the solver again has exactly one move that
   keeps the win (the next best is at least 1.2 pawns worse, or the only/shortest mate), add it. Up to 4
   solver moves. One-move puzzles are kept only 10% of the time, or if nothing longer turns up in 15 s.
   In tests 16 of 16 middlegame puzzles were 2–4 moves long.
5. **Themes** (`ThemeDetector`), each with an explicit definition in the class comment: fork, double
   check, discovered check (uses `BoardAnalysis.isRevealChecker`), discovered attack, pin, skewer, remove
   the defender (a piece is captured and what it guarded is then won), deflection (a defender is forced
   away and what it guarded is then won), attraction, sacrifice, promotion / underpromotion, pawn
   breakthrough, quiet move, checkmate / back-rank mate / smothered mate, endgame.

On the screen, each correct move is followed by the opponent's reply and "Move 2 of 3". HINT first gives
the idea of the most specific theme ("Look for a fork"), then the piece. The themes are shown once the
puzzle is solved. Any move that mates on the spot is accepted.

On a desktop a puzzle takes 0.3–25 s (usually under 10); expect a few times longer on a phone. The
thresholds are constants at the top of `PuzzleGenerator`.

## Puzzle collection

`PuzzleBook` holds puzzles found ahead of time, grouped for the puzzle screen's "Select theme" menu
(Mate in 1–4, then one group per theme; a puzzle is in every group it fits). There are 10 for now, one
per category: mate in 1, 2, 3 and 4, fork, skewer/pin, discovered/double check, remove the defender /
deflection, pawn race, pawn breakthrough. To add more, raise the counts in
`app/src/test/java/.../tools/BuildPuzzleBook.java`, run its `main` on a desktop (the 10 took about
6,000 positions and a few minutes) and paste the output between the GENERATED markers. Mates of 3–4
moves are found with `MateFinder` first and then confirmed by a full search at 2×N plies. Themes are
recomputed when the book loads, so improvements to `ThemeDetector` apply to old puzzles too.

## Analysis screen

- **Graph of the game** (`ui/EvalGraphView`): top half White, bottom half Black, one node per move,
  filled in move by move in the background (depth 3, about 0.25 s per position). Red nodes are moves that
  threw away 2+ pawns; the purple node is the move on the board. Tap or drag to jump.
- **VIEW LINES** (`ui/LinesDialog` + `ui/MoveSpectrumView`): every legal move scored with a full search
  (`AlphaBetaSearch.scoreAllMoves`, depth 3), drawn as bars from best to worst, from the point of view of
  the side to move. Bars within half a pawn of the best are solid, so the number of good moves stands out.
  Tap a bar to see the move.
- **Position labels** (`analysis/PositionProfile`, worked out by `PositionAnalyzer`):
  - *Puzzle*: one move wins and nothing else does (the same test as the puzzle generator); if the side to
    move has another only-winning move after the best reply, it's labelled as a 2- or 3-move combination.
  - *Only move*: every other move is at least 1.5 pawns worse.
  - *Sharp*: walking 4 plies down the best line, on average two or fewer good moves at each step.
  - *Calm*: 35%+ of moves are good and the standard deviation is under 1 pawn.
  - *Winning / Losing / Balanced*.
  The dialog also shows the best, second best and average score, and the standard deviation of the
  spread ("mistakes are costly here" above 2 pawns).

## Tweaking by hand

Change a default in `Term.java`. Nothing else needs to change. `EvaluatorTest` and `FeatureTest`
check that the evaluation stays colour-symmetric and that each detector works.

## Training (prepared, not implemented)

The pieces are in place:

```java
EvalWeights candidate = EvalWeights.fromVector(vector);           // any optimiser's output
MatchResult r = Arena.playMatch(
    () -> new MinMax("candidate", candidate, SearchLimits.forDepth(2), EndgameOracle.NONE),
    () -> new MinMax("baseline", EvalWeights.defaults(), SearchLimits.forDepth(2), EndgameOracle.NONE),
    200, Arena.DEFAULT_MAX_PLIES, null, null);
double fitness = r.scoreA();                                      // maximise this
```

Use fixed depth (`SearchLimits.forDepth`) for training so results don't depend on machine speed.
Run it on a desktop JVM (for example from a JUnit test), which is much faster than the phone.
When you're happy with the result, paste `candidate.serialize()` into `FishStock.TRAINED_WEIGHTS`.

The Tournament screen appends every match to `tournament_results.csv` in the app's files folder,
and SHARE RESULTS exports the full history.

## Move review

- **Move verdicts** (`analysis/MoveReview`): every move gets Brilliant, Best, Okay, Mistake, Blunder or
  Miss, shown under the lines for the move that led to the board position, with a sentence on why, and
  coloured nodes on the graph. Scores become a winning chance w = 1/(1+e^(−0.368·pawns)), and the verdict
  is how much winning chance was thrown away versus the engine's move: under 2% Best, under 10% Okay,
  10–20% Mistake, over 20% Blunder. Miss: a winning move (+2 or mate) existed and the move let it slip
  without going on to lose. Brilliant: as good as best, gives up 2+ points for good (still down after the
  next four plies of the expected line) or leaves a piece en prise, in a position that wasn't already won.
  The "why" looks, in order, for: an allowed mate; a big piece left en prise; a tactic in the opponent's
  best reply line (`ThemeDetector`: "it allows a fork: 24... Nd3+"); for a miss, the tactic in the best line
  ("you missed a fork"); then the evaluation term that got most worse compared with the best move and that
  the move itself caused ("this move is not bad, but it weakens your king by opening a file next to it
  (king safety −0.25)"). The term phrases are a table at the bottom of `MoveReview`, easy to extend.
  A summary per side follows the graph.

## Bugs fixed from the 2023 version

- Check, mate and stalemate were only tested after White moved, so Black couldn't be checkmated
  properly and promotion-mates showed no game-over box. One `Game` class now decides the result
  after every move. Threefold repetition and the 50-move rule are added.
- Pinned pieces, en passant, castling through check, and promotion under check are all handled by
  one legality filter. It's verified against the standard perft counts.
- Agents thought on the UI thread, so the app froze. They now run in the background and can be
  cancelled by undo, resign or leaving the screen.
- Undo: one take-back per move, disabled in hard mode, and it no longer corrupts the board.
- Captured-piece counters and scores are derived from the move list, so they stay right with
  undo, flip, the arrows, and human vs human.
- The board is drawn before the game-over box appears.
- Agents avoid repetition draws when winning (repetitions score as 0 in the search).
- The draw offer now asks the agent's own evaluation (it used to accept when *it* was winning).
- The landscape tournament layout was an empty stub that crashed. It's removed, and the board
  screens are portrait-only.
- The third analysis line's text was attached to the wrong edge of its score label.
- The Game Over box was a plain Dialog that wrapped its contents, so on a phone the text and three
  buttons were squeezed together. It's now 90% of the screen wide, says how the game ended, and
  PLAY AGAIN has its own row.
- The Tournament screen's dropdowns were white on white in dark mode, and its last button was below
  the bottom of the screen. The screen now scrolls, the dropdowns are black on white, and "Share
  Results" (which sent the results and the CSV history to another app) moved into VIEW RESULTS as
  SHARE AS TEXT, next to a VIEW GAME button for every game.
- Pieces now slide when moved. The cut-out images in `drawable-nodpi/anim_*.png` were made from
  the existing piece art.
