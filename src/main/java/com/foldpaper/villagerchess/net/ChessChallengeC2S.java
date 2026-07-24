package com.foldpaper.villagerchess.net;

import com.foldpaper.villagerchess.VillagerChess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Player clicked "Play Chess" in the trade screen; the server resolves the villager from the open menu. */
public record ChessChallengeC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ChessChallengeC2S> TYPE =
			new CustomPacketPayload.Type<>(VillagerChess.id("challenge"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ChessChallengeC2S> CODEC =
			StreamCodec.unit(new ChessChallengeC2S());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
