package io.github.pok8.mysticismultimates.mixin;

import io.github.pok8.mysticismultimates.skill.GoldLuckHandler;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerMixin {

    @Inject(method = "updateSpecialPrices", at = @At("RETURN"))
    private void mysticism$onUpdateSpecialPrices(Player player, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        if (villager.getTags().contains("OLucked")) {
            GoldLuckHandler.optimizeMerchant(villager);
        }
    }
}
