package io.github.pok8.mysticismultimates.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.entity.ai.goal.TensuraOwnerHurtByTargetGoal;
import io.github.manasmods.tensura.entity.ai.goal.TensuraOwnerHurtGoal;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OtherworldlySkill extends Skill {
    public static final int DAY_COOLDOWN = 1440; // 24 minutes (1440 seconds)
    public static final int MISS_COOLDOWN = 10;  // 10 seconds

    public OtherworldlySkill() {
        super(SkillType.UNIQUE);
    }

    @Override
    public int getModes(ManasSkillInstance instance) {
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
            case 1 -> "otherworldly.closed_loop";
            case 2 -> "otherworldly.gold_luck";
            default -> "otherworldly.shared_secrets";
        };
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable("mysticism_ultimates.skill.otherworldly");
    }

    @Override
    public MutableComponent getSkillDescription() {
        return Component.translatable("mysticism_ultimates.skill.otherworldly.description");
    }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "textures/skill/otherworldly.png");
    }

    @Override
    public double getDefaultAcquiringMagiculeCost() {
        return 100_000.0;
    }

    @Override
    public int getMaxMastery() {
        return 1500;
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
            // Passive toggle: Luck 5 and Hero of the Village 5 with 30s timer (600 ticks)
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 4, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 600, 4, false, false, true));
        }
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        // Mode 2: Gold Luck active maintainer
        if (instance.getTag().getBoolean("gold_luck_active")) {
            if (!player.hasEffect(io.github.pok8.mysticismultimates.registry.ShadowEffects.GOLD_LUCK)
                    || player.getEffect(io.github.pok8.mysticismultimates.registry.ShadowEffects.GOLD_LUCK).getDuration() < 100) {
                player.addEffect(new MobEffectInstance(io.github.pok8.mysticismultimates.registry.ShadowEffects.GOLD_LUCK, 600, 0, false, false, true));
            }
        }
        if (!instance.isToggled()) {
            return;
        }
        // Grant effects with a timer of 30s (600 ticks) and refresh every 5s (100 ticks)
        if (player.tickCount % 100 == 0 || !player.hasEffect(MobEffects.LUCK) || !player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 4, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 600, 4, false, false, true));
        }
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            player.removeEffect(MobEffects.LUCK);
            player.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
            player.removeEffect(io.github.pok8.mysticismultimates.registry.ShadowEffects.GOLD_LUCK);
        }
    }

    public static final String DIM_TAG_PREFIX = "mysticism_visited_dim:";
    public static final ResourceLocation DIM_OVERWORLD = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    public static final ResourceLocation DIM_NETHER = ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether");
    public static final ResourceLocation DIM_END = ResourceLocation.fromNamespaceAndPath("minecraft", "the_end");
    public static final ResourceLocation DIM_HELL = ResourceLocation.fromNamespaceAndPath("tensura", "hell");
    public static final ResourceLocation DIM_LABYRINTH = ResourceLocation.fromNamespaceAndPath("tensura", "labyrinth");

    public static final List<ResourceLocation> REQUIRED_DIMENSIONS = List.of(
            DIM_OVERWORLD,
            DIM_NETHER,
            DIM_END,
            DIM_HELL,
            DIM_LABYRINTH
    );

    public static void onPlayerChangedDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recordDimension(player, event.getTo().location());
        }
    }

    public static void recordDimension(ServerPlayer player, ResourceLocation dim) {
        if (player == null || dim == null) {
            return;
        }
        player.addTag(DIM_TAG_PREFIX + dim.toString());
    }

    public static boolean hasVisited(ServerPlayer player, ResourceLocation dim) {
        if (player == null || dim == null) {
            return false;
        }
        if (player.getTags().contains(DIM_TAG_PREFIX + dim.toString())) {
            return true;
        }

        // Fallbacks: Check current location, spawn point, or advancements
        if (dim.equals(DIM_OVERWORLD)) {
            if (player.serverLevel().dimension().location().equals(DIM_OVERWORLD)
                    || (player.getRespawnDimension() != null && player.getRespawnDimension().location().equals(DIM_OVERWORLD))) {
                recordDimension(player, DIM_OVERWORLD);
                return true;
            }
        } else if (dim.equals(DIM_NETHER)) {
            if (hasAdvancement(player, ResourceLocation.fromNamespaceAndPath("minecraft", "story/enter_the_nether"))) {
                recordDimension(player, DIM_NETHER);
                return true;
            }
        } else if (dim.equals(DIM_END)) {
            if (hasAdvancement(player, ResourceLocation.fromNamespaceAndPath("minecraft", "story/enter_the_end"))) {
                recordDimension(player, DIM_END);
                return true;
            }
        } else if (dim.equals(DIM_HELL)) {
            if (hasAdvancement(player, ResourceLocation.fromNamespaceAndPath("tensura", "hell"))) {
                recordDimension(player, DIM_HELL);
                return true;
            }
        } else if (dim.equals(DIM_LABYRINTH)) {
            if (hasAdvancement(player, ResourceLocation.fromNamespaceAndPath("tensura", "labyrinth"))) {
                recordDimension(player, DIM_LABYRINTH);
                return true;
            }
        }
        return false;
    }

    public static List<ResourceLocation> getMissingDimensions(ServerPlayer player) {
        recordDimension(player, player.serverLevel().dimension().location());
        List<ResourceLocation> missing = new ArrayList<>();
        for (ResourceLocation dim : REQUIRED_DIMENSIONS) {
            if (!hasVisited(player, dim)) {
                missing.add(dim);
            }
        }
        return missing;
    }

    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation advId) {
        if (player == null || player.server == null) {
            return false;
        }
        net.minecraft.advancements.AdvancementHolder holder = player.server.getAdvancements().get(advId);
        if (holder == null) {
            return false;
        }
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
            return;
        }

        int activeMode = Math.floorMod(mode, getModes(instance));
        if (activeMode == 2) {
            castGoldLuck(player, instance);
            return;
        }

        // Shared cooldown check across both active abilities (modes 0 and 1)
        int remaining = Math.max(instance.getCoolDown(0), instance.getCoolDown(1));
        if (remaining > 0 || instance.onCoolDown(0) || instance.onCoolDown(1)) {
            int mins = remaining / 60;
            int secs = remaining % 60;
            String timeStr = mins > 0 ? (mins + "m " + secs + "s") : (secs + "s");
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.otherworldly.cooldown", timeStr)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (activeMode == 0) {
            castSharedSecrets(player, instance);
        } else {
            castClosedLoop(player, instance);
        }
    }

    private void castGoldLuck(ServerPlayer player, ManasSkillInstance instance) {
        int points = instance.getTag().getInt("gold_luck_learn_points");
        if (points < 100) {
            if (instance.onCoolDown(2)) {
                int rem = instance.getCoolDown(2);
                player.displayClientMessage(
                        Component.translatable("mysticism_ultimates.skill.otherworldly.gold_luck.learning_cooldown", rem)
                                .withStyle(ChatFormatting.RED), true);
                return;
            }
            points += 5;
            instance.getTag().putInt("gold_luck_learn_points", points);
            instance.markDirty();

            if (points < 100) {
                instance.setCoolDown(10, 2);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 16, 0.3, 0.4, 0.3, 0.1);
                }
                player.displayClientMessage(
                        Component.translatable("mysticism_ultimates.skill.otherworldly.gold_luck.learning", points, 100)
                                .withStyle(ChatFormatting.GOLD), true);
            } else {
                instance.setCoolDown(0, 2);
                player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.2, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
                }
                player.displayClientMessage(
                        Component.translatable("mysticism_ultimates.skill.otherworldly.gold_luck.mastered")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            }
        } else {
            // Mastered: 0 cooldown toggle!
            boolean active = !instance.getTag().getBoolean("gold_luck_active");
            instance.getTag().putBoolean("gold_luck_active", active);
            instance.markDirty();
            if (active) {
                player.addEffect(new MobEffectInstance(io.github.pok8.mysticismultimates.registry.ShadowEffects.GOLD_LUCK, 600, 0, false, false, true));
                player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.3F);
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.3, 0.4, 0.3, 0.05);
                }
                player.displayClientMessage(
                        Component.translatable("mysticism_ultimates.skill.otherworldly.gold_luck.activated")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            } else {
                player.removeEffect(io.github.pok8.mysticismultimates.registry.ShadowEffects.GOLD_LUCK);
                player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.5F);
                player.displayClientMessage(
                        Component.translatable("mysticism_ultimates.skill.otherworldly.gold_luck.deactivated")
                                .withStyle(ChatFormatting.GRAY), true);
            }
        }
    }

    private void applySharedCooldown(ManasSkillInstance instance, int ticks) {
        instance.setCoolDowns(ticks);
        instance.setCoolDown(ticks, 0);
        instance.setCoolDown(ticks, 1);
        instance.markDirty();
    }

    private void castSharedSecrets(ServerPlayer player, ManasSkillInstance instance) {
        // Enforce required dimension visits: Overworld, Nether, End, Hell, Labyrinth
        List<ResourceLocation> missing = getMissingDimensions(player);
        if (!missing.isEmpty()) {
            List<String> missingNames = missing.stream().map(d -> switch (d.getPath()) {
                case "overworld" -> "Overworld";
                case "the_nether" -> "Nether";
                case "the_end" -> "The End";
                case "hell" -> "Hell";
                case "labyrinth" -> "Labyrinth";
                default -> d.getPath();
            }).toList();
            applySharedCooldown(instance, MISS_COOLDOWN);
            player.displayClientMessage(
                    Component.translatable("mysticism_ultimates.skill.otherworldly.shared_secrets.missing_dimensions",
                            String.join(", ", missingNames)).withStyle(ChatFormatting.RED),
                    true
            );
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        LivingEntity target = look(player, LivingEntity.class, 12.0);
        if (target == null) {
            // Missed targeting: 10s cooldown
            applySharedCooldown(instance, MISS_COOLDOWN);
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.otherworldly.shared_secrets.miss")
                    .withStyle(ChatFormatting.YELLOW), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        // Check if entity already has the Otherwordly tag
        if (target.getTags().contains("Otherwordly") || target.getTags().contains("Otherworldly")
                || target.getTags().contains("otherworldly")) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.otherworldly.shared_secrets.already_used", target.getDisplayName())
                    .withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        // 1. Strip entity of any boss, mini boss, no mind control, no plunder tags
        target.getTags().removeIf(t -> {
            String lower = t.toLowerCase(Locale.ROOT);
            return lower.contains("boss") || lower.contains("mind_control") || lower.contains("plunder")
                    || lower.contains("immune") || lower.contains("no_") || lower.contains("not_");
        });
        target.addTag("Otherwordly");
        target.addTag("Otherworldly");
        target.addTag("otherworldly_stripped");
        target.addTag("nameable");
        target.addTag("Nameable");
        target.addTag("tensura:nameable");
        target.addTag("tamable");
        target.addTag("Tamable");
        target.addTag("mind_controllable");
        target.addTag("MindControllable");
        target.addTag("plunderable");
        target.addTag("Plunderable");

        // 2. Make entity vulnerable and nameable in existence storage, clear existing owner/summoner blockers
        ExistenceStorage existence = ShadowArmy.existence(target);
        if (existence != null) {
            existence.setNameable(true);
            existence.setName(null);
            existence.setPermanentOwner(null);
            existence.setSummoner(null);
            existence.setSummonedSecond(0);
            existence.setSkippingEPDrop(true);
            existence.markDirty();
        }

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    24, 0.5, 0.5, 0.5, 0.1);
            level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    16, 0.4, 0.4, 0.4, 0.1);
        }
        player.level().playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);

        // Cooldown: 24 minutes, grants 100 mastery points
        applySharedCooldown(instance, DAY_COOLDOWN);
        instance.addMasteryPoint(player, 100);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.otherworldly.shared_secrets.hit", target.getDisplayName())
                .withStyle(ChatFormatting.GOLD), false);
    }

    private void castClosedLoop(ServerPlayer player, ManasSkillInstance instance) {
        if (!instance.isMastered(player)) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.otherworldly.closed_loop.not_mastered")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        List<ManasSkillInstance> tempSkills = new ArrayList<>();
        for (ManasSkillInstance learned : SkillAPI.getSkillsFrom(player).getLearnedSkills()) {
            if (learned != null && learned.isTemporarySkill()) {
                tempSkills.add(learned);
            }
        }

        if (tempSkills.isEmpty()) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.otherworldly.closed_loop.none")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        ManasSkillInstance chosen = tempSkills.get(player.getRandom().nextInt(tempSkills.size()));
        chosen.setRemoveTime(-1);
        chosen.markDirty();

        applySharedCooldown(instance, DAY_COOLDOWN);
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.otherworldly.closed_loop.success", chosen.getDisplayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    private static <T extends Entity> T look(Player player, Class<T> type, double range) {
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0);
        T best = null;
        double bestDot = 0.80;
        for (T entity : player.level().getEntitiesOfClass(type, box, entity -> entity != player && entity.isAlive())) {
            double dot = entity.getEyePosition().subtract(player.getEyePosition()).normalize().dot(player.getLookAngle());
            if (dot > bestDot && player.distanceTo(entity) <= range) {
                best = entity;
                bestDot = dot;
            }
        }
        return best;
    }
}
