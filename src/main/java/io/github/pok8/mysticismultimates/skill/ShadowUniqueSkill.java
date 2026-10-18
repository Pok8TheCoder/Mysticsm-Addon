package io.github.pok8.mysticismultimates.skill;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.Memoires.mysticism.registry.skill.ExtraMysticismSkills;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.subclass.ISpatialStorage;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import net.minecraft.core.HolderLookup;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.registry.UniqueShadowSkills;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ShadowUniqueSkill extends Skill implements ISpatialStorage {
    private final String id;
    private final boolean vainglory;

    public ShadowUniqueSkill(String id, boolean vainglory) {
        super(SkillType.UNIQUE);
        this.id = id;
        this.vainglory = vainglory;
    }

    @Override
    public int getModes(ManasSkillInstance instance) {
        if (vainglory && instance.getMastery() >= getMaxMastery()) {
            return 5;
        }
        return 3;
    }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int currentMode, boolean reverse) {
        int step = reverse ? -1 : 1;
        return Math.floorMod(currentMode + step, getModes(instance));
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return switch (Math.floorMod(mode, getModes(instance))) {
            case 1 -> id + ".shadow_storage";
            case 2 -> id + ".shadow_merge";
            case 3 -> id + ".shadow_pit";
            case 4 -> id + ".army_summon";
            default -> id + ".shadow_warrior";
        };
    }

    @Override
    public SpatialStorageContainer getSpatialStorage(ManasSkillInstance instance, HolderLookup.Provider provider) {
        return ShadowPit.load(instance, provider, vainglory ? ShadowPit.VAINGLORY_SLOTS : 1);
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
        return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "textures/skill/" + id + ".png");
    }

    @Override
    public double getDefaultAcquiringMagiculeCost() {
        return vainglory ? 100_000.0 : 75_000.0;
    }

    @Override
    public int getMaxMastery() {
        return vainglory ? 1500 : super.getMaxMastery();
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double cost) {
        if (!vainglory) {
            return super.checkAcquiringRequirement(player, cost);
        }
        ManasSkill plunderer = UniqueShadowSkills.PLUNDERER.get();
        ManasSkill darkness = ExtraMysticismSkills.DARKNESS_MANIPULATION.get();
        return plunderer != null && darkness != null
                && SkillUtils.isSkillMastered(player, plunderer)
                && SkillUtils.isSkillMastered(player, darkness);
    }

    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return instance.getMastery() >= 0;
    }

    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public void onSkillMastered(ManasSkillInstance instance, LivingEntity entity) {
        super.onSkillMastered(instance, entity);
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (vainglory) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("mysticism_ultimates.skill.shadow_pit.unlocked").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), false);
        } else {
            ShadowArmy.teachShadowMotion(player);
        }
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            ShadowArmy.maintain(player, instance);
        }
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
            return;
        }
        ExistenceStorage existence = ShadowArmy.existence(player);
        if (existence != null && existence.getMagicule() < 0) {
            return;
        }
        switch (Math.floorMod(mode, getModes(instance))) {
            case 0 -> ShadowArmy.summonWarrior(player, instance);
            case 1 -> ShadowArmy.storeSubordinate(player, instance);
            case 2 -> ShadowArmy.toggleMerge(player, instance);
            case 3 -> ShadowPit.open(player, instance);
            case 4 -> ShadowPit.toggleArmy(player, instance);
            default -> {
            }
        }
    }
}
