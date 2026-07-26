# Media guide + in-game QA — Villager Chess 0.1.0

> ## Status: session run 2026-07-24 — screenshots DONE, most QA PASSED
>
> All six gallery shots are captured in `screenshots/` (a full Novice game was played to
> checkmate: 1.e4 → 33.Rxa6#). **Verified in-game:** Play Chess button placement, board
> rendering + click-to-move, legal-move highlights, check state (red king + "Your move —
> check!"), promotion picker (all 4 choices shown), "You win!" screen + clean close,
> Resign button (vs the Master), "…is thinking" indicator, material bar, SAN move list —
> plus castling (11.O-O) and **en passant** (7.exd6) through the click UI.
>
> **Trailer: DONE** — `trailer/villager-chess-trailer.mp4`, 46.7 s, 1080p30, built from the
> two recorded clips plus the gallery stills by `trailer/build_trailer.ps1` (section 4).
>
> **Still for a human before upload (5 min):** villager sounds (automation is deaf),
> walk >16 blocks away mid-game abandons, villager dying mid-game abandons, Esc-close
> resigns, and the "already playing chess!" busy message (needs a second player). These five
> are now the *only* thing standing between here and `UPLOAD_CHECKLIST.md`.

This is a **combined session**: the chess UI has never been verified by a human in-game
(the code is tested, the pixels are not). Every scene below has a **VERIFY** part (QA) and a
**CAPTURE** part (marketing). Play through in order — if a VERIFY item fails, stop, fix,
rebuild, and restart the session. **Nothing gets uploaded until every VERIFY passes.**

---

## 1. Setup (10 min)

1. **Launch:** `.\gradlew.bat runClient` from the project root (uses the dev world in `run\`).
2. **World:** create a new world — Creative, Superflat, peaceful, `/gamerule doDaylightCycle false`
   and `/time set noon` for consistent bright lighting in every shot.
3. **Villagers:** summon one low-level and one max-level villager:
   ```
   /summon minecraft:villager ~ ~ ~ {VillagerData:{profession:"minecraft:librarian",level:1,type:"minecraft:plains"}}
   /summon minecraft:villager ~ ~ ~ {VillagerData:{profession:"minecraft:librarian",level:5,type:"minecraft:plains"}}
   ```
   ⚠ NBT syntax may have shifted in 26.2 — if the summon rejects `VillagerData`, check
   `/summon minecraft:villager` tab-completion, or fall back to trading a natural villager up a level.
   Trap them behind a fence or `/summon` them into a boat so they don't wander mid-shoot.
4. **Stage the scene:** a plain villager-house backdrop (oak + cobble) beats void-flat for
   screenshots. Two minutes of set dressing pays off in every image.
5. **Video settings:** windowed 1920×1080, GUI scale **2**, particles minimal, hide the F3 overlay.
   Screenshots: **F2** → saved to `run\screenshots\`.

## 2. Scene-by-scene: QA + shot list

Shot numbers = gallery order on both stores.

### Scene A — the button (lead image ①)
- **VERIFY:** right-click the villager → trade screen opens → the **Play Chess** button is
  visible, not overlapping vanilla widgets (trade list, XP bar, inventory), and hovers/clicks.
- **CAPTURE ①:** trade screen with the button clearly visible. This is the money shot — it
  explains the whole mod in one image. Featured/lead image on both stores.

### Scene B — the board + move highlights (②)
- **VERIFY:** clicking Play Chess opens the board; all 32 pieces render on the right squares;
  clicking a piece highlights its legal moves; clicking a highlighted square makes the move;
  clicking elsewhere deselects; the villager replies with a move; "%s is thinking…" shows.
- **CAPTURE ②:** early middlegame, one of your pieces selected with its legal-move
  highlights visible.

### Scene C — check (③)
- **VERIFY:** put the Novice in check → the "Your move — check!" state shows; illegal moves
  that ignore check are rejected.
- **CAPTURE ③:** the check indicator visible on screen.

### Scene D — promotion picker (④)
- **VERIFY:** march a pawn to the last rank (easy vs the Novice — it hangs everything) →
  picker opens → each of the four choices works → the promoted piece appears and plays.
- **CAPTURE ④:** the promotion picker open.

### Scene E — checkmate (⑤)
- **VERIFY:** checkmate the Novice → "Checkmate!" / "You win!" shows; board closes cleanly
  afterwards; villager sounds play (listen for the trade/celebrate sounds).
- **CAPTURE ⑤:** the victory screen. Second-best shot after ①.

### Scene F — the Master (⑥)
- **VERIFY:** start a game vs the level-5 villager → noticeably stronger play; thinking
  message appears; no client stutter while it thinks (watch the F3 frame graph if unsure).
- **CAPTURE ⑥:** mid-game vs the Master, ideally with "…is thinking" on screen.

### Scene G — exits (QA only, no shot)
- **VERIFY, each in a fresh game:**
  - **Resign** button ends the game with the right message.
  - Closing the board (Esc / Close) resigns.
  - Walking >16 blocks away abandons the game.
  - Re-opening the same villager afterwards offers a fresh game.
  - Two players / second window: villager already in a game shows "This villager is already playing chess!" *(skip if no second account handy)*.
  - Killing the villager mid-game abandons cleanly (no crash, no stuck screen).

## 3. Screenshot polish

- Crop to 16:9 where it helps; both stores display roughly 16:9 cards.
- No debug overlay, no chat history, no coordinates in shot.
- Consistency sells: same world, same lighting, same GUI scale across all six shots.
- PNG straight from F2 is fine — don't re-compress.

## 4. The trailer — built, not edited

`trailer/villager-chess-trailer.mp4` is assembled by **`trailer/build_trailer.ps1`** from
assets already in this repo: `trailer/seg1.mp4` (walk-up → trade screen → board opens),
`trailer/seg2.mp4` (move montage), and four of the gallery screenshots. No video editor is
involved and no further recording is needed.

```powershell
powershell -File publishing\trailer\build_trailer.ps1
```

**Silent by design.** Neither recorded clip has an audio stream, so the story is told with
burned-in captions instead. That also sidesteps the music-licensing risk entirely — do not
add a music bed unless it is explicitly licensed.

**The cut** (46.7 s, 1080p30):

| # | Length | Source | Caption |
|---|---|---|---|
| 1 | 2.5 s | title card | **VILLAGER CHESS** · Fabric · Minecraft 26.2 |
| 2 | 4.4 s | seg1, full frame | "Walk up to any villager" |
| 3 | 3.0 s | seg1, punch-in | "Right-click → Play Chess" |
| 4 | 6.6 s | seg1, punch-in | "A real chess engine, in the trade screen" |
| 5 | 10.7 s | seg2, punch-in, 0.85× | "Click to move · legal moves highlighted" |
| 6 | 3.5 s | `03-check.png` | "It knows when you're in check" |
| 7 | 3.5 s | `04-promotion-picker.png` | "Promote to anything" |
| 8 | 3.5 s | `06-master-thinking.png` | "Level 5 villagers play at Master strength" |
| 9 | 4.0 s | `05-you-win.png` | "Beat them if you can" |
| 10 | 5.0 s | `01-play-chess-button.png`, darkened | end card + Modrinth · CurseForge |

**Re-cutting it** means editing the `TUNABLES` block at the top of the script — cut points,
beat durations, captions, crop — and running it again. `_build/` is scratch and is rebuilt
every run, so there is no state to clean up. Two things in there are load-bearing and worth
understanding before you change them:

- **The punch-in.** `crop=1280:720:320:144` then scale back to 1080p. The GUI occupies only
  the middle third of a 1920-wide frame; without the 1.5× punch-in the board is too small to
  read on a store page. The crop was measured to contain the trade screen, the Play Chess
  button, the board, the villager panel and the move list in both clips.
- **The colour tags.** Video sources and PNG sources land on different colour metadata by
  default, which puts a mid-file parameter change in the output. Every beat is therefore
  pinned to `bt709`/`tv` so the concat stays a clean stream copy.

**Upload to YouTube** (public or unlisted — both embed fine), title "Villager Chess —
Minecraft Fabric mod (26.2)". Put the link in both store descriptions; on Modrinth a YouTube
URL on its own line auto-embeds.

## 5. Done?

Every VERIFY passed and media is captured → go to `UPLOAD_CHECKLIST.md` step 1.
Any VERIFY failed → fix, `.\gradlew.bat build`, and rerun the failed scene before uploading.
