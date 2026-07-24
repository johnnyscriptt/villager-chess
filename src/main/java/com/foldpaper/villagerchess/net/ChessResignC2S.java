package com.foldpaper.villagerchess.net;

import com.foldpaper.villagerchess.VillagerChess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChessResignC2S(int gameId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ChessResignC2S> TYPE =
			new CustomPacketPayload.Type<>(VillagerChess.id("resign"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ChessResignC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ChessResignC2S::gameId,
			ChessResignC2S::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
