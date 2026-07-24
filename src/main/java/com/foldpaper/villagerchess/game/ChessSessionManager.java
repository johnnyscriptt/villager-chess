package com.foldpaper.villagerchess.game;

import com.foldpaper.villagerchess.VillagerChess;
import com.foldpaper.villagerchess.chess.Board;
import com.foldpaper.villagerchess.chess.GameStatus;
import com.foldpaper.villagerchess.chess.Move;
import com.foldpaper.villagerchess.chess.engine.Engine;
import com.foldpaper.villagerchess.mixin.MerchantMenuAccessor;
import com.foldpaper.villagerchess.net.ChessMoveC2S;
import com.foldpaper.villagerchess.net.ChessOpenS2C;
import com.foldpaper.villagerchess.net.ChessOutcome;
import com.foldpaper.villagerchess.net.ChessStateS2C;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server authority for all chess games. Player moves are validated and applied on the
 * server thread; engine searches run on a single background thread and their results
 * are re-validated and applied in the end-of-tick hook.
 */
public final class ChessSessionManager {
	public static final ChessSessionManager INSTANCE = new ChessSessionManager();

	private static final double START_DISTANCE_SQ = 8 * 8;
	private static final double ABANDON_DISTANCE_SQ = 16 * 16;
	private static final int VALIDATE_EVERY_TICKS = 20;
	private static final int MIN_THINK_MS = 300, EXTRA_THINK_MS = 1200;

	private final Map<UUID, ChessSession> byPlayer = new HashMap<>();
	private final Map<UUID, ChessSession> byVillager = new HashMap<>();
	private final ConcurrentLinkedQueue<PendingAiMove> completedSearches = new ConcurrentLinkedQueue<>();
	private final AtomicInteger nextGameId = new AtomicInteger(1);
	private ExecutorService engineExecutor;
	private int tickCounter;

	private record PendingAiMove(UUID playerId, int gameId, int expectedMovesMade, int move) {}

	private ChessSessionManager() {}

	/** Handles the "Play Chess" button: resolves the villager from the player's open trade menu. */
	public void startGame(ServerPlayer player) {
		if (!(player.containerMenu instanceof MerchantMenu menu)) return;
		if (!(((MerchantMenuAccessor) menu).villagerchess$getTrader() instanceof Villager villager)) return;
		if (!villager.isAlive() || villager.distanceToSqr(player) > START_DISTANCE_SQ) return;
		if (byVillager.containsKey(villager.getUUID())) {
			player.sendOverlayMessage(Component.translatable("villagerchess.busy"));
			return;
		}
		endSession(byPlayer.get(player.getUUID()), null); // silently drop any previous game

		int level = villager.getVillagerData().level();
		ChessSession session = new ChessSession(nextGameId.getAndIncrement(), player.getUUID(), villager, level);
		byPlayer.put(player.getUUID(), session);
		byVillager.put(villager.getUUID(), session);

		player.closeContainer();
		ServerPlayNetworking.send(player, new ChessOpenS2C(session.gameId,
				villager.getDisplayName().getString(), level, session.board.toFen(), villager.getId()));
		VillagerChess.LOGGER.debug("chess game {} started: {} vs villager level {}", session.gameId,
				player.getGameProfile().name(), level);
	}

	public void handleMove(ServerPlayer player, ChessMoveC2S payload) {
		ChessSession session = byPlayer.get(player.getUUID());
		if (session == null || session.gameId != payload.gameId() || !session.isPlayersTurn()) return;

		int move = findLegalMove(session.board, payload.from(), payload.to(), payload.promotion());
		if (move == Move.NONE) return;

		applyMove(session, move);
		ChessOutcome outcome = currentOutcome(session.board);
		sendState(player, session, move, outcome);
		if (outcome.isOver()) {
			villagerReaction(session, outcome);
			endSession(session, null);
		} else {
			scheduleAiMove(session);
		}
	}

	public void handleResign(ServerPlayer player, int gameId) {
		ChessSession session = byPlayer.get(player.getUUID());
		if (session == null || session.gameId != gameId) return;
		villagerReaction(session, ChessOutcome.PLAYER_RESIGNED);
		endSession(session, ChessOutcome.PLAYER_RESIGNED);
	}

	public void onDisconnect(ServerPlayer player) {
		endSession(byPlayer.get(player.getUUID()), null);
	}

	/** Applies finished engine searches and periodically validates villager/player proximity. */
	public void tick(MinecraftServer server) {
		PendingAiMove pending;
		while ((pending = completedSearches.poll()) != null) {
			ChessSession session = byPlayer.get(pending.playerId());
			if (session == null || session.gameId != pending.gameId()
					|| session.movesMade != pending.expectedMovesMade()) {
				continue; // session ended or was superseded while the engine was thinking
			}
			session.aiThinking = false;
			ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId());
			if (player == null || pending.move() == Move.NONE) {
				endSession(session, null);
				continue;
			}
			applyMove(session, pending.move());
			ChessOutcome outcome = currentOutcome(session.board);
			sendState(player, session, pending.move(), outcome);
			if (outcome.isOver()) {
				villagerReaction(session, outcome);
				endSession(session, null);
			}
		}

		if (++tickCounter % VALIDATE_EVERY_TICKS != 0) return;
		for (ChessSession session : byPlayer.values().toArray(new ChessSession[0])) {
			ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
			if (player == null) {
				endSession(session, null);
			} else if (!session.villager.isAlive() || session.villager.isRemoved()
					|| session.villager.distanceToSqr(player) > ABANDON_DISTANCE_SQ) {
				notifyEnd(player, session, ChessOutcome.ABANDONED);
				endSession(session, null);
			}
		}
	}

	public void shutdown() {
		for (ChessSession session : byPlayer.values()) session.stop.set(true);
		byPlayer.clear();
		byVillager.clear();
		completedSearches.clear();
		if (engineExecutor != null) {
			engineExecutor.shutdownNow();
			engineExecutor = null;
		}
	}

	private void scheduleAiMove(ChessSession session) {
		session.aiThinking = true;
		Board snapshot = session.board.copy();
		int expectedMovesMade = session.movesMade;
		executor().submit(() -> {
			long start = System.nanoTime();
			int move = Engine.findMove(snapshot, session.profile, session.random, session.stop::get);
			// A little artificial thought makes instant replies feel alive.
			long thinkMs = MIN_THINK_MS + session.random.nextInt(EXTRA_THINK_MS);
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;
			if (elapsedMs < thinkMs && !session.stop.get()) {
				try {
					Thread.sleep(thinkMs - elapsedMs);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			if (!session.stop.get()) {
				completedSearches.add(new PendingAiMove(session.playerId, session.gameId, expectedMovesMade, move));
			}
		});
	}

	private void applyMove(ChessSession session, int move) {
		session.board.makeMove(move);
		session.movesMade++;
	}

	private void sendState(ServerPlayer player, ChessSession session, int lastMove, ChessOutcome outcome) {
		ServerPlayNetworking.send(player, new ChessStateS2C(session.gameId, session.board.toFen(),
				Move.from64(lastMove), Move.to64(lastMove), outcome.ordinal(),
				session.board.sideToMove() == Board.WHITE));
	}

	private void notifyEnd(ServerPlayer player, ChessSession session, ChessOutcome outcome) {
		ServerPlayNetworking.send(player, new ChessStateS2C(session.gameId, session.board.toFen(),
				-1, -1, outcome.ordinal(), false));
	}

	/** Ends a session; if {@code announce} is non-null the player is notified with that outcome. */
	private void endSession(ChessSession session, ChessOutcome announce) {
		if (session == null) return;
		session.stop.set(true);
		byPlayer.remove(session.playerId);
		byVillager.remove(session.villager.getUUID());
		if (announce != null) {
			ServerPlayer player = session.villager.level().getServer() != null
					? session.villager.level().getServer().getPlayerList().getPlayer(session.playerId)
					: null;
			if (player != null) notifyEnd(player, session, announce);
		}
	}

	private static void villagerReaction(ChessSession session, ChessOutcome outcome) {
		if (!session.villager.isAlive()) return;
		switch (outcome) {
			case PLAYER_WON -> session.villager.playSound(SoundEvents.VILLAGER_NO);
			case VILLAGER_WON, PLAYER_RESIGNED -> session.villager.playSound(SoundEvents.VILLAGER_CELEBRATE);
			case DRAW_STALEMATE, DRAW_FIFTY_MOVE, DRAW_REPETITION, DRAW_INSUFFICIENT_MATERIAL ->
					session.villager.playSound(SoundEvents.VILLAGER_YES);
			default -> {}
		}
	}

	private static int findLegalMove(Board board, int from, int to, int promotion) {
		for (int move : board.legalMoves()) {
			if (Move.from64(move) == from && Move.to64(move) == to && Move.promotion(move) == promotion) {
				return move;
			}
		}
		return Move.NONE;
	}

	private static ChessOutcome currentOutcome(Board board) {
		GameStatus status = board.status();
		return switch (status) {
			case ONGOING -> ChessOutcome.ONGOING;
			case CHECKMATE -> board.sideToMove() == Board.WHITE ? ChessOutcome.VILLAGER_WON : ChessOutcome.PLAYER_WON;
			case STALEMATE -> ChessOutcome.DRAW_STALEMATE;
			case DRAW_FIFTY_MOVE -> ChessOutcome.DRAW_FIFTY_MOVE;
			case DRAW_REPETITION -> ChessOutcome.DRAW_REPETITION;
			case DRAW_INSUFFICIENT_MATERIAL -> ChessOutcome.DRAW_INSUFFICIENT_MATERIAL;
		};
	}

	private ExecutorService executor() {
		if (engineExecutor == null) {
			engineExecutor = Executors.newSingleThreadExecutor(runnable -> {
				Thread thread = new Thread(runnable, "villagerchess-engine");
				thread.setDaemon(true);
				return thread;
			});
		}
		return engineExecutor;
	}
}
