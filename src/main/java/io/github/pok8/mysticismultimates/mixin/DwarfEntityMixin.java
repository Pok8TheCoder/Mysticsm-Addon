package io.github.pok8.mysticismultimates.mixin;

import io.github.manasmods.tensura.entity.human.DwarfEntity;
import io.github.pok8.mysticismultimates.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DwarfEntity.class, remap = false)
public abstract class DwarfEntityMixin {

    @Inject(method = "wantsToTrade", at = @At("HEAD"), cancellable = true)
    private void mysticism$wantsToTrade(CallbackInfoReturnable<Boolean> cir) {
        DwarfEntity dwarf = (DwarfEntity) (Object) this;
        if (dwarf.level() instanceof ServerLevel serverLevel) {
            LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(dwarf.chunkPosition().x, dwarf.chunkPosition().z);
            if (chunk != null) {
                long customTime = ModAttachments.getChunkTime(chunk);
                if (customTime >= 0L && customTime < 12000L) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "startSleeping", at = @At("HEAD"), cancellable = true)
    private void mysticism$onStartSleeping(BlockPos pos, CallbackInfo ci) {
        DwarfEntity dwarf = (DwarfEntity) (Object) this;
        if (dwarf.level() instanceof ServerLevel serverLevel) {
            LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(dwarf.chunkPosition().x, dwarf.chunkPosition().z);
            if (chunk != null) {
                long customTime = ModAttachments.getChunkTime(chunk);
                if (customTime >= 0L && customTime < 12000L) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "updateSpecialPrices", at = @At("RETURN"), remap = false)
    private void mysticism$preventUpcharge(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        mysticism$preventMerchantUpcharge((DwarfEntity) (Object) this);
    }

    @Inject(method = "rewardTradeXp", at = @At("RETURN"), remap = false)
    private void mysticism$preventUpchargeAfterTrade(net.minecraft.world.item.trading.MerchantOffer offer, CallbackInfo ci) {
        mysticism$preventMerchantUpcharge((DwarfEntity) (Object) this);
    }

    private static void mysticism$preventMerchantUpcharge(DwarfEntity dwarf) {
        if (dwarf == null || !dwarf.getTags().contains("OLucked")) {
            return;
        }
        for (net.minecraft.world.item.trading.MerchantOffer offer : dwarf.getOffers()) {
            // Can never upcharge (specialPriceDiff > 0), but can still low charge (specialPriceDiff < 0)
            if (offer.getSpecialPriceDiff() > 0) {
                offer.setSpecialPriceDiff(0);
            }
            // Can never build positive demand from higher sale patterns
            if (offer.getDemand() > 0) {
                try {
                    java.lang.reflect.Field f = net.minecraft.world.item.trading.MerchantOffer.class.getDeclaredField("demand");
                    f.setAccessible(true);
                    f.setInt(offer, 0);
                } catch (Throwable ignored) {}
            }
        }
    }
}
