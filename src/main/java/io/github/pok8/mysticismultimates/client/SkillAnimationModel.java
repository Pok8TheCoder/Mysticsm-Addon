package io.github.pok8.mysticismultimates.client;

import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.entity.SkillAnimationEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SkillAnimationModel extends GeoModel<SkillAnimationEntity> {
    @Override
    public ResourceLocation getModelResource(SkillAnimationEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, entity.variant().model());
    }

    @Override
    public ResourceLocation getTextureResource(SkillAnimationEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, entity.variant().texture());
    }

    @Override
    public ResourceLocation getAnimationResource(SkillAnimationEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, entity.variant().animationFile());
    }
}
