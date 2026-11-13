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
├── search/     AlphaBetaSearch (MinMax with alpha-beta, checks → captures first)
├── endgame/    solved-endgame detection + Lichess tablebase lookup (≤ 7 pieces, needs internet)
├── agents/     Randy, Simple, MinMax, FishStock, Human, AgentSpec, AgentFactory
├── arena/      Arena: agent vs agent for N games, win rates, Elo difference
└── ui/ + activities   MainActivity, GameManager, TournamentActivity, AgentBuilderActivity, GameAnalysis
```

## The agents

| Agent | Brain | Search |
|---|---|---|
| Randy | none | random legal move |
| Simple | `EvalWeights.defaults()` | 1 move ahead, plus a "don't allow mate in one" check |
| MinMax | same weights as Simple | alpha-beta, about 2–5 s per move (iterative deepening to a 5 s limit) |
| FishStock | `FishStock.TRAINED_WEIGHTS` (still the defaults, for now) | same search as MinMax, 7 s per move |
| Custom | defaults scaled by your sliders | 1-ply, or MinMax for the chosen time |

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
6. **Outposts and placement**: knight and bishop outposts, bishop pair, long diagonals, rooks on open
   files and the 7th rank, connected rooks, development, early queen moves.
7. **Endgame**: king centralisation, mop-up (drive the lone king to the edge), drawish-material scaling.

`Evaluator.explain(position)` prints each term's contribution, which helps when tuning.

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
- Pieces now slide when moved. The cut-out images in `drawable-nodpi/anim_*.png` were made from
  the existing piece art.
