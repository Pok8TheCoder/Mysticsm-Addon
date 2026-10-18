package io.github.pok8.mysticismultimates.mixin;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.entity.magic.beam.PredatorMistProjectile;
import io.github.pok8.mysticismultimates.registry.UltimateSkills;
import io.github.pok8.mysticismultimates.skill.BeelzebubSkill;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gluttony mist reads predationList as a compound. Older Beelzebub saves stored a string list there.
 */
@Mixin(value = PredatorMistProjectile.class, remap = false)
public class PredatorMistProjectileMixin {
    @Inject(method = "devourEP", at = @At("HEAD"))
    private void mysticism$repairPredationList(LivingEntity target, LivingEntity owner, float amountToMax, CallbackInfo ci) {
        ManasSkill beelzebub = UltimateSkills.BEELZEBUB.get();
        if (beelzebub == null || owner == null || !SkillUtils.hasSkill(owner, beelzebub)) {
            return;
        }
        ManasSkillInstance instance = SkillUtils.getSkillOrNull(owner, beelzebub);
        if (instance != null) {
            BeelzebubSkill.repairPredationList(instance);
        }
    }
}
