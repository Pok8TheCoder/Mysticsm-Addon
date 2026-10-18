package io.github.pok8.mysticismultimates;

import io.github.Memoires.mysticism.registry.skill.ExtraMysticismSkills;
import io.github.pok8.mysticismultimates.registry.UniqueShadowSkills;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class UltimateEvolution {
    private static final int[] SOUL_WEIGHTS = {10, 30, 25, 15, 5, 3, 2, 1};
    private static final List<Requirement> REQUIREMENTS = new ArrayList<>();

    private UltimateEvolution() {
    }

    public static void bind(List<Requirement> requirements) {
        REQUIREMENTS.clear();
        REQUIREMENTS.addAll(requirements);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        // Continuously record visited dimensions for Otherworldly Shared Secrets requirement
        io.github.pok8.mysticismultimates.skill.OtherworldlySkill.recordDimension(player, player.serverLevel().dimension().location());

        GameRules rules = player.serverLevel().getGameRules();
        if (!rules.getBoolean(UltimateGamerules.ENABLE_ACQUISITION)) {
            return;
        }

        // Unique evolution: Plunderer -> Vainglory (Requires Mastered Plunderer + Mastered Darkness Manipulation)
        checkUniqueEvolutions(player, rules);

        UltimateSlotStorage slots = UltimateSlotStorage.get(player);
        if (slots == null) {
            return;
        }
        if (rules.getBoolean(UltimateGamerules.SOUL_QUALITY_MODE)) {
            rollSoulQuality(player, slots);
            if (slots.currentUltimates() >= slots.soulQuality()) {
                return;
            }
        }

        // ANY and ALL Ultimates strictly require True Demon Lord or True Hero awakening
        ExistenceStorage existence = existence(player);
        if (existence == null || (!existence.isTrueDemonLord() && !existence.isTrueHero())) {
            return;
        }
        boolean loseUnique = rules.getBoolean(UltimateGamerules.LOSE_UNIQUE);
        for (Requirement requirement : REQUIREMENTS) {
            ManasSkill ultimate = requirement.ultimate().get();
            if (ultimate == null || SkillUtils.hasSkill(player, ultimate) || !mastered(player, requirement)) {
                continue;
            }
            if (ultimate == io.github.pok8.mysticismultimates.registry.UltimateSkills.LAVERNA.get()
                    && !io.github.pok8.mysticismultimates.skill.ShadowSpirits.hasGreaterDarkness(player)) {
                continue;
            }
            if (existence.getEP() < requirement.epCost()) {
                continue;
            }
            if (!SkillHelper.learnSkill(player, ultimate)) {
                continue;
            }
            if (requirement.epCost() > 0) {
                existence.setEP(existence.getEP() - requirement.epCost());
                existence.markDirty();
                ((StorageHolder) (Object) player).manasCore$sync();
            }
            if (loseUnique && requirement.uniquesToForget() != null) {
                for (Supplier<? extends ManasSkill> uniqueSupplier : requirement.uniquesToForget()) {
                    ManasSkill unique = uniqueSupplier.get();
                    if (unique != null && SkillUtils.hasSkill(player, unique)) {
                        SkillAPI.getSkillsFrom(player).forgetSkill(
                                unique.getRegistryName(),
                                Component.translatable("mysticism_ultimates.skill.ultimate_upgrade", unique.getName(), ultimate.getName()));
                    }
                }
            }
            player.displayClientMessage(Component.translatable(
                    "mysticism_ultimates.skill.acquired", ultimate.getName(), (long) requirement.epCost()), false);
            if (rules.getBoolean(UltimateGamerules.SOUL_QUALITY_MODE) && slots.currentUltimates() >= slots.soulQuality()) {
                return;
            }
        }
    }

    private static void rollSoulQuality(ServerPlayer player, UltimateSlotStorage slots) {
        if (slots.soulRolled() || RaceAPI.getRaceFrom(player).getRace().isEmpty()) {
            return;
        }
        int quality = rollWeighted(player.getRandom());
        slots.setSoulQuality(quality);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.soul_quality.rolled", quality), false);
    }

    private static int rollWeighted(RandomSource random) {
        int total = 0;
        for (int weight : SOUL_WEIGHTS) {
            total += weight;
        }
        int roll = random.nextInt(total);
        int cursor = 0;
        for (int index = 0; index < SOUL_WEIGHTS.length; index++) {
            cursor += SOUL_WEIGHTS[index];
            if (roll < cursor) {
                return index;
            }
        }
        return 0;
    }

    private static boolean mastered(ServerPlayer player, Requirement requirement) {
        for (Supplier<? extends ManasSkill> supplier : requirement.mastered()) {
            ManasSkill skill = supplier.get();
            if (skill == null || !SkillUtils.hasSkill(player, skill) || !SkillUtils.isSkillMastered(player, skill)) {
                return false;
            }
        }
        return !requirement.mastered().isEmpty();
    }

    private static void checkUniqueEvolutions(ServerPlayer player, GameRules rules) {
        ManasSkill plunderer = UniqueShadowSkills.PLUNDERER == null ? null : UniqueShadowSkills.PLUNDERER.get();
        ManasSkill darkness = ExtraMysticismSkills.DARKNESS_MANIPULATION == null ? null : ExtraMysticismSkills.DARKNESS_MANIPULATION.get();
        ManasSkill vainglory = UniqueShadowSkills.VAINGLORY == null ? null : UniqueShadowSkills.VAINGLORY.get();
        if (plunderer != null && darkness != null && vainglory != null
                && SkillUtils.hasSkill(player, plunderer)
                && SkillUtils.isSkillMastered(player, plunderer)
                && SkillUtils.hasSkill(player, darkness)
                && SkillUtils.isSkillMastered(player, darkness)
                && !SkillUtils.hasSkill(player, vainglory)) {
            if (SkillHelper.learnSkill(player, vainglory)) {
                if (rules.getBoolean(UltimateGamerules.LOSE_UNIQUE)) {
                    SkillAPI.getSkillsFrom(player).forgetSkill(
                            plunderer.getRegistryName(),
                            Component.translatable("mysticism_ultimates.skill.ultimate_upgrade", plunderer.getName(), vainglory.getName())
                    );
                }
                player.displayClientMessage(Component.translatable(
                        "mysticism_ultimates.skill.acquired", vainglory.getName(), 0L), false);
            }
        }
    }

    private static ExistenceStorage existence(ServerPlayer player) {
        return ((StorageHolder) (Object) player).manasCore$getStorage(ExistenceStorage.getKey());
    }

    public record Requirement(
            Supplier<? extends ManasSkill> ultimate,
            double epCost,
            List<Supplier<? extends ManasSkill>> uniquesToForget,
            List<Supplier<? extends ManasSkill>> mastered) {
    }
}
