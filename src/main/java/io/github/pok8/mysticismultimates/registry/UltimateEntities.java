package io.github.pok8.mysticismultimates.registry;

import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.entity.ShadowSoulEntity;
import io.github.pok8.mysticismultimates.entity.ShadowSubordinateEntity;
import io.github.pok8.mysticismultimates.entity.SkillAnimationEntity;
import io.github.pok8.mysticismultimates.entity.SkillAnimationVariant;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class UltimateEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MysticismUltimates.MOD_ID);
    private static final Map<SkillAnimationVariant, DeferredHolder<EntityType<?>, EntityType<SkillAnimationEntity>>> TYPES = new EnumMap<>(SkillAnimationVariant.class);

    static {
        for (SkillAnimationVariant variant : SkillAnimationVariant.values()) {
            TYPES.put(variant, ENTITIES.register(variant.id(), () -> EntityType.Builder.of(
                    (EntityType<SkillAnimationEntity> type, net.minecraft.world.level.Level level) -> new SkillAnimationEntity(type, level),
                    MobCategory.MISC)
                    .sized(variant.projectile() ? 0.8F : 1.5F, variant.projectile() ? 0.8F : 1.5F)
                    .clientTrackingRange(80)
                    .updateInterval(1)
                    .build(variant.id())));
        }
    }

    private UltimateEntities() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(UniqueShadowSkills.SHADOW_SOUL.get(), ShadowSoulEntity.createAttributes().build());
        event.put(UniqueShadowSkills.SHADOW_SUBORDINATE.get(), ShadowSubordinateEntity.createAttributes().build());
    }

    public static EntityType<SkillAnimationEntity> type(SkillAnimationVariant variant) {
        return TYPES.get(variant).get();
    }

    public static SkillAnimationVariant variant(EntityType<?> type) {
        for (Map.Entry<SkillAnimationVariant, DeferredHolder<EntityType<?>, EntityType<SkillAnimationEntity>>> entry : TYPES.entrySet()) {
            if (entry.getValue().get() == type) {
                return entry.getKey();
            }
        }
        return SkillAnimationVariant.ANTAEUS_BLUE;
    }
}
