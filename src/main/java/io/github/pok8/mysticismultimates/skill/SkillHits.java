package io.github.pok8.mysticismultimates.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class SkillHits {
    private enum Shape { BURST, CONE, BEAM, DASH, BLINK, SELF, ALLY, DRAIN, SINGLE, CLEANSE, CHECKPOINT }

    private record Hit(Shape shape, double cost, float damage, double range, double knock, int cooldown,
                       ResourceKey<DamageType> type, Supplier<Holder<MobEffect>> effect, int effectTicks, int amplifier) {
    }

    private static final Map<String, Hit> HITS = new HashMap<>();

    static {
        burst("amaterasu.haze_black_flame", 5000, 300, 8, 0.3, TensuraDamageTypes.BLACK_FLAME, tensura(TensuraMobEffects.BLACK_BURN), 200);
        self("amaterasu.movement_communication", 1000, vanilla(MobEffects.MOVEMENT_SPEED), 400, 2);
        dash("amaterasu.prominence_acceleration", 10000, 200, 12, TensuraDamageTypes.FIRE_ELEMENTAL);
        self("amaterasu.targeting_communication", 1000, tensura(TensuraMobEffects.ENEMY_SEARCH), 600, 0);
        burst("amaterasu.will_control", 8000, 40, 10, 0.0, TensuraDamageTypes.MIND_CRUSH, tensura(TensuraMobEffects.MIND_CONTROL), 200);

        self("ame_no_uzume.performance", 1500, tensura(TensuraMobEffects.INSPIRATION), 400, 1);
        ally("ame_no_uzume.encore", 2000, 12, 8, tensura(TensuraMobEffects.INSPIRATION), 300, 0);
        burst("ame_no_uzume.spotlight", 3000, 25, 10, 0.2, TensuraDamageTypes.LIGHT_ELEMENTAL, tensura(TensuraMobEffects.FLASHED_BLINDNESS), 80);
        beam("ame_no_uzume.ornate_star", 8000, 80, 18, TensuraDamageTypes.HOLY_DAMAGE, null, 0);

        single("antithesis.redirect", 2000, 120, 16, TensuraDamageTypes.REFLECTED, null, 0);
        single("antithesis.malice_exchange", 4000, 80, 16, TensuraDamageTypes.CURSE, tensura(TensuraMobEffects.BURDEN), 200);
        drain("antithesis.vital_shift", 3000, 60, 8, TensuraDamageTypes.MAGIC_GENERIC);
        burst("antithesis.absolute_antithesis", 10000, 400, 12, 0.8, TensuraDamageTypes.SEVERANCE, null, 0);

        drain("asmodeus.drain", 2000, 100, 8, TensuraDamageTypes.BLOOD_DRAIN);
        self("asmodeus.invigorate", 2000, vanilla(MobEffects.DAMAGE_BOOST), 400, 2);
        beam("asmodeus.death_blessing", 8000, 200, 16, TensuraDamageTypes.DEATH_BLESS, tensura(TensuraMobEffects.CURSE), 160);
        burst("asmodeus.memory_end_requiem", 12000, 350, 10, 0.4, TensuraDamageTypes.MIND_REQUIEM, tensura(TensuraMobEffects.MIND_CONTROL), 100);
        beam("asmodeus.angel_of_death", 10000, 300, 22, TensuraDamageTypes.DEATH_BLESS, null, 0);

        self("belphegor.rest", 1000, tensura(TensuraMobEffects.REST), 400, 0);
        drain("belphegor.deprive", 3000, 100, 8, TensuraDamageTypes.ENERGY_DRAIN);
        burst("belphegor.fallen_hypno", 4000, 40, 10, 0.0, TensuraDamageTypes.DROWSY_DEATH, tensura(TensuraMobEffects.SLEEP), 200);
        cone("belphegor.fallen_strike", 4000, 150, 6, TensuraDamageTypes.SEVERANCE);
        beam("belphegor.fallen_thanatos", 8000, 300, 16, TensuraDamageTypes.DEATH_BLESS, null, 0);
        burst("belphegor.fallen_catastrophe", 15000, 400, 12, 1.0, TensuraDamageTypes.GRAVITY_EXPLODE, tensura(TensuraMobEffects.BURDEN), 200);

        burst("bushyasta.inaction", 3000, 50, 8, 0.0, TensuraDamageTypes.MAGIC_GENERIC, tensura(TensuraMobEffects.PARALYSIS), 120);
        burst("bushyasta.stagnate", 4000, 40, 8, 0.0, TensuraDamageTypes.MAGIC_GENERIC, tensura(TensuraMobEffects.MOVEMENT_INTERFERENCE), 200);
        self("bushyasta.stillness_of_ones_heart", 2000, vanilla(MobEffects.DAMAGE_RESISTANCE), 400, 2);
        self("bushyasta.stasis_coat", 5000, tensura(TensuraMobEffects.PROTECTION), 300, 1);
        burst("bushyasta.time_manipulation", 12000, 80, 12, 0.0, TensuraDamageTypes.SPACE_ELEMENTAL, tensura(TensuraMobEffects.PARALYSIS), 80);

        burst("dionysus.destroyer_haki", 5000, 200, 10, 0.6, TensuraDamageTypes.FEAR, tensura(TensuraMobEffects.INSANITY), 160);
        blink("dionysus.dimension_hopper", 4000, 16);
        beam("dionysus.turn_null", 20000, 500, 24, TensuraDamageTypes.DISINTEGRATION, null, 0);

        self("galileo.enhanced_danger_detection", 500, tensura(TensuraMobEffects.ENEMY_SEARCH), 600, 0);
        self("galileo.enhanced_treasure_detection", 500, tensura(TensuraMobEffects.PRESENCE_SENSE), 600, 0);
        self("galileo.eye_of_the_allfather", 2000, tensura(TensuraMobEffects.FUTURE_VISION), 400, 0);
        self("galileo.multidimensional_barrier", 5000, tensura(TensuraMobEffects.MAGIC_BARRIER), 300, 1);


        cone("ignis.disassemble", 3000, 120, 8, TensuraDamageTypes.SEVERANCE);
        cone("ignis.stone", 4000, 60, 8, TensuraDamageTypes.EARTH_ELEMENTAL);
        burst("ignis.fuse", 5000, 200, 6, 0.5, TensuraDamageTypes.FIRE_ELEMENTAL, null, 0);
        dash("ignis.explosion_dash", 6000, 250, 10, TensuraDamageTypes.GRAVITY_EXPLODE);
        single("ignis.bites_the_dust", 10000, 400, 20, TensuraDamageTypes.GRAVITY_EXPLODE, vanilla(MobEffects.WITHER), 100);

        burst("invictus.conquest", 4000, 200, 8, 0.7, TensuraDamageTypes.HOLY_DAMAGE, tensura(TensuraMobEffects.FEAR), 120);
        cone("invictus.relentless_echo", 5000, 150, 14, TensuraDamageTypes.SOUND_BLAST);
        beam("invictus.glorious_verdict", 8000, 300, 20, TensuraDamageTypes.HOLY_DAMAGE, null, 0);
        burst("invictus.kaisers_dominion", 6000, 80, 10, 0.2, TensuraDamageTypes.FEAR, tensura(TensuraMobEffects.FEAR), 160);
        burst("invictus.invincible_victory", 15000, 500, 14, 1.0, TensuraDamageTypes.HOLY_DAMAGE, null, 0);

        dash("laverna.blitz", 3000, 150, 12, TensuraDamageTypes.DARKNESS_ELEMENTAL);
        self("laverna.shadow_merge", 2000, tensura(TensuraMobEffects.PRESENCE_CONCEALMENT), 300, 0);
        burst("laverna.shadow_garden", 6000, 200, 10, 0.3, TensuraDamageTypes.DARKNESS_ELEMENTAL, tensura(TensuraMobEffects.TRUE_BLINDNESS), 80);
        beam("laverna.euclidean_dystopia", 12000, 350, 20, TensuraDamageTypes.SPACE_ELEMENTAL, tensura(TensuraMobEffects.SPATIAL_BLOCKADE), 100);

        burst("lucifer.akasha", 12000, 400, 12, 0.4, TensuraDamageTypes.SPACE_ELEMENTAL, null, 0);
        beam("lucifer.arroganz", 20000, 600, 30, TensuraDamageTypes.DISINTEGRATION, null, 0);

        checkpoint("mephisto.checkpoint", 1000);
        burst("mephisto.dream_end", 8000, 200, 10, 0.2, TensuraDamageTypes.DROWSY_DEATH, tensura(TensuraMobEffects.SLEEP), 200);
        drain("mephisto.life_domination", 6000, 100, 8, TensuraDamageTypes.ENERGY_DRAIN);

        burst("michael.true_echo_counter", 4000, 100, 6, 0.6, TensuraDamageTypes.REFLECTED, null, 0);
        cone("michael.true_echo_reflection", 5000, 200, 12, TensuraDamageTypes.REFLECTED);
        burst("michael.regalia_dominion", 8000, 80, 10, 0.2, TensuraDamageTypes.FEAR, tensura(TensuraMobEffects.BURDEN), 200);
        burst("michael.ultimate_dominion", 12000, 300, 10, 0.4, TensuraDamageTypes.HOLY_DAMAGE, tensura(TensuraMobEffects.ANTI_SKILL), 100);
        beam("michael.armageddon", 20000, 800, 24, TensuraDamageTypes.HOLY_DAMAGE, null, 0);

        cone("oizys.gust", 2000, 120, 10, TensuraDamageTypes.WIND_ELEMENTAL);
        burst("oizys.baneful_requiem", 6000, 180, 10, 0.2, TensuraDamageTypes.CURSE, tensura(TensuraMobEffects.FATAL_POISON), 120);
        burst("oizys.turbulent_force", 8000, 250, 12, 1.2, TensuraDamageTypes.WIND_ELEMENTAL, null, 0);
        burst("oizys.circus_show", 5000, 100, 10, 0.0, TensuraDamageTypes.MIND_CRUSH, tensura(TensuraMobEffects.CONFUSION), 200);

        cone("sandalphon.assassination", 3000, 250, 5, TensuraDamageTypes.SEVERANCE);
        cleanse("sandalphon.dispel", 2000, 8);
        cone("sandalphon.eraser", 5000, 200, 8, TensuraDamageTypes.DISINTEGRATION);
        beam("sandalphon.judgement", 8000, 400, 18, TensuraDamageTypes.HOLY_DAMAGE, null, 0);
        burst("sandalphon.necrosis", 6000, 150, 8, 0.0, TensuraDamageTypes.FATAL_POISON, tensura(TensuraMobEffects.FATAL_POISON), 160);
        single("sandalphon.remove", 10000, 600, 12, TensuraDamageTypes.SOUL_SCATTER, null, 0);

        ally("sariel.backup", 4000, 20, 10, tensura(TensuraMobEffects.ALLY_BOOST), 300, 0);
        self("sariel.fortitude", 3000, vanilla(MobEffects.DAMAGE_RESISTANCE), 400, 3);
        self("sariel.return", 5000, tensura(TensuraMobEffects.INSTANT_REGENERATION), 40, 1);
        burst("sariel.sacred_haki", 8000, 200, 10, 0.5, TensuraDamageTypes.FEAR, tensura(TensuraMobEffects.FEAR), 160);

        self("satanael.breeder", 2000, tensura(TensuraMobEffects.MAGICULE_REGENERATION), 400, 1);
        self("satanael.enrage", 3000, tensura(TensuraMobEffects.RAMPAGE), 400, 1);
        dash("satanael.stampede", 8000, 300, 14, TensuraDamageTypes.FIRE_ELEMENTAL);

        checkpoint("sephirot.checkpoint", 1000);
        drain("sephirot.dream_eater", 4000, 120, 8, TensuraDamageTypes.ENERGY_DRAIN);
        burst("sephirot.hypnosis", 3000, 20, 10, 0.0, TensuraDamageTypes.MIND_CRUSH, tensura(TensuraMobEffects.HYPNOSIS), 200);
        drain("sephirot.life_domination", 6000, 100, 8, TensuraDamageTypes.ENERGY_DRAIN);
        self("sephirot.restore_life", 8000, tensura(TensuraMobEffects.INSTANT_REGENERATION), 40, 2);

        cone("takemikazuchi.channel", 4000, 300, 6, TensuraDamageTypes.LIGHTNING);

        beam("uriel.absolute_severance", 8000, 400, 16, TensuraDamageTypes.SEVERANCE, tensura(TensuraMobEffects.SEVERANCE_BLADE), 40);
        burst("uriel.fatal_imprison", 10000, 200, 8, 0.0, TensuraDamageTypes.SPACE_ELEMENTAL, tensura(TensuraMobEffects.INFINITE_IMPRISONMENT), 100);
        burst("uriel.gravity_manipulation", 5000, 150, 10, -0.35, TensuraDamageTypes.GRAVITY_ELEMENTAL, null, 0);
        burst("uriel.imaginary_space", 8000, 100, 10, 0.0, TensuraDamageTypes.SPACE_ELEMENTAL, tensura(TensuraMobEffects.SPATIAL_BLOCKADE), 160);
        burst("uriel.infinity_prison", 15000, 700, 8, 0.0, TensuraDamageTypes.SPACE_ELEMENTAL, tensura(TensuraMobEffects.INFINITE_IMPRISONMENT), 200);
        burst("uriel.insulated_prison", 6000, 100, 8, 0.0, TensuraDamageTypes.MAGIC_GENERIC, tensura(TensuraMobEffects.MAGIC_INTERFERENCE), 200);
        cone("uriel.law_manipulation", 8000, 250, 10, TensuraDamageTypes.DISINTEGRATION);
        burst("uriel.nova_break", 15000, 500, 14, 0.8, TensuraDamageTypes.HOLY_DAMAGE, tensura(TensuraMobEffects.DISINTEGRATING), 60);
        self("uriel.universal_barrier", 8000, tensura(TensuraMobEffects.MAGIC_BARRIER), 300, 2);

        burst("xezbeth.deception", 3000, 80, 10, 0.0, TensuraDamageTypes.MIND_CRUSH, tensura(TensuraMobEffects.CONFUSION), 200);
        self("xezbeth.display", 1500, vanilla(MobEffects.GLOWING), 400, 0);
    }

    private SkillHits() {
    }

    public static boolean tryCast(ManasSkillInstance instance, Player player, int mode, String modeId) {
        Hit hit = HITS.get(modeId);
        if (hit == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (instance.onCoolDown(mode)) {
            return true;
        }
        boolean mastered = instance.isMastered(player);
        float damage = mastered ? hit.damage * 2.0f : hit.damage;
        if (!fire(instance, level, player, hit, damage)) {
            return true;
        }
        int seconds = mastered ? Math.max(2, hit.cooldown / 2) : hit.cooldown;
        instance.setCoolDown(mode, seconds);
        instance.addMasteryPoint(player);
        return true;
    }

    private static boolean fire(ManasSkillInstance instance, ServerLevel level, Player player, Hit hit, float damage) {
        return switch (hit.shape) {
            case BURST, CONE, BEAM -> area(level, player, hit, damage);
            case DASH -> dash(level, player, hit, damage);
            case BLINK -> blink(level, player, hit);
            case SELF -> self(level, player, hit, damage);
            case ALLY -> ally(level, player, hit);
            case DRAIN -> drain(level, player, hit, damage);
            case SINGLE -> single(level, player, hit, damage);
            case CLEANSE -> cleanse(level, player, hit);
            case CHECKPOINT -> checkpoint(instance, level, player, hit);
        };
    }

    private static boolean area(ServerLevel level, Player player, Hit hit, float damage) {
        if (!UltimateAttacks.pay(player, hit.cost)) {
            return false;
        }
        List<LivingEntity> targets = hit.shape == Shape.BURST
                ? UltimateAttacks.nearby(level, player, hit.range)
                : aimed(level, player, hit.range);
        for (LivingEntity target : targets) {
            strike(level, player, target, hit, damage, hit.shape != Shape.BURST);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.6f);
        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 24, hit.range / 4.0, 0.4, hit.range / 4.0, 0.1);
        return true;
    }

    private static boolean dash(ServerLevel level, Player player, Hit hit, float damage) {
        Vec3 delta = step(level, player, hit.range);
        if (delta == null || !UltimateAttacks.pay(player, hit.cost)) {
            return false;
        }
        player.teleportTo(player.getX() + delta.x, player.getY() + delta.y, player.getZ() + delta.z);
        for (LivingEntity target : UltimateAttacks.nearby(level, player, 3.0)) {
            strike(level, player, target, hit, damage, true);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
        return true;
    }

    private static boolean blink(ServerLevel level, Player player, Hit hit) {
        Vec3 delta = step(level, player, hit.range);
        if (delta == null || !UltimateAttacks.pay(player, hit.cost)) {
            return false;
        }
        player.teleportTo(player.getX() + delta.x, player.getY() + delta.y, player.getZ() + delta.z);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
        return true;
    }

    private static boolean self(ServerLevel level, Player player, Hit hit, float damage) {
        if (!UltimateAttacks.pay(player, hit.cost)) {
            return false;
        }
        apply(player, player, hit);
        if (damage >= 40.0f) {
            player.heal(damage / 10.0f);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.7f, 1.2f);
        return true;
    }

    private static boolean ally(ServerLevel level, Player player, Hit hit) {
        if (!UltimateAttacks.pay(player, hit.cost)) {
            return false;
        }
        apply(player, player, hit);
        player.heal(hit.damage);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(hit.range), entity -> entity.isAlive() && entity != player && player.isAlliedTo(entity))) {
            apply(player, living, hit);
            living.heal(hit.damage);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.4f);
        return true;
    }

    private static boolean drain(ServerLevel level, Player player, Hit hit, float damage) {
        if (!UltimateAttacks.pay(player, hit.cost)) {
            return false;
        }
        List<LivingEntity> targets = UltimateAttacks.nearby(level, player, hit.range);
        for (LivingEntity target : targets) {
            strike(level, player, target, hit, damage, false);
        }
        player.heal(Math.min(player.getMaxHealth(), damage * 0.15f * Math.max(1, targets.size())));
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.4f, 1.4f);
        return true;
    }

    private static boolean single(ServerLevel level, Player player, Hit hit, float damage) {
        LivingEntity target = nearest(level, player, hit.range);
        if (target == null) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.no_target"), true);
            return false;
        }
        if (!UltimateAttacks.pay(player, hit.cost)) {
            return false;
        }
        strike(level, player, target, hit, damage, true);
        if (hit.type == TensuraDamageTypes.REFLECTED || hit.type == TensuraDamageTypes.CURSE) {
            player.heal(Math.min(player.getMaxHealth() * 0.2f, damage * 0.1f));
        }
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }

    private static boolean cleanse(ServerLevel level, Player player, Hit hit) {
        if (!UltimateAttacks.pay(player, hit.cost)) {
            return false;
        }
        for (LivingEntity target : UltimateAttacks.nearby(level, player, hit.range)) {
            List<Holder<MobEffect>> remove = new ArrayList<>();
            for (MobEffectInstance active : target.getActiveEffects()) {
                if (active.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                    remove.add(active.getEffect());
                }
            }
            remove.forEach(target::removeEffect);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    private static boolean checkpoint(ManasSkillInstance instance, ServerLevel level, Player player, Hit hit) {
        CompoundTag tag = instance.getOrCreateTag();
        if (player.isShiftKeyDown()) {
            if (!tag.contains("checkpointX")) {
                player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.checkpoint_missing"), true);
                return false;
            }
            String saved = tag.getString("checkpointDim");
            if (!saved.equals(level.dimension().location().toString())) {
                player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.checkpoint_dimension"), true);
                return false;
            }
            if (!UltimateAttacks.pay(player, hit.cost)) {
                return false;
            }
            player.teleportTo(tag.getDouble("checkpointX"), tag.getDouble("checkpointY"), tag.getDouble("checkpointZ"));
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);
            return true;
        }
        if (!UltimateAttacks.pay(player, hit.cost)) {
            return false;
        }
        tag.putDouble("checkpointX", player.getX());
        tag.putDouble("checkpointY", player.getY());
        tag.putDouble("checkpointZ", player.getZ());
        tag.putString("checkpointDim", level.dimension().location().toString());
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.checkpoint_saved"), true);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7f, 1.4f);
        return true;
    }

    private static void strike(ServerLevel level, Player player, LivingEntity target, Hit hit, float damage, boolean alongLook) {
        var source = TensuraDamageTypes.getEntityDamageSource(level, hit.type, player);
        TensuraDamageHelper.hurtSplitElemental(target, source, damage * 0.9f, damage / 10.0f);
        if (hit.type == TensuraDamageTypes.FIRE_ELEMENTAL) {
            target.setRemainingFireTicks(80);
        }
        apply(player, target, hit);
        Vec3 push = alongLook ? player.getLookAngle() : target.position().subtract(player.position());
        if (hit.knock != 0.0 && push.lengthSqr() > 1.0E-4) {
            double scale = hit.knock < 0.0 ? hit.knock : hit.knock;
            Vec3 motion = hit.knock < 0.0 ? player.position().subtract(target.position()) : push;
            target.setDeltaMovement(target.getDeltaMovement().add(motion.normalize().scale(Math.abs(scale))));
            target.hasImpulse = true;
        }
    }

    private static void apply(Player player, LivingEntity target, Hit hit) {
        if (hit.effect != null && hit.effectTicks > 0) {
            target.addEffect(new MobEffectInstance(hit.effect.get(), hit.effectTicks, hit.amplifier), player);
        }
    }

    private static List<LivingEntity> aimed(ServerLevel level, Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity target : UltimateAttacks.nearby(level, player, range)) {
            Vec3 to = target.getEyePosition().subtract(start);
            double along = to.dot(look);
            if (along < 0.0 || along > range) {
                continue;
            }
            if (start.add(look.scale(along)).distanceToSqr(target.getEyePosition()) <= 4.0) {
                found.add(target);
            }
        }
        return found;
    }

    private static LivingEntity nearest(ServerLevel level, Player player, double range) {
        Vec3 look = player.getLookAngle();
        LivingEntity best = null;
        double bestDot = 0.55;
        for (LivingEntity target : UltimateAttacks.nearby(level, player, range)) {
            Vec3 to = target.getEyePosition().subtract(player.getEyePosition());
            if (to.lengthSqr() < 1.0E-4) {
                continue;
            }
            double dot = to.normalize().dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = target;
            }
        }
        return best;
    }

    private static Vec3 step(ServerLevel level, Player player, double distance) {
        Vec3 look = player.getLookAngle();
        Vec3 delta = look.scale(distance);
        if (!level.noCollision(player, player.getBoundingBox().move(delta))) {
            delta = look.scale(distance / 2.0);
            if (!level.noCollision(player, player.getBoundingBox().move(delta))) {
                return null;
            }
        }
        return delta;
    }

    private static Supplier<Holder<MobEffect>> vanilla(Holder<MobEffect> effect) {
        return () -> effect;
    }

    private static Supplier<Holder<MobEffect>> tensura(dev.architectury.registry.registries.RegistrySupplier<MobEffect> effect) {
        return () -> TensuraMobEffects.getReference(effect);
    }

    private static void burst(String id, double cost, float damage, double range, double knock, ResourceKey<DamageType> type, Supplier<Holder<MobEffect>> effect, int ticks) {
        HITS.put(id, new Hit(Shape.BURST, cost, damage, range, knock, 10, type, effect, ticks, 0));
    }

    private static void cone(String id, double cost, float damage, double range, ResourceKey<DamageType> type) {
        HITS.put(id, new Hit(Shape.CONE, cost, damage, range, 0.6, 8, type, null, 0, 0));
    }

    private static void beam(String id, double cost, float damage, double range, ResourceKey<DamageType> type, Supplier<Holder<MobEffect>> effect, int ticks) {
        HITS.put(id, new Hit(Shape.BEAM, cost, damage, range, 1.2, 20, type, effect, ticks, 0));
    }

    private static void dash(String id, double cost, float damage, double range, ResourceKey<DamageType> type) {
        HITS.put(id, new Hit(Shape.DASH, cost, damage, range, 0.8, 8, type, null, 0, 0));
    }

    private static void blink(String id, double cost, double range) {
        HITS.put(id, new Hit(Shape.BLINK, cost, 0, range, 0, 4, TensuraDamageTypes.SPACE_ELEMENTAL, null, 0, 0));
    }

    private static void self(String id, double cost, Supplier<Holder<MobEffect>> effect, int ticks, int amplifier) {
        HITS.put(id, new Hit(Shape.SELF, cost, 0, 0, 0, 6, null, effect, ticks, amplifier));
    }

    private static void ally(String id, double cost, float heal, double range, Supplier<Holder<MobEffect>> effect, int ticks, int amplifier) {
        HITS.put(id, new Hit(Shape.ALLY, cost, heal, range, 0, 10, null, effect, ticks, amplifier));
    }

    private static void drain(String id, double cost, float damage, double range, ResourceKey<DamageType> type) {
        HITS.put(id, new Hit(Shape.DRAIN, cost, damage, range, 0.1, 8, type, null, 0, 0));
    }

    private static void single(String id, double cost, float damage, double range, ResourceKey<DamageType> type, Supplier<Holder<MobEffect>> effect, int ticks) {
        HITS.put(id, new Hit(Shape.SINGLE, cost, damage, range, 0.4, 12, type, effect, ticks, 0));
    }

    private static void cleanse(String id, double cost, double range) {
        HITS.put(id, new Hit(Shape.CLEANSE, cost, 0, range, 0, 8, null, null, 0, 0));
    }

    private static void checkpoint(String id, double cost) {
        HITS.put(id, new Hit(Shape.CHECKPOINT, cost, 0, 0, 0, 2, null, null, 0, 0));
    }
}
