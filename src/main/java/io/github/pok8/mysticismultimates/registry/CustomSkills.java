package io.github.pok8.mysticismultimates.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.impl.SkillRegistry;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.skill.OtherworldlySkill;
import io.github.pok8.mysticismultimates.skill.TimeSkills;
import net.minecraft.resources.ResourceLocation;

public final class CustomSkills {
    public static RegistrySupplier<ManasSkill> OTHERWORLDLY;
    public static RegistrySupplier<ManasSkill> TRUE_DAYLIGHT;
    public static RegistrySupplier<ManasSkill> TRUE_NIGHTLIGHT;

    private CustomSkills() {
    }

    public static void registerSkills() {
        OTHERWORLDLY = SkillRegistry.SKILLS.register(
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "otherworldly"),
                OtherworldlySkill::new);
        TRUE_DAYLIGHT = SkillRegistry.SKILLS.register(
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "true_daylight"),
                TimeSkills.TrueDaylightSkill::new);
        TRUE_NIGHTLIGHT = SkillRegistry.SKILLS.register(
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "true_nightlight"),
                TimeSkills.TrueNightlightSkill::new);
    }
}
