package io.github.pok8.mysticismultimates.skill;

import io.github.Memoires.mysticism.registry.skill.ExtraMysticismSkills;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.subclass.ISpatialStorage;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import net.minecraft.core.HolderLookup;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.extra.SenseHeatSourceSkill;
import io.github.manasmods.tensura.ability.skill.extra.ThoughtAccelerationSkill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.world.TensuraGameRules;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.entity.ShadowSubordinateEntity;
import io.github.pok8.mysticismultimates.registry.ShadowEffects;
import io.github.pok8.mysticismultimates.registry.UltimateSkills;
import io.github.pok8.mysticismultimates.registry.UniqueShadowSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LavernaSkill extends DifficultUltimateSkill implements ISpatialStorage {
    private static final Map<UUID, Map<BlockPos, CompoundTag>> DOMAINS = new HashMap<>();
    private static final Map<UUID, BlockPos> DOMAIN_CENTERS = new HashMap<>();
    private static final ResourceLocation HEAT_SENSE = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "laverna_heat_sense");
    private static final String DYSTOPIA = "EuclideanDystopia";
    private static final Map<UUID, Long> DYSTOPIA_SEEDS = new HashMap<>();

    public LavernaSkill(String[] modes) {
        super("laverna", modes);
    }

    @Override
    public int getMaxMastery() {
        return 3000;
    }

    @Override
    public double getDefaultAcquiringMagiculeCost() {
        return 1_000_000.0;
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double cost) {
        ManasSkill vainglory = UniqueShadowSkills.VAINGLORY.get();
        ManasSkill domination = ExtraMysticismSkills.DARKNESS_DOMINATION.get();
        return vainglory != null && domination != null
                && SkillUtils.isSkillMastered(player, vainglory)
                && SkillUtils.isSkillMastered(player, domination)
                && ShadowSpirits.hasGreaterDarkness(player);
    }

    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return instance.getMastery() >= 0.0;
    }

    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        ThoughtAccelerationSkill.onToggle(instance, entity, instance.getSkill().getRegistryName(), true);
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        ThoughtAccelerationSkill.onToggle(instance, entity, instance.getSkill().getRegistryName(), false);
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        ShadowArmy.maintain(player, instance);
        maintainHeatSense(player, instance.isToggled());
        boostArmy(player, instance);
        if (!instance.isToggled()) {
            return;
        }
        if (TensuraMobEffects.PRESENCE_SENSE.get() != null) {
            player.addEffect(new MobEffectInstance(TensuraMobEffects.PRESENCE_SENSE, 200, 1, false, false, true));
        }
        if (TensuraMobEffects.AUDITORY_SENSE.get() != null) {
            player.addEffect(new MobEffectInstance(TensuraMobEffects.AUDITORY_SENSE, 200, 0, false, false, true));
        }
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        switch (Math.floorMod(mode, getModes(instance))) {
            case 0 -> blitz(player, instance);
            case 1 -> ShadowArmy.storeSubordinate(player, instance);
            case 2 -> ShadowArmy.toggleMerge(player, instance);
            case 3 -> toggleDystopia(player, instance);
            case 4 -> toggleGarden(player, instance);
            case 5 -> ShadowPit.open(player, instance);
            case 6 -> ShadowPit.toggleArmy(player, instance);
            default -> {
            }
        }
    }

    @Override
    public SpatialStorageContainer getSpatialStorage(ManasSkillInstance instance, HolderLookup.Provider provider) {
        return ShadowPit.load(instance, provider, ShadowPit.LAVERNA_SLOTS);
    }

    @Override
    public void onForgetSkill(ManasSkillInstance instance, LivingEntity entity) {
        restoreDomain(entity);
        DYSTOPIA_SEEDS.remove(entity.getUUID());
        super.onForgetSkill(instance, entity);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ManasSkill laverna = UltimateSkills.LAVERNA.get();
        if (laverna == null) {
            return;
        }
        ManasSkillInstance instance = SkillUtils.getSkillOrNull(player, laverna);
        if (instance != null && instance.getOrCreateTag().getBoolean(DYSTOPIA)) {
            maintainDystopia(player);
        }
    }

    private static void toggleDystopia(ServerPlayer player, ManasSkillInstance instance) {
        CompoundTag tag = instance.getOrCreateTag();
        boolean active = !tag.getBoolean(DYSTOPIA);
        tag.putBoolean(DYSTOPIA, active);
        instance.markDirty();
        if (active) {
            DYSTOPIA_SEEDS.put(player.getUUID(), player.getRandom().nextLong());
            paintDomain(player, player.blockPosition());
        } else {
            restoreDomain(player);
            DYSTOPIA_SEEDS.remove(player.getUUID());
        }
        player.displayClientMessage(Component.translatable(
                "mysticism_ultimates.skill.laverna.euclidean_dystopia.toggle",
                Component.translatable(active ? "mysticism_ultimates.skill.laverna.on" : "mysticism_ultimates.skill.laverna.off"))
                .withStyle(active ? ChatFormatting.DARK_PURPLE : ChatFormatting.RED), true);
    }

    private static void maintainDystopia(ServerPlayer player) {
        BlockPos center = player.blockPosition();
        if (!center.equals(DOMAIN_CENTERS.get(player.getUUID()))) {
            paintDomain(player, center);
        }
        grasp(player, player.tickCount);
    }

    public static void restoreDomain(LivingEntity entity) {
        Map<BlockPos, CompoundTag> saved = DOMAINS.remove(entity.getUUID());
        DOMAIN_CENTERS.remove(entity.getUUID());
        if (saved == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        for (Map.Entry<BlockPos, CompoundTag> entry : saved.entrySet()) {
            BlockState state = NbtUtils.readBlockState(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK), entry.getValue());
            level.setBlock(entry.getKey(), state, 3);
        }
    }

    private static void paintDomain(ServerPlayer player, BlockPos center) {
        restoreDomain(player);
        if (!(player.level() instanceof ServerLevel level) || !TensuraGameRules.canSkillGrief(level)) {
            return;
        }
        Map<BlockPos, CompoundTag> saved = new HashMap<>();
        int radius = 32;
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        net.minecraft.world.phys.AABB body = player.getBoundingBox().inflate(0.15);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            int dx = pos.getX() - center.getX();
            int dy = pos.getY() - center.getY();
            int dz = pos.getZ() - center.getZ();
            if (dx * dx + dy * dy + dz * dz > radius * radius) {
                continue;
            }
            if (body.intersects(new net.minecraft.world.phys.AABB(pos))) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty() || state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                continue;
            }
            saved.put(pos.immutable(), NbtUtils.writeBlockState(state));
            level.setBlock(pos, dystopiaBlock(player, pos, state), 3);
        }
        DOMAINS.put(player.getUUID(), saved);
        DOMAIN_CENTERS.put(player.getUUID(), center.immutable());
    }

    private static final BlockState[] DYSTOPIA_PALETTE = new BlockState[] {
            Blocks.CRYING_OBSIDIAN.defaultBlockState(),
            Blocks.OBSIDIAN.defaultBlockState(),
            Blocks.BLACK_CONCRETE.defaultBlockState()
    };

    private static BlockState dystopiaBlock(ServerPlayer player, BlockPos pos, BlockState state) {
        if (state.is(BlockTags.LEAVES)) {
            return Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
        }
        long mixed = DYSTOPIA_SEEDS.getOrDefault(player.getUUID(), 0L);
        mixed ^= Integer.toUnsignedLong(pos.getX()) * 0x9E3779B97F4A7C15L;
        mixed = Long.rotateLeft(mixed, 21);
        mixed ^= Integer.toUnsignedLong(pos.getY()) * 0xBF58476D1CE4E5B9L;
        mixed = Long.rotateLeft(mixed, 21);
        mixed ^= Integer.toUnsignedLong(pos.getZ()) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        int index = Math.floorMod((int) (mixed >>> 16), DYSTOPIA_PALETTE.length);
        return DYSTOPIA_PALETTE[index];
    }

    private static void grasp(ServerPlayer player, int heldTicks) {
        AABB area = player.getBoundingBox().inflate(15.0);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && entity.isAlive() && !player.getUUID().equals(ownerOf(entity)))) {
            if (player.isAlliedTo(target)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(ShadowEffects.SHADOW_GRASP, 40, 0, false, false, true));
            if (heldTicks % 20 == 0) {
                ExistenceStorage existence = ShadowArmy.existence(target);
                if (existence != null) {
                    existence.setSpiritualHealth(Math.max(0.0, existence.getSpiritualHealth() * 0.8));
                    existence.markDirty();
                }
            }
        }
    }

    private static UUID ownerOf(LivingEntity entity) {
        return entity instanceof ShadowSubordinateEntity soldier ? soldier.getOwnerUUID() : null;
    }

    public static final int BLITZ_COOLDOWN = 3000; // 3000 seconds

    private static void blitz(ServerPlayer player, ManasSkillInstance instance) {
        if (instance.onCoolDown(0)) {
            int remaining = instance.getCoolDown(0);
            int mins = remaining / 60;
            int secs = remaining % 60;
            String timeStr = mins > 0 ? (mins + "m " + secs + "s") : (secs + "s");
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.laverna.blitz.cooldown", timeStr).withStyle(ChatFormatting.RED), false);
            return;
        }
        AABB area = player.getBoundingBox().inflate(15.0);
        var soldiers = player.level().getEntitiesOfClass(ShadowSubordinateEntity.class, area, soldier -> player.getUUID().equals(soldier.getOwnerUUID()) && soldier.isAlive());
        if (soldiers.isEmpty()) {
            player.displayClientMessage(Component.translatable("tensura.targeting.not_targeted").withStyle(ChatFormatting.RED), false);
            return;
        }
        double totalEp = 0.0;
        int count = 0;
        for (ShadowSubordinateEntity soldier : soldiers) {
            double ep = Math.max(soldier.sourceEp(), ShadowArmy.getEffectiveEP(soldier));
            totalEp += ep;
            for (ManasSkillInstance known : SkillAPI.getSkillsFrom(soldier).getLearnedSkills()) {
                ManasSkill skill = known.getSkill();
                if (skill == null || known.isTemporarySkill()) {
                    continue;
                }
                if (SkillHelper.learnSkill(player, skill)) {
                    player.displayClientMessage(Component.translatable("tensura.skill.acquire", skill.getName()).withStyle(ChatFormatting.GOLD), false);
                } else if (SkillUtils.hasSkill(player, skill)) {
                    player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.laverna.blitz.already_had", skill.getName()).withStyle(ChatFormatting.RED), false);
                }
            }
            soldier.kill();
            instance.addMasteryPoint(player, 5);
            count++;
        }
        if (totalEp > 0.0) {
            double half = totalEp / 2.0;
            EnergyHelper.gainAura(player, half, EnergyHelper.GainType.MAX);
            EnergyHelper.gainMagicule(player, half, EnergyHelper.GainType.MAX);
            ExistenceStorage existence = ShadowArmy.existence(player);
            if (existence != null) {
                existence.setGainedEP(existence.getGainedEP() + totalEp);
                existence.markDirty();
            }
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.laverna.blitz.success", count, (long) totalEp).withStyle(ChatFormatting.GOLD), false);
        instance.setCoolDown(BLITZ_COOLDOWN, 0);
        instance.markDirty();
    }

    private static void maintainHeatSense(ServerPlayer player, boolean active) {
        var attribute = player.getAttribute(TensuraAttributes.HEAT_SENSE_RADIUS);
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

    private static void boostArmy(ServerPlayer player, ManasSkillInstance instance) {
        if (ShadowEffects.SOLAR_BOOST.get() == null || ShadowEffects.LUNAR_BOOST.get() == null) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(30.0);
        for (ShadowSubordinateEntity soldier : player.level().getEntitiesOfClass(ShadowSubordinateEntity.class, area, entity -> player.getUUID().equals(entity.getOwnerUUID()))) {
            soldier.addEffect(new MobEffectInstance(ShadowEffects.SOLAR_BOOST, 200, 0, false, false, false));
            soldier.addEffect(new MobEffectInstance(ShadowEffects.LUNAR_BOOST, 200, 0, false, false, false));
        }
        if (instance.getOrCreateTag().getBoolean(ShadowArmy.MERGED)) {
            player.addEffect(new MobEffectInstance(ShadowEffects.SOLAR_BOOST, 200, 0, false, false, false));
            player.addEffect(new MobEffectInstance(ShadowEffects.LUNAR_BOOST, 200, 0, false, false, false));
        }
    }

    private static void toggleGarden(ServerPlayer player, ManasSkillInstance instance) {
        boolean active = !instance.getOrCreateTag().getBoolean(ShadowArmy.GARDEN);
        instance.getOrCreateTag().putBoolean(ShadowArmy.GARDEN, active);
        instance.markDirty();
        player.displayClientMessage(Component.translatable(
                "mysticism_ultimates.skill.laverna.shadow_garden.toggle",
                Component.translatable(active ? "mysticism_ultimates.skill.laverna.on" : "mysticism_ultimates.skill.laverna.off"))
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED), true);
    }
}
