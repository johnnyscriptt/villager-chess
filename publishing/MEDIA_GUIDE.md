# Media guide + in-game QA — Villager Chess 0.1.0

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

## 4. The trailer (30–60 s)

**Recording:** OBS Studio (best) or Xbox Game Bar `Win+Alt+R` (zero setup). 1080p, 60 fps.
Game audio only — vanilla Minecraft sounds are safe; **don't add music** unless it's
explicitly licensed (store takedowns aren't worth it).

**Storyboard** (record long, cut hard):

| # | Time | Shot |
|---|---|---|
| 1 | 0–5 s | Walk up to a villager in the staged scene, right-click → trade screen. |
| 2 | 5–8 s | Cursor moves to **Play Chess**, click. Hold one beat on the board appearing. |
| 3 | 8–25 s | Quick move montage vs the Novice — 4–6 moves, cut between them, don't show every reply. |
| 4 | 25–33 s | Pawn reaches last rank → promotion picker → queen appears. |
| 5 | 33–45 s | Cut to the Master game: "…is thinking" message, a strong reply, your reaction move. |
| 6 | 45–55 s | Checkmate the Novice → "You win!" Hold on the victory screen. |
| 7 | 55–60 s | End card: freeze on shot ① with the mod name — "**Villager Chess** · Fabric · MC 26.2". |

**Editing:** Clipchamp (preinstalled on Windows 11) is plenty — trim, cut, one title card.
Export 1080p. **Upload to YouTube** (public or unlisted — both embed fine), title
"Villager Chess — Minecraft Fabric mod (26.2)". Put the link in both store descriptions;
on Modrinth a YouTube URL on its own line auto-embeds.

## 5. Done?

Every VERIFY passed and media is captured → go to `UPLOAD_CHECKLIST.md` step 1.
Any VERIFY failed → fix, `.\gradlew.bat build`, and rerun the failed scene before uploading.
