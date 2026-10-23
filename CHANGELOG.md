# Changelog

All notable changes to Villager Chess are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/); versions follow [SemVer](https://semver.org/).

## [0.1.3] - 2026-10-22

### Fixed
- Pieces can be moved again. On Minecraft 26.3 the board ignored every mouse click, so no piece could be selected or moved.

## [0.1.2] - 2026-09-30

### Changed
- Updated to Minecraft 26.3 (Fabric Loader 0.19.5, Fabric API 0.161.0+26.3). No gameplay changes.

## [0.1.1] - 2026-07-25

### Fixed
- The **Play Chess** button now stacks below other Villager Games mods' trade-screen buttons instead of landing on the same pixel. With only this mod installed the button is exactly where it was.

## [0.1.0] - 2026-07-24

Initial release.

### Added
- **Play Chess** button on every villager's trade screen - challenge any villager to a full game of chess (you play White).
- Difficulty scales with the villager's trading level: Novice (hangs pieces) → Apprentice → Journeyman → Expert → Master (near-unbeatable).
- Full chess rules: castling, en passant, pawn promotion with a piece picker, threefold repetition, 50-move rule, and insufficient-material draws.
- Click-to-move board UI with legal-move highlights, check indicator, and Resign/Close buttons.
- Abandon handling: walking more than 16 blocks away, the villager dying, or disconnecting abandons the game; closing the board resigns.
- From-scratch chess engine (negamax + alpha-beta + iterative deepening + quiescence search) running on a background thread; move generation is perft-verified.
- Fully server-authoritative: the server owns all game state; clients only send move attempts.
