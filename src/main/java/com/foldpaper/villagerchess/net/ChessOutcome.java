package com.foldpaper.villagerchess.net;

/** How a chess game against a villager stands or ended, from the player's point of view. */
public enum ChessOutcome {
	ONGOING,
	PLAYER_WON,
	VILLAGER_WON,
	DRAW_STALEMATE,
	DRAW_FIFTY_MOVE,
	DRAW_REPETITION,
	DRAW_INSUFFICIENT_MATERIAL,
	PLAYER_RESIGNED,
	ABANDONED;

	private static final ChessOutcome[] VALUES = values();

	public static ChessOutcome byId(int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : ABANDONED;
	}

	public boolean isOver() {
		return this != ONGOING;
	}
}
