package io.github.pok8.mysticismultimates.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import io.github.Memoires.mysticism.registry.skill.UniqueMysticismSkills;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.impl.SkillRegistry;
import io.github.manasmods.tensura.registry.skill.CommonSkills;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.UltimateEvolution;
import io.github.pok8.mysticismultimates.skill.BeelzebubSkill;
import io.github.pok8.mysticismultimates.skill.DifficultUltimateSkill;
import io.github.pok8.mysticismultimates.skill.HadesSkill;
import io.github.pok8.mysticismultimates.skill.LavernaSkill;
import io.github.pok8.mysticismultimates.skill.TsukuyomiSkill;
import io.github.pok8.mysticismultimates.skill.UrielSkill;
import io.github.pok8.mysticismultimates.skill.VicielSkill;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class UltimateSkills {
    // --- ACTIVE ULTIMATE SKILLS ---
    public static RegistrySupplier<ManasSkill> ANTAEUS;
    public static RegistrySupplier<ManasSkill> BEELZEBUB;
    public static RegistrySupplier<ManasSkill> GILGAMESH;
    public static RegistrySupplier<ManasSkill> HADES;
    public static RegistrySupplier<ManasSkill> LAVERNA;
    public static RegistrySupplier<ManasSkill> TSUKUYOMI;
    public static RegistrySupplier<ManasSkill> URIEL;
    public static RegistrySupplier<ManasSkill> VICIEL;

    // --- DISABLED CATEGORY 3 PLACEHOLDER SKILLS (Pending faithful ports from 1.19.2) ---
    public static RegistrySupplier<ManasSkill> AMATERASU;
    public static RegistrySupplier<ManasSkill> AME_NO_UZUME;
    public static RegistrySupplier<ManasSkill> ANTITHESIS;
    public static RegistrySupplier<ManasSkill> ASMODEUS;
    public static RegistrySupplier<ManasSkill> BELPHEGOR;
    public static RegistrySupplier<ManasSkill> BUSHYASTA;
    public static RegistrySupplier<ManasSkill> DIONYSUS;
    public static RegistrySupplier<ManasSkill> GALILEO;
    public static RegistrySupplier<ManasSkill> IGNIS;
    public static RegistrySupplier<ManasSkill> INVICTUS;
    public static RegistrySupplier<ManasSkill> LUCIFER;
    public static RegistrySupplier<ManasSkill> MEPHISTO;
    public static RegistrySupplier<ManasSkill> MICHAEL;
    public static RegistrySupplier<ManasSkill> OIZYS;
    public static RegistrySupplier<ManasSkill> PERNIDA;
    public static RegistrySupplier<ManasSkill> SANDALPHON;
    public static RegistrySupplier<ManasSkill> SARIEL;
    public static RegistrySupplier<ManasSkill> SATANAEL;
    public static RegistrySupplier<ManasSkill> SEPHIROT;
    public static RegistrySupplier<ManasSkill> TAKEMIKAZUCHI;
    public static RegistrySupplier<ManasSkill> XEZBETH;
    public static RegistrySupplier<ManasSkill> DEMETER;

    private UltimateSkills() {
    }

    public static void register() {
        // Active fully-developed skills
        ANTAEUS = skill("antaeus", "oppress", "attract_blue", "repel_red", "hollow_purple", "bleve", "flicker");
        BEELZEBUB = skill("beelzebub", "predation", "stomach", "provide", "receive", "isolation", "corrosion", "substitution");
        GILGAMESH = skill("gilgamesh", "territory", "ea", "axe", "enuma_elish");
        HADES = SkillRegistry.SKILLS.register(ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "hades"), HadesSkill::new);
        LAVERNA = skill("laverna", "blitz", "shadow_storage", "shadow_merge", "euclidean_dystopia", "shadow_garden", "shadow_pit", "army_summon");
        TSUKUYOMI = skill("tsukuyomi", "eye_of_the_moon", "insta_kill", "lightspeed_acceleration", "parallel_existence");
        URIEL = SkillRegistry.SKILLS.register(ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "uriel"), UrielSkill::new);
        VICIEL = skill("viciel", "true_copy", "true_paste", "darkness_conscription", "petrichor_stained_destiny", "shadow_jump");

        // Category 3 placeholder skills disabled:
        // AMATERASU, AME_NO_UZUME, ANTITHESIS, ASMODEUS, BELPHEGOR, BUSHYASTA, DIONYSUS, GALILEO,
        // IGNIS, INVICTUS, LUCIFER, MEPHISTO, MICHAEL, OIZYS, PERNIDA, SANDALPHON, SARIEL,
        // SATANAEL, SEPHIROT, TAKEMIKAZUCHI, XEZBETH, DEMETER

        UltimateEvolution.bind(List.of(
                req(ANTAEUS, 600_000, UniqueSkills.OPPRESSOR::get, ExtraSkills.GRAVITY_DOMINATION::get, CommonSkills.GRAVITY_FIELD::get, CommonSkills.RANGED_BARRIER::get),
                req(BEELZEBUB, 500_000, UniqueSkills.GOURMET::get, UniqueSkills.GUARDIAN::get, ExtraSkills.MULTILAYER_BARRIER::get, CommonSkills.CORROSION::get),
                req(GILGAMESH, 750_000, UniqueMysticismSkills.GATEKEEPER::get),
                reqMulti(HADES, 600_000, List.of(UniqueSkills.REAPER::get, UniqueSkills.MERCILESS::get), CommonSkills.POISON::get, CommonSkills.CORROSION::get, CommonSkills.PARALYSIS::get),
                req(LAVERNA, 1_000_000, () -> UniqueShadowSkills.VAINGLORY.get(), () -> io.github.Memoires.mysticism.registry.skill.ExtraMysticismSkills.DARKNESS_DOMINATION.get()),
                req(TSUKUYOMI, 300_000, UniqueSkills.SHADOW_STRIKER::get, ExtraSkills.BODY_DOUBLE::get),
                req(URIEL, 1_000_000, UniqueSkills.INFINITY_PRISON::get),
                req(VICIEL, 666_666, UniqueMysticismSkills.HIDDEN_RULER::get)
        ));
    }

    private static RegistrySupplier<ManasSkill> skill(String id, String... modes) {
        return SkillRegistry.SKILLS.register(
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, id),
                () -> switch (id) {
                    case "beelzebub" -> new BeelzebubSkill(id, modes);
                    case "laverna" -> new LavernaSkill(modes);
                    case "tsukuyomi" -> new TsukuyomiSkill(modes);
                    case "viciel" -> new VicielSkill(modes);
                    default -> new DifficultUltimateSkill(id, modes);
                });
    }

    @SafeVarargs
    private static UltimateEvolution.Requirement req(
            Supplier<? extends ManasSkill> ultimate,
            double epCost,
            Supplier<? extends ManasSkill> unique,
            Supplier<? extends ManasSkill>... extras) {
        List<Supplier<? extends ManasSkill>> mastered = new ArrayList<>();
        mastered.add(unique);
        mastered.addAll(List.of(extras));
        return new UltimateEvolution.Requirement(ultimate, epCost, List.of(unique), List.copyOf(mastered));
    }

    @SafeVarargs
    private static UltimateEvolution.Requirement reqMulti(
            Supplier<? extends ManasSkill> ultimate,
            double epCost,
            List<Supplier<? extends ManasSkill>> uniques,
            Supplier<? extends ManasSkill>... extras) {
        List<Supplier<? extends ManasSkill>> mastered = new ArrayList<>(uniques);
        mastered.addAll(List.of(extras));
        return new UltimateEvolution.Requirement(ultimate, epCost, uniques, List.copyOf(mastered));
    }
}
