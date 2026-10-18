package io.github.pok8.mysticismultimates.skill;

import io.github.Memoires.mysticism.config.ability.skill.UniqueMysticismSkillConfig;
import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.TensuraSkillInstance;
import io.github.manasmods.tensura.data.TensuraEntityTags;
import io.github.manasmods.tensura.registry.sound.TensuraSoundEvents;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.util.SubordinateHelper;
import io.github.pok8.mysticismultimates.menu.SkillPickMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HiddenRulerHandler {
    private HiddenRulerHandler() {
    }

    private static UniqueMysticismSkillConfig.HiddenRuler config() {
        UniqueMysticismSkillConfig config = ConfigRegistry.getConfig(UniqueMysticismSkillConfig.class);
        if (config != null && config.HiddenRuler != null) {
            return config.HiddenRuler;
        }
        return new UniqueMysticismSkillConfig.HiddenRuler();
    }

    public static boolean onCopy(ServerPlayer player, ManasSkillInstance instance, int mode, LivingEntity target, CompoundTag tag) {
        if (instance.onCoolDown(mode)) {
            return true;
        }
        var cfg = config();
        if (target == null) {
            player.displayClientMessage(Component.translatable("mysticism.skill.mode.hidden_ruler.no_target").withStyle(ChatFormatting.RED), false);
            instance.setCoolDown(mode, cfg.noTargetCooldown);
            player.playSound(TensuraSoundEvents.GENERIC_CAST_FAIL.get(), 1.0f, 1.0f);
            return true;
        }
        if (target.getType().is(TensuraEntityTags.NO_SKILL_PLUNDER)) {
            player.displayClientMessage(Component.translatable("tensura.targeting.not_allowed").withStyle(ChatFormatting.RED), false);
            player.playSound(TensuraSoundEvents.GENERIC_CAST_FAIL.get(), 1.0f, 1.0f);
            return true;
        }
        ListTag bank = tag.getList("HiddenRulerList", Tag.TAG_STRING);
        List<ManasSkillInstance> skills = SkillGrant.transferable(target, false, player, bank, true);
        if (skills.isEmpty()) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.viciel.no_skills").withStyle(ChatFormatting.RED), true);
            player.playSound(TensuraSoundEvents.GENERIC_CAST_FAIL.get(), 1.0f, 1.0f);
            return true;
        }
        SkillPickMenu.open(player, instance, mode, target.getUUID(), SkillGrant.ids(skills));
        return true;
    }

    public static boolean onPaste(ServerPlayer player, ManasSkillInstance instance, int mode, LivingEntity target, CompoundTag tag) {
        if (instance.onCoolDown(mode)) {
            return true;
        }
        var cfg = config();
        if (target == null) {
            player.displayClientMessage(Component.translatable("mysticism.skill.mode.hidden_ruler.no_target").withStyle(ChatFormatting.RED), false);
            instance.setCoolDown(mode, cfg.noTargetCooldown);
            player.playSound(TensuraSoundEvents.GENERIC_CAST_FAIL.get(), 1.0f, 1.0f);
            return true;
        }
        if (!SubordinateHelper.isSubordinate(player, target)) {
            player.displayClientMessage(Component.translatable("tensura.targeting.not_allowed").withStyle(ChatFormatting.RED), false);
            player.playSound(TensuraSoundEvents.GENERIC_CAST_FAIL.get(), 1.0f, 1.0f);
            return true;
        }
        ListTag bank = tag.getList("HiddenRulerList", Tag.TAG_STRING);
        if (bank.isEmpty()) {
            player.displayClientMessage(Component.translatable("mysticism.skill.mode.hidden_ruler.paste.fail_empty").withStyle(ChatFormatting.RED), false);
            player.playSound(TensuraSoundEvents.GENERIC_CAST_FAIL.get(), 1.0f, 1.0f);
            return true;
        }
        List<ResourceLocation> ids = new ArrayList<>();
        for (int i = 0; i < bank.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(bank.getString(i));
            if (id != null) {
                ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
                if (skill != null && !SkillUtils.hasSkill(target, skill)) {
                    ids.add(id);
                }
            }
        }
        if (ids.isEmpty()) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.viciel.no_skills_to_paste").withStyle(ChatFormatting.RED), false);
            player.playSound(TensuraSoundEvents.GENERIC_CAST_FAIL.get(), 1.0f, 1.0f);
            return true;
        }
        SkillPickMenu.open(player, instance, mode, target.getUUID(), ids);
        return true;
    }

    public static boolean grantPicked(ServerPlayer player, ManasSkillInstance instance, int mode, UUID targetId, ManasSkill chosen) {
        return switch (mode) {
            case 0 -> grantCopy(player, instance, mode, targetId, chosen);
            case 1 -> grantPaste(player, instance, mode, targetId, chosen);
            default -> false;
        };
    }

    private static boolean grantCopy(ServerPlayer player, ManasSkillInstance instance, int mode, UUID targetId, ManasSkill chosen) {
        LivingEntity target = SkillGrant.find(player, targetId, 16.0);
        if (target == null || target.getType().is(TensuraEntityTags.NO_SKILL_PLUNDER) || chosen.getRegistryName() == null) {
            player.displayClientMessage(Component.translatable("tensura.targeting.not_allowed").withStyle(ChatFormatting.RED), false);
            return false;
        }
        var cfg = config();
        ExistenceStorage userEx = ShadowArmy.existence(player);
        ExistenceStorage targetEx = ShadowArmy.existence(target);
        double userEp = userEx != null ? userEx.getEP() : 0.0;
        double targetEp = targetEx != null ? targetEx.getEP() : 0.0;
        double threshold = instance.isMastered(player) ? cfg.failureThresholdMastered : cfg.failureThreshold;
        if (targetEp > userEp * threshold) {
            player.displayClientMessage(Component.translatable("mysticism.skill.mode.hidden_ruler.copy.fail_ep", threshold).withStyle(ChatFormatting.RED), false);
            instance.setCoolDown(mode, cfg.copyFailedCooldown);
            return false;
        }
        double chance = instance.isMastered(player) ? cfg.copyChanceMastered : cfg.copyChance;
        if (player.getRandom().nextInt(100) >= chance) {
            player.displayClientMessage(Component.translatable("mysticism.skill.mode.hidden_ruler.copy.fail_chance").withStyle(ChatFormatting.RED), false);
            instance.setCoolDown(mode, cfg.copyFailedCooldown);
            return false;
        }
        if (!SkillGrant.pay(player, cfg.copyCost)) {
            return false;
        }
        ManasSkill allowed = SkillGrant.afterPlunder(player, target, chosen, false);
        if (allowed == null || allowed.getRegistryName() == null) {
            player.displayClientMessage(Component.translatable("tensura.ability.activation_failed").withStyle(ChatFormatting.RED), true);
            return false;
        }
        CompoundTag tag = instance.getOrCreateTag();
        ListTag bank = tag.getList("HiddenRulerList", Tag.TAG_STRING);
        if (!SkillUtils.hasSkill(player, allowed)) {
            SkillHelper.learnSkill(player, allowed, -2);
            player.displayClientMessage(Component.translatable("mysticism.skill.mode.hidden_ruler.copy.success", allowed.getName()).withStyle(ChatFormatting.GREEN), false);
        } else {
            bank.add(StringTag.valueOf(allowed.getRegistryName().toString()));
            tag.put("HiddenRulerList", bank);
            instance.markDirty();
            player.displayClientMessage(Component.translatable("mysticism.skill.mode.hidden_ruler.copy.success_bank", allowed.getName()).withStyle(ChatFormatting.GREEN), false);
        }
        instance.addMasteryPoint(player);
        int cd = instance.isMastered(player) ? cfg.copyCooldownMastered : cfg.copyCooldown;
        instance.setCoolDown(mode, cd);
        player.swing(InteractionHand.MAIN_HAND, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TensuraSoundEvents.GENERIC_CAST.get(), TensuraSkill.ABILITY_SOUND, 1.0f, 1.0f);
        return true;
    }

    private static boolean grantPaste(ServerPlayer player, ManasSkillInstance instance, int mode, UUID targetId, ManasSkill chosen) {
        LivingEntity target = SkillGrant.find(player, targetId, 16.0);
        if (target == null || !SubordinateHelper.isSubordinate(player, target) || chosen.getRegistryName() == null) {
            player.displayClientMessage(Component.translatable("tensura.targeting.not_allowed").withStyle(ChatFormatting.RED), false);
            return false;
        }
        if (SkillUtils.hasSkill(target, chosen)) {
            player.displayClientMessage(Component.translatable("mysticism.skill.mode.hidden_ruler.paste.fail_already_known", target.getName()).withStyle(ChatFormatting.RED), false);
            return false;
        }
        var cfg = config();
        double chance = instance.isMastered(player) ? cfg.pasteChanceMastered : cfg.pasteChance;
        if (player.getRandom().nextInt(100) >= chance) {
            player.displayClientMessage(Component.translatable("mysticism.skill.mode.hidden_ruler.paste.fail_chance").withStyle(ChatFormatting.RED), false);
            instance.setCoolDown(mode, cfg.pasteFailedCooldown);
            return false;
        }
        if (!SkillGrant.pay(player, cfg.pasteCost)) {
            return false;
        }
        TensuraSkillInstance granted = new TensuraSkillInstance(chosen);
        granted.setToggled(true);
        granted.getOrCreateTag().putBoolean("GiftedInstance", true);
        if (!SkillHelper.learnSkill(target, granted, -2)) {
            player.displayClientMessage(Component.translatable("mysticism.skill.mode.hidden_ruler.paste.fail_already_known", target.getName()).withStyle(ChatFormatting.RED), false);
            return false;
        }
        instance.addMasteryPoint(player);
        int cd = instance.isMastered(player) ? cfg.pasteCooldownMastered : cfg.pasteCooldown;
        instance.setCoolDown(mode, cd);
        player.swing(InteractionHand.MAIN_HAND, true);
        player.displayClientMessage(Component.translatable("mysticism.skill.mode.hidden_ruler.paste.success", chosen.getName()).withStyle(ChatFormatting.GREEN), false);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TensuraSoundEvents.GENERIC_CAST.get(), TensuraSkill.ABILITY_SOUND, 1.0f, 1.0f);
        return true;
    }
}
