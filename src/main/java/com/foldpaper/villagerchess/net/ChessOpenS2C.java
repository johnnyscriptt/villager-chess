package com.foldpaper.villagerchess.net;

import com.foldpaper.villagerchess.VillagerChess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Opens the chess screen. The player always plays white; board state travels as FEN.
 * villagerEntityId lets the client render the actual villager beside the board.
 */
public record ChessOpenS2C(int gameId, String villagerName, int villagerLevel, String fen,
		int villagerEntityId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ChessOpenS2C> TYPE =
			new CustomPacketPayload.Type<>(VillagerChess.id("open"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ChessOpenS2C> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ChessOpenS2C::gameId,
			ByteBufCodecs.STRING_UTF8, ChessOpenS2C::villagerName,
			ByteBufCodecs.VAR_INT, ChessOpenS2C::villagerLevel,
			ByteBufCodecs.STRING_UTF8, ChessOpenS2C::fen,
			ByteBufCodecs.VAR_INT, ChessOpenS2C::villagerEntityId,
			ChessOpenS2C::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
