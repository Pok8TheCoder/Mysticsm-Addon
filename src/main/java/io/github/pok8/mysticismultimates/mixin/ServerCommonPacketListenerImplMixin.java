package io.github.pok8.mysticismultimates.mixin;

import io.github.pok8.mysticismultimates.skill.TimeSkills;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {

    @Shadow
    protected Connection connection;

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mysticism$onSend1(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundSetTimePacket timePacket && (Object) this instanceof ServerGamePacketListenerImpl gameHandler) {
            ServerPlayer player = gameHandler.player;
            if (player != null) {
                Long customTime = TimeSkills.getEffectiveTimeForPlayer(player);
                if (customTime != null && customTime >= 0L) {
                    long packetDayTime = timePacket.getDayTime();
                    if (packetDayTime != -customTime) {
                        ci.cancel();
                        this.connection.send(new ClientboundSetTimePacket(timePacket.getGameTime(), customTime, false));
                    }
                }
            }
        }
    }

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mysticism$onSend2(Packet<?> packet, PacketSendListener listener, CallbackInfo ci) {
        if (packet instanceof ClientboundSetTimePacket timePacket && (Object) this instanceof ServerGamePacketListenerImpl gameHandler) {
            ServerPlayer player = gameHandler.player;
            if (player != null) {
                Long customTime = TimeSkills.getEffectiveTimeForPlayer(player);
                if (customTime != null && customTime >= 0L) {
                    long packetDayTime = timePacket.getDayTime();
                    if (packetDayTime != -customTime) {
                        ci.cancel();
                        this.connection.send(new ClientboundSetTimePacket(timePacket.getGameTime(), customTime, false), listener);
                    }
                }
            }
        }
    }
}
