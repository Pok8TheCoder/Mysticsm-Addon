package io.github.pok8.mysticismultimates.effect;

import io.github.pok8.mysticismultimates.MysticismUltimates;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SolarBoostEffect extends MobEffect {
    public SolarBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF6C445);
        addAttributeModifier(
                Attributes.ARMOR,
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "solar_armor"),
                20.0,
                AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(
                Attributes.ARMOR_TOUGHNESS,
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "solar_toughness"),
                20.0,
                AttributeModifier.Operation.ADD_VALUE);
    }
}
