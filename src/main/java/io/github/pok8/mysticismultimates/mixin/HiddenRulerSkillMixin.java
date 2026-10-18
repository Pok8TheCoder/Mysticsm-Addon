package io.github.pok8.mysticismultimates.mixin;

import io.github.Memoires.mysticism.ability.skill.unique.HiddenRulerSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.pok8.mysticismultimates.skill.HiddenRulerHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HiddenRulerSkill.class, remap = false)
public abstract class HiddenRulerSkillMixin {
    @Inject(method = "copy", at = @At("HEAD"), cancellable = true)
    private void mysticismUltimates$copy(ManasSkillInstance instance, LivingEntity entity, int mode, LivingEntity target, CompoundTag tag, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) {
            if (HiddenRulerHandler.onCopy(player, instance, mode, target, tag)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "paste", at = @At("HEAD"), cancellable = true)
    private void mysticismUltimates$paste(ManasSkillInstance instance, LivingEntity entity, int mode, LivingEntity target, CompoundTag tag, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) {
            if (HiddenRulerHandler.onPaste(player, instance, mode, target, tag)) {
                ci.cancel();
            }
        }
    }
}
