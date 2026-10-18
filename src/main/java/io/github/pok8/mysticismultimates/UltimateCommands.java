package io.github.pok8.mysticismultimates;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import io.github.pok8.mysticismultimates.registry.ModAttachments;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class UltimateCommands {
    private UltimateCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("setchunktime")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("time", LongArgumentType.longArg(-1L, 24000L))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            long time = LongArgumentType.getLong(ctx, "time");
                            LevelChunk chunk = player.serverLevel().getChunkAt(player.blockPosition());
                            ModAttachments.setChunkTime(chunk, time);
                            chunk.setUnsaved(true);

                            // Immediately update the player's sky
                            player.connection.send(new ClientboundSetTimePacket(
                                    player.level().getGameTime(),
                                    time >= 0 ? time : player.level().getDayTime(),
                                    time < 0 ? player.level().getGameRules().getBoolean(GameRules.RULE_DAYLIGHT) : false
                            ));
                            ctx.getSource().sendSuccess(() -> Component.literal("Set chunk [" + chunk.getPos().x + ", " + chunk.getPos().z + "] time to " + time), true);
                            return 1;
                        })
                )
        );

        event.getDispatcher().register(Commands.literal("mysticismultimates")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("soulquality")
                        .then(Commands.literal("get")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> {
                                            int found = 0;
                                            for (ServerPlayer player : EntityArgument.getPlayers(context, "targets")) {
                                                UltimateSlotStorage storage = UltimateSlotStorage.get(player);
                                                int quality = storage == null ? 0 : storage.soulQuality();
                                                int used = storage == null ? 0 : storage.currentUltimates();
                                                context.getSource().sendSuccess(() -> Component.translatable(
                                                        "mysticism_ultimates.command.soul_quality.get",
                                                        player.getDisplayName(), quality, used), false);
                                                found++;
                                            }
                                            return found;
                                        })))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            int changed = 0;
                                            for (ServerPlayer player : EntityArgument.getPlayers(context, "targets")) {
                                                UltimateSlotStorage storage = UltimateSlotStorage.get(player);
                                                if (storage == null) {
                                                    continue;
                                                }
                                                storage.setSoulQuality(amount);
                                                context.getSource().sendSuccess(() -> Component.translatable(
                                                        "mysticism_ultimates.command.soul_quality.set",
                                                        player.getDisplayName(), amount), true);
                                                changed++;
                                            }
                                            return changed;
                                        })))));
    }
}
