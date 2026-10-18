package io.github.pok8.mysticismultimates.effect;

import io.github.pok8.mysticismultimates.MysticismUltimates;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class GoldLuckEffect extends MobEffect {
    public GoldLuckEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
        addAttributeModifier(
                Attributes.LUCK,
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "gold_luck_attribute"),
                5.0,
                AttributeModifier.Operation.ADD_VALUE
        );
    }
}
