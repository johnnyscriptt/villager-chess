# Villager Chess

A Fabric mod for Minecraft 26.2: play chess against villagers. Open any villager's
trade screen and hit **Play Chess**. The villager's trading level sets its strength:

| Villager level | Chess strength |
|---|---|
| Novice | Hangs pieces, misses mates |
| Apprentice | Punishes only blatant blunders |
| Journeyman | Solid club beginner |
| Expert | Strong; rarely errs |
| Master | Near-unbeatable |

You always play white. Click a piece, then a highlighted square. Pawns reaching the
last rank open a promotion picker. Walking away (>16 blocks), the villager dying, or
disconnecting abandons the game; closing the board resigns it. Just for fun — no
stakes, no rewards.

## Tech notes

- **Chess core** (`chess/`): pure Java, no Minecraft imports — 0x88 board, full legality
  (castling, en passant, promotion, pins), Zobrist hashing, threefold/50-move/material
  draws. Verified by perft reference counts.
- **Engine** (`chess/engine/`): negamax + alpha-beta + iterative deepening + quiescence.
  Difficulty knobs per villager level: search depth, time budget, blunder chance, eval
  noise. Runs on a background thread; results are re-validated on the server tick.
- **Server-authoritative**: clients send move attempts; the server owns all game state.
- Board state travels as FEN in small custom payloads.

## Build & run

```
./gradlew test        # chess core + engine unit tests (perft, mates, self-play)
./gradlew runClient   # dev client
./gradlew build       # mod jar in build/libs/
```

Requires Java 25. Install the jar with Fabric Loader ≥ 0.19.3 + Fabric API for 26.2.
