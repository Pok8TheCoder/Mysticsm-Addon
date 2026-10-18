package io.github.pok8.mysticismultimates.skill;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.skill.extra.ThoughtAccelerationSkill;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.entity.human.CloneEntity;
import io.github.manasmods.tensura.data.TensuraEntityTags;
import io.github.manasmods.tensura.particle.TensuraParticleHelper;
import io.github.manasmods.tensura.particle.TensuraParticleUtils;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.entity.HumanEntityTypes;
import io.github.manasmods.tensura.registry.particle.TensuraParticleTypes;
import io.github.manasmods.tensura.registry.skill.CommonSkills;
import io.github.manasmods.tensura.registry.skill.ResistanceSkills;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.registry.sound.TensuraSoundEvents;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.AbilityStorage;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.UltimateSlotStorage;
import io.github.pok8.mysticismultimates.registry.UltimateSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import io.github.manasmods.tensura.ability.subclass.ICloning;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.UUID;

public class HadesSkill extends Skill implements ICloning {
    public static final String ID = "hades";
    public static final ResourceLocation ACCELERATION = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "hades_acceleration");
    public static final ResourceLocation RECON_MODIFIER = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "hades_recon");
    public static final ResourceLocation ATTACK_MODIFIER = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "hades_attack");

    private static final String[] MODES = {
            "recon_mode",
            "attack_mode",
            "infinite_eater",
            "fear_of_insanity",
            "merciless"
    };

    public HadesSkill() {
        super(SkillType.ULTIMATE);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(HadesSkill::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(HadesSkill::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(HadesSkill::onLivingDeath);
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
        if (index == 4) {
            return Component.literal("Merciless");
        }
        return Component.translatable("mysticism_ultimates.skill.mode.hades." + MODES[index]);
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double cost) {
        ExistenceStorage existence = ShadowArmy.existence(player);
        if (existence == null || (!existence.isTrueDemonLord() && !existence.isTrueHero())) {
            return false;
        }
        ManasSkill reaper = UniqueSkills.REAPER.get();
        ManasSkill merciless = UniqueSkills.MERCILESS.get();
        ManasSkill poison = CommonSkills.POISON.get();
        ManasSkill corrosion = CommonSkills.CORROSION.get();
        ManasSkill paralysis = CommonSkills.PARALYSIS.get();
        return reaper != null && merciless != null && poison != null && corrosion != null && paralysis != null
                && SkillUtils.isSkillMastered(player, reaper)
                && SkillUtils.isSkillMastered(player, merciless)
                && SkillUtils.isSkillMastered(player, poison)
                && SkillUtils.isSkillMastered(player, corrosion)
                && SkillUtils.isSkillMastered(player, paralysis);
    }

    @Override
    public boolean canLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        if (!super.canLearnSkill(instance, entity)) {
            return false;
        }
        if (entity instanceof Player player) {
            ExistenceStorage existence = ShadowArmy.existence(player);
            return existence != null && (existence.isTrueDemonLord() || existence.isTrueHero());
        }
        return true;
    }

    @Override
    public void onCloneTick(CloneEntity clone, LivingEntity owner) {
        IExistence cloneExistence = TensuraStorages.getExistenceFrom(clone);
        if (cloneExistence != null && cloneExistence.getSleepModeTime() > 0) {
            clone.remove();
            return;
        }
        AttributeInstance scale = owner.getAttribute(Attributes.SCALE);
        if (scale == null || !scale.hasModifier(ATTACK_MODIFIER)) {
            clone.remove();
        }
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable("mysticism_ultimates.skill.hades");
    }

    @Override
    public MutableComponent getSkillDescription() {
        return Component.translatable("mysticism_ultimates.skill.hades.description");
    }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "textures/skill/hades.png");
    }

    @Override
    public double getDefaultAcquiringMagiculeCost() {
        return 600_000.0;
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        return switch (Math.floorMod(mode, MODES.length)) {
            case 3 -> 50.0;
            case 4 -> 2000.0;
            default -> 0.0;
        };
    }

    @Override
    public int getMaxMastery() {
        return 2000;
    }

    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        return instance.isToggled();
    }

    @Override
    public boolean canScroll(ManasSkillInstance instance, LivingEntity entity, int mode) {
        return Math.floorMod(mode, MODES.length) == 1; // attack_mode
    }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        ThoughtAccelerationSkill.onToggle(instance, entity, ACCELERATION, true);
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        ThoughtAccelerationSkill.onToggle(instance, entity, ACCELERATION, false);
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (instance.isToggled() && !entity.level().isClientSide()) {
            entity.addEffect(new MobEffectInstance(TensuraMobEffects.PRESENCE_SENSE, 200, 1, false, false, false));
            entity.addEffect(new MobEffectInstance(TensuraMobEffects.AUDITORY_SENSE, 200, 0, false, false, false));
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
        deactivateReconMode(instance, entity);
        revertAttackMode(instance, entity);
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
            case 0 -> toggleReconMode(instance, entity);
            case 2 -> activateInfiniteEater(instance, entity);
        }
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        int resolvedMode = Math.floorMod(mode, MODES.length);
        if (resolvedMode == 1) { // attack_mode
            if (isReconActive(instance)) {
                return false;
            }
            CompoundTag tag = instance.getTag();
            int clones = tag != null && tag.contains("clones") ? Math.max(tag.getInt("clones"), 1) : 10;
            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.translatable("tensura.skill.output_number", clones)
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA)), true);
            }
            return true;
        } else if (resolvedMode == 3) { // fear_of_insanity
            channelFearOfInsanity(instance, entity, heldTicks);
            return true;
        } else if (resolvedMode == 4) { // merciless
            channelMerciless(instance, entity, heldTicks);
            return true;
        }
        return false;
    }

    @Override
    public void onScroll(ManasSkillInstance instance, LivingEntity living, double delta, int mode) {
        if (Math.floorMod(mode, MODES.length) == 1) { // attack_mode
            CompoundTag tag = instance.getOrCreateTag();
            int current = tag.contains("clones") ? tag.getInt("clones") : 10;
            int next = current + (int) delta;
            if (next > 10) {
                next = 1;
            } else if (next < 1) {
                next = 10;
            }
            tag.putInt("clones", next);
            instance.markDirty();
        }
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int keyNumber, int mode) {
        if (entity.level().isClientSide() || Math.floorMod(mode, MODES.length) != 1) {
            return;
        }
        if (isReconActive(instance)) {
            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.translatable("tensura.skill.mode.need_toggle_off", Component.translatable("mysticism_ultimates.skill.hades.recon_mode"))
                                .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        AttributeInstance scale = entity.getAttribute(Attributes.SCALE);
        if (scale != null && scale.hasModifier(ATTACK_MODIFIER)) {
            revertAttackMode(instance, entity);
            instance.setCoolDown(1, 10);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            CompoundTag tag = instance.getTag();
            int clones = tag != null && tag.contains("clones") ? Math.max(tag.getInt("clones"), 1) : 10;
            summonClonesAndTransform(instance, entity, clones);
        }
    }

    // --- MODE 0: RECON MODE ---

    private void toggleReconMode(ManasSkillInstance instance, LivingEntity entity) {
        if (UniqueSkills.REAPER.get() != null && SkillUtils.isSkillToggled(entity, UniqueSkills.REAPER.get())) {
            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.translatable("tensura.skill.mode.need_toggle_off", UniqueSkills.REAPER.get().getName())
                                .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        CompoundTag tag = instance.getOrCreateTag();
        boolean active = tag.getBoolean("ReconMode");
        if (active) {
            deactivateReconMode(instance, entity);
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("mysticism_ultimates.hades.recon.off").withStyle(ChatFormatting.GRAY), true);
            }
        } else {
            activateReconMode(instance, entity);
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("mysticism_ultimates.hades.recon.on").withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }
    }

    private void activateReconMode(ManasSkillInstance instance, LivingEntity entity) {
        CompoundTag tag = instance.getOrCreateTag();
        tag.putBoolean("ReconMode", true);
        instance.markDirty();

        boolean mastered = instance.isMastered(entity);
        double dodge = mastered ? 1.0 : 0.75;
        double senseBonus = mastered ? 2.0 : 1.0;
        double radiusBonus = mastered ? 32.0 : 16.0;

        applyModifier(entity, TensuraAttributes.AUTO_MELEE_DODGE_CHANCE, RECON_MODIFIER, dodge, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(entity, TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE, RECON_MODIFIER, dodge, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(entity, TensuraAttributes.PRESENCE_SENSE, RECON_MODIFIER, senseBonus, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(entity, TensuraAttributes.PRESENCE_SENSE_RADIUS, RECON_MODIFIER, radiusBonus, AttributeModifier.Operation.ADD_VALUE);

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);
        if (entity.level() instanceof ServerLevel serverLevel) {
            TensuraParticleHelper.addServerAuraParticles(entity, TensuraParticleUtils.getBlackAura(0.5f, 2.0f, -0.3f), 15, 0.01);
        }
    }

    private void deactivateReconMode(ManasSkillInstance instance, LivingEntity entity) {
        CompoundTag tag = instance.getOrCreateTag();
        tag.putBoolean("ReconMode", false);
        instance.markDirty();

        removeModifier(entity, TensuraAttributes.AUTO_MELEE_DODGE_CHANCE, RECON_MODIFIER);
        removeModifier(entity, TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE, RECON_MODIFIER);
        removeModifier(entity, TensuraAttributes.PRESENCE_SENSE, RECON_MODIFIER);
        removeModifier(entity, TensuraAttributes.PRESENCE_SENSE_RADIUS, RECON_MODIFIER);

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    public static boolean isReconActive(ManasSkillInstance instance) {
        CompoundTag tag = instance.getTag();
        return tag != null && tag.getBoolean("ReconMode");
    }

    // --- MODE 1: ATTACK MODE (CLONE SQUAD) ---

    private void summonClonesAndTransform(ManasSkillInstance instance, LivingEntity entity, int number) {
        Level level = entity.level();
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), (SoundEvent) TensuraSoundEvents.GENERIC_SPLIT.get(), TensuraSkill.ABILITY_SOUND, 1.0f, 1.0f);
        if (level instanceof ServerLevel serverLevel) {
            TensuraParticleHelper.addServerAuraParticles(entity, TensuraParticleUtils.getBlackAura(0.5f, 3.0f, -0.3f), 20, 0.01);
        }

        IExistence existence = TensuraStorages.getExistenceFrom(entity);
        double ownerAura = existence != null ? existence.getAura() : 10_000.0;
        double ownerMagicule = existence != null ? existence.getMagicule() : 10_000.0;
        double eachAura = Math.max(10.0, (ownerAura * 0.5) / number);
        double eachMagicule = Math.max(10.0, (ownerMagicule * 0.5) / number);

        float size = switch (number) {
            case 1 -> 0.5F;
            case 2 -> 0.48F;
            case 3 -> 0.45F;
            case 4 -> 0.42F;
            case 5 -> 0.4F;
            case 6 -> 0.38F;
            case 7 -> 0.36F;
            case 8 -> 0.34F;
            case 9 -> 0.32F;
            default -> 0.3F;
        };

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("tensura", "clone"));
        if (type == null) {
            type = HumanEntityTypes.CLONE.get();
        }

        for (int i = 0; i < number; i++) {
            if (type.create(level) instanceof CloneEntity clone) {
                if (entity instanceof Player player) {
                    clone.tame(player);
                }
                clone.setSkill(instance);
                clone.copyStatsAndSkills(entity, CloneEntity.CopySkill.ALL, false);
                CloneEntity.copyStatusEffect(entity, clone, CloneEntity.CopySkill.ALL, false);

                AttributeInstance cloneScale = clone.getAttribute(Attributes.SCALE);
                if (cloneScale != null) {
                    cloneScale.addOrReplacePermanentModifier(new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath("tensura", "reaper_clone"),
                            size - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }

                clone.moveTo(entity.getX() + (level.random.nextDouble() - 0.5) * 2.0, entity.getY(), entity.getZ() + (level.random.nextDouble() - 0.5) * 2.0, entity.getYRot(), entity.getXRot());
                EnergyHelper.setMaxAura(clone, eachAura);
                EnergyHelper.setMaxMagicule(clone, eachMagicule);
                IExistence cloneExistence = TensuraStorages.getExistenceFrom(clone);
                if (cloneExistence != null) {
                    cloneExistence.setAura(eachAura);
                    cloneExistence.setMagicule(eachMagicule);
                    cloneExistence.markDirty();
                }
                level.addFreshEntity(clone);
            }
        }

        // Transform owner
        applyModifier(entity, Attributes.SCALE, ATTACK_MODIFIER, size - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(entity, TensuraAttributes.MAX_AURA, ATTACK_MODIFIER, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(entity, TensuraAttributes.MAX_MAGICULE, ATTACK_MODIFIER, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        if (existence != null) {
            double maxAura = EnergyHelper.getMaxAura(entity);
            double maxMagicule = EnergyHelper.getMaxMagicule(entity);
            existence.setAura(Math.min(existence.getAura() * 0.5, maxAura));
            existence.setMagicule(Math.min(existence.getMagicule() * 0.5, maxMagicule));
            existence.markDirty();
        }

        instance.addMasteryPoint(entity);
    }

    private void revertAttackMode(ManasSkillInstance instance, LivingEntity entity) {
        removeModifier(entity, Attributes.SCALE, ATTACK_MODIFIER);
        removeModifier(entity, TensuraAttributes.MAX_AURA, ATTACK_MODIFIER);
        removeModifier(entity, TensuraAttributes.MAX_MAGICULE, ATTACK_MODIFIER);

        IExistence existence = TensuraStorages.getExistenceFrom(entity);
        if (existence != null) {
            double maxAura = EnergyHelper.getMaxAura(entity);
            double maxMagicule = EnergyHelper.getMaxMagicule(entity);
            existence.setAura(Math.min(existence.getAura() * 2.0, maxAura));
            existence.setMagicule(Math.min(existence.getMagicule() * 2.0, maxMagicule));
            existence.markDirty();
        }
    }

    // --- MODE 2: INFINITE EATER ---

    private void activateInfiniteEater(ManasSkillInstance instance, LivingEntity entity) {
        if (isReconActive(instance)) {
            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.translatable("tensura.skill.mode.need_toggle_off", Component.translatable("mysticism_ultimates.skill.hades.recon_mode"))
                                .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        AttributeInstance scale = entity.getAttribute(Attributes.SCALE);
        if (scale != null && scale.hasModifier(ATTACK_MODIFIER)) {
            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.translatable("tensura.ability.activation_failed").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        Level level = entity.level();
        LivingEntity target = ObjectSelectionHelper.getTargetingEntity(entity, 6.0, false);
        if (target == null || !target.isAlive()) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("tensura.targeting.not_targeted").withStyle(ChatFormatting.RED), false);
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), (SoundEvent) TensuraSoundEvents.GENERIC_CAST_FAIL.get(), TensuraSkill.ABILITY_SOUND, 1.0f, 1.0f);
            instance.setCoolDown(2, instance.isMastered(entity) ? 5 : 10);
            return;
        }

        if (target instanceof Player player && player.getAbilities().invulnerable) {
            return;
        }

        double targetEp = TensuraStorages.getExistenceFrom(target) != null ? TensuraStorages.getExistenceFrom(target).getEP() : EnergyHelper.getMaxEP(target);
        double userEp = TensuraStorages.getExistenceFrom(entity) != null ? TensuraStorages.getExistenceFrom(entity).getEP() : EnergyHelper.getMaxEP(entity);
        if (targetEp > userEp) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("tensura.targeting.ep_not_meet").withStyle(ChatFormatting.RED), false);
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), (SoundEvent) TensuraSoundEvents.GENERIC_CAST_FAIL.get(), TensuraSkill.ABILITY_SOUND, 1.0f, 1.0f);
            return;
        }

        IExistence userExistence = TensuraStorages.getExistenceFrom(entity);
        if (userExistence == null || userExistence.getMagicule() < targetEp) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("tensura.skill.lack_magicule").withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        userExistence.setMagicule(Math.max(0.0, userExistence.getMagicule() - targetEp));
        userExistence.markDirty();

        double regularEpGain = EnergyHelper.getEPGain(target, entity);

        DamageSource source = this.createSource(instance, entity, TensuraDamageTypes.INFINITE_EATER, 2);
        if (source instanceof TensuraDamageSource tensuraSource) {
            tensuraSource.tensura$setResistanceBypassLevel(10.0f);
            tensuraSource.tensura$setBarrierBypassLevel(10.0f);
            tensuraSource.tensura$setDodgeBypass();
        }

        target.invulnerableTime = 0;
        float damage = Math.max(target.getMaxHealth() * 100.0f, 1_000_000.0f);
        target.hurt(source, damage);
        if (target.isAlive() && !(target instanceof Player p && p.getAbilities().invulnerable)) {
            target.setHealth(0.0f);
            target.die(source);
        }

        entity.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0f, 1.0f);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 1.0, target.getZ(), 30, 0.5, 0.5, 0.5, 0.05);
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        }

        // Steal EP: 50% of target EP on first kill, regular EP calculation afterwards
        CompoundTag tag = instance.getOrCreateTag();
        CompoundTag eatenList = tag.contains("eatenList") ? tag.getCompound("eatenList") : new CompoundTag();
        String targetId = target instanceof Player ? "player_" + target.getStringUUID() : BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        boolean isFirstKill = !eatenList.contains(targetId);

        double epGain;
        if (isFirstKill) {
            eatenList.putBoolean(targetId, true);
            tag.put("eatenList", eatenList);
            instance.markDirty();

            // First kill grants 50% of target EP
            epGain = targetEp * 0.5;
        } else {
            // Afterwards: regular EP calculation
            double maxSteal = EnergyHelper.CONFIG != null ? EnergyHelper.CONFIG.maximumEPSteal : 50_000.0;
            epGain = maxSteal > 0 ? Math.min(regularEpGain, maxSteal) : regularEpGain;
        }

        IExistence targetExistence = TensuraStorages.getExistenceFrom(target);
        if (targetExistence != null) {
            targetExistence.setSkippingEPDrop(true);
            targetExistence.markDirty();
        }

        if (epGain > 0.0 && !target.getType().is(TensuraEntityTags.NO_EP_PLUNDER)) {
            EnergyHelper.drainEnergy(target, entity, epGain, false, EnergyHelper.DrainType.MAX_EP, EnergyHelper.GainType.NONE);
            EnergyHelper.gainMagicule(entity, epGain / 2.0, EnergyHelper.GainType.MAX);
            EnergyHelper.gainAura(entity, epGain / 2.0, EnergyHelper.GainType.MAX);
            EnergyHelper.gainMagicule(entity, epGain / 2.0, EnergyHelper.GainType.NORMAL);
            EnergyHelper.gainAura(entity, epGain / 2.0, EnergyHelper.GainType.NORMAL);

            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("+" + String.format("%,d", (long) epGain) + " EP" + (isFirstKill ? " (First Devour)" : ""))
                                .withStyle(isFirstKill ? ChatFormatting.GOLD : ChatFormatting.DARK_AQUA),
                        true);
            }
        }

        instance.addMasteryPoint(entity);
        instance.setCoolDown(2, instance.isMastered(entity) ? 5 : 10);
    }

    // --- MODE 3: FEAR OF INSANITY ---

    private void channelFearOfInsanity(ManasSkillInstance instance, LivingEntity entity, int heldTicks) {
        Level level = entity.level();
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.6f, 0.7f);

        if (level instanceof ServerLevel serverLevel && heldTicks % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY() + 0.8, entity.getZ(), 10, 1.0, 0.5, 1.0, 0.02);
            serverLevel.sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 6, 0.8, 0.4, 0.8, 0.01);
        }

        float radius = instance.isMastered(entity) ? 10.0F : 5.0F;
        List<LivingEntity> list = level.getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(radius),
                target -> target != entity && target.isAlive() && !target.isAlliedTo(entity));

        if (!list.isEmpty()) {
            if (heldTicks % 100 == 0 && heldTicks > 0) {
                instance.addMasteryPoint(entity);
            }

            int baseDuration = instance.isMastered(entity) ? 400 : 200;
            for (LivingEntity target : list) {
                if (target instanceof Player player && player.getAbilities().invulnerable) {
                    continue;
                }
                MobEffectInstance fear = target.getEffect(TensuraMobEffects.FEAR);
                MobEffectInstance weakness = target.getEffect(MobEffects.WEAKNESS);
                MobEffectInstance insanity = target.getEffect(TensuraMobEffects.INSANITY);

                if (fear != null && weakness != null && insanity != null) {
                    if (heldTicks % 20 == 0) {
                        int cur = fear.getDuration();
                        target.addEffect(new MobEffectInstance(TensuraMobEffects.FEAR, cur + 40, 9, false, false));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, cur + 40, 4, false, false));
                        target.addEffect(new MobEffectInstance(TensuraMobEffects.INSANITY, cur + 40, 4, false, false));
                    }
                } else {
                    target.addEffect(new MobEffectInstance(TensuraMobEffects.FEAR, baseDuration, 9, false, false));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, baseDuration, 4, false, false));
                    target.addEffect(new MobEffectInstance(TensuraMobEffects.INSANITY, baseDuration, 4, false, false));
                }
            }
        }
    }

    // --- MODE 4: MERCILESS ABILITY ---

    public static boolean isMercilessImmune(LivingEntity target) {
        if (target == null) {
            return true;
        }
        // 1. Named check
        if (target.hasCustomName()) {
            return true;
        }
        IExistence existence = TensuraStorages.getExistenceFrom(target);
        if (existence != null && existence.getName() != null && !existence.getName().isEmpty()) {
            return true;
        }

        // 2. String tags check
        for (String tag : target.getTags()) {
            String lower = tag.toLowerCase(java.util.Locale.ROOT);
            if (lower.equals("boss") || lower.equals("mini_boss") || lower.equals("miniboss") || lower.equals("bosses") || lower.equals("mini_bosses")) {
                return true;
            }
            if (lower.equals("named")) {
                return true;
            }
            if (lower.equals("otherwordly") || lower.equals("otherworldly")) {
                return true;
            }
        }

        // 3. EntityType tags check
        EntityType<?> type = target.getType();
        if (type.is(TensuraEntityTags.HERO_BOSS)) {
            return true;
        }
        TagKey<EntityType<?>> cBosses = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
        TagKey<EntityType<?>> cMiniBosses = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "mini_bosses"));
        TagKey<EntityType<?>> tensuraBosses = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("tensura", "bosses"));
        TagKey<EntityType<?>> tensuraMiniBosses = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("tensura", "mini_bosses"));
        if (type.is(cBosses) || type.is(cMiniBosses) || type.is(tensuraBosses) || type.is(tensuraMiniBosses)) {
            return true;
        }

        // 4. Vanilla boss types fallback
        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || type == EntityType.WARDEN || type == EntityType.ELDER_GUARDIAN) {
            return true;
        }

        return false;
    }

    private void channelMerciless(ManasSkillInstance instance, LivingEntity entity, int heldTicks) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (heldTicks % 20 == 0 && EnergyHelper.isOutOfEnergy(entity, instance, 4)) {
            return;
        }
        if (heldTicks % 20 == 0 && heldTicks > 0) {
            instance.addMasteryPoint(entity);
        }

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.SOUL_ESCAPE, TensuraSkill.ABILITY_SOUND, 1.0f, 1.0f);
        TensuraParticleHelper.addServerParticlesAroundSelf(entity, (ParticleOptions) TensuraParticleTypes.SOUL.get(), 1.0);

        double radius = instance.isMastered(entity) ? 35.0 : 25.0;
        List<LivingEntity> list = entity.level().getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(radius),
                target -> !target.is(entity)
                        && target.isAlive()
                        && !target.isAlliedTo(entity)
                        && !target.getType().is(TensuraEntityTags.NO_SPIRITUAL_DAMAGE)
        );

        if (list.isEmpty()) {
            return;
        }

        IExistence userExistence = TensuraStorages.getExistenceFrom(entity);
        if (userExistence == null) {
            return;
        }

        double ownerEP = EnergyHelper.getMaxEP(entity);
        for (LivingEntity target : list) {
            if (target instanceof Player player && player.getAbilities().invulnerable) {
                continue;
            }

            // Cannot damage any entity with boss, mini boss, named, or otherwordly tag
            if (isMercilessImmune(target)) {
                continue;
            }

            boolean hasFear = target.hasEffect(TensuraMobEffects.FEAR) || target.hasEffect(TensuraMobEffects.INSANITY);
            boolean lowHp = target.getHealth() < target.getMaxHealth() * 0.5f;
            double targetEp = TensuraStorages.getExistenceFrom(target) != null ? TensuraStorages.getExistenceFrom(target).getEP() : EnergyHelper.getMaxEP(target);
            boolean lowEP = targetEp < ownerEP;

            if (lowHp || lowEP || hasFear) {
                // Costs 50% of target ep as magicule to kill the said entity
                double magiculeCost = targetEp * 0.5;
                if (userExistence.getMagicule() < magiculeCost) {
                    continue; // Skip if user cannot afford magicule cost
                }

                userExistence.setMagicule(Math.max(0.0, userExistence.getMagicule() - magiculeCost));
                userExistence.markDirty();

                DamageSource source = this.createSource(instance, entity, TensuraDamageTypes.SOUL_CONSUMED, 4);
                if (source instanceof TensuraDamageSource tensuraSource) {
                    tensuraSource.tensura$setBarrierBypassLevel(10.0f);
                    tensuraSource.tensura$setResistanceBypassLevel(10.0f);
                    tensuraSource.tensura$setDodgeBypass();
                }

                target.invulnerableTime = 0;
                float damage = Math.max(target.getMaxHealth() * 100.0f, 1_000_000.0f);
                target.hurt(source, damage);
                if (target.isAlive() && !(target instanceof Player p && p.getAbilities().invulnerable)) {
                    target.setHealth(0.0f);
                    target.die(source);
                }

                TensuraParticleHelper.addServerParticlesAroundSelf(target, (ParticleOptions) TensuraParticleTypes.SOUL.get(), 1.0);
            }
        }
    }

    // --- PASSIVE ATTACK: RESISTANCE STRIPPING & DEBUFFS ---

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        ManasSkillInstance instance = getHadesInstanceInSlot(attacker);
        if (instance == null) {
            return;
        }

        // Check if physical/melee
        if (source.isDirect() || source.getEntity() == source.getDirectEntity()) {
            Level level = attacker.level();
            stripSpecificResistances(target);

            level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8f, 1.2f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.3, 0.4, 0.3, 0.05);
            }

            int mastery = (int) instance.getMastery();
            // Soul Drain (Merciless ability)
            target.addEffect(new MobEffectInstance(TensuraMobEffects.SOUL_DRAIN, 200, 0, false, false));

            // Fatal Poison (Level 10)
            target.addEffect(new MobEffectInstance(TensuraMobEffects.FATAL_POISON, 200, 9, false, false));

            // Paralysis (Level 6)
            if (mastery >= 334) {
                target.addEffect(new MobEffectInstance(TensuraMobEffects.PARALYSIS, 200, 5, false, false));
            }
            // Corrosion (Level 8)
            if (mastery >= 667) {
                target.addEffect(new MobEffectInstance(TensuraMobEffects.CORROSION, 200, 7, false, false));
            }
            // Burden (Level 5)
            if (mastery >= 1000) {
                target.addEffect(new MobEffectInstance(TensuraMobEffects.BURDEN, 200, 4, false, false));
            }
            // Fragility (Level 5)
            if (mastery >= 1500) {
                target.addEffect(new MobEffectInstance(TensuraMobEffects.FRAGILITY, 200, 4, false, false));
            }
            // Marked For Death (Mysticism effect)
            if (instance.isMastered(attacker)) {
                var markedSupplier = io.github.Memoires.mysticism.registry.effects.MysticismMobEffects.MARKED_FOR_DEATH;
                if (markedSupplier != null && markedSupplier.get() != null) {
                    target.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(markedSupplier.get()), 200, 0, false, false));
                }
            }

            CompoundTag tag = instance.getOrCreateTag();
            int times = tag.getInt("activatedTimes") + 1;
            tag.putInt("activatedTimes", times);
            if (times % 10 == 0) {
                instance.addMasteryPoint(attacker);
            }
            instance.markDirty();
        }
    }

    private static void stripSpecificResistances(LivingEntity target) {
        SkillStorage storage = SkillAPI.getSkillsFrom(target);
        if (storage == null) {
            return;
        }

        for (ManasSkillInstance instance : storage.getLearnedSkills()) {
            if (instance.isToggled()) {
                ManasSkill skill = instance.getSkill();
                if (isStripTarget(skill)) {
                    instance.setToggled(false);
                    instance.onToggleOff(target);
                }
            }
        }
    }

    private static boolean isStripTarget(ManasSkill skill) {
        return skill == ResistanceSkills.CORROSION_RESISTANCE.get()
                || skill == ResistanceSkills.POISON_RESISTANCE.get()
                || skill == ResistanceSkills.PARALYSIS_RESISTANCE.get()
                || skill == ResistanceSkills.ABNORMAL_CONDITION_RESISTANCE.get()
                || skill == ResistanceSkills.CORROSION_NULLIFICATION.get()
                || skill == ResistanceSkills.POISON_NULLIFICATION.get()
                || skill == ResistanceSkills.PARALYSIS_NULLIFICATION.get()
                || skill == ResistanceSkills.ABNORMAL_CONDITION_NULLIFICATION.get();
    }

    // --- RECON PASSIVE DODGE ---

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        ManasSkillInstance instance = getHadesInstanceInSlot(entity);
        if (instance == null || !isReconActive(instance)) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.getEntity() != null) {
            boolean mastered = instance.isMastered(entity);
            float chance = entity.getRandom().nextFloat();
            float threshold = mastered ? 1.0f : 0.75f;
            if (chance < threshold) {
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.0f);
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
                }
                event.setCanceled(true);
            }
        }
    }

    // --- DEATH CLEANUP ---

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale != null && scale.hasModifier(ATTACK_MODIFIER)) {
            scale.removeModifier(ATTACK_MODIFIER);
            removeModifier(player, TensuraAttributes.MAX_AURA, ATTACK_MODIFIER);
            removeModifier(player, TensuraAttributes.MAX_MAGICULE, ATTACK_MODIFIER);

            IExistence existence = TensuraStorages.getExistenceFrom(player);
            if (existence != null) {
                double maxAura = EnergyHelper.getMaxAura(player);
                double maxMagicule = EnergyHelper.getMaxMagicule(player);
                existence.setAura(Math.min(existence.getAura() * 2.0, maxAura));
                existence.setMagicule(Math.min(existence.getMagicule() * 2.0, maxMagicule));
                existence.markDirty();
            }
        }
    }

    // --- HELPERS ---

    private static ManasSkillInstance getHadesInstanceInSlot(LivingEntity entity) {
        if (!(entity instanceof StorageHolder storageHolder)) {
            return null;
        }
        AbilityStorage storage = storageHolder.manasCore$getStorage(AbilityStorage.getKey());
        if (storage == null) {
            return null;
        }
        ManasSkill hades = UltimateSkills.HADES != null ? UltimateSkills.HADES.get() : null;
        if (hades == null || !storage.isAbilityInActivePreset(hades)) {
            return null;
        }
        SkillStorage skillStorage = SkillAPI.getSkillsFrom(entity);
        return skillStorage != null ? skillStorage.getSkill(hades).orElse(null) : null;
    }

    private static void applyModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double value, AttributeModifier.Operation op) {
        if (attribute == null) return;
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst != null) {
            inst.addOrReplacePermanentModifier(new AttributeModifier(id, value, op));
        }
    }

    private static void removeModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id) {
        if (attribute == null) return;
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst != null) {
            inst.removeModifier(id);
        }
    }
}
