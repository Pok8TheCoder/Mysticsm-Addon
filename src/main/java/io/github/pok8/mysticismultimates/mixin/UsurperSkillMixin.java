package io.github.pok8.mysticismultimates.mixin;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.unique.UsurperSkill;
import io.github.pok8.mysticismultimates.registry.ShadowEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UsurperSkill.class, remap = false)
public abstract class UsurperSkillMixin {
    private static double mysticism$origRobSuccess = -1.0;
    private static double mysticism$origRobSuccessMastered = -1.0;
    private static double mysticism$origCopySuccess = -1.0;
    private static double mysticism$origCopySuccessMastered = -1.0;

    @Inject(method = "onPressed", at = @At("HEAD"))
    private void mysticism$onPressedHead(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode, CallbackInfo ci) {
        if (entity != null && entity.hasEffect(ShadowEffects.GOLD_LUCK)) {
            try {
                java.lang.reflect.Field configField = UsurperSkill.class.getDeclaredField("CONFIG");
                configField.setAccessible(true);
                Object config = configField.get(null);
                if (config != null) {
                    java.lang.reflect.Field fRob = config.getClass().getField("robSuccess");
                    java.lang.reflect.Field fRobM = config.getClass().getField("robSuccessMastered");
                    java.lang.reflect.Field fCopy = config.getClass().getField("copySuccess");
                    java.lang.reflect.Field fCopyM = config.getClass().getField("copySuccessMastered");

                    mysticism$origRobSuccess = fRob.getDouble(config);
                    mysticism$origRobSuccessMastered = fRobM.getDouble(config);
                    mysticism$origCopySuccess = fCopy.getDouble(config);
                    mysticism$origCopySuccessMastered = fCopyM.getDouble(config);

                    fRob.setDouble(config, Math.min(100.0, mysticism$origRobSuccess + 25.0));
                    fRobM.setDouble(config, Math.min(100.0, mysticism$origRobSuccessMastered + 25.0));
                    fCopy.setDouble(config, Math.min(100.0, mysticism$origCopySuccess + 25.0));
                    fCopyM.setDouble(config, Math.min(100.0, mysticism$origCopySuccessMastered + 25.0));
                }
            } catch (Throwable ignored) {}
        }
    }

    @Inject(method = "onPressed", at = @At("RETURN"))
    private void mysticism$onPressedReturn(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode, CallbackInfo ci) {
        if (mysticism$origRobSuccess >= 0.0) {
            try {
                java.lang.reflect.Field configField = UsurperSkill.class.getDeclaredField("CONFIG");
                configField.setAccessible(true);
                Object config = configField.get(null);
                if (config != null) {
                    config.getClass().getField("robSuccess").setDouble(config, mysticism$origRobSuccess);
                    config.getClass().getField("robSuccessMastered").setDouble(config, mysticism$origRobSuccessMastered);
                    config.getClass().getField("copySuccess").setDouble(config, mysticism$origCopySuccess);
                    config.getClass().getField("copySuccessMastered").setDouble(config, mysticism$origCopySuccessMastered);
                }
            } catch (Throwable ignored) {}
            mysticism$origRobSuccess = -1.0;
            mysticism$origRobSuccessMastered = -1.0;
            mysticism$origCopySuccess = -1.0;
            mysticism$origCopySuccessMastered = -1.0;
        }
    }
}
