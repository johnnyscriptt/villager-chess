package com.foldpaper.villagerchess.net;

import com.foldpaper.villagerchess.VillagerChess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A move attempt: squares are 0-63 (a1 = 0), promotion is 0 or a Board piece type. */
public record ChessMoveC2S(int gameId, int from, int to, int promotion) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ChessMoveC2S> TYPE =
			new CustomPacketPayload.Type<>(VillagerChess.id("move"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ChessMoveC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ChessMoveC2S::gameId,
			ByteBufCodecs.VAR_INT, ChessMoveC2S::from,
			ByteBufCodecs.VAR_INT, ChessMoveC2S::to,
			ByteBufCodecs.VAR_INT, ChessMoveC2S::promotion,
			ChessMoveC2S::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
