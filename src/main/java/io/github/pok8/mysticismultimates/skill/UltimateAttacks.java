package io.github.pok8.mysticismultimates.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.pok8.mysticismultimates.entity.SkillAnimationEntity;
import io.github.pok8.mysticismultimates.entity.SkillAnimationVariant;
import io.github.pok8.mysticismultimates.registry.UltimateEntities;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class UltimateAttacks {
    private UltimateAttacks() {
    }

    public static boolean cast(DifficultUltimateSkill skill, ManasSkillInstance instance, Player player, int mode) {
        String modeId = skill.getModeId(instance, mode);
        if (SkillHits.tryCast(instance, player, mode, modeId)) {
            return true;
        }
        if (!modeId.startsWith("antaeus.") && !modeId.startsWith("gilgamesh.")) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level) || instance.onCoolDown(mode)) {
            return true;
        }
        boolean mastered = instance.isMastered(player);
        boolean fired = switch (modeId) {
            case "antaeus.oppress" -> oppress(level, player, mastered);
            case "antaeus.attract_blue" -> orb(level, player, SkillAnimationVariant.ANTAEUS_BLUE, 1.15, 200.0f, 0.05, 3.0, 80, 2000.0);
            case "antaeus.repel_red" -> orb(level, player, SkillAnimationVariant.ANTAEUS_RED, 1.15, 400.0f, -0.05, 3.0, 80, 2000.0);
            case "antaeus.hollow_purple" -> orb(level, player, SkillAnimationVariant.ANTAEUS_PURPLE, 1.5, 1000.0f, 0.05, 5.0, 100, 5000.0);
            case "antaeus.bleve" -> bleve(level, player, mastered);
            case "antaeus.flicker" -> flicker(level, player);
            case "gilgamesh.territory" -> orb(level, player, SkillAnimationVariant.GATES, 0.0, 100.0f, 0.0, 4.0, 160, 2500.0);
            case "gilgamesh.ea" -> orb(level, player, SkillAnimationVariant.EA, 0.0, 150.0f, 0.15, 5.0, 80, 7500.0);
            case "gilgamesh.axe" -> cone(level, player, 4.0, 80.0f, 0.6, 1000.0, TensuraDamageTypes.SEVERANCE);
            case "gilgamesh.enuma_elish" -> cone(level, player, 30.0, 1000.0f, 1.5, 12500.0, TensuraDamageTypes.GRAVITY_EXPLODE);
            default -> false;
        };
        if (!fired) {
            return true;
        }
        int seconds = mastered ? 5 : 10;
        if (modeId.equals("antaeus.hollow_purple") || modeId.equals("gilgamesh.ea")) {
            seconds = mastered ? 10 : 20;
        } else if (modeId.equals("antaeus.bleve") || modeId.equals("gilgamesh.territory")) {
            seconds = mastered ? 15 : 30;
        } else if (modeId.equals("gilgamesh.enuma_elish")) {
            seconds = mastered ? 20 : 40;
        } else if (modeId.equals("antaeus.flicker")) {
            seconds = 3;
        }
        instance.setCoolDown(mode, seconds);
        instance.addMasteryPoint(player);
        return true;
    }

    private static boolean oppress(ServerLevel level, Player player, boolean mastered) {
        if (!pay(player, 1400.0)) {
            return false;
        }
        float damage = mastered ? 500.0f : 250.0f;
        var source = TensuraDamageTypes.getEntityDamageSource(level, TensuraDamageTypes.GRAVITY_PRESS, player);
        for (LivingEntity target : nearby(level, player, 8.0)) {
            TensuraDamageHelper.hurtSplitElemental(target, source, damage * 0.9f, damage / 10.0f);
            target.addEffect(new MobEffectInstance(effect(TensuraMobEffects.OPPRESSION.get()), 600, 0), player);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, -0.4, 0.0));
            target.hasImpulse = true;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 0.5f);
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 40, 2.0, 0.5, 2.0, 0.02);
        return true;
    }

    private static boolean bleve(ServerLevel level, Player player, boolean mastered) {
        if (!pay(player, 5000.0)) {
            return false;
        }
        float damage = mastered ? 400.0f : 200.0f;
        var source = TensuraDamageTypes.getEntityDamageSource(level, TensuraDamageTypes.GRAVITY_EXPLODE, player);
        for (LivingEntity target : nearby(level, player, 10.0)) {
            TensuraDamageHelper.hurtSplitElemental(target, source, damage * 0.9f, damage / 10.0f);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 2.0, 0.0));
            target.hasImpulse = true;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
        return true;
    }

    private static boolean flicker(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 delta = look.scale(8.0);
        if (!level.noCollision(player, player.getBoundingBox().move(delta))) {
            delta = look.scale(4.0);
            if (!level.noCollision(player, player.getBoundingBox().move(delta))) {
                return false;
            }
        }
        if (!pay(player, 300.0)) {
            return false;
        }
        player.teleportTo(player.getX() + delta.x, player.getY() + delta.y, player.getZ() + delta.z);
        player.getActiveEffects().stream()
                .filter(active -> active.getEffect().value() == TensuraMobEffects.WARPING.get())
                .map(MobEffectInstance::getEffect)
                .findFirst()
                .ifPresent(player::removeEffect);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
        return true;
    }

    private static boolean orb(ServerLevel level, Player player, SkillAnimationVariant variant, double speed, float damage, double pull, double radius, int life, double cost) {
        if (!pay(player, cost)) {
            return false;
        }
        SkillAnimationEntity entity = new SkillAnimationEntity(UltimateEntities.type(variant), level);
        Vec3 look = player.getLookAngle();
        double ahead = speed > 0.0 ? 2.0 : 3.0;
        entity.moveTo(player.getX() + look.x * ahead, player.getEyeY() - 0.4, player.getZ() + look.z * ahead, player.getYRot(), player.getXRot());
        if (speed > 0.0) {
            entity.setDeltaMovement(look.scale(speed));
        }
        entity.arm(player.getUUID(), damage, pull, radius, life);
        level.addFreshEntity(entity);
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 0.6f);
        return true;
    }

    private static boolean cone(ServerLevel level, Player player, double range, float damage, double knock, double cost, net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type) {
        if (!pay(player, cost)) {
            return false;
        }
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        var source = TensuraDamageTypes.getEntityDamageSource(level, type, player);
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, living -> living.isAlive() && living != player && !player.isAlliedTo(living))) {
            Vec3 to = target.getEyePosition().subtract(start);
            double along = to.dot(look);
            if (along < 0.0 || along > range) {
                continue;
            }
            if (start.add(look.scale(along)).distanceToSqr(target.getEyePosition()) > 4.0) {
                continue;
            }
            TensuraDamageHelper.hurtSplitElemental(target, source, damage * 0.9f, damage / 10.0f);
            target.setDeltaMovement(target.getDeltaMovement().add(look.scale(knock)).add(0.0, 0.3, 0.0));
            target.hasImpulse = true;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.5f);
        for (int step = 1; step <= (int) range; step += 2) {
            Vec3 point = start.add(look.scale(step));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        return true;
    }

    static java.util.List<LivingEntity> nearby(ServerLevel level, Player player, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), living -> living.isAlive() && living != player && !player.isAlliedTo(living));
    }

    private static Holder<MobEffect> effect(MobEffect effect) {
        return Holder.direct(effect);
    }

    static boolean pay(Player player, double cost) {
        if (!(player instanceof ServerPlayer server)) {
            return false;
        }
        ExistenceStorage existence = ((StorageHolder) (Object) server).manasCore$getStorage(ExistenceStorage.getKey());
        if (existence == null || existence.getMagicule() < cost) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("mysticism_ultimates.skill.lack_magicule", (long) cost), true);
            return false;
        }
        existence.setMagicule(existence.getMagicule() - cost);
        existence.markDirty();
        ((StorageHolder) (Object) server).manasCore$sync();
        return true;
    }
}
