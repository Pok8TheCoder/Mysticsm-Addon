package io.github.pok8.mysticismultimates.mixin;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.unique.GreatSageSkill;
import io.github.pok8.mysticismultimates.registry.ShadowEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GreatSageSkill.class, remap = false)
public abstract class GreatSageSkillMixin {
    private static float mysticism$origCopyChance = -1.0f;
    private static float mysticism$origCopyChanceMastered = -1.0f;

    @Inject(method = "onPressed", at = @At("HEAD"))
    private void mysticism$onPressedHead(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode, CallbackInfo ci) {
        if (entity != null && entity.hasEffect(ShadowEffects.GOLD_LUCK)) {
            try {
                java.lang.reflect.Field configField = GreatSageSkill.class.getDeclaredField("CONFIG");
                configField.setAccessible(true);
                Object config = configField.get(null);
                if (config != null) {
                    java.lang.reflect.Field fChance = config.getClass().getField("copyChance");
                    java.lang.reflect.Field fChanceM = config.getClass().getField("copyChanceMastered");

                    mysticism$origCopyChance = fChance.getFloat(config);
                    mysticism$origCopyChanceMastered = fChanceM.getFloat(config);

                    fChance.setFloat(config, Math.min(100.0f, mysticism$origCopyChance + 25.0f));
                    fChanceM.setFloat(config, Math.min(100.0f, mysticism$origCopyChanceMastered + 25.0f));
                }
            } catch (Throwable ignored) {}
        }
    }

    @Inject(method = "onPressed", at = @At("RETURN"))
    private void mysticism$onPressedReturn(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode, CallbackInfo ci) {
        if (mysticism$origCopyChance >= 0.0f) {
            try {
                java.lang.reflect.Field configField = GreatSageSkill.class.getDeclaredField("CONFIG");
                configField.setAccessible(true);
                Object config = configField.get(null);
                if (config != null) {
                    config.getClass().getField("copyChance").setFloat(config, mysticism$origCopyChance);
                    config.getClass().getField("copyChanceMastered").setFloat(config, mysticism$origCopyChanceMastered);
                }
            } catch (Throwable ignored) {}
            mysticism$origCopyChance = -1.0f;
            mysticism$origCopyChanceMastered = -1.0f;
        }
    }
}
