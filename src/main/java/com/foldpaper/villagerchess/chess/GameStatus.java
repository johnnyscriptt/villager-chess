package com.foldpaper.villagerchess.chess;

/** Terminal state of a chess position. CHECKMATE/STALEMATE refer to the side to move. */
public enum GameStatus {
	ONGOING,
	CHECKMATE,
	STALEMATE,
	DRAW_FIFTY_MOVE,
	DRAW_REPETITION,
	DRAW_INSUFFICIENT_MATERIAL;

	public boolean isOver() {
		return this != ONGOING;
	}
}
