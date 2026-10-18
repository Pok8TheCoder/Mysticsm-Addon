package io.github.pok8.mysticismultimates.mixin;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.entity.magic.beam.PredatorMistProjectile;
import io.github.pok8.mysticismultimates.registry.ShadowEffects;
import io.github.pok8.mysticismultimates.registry.UltimateSkills;
import io.github.pok8.mysticismultimates.skill.BeelzebubSkill;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gluttony mist reads predationList as a compound. Older Beelzebub saves stored a string list there.
 * Also boosts predation chance by flat 25% when Gold Luck is active.
 */
@Mixin(value = PredatorMistProjectile.class, remap = false)
public class PredatorMistProjectileMixin {
    private static float mysticism$origPredationChance = -1.0f;

    @Inject(method = "devourEP", at = @At("HEAD"))
    private void mysticism$repairPredationList(LivingEntity target, LivingEntity owner, float amountToMax, CallbackInfo ci) {
        ManasSkill beelzebub = UltimateSkills.BEELZEBUB.get();
        if (beelzebub != null && owner != null && SkillUtils.hasSkill(owner, beelzebub)) {
            ManasSkillInstance instance = SkillUtils.getSkillOrNull(owner, beelzebub);
            if (instance != null) {
                BeelzebubSkill.repairPredationList(instance);
            }
        }

        // Gold Luck: boost predation chance by flat 25% (2.5 / 10.0 = +25%)
        if (owner != null && owner.hasEffect(ShadowEffects.GOLD_LUCK)) {
            try {
                mysticism$origPredationChance = io.github.manasmods.tensura.ability.skill.unique.PredatorSkill.CONFIG.predationSkillChance;
                io.github.manasmods.tensura.ability.skill.unique.PredatorSkill.CONFIG.predationSkillChance = Math.min(10.0f, mysticism$origPredationChance + 2.5f);
            } catch (Throwable ignored) {}
        }
    }

    @Inject(method = "devourEP", at = @At("RETURN"))
    private void mysticism$restorePredationChance(LivingEntity target, LivingEntity owner, float amountToMax, CallbackInfo ci) {
        if (mysticism$origPredationChance >= 0.0f) {
            try {
                io.github.manasmods.tensura.ability.skill.unique.PredatorSkill.CONFIG.predationSkillChance = mysticism$origPredationChance;
            } catch (Throwable ignored) {}
            mysticism$origPredationChance = -1.0f;
        }
    }
}
