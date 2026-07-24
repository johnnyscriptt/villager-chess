# Upload checklist — Villager Chess 0.1.0

Work top to bottom. **Do not upload until every box in step 0 is checked.**

## 0. Gate: QA + media session

- [ ] Complete the QA/capture session in `MEDIA_GUIDE.md` — every VERIFY item passes
- [ ] Any failure: fix it, rebuild, recapture, and only then continue
- [ ] Screenshots picked and cropped (at least the lead shot + 3 more)
- [ ] Trailer uploaded to YouTube (public or unlisted) — optional but strongly recommended

## 1. The file you upload

- **`build\libs\villagerchess-0.1.0.jar`** — this one and only this one.
- Never upload `villagerchess-0.1.0-sources.jar`.
- Sanity check: jar contains the MIT `LICENSE`, the new icon, and contact URLs in `fabric.mod.json` (already verified at build time).

## 2. Shared form values (both sites)

| Field | Value |
|---|---|
| Name | Villager Chess |
| Slug | `villagerchess` |
| Summary | see `MODRINTH.md` / `CURSEFORGE.md` (same line) |
| Icon | `publishing\icon-512.png` |
| License | MIT |
| Loader | Fabric |
| Game version | 26.2 |
| Environment | Client **and** server — both required |
| Dependency | **Fabric API — Required** |
| Version number | 0.1.0 |
| Release channel | **Release** |
| Changelog | paste the 0.1.0 section from `CHANGELOG.md` |

## 3. Modrinth (do this first — instant publish)

1. modrinth.com → **Create a project** → fill name/slug/summary, visibility **Public** (or Draft until media is ready).
2. Description tab → paste the body from `MODRINTH.md`.
3. Settings: License **MIT**; Categories **Game Mechanics, Minigame, Social**; Environment **client & server: required**.
4. Links: Source `https://github.com/johnnyscriptt/villager-chess`, Issues `.../issues` — *these 404 while the repo is private; either make the repo public first or leave the links blank for now.*
5. Gallery: upload screenshots with captions from `MODRINTH.md`; set the trade-screen shot as **featured**.
6. **Versions → Create version**: upload the jar, version `0.1.0`, channel Release, MC `26.2`, loader Fabric, add **Fabric API** as required dependency, paste changelog.
7. Add the YouTube trailer link in the description (Modrinth embeds YouTube URLs on their own line).

## 4. CurseForge (manual approval — hours to days)

1. legacy.curseforge.com → **Start a Project** → Minecraft → **Mods**. Name, slug, summary, icon as above.
2. Description → paste from `CURSEFORGE.md`, re-apply formatting in the editor.
3. Categories: **Miscellaneous** (+ Mobs). License: **MIT License** from the dropdown.
4. Source/Issues URLs: same GitHub caveat as above.
5. **Upload file**: the jar; display name `Villager Chess 0.1.0`; channel **Release**; game version **26.2**; loader **Fabric**; Java **25**; paste changelog (choose Markdown changelog type).
6. Relations tab on the file → **Fabric API** as *Required Dependency*.
7. Gallery images + embed the trailer, then submit for approval. Approval is manual; images can be added while waiting.

## 5. After both are live

- [ ] Open each public page in a private browser window: icon renders, description formatting intact, file downloads, dependency shows Fabric API.
- [ ] If the Modrinth slug ended up different from `villagerchess`, update `homepage` in `src\main\resources\fabric.mod.json` for the next release.
- [ ] Decide on making the GitHub repo public so Source/Issues links resolve (it ships under MIT either way).
- [ ] Commit any last tweaks; tag the release: `git tag v0.1.0 && git push --tags`.

## Known nits (fine to ship, fix in 0.1.1)

- CurseForge "server pack" prompts don't apply to a mod — skip them.
- Both sites let you edit *everything* (icon, description, gallery) after publishing except the uploaded file itself — file mistakes need a new version.
