package io.github.pok8.mysticismultimates.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.UltimateSlotStorage;
import io.github.pok8.mysticismultimates.entity.AnimationSpawner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class DifficultUltimateSkill extends Skill {
    private final String id;
    private final String[] modes;

    public DifficultUltimateSkill(String id, String[] modes) {
        super(SkillType.ULTIMATE);
        this.id = id;
        this.modes = modes;
    }

    public String id() {
        return id;
    }

    @Override
    public int getModes(ManasSkillInstance instance) {
        return Math.max(modes.length, 1);
    }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int currentMode, boolean reverse) {
        int count = getModes(instance);
        if (count <= 1) {
            return currentMode;
        }
        return Math.floorMod(currentMode + (reverse ? -1 : 1), count);
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        if (modes.length == 0) {
            return id + ".unfinished";
        }
        int index = Math.floorMod(mode, modes.length);
        return id + "." + modes[index];
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable("mysticism_ultimates.skill." + id);
    }

    @Override
    public MutableComponent getSkillDescription() {
        return Component.translatable("mysticism_ultimates.skill." + id + ".description");
    }

    @Override
    public ResourceLocation getSkillIcon() {
        String file = switch (id) {
            case "ame_no_uzume" -> "ame_no_uzume_idol";
            case "lucifer", "demeter" -> null;
            default -> id;
        };
        if (file == null) {
            return ResourceLocation.fromNamespaceAndPath("mysticism", "textures/skill/ultimate/embryo.png");
        }
        return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "textures/skill/" + file + ".png");
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (entity instanceof ServerPlayer player) {
            TimeSkills.setCurrentContextEntity(player);
        }
        try {
            if (!(entity instanceof Player player) || entity.level().isClientSide()) {
                return;
            }
            if (UltimateAttacks.cast(this, instance, player, mode)) {
                return;
            }
            if (AnimationSpawner.spawn(player, getModeId(instance, mode))) {
                return;
            }
            player.displayClientMessage(Component.translatable(
                    "mysticism_ultimates.skill.not_implemented",
                    getName(),
                    getModeName(instance, mode),
                    Component.translatable("mysticism_ultimates.blocker." + id)), false);
        } finally {
            if (entity instanceof ServerPlayer) {
                TimeSkills.clearCurrentContextEntity();
            }
        }
    }

    @Override
    public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        if (entity.level().isClientSide() || instance.isTemporarySkill()) {
            return;
        }
        UltimateSlotStorage storage = UltimateSlotStorage.get(entity);
        if (storage != null) {
            storage.increment();
        }
    }

    @Override
    public void onForgetSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onForgetSkill(instance, entity);
        if (entity.level().isClientSide() || instance.isTemporarySkill()) {
            return;
        }
        UltimateSlotStorage storage = UltimateSlotStorage.get(entity);
        if (storage != null) {
            storage.decrement();
        }
    }
}
