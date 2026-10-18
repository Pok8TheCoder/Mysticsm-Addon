package io.github.pok8.mysticismultimates.skill;

import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.extra.SenseHeatSourceSkill;
import io.github.manasmods.tensura.ability.skill.extra.ThoughtAccelerationSkill;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import io.github.manasmods.tensura.entity.human.CloneEntity;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.skill.ResistanceSkills;
import io.github.manasmods.tensura.storage.ability.AbilitySlot;
import io.github.manasmods.tensura.storage.ability.AbilityStorage;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class TsukuyomiSkill extends DifficultUltimateSkill {
    private static final double EYE_COST = 50_000.0;
    private static final double INSTANT_COST = 350_000.0;
    private static final double CLONE_COST = 75_000.0;
    private static final double DASH_AURA = 100_000.0;
    private static final ResourceLocation ACCELERATION = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "tsukuyomi_acceleration");
    private static final ResourceLocation HEAT_SENSE = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "tsukuyomi_heat_sense");
    private static final ResourceLocation LABYRINTH_BARRIER = ResourceLocation.fromNamespaceAndPath("tensura", "labyrinth_barrier");

    public TsukuyomiSkill(String[] modes) {
        super("tsukuyomi", modes);
    }

    @Override
    public int getMaxMastery() {
        return 2000;
    }

    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return instance.getMastery() >= 0.0;
    }

    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        return instance.isToggled();
    }

    @Override
    public boolean canIgnoreCoolDown(ManasSkillInstance instance, LivingEntity entity, int mode) {
        if (instance.getMastery() < 0.0) {
            return false;
        }
        String modeId = getModeId(instance, mode);
        return "tsukuyomi.eye_of_the_moon".equals(modeId) || "tsukuyomi.lightspeed_acceleration".equals(modeId);
    }

    @Override
    public boolean canScroll(ManasSkillInstance instance, LivingEntity entity, int mode) {
        return "tsukuyomi.parallel_existence".equals(getModeId(instance, mode));
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        String modeId = getModeId(instance, mode);
        return switch (modeId) {
            case "tsukuyomi.eye_of_the_moon" -> EYE_COST;
            case "tsukuyomi.insta_kill" -> INSTANT_COST;
            case "tsukuyomi.parallel_existence" -> CLONE_COST;
            default -> 0.0;
        };
    }

    @Override
    public double getAuraCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        String modeId = getModeId(instance, mode);
        if ("tsukuyomi.lightspeed_acceleration".equals(modeId)) {
            return DASH_AURA;
        }
        return 0.0;
    }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        ThoughtAccelerationSkill.onToggle(instance, entity, ACCELERATION, true);
    }

    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity entity, DamageSource source) {
        clearCloneFlag(instance);
        return false;
    }

    @Override
    public void onForgetSkill(ManasSkillInstance instance, LivingEntity entity) {
        heatSense(entity, false);
        clearCloneFlag(instance);
        super.onForgetSkill(instance, entity);
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        ThoughtAccelerationSkill.onToggle(instance, entity, ACCELERATION, false);
        heatSense(entity, false);
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (!instance.isToggled() || entity.level().isClientSide()) {
            return;
        }
        entity.addEffect(new MobEffectInstance(TensuraMobEffects.PRESENCE_SENSE, 200, 1, false, false, false));
        entity.addEffect(new MobEffectInstance(TensuraMobEffects.AUDITORY_SENSE, 200, 0, false, false, false));
        heatSense(entity, true);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (entity.level().isClientSide()) {
            return;
        }
        String modeId = getModeId(instance, mode);
        if ("tsukuyomi.eye_of_the_moon".equals(modeId)) {
            eyeOfTheMoon(instance, entity, mode);
        } else if ("tsukuyomi.lightspeed_acceleration".equals(modeId)) {
            lightspeed(instance, entity);
        } else if ("tsukuyomi.insta_kill".equals(modeId)) {
            instaKill(instance, entity, mode);
        }
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (!"tsukuyomi.parallel_existence".equals(getModeId(instance, mode))) {
            return false;
        }
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("tensura.skill.output_number", cloneCount(instance)).withStyle(ChatFormatting.DARK_AQUA), true);
        }
        return true;
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int ticksHeld, int mode) {
        if (entity.level().isClientSide() || !"tsukuyomi.parallel_existence".equals(getModeId(instance, mode))) {
            return;
        }
        CompoundTag tag = instance.getOrCreateTag();
        if (tag.getBoolean("clonesOut")) {
            tag.putBoolean("clonesOut", false);
            instance.markDirty();
            instance.setCoolDown(mode, 10);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }
        if (!payMagicule(entity, CLONE_COST)) {
            return;
        }
        instance.addMasteryPoint(entity);
        summonClones(instance, entity, cloneCount(instance));
        tag.putBoolean("clonesOut", true);
        instance.markDirty();
    }

    @Override
    public void onScroll(ManasSkillInstance instance, LivingEntity entity, double delta, int mode) {
        if (!"tsukuyomi.parallel_existence".equals(getModeId(instance, mode))) {
            return;
        }
        CompoundTag tag = instance.getOrCreateTag();
        int next = cloneCount(instance) + (int) delta;
        if (next > 5) {
            next = 1;
        } else if (next < 1) {
            next = 5;
        }
        tag.putInt("clones", next);
        instance.markDirty();
    }

    public static void clearCloneFlag(ManasSkillInstance instance) {
        CompoundTag tag = instance.getOrCreateTag();
        if (!tag.getBoolean("clonesOut")) {
            return;
        }
        tag.putBoolean("clonesOut", false);
        instance.markDirty();
    }

    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity attacker, LivingEntity victim, DamageSource source, Changeable<Float> amount) {
        if (amount.get() == null || source.getEntity() != attacker) {
            return true;
        }
        if (instance.isToggled() && TensuraDamageHelper.isSpatialDamage(source)) {
            amount.set(amount.get() * 2.0f);
        }
        int mode = getInstaKillMode(attacker, instance);
        if (mode < 0 || instance.onCoolDown(mode) || !TensuraDamageHelper.isPhysicalAttack(source)) {
            return true;
        }
        if (!payMagicule(attacker, INSTANT_COST)) {
            return true;
        }
        executeSpiritualInstaKill(instance, attacker, victim, mode);
        instance.setCoolDown(mode, instance.isMastered(attacker) ? 5 : 10);
        instance.addMasteryPoint(attacker);
        if (!victim.isAlive()) {
            amount.set(0.0f);
            return false;
        }
        return true;
    }

    private void eyeOfTheMoon(ManasSkillInstance instance, LivingEntity entity, int mode) {
        if (entity.hasEffect(TensuraMobEffects.SHADOW_STEP)) {
            entity.removeEffect(TensuraMobEffects.SHADOW_STEP);
            play(entity, SoundEvents.ENDERMAN_TELEPORT, 0.5f);
            return;
        }
        if (!payMagicule(entity, EYE_COST)) {
            return;
        }
        instance.addMasteryPoint(entity);
        entity.addEffect(new MobEffectInstance(TensuraMobEffects.SHADOW_STEP, 6000, 0, false, false, false));
        play(entity, SoundEvents.ENDERMAN_TELEPORT, 1.0f);
    }

    private void lightspeed(ManasSkillInstance instance, LivingEntity entity) {
        if (!entity.onGround() && !entity.isInWater()) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean mastered = instance.isMastered(entity);
        double range = mastered ? 25.0 : 20.0;
        BlockHitResult hit = ObjectSelectionHelper.getPlayerPOVHitResult(level, entity, ClipContext.Fluid.NONE, range);
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        Block barrier = BuiltInRegistries.BLOCK.get(LABYRINTH_BARRIER);
        if (level.getBlockState(pos).is(barrier)) {
            play(entity, SoundEvents.ENDERMAN_TELEPORT, 1.0f);
            return;
        }
        Vec3 floor = ObjectSelectionHelper.getFloorPos(pos);
        if (!level.getBlockState(pos).blocksMotion()) {
            floor = ObjectSelectionHelper.getFloorPos(pos.above());
        }
        if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(floor))) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("tensura.skill.teleport.out_border").withStyle(ChatFormatting.RED), false);
            }
            return;
        }
        if (!payAura(entity, DASH_AURA)) {
            return;
        }
        instance.addMasteryPoint(entity);
        Vec3 start = entity.position().add(0.0, entity.getBbHeight() / 2.0, 0.0);
        Vec3 travel = floor.subtract(start);
        int steps = Math.max(1, (int) Math.floor(travel.length()));
        Vec3 step = travel.normalize();
        float bonus = mastered ? 300.0f : 75.0f;
        float attack = (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE) + bonus;
        double reach = Math.max(2.0, entity.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
        for (int i = 1; i < steps; i++) {
            Vec3 point = start.add(step.scale(i));
            server.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
            server.sendParticles(ParticleTypes.FIREWORK, point.x, point.y, point.z, 2, 0.2, 0.2, 0.2, 0.0);
            AABB box = new AABB(BlockPos.containing(point)).inflate(reach);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, living -> living != entity && living.isAlive() && !living.isAlliedTo(entity))) {
                if (!target.hurt(entity instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().mobAttack(entity), attack)) {
                    continue;
                }
                float spiritual = (float) target.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);
                if (!mastered) {
                    TensuraDamageHelper.directSpiritualHurt(target, entity, spiritual * 0.5f);
                } else if (has(target, ResistanceSkills.SPIRITUAL_ATTACK_NULLIFICATION.get()) && entity.getRandom().nextFloat() <= 0.25f) {
                    TensuraDamageHelper.directSpiritualHurt(target, entity, spiritual * 0.5f);
                } else if (has(target, ResistanceSkills.SPIRITUAL_ATTACK_RESISTANCE.get()) && entity.getRandom().nextFloat() <= 0.5f) {
                    TensuraDamageHelper.directSpiritualHurt(target, entity, spiritual * 0.5f);
                } else {
                    TensuraDamageHelper.directSpiritualHurt(target, entity, Float.MAX_VALUE);
                }
                if (entity instanceof Player attacker) {
                    entity.getMainHandItem().hurtEnemy(target, attacker);
                }
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, entity.getSoundSource(), 1.0f, 1.0f);
                server.getChunkSource().broadcast(entity, new ClientboundAnimatePacket(entity, 4));
            }
        }
        entity.teleportTo(floor.x, floor.y, floor.z);
        entity.resetFallDistance();
        entity.swing(InteractionHand.MAIN_HAND, true);
        play(entity, SoundEvents.ENDERMAN_TELEPORT, 1.0f);
    }

    private static void summonClones(ManasSkillInstance instance, LivingEntity owner, int count) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }
        EntityType<?> type = EntityType.byString("tensura:clone").orElse(null);
        if (type == null || count <= 0) {
            return;
        }
        ExistenceStorage ownerExistence = ShadowArmy.existence(owner);
        double each = ownerExistence == null ? 0.0 : ownerExistence.getEP() * 0.5 / count;
        play(owner, SoundEvents.EVOKER_CAST_SPELL, 1.0f);
        level.sendParticles(ParticleTypes.POOF, owner.getX(), owner.getY() + 1.0, owner.getZ(), 24, 0.4, 0.6, 0.4, 0.02);
        for (int i = 0; i < count; i++) {
            if (!(type.create(level) instanceof CloneEntity clone)) {
                return;
            }
            if (owner instanceof Player player) {
                clone.tame(player);
            }
            clone.setSkill(instance);
            clone.copyStatsAndSkills(owner, CloneEntity.CopySkill.ALL, false);
            clone.moveTo(owner.position());
            ExistenceStorage cloneExistence = ShadowArmy.existence(clone);
            if (cloneExistence != null) {
                cloneExistence.setEP(Math.round(each));
                cloneExistence.markDirty();
            }
            level.addFreshEntity(clone);
        }
    }

    private void instaKill(ManasSkillInstance instance, LivingEntity entity, int mode) {
        if (instance.onCoolDown(mode)) {
            return;
        }
        boolean mastered = instance.isMastered(entity);
        double range = mastered ? 25.0 : 20.0;
        LivingEntity target = SkillGrant.look(entity, range);
        if (target == null && entity.level() instanceof ServerLevel server) {
            target = findLookTarget(server, entity, range);
        }
        if (target == null) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.no_target").withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        if (!payMagicule(entity, INSTANT_COST)) {
            return;
        }
        executeSpiritualInstaKill(instance, entity, target, mode);
        if (target.isAlive()) {
            DamageSource source = entity instanceof Player player ? entity.level().damageSources().playerAttack(player) : entity.level().damageSources().mobAttack(entity);
            target.hurt(source, (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE) + (mastered ? 200.0f : 100.0f));
            if (entity instanceof Player player) {
                entity.getMainHandItem().hurtEnemy(target, player);
            }
        }
        instance.setCoolDown(mode, mastered ? 5 : 10);
        instance.addMasteryPoint(entity);
    }

    private void executeSpiritualInstaKill(ManasSkillInstance instance, LivingEntity attacker, LivingEntity victim, int mode) {
        float spiritual = (float) victim.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);
        boolean mastered = instance.isMastered(attacker);
        if (!mastered && has(victim, ResistanceSkills.SPIRITUAL_ATTACK_NULLIFICATION.get())) {
            TensuraDamageHelper.directSpiritualHurt(victim, attacker, 10_000.0f + spiritual * 0.25f);
        } else if (has(victim, ResistanceSkills.SPIRITUAL_ATTACK_RESISTANCE.get())) {
            TensuraDamageHelper.directSpiritualHurt(victim, attacker, 5_000.0f + spiritual * 0.5f);
        } else {
            TensuraDamageHelper.directSpiritualHurt(victim, attacker, Float.MAX_VALUE);
        }
        Level level = attacker.level();
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + victim.getBbHeight() / 2.0, victim.getZ(), 25, 0.4, 0.4, 0.4, 0.05);
            server.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() / 2.0, victim.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
            server.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + victim.getBbHeight() / 2.0, victim.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            server.getChunkSource().broadcast(attacker, new ClientboundAnimatePacket(attacker, 4));
        }
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);
        level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.2f);
        attacker.swing(InteractionHand.MAIN_HAND, true);
    }

    private static LivingEntity findLookTarget(ServerLevel level, LivingEntity player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        LivingEntity best = null;
        double bestDot = 0.55;
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && !e.isAlliedTo(player))) {
            Vec3 to = other.getEyePosition().subtract(eyePos);
            double dist = to.length();
            if (dist <= 0.001 || dist > range) {
                continue;
            }
            double dot = to.normalize().dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = other;
            }
        }
        return best;
    }

    private int getInstaKillMode(LivingEntity entity, ManasSkillInstance instance) {
        if (this.isInSlot(entity, instance, 1)) {
            return 1;
        }
        try {
            AbilityStorage storage = ((StorageHolder) (Object) entity).manasCore$getStorage(AbilityStorage.getKey());
            if (storage != null) {
                for (AbilitySlot slot : storage.getAbilitySlots()) {
                    if (slot != null && slot.getSkill() == this && "tsukuyomi.insta_kill".equals(getModeId(instance, slot.getMode()))) {
                        return slot.getMode();
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
        return -1;
    }

    private static int cloneCount(ManasSkillInstance instance) {
        int count = instance.getOrCreateTag().getInt("clones");
        return count >= 1 && count <= 5 ? count : 5;
    }

    private static void heatSense(LivingEntity entity, boolean active) {
        var attribute = entity.getAttribute(TensuraAttributes.HEAT_SENSE_RADIUS);
        if (attribute == null) {
            return;
        }
        if (!active) {
            attribute.removeModifier(HEAT_SENSE);
            return;
        }
        attribute.addOrUpdateTransientModifier(new AttributeModifier(
                HEAT_SENSE, SenseHeatSourceSkill.CONFIG.heatRadius, AttributeModifier.Operation.ADD_VALUE));
    }

    private static boolean has(LivingEntity entity, ManasSkill skill) {
        return skill != null && SkillUtils.hasSkill(entity, skill);
    }

    private static boolean payMagicule(LivingEntity entity, double cost) {
        if (entity instanceof Player player && player.isCreative()) {
            return true;
        }
        return SkillGrant.pay(entity, cost);
    }

    private static boolean payAura(LivingEntity entity, double cost) {
        if (entity instanceof Player player && player.isCreative()) {
            return true;
        }
        ExistenceStorage existence = ShadowArmy.existence(entity);
        if (existence == null || existence.getAura() < cost) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.lack_aura").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        existence.setAura(existence.getAura() - cost);
        existence.markDirty();
        return true;
    }

    private static void play(LivingEntity entity, net.minecraft.sounds.SoundEvent sound, float pitch) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, 1.0f, pitch);
    }
}
