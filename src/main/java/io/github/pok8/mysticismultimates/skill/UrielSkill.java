package io.github.pok8.mysticismultimates.skill;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.subclass.ISpatialStorage;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.entity.projectile.TensuraFlyingProjectile;
import io.github.manasmods.tensura.entity.projectile.magic.SeveranceCutterProjectile;
import io.github.manasmods.tensura.event.TensuraEntityEvents;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import io.github.manasmods.tensura.particle.TensuraParticleHelper;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.AbilityStorage;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.UltimateSlotStorage;
import io.github.pok8.mysticismultimates.registry.UltimateSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.List;

public class UrielSkill extends Skill implements ISpatialStorage {
    public static final String ID = "uriel";
    public static final ResourceLocation UNIVERSAL_BARRIER = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "uriel_universal_barrier");

    private static final String[] MODES = {
            "infinity_prison",       // 0
            "imaginary_space",      // 1
            "law_manipulation",     // 2
            "gravity_manipulation", // 3
            "universal_barrier",    // 4
            "absolute_severance",   // 5
            "fatal_imprison",       // 6
            "nova_break",           // 7
            "insulated_prison"      // 8
    };

    public UrielSkill() {
        super(SkillType.ULTIMATE);
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double cost) {
        io.github.manasmods.tensura.storage.ep.ExistenceStorage existence = ShadowArmy.existence(player);
        if (existence == null || (!existence.isTrueDemonLord() && !existence.isTrueHero())) {
            return false;
        }
        ManasSkill prison = io.github.manasmods.tensura.registry.skill.UniqueSkills.INFINITY_PRISON.get();
        return prison != null && io.github.manasmods.tensura.ability.SkillUtils.isSkillMastered(player, prison);
    }

    @Override
    public boolean canLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        if (!super.canLearnSkill(instance, entity)) {
            return false;
        }
        if (entity instanceof Player player) {
            io.github.manasmods.tensura.storage.ep.ExistenceStorage existence = ShadowArmy.existence(player);
            return existence != null && (existence.isTrueDemonLord() || existence.isTrueHero());
        }
        return true;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(UrielSkill::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(UrielSkill::onEffectApplicable);

        TensuraEntityEvents.SPIRITUAL_HURT_EVENT.register((target, attacker, amount, spiritualHealth, mpDamage, source) -> {
            if (target == null) {
                return EventResult.pass();
            }
            ManasSkillInstance instance = getUrielInstanceInSlot(target);
            if (instance == null) {
                return EventResult.pass();
            }

            if (attacker instanceof LivingEntity livingAttacker) {
                if (hasSusanoo(livingAttacker)) {
                    return EventResult.pass();
                }
                if (EnergyHelper.getMaxEP(livingAttacker) >= EnergyHelper.getMaxEP(target) * 8.0) {
                    return EventResult.pass();
                }
            }

            // Consume magicules = amount * 0.8
            double cost = amount * 0.8;
            if (EnergyHelper.isOutOfMagiculeConsuming(target, cost) == 0.0) {
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        });

        TensuraEntityEvents.ENERGY_DRAIN_EVENT.register((target, attacker, drainType, gainType, amount, percentage) -> {
            if (target == null) {
                return EventResult.pass();
            }
            ManasSkillInstance instance = getUrielInstanceInSlot(target);
            if (instance == null) {
                return EventResult.pass();
            }

            if (attacker instanceof LivingEntity livingAttacker) {
                if (hasSusanoo(livingAttacker)) {
                    return EventResult.pass();
                }
                if (EnergyHelper.getMaxEP(livingAttacker) >= EnergyHelper.getMaxEP(target) * 8.0) {
                    return EventResult.pass();
                }
            }
            return EventResult.interruptFalse();
        });
    }

    @Override
    public int getModes(ManasSkillInstance instance) {
        return MODES.length;
    }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int currentMode, boolean reverse) {
        return Math.floorMod(currentMode + (reverse ? -1 : 1), MODES.length);
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        int index = Math.floorMod(mode, MODES.length);
        return ID + "." + MODES[index];
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        int index = Math.floorMod(mode, MODES.length);
        return Component.translatable("mysticism_ultimates.skill.mode.uriel." + MODES[index]);
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable("mysticism_ultimates.skill.uriel");
    }

    @Override
    public MutableComponent getSkillDescription() {
        return Component.translatable("mysticism_ultimates.skill.uriel.description");
    }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "textures/skill/uriel.png");
    }

    @Override
    public double getDefaultAcquiringMagiculeCost() {
        return 1_000_000.0;
    }

    @Override
    public int getMaxMastery() {
        return 3000;
    }

    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        return switch (Math.floorMod(mode, MODES.length)) {
            case 0 -> 100_000.0;
            case 1 -> 0.0;
            case 2 -> 5.0;
            case 3 -> 0.0;
            case 4 -> 20_000.0;
            case 5 -> 100_000.0;
            case 6 -> 250_000.0;
            case 7 -> 2_000_000.0;
            case 8 -> 200_000.0;
            default -> 0.0;
        };
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
        AttributeInstance phys = entity.getAttribute(TensuraAttributes.PHYSICAL_BARRIER);
        if (phys != null) {
            phys.removeModifier(UNIVERSAL_BARRIER);
        }
        AttributeInstance mag = entity.getAttribute(TensuraAttributes.MAGIC_BARRIER);
        if (mag != null) {
            mag.removeModifier(UNIVERSAL_BARRIER);
        }
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        UltimateSlotStorage storage = UltimateSlotStorage.get(entity);
        if (storage != null) {
            storage.decrement();
        }
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (entity.level().isClientSide()) {
            return;
        }
        int resolvedMode = Math.floorMod(mode, MODES.length);
        switch (resolvedMode) {
            case 0 -> activateInfinityPrison(instance, entity);
            case 1 -> this.openSpatialStorage(entity, instance);
            case 2 -> breakBlock(entity, instance);
            case 3 -> toggleGravityManipulation(instance, entity);
            case 4 -> toggleUniversalBarrier(instance, entity);
            case 5 -> shootAbsoluteSeverance(instance, entity);
            case 6 -> activateFatalImprison(instance, entity);
            case 7 -> activateNovaBreak(instance, entity);
            case 8 -> activateInsulatedPrison(instance, entity);
        }
    }

    // --- MODE 0: INFINITY PRISON (IMPRISON) ---

    private void activateInfinityPrison(ManasSkillInstance instance, LivingEntity entity) {
        if (EnergyHelper.isOutOfEnergy(entity, instance, 0)) {
            return;
        }
        LivingEntity target = ObjectSelectionHelper.getTargetingEntity(entity, 30.0, false);
        if (target == null) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.no_target").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        if (target.hasEffect(TensuraMobEffects.INFINITE_IMPRISONMENT)) {
            target.removeEffect(TensuraMobEffects.INFINITE_IMPRISONMENT);
            entity.swing(InteractionHand.MAIN_HAND, true);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            TensuraParticleHelper.addServerParticlesAroundSelf(target, ParticleTypes.ENCHANT, 1.0);
            TensuraParticleHelper.addServerParticlesAroundSelf(target, ParticleTypes.CRIT, 1.0);
            return;
        }

        if (target instanceof Player player && player.getAbilities().invulnerable) {
            return;
        }

        instance.addMasteryPoint(entity);
        instance.setCoolDown(0, instance.isMastered(entity) ? 5 : 10);
        int duration = instance.isMastered(entity) ? 20000 : 12000;
        target.addEffect(new MobEffectInstance(TensuraMobEffects.INFINITE_IMPRISONMENT, duration, 1, false, false, false));
        entity.swing(InteractionHand.MAIN_HAND, true);
        TensuraParticleHelper.addServerParticlesAroundSelf(target, ParticleTypes.ENCHANT, 1.0);
        TensuraParticleHelper.addServerParticlesAroundSelf(target, ParticleTypes.SOUL_FIRE_FLAME, 1.0);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // --- MODE 1: IMAGINARY SPACE (ISpatialStorage) ---

    @Override
    public SpatialStorageContainer getSpatialStorage(ManasSkillInstance instance, HolderLookup.Provider provider) {
        SpatialStorageContainer container = new SpatialStorageContainer(135, 999);
        CompoundTag tag = instance.getOrCreateTag();
        if (tag.contains("SpatialStorage", 9)) {
            container.fromTag(tag.getList("SpatialStorage", 10), provider);
        }
        return container;
    }

    // --- MODE 2: LAW MANIPULATION (MOLECULAR MANIPULATION) ---

    private void breakBlock(LivingEntity entity, ManasSkillInstance instance) {
        if (!(entity instanceof Player player) || EnergyHelper.isOutOfEnergy(entity, instance, 2)) {
            return;
        }
        Level level = entity.level();
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVec = player.getViewVector(1.0f);
        Vec3 reachVec = eyePos.add(viewVec.scale(30.0));
        BlockHitResult result = level.clip(new ClipContext(eyePos, reachVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (result.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = result.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return;
            }
            if (state.getDestroySpeed(level, pos) < 0 && !player.isCreative()) {
                return;
            }
            ItemStack stack = new ItemStack(state.getBlock());
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
            level.destroyBlock(pos, false, player);
            player.swing(InteractionHand.MAIN_HAND, true);
            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.05);
            }
            instance.addMasteryPoint(player);
        }
    }

    // --- MODE 3: GRAVITY MANIPULATION ---

    private void toggleGravityManipulation(ManasSkillInstance instance, LivingEntity entity) {
        if (!(entity instanceof Player player) || EnergyHelper.isOutOfEnergy(entity, instance, 3)) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        boolean canFly = player.getAbilities().mayfly;
        if (canFly && !player.isCreative()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.displayClientMessage(Component.translatable("mysticism_ultimates.uriel.gravity.off").withStyle(ChatFormatting.GRAY), true);
        } else {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.displayClientMessage(Component.translatable("mysticism_ultimates.uriel.gravity.on").withStyle(ChatFormatting.AQUA), true);
            instance.addMasteryPoint(player);
        }
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // --- MODE 4: UNIVERSAL BARRIER ---

    private void toggleUniversalBarrier(ManasSkillInstance instance, LivingEntity entity) {
        if (EnergyHelper.isOutOfEnergy(entity, instance, 4)) {
            return;
        }
        AttributeInstance phys = entity.getAttribute(TensuraAttributes.PHYSICAL_BARRIER);
        AttributeInstance mag = entity.getAttribute(TensuraAttributes.MAGIC_BARRIER);
        if (phys == null || mag == null) {
            return;
        }

        if (phys.getModifier(UNIVERSAL_BARRIER) != null) {
            phys.removeModifier(UNIVERSAL_BARRIER);
            mag.removeModifier(UNIVERSAL_BARRIER);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("mysticism_ultimates.uriel.universal_barrier.off").withStyle(ChatFormatting.GRAY), true);
            }
        } else {
            instance.addMasteryPoint(entity);
            instance.setCoolDown(4, 10);
            double barrierAmount = entity.getMaxHealth() * (instance.isMastered(entity) ? 8.0 : 4.0);
            AttributeModifier mod = new AttributeModifier(UNIVERSAL_BARRIER, barrierAmount, AttributeModifier.Operation.ADD_VALUE);
            phys.addPermanentModifier(mod);
            mag.addPermanentModifier(mod);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("mysticism_ultimates.uriel.universal_barrier.on").withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    // --- MODE 5: ABSOLUTE SEVERANCE ---

    private void shootAbsoluteSeverance(ManasSkillInstance instance, LivingEntity entity) {
        if (EnergyHelper.isOutOfEnergy(entity, instance, 5)) {
            return;
        }
        SeveranceCutterProjectile spaceCutter = new SeveranceCutterProjectile(entity.level(), entity);
        spaceCutter.setSpeed(2.5F);
        spaceCutter.setDamage(instance.isMastered(entity) ? 1400.0F : 700.0F);
        spaceCutter.setSize(instance.isMastered(entity) ? 10.0F : 8.0F);
        spaceCutter.setMpCost(getMagiculeCost(entity, instance, 5));
        spaceCutter.setSkill(instance);
        spaceCutter.setPosAndShoot(entity);
        spaceCutter.setPosDirection(entity, TensuraFlyingProjectile.PositionDirection.MIDDLE);
        entity.level().addFreshEntity(spaceCutter);

        instance.addMasteryPoint(entity);
        instance.setCoolDown(5, instance.isMastered(entity) ? 3 : 5);
        entity.swing(InteractionHand.MAIN_HAND, true);
        entity.swing(InteractionHand.OFF_HAND, true);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // --- MODE 6: FATAL IMPRISON ---

    private void activateFatalImprison(ManasSkillInstance instance, LivingEntity entity) {
        if (EnergyHelper.isOutOfEnergy(entity, instance, 6)) {
            return;
        }
        double radius = instance.isMastered(entity) ? 30.0 : 20.0;
        List<LivingEntity> list = entity.level().getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(radius),
                target -> !target.is(entity) && target.isAlive() && !target.isAlliedTo(entity)
        );
        if (list.isEmpty()) {
            return;
        }
        instance.setCoolDown(6, instance.isMastered(entity) ? 20 : 40);
        instance.addMasteryPoint(entity);

        int duration = instance.isMastered(entity) ? 20000 : 12000;
        for (LivingEntity target : list) {
            if (target instanceof Player player && player.getAbilities().invulnerable) {
                continue;
            }
            target.addEffect(new MobEffectInstance(TensuraMobEffects.INFINITE_IMPRISONMENT, duration, 1, false, false, false));
            TensuraParticleHelper.addServerParticlesAroundSelf(target, ParticleTypes.ENCHANT, 1.0);
            TensuraParticleHelper.addServerParticlesAroundSelf(target, ParticleTypes.SOUL_FIRE_FLAME, 1.0);
        }
        entity.swing(InteractionHand.MAIN_HAND, true);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // --- MODE 7: NOVA BREAK ---

    private void activateNovaBreak(ManasSkillInstance instance, LivingEntity entity) {
        if (EnergyHelper.isOutOfEnergy(entity, instance, 7)) {
            return;
        }
        LivingEntity target = ObjectSelectionHelper.getTargetingEntity(entity, 8.0, false);
        if (target == null) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.no_target").withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        if (target instanceof Player player && player.getAbilities().invulnerable) {
            return;
        }

        entity.swing(InteractionHand.MAIN_HAND, true);
        DamageSource holySource = TensuraDamageTypes.getEntityDamageSource(entity.level(), TensuraDamageTypes.HOLY_DAMAGE, entity);
        target.hurt(holySource, 5000.0F);

        TensuraDamageHelper.directSpiritualHurt(target, entity, 3000.0F, 0.0F);

        instance.setCoolDown(7, 200);
        instance.addMasteryPoint(entity);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
        TensuraParticleHelper.addServerParticlesAroundSelf(target, ParticleTypes.EXPLOSION, 1.0);
        TensuraParticleHelper.addServerParticlesAroundSelf(target, ParticleTypes.FLASH, 1.0);
    }

    // --- MODE 8: INSULATED PRISON ---

    private void activateInsulatedPrison(ManasSkillInstance instance, LivingEntity entity) {
        if (EnergyHelper.isOutOfEnergy(entity, instance, 8)) {
            return;
        }
        double radius = instance.isMastered(entity) ? 30.0 : 20.0;
        List<LivingEntity> list = entity.level().getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(radius),
                target -> !target.is(entity) && target.isAlive() && !target.isAlliedTo(entity)
        );
        if (list.isEmpty()) {
            return;
        }
        instance.setCoolDown(8, instance.isMastered(entity) ? 20 : 40);
        instance.addMasteryPoint(entity);

        int duration = instance.isMastered(entity) ? 1200 : 600;
        for (LivingEntity target : list) {
            if (target instanceof Player player && player.getAbilities().invulnerable) {
                continue;
            }
            target.addEffect(new MobEffectInstance(TensuraMobEffects.SPATIAL_BLOCKADE, duration, 1, false, false, false));
            target.addEffect(new MobEffectInstance(TensuraMobEffects.MAGIC_INTERFERENCE, duration, 1, false, false, false));
            target.addEffect(new MobEffectInstance(TensuraMobEffects.FRAGILITY, duration, 1, false, false, false));
            target.addEffect(new MobEffectInstance(TensuraMobEffects.SILENCE, duration, 1, false, false, false));
            target.addEffect(new MobEffectInstance(TensuraMobEffects.ENERGY_BLOCKADE, duration, 1, false, false, false));
            TensuraParticleHelper.addServerParticlesAroundSelf(target, ParticleTypes.ENCHANT, 1.0);
            TensuraParticleHelper.addServerParticlesAroundSelf(target, ParticleTypes.SOUL_FIRE_FLAME, 1.0);
        }
        entity.swing(InteractionHand.MAIN_HAND, true);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // --- PASSIVE & DAMAGE EVENTS ---

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        // 1. Attacker spatial / gravity amplification when Uriel is toggled
        if (source.getEntity() instanceof LivingEntity attacker) {
            ManasSkillInstance instance = getUrielInstanceInSlot(attacker);
            if (instance != null && instance.isToggled()) {
                if (TensuraDamageHelper.isGravityDamage(source)) {
                    event.setNewDamage(event.getNewDamage() * 4.0F);
                }
                if (TensuraDamageHelper.isSpatialDamage(source)) {
                    event.setNewDamage(event.getNewDamage() * 8.0F);
                }
            }
        }

        // 2. Target general damage absorption with magicules
        ManasSkillInstance targetInstance = getUrielInstanceInSlot(target);
        if (targetInstance != null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            float damage = event.getNewDamage();
            if (damage > 0.0f) {
                double cost = damage * 50.0;
                double lacked = EnergyHelper.isOutOfMagiculeConsuming(target, cost);
                if (lacked == 0.0) {
                    event.setNewDamage(0.0f);
                } else {
                    float absorbed = (float) ((cost - lacked) / 50.0);
                    event.setNewDamage(Math.max(damage - absorbed, 0.0f));
                }
            }
        }
    }

    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        ManasSkillInstance instance = getUrielInstanceInSlot(entity);
        if (instance != null && instance.isToggled()) {
            Holder<MobEffect> effect = event.getEffectInstance().getEffect();
            if (isAbsoluteGuardEffect(effect)) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            }
        }
    }

    private static boolean isAbsoluteGuardEffect(Holder<MobEffect> effect) {
        MobEffect val = effect.value();
        return val == TensuraMobEffects.INFINITE_IMPRISONMENT.get()
                || val == TensuraMobEffects.DISINTEGRATING.get()
                || val == TensuraMobEffects.SEVERANCE_BLADE.get()
                || val == TensuraMobEffects.BLACK_BURN.get()
                || val == TensuraMobEffects.ANTI_SKILL.get();
    }

    private static boolean hasSusanoo(LivingEntity entity) {
        SkillStorage storage = SkillAPI.getSkillsFrom(entity);
        if (storage == null) {
            return false;
        }
        for (ManasSkillInstance instance : storage.getLearnedSkills()) {
            ManasSkill skill = instance.getSkill();
            if (skill != null && skill.getRegistryName() != null && "susanoo".equals(skill.getRegistryName().getPath())) {
                return true;
            }
        }
        return false;
    }

    private static ManasSkillInstance getUrielInstanceInSlot(LivingEntity entity) {
        SkillStorage skillStorage = SkillAPI.getSkillsFrom(entity);
        AbilityStorage storage = (AbilityStorage) TensuraStorages.getAbilityFrom(entity);
        if (storage == null || skillStorage == null) {
            return null;
        }
        ManasSkill uriel = UltimateSkills.URIEL != null ? UltimateSkills.URIEL.get() : null;
        if (uriel == null || !storage.isAbilityInActivePreset(uriel)) {
            return null;
        }
        return skillStorage.getSkill(uriel).orElse(null);
    }
}
