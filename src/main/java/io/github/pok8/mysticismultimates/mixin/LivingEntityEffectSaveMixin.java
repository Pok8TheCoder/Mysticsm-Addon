package io.github.pok8.mysticismultimates.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Tensura can apply an effect through a holder that is not in the mob-effect registry.
 * Saving that holder throws "Unregistered holder" and crashes the world.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectSaveMixin {
    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void mysticism$dropUnregisteredEffects(CompoundTag tag, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        List<MobEffectInstance> active = new ArrayList<>(self.getActiveEffects());
        for (MobEffectInstance instance : active) {
            if (saves(instance)) {
                continue;
            }
            Holder<MobEffect> current = instance.getEffect();
            self.removeEffect(current);
            Holder<MobEffect> registered = current.unwrapKey()
                    .flatMap(BuiltInRegistries.MOB_EFFECT::getHolder)
                    .map(holder -> (Holder<MobEffect>) holder)
                    .orElse(null);
            if (registered == null) {
                continue;
            }
            MobEffectInstance replacement = new MobEffectInstance(
                    registered,
                    instance.getDuration(),
                    instance.getAmplifier(),
                    instance.isAmbient(),
                    instance.isVisible(),
                    instance.showIcon());
            if (saves(replacement)) {
                self.addEffect(replacement);
            }
        }
    }

    private static boolean saves(MobEffectInstance instance) {
        try {
            instance.save();
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
