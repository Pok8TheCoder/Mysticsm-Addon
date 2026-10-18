package io.github.pok8.mysticismultimates.skill;

import com.mojang.datafixers.util.Pair;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.EntityEvents;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.skill.unique.GuardianSkill;
import io.github.manasmods.tensura.ability.skill.unique.PredatorSkill;
import io.github.manasmods.tensura.ability.subclass.ISpatialStorage;
import io.github.manasmods.tensura.data.TensuraBlockTags;
import io.github.manasmods.tensura.data.TensuraEntityTags;
import io.github.manasmods.tensura.data.TensuraSkillTags;
import io.github.manasmods.tensura.data.disolving.ItemDissolving;
import io.github.manasmods.tensura.effect.template.TensuraMobEffect;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.entity.magic.beam.PredatorMistProjectile;
import io.github.manasmods.tensura.event.TensuraSkillEvents;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import io.github.manasmods.tensura.registry.data.TensuraCustomData;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.entity.MiscEntityTypes;
import io.github.manasmods.tensura.registry.sound.TensuraSoundEvents;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.IAbility;
import io.github.manasmods.tensura.storage.effect.IEffect;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.util.SubordinateHelper;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.registry.UltimateSkills;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import io.github.pok8.mysticismultimates.menu.SkillPickMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class BeelzebubSkill extends DifficultUltimateSkill implements ISpatialStorage {
    static final int SLOTS = 256;
    static final int STACK_SIZE = 1337;
    private static final float PREDATION_DAMAGE = 50.0f;
    private static final double PREDATION_RANGE = 10.0;
    private static final double PREDATION_RANGE_MASTERED = 15.0;
    private static final ResourceLocation WALL_ARMOR = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "beelzebub_wall_armor");
    private static final ResourceLocation WALL_KNOCK = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "beelzebub_wall_knock");
    private static boolean redirecting;

    public BeelzebubSkill(String id, String[] modes) {
        super(id, modes);
    }

    public static void register() {
        EntityEvents.LIVING_DAMAGE.register(BeelzebubSkill::redirect);
        NeoForge.EVENT_BUS.addListener(BeelzebubSkill::onLivingDeath);
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
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        applyWall(entity, instance.isMastered(entity));
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        clearWall(entity);
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (instance.isToggled() && entity.level() instanceof ServerLevel level && entity.tickCount % 10 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 4, 0.3, 0.4, 0.3, 0.0);
        }
    }

    @Override
    public SpatialStorageContainer getSpatialStorage(ManasSkillInstance instance, HolderLookup.Provider provider) {
        SpatialStorageContainer container = new SpatialStorageContainer(SLOTS, STACK_SIZE);
        container.fromTag(instance.getOrCreateTag().getList("SpatialStorage", 10), provider);
        return container;
    }

    @Override
    public boolean canIgnoreCoolDown(ManasSkillInstance instance, LivingEntity entity, int mode) {
        return "beelzebub.predation".equals(getModeId(instance, mode)) && entity.isShiftKeyDown();
    }

    @Override
    public boolean canScroll(ManasSkillInstance instance, LivingEntity entity, int mode) {
        return "beelzebub.predation".equals(getModeId(instance, mode)) && instance.getMastery() >= 0.0;
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (entity.level().isClientSide()) {
            return;
        }
        String modeId = getModeId(instance, mode);
        if ("beelzebub.stomach".equals(modeId) && entity instanceof Player player) {
            openSpatialStorage(player, instance);
            return;
        }
        if ("beelzebub.predation".equals(modeId)) {
            CompoundTag tag = instance.getOrCreateTag();
            tag.putInt("Mist", 0);
            if (entity.isShiftKeyDown() && entity instanceof Player player) {
                cycleBlockMode(player, tag);
            }
            instance.markDirty();
            return;
        }
        if (!(entity instanceof Player player)) {
            return;
        }
        switch (modeId) {
            case "beelzebub.provide" -> provide(player, instance, mode);
            case "beelzebub.receive" -> receive(player, instance, mode);
            case "beelzebub.isolation" -> isolate(player, instance, mode);
            case "beelzebub.substitution" -> armSubstitution(player, instance);
            default -> {
            }
        }
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (entity.level().isClientSide()) {
            return false;
        }
        String modeId = getModeId(instance, mode);
        if ("beelzebub.corrosion".equals(modeId)) {
            return corrode(instance, entity, heldTicks, mode);
        }
        if (!"beelzebub.predation".equals(modeId)) {
            return false;
        }
        if (instance.onCoolDown(mode) || entity.isShiftKeyDown()) {
            return false;
        }
        repairPredationList(instance);
        CompoundTag tag = instance.getOrCreateTag();
        if (tag.getDouble("range") < 3.0) {
            tag.putDouble("range", instance.isMastered(entity) ? PREDATION_RANGE_MASTERED : PREDATION_RANGE);
            instance.markDirty();
        }
        if (heldTicks > 0 && heldTicks % 20 == 0) {
            instance.addMasteryPoint(entity);
        }
        PredatorMistProjectile.spawnPredationMist(
                MiscEntityTypes.GLUTTONY_MIST.get(),
                entity,
                instance,
                mode,
                PREDATION_DAMAGE,
                Pair.of(0.0, 0.0),
                (float) tag.getDouble("range"),
                1,
                true);
        if (entity instanceof Player player && entity.level() instanceof ServerLevel level) {
            devourTerrain(instance, player, level, tag.getInt("blockMode"), tag.getDouble("range"));
            if (heldTicks % 10 == 0) {
                devourSkills(player, tag.getDouble("range"));
            }
        }
        if (heldTicks % 3 == 0) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), TensuraSoundEvents.PREDATION.get(), SoundSource.PLAYERS, 1.0f, 0.8f + entity.getRandom().nextFloat() * 0.4f);
        }
        return true;
    }

    @Override
    public void onScroll(ManasSkillInstance instance, LivingEntity entity, double delta, int mode) {
        if (!(entity instanceof Player player) || !"beelzebub.predation".equals(getModeId(instance, mode))) {
            return;
        }
        CompoundTag tag = instance.getOrCreateTag();
        double max = instance.isMastered(entity) ? PREDATION_RANGE_MASTERED : PREDATION_RANGE;
        double next = tag.getDouble("range") + delta;
        if (next > max) {
            next = max;
        } else if (next < 3.0) {
            next = 3.0;
        }
        if (next == tag.getDouble("range")) {
            return;
        }
        tag.putDouble("range", next);
        instance.markDirty();
        player.displayClientMessage(Component.translatable("tensura.skill.range", next).withStyle(ChatFormatting.DARK_AQUA), true);
    }

    private void devourSkills(Player player, double range) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        int limit = Math.max(1, PredatorSkill.CONFIG.predationSkillNumber);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), living -> living != player && living.isAlive() && !SubordinateHelper.isAlly(player, living))) {
            if (target.getType().is(TensuraEntityTags.NO_SKILL_PLUNDER)) {
                continue;
            }
            Vec3 to = target.getEyePosition().subtract(eye);
            double distance = to.length();
            if (distance > range || distance < 1.0e-4 || to.normalize().dot(look) < 0.5) {
                continue;
            }
            SkillStorage storage = SkillAPI.getSkillsFrom(target);
            if (storage == null) {
                continue;
            }
            int taken = 0;
            for (ManasSkillInstance learned : new ArrayList<>(storage.getLearnedSkills())) {
                if (taken >= limit || !canPredate(learned)) {
                    continue;
                }
                ManasSkill skill = learned.getSkill();
                if (SkillUtils.hasSkill(player, skill)) {
                    continue;
                }
                ManasSkill allowed = SkillGrant.afterPlunder(player, target, skill, true);
                if (allowed == null || allowed.getRegistryName() == null || SkillUtils.hasSkill(player, allowed)) {
                    continue;
                }
                double mastery = learned.getMastery();
                boolean gained = SkillGrant.grant(player, allowed, mastery, false, null);
                storage.forgetSkill(allowed.getRegistryName(), Component.translatable("mysticism_ultimates.skill.beelzebub.lost", allowed.getName()));
                if (gained) {
                    player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.beelzebub.devoured", allowed.getName()).withStyle(ChatFormatting.DARK_GREEN), false);
                }
                taken++;
            }
        }
    }

    private static boolean canPredate(ManasSkillInstance learned) {
        if (learned == null || learned.isTemporarySkill() || learned.getMastery() < 0.0 || !(learned.getSkill() instanceof Skill skill)) {
            return false;
        }
        if (skill.getType() == Skill.SkillType.ULTIMATE || learned.is(TensuraSkillTags.ULTIMATE_SKILLS) || learned.is(TensuraSkillTags.NO_PLUNDERING)) {
            return false;
        }
        return learned.is(TensuraSkillTags.SKILLS);
    }

    private void devourTerrain(ManasSkillInstance instance, Player player, ServerLevel level, int blockMode, double range) {
        if (blockMode < 2) {
            return;
        }
        int power = Math.max(1, (int) Math.round(range));
        int length = power;
        int diameter = 3 + Math.max(0, (power - 3) / 3);
        double radius = (diameter - 1) / 2.0;
        double radiusSq = radius * radius;
        Vec3 look = player.getLookAngle().normalize();
        Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
        if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1.0, 0.0, 0.0);
        }
        side = side.normalize();
        Vec3 up = side.cross(look).normalize();
        Vec3 eye = player.getEyePosition();
        double start = 1.0;
        for (double step = 1.0; step <= length; step += 1.0) {
            BlockPos pos = BlockPos.containing(eye.add(look.scale(step)));
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() || !state.getFluidState().isEmpty()) {
                start = step;
                break;
            }
        }
        boolean blocks = blockMode == 2 || blockMode == 4;
        boolean fluids = blockMode == 3 || blockMode == 4;
        int reach = (int) Math.ceil(radius);
        Set<BlockPos> carved = new HashSet<>();
        for (int depth = 0; depth < length; depth++) {
            Vec3 center = eye.add(look.scale(start + depth));
            for (int across = -reach; across <= reach; across++) {
                for (int vertical = -reach; vertical <= reach; vertical++) {
                    if (across * across + vertical * vertical > radiusSq) {
                        continue;
                    }
                    BlockPos pos = BlockPos.containing(center.add(side.scale(across)).add(up.scale(vertical)));
                    if (!carved.add(pos)) {
                        continue;
                    }
                    if (fluids) {
                        takeFluid(instance, player, level, pos);
                    }
                    if (blocks) {
                        takeBlock(instance, player, level, pos);
                    }
                }
            }
        }
    }

    private void takeBlock(ManasSkillInstance instance, Player player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA) || state.is(Blocks.BUBBLE_COLUMN)) {
            return;
        }
        if (state.is(TensuraBlockTags.SKILL_UNBREAKABLE) || state.is(TensuraBlockTags.SKILL_UNOBTAINABLE) || state.getDestroySpeed(level, pos) < 0.0f) {
            return;
        }
        if (TensuraSkillEvents.SKILL_GRIEF_PRE.invoker().grief(instance, level, player, pos.getX(), pos.getY(), pos.getZ()).isFalse()) {
            return;
        }
        ItemStack stack = new ItemStack(state.getBlock());
        state.getBlock().playerWillDestroy(level, pos, state, player);
        if (stack.isEmpty()) {
            level.destroyBlock(pos, true, player);
            scoopDrops(instance, player, level, pos);
            return;
        }
        store(instance, player, stack);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private void takeFluid(ManasSkillInstance instance, Player player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getFluidState().isEmpty()) {
            return;
        }
        if (TensuraSkillEvents.SKILL_GRIEF_PRE.invoker().grief(instance, level, player, pos.getX(), pos.getY(), pos.getZ()).isFalse()) {
            return;
        }
        if (state.getFluidState().isSource()) {
            IAbility ability = TensuraStorages.getAbilityFrom(player);
            if (ability != null && state.getFluidState().is(FluidTags.WATER)) {
                ability.setWaterPoint(ability.getWaterPoint() + 1.0);
                ability.markDirty();
            } else if (ability != null && state.getFluidState().is(FluidTags.LAVA)) {
                ability.setLavaPoint(ability.getLavaPoint() + 1.0);
                ability.markDirty();
            }
        }
        if (state.is(Blocks.WATER) || state.is(Blocks.LAVA) || state.is(Blocks.BUBBLE_COLUMN)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        } else if (state.hasProperty(BlockStateProperties.WATERLOGGED) && Boolean.TRUE.equals(state.getValue(BlockStateProperties.WATERLOGGED))) {
            level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 11);
        }
    }

    private void store(ManasSkillInstance instance, Player player, ItemStack stack) {
        if (!addItemToSpatialStorage(instance, player, stack) && !player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private void scoopDrops(ManasSkillInstance instance, Player player, ServerLevel level, BlockPos pos) {
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.5))) {
            if (addItemToSpatialStorage(instance, player, drop.getItem())) {
                drop.discard();
            }
        }
    }

    private void cycleBlockMode(Player player, CompoundTag tag) {
        int next;
        String key;
        switch (tag.getInt("blockMode")) {
            case 1 -> {
                next = 2;
                key = "tensura.skill.predator.block_mode.blocks";
            }
            case 2 -> {
                next = 3;
                key = "tensura.skill.predator.block_mode.fluid";
            }
            case 3 -> {
                next = 4;
                key = "tensura.skill.predator.block_mode.all";
            }
            default -> {
                next = 1;
                key = "tensura.skill.predator.block_mode.none";
            }
        }
        tag.putInt("blockMode", next);
        player.displayClientMessage(Component.translatable(key, getName()).withStyle(ChatFormatting.DARK_AQUA), true);
    }

    private void provide(Player player, ManasSkillInstance instance, int mode) {
        if (!(player instanceof ServerPlayer serverPlayer) || instance.onCoolDown(mode)) {
            return;
        }
        LivingEntity target = SkillGrant.look(player, 12.0);
        if (target == null) {
            tell(player, "mysticism_ultimates.skill.beelzebub.no_target");
            return;
        }
        if (!SubordinateHelper.isSubordinate(player, target)) {
            tell(player, "tensura.targeting.not_allowed");
            return;
        }
        List<ManasSkillInstance> skills = SkillGrant.transferable(player, false, target, null);
        if (skills.isEmpty()) {
            tell(player, "mysticism_ultimates.skill.beelzebub.no_skills");
            return;
        }
        SkillPickMenu.open(serverPlayer, instance, mode, target.getUUID(), SkillGrant.ids(skills));
    }

    private void receive(Player player, ManasSkillInstance instance, int mode) {
        if (!(player instanceof ServerPlayer serverPlayer) || instance.onCoolDown(mode)) {
            return;
        }
        LivingEntity target = SkillGrant.look(player, 12.0);
        if (target == null || target == player) {
            tell(player, "mysticism_ultimates.skill.beelzebub.no_target");
            return;
        }
        if (target.getType().is(TensuraEntityTags.NO_SKILL_PLUNDER)) {
            tell(player, "tensura.targeting.not_allowed");
            return;
        }
        List<ManasSkillInstance> skills = SkillGrant.transferable(target, false, player, null);
        if (skills.isEmpty()) {
            tell(player, "mysticism_ultimates.skill.beelzebub.no_skills");
            return;
        }
        SkillPickMenu.open(serverPlayer, instance, mode, target.getUUID(), SkillGrant.ids(skills));
    }

    public boolean grantPicked(ServerPlayer player, ManasSkillInstance instance, int mode, UUID targetId, ManasSkill chosen) {
        String modeId = getModeId(instance, mode);
        if ("beelzebub.provide".equals(modeId)) {
            return grantProvide(player, instance, mode, targetId, chosen);
        }
        if ("beelzebub.receive".equals(modeId)) {
            return grantReceive(player, instance, mode, targetId, chosen);
        }
        return false;
    }

    private boolean grantProvide(ServerPlayer player, ManasSkillInstance instance, int mode, UUID targetId, ManasSkill chosen) {
        LivingEntity target = SkillGrant.find(player, targetId, 16.0);
        if (target == null || !SubordinateHelper.isSubordinate(player, target)) {
            tell(player, "tensura.targeting.not_allowed");
            return false;
        }
        if (SkillUtils.hasSkill(target, chosen)) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.beelzebub.already_knows", target.getName(), chosen.getName()).withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        ManasSkill skill = SkillGrant.afterPlunder(player, target, chosen, false);
        if (skill == null || !SkillGrant.pay(player, 200.0)) {
            if (skill == null) {
                tell(player, "tensura.ability.activation_failed");
            }
            return false;
        }
        if (!SkillGrant.grant(target, skill, SkillGrant.mastery(player, chosen), false, null)) {
            tell(player, "tensura.ability.activation_failed");
            return false;
        }
        instance.setCoolDown(mode, 2);
        instance.addMasteryPoint(player);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.beelzebub.provided", skill.getName(), target.getName()).withStyle(ChatFormatting.GREEN), false);
        return true;
    }

    private boolean grantReceive(ServerPlayer player, ManasSkillInstance instance, int mode, UUID targetId, ManasSkill chosen) {
        LivingEntity target = SkillGrant.find(player, targetId, 16.0);
        if (target == null || target.getType().is(TensuraEntityTags.NO_SKILL_PLUNDER)) {
            tell(player, "tensura.targeting.not_allowed");
            return false;
        }
        if (SkillUtils.hasSkill(player, chosen)) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.beelzebub.already_knows", player.getName(), chosen.getName()).withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        double mastery = SkillGrant.mastery(target, chosen);
        ManasSkill skill = SkillGrant.afterPlunder(player, target, chosen, true);
        if (skill == null || skill.getRegistryName() == null || !SkillGrant.pay(player, 500.0)) {
            if (skill == null || skill.getRegistryName() == null) {
                tell(player, "tensura.ability.activation_failed");
            }
            return false;
        }
        if (!SkillGrant.grant(player, skill, mastery, false, null)) {
            tell(player, "tensura.ability.activation_failed");
            return false;
        }
        SkillStorage storage = SkillAPI.getSkillsFrom(target);
        if (storage != null) {
            storage.forgetSkill(skill.getRegistryName(), Component.translatable("mysticism_ultimates.skill.beelzebub.lost", skill.getName()));
        }
        instance.setCoolDown(mode, 5);
        instance.addMasteryPoint(player);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.beelzebub.received", skill.getName(), target.getName()).withStyle(ChatFormatting.GREEN), false);
        return true;
    }

    private void isolate(Player player, ManasSkillInstance instance, int mode) {
        if (instance.onCoolDown(mode)) {
            return;
        }
        boolean harmful = hasHarmful(player);
        IEffect effect = TensuraStorages.getEffectFrom(player);
        boolean severed = effect != null && effect.getSeveranceAmount() > 0.0f;
        boolean canDissolve = dissolvingMatch(player) != null;
        if (!harmful && !severed && !canDissolve) {
            tell(player, "mysticism_ultimates.skill.beelzebub.isolation_empty");
            return;
        }
        if (!SkillGrant.pay(player, 100.0)) {
            return;
        }
        TensuraMobEffect.removePredicateEffect(player, holder -> holder.value().getCategory() == MobEffectCategory.HARMFUL);
        if (effect != null) {
            effect.setSeveranceAmount(0.0f);
        }
        boolean dissolved = dissolveHeld(player);
        instance.setCoolDown(mode, instance.isMastered(player) ? 3 : 5);
        instance.addMasteryPoint(player);
        tell(player, dissolved ? "mysticism_ultimates.skill.beelzebub.dissolved" : "mysticism_ultimates.skill.beelzebub.isolated");
    }

    private boolean dissolveHeld(Player player) {
        DissolvingMatch match = dissolvingMatch(player);
        if (match == null) {
            return false;
        }
        match.stack().shrink(1);
        player.heal((float) (match.dissolving().health() * 2.0));
        ExistenceStorage existence = ShadowArmy.existence(player);
        if (existence != null) {
            existence.setSpiritualHealth(existence.getSpiritualHealth() + match.dissolving().spiritualHealth());
        }
        EnergyHelper.gainAura(player, match.dissolving().aura() * 2.0, EnergyHelper.GainType.MAX);
        EnergyHelper.gainMagicule(player, match.dissolving().magicule() * 2.0, EnergyHelper.GainType.MAX);
        return true;
    }

    private DissolvingMatch dissolvingMatch(Player player) {
        var registry = player.level().registryAccess().registry(TensuraCustomData.ITEM_DISSOLVING);
        if (registry.isEmpty()) {
            return null;
        }
        for (ItemStack stack : heldAndInventory(player)) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            for (ItemDissolving dissolving : registry.get()) {
                if (itemId.equals(dissolving.item())) {
                    return new DissolvingMatch(stack, dissolving);
                }
            }
        }
        return null;
    }

    private static List<ItemStack> heldAndInventory(Player player) {
        List<ItemStack> stacks = new java.util.ArrayList<>();
        if (!player.getMainHandItem().isEmpty()) {
            stacks.add(player.getMainHandItem());
        }
        if (!player.getOffhandItem().isEmpty()) {
            stacks.add(player.getOffhandItem());
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack != player.getMainHandItem() && stack != player.getOffhandItem()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static boolean hasHarmful(Player player) {
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                return true;
            }
        }
        return false;
    }

    private record DissolvingMatch(ItemStack stack, ItemDissolving dissolving) {
    }

    private void armSubstitution(Player player, ManasSkillInstance instance) {
        CompoundTag tag = instance.getOrCreateTag();
        boolean armed = !tag.getBoolean("Substitution");
        tag.putBoolean("Substitution", armed);
        instance.markDirty();
        player.displayClientMessage(Component.translatable(armed
                ? "mysticism_ultimates.skill.beelzebub.substitution_on"
                : "mysticism_ultimates.skill.beelzebub.substitution_off").withStyle(armed ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
    }

    private boolean corrode(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (!(entity instanceof Player player) || !(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        if (heldTicks % 10 != 0) {
            return true;
        }
        if (!SkillGrant.pay(player, 200.0)) {
            return false;
        }
        boolean mastered = instance.isMastered(player);
        double radius = mastered ? 8.0 : 5.0;
        float damage = mastered ? 20.0f : 10.0f;
        DamageSource source = TensuraDamageTypes.getEntityDamageSource(level, TensuraDamageTypes.CORROSION, player);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), living -> living != player && living.isAlive() && !SubordinateHelper.isAlly(player, living))) {
            TensuraDamageHelper.hurtSplitElemental(target, source, damage * 0.8f, damage * 0.2f);
            if (TensuraMobEffects.CORROSION != null && TensuraMobEffects.CORROSION.get() != null) {
                target.addEffect(new MobEffectInstance(TensuraMobEffects.CORROSION, 80, mastered ? 2 : 1), player);
            }
            if (!target.getType().is(TensuraEntityTags.NO_EP_PLUNDER)) {
                steal(player, target);
            }
        }
        if (heldTicks > 0 && heldTicks % 20 == 0) {
            instance.addMasteryPoint(player);
        }
        return true;
    }

    private static void steal(Player player, LivingEntity target) {
        ExistenceStorage existence = ShadowArmy.existence(target);
        if (existence == null || existence.getEP() <= 0.0) {
            return;
        }
        double amount = Math.min(2500.0, existence.getEP() * 0.02);
        double aura = Math.min(existence.getAura(), amount / 2.0);
        double magicule = Math.min(existence.getMagicule(), amount - aura);
        if (aura <= 0.0 && magicule <= 0.0) {
            return;
        }
        existence.setAura(existence.getAura() - aura);
        existence.setMagicule(existence.getMagicule() - magicule);
        EnergyHelper.gainAura(player, aura, EnergyHelper.GainType.MAX);
        EnergyHelper.gainMagicule(player, magicule, EnergyHelper.GainType.MAX);
    }

    private static void applyWall(LivingEntity entity, boolean mastered) {
        double armor = mastered ? GuardianSkill.CONFIG.wallArmorMastered : GuardianSkill.CONFIG.wallArmor;
        double knock = mastered ? GuardianSkill.CONFIG.wallKnockResistMastered : GuardianSkill.CONFIG.wallKnockResist;
        if (entity.getAttribute(Attributes.ARMOR) != null) {
            entity.getAttribute(Attributes.ARMOR).addOrUpdateTransientModifier(new AttributeModifier(WALL_ARMOR, armor, AttributeModifier.Operation.ADD_VALUE));
        }
        if (entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addOrUpdateTransientModifier(new AttributeModifier(WALL_KNOCK, knock, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void clearWall(LivingEntity entity) {
        if (entity.getAttribute(Attributes.ARMOR) != null) {
            entity.getAttribute(Attributes.ARMOR).removeModifier(WALL_ARMOR);
        }
        if (entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(WALL_KNOCK);
        }
    }

    private static EventResult redirect(LivingEntity victim, DamageSource source, Changeable<Float> amount) {
        if (redirecting || amount.get() == null || amount.get() <= 0.0f) {
            return EventResult.pass();
        }
        LivingEntity owner = SubordinateHelper.getSubordinateOwner(victim);
        if (!(owner instanceof Player player) || source.getEntity() == player) {
            return EventResult.pass();
        }
        ManasSkill skill = UltimateSkills.BEELZEBUB == null ? null : UltimateSkills.BEELZEBUB.get();
        if (skill == null) {
            return EventResult.pass();
        }
        SkillStorage storage = SkillAPI.getSkillsFrom(player);
        if (storage == null) {
            return EventResult.pass();
        }
        var found = storage.getSkill(skill.getRegistryName());
        if (found.isEmpty() || !found.get().getOrCreateTag().getBoolean("Substitution")) {
            return EventResult.pass();
        }
        float taken = amount.get();
        amount.set(0.0f);
        redirecting = true;
        try {
            player.hurt(source, taken);
        } finally {
            redirecting = false;
        }
        player.displayClientMessage(Component.translatable("tensura.skill.guardian.substitution_notification", taken, victim.getName()).withStyle(ChatFormatting.GOLD), true);
        return EventResult.interruptFalse();
    }

    private static void tell(Player player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
    }

    static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || victim instanceof Player) {
            return;
        }
        if (victim.getType().is(TensuraEntityTags.NO_EP_PLUNDER)) {
            return;
        }
        Player killer = predationKiller(event.getSource());
        if (killer == null) {
            return;
        }
        ManasSkill beelzebub = UltimateSkills.BEELZEBUB.get();
        if (beelzebub == null || !SkillUtils.hasSkill(killer, beelzebub)) {
            return;
        }
        ManasSkillInstance instance = SkillUtils.getSkillOrNull(killer, beelzebub);
        if (instance == null) {
            return;
        }
        ExistenceStorage victimExistence = ShadowArmy.existence(victim);
        if (victimExistence == null || victimExistence.getEP() <= 0.0) {
            return;
        }
        double ep = victimExistence.getEP();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (typeId == null) {
            return;
        }
        String typeKey = typeId.toString();
        repairPredationList(instance);
        CompoundTag tag = instance.getOrCreateTag();
        ListTag eaten = tag.getList("eatenTypes", Tag.TAG_STRING);
        boolean first = true;
        for (int i = 0; i < eaten.size(); i++) {
            if (typeKey.equals(eaten.getString(i))) {
                first = false;
                break;
            }
        }
        double gain = first ? ep : ep * 0.05;
        EnergyHelper.gainMagicule(killer, gain, EnergyHelper.GainType.MAX);
        if (first) {
            eaten.add(StringTag.valueOf(typeKey));
            tag.put("eatenTypes", eaten);
            instance.markDirty();
        }
    }

    public static void repairPredationList(ManasSkillInstance instance) {
        CompoundTag tag = instance.getOrCreateTag();
        if (!(tag.get("predationList") instanceof ListTag legacy)) {
            return;
        }
        tag.put("eatenTypes", legacy);
        tag.remove("predationList");
        instance.markDirty();
    }

    private static Player predationKiller(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof PredatorMistProjectile mist && mist.getOwner() instanceof Player player) {
            return player;
        }
        Entity entity = source.getEntity();
        if (entity instanceof Player player && source.is(TensuraDamageTypes.CORROSION)) {
            return player;
        }
        return null;
    }

    @Override
    public void onForgetSkill(ManasSkillInstance instance, LivingEntity entity) {
        clearWall(entity);
        if (!entity.level().isClientSide() && !instance.isTemporarySkill() && entity instanceof Player player) {
            dropAllItems(instance, player);
        }
        super.onForgetSkill(instance, entity);
    }
}
