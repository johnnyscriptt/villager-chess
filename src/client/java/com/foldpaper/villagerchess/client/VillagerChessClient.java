package com.foldpaper.villagerchess.client;

import com.foldpaper.villagerchess.client.gui.ChessScreen;
import com.foldpaper.villagerchess.client.gui.ClientChessGame;
import com.foldpaper.villagerchess.net.ChessChallengeC2S;
import com.foldpaper.villagerchess.net.ChessOpenS2C;
import com.foldpaper.villagerchess.net.ChessStateS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;

public class VillagerChessClient implements ClientModInitializer {
	// The merchant GUI texture is 276x166 (see MerchantScreen's constructor).
	private static final int MERCHANT_GUI_WIDTH = 276, MERCHANT_GUI_HEIGHT = 166;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(ChessOpenS2C.TYPE, (payload, context) ->
				Minecraft.getInstance().gui.setScreen(new ChessScreen(new ClientChessGame(payload))));
		ClientPlayNetworking.registerGlobalReceiver(ChessStateS2C.TYPE, (payload, context) -> {
			if (Minecraft.getInstance().gui.screen() instanceof ChessScreen chess
					&& chess.gameId() == payload.gameId()) {
				chess.onServerState(payload);
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof MerchantScreen)) return;
			int x = scaledWidth / 2 + MERCHANT_GUI_WIDTH / 2 + 4;
			int y = (scaledHeight - MERCHANT_GUI_HEIGHT) / 2;
			Screens.getWidgets(screen).add(Button.builder(Component.translatable("villagerchess.play"),
					button -> ClientPlayNetworking.send(new ChessChallengeC2S()))
					.bounds(x, y, 70, 20)
					.build());
		});
	}
}
