package io.github.pok8.mysticismultimates.skill;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.event.TensuraSkillEvents;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import io.github.manasmods.tensura.data.TensuraSkillTags;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class SkillGrant {
    private SkillGrant() {
    }

    public static LivingEntity look(LivingEntity user, double range) {
        return ObjectSelectionHelper.getTargetingEntity(user, range, false);
    }

    public static boolean pay(LivingEntity entity, double cost) {
        if (!canPay(entity, cost)) {
            if (cost > 0.0 && entity instanceof net.minecraft.world.entity.player.Player player) {
                player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.lack_magicule").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        if (cost <= 0.0) {
            return true;
        }
        ExistenceStorage existence = ShadowArmy.existence(entity);
        existence.setMagicule(existence.getMagicule() - cost);
        return true;
    }

    public static boolean canPay(LivingEntity entity, double cost) {
        if (cost <= 0.0) {
            return true;
        }
        ExistenceStorage existence = ShadowArmy.existence(entity);
        return existence != null && existence.getMagicule() >= cost;
    }

    public static List<ManasSkillInstance> transferable(LivingEntity target, boolean includeUltimate) {
        return transferable(target, includeUltimate, null, null, false);
    }

    public static List<ManasSkillInstance> transferable(LivingEntity target, boolean includeUltimate, LivingEntity recipient, ListTag bank) {
        return transferable(target, includeUltimate, recipient, bank, false);
    }

    public static List<ManasSkillInstance> transferable(LivingEntity target, boolean includeUltimate, LivingEntity recipient, ListTag bank, boolean restrictCommonExtraIntrinsic) {
        if (target == null) {
            return List.of();
        }
        SkillStorage storage = SkillAPI.getSkillsFrom(target);
        if (storage == null) {
            return List.of();
        }
        List<ManasSkillInstance> skills = new ArrayList<>();
        for (ManasSkillInstance learned : storage.getLearnedSkills()) {
            if (learned == null || learned.isTemporarySkill() || !(learned.getSkill() instanceof Skill skill)) {
                continue;
            }
            // Target must actually possess the skill with non-negative mastery (avoids unlearned resistances initialized at -1.0)
            if (learned.getMastery() < 0.0 || !SkillUtils.hasSkill(target, skill)) {
                continue;
            }
            if (skill.getType() == Skill.SkillType.ULTIMATE && !includeUltimate) {
                continue;
            }
            if (learned.is(TensuraSkillTags.ULTIMATE_SKILLS) && !includeUltimate) {
                continue;
            }
            if (learned.is(TensuraSkillTags.NO_PLUNDERING)) {
                continue;
            }
            if (restrictCommonExtraIntrinsic) {
                Skill.SkillType type = skill.getType();
                if (type != Skill.SkillType.COMMON && type != Skill.SkillType.EXTRA && type != Skill.SkillType.INTRINSIC) {
                    continue;
                }
            }
            // Exclude skills the recipient already has
            if (recipient != null && SkillUtils.hasSkill(recipient, skill)) {
                continue;
            }
            // Exclude skills already in the skill bank
            if (bank != null && skill.getRegistryName() != null && containsBank(bank, skill.getRegistryName().toString())) {
                continue;
            }
            skills.add(learned);
        }
        skills.sort(Comparator.comparing(learned -> learned.getSkill().getRegistryName().toString()));
        return skills;
    }

    public static boolean containsBank(ListTag bank, String id) {
        if (bank == null || id == null) {
            return false;
        }
        for (int i = 0; i < bank.size(); i++) {
            Tag entry = bank.get(i);
            if (entry instanceof CompoundTag comp && id.equals(comp.getString("id"))) {
                return true;
            }
            if (entry instanceof StringTag str && id.equals(str.getAsString())) {
                return true;
            }
            if (id.equals(bank.getString(i))) {
                return true;
            }
        }
        return false;
    }

    public static ManasSkill afterPlunder(LivingEntity user, LivingEntity target, ManasSkill skill, boolean steal) {
        Changeable<ManasSkill> changeable = Changeable.of(skill);
        EventResult result = TensuraSkillEvents.SKILL_PLUNDER.invoker().plunder(user, target, steal, changeable);
        if (result.isFalse()) {
            return null;
        }
        return changeable.get() == null ? skill : changeable.get();
    }

    public static boolean grant(LivingEntity recipient, ManasSkill skill, double mastery, boolean gift, UUID owner) {
        if (skill == null || SkillUtils.hasSkill(recipient, skill)) {
            return false;
        }
        ManasSkillInstance copy = skill.createDefaultInstance();
        copy.setMastery(mastery);
        copy.setRemoveTime(-1);
        if (gift) {
            CompoundTag tag = copy.getOrCreateTag();
            tag.putBoolean("VicielGift", true);
            tag.putBoolean("NoMagiculeCost", true);
            if (owner != null) {
                tag.putString("VicielOwner", owner.toString());
            }
        }
        return SkillHelper.learnSkill(recipient, copy);
    }

    public static List<ResourceLocation> ids(List<ManasSkillInstance> skills) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (ManasSkillInstance learned : skills) {
            if (learned.getSkill() != null && learned.getSkill().getRegistryName() != null) {
                ids.add(learned.getSkill().getRegistryName());
            }
        }
        return ids;
    }

    public static LivingEntity find(ServerPlayer player, UUID id, double range) {
        if (id == null || (id.getMostSignificantBits() == 0L && id.getLeastSignificantBits() == 0L)) {
            return null;
        }
        for (ServerLevel level : player.server.getAllLevels()) {
            Entity found = level.getEntity(id);
            if (found instanceof LivingEntity living && living.isAlive() && living.level() == player.level() && living.distanceTo(player) <= range) {
                return living;
            }
        }
        return null;
    }

    public static double mastery(LivingEntity entity, ManasSkill skill) {
        if (skill == null || skill.getRegistryName() == null) {
            return 0.0;
        }
        SkillStorage storage = SkillAPI.getSkillsFrom(entity);
        if (storage == null) {
            return 0.0;
        }
        return storage.getSkill(skill.getRegistryName()).map(ManasSkillInstance::getMastery).orElse(0.0);
    }

    public static ManasSkill byId(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return null;
        }
        return SkillAPI.getSkillRegistry().get(location);
    }
}
