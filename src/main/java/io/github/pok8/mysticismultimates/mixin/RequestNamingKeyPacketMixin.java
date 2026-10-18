package io.github.pok8.mysticismultimates.mixin;

import io.github.manasmods.tensura.network.c2s.RequestNamingKeyPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RequestNamingKeyPacket.class, remap = false)
public abstract class RequestNamingKeyPacketMixin {

    @Inject(method = "isNotNameable", at = @At("HEAD"), cancellable = true)
    private static void mysticism$allowNamingStripped(LivingEntity entity, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (entity != null && (entity.getTags().contains("otherworldly_stripped")
                || entity.getTags().contains("tensura:nameable")
                || entity.getTags().contains("nameable")
                || entity.getTags().contains("Nameable"))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isNotSubmitting", at = @At("HEAD"), cancellable = true)
    private static void mysticism$allowSubmitStripped(LivingEntity entity, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (entity != null && (entity.getTags().contains("otherworldly_stripped")
                || entity.getTags().contains("tensura:nameable")
                || entity.getTags().contains("nameable")
                || entity.getTags().contains("Nameable"))) {
            cir.setReturnValue(false);
        }
    }
}
