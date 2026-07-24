package com.foldpaper.villagerchess;

import com.foldpaper.villagerchess.game.ChessSessionManager;
import com.foldpaper.villagerchess.net.ChessChallengeC2S;
import com.foldpaper.villagerchess.net.ChessMoveC2S;
import com.foldpaper.villagerchess.net.ChessOpenS2C;
import com.foldpaper.villagerchess.net.ChessResignC2S;
import com.foldpaper.villagerchess.net.ChessStateS2C;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerChess implements ModInitializer {
	public static final String MOD_ID = "villagerchess";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(ChessChallengeC2S.TYPE, ChessChallengeC2S.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChessMoveC2S.TYPE, ChessMoveC2S.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChessResignC2S.TYPE, ChessResignC2S.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ChessOpenS2C.TYPE, ChessOpenS2C.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ChessStateS2C.TYPE, ChessStateS2C.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ChessChallengeC2S.TYPE,
				(payload, context) -> ChessSessionManager.INSTANCE.startGame(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(ChessMoveC2S.TYPE,
				(payload, context) -> ChessSessionManager.INSTANCE.handleMove(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(ChessResignC2S.TYPE,
				(payload, context) -> ChessSessionManager.INSTANCE.handleResign(context.player(), payload.gameId()));

		ServerTickEvents.END_SERVER_TICK.register(ChessSessionManager.INSTANCE::tick);
		ServerPlayConnectionEvents.DISCONNECT.register(
				(handler, server) -> ChessSessionManager.INSTANCE.onDisconnect(handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ChessSessionManager.INSTANCE.shutdown());

		LOGGER.info("Villager Chess initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
