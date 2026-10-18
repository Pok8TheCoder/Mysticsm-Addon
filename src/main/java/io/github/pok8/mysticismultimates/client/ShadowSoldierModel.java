package io.github.pok8.mysticismultimates.client;

import io.github.pok8.mysticismultimates.entity.ShadowSubordinateEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ShadowSoldierModel extends GeoModel<ShadowSubordinateEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("mysticism", "geo/shadow_warrior.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mysticism", "textures/entity/monster/shadow/shadow_warrior.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("mysticism", "animations/shadow_warrior.animation.json");

    @Override
    public ResourceLocation getModelResource(ShadowSubordinateEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ShadowSubordinateEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ShadowSubordinateEntity entity) {
        return ANIMATION;
    }
}
