package io.github.pok8.mysticismultimates.registry;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.impl.SkillRegistry;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.entity.ShadowSoulEntity;
import io.github.pok8.mysticismultimates.entity.ShadowSubordinateEntity;
import io.github.pok8.mysticismultimates.skill.ShadowUniqueSkill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class UniqueShadowSkills {
    public static DeferredHolder<EntityType<?>, EntityType<ShadowSoulEntity>> SHADOW_SOUL;
    public static DeferredHolder<EntityType<?>, EntityType<ShadowSubordinateEntity>> SHADOW_SUBORDINATE;
    public static dev.architectury.registry.registries.RegistrySupplier<ManasSkill> PLUNDERER;
    public static dev.architectury.registry.registries.RegistrySupplier<ManasSkill> VAINGLORY;

    private UniqueShadowSkills() {
    }

    public static void registerEntities() {
        SHADOW_SOUL = UltimateEntities.ENTITIES.register("shadow_soul", () -> EntityType.Builder
                .of(ShadowSoulEntity::new, MobCategory.MISC)
                .sized(0.6F, 0.6F)
                .clientTrackingRange(64)
                .build("shadow_soul"));
        SHADOW_SUBORDINATE = UltimateEntities.ENTITIES.register("shadow_subordinate", () -> EntityType.Builder
                .of(ShadowSubordinateEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .build("shadow_subordinate"));
    }

    public static void registerSkills() {
        PLUNDERER = SkillRegistry.SKILLS.register(
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "plunderer"),
                () -> new ShadowUniqueSkill("plunderer", false));
        VAINGLORY = SkillRegistry.SKILLS.register(
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "vainglory"),
                () -> new ShadowUniqueSkill("vainglory", true));
    }
}
