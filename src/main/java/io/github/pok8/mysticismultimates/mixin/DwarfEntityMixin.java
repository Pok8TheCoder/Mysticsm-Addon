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
        DwarfEntity dwarf = (DwarfEntity) (Object) this;
        mysticism$applyVillagerHat(dwarf, player);
        mysticism$preventMerchantUpcharge(dwarf);
    }

    @Inject(method = "rewardTradeXp", at = @At("RETURN"), remap = false)
    private void mysticism$preventUpchargeAfterTrade(net.minecraft.world.item.trading.MerchantOffer offer, CallbackInfo ci) {
        mysticism$preventMerchantUpcharge((DwarfEntity) (Object) this);
    }

    private static void mysticism$applyVillagerHat(DwarfEntity dwarf, net.minecraft.world.entity.player.Player player) {
        if (dwarf == null || player == null || dwarf.getOffers() == null) {
            return;
        }
        try {
            var holder = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE
                    .getHolder(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("artifacts", "villager_reputation"));
            if (holder.isPresent()) {
                var instance = player.getAttribute(holder.get());
                if (instance != null) {
                    double bonus = instance.getValue();
                    if (bonus > 0.0) {
                        double discountPct = DwarfEntity.REPUTATION_CONFIG != null ? DwarfEntity.REPUTATION_CONFIG.discountPercentage : 0.005;
                        double factor = bonus * discountPct;
                        for (net.minecraft.world.item.trading.MerchantOffer offer : dwarf.getOffers()) {
                            if (offer.getPriceMultiplier() > 0.0f) {
                                int discount = (int) Math.floor(factor * offer.getBaseCostA().getCount());
                                if (discount > 0) {
                                    offer.addToSpecialPriceDiff(-discount);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void mysticism$preventMerchantUpcharge(DwarfEntity dwarf) {
        if (dwarf == null || !dwarf.getTags().contains("OLucked") || dwarf.getOffers() == null) {
            return;
        }
        io.github.pok8.mysticismultimates.skill.GoldLuckHandler.optimizeMerchant(dwarf);
    }
}
