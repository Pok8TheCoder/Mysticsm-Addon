package io.github.pok8.mysticismultimates.effect;

import io.github.pok8.mysticismultimates.MysticismUltimates;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class LunarBoostEffect extends MobEffect {
    public LunarBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3A2A6A);
        addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "lunar_attack"),
                1.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "lunar_speed"),
                1.2,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
