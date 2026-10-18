package io.github.pok8.mysticismultimates.skill;

import dev.architectury.event.EventResult;
import io.github.Memoires.mysticism.registry.skill.UniqueMysticismSkills;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.EntityEvents;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.menu.SkillPickMenu;
import io.github.pok8.mysticismultimates.registry.UltimateSkills;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.data.TensuraEntityTags;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.skill.ResistanceSkills;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import io.github.manasmods.tensura.util.SubordinateHelper;
import io.github.manasmods.tensura.world.TensuraGameRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.UUID;

public final class VicielSkill extends DifficultUltimateSkill {
    private static final ResourceLocation SPEED = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "viciel_darkness_speed");
    private static final ResourceLocation ATTACK = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "viciel_darkness_attack");
    private static final int BANK_LIMIT = 64;

    public VicielSkill(String[] modes) {
        super("viciel", modes);
    }

    public static void register() {
        EntityEvents.LIVING_CHANGE_TARGET.register(VicielSkill::hideFromTargeting);
    }

    @Override
    public double getDefaultAcquiringMagiculeCost() {
        return 666_666.0;
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double cost) {
        io.github.manasmods.tensura.storage.ep.ExistenceStorage existence = ShadowArmy.existence(player);
        if (existence == null || (!existence.isTrueDemonLord() && !existence.isTrueHero())) {
            return false;
        }
        ManasSkill hidden = UniqueMysticismSkills.HIDDEN_RULER.get();
        return hidden != null && SkillUtils.isSkillMastered(player, hidden);
    }

    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            announce(player, true);
        }
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        clearDarkness(entity);
        if (entity instanceof ServerPlayer player) {
            announce(player, false);
        }
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !instance.isToggled()) {
            clearDarkness(entity);
            return;
        }
        if (player.tickCount % 40 == 0) {
            inspire(player);
        }
        if (player.level().getMaxLocalRawBrightness(player.blockPosition()) <= 7) {
            applyDarkness(player);
        } else {
            clearDarkness(player);
        }
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        TimeSkills.setCurrentContextEntity(player);
        try {
            switch (Math.floorMod(mode, getModes(instance))) {
                case 0 -> copy(player, instance, mode);
                case 1 -> paste(player, instance, mode);
                case 2 -> conscript(player, instance, mode);
                case 3 -> petrichor(player, instance, mode);
                case 4 -> shadowJump(player, instance, mode);
                default -> {
                }
            }
        } finally {
            TimeSkills.clearCurrentContextEntity();
        }
    }

    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        revokeGifts(entity, instance);
        return false;
    }

    @Override
    public void onForgetSkill(ManasSkillInstance instance, LivingEntity entity) {
        revokeGifts(entity, instance);
        clearDarkness(entity);
        super.onForgetSkill(instance, entity);
    }

    private void copy(ServerPlayer player, ManasSkillInstance instance, int mode) {
        if (player.isShiftKeyDown()) {
            showBank(player, instance);
            return;
        }
        if (instance.onCoolDown(mode)) {
            return;
        }
        LivingEntity target = SkillGrant.look(player, 24.0);
        if (target == null) {
            tell(player, "mysticism_ultimates.skill.viciel.no_target", ChatFormatting.RED);
            return;
        }
        if (target.getType().is(TensuraEntityTags.NO_SKILL_PLUNDER)) {
            tell(player, "tensura.targeting.not_allowed", ChatFormatting.RED);
            return;
        }
        if (bank(instance).size() >= BANK_LIMIT) {
            tell(player, "mysticism_ultimates.skill.viciel.bank_full", ChatFormatting.RED);
            return;
        }
        List<ManasSkillInstance> skills = SkillGrant.transferable(target, false, player, bank(instance));
        if (skills.isEmpty()) {
            tell(player, "mysticism_ultimates.skill.viciel.no_skills", ChatFormatting.RED);
            return;
        }
        SkillPickMenu.open(player, instance, mode, target.getUUID(), SkillGrant.ids(skills));
    }

    private void paste(ServerPlayer player, ManasSkillInstance instance, int mode) {
        if (instance.onCoolDown(mode)) {
            return;
        }
        LivingEntity target = SkillGrant.look(player, 16.0);
        if (target == null) {
            tell(player, "mysticism_ultimates.skill.viciel.no_target", ChatFormatting.RED);
            return;
        }
        if (!SubordinateHelper.isSubordinate(player, target)) {
            tell(player, "tensura.targeting.not_allowed", ChatFormatting.RED);
            return;
        }
        ListTag bank = bank(instance);
        if (bank.isEmpty()) {
            tell(player, "mysticism_ultimates.skill.viciel.bank_empty", ChatFormatting.RED);
            return;
        }
        List<ResourceLocation> ids = new java.util.ArrayList<>();
        for (int i = 0; i < bank.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(bank.getCompound(i).getString("id"));
            if (id != null) {
                ManasSkill skill = SkillGrant.byId(id.toString());
                if (skill != null && !SkillUtils.hasSkill(target, skill)) {
                    ids.add(id);
                }
            }
        }
        if (ids.isEmpty()) {
            tell(player, "mysticism_ultimates.skill.viciel.no_skills_to_paste", ChatFormatting.RED);
            return;
        }
        SkillPickMenu.open(player, instance, mode, target.getUUID(), ids);
    }

    public boolean grantPicked(ServerPlayer player, ManasSkillInstance instance, int mode, UUID targetId, ManasSkill chosen) {
        return switch (Math.floorMod(mode, getModes(instance))) {
            case 0 -> grantCopy(player, instance, mode, targetId, chosen);
            case 1 -> grantPaste(player, instance, mode, targetId, chosen);
            default -> false;
        };
    }

    private boolean grantCopy(ServerPlayer player, ManasSkillInstance instance, int mode, UUID targetId, ManasSkill chosen) {
        LivingEntity target = SkillGrant.find(player, targetId, 32.0);
        if (target == null || target.getType().is(TensuraEntityTags.NO_SKILL_PLUNDER) || chosen.getRegistryName() == null) {
            tell(player, "tensura.targeting.not_allowed", ChatFormatting.RED);
            return false;
        }
        ListTag bank = bank(instance);
        if (bank.size() >= BANK_LIMIT) {
            tell(player, "mysticism_ultimates.skill.viciel.bank_full", ChatFormatting.RED);
            return false;
        }
        ManasSkill skill = SkillGrant.afterPlunder(player, target, chosen, false);
        if (skill == null || skill.getRegistryName() == null) {
            tell(player, "tensura.ability.activation_failed", ChatFormatting.RED);
            return false;
        }
        String id = skill.getRegistryName().toString();
        if (contains(bank, id) || SkillUtils.hasSkill(player, skill)) {
            tell(player, "mysticism_ultimates.skill.viciel.already_banked", ChatFormatting.GOLD);
            return false;
        }
        double mastery = SkillGrant.mastery(target, chosen);
        if (!SkillGrant.pay(player, 300.0)) {
            return false;
        }
        if (!SkillGrant.grant(player, skill, mastery, false, null)) {
            tell(player, "tensura.ability.activation_failed", ChatFormatting.RED);
            return false;
        }
        CompoundTag entry = new CompoundTag();
        entry.putString("id", id);
        entry.putDouble("mastery", mastery);
        bank.add(entry);
        instance.getOrCreateTag().put("Copied", bank);
        instance.markDirty();
        instance.setCoolDown(mode, 2);
        instance.addMasteryPoint(player);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.viciel.copied", skill.getName()).withStyle(ChatFormatting.GREEN), false);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    private boolean grantPaste(ServerPlayer player, ManasSkillInstance instance, int mode, UUID targetId, ManasSkill chosen) {
        LivingEntity target = SkillGrant.find(player, targetId, 20.0);
        if (target == null || !SubordinateHelper.isSubordinate(player, target) || chosen.getRegistryName() == null) {
            tell(player, "tensura.targeting.not_allowed", ChatFormatting.RED);
            return false;
        }
        if (SkillUtils.hasSkill(target, chosen)) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.viciel.already_knows", target.getName(), chosen.getName()).withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        double mastery = 0.0;
        ListTag bank = bank(instance);
        boolean banked = false;
        for (int i = 0; i < bank.size(); i++) {
            if (chosen.getRegistryName().toString().equals(bank.getCompound(i).getString("id"))) {
                mastery = bank.getCompound(i).getDouble("mastery");
                banked = true;
                break;
            }
        }
        if (!banked) {
            tell(player, "tensura.ability.activation_failed", ChatFormatting.RED);
            return false;
        }
        double acquireCost = acquireCost(target, chosen, mastery);
        if (!SkillGrant.canPay(target, acquireCost)) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.viciel.need_magicule", target.getName(), (int) Math.ceil(acquireCost)).withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!SkillGrant.pay(player, 300.0)) {
            return false;
        }
        if (!SkillGrant.pay(target, acquireCost) || !SkillGrant.grant(target, chosen, mastery, true, player.getUUID())) {
            tell(player, "tensura.ability.activation_failed", ChatFormatting.RED);
            return false;
        }
        CompoundTag gift = new CompoundTag();
        gift.putString("Target", target.getStringUUID());
        gift.putString("Skill", chosen.getRegistryName().toString());
        ListTag gifts = instance.getOrCreateTag().getList("Gifted", Tag.TAG_COMPOUND);
        gifts.add(gift);
        instance.getOrCreateTag().put("Gifted", gifts);
        instance.markDirty();
        instance.setCoolDown(mode, 2);
        instance.addMasteryPoint(player);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.viciel.pasted", chosen.getName(), target.getName()).withStyle(ChatFormatting.GREEN), false);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    private static double acquireCost(LivingEntity target, ManasSkill skill, double mastery) {
        if (target instanceof Player learner && learner.isCreative()) {
            return 0.0;
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return 0.0;
        }
        double base;
        if (skill instanceof TensuraSkill tensuraSkill) {
            ManasSkillInstance sample = skill.createDefaultInstance();
            sample.setMastery(mastery);
            base = tensuraSkill.getAcquiringMagiculeCost(sample);
        } else if (skill instanceof Skill tensuraSkill) {
            base = tensuraSkill.getDefaultAcquiringMagiculeCost();
        } else {
            return 0.0;
        }
        return base * level.getGameRules().getInt(TensuraGameRules.MP_SKILL_COST) / 100.0;
    }

    private void conscript(ServerPlayer player, ManasSkillInstance instance, int mode) {
        if (instance.onCoolDown(mode)) {
            return;
        }
        LivingEntity target = SkillGrant.look(player, 20.0);
        if (target == null || target instanceof Player) {
            tell(player, "mysticism_ultimates.skill.viciel.no_target", ChatFormatting.RED);
            return;
        }
        if (player.level().getMaxLocalRawBrightness(player.blockPosition()) > 7
                || target.level().getMaxLocalRawBrightness(target.blockPosition()) > 7) {
            tell(player, "mysticism_ultimates.skill.viciel.too_bright", ChatFormatting.RED);
            return;
        }
        if (target.getType().is(TensuraEntityTags.NO_MIND_CONTROL)) {
            tell(player, "mysticism_ultimates.skill.viciel.recruitment_fail", ChatFormatting.RED);
            return;
        }
        ManasSkill immunity = ResistanceSkills.SPIRITUAL_ATTACK_NULLIFICATION.get();
        if (immunity != null && SkillUtils.hasSkill(target, immunity)) {
            tell(player, "tensura.ability.activation_failed", ChatFormatting.RED);
            return;
        }
        ExistenceStorage existence = ShadowArmy.existence(target);
        if (existence != null && existence.getPermanentOwner() != null && !player.getUUID().equals(existence.getPermanentOwner())) {
            tell(player, "tensura.targeting.not_allowed", ChatFormatting.RED);
            return;
        }
        if (!SkillGrant.pay(player, 1000.0)) {
            return;
        }
        if (existence != null) {
            existence.setPermanentOwner(player.getUUID());
        }
        if (target instanceof TamableAnimal animal) {
            animal.tame(player);
        }
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.setPersistenceRequired();
        }
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 600, 1));
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 600, 1));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + 1.0, target.getZ(), 24, 0.4, 0.6, 0.4, 0.02);
        }
        instance.setCoolDown(mode, instance.isMastered(player) ? 8 : 15);
        instance.addMasteryPoint(player);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.viciel.conscripted", target.getName()).withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    private void petrichor(ServerPlayer player, ManasSkillInstance instance, int mode) {
        CompoundTag tag = instance.getOrCreateTag();
        if (player.isShiftKeyDown()) {
            BlockHitResult hit = ObjectSelectionHelper.getPlayerPOVHitResult(player.level(), player, net.minecraft.world.level.ClipContext.Fluid.NONE, 32.0);
            if (hit.getType() == HitResult.Type.MISS) {
                tell(player, "mysticism_ultimates.skill.viciel.no_target", ChatFormatting.RED);
                return;
            }
            tag.putString("StainDim", player.level().dimension().location().toString());
            tag.putInt("StainX", hit.getBlockPos().getX());
            tag.putInt("StainY", hit.getBlockPos().getY() + 1);
            tag.putInt("StainZ", hit.getBlockPos().getZ());
            tag.putBoolean("StainSet", true);
            instance.markDirty();
            tell(player, "mysticism_ultimates.skill.viciel.block_marker", ChatFormatting.AQUA);
            return;
        }
        LivingEntity looked = SkillGrant.look(player, 32.0);
        if (looked != null && looked != player) {
            tag.putString("Destiny", looked.getStringUUID());
            tag.putString("DestinyDim", looked.level().dimension().location().toString());
            instance.markDirty();
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.viciel.target_marker", looked.getName()).withStyle(ChatFormatting.DARK_AQUA), true);
            return;
        }
        if (instance.onCoolDown(mode) || !tag.getBoolean("StainSet") || !tag.contains("Destiny")) {
            tell(player, "mysticism_ultimates.skill.viciel.target_missing", ChatFormatting.RED);
            return;
        }
        LivingEntity marked = find(player, tag.getString("Destiny"));
        if (marked == null) {
            tell(player, "mysticism_ultimates.skill.viciel.target_missing", ChatFormatting.RED);
            return;
        }
        if (marked.getType().is(TensuraEntityTags.NO_FORCED_WARP)) {
            tell(player, "tensura.targeting.not_allowed", ChatFormatting.RED);
            return;
        }
        ServerLevel destination = dimension(player, tag.getString("StainDim"));
        if (destination == null) {
            tell(player, "mysticism_ultimates.skill.viciel.bad_dimension", ChatFormatting.RED);
            return;
        }
        if (!SkillGrant.pay(player, 800.0)) {
            return;
        }
        marked.teleportTo(destination, tag.getInt("StainX") + 0.5, tag.getInt("StainY"), tag.getInt("StainZ") + 0.5, java.util.Set.of(), marked.getYRot(), marked.getXRot());
        marked.hurt(player.damageSources().magic(), instance.isMastered(player) ? 40.0f : 20.0f);
        if (TensuraMobEffects.CURSE != null && TensuraMobEffects.CURSE.get() != null) {
            marked.addEffect(new MobEffectInstance(TensuraMobEffects.CURSE, 200, 1));
        }
        instance.setCoolDown(mode, 8);
        instance.addMasteryPoint(player);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.viciel.stained", marked.getName()).withStyle(ChatFormatting.DARK_AQUA), false);
    }

    private void shadowJump(ServerPlayer player, ManasSkillInstance instance, int mode) {
        CompoundTag tag = instance.getOrCreateTag();
        if (player.isShiftKeyDown()) {
            tag.putBoolean("JumpSet", true);
            tag.putString("JumpDim", player.level().dimension().location().toString());
            tag.putDouble("JumpX", player.getX());
            tag.putDouble("JumpY", player.getY());
            tag.putDouble("JumpZ", player.getZ());
            tag.putFloat("JumpYaw", player.getYRot());
            instance.markDirty();
            tell(player, "mysticism_ultimates.skill.viciel.block_marker", ChatFormatting.DARK_PURPLE);
            return;
        }
        if (instance.onCoolDown(mode) || !tag.getBoolean("JumpSet")) {
            tell(player, "mysticism_ultimates.skill.viciel.target_missing", ChatFormatting.RED);
            return;
        }
        ServerLevel destination = dimension(player, tag.getString("JumpDim"));
        if (destination == null) {
            tell(player, "mysticism_ultimates.skill.viciel.bad_dimension", ChatFormatting.RED);
            return;
        }
        if (!SkillGrant.pay(player, 500.0)) {
            return;
        }
        player.teleportTo(destination, tag.getDouble("JumpX"), tag.getDouble("JumpY"), tag.getDouble("JumpZ"), java.util.Set.of(), tag.getFloat("JumpYaw"), player.getXRot());
        instance.setCoolDown(mode, 3);
        instance.addMasteryPoint(player);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    private static void showBank(ServerPlayer player, ManasSkillInstance instance) {
        ListTag bank = bank(instance);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.viciel.collection_header").withStyle(ChatFormatting.GOLD), false);
        if (bank.isEmpty()) {
            tell(player, "mysticism_ultimates.skill.viciel.bank_empty", ChatFormatting.GRAY);
            return;
        }
        for (int i = 0; i < bank.size(); i++) {
            ManasSkill skill = SkillGrant.byId(bank.getCompound(i).getString("id"));
            Component name = skill == null ? Component.translatable("mysticism_ultimates.skill.viciel.unknown_skill") : skill.getName();
            player.displayClientMessage(Component.literal((i + 1) + ". ").append(name), false);
        }
    }

    private static ListTag bank(ManasSkillInstance instance) {
        return instance.getOrCreateTag().getList("Copied", Tag.TAG_COMPOUND);
    }

    private static boolean contains(ListTag bank, String id) {
        for (int i = 0; i < bank.size(); i++) {
            if (id.equals(bank.getCompound(i).getString("id"))) {
                return true;
            }
        }
        return false;
    }

    private static void inspire(ServerPlayer player) {
        if (TensuraMobEffects.INSPIRATION == null || TensuraMobEffects.INSPIRATION.get() == null) {
            return;
        }
        for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16.0))) {
            if (nearby != player && SubordinateHelper.isSubordinate(player, nearby)) {
                nearby.addEffect(new MobEffectInstance(TensuraMobEffects.INSPIRATION, 80, 0, false, false, true));
            }
        }
    }

    private static void applyDarkness(ServerPlayer player) {
        if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(new AttributeModifier(SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            player.getAttribute(Attributes.ATTACK_DAMAGE).addOrUpdateTransientModifier(new AttributeModifier(ATTACK, 4.0, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void clearDarkness(LivingEntity entity) {
        if (entity.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPEED);
        }
        if (entity.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(ATTACK);
        }
    }

    private static void announce(ServerPlayer player, boolean hiding) {
        if (TensuraMobEffects.PRESENCE_CONCEALMENT != null && TensuraMobEffects.PRESENCE_CONCEALMENT.get() != null) {
            if (hiding) {
                player.addEffect(new MobEffectInstance(TensuraMobEffects.PRESENCE_CONCEALMENT, -1, 0, false, false, true));
            } else {
                player.removeEffect(TensuraMobEffects.PRESENCE_CONCEALMENT);
            }
        }
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other == player) {
                continue;
            }
            MobEffectInstance sense = TensuraMobEffects.PRESENCE_SENSE == null ? null : other.getEffect(TensuraMobEffects.PRESENCE_SENSE);
            if (sense != null && sense.getAmplifier() >= 1) {
                continue;
            }
            if (hiding) {
                other.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
                other.sendSystemMessage(Component.translatable("multiplayer.player.left", player.getDisplayName()).withStyle(ChatFormatting.YELLOW));
            } else {
                other.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
                other.sendSystemMessage(Component.translatable("multiplayer.player.joined", player.getDisplayName()).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    private static void revokeGifts(LivingEntity entity, ManasSkillInstance instance) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        ListTag gifts = instance.getOrCreateTag().getList("Gifted", Tag.TAG_COMPOUND);
        for (int i = 0; i < gifts.size(); i++) {
            CompoundTag gift = gifts.getCompound(i);
            LivingEntity recipient = find(player, gift.getString("Target"));
            ManasSkill skill = SkillGrant.byId(gift.getString("Skill"));
            if (recipient == null || skill == null || skill.getRegistryName() == null) {
                continue;
            }
            SkillStorage storage = SkillAPI.getSkillsFrom(recipient);
            if (storage == null) {
                continue;
            }
            storage.getSkill(skill.getRegistryName()).ifPresent(learned -> {
                if (learned.getOrCreateTag().getBoolean("VicielGift")) {
                    storage.forgetSkill(skill.getRegistryName(), Component.translatable("mysticism_ultimates.skill.viciel.gift_reclaimed"));
                }
            });
        }
        instance.getOrCreateTag().remove("Gifted");
        instance.markDirty();
    }

    private static LivingEntity find(ServerPlayer player, String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        for (ServerLevel level : player.server.getAllLevels()) {
            Entity found = level.getEntity(uuid);
            if (found instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        return null;
    }

    private static ServerLevel dimension(ServerPlayer player, String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return null;
        }
        return player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
    }

    private static EventResult hideFromTargeting(LivingEntity mob, Changeable<LivingEntity> target) {
        LivingEntity aimed = target.get();
        if (!(aimed instanceof ServerPlayer player) || mob.distanceTo(player) <= 8.0) {
            return EventResult.pass();
        }
        ManasSkill skill = UltimateSkills.VICIEL == null ? null : UltimateSkills.VICIEL.get();
        if (skill == null) {
            return EventResult.pass();
        }
        SkillStorage storage = SkillAPI.getSkillsFrom(player);
        if (storage == null) {
            return EventResult.pass();
        }
        return storage.getSkill(skill.getRegistryName())
                .filter(ManasSkillInstance::isToggled)
                .map(ignored -> {
                    target.set(null);
                    return EventResult.interruptFalse();
                })
                .orElse(EventResult.pass());
    }

    private static void tell(Player player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }
}
