# Modrinth listing - Villager Chess

Copy-paste material for the Modrinth project. Form values live in `UPLOAD_CHECKLIST.md`.

---

## Summary (the short one-liner field, ≤256 chars)

Play chess against villagers! A Play Chess button on the trade screen starts a full game - their skill scales with trading level, from Novice pushover to near-unbeatable Master.

---

## Description (Markdown body - paste everything below this line)

# ♞ Villager Chess

Ever wondered if that Master-level librarian is actually smart? Challenge them to chess and find out.

Open any villager's trade screen and hit **Play Chess**. A full chess board opens right there - no items, no commands, no setup. You always play White.

## How strong are they?

A villager's **trading level sets its chess strength**:

| Villager level | Chess strength |
|---|---|
| Novice | Hangs pieces, misses mates |
| Apprentice | Punishes only blatant blunders |
| Journeyman | Solid club beginner |
| Expert | Strong; rarely errs |
| Master | Near-unbeatable |

So a fresh Novice is a free win - but if you want a real fight, go trade one up to Master first.

## How to play

- Click a piece, then click one of its highlighted squares.
- Pawns reaching the last rank open a **promotion picker**.
- Full rules are enforced: castling, en passant, threefold repetition, the 50-move rule, and insufficient-material draws.
- **Resign** with the button, or just close the board. Walking more than 16 blocks away, the villager dying, or disconnecting abandons the game.
- No stakes, no rewards - just for fun (and bragging rights).

## Under the hood

- A **from-scratch pure-Java chess engine**: 0x88 board, full move legality, Zobrist hashing - move generation verified against perft reference counts.
- Search: negamax with alpha-beta pruning, iterative deepening, and quiescence search, running on a background thread so the server never stutters.
- **Fully server-authoritative**: the server owns all game state; clients only send move attempts. No cheating with a modified client.

## Requirements

- Minecraft **26.3**
- **Fabric Loader** ≥ 0.19.3 + **Fabric API**
- Java **25**
- Install on **both client and server** (works in singleplayer out of the box)

---

## Gallery captions (pair with the shots from MEDIA_GUIDE.md)

1. Lead image - "The Play Chess button on any villager's trade screen"
2. "Click a piece to see its legal moves"
3. "Check! Full rules enforced - castling, en passant, the lot"
4. "Promote your pawn with one click"
5. "Checkmate - you win!"
6. "A Master villager thinking… good luck"

## Suggested form picks

- **Categories:** Game Mechanics, Minigame, Social
- **Environment:** Client **and** server (both required)
- **License:** MIT
- **Donation/source links:** source + issues point at the GitHub repo (only resolve once it's public)
