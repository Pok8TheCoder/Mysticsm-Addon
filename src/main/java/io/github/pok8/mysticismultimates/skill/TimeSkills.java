package io.github.pok8.mysticismultimates.skill;

import dev.architectury.registry.registries.RegistrySupplier;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.entity.human.DwarfEntity;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.client.TimeSkillsClient;
import io.github.pok8.mysticismultimates.registry.CustomSkills;
import io.github.pok8.mysticismultimates.registry.ModAttachments;
import io.github.pok8.mysticismultimates.registry.ShadowEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TimeSkills {
    public static final int CHUNK_RADIUS = 4; // 4 chunks in each direction (9x9 = 81 chunks)
    public static final long DAY_TIME = 6000L;    // Midday noon
    public static final long NIGHT_TIME = 18000L; // Midnight

    private static final ThreadLocal<ArrayDeque<Long>> SPOOFED_TIME_STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Entity> CURRENT_CONTEXT_ENTITY = new ThreadLocal<>();

    // Tracks active fields by player UUID
    private static final Map<UUID, ChunkTimeField> ACTIVE_FIELDS = new ConcurrentHashMap<>();

    private TimeSkills() {
    }

    public static class TrueDaylightSkill extends Skill {
        public TrueDaylightSkill() {
            super(SkillType.EXTRA);
        }

        @Override
        public MutableComponent getName() {
            return Component.translatable("mysticism_ultimates.skill.true_daylight");
        }

        @Override
        public MutableComponent getSkillDescription() {
            return Component.translatable("mysticism_ultimates.skill.true_daylight.description");
        }

        @Override
        public ResourceLocation getSkillIcon() {
            return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "textures/skill/true_daylight.png");
        }

        @Override
        public double getDefaultAcquiringMagiculeCost() {
            return 35_000.0;
        }

        @Override
        public int getMaxMastery() {
            return 500;
        }

        @Override
        public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
            if (instance.isToggled()) {
                return true; // Can always turn off
            }
            if (entity instanceof Player player) {
                return !isSkillActive(player, CustomSkills.TRUE_NIGHTLIGHT);
            }
            return true;
        }

        @Override
        public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
            return true;
        }

        @Override
        public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
            if (entity instanceof ServerPlayer player) {
                if (isSkillActive(player, CustomSkills.TRUE_NIGHTLIGHT)) {
                    instance.setToggled(false);
                    instance.markDirty();
                    player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.true_daylight.conflict")
                            .withStyle(ChatFormatting.RED), true);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
                    return;
                }
                activateField(player, DAY_TIME);
                player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.true_daylight.on")
                        .withStyle(ChatFormatting.GOLD), true);
            }
        }

        @Override
        public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
            if (entity instanceof ServerPlayer player) {
                deactivateField(player);
                player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.true_daylight.off")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
        }

        @Override
        public void onTick(ManasSkillInstance instance, LivingEntity entity) {
            if (entity instanceof ServerPlayer player && instance.isToggled()) {
                tickField(player, DAY_TIME);
            }
        }

        @Override
        public void onPressed(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
            if (entity instanceof ServerPlayer player && !entity.level().isClientSide()) {
                if (!instance.isToggled()) {
                    if (isSkillActive(player, CustomSkills.TRUE_NIGHTLIGHT)) {
                        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.true_daylight.conflict")
                                .withStyle(ChatFormatting.RED), true);
                        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
                        return;
                    }
                    instance.setToggled(true);
                    onToggleOn(instance, entity);
                } else {
                    instance.setToggled(false);
                    onToggleOff(instance, entity);
                }
            }
        }
    }

    public static class TrueNightlightSkill extends Skill {
        public TrueNightlightSkill() {
            super(SkillType.EXTRA);
        }

        @Override
        public MutableComponent getName() {
            return Component.translatable("mysticism_ultimates.skill.true_nightlight");
        }

        @Override
        public MutableComponent getSkillDescription() {
            return Component.translatable("mysticism_ultimates.skill.true_nightlight.description");
        }

        @Override
        public ResourceLocation getSkillIcon() {
            return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "textures/skill/true_nightlight.png");
        }

        @Override
        public double getDefaultAcquiringMagiculeCost() {
            return 35_000.0;
        }

        @Override
        public int getMaxMastery() {
            return 500;
        }

        @Override
        public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
            if (instance.isToggled()) {
                return true; // Can always turn off
            }
            if (entity instanceof Player player) {
                return !isSkillActive(player, CustomSkills.TRUE_DAYLIGHT);
            }
            return true;
        }

        @Override
        public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
            return true;
        }

        @Override
        public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
            if (entity instanceof ServerPlayer player) {
                if (isSkillActive(player, CustomSkills.TRUE_DAYLIGHT)) {
                    instance.setToggled(false);
                    instance.markDirty();
                    player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.true_nightlight.conflict")
                            .withStyle(ChatFormatting.RED), true);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
                    return;
                }
                activateField(player, NIGHT_TIME);
                player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.true_nightlight.on")
                        .withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }

        @Override
        public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
            if (entity instanceof ServerPlayer player) {
                deactivateField(player);
                player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.true_nightlight.off")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }

        @Override
        public void onTick(ManasSkillInstance instance, LivingEntity entity) {
            if (entity instanceof ServerPlayer player && instance.isToggled()) {
                tickField(player, NIGHT_TIME);
            }
        }

        @Override
        public void onPressed(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
            if (entity instanceof ServerPlayer player && !entity.level().isClientSide()) {
                if (!instance.isToggled()) {
                    if (isSkillActive(player, CustomSkills.TRUE_DAYLIGHT)) {
                        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.true_nightlight.conflict")
                                .withStyle(ChatFormatting.RED), true);
                        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
                        return;
                    }
                    instance.setToggled(true);
                    onToggleOn(instance, entity);
                } else {
                    instance.setToggled(false);
                    onToggleOff(instance, entity);
                }
            }
        }
    }

    public static boolean isSkillActive(Player player, RegistrySupplier<ManasSkill> skillSupplier) {
        if (player == null || skillSupplier == null || skillSupplier.get() == null) {
            return false;
        }
        return SkillAPI.getSkillsFrom(player).getSkill(skillSupplier.get())
                .map(ManasSkillInstance::isToggled)
                .orElse(false);
    }

    public static final class ChunkTimeField {
        public final UUID casterId;
        public final ResourceKey<Level> dimension;
        public ChunkPos centerChunk;
        public final long customTime;
        public final Set<ChunkPos> activeChunks = new HashSet<>();

        public ChunkTimeField(UUID casterId, ResourceKey<Level> dimension, ChunkPos centerChunk, long customTime) {
            this.casterId = casterId;
            this.dimension = dimension;
            this.centerChunk = centerChunk;
            this.customTime = customTime;
        }
    }

    public static Set<ChunkPos> getChunksInRadius(ChunkPos center, int radius) {
        Set<ChunkPos> set = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                set.add(new ChunkPos(center.x + dx, center.z + dz));
            }
        }
        return set;
    }

    public static boolean isChunkCoveredByOther(UUID excludePlayer, ResourceKey<Level> dim, ChunkPos pos) {
        for (ChunkTimeField other : ACTIVE_FIELDS.values()) {
            if (!other.casterId.equals(excludePlayer) && other.dimension.equals(dim) && other.activeChunks.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    public static Long getEffectiveTimeForPlayer(ServerPlayer player) {
        if (player == null) return null;
        ChunkPos playerChunk = player.chunkPosition();
        ResourceKey<Level> dim = player.serverLevel().dimension();
        for (ChunkTimeField field : ACTIVE_FIELDS.values()) {
            if (field.dimension.equals(dim) && field.activeChunks.contains(playerChunk)) {
                return field.customTime;
            }
        }
        try {
            LevelChunk chunk = player.serverLevel().getChunk(playerChunk.x, playerChunk.z);
            if (chunk != null) {
                long time = ModAttachments.getChunkTime(chunk);
                if (time >= 0L) return time;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static Long getEffectiveTimeForEntity(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return getEffectiveTimeForPlayer(player);
        }
        if (entity != null && entity.level() instanceof ServerLevel serverLevel) {
            ChunkPos pos = entity.chunkPosition();
            ResourceKey<Level> dim = serverLevel.dimension();
            for (ChunkTimeField field : ACTIVE_FIELDS.values()) {
                if (field.dimension.equals(dim) && field.activeChunks.contains(pos)) {
                    return field.customTime;
                }
            }
            try {
                LevelChunk chunk = serverLevel.getChunk(pos.x, pos.z);
                if (chunk != null) {
                    long time = ModAttachments.getChunkTime(chunk);
                    if (time >= 0L) return time;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static Long getEffectiveTimeForBlockPos(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        if (level.isClientSide()) {
            return TimeSkillsClient.getClientEffectiveTime();
        }
        if (level instanceof ServerLevel serverLevel) {
            ChunkPos chunkPos = new ChunkPos(pos);
            ResourceKey<Level> dim = serverLevel.dimension();
            for (ChunkTimeField field : ACTIVE_FIELDS.values()) {
                if (field.dimension.equals(dim) && field.activeChunks.contains(chunkPos)) {
                    return field.customTime;
                }
            }
            try {
                if (serverLevel.hasChunk(chunkPos.x, chunkPos.z)) {
                    LevelChunk chunk = serverLevel.getChunk(chunkPos.x, chunkPos.z);
                    if (chunk != null) {
                        long time = ModAttachments.getChunkTime(chunk);
                        if (time >= 0L) return time;
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static void activateField(ServerPlayer player, long customTime) {
        ServerLevel level = player.serverLevel();
        ChunkPos center = player.chunkPosition();
        ChunkTimeField field = new ChunkTimeField(player.getUUID(), level.dimension(), center, customTime);
        Set<ChunkPos> chunks = getChunksInRadius(center, CHUNK_RADIUS);
        field.activeChunks.addAll(chunks);
        ACTIVE_FIELDS.put(player.getUUID(), field);

        for (ChunkPos pos : chunks) {
            LevelChunk chunk = level.getChunk(pos.x, pos.z);
            ModAttachments.setChunkTime(chunk, customTime);
            chunk.setUnsaved(true);
            for (ServerPlayer p : level.players()) {
                if (p.chunkPosition().equals(pos)) {
                    p.connection.send(new ClientboundSetTimePacket(level.getGameTime(), customTime, false));
                }
            }
        }
    }

    public static void deactivateField(ServerPlayer player) {
        ChunkTimeField field = ACTIVE_FIELDS.remove(player.getUUID());
        if (field != null) {
            ServerLevel level = player.serverLevel();
            for (ChunkPos pos : field.activeChunks) {
                if (!isChunkCoveredByOther(player.getUUID(), field.dimension, pos)) {
                    LevelChunk chunk = level.getChunk(pos.x, pos.z);
                    if (chunk != null) {
                        ModAttachments.setChunkTime(chunk, -1L);
                        chunk.setUnsaved(true);
                        for (ServerPlayer p : level.players()) {
                            if (p.chunkPosition().equals(pos)) {
                                p.connection.send(new ClientboundSetTimePacket(
                                        level.getGameTime(), level.getDayTime(),
                                        level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
                                p.removeEffect(MobEffects.DARKNESS);
                            }
                        }
                    }
                }
            }
            player.connection.send(new ClientboundSetTimePacket(
                    level.getGameTime(), level.getDayTime(),
                    level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
            player.removeEffect(MobEffects.DARKNESS);
        }
    }

    public static void tickField(ServerPlayer player, long customTime) {
        ChunkPos currentCenter = player.chunkPosition();
        ChunkTimeField field = ACTIVE_FIELDS.get(player.getUUID());
        if (field == null) {
            activateField(player, customTime);
            return;
        }

        ServerLevel level = player.serverLevel();
        // Dynamic area following: update chunks immediately whenever the player moves chunk!
        if (!currentCenter.equals(field.centerChunk)) {
            Set<ChunkPos> newChunkSet = getChunksInRadius(currentCenter, CHUNK_RADIUS);

            // Chunks leaving radius
            for (ChunkPos oldPos : field.activeChunks) {
                if (!newChunkSet.contains(oldPos)) {
                    if (!isChunkCoveredByOther(player.getUUID(), level.dimension(), oldPos)) {
                        LevelChunk chunk = level.getChunk(oldPos.x, oldPos.z);
                        if (chunk != null) {
                            ModAttachments.setChunkTime(chunk, -1L);
                            chunk.setUnsaved(true);
                            for (ServerPlayer p : level.players()) {
                                if (p.chunkPosition().equals(oldPos)) {
                                    p.connection.send(new ClientboundSetTimePacket(
                                            level.getGameTime(), level.getDayTime(),
                                            level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
                                    p.removeEffect(MobEffects.DARKNESS);
                                }
                            }
                        }
                    }
                }
            }

            // Chunks entering radius
            for (ChunkPos newPos : newChunkSet) {
                if (!field.activeChunks.contains(newPos)) {
                    LevelChunk chunk = level.getChunk(newPos.x, newPos.z);
                    ModAttachments.setChunkTime(chunk, customTime);
                    chunk.setUnsaved(true);
                    for (ServerPlayer p : level.players()) {
                        if (p.chunkPosition().equals(newPos)) {
                            p.connection.send(new ClientboundSetTimePacket(level.getGameTime(), customTime, false));
                        }
                    }
                }
            }

            field.centerChunk = currentCenter;
            field.activeChunks.clear();
            field.activeChunks.addAll(newChunkSet);
        }

        // Continual anti-flicker sync: every 10 ticks, refresh client time for players in range
        if (player.tickCount % 10 == 0) {
            player.connection.send(new ClientboundSetTimePacket(level.getGameTime(), customTime, false));
            for (ServerPlayer target : level.players()) {
                if (target != player && field.activeChunks.contains(target.chunkPosition())) {
                    target.connection.send(new ClientboundSetTimePacket(level.getGameTime(), customTime, false));
                }
            }
        }

        // Apply mechanics & buffs to entities inside the active 4-chunk area
        int minX = (field.centerChunk.x - CHUNK_RADIUS) << 4;
        int maxX = (field.centerChunk.x + CHUNK_RADIUS + 1) << 4;
        int minZ = (field.centerChunk.z - CHUNK_RADIUS) << 4;
        int maxZ = (field.centerChunk.z + CHUNK_RADIUS + 1) << 4;
        AABB chunkArea = new AABB(minX, level.getMinBuildHeight(), minZ, maxX, level.getMaxBuildHeight(), maxZ);

        boolean isDaylight = customTime < 12000L;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, chunkArea, e -> field.activeChunks.contains(e.chunkPosition()))) {
            if (isDaylight) {
                // True Daylight: Solar Boost + Day Perks
                if (living == player || player.isAlliedTo(living)) {
                    if (ShadowEffects.SOLAR_BOOST.get() != null) {
                        living.addEffect(new MobEffectInstance(ShadowEffects.SOLAR_BOOST, 60, 0, false, false, false));
                    }
                    living.removeEffect(MobEffects.DARKNESS);
                    living.removeEffect(MobEffects.BLINDNESS);
                }
                if (living instanceof Mob mob && isSunSensitive(mob)) {
                    if (level.canSeeSky(mob.blockPosition())) {
                        mob.igniteForSeconds(8);
                    }
                }
                if (living instanceof Phantom phantom) {
                    phantom.hurt(level.damageSources().onFire(), 2.0F);
                }
                if (living instanceof DwarfEntity dwarf) {
                    if (dwarf.isSleeping()) {
                        dwarf.stopSleeping();
                    }
                    dwarf.setWorking(true);
                    dwarf.getBrain().setActiveActivityIfPossible(Activity.WORK);
                } else if (living instanceof Villager villager) {
                    if (villager.isSleeping()) {
                        villager.stopSleeping();
                    }
                }
            } else {
                // True Nightlight: Lunar Boost + Night Vision for allies (NEVER apply darkness to caster/allies)
                if (living == player || player.isAlliedTo(living)) {
                    if (ShadowEffects.LUNAR_BOOST.get() != null) {
                        living.addEffect(new MobEffectInstance(ShadowEffects.LUNAR_BOOST, 60, 0, false, false, false));
                    }
                    // Night vision for allies so they have perfect sight under the night sky
                    living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 0, false, false, false));
                    living.removeEffect(MobEffects.DARKNESS);
                } else {
                    // Enemies get Darkness
                    living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
                }
                if (living.isOnFire() && isSunSensitive(living)) {
                    living.clearFire();
                }
            }
        }
    }

    private static boolean isSunSensitive(LivingEntity living) {
        return living instanceof Zombie || living instanceof Skeleton || living instanceof Phantom
                || living instanceof Drowned || living instanceof Stray || living instanceof WitherSkeleton;
    }

    // --- NEOFORGE EVENT LISTENERS ---

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel serverLevel) {
            CURRENT_CONTEXT_ENTITY.set(player);
            Long customTime = getEffectiveTimeForPlayer(player);
            if (customTime != null && customTime >= 0L && serverLevel.getLevelData() instanceof ServerLevelData levelData) {
                long realDayTime = serverLevel.getDayTime();
                SPOOFED_TIME_STACK.get().push(realDayTime);
                levelData.setDayTime(customTime);
                serverLevel.updateSkyBrightness();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel serverLevel) {
            CURRENT_CONTEXT_ENTITY.set(player);
            try {
                boolean dayActive = isSkillActive(player, CustomSkills.TRUE_DAYLIGHT);
                boolean nightActive = isSkillActive(player, CustomSkills.TRUE_NIGHTLIGHT);

                // If both are somehow toggled at the same time, force nightlight off
                if (dayActive && nightActive) {
                    if (CustomSkills.TRUE_NIGHTLIGHT != null && CustomSkills.TRUE_NIGHTLIGHT.get() != null) {
                        SkillAPI.getSkillsFrom(player).getSkill(CustomSkills.TRUE_NIGHTLIGHT.get()).ifPresent(inst -> {
                            inst.setToggled(false);
                            inst.getSkill().onToggleOff(inst, player);
                            inst.markDirty();
                        });
                    }
                    nightActive = false;
                }

                if (dayActive) {
                    tickField(player, DAY_TIME);
                } else if (nightActive) {
                    tickField(player, NIGHT_TIME);
                }
            } finally {
                ArrayDeque<Long> stack = SPOOFED_TIME_STACK.get();
                if (!stack.isEmpty()) {
                    long originalDayTime = stack.pop();
                    if (serverLevel.getLevelData() instanceof ServerLevelData levelData) {
                        levelData.setDayTime(originalDayTime);
                        serverLevel.updateSkyBrightness();
                    }
                }
                CURRENT_CONTEXT_ENTITY.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer) return; // Handled by onPlayerTickPre!
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        Long customTime = getEffectiveTimeForEntity(entity);
        if (customTime != null && customTime >= 0L && serverLevel.getLevelData() instanceof ServerLevelData levelData) {
            long realDayTime = serverLevel.getDayTime();
            SPOOFED_TIME_STACK.get().push(realDayTime);
            CURRENT_CONTEXT_ENTITY.set(entity);

            // Temporarily set the world's clock to the chunk's time for this entity's tick
            levelData.setDayTime(customTime);
            // Recalculate sky darkness so Mob#isSunBurnTick() and Fox/Monster light checks obey the chunk time!
            serverLevel.updateSkyBrightness();

            // Seamless waking up for Dwarfs and Villagers in daylight chunks
            if (customTime < 12000L) {
                if (entity instanceof DwarfEntity dwarf && dwarf.isSleeping()) {
                    dwarf.stopSleeping();
                    dwarf.setWorking(true);
                    dwarf.getBrain().setActiveActivityIfPossible(Activity.WORK);
                } else if (entity instanceof Villager villager && villager.isSleeping()) {
                    villager.stopSleeping();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer) return; // Handled by onPlayerTick!
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        ArrayDeque<Long> stack = SPOOFED_TIME_STACK.get();
        if (!stack.isEmpty()) {
            long originalDayTime = stack.pop();
            if (serverLevel.getLevelData() instanceof ServerLevelData levelData) {
                // Restore real world time immediately after the entity finishes its tick
                levelData.setDayTime(originalDayTime);
                serverLevel.updateSkyBrightness();
            }
            if (stack.isEmpty()) {
                CURRENT_CONTEXT_ENTITY.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onEnteringChunk(EntityEvent.EnteringSection event) {
        if (!event.didChunkChange()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        Long customTime = getEffectiveTimeForPlayer(player);

        if (customTime != null && customTime >= 0L) {
            // Send packet with doDaylightCycle = false so the client locks the sky to customTime
            player.connection.send(new ClientboundSetTimePacket(
                    level.getGameTime(), customTime, false
            ));
        } else {
            ChunkPos oldPos = new ChunkPos(event.getOldPos().x(), event.getOldPos().z());
            LevelChunk fromChunk = level.getChunk(oldPos.x, oldPos.z);
            if (fromChunk != null && ModAttachments.getChunkTime(fromChunk) >= 0L) {
                // Restore normal server time and daylight cycle rule
                player.connection.send(new ClientboundSetTimePacket(
                        level.getGameTime(), level.getDayTime(),
                        level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)
                ));
            }
        }
    }

    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos pos = event.getPos();
            ChunkPos chunkPos = new ChunkPos(pos);
            LevelChunk chunk = serverLevel.getChunk(chunkPos.x, chunkPos.z);
            if (chunk != null) {
                long customTime = ModAttachments.getChunkTime(chunk);
                if (customTime >= 12000L && customTime <= 24000L) {
                    event.setProblem(null); // Allow sleeping locally in nightlight chunk!
                } else if (customTime >= 0L && customTime < 12000L) {
                    event.setProblem(Player.BedSleepingProblem.NOT_POSSIBLE_NOW); // Prevent sleep in daylight chunk
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CURRENT_CONTEXT_ENTITY.set(player);
        }
        if (event.getTarget() instanceof DwarfEntity dwarf && dwarf.level() instanceof ServerLevel serverLevel) {
            LevelChunk chunk = serverLevel.getChunk(dwarf.chunkPosition().x, dwarf.chunkPosition().z);
            if (chunk != null) {
                long customTime = ModAttachments.getChunkTime(chunk);
                if (customTime >= 0L && customTime < 12000L) {
                    if (dwarf.isSleeping()) {
                        dwarf.stopSleeping();
                    }
                    dwarf.setWorking(true);
                    dwarf.getBrain().setActiveActivityIfPossible(Activity.WORK);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            CURRENT_CONTEXT_ENTITY.set(attacker);
        } else {
            CURRENT_CONTEXT_ENTITY.set(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        CURRENT_CONTEXT_ENTITY.remove();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            deactivateField(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            deactivateField(player);
        }
    }

    // --- CONTEXT ACCESSORS FOR MIXINS & QUERIES ---

    public static Entity getCurrentContextEntity() {
        return CURRENT_CONTEXT_ENTITY.get();
    }

    public static void setCurrentContextEntity(Entity entity) {
        CURRENT_CONTEXT_ENTITY.set(entity);
    }

    public static void clearCurrentContextEntity() {
        CURRENT_CONTEXT_ENTITY.remove();
    }

    public static Long getContextDayTime(Level level) {
        Entity entity = CURRENT_CONTEXT_ENTITY.get();
        if (entity != null && entity.level() == level) {
            Long time = getEffectiveTimeForEntity(entity);
            if (time != null) {
                return time;
            }
        }
        if (!SPOOFED_TIME_STACK.get().isEmpty() && level instanceof ServerLevel) {
            return level.getLevelData().getDayTime();
        }
        if (level instanceof ServerLevel serverLevel) {
            ResourceKey<Level> dim = serverLevel.dimension();
            Long singleTime = null;
            boolean multiple = false;
            for (ChunkTimeField field : ACTIVE_FIELDS.values()) {
                if (field.dimension.equals(dim)) {
                    if (singleTime == null) {
                        singleTime = field.customTime;
                    } else if (!singleTime.equals(field.customTime)) {
                        multiple = true;
                        break;
                    }
                }
            }
            if (singleTime != null && !multiple) {
                return singleTime;
            }
        }
        return null;
    }

    public static Boolean getContextIsDay(Level level) {
        Long time = getContextDayTime(level);
        if (time != null) {
            return time < 12000L;
        }
        return null;
    }

    public static Map<UUID, ChunkTimeField> getActiveFields() {
        return ACTIVE_FIELDS;
    }
}
