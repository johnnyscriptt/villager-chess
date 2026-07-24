package com.foldpaper.villagerchess.game;

import com.foldpaper.villagerchess.chess.Board;
import com.foldpaper.villagerchess.chess.engine.DifficultyProfile;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** One running game between a player and a villager. All state is server-side; the player is white. */
final class ChessSession {
	final int gameId;
	final UUID playerId;
	final Villager villager;
	final Board board = Board.startpos();
	final DifficultyProfile profile;
	final Random random = new Random();
	/** Cancels an in-flight engine search when the session dies. */
	final AtomicBoolean stop = new AtomicBoolean();

	/** Moves applied so far; used to reject stale engine results. */
	int movesMade;
	/** True while the engine is computing the villager's reply. */
	boolean aiThinking;

	ChessSession(int gameId, UUID playerId, Villager villager, int villagerLevel) {
		this.gameId = gameId;
		this.playerId = playerId;
		this.villager = villager;
		this.profile = DifficultyProfile.forVillagerLevel(villagerLevel);
	}

	boolean isPlayersTurn() {
		return !aiThinking && board.sideToMove() == Board.WHITE;
	}
}
