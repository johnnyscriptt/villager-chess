# CurseForge listing - Villager Chess

Copy-paste material for the CurseForge project. CurseForge's description editor is a WYSIWYG -
paste the text below and re-apply the two headings and the table with the editor's toolbar
(everything else survives a plain paste). Form values live in `UPLOAD_CHECKLIST.md`.

---

## Short description / summary field

Play chess against villagers! A Play Chess button on the trade screen starts a full game - their skill scales with trading level, from Novice pushover to near-unbeatable Master.

---

## Description (paste below into the editor)

Ever wondered if that Master-level librarian is actually smart? Challenge them to chess and find out.

Open any villager's trade screen and hit **Play Chess**. A full chess board opens right there - no items, no commands, no setup. You always play White.

**How strong are they?**

A villager's trading level sets its chess strength:

- Novice - hangs pieces, misses mates
- Apprentice - punishes only blatant blunders
- Journeyman - solid club beginner
- Expert - strong; rarely errs
- Master - near-unbeatable

So a fresh Novice is a free win - but if you want a real fight, trade one up to Master first.

**How to play**

- Click a piece, then click one of its highlighted squares.
- Pawns reaching the last rank open a promotion picker.
- Full rules are enforced: castling, en passant, threefold repetition, the 50-move rule, and insufficient-material draws.
- Resign with the button, or just close the board. Walking more than 16 blocks away, the villager dying, or disconnecting abandons the game.
- No stakes, no rewards - just for fun (and bragging rights).

**Under the hood**

- A from-scratch pure-Java chess engine: 0x88 board, full move legality, Zobrist hashing - move generation verified against perft reference counts.
- Negamax search with alpha-beta pruning, iterative deepening, and quiescence, on a background thread so the server never stutters.
- Fully server-authoritative: the server owns all game state; clients only send move attempts.

**Requirements**

- Minecraft 26.3
- Fabric Loader 0.19.3+ and Fabric API
- Java 25
- Install on both client and server (works in singleplayer out of the box)

---

## Suggested form picks

- **Categories:** Miscellaneous (primary); Mobs fits as secondary
- **Loader:** Fabric
- **Java version:** 25
- **License:** MIT (pick "MIT License" in the dropdown)
- **Relations:** add **Fabric API** as a *Required Dependency*
- Project approval on CurseForge is manual and can take hours to a few days - submit early, add gallery images while you wait.
