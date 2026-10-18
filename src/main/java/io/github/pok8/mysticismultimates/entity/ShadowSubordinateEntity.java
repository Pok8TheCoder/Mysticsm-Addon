package io.github.pok8.mysticismultimates.entity;

import io.github.manasmods.tensura.entity.template.TensuraTamableEntity;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.pathfinder.PathType;
import io.github.manasmods.tensura.entity.ai.controller.FlightMoveController;
import io.github.manasmods.tensura.entity.template.subclass.ISpiritual;
import io.github.manasmods.tensura.item.misc.BoneGolemItem;
import io.github.manasmods.tensura.entity.human.golem.BoneGolemEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ShadowSubordinateEntity extends TensuraTamableEntity implements GeoEntity {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    public static final EntityDataAccessor<CompoundTag> FORM = SynchedEntityData.defineId(ShadowSubordinateEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private CompoundTag mobData = new CompoundTag();
    private String sourceName = "Shadow Subordinate";
    private double sourceEp;
    private double sourceAttack = 4.0;
    private boolean shadowFlight;
    private final SimpleContainer inventory = new SimpleContainer(27);

    public ShadowSubordinateEntity(EntityType<? extends ShadowSubordinateEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TensuraTamableEntity.setAttributes()
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(TensuraAttributes.MAX_AURA, 50.0)
                .add(TensuraAttributes.MAX_MAGICULE, 50.0)
                .add(TensuraAttributes.MAX_SPIRITUAL_HEALTH, 20.0);
    }

    public void store(String name, double ep, float health, double attack, CompoundTag mobData) {
        this.sourceName = name.isEmpty() ? "Shadow Subordinate" : name;
        this.sourceEp = ep;
        this.sourceAttack = attack;
        this.mobData = mobData.copy();
        entityData.set(FORM, this.mobData);
        setCustomName(net.minecraft.network.chat.Component.literal(this.sourceName));
        setCustomNameVisible(true);
        if (getAttribute(Attributes.MAX_HEALTH) != null) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(4.0, health));
        }
        if (getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(1.0, attack));
        }
        setHealth(getMaxHealth());
        applyCapturedStats();
        refreshDimensions();
    }

    public void applyCapturedStats() {
        CompoundTag attributes = mobData.getCompound("MysticismAttributes");
        for (String key : attributes.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) {
                continue;
            }
            var attribute = BuiltInRegistries.ATTRIBUTE.getHolder(id).orElse(null);
            if (attribute != null && getAttributes().hasAttribute(attribute)) {
                getAttribute(attribute).setBaseValue(attributes.getDouble(key));
            }
        }
        if (getAttribute(Attributes.FOLLOW_RANGE) != null) {
            getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32.0);
        }
        if (getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(1.0, sourceAttack));
        }
        if (sourceEp > 0.0) {
            if (getAttribute(TensuraAttributes.MAX_AURA) != null) {
                getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(sourceEp / 2.0);
            }
            if (getAttribute(TensuraAttributes.MAX_MAGICULE) != null) {
                getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(sourceEp / 2.0);
            }
            EnergyHelper.setBaseMaxEP(this, sourceEp);
        }
        double shp = Math.max(20.0, mobData.getDouble("MysticismSpiritual"));
        if (getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH) != null) {
            getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(shp);
        }
        if (getAttribute(Attributes.MAX_HEALTH) != null) {
            setHealth(getMaxHealth());
        }
        var existence = io.github.pok8.mysticismultimates.skill.ShadowArmy.existence(this);
        if (existence != null) {
            existence.setSpiritualHealth(shp);
            if (sourceEp > 0.0) {
                existence.setAura(sourceEp / 2.0);
                existence.setMagicule(sourceEp / 2.0);
                existence.setEP(sourceEp);
            }
            existence.setSkippingEPDrop(true);
            existence.setNameable(true);
            existence.markDirty();
        }
        learnCapturedSkills();
        applyFlight();
    }

    private void applyFlight() {
        shadowFlight = mobData.getBoolean("MysticismCanFly");
        if (!shadowFlight) {
            return;
        }
        setNoGravity(true);
        moveControl = new FlightMoveController(this, true, false);
        navigation = new FlyingPathNavigation(this, level());
        navigation.setCanFloat(true);
        setPathfindingMalus(PathType.FENCE, -1.0F);
    }

    public void learnCapturedSkills() {
        var storage = io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(this);
        var registry = io.github.manasmods.manascore.skill.api.SkillAPI.getSkillRegistry();
        for (var skillTag : mobData.getList("MysticismSkills", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            if (!(skillTag instanceof CompoundTag saved)) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(saved.getString("id"));
            if (id == null) {
                continue;
            }
            var skill = registry.get(id);
            if (skill == null || !storage.learnSkill(skill)) {
                continue;
            }
            storage.getSkill(skill).ifPresent(instance -> instance.setMastery(saved.getDouble("mastery")));
        }
    }

    public CompoundTag shadowForm() {
        return entityData.get(FORM);
    }

    public String sourceName() {
        return sourceName;
    }

    public double sourceEp() {
        return sourceEp;
    }

    public double sourceAttack() {
        return sourceAttack;
    }

    public CompoundTag mobData() {
        return mobData.copy();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, state -> {
            if (attackAnim > 0.0F) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("punch2"));
            }
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle "));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && getOwnerUUID() == null && tickCount <= 2) {
            Player nearest = level().getNearestPlayer(this, 8.0);
            if (nearest != null) {
                tame(nearest);
            }
        }
        LivingEntity owner = getOwner();
        if (!level().isClientSide() && owner != null) {
            if (isOrderedToSit() || isWandering()) {
                if (isOrderedToSit()) {
                    getNavigation().stop();
                }
            } else if (shouldFollowOwner()) {
                getNavigation().moveTo(owner, 1.15);
            }
        }
        if (shadowFlight) {
            setNoGravity(true);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return !shadowFlight && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        float damage = (float) sourceAttack;
        if (getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        }
        return target.hurt(damageSources().mobAttack(this), damage);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        CompoundTag form = entityData.get(FORM);
        CompoundTag body = form.contains("MysticismBody") ? form.getCompound("MysticismBody") : form;
        EntityDimensions sized = ShadowForms.dimensions(body);
        return sized == null ? super.getDefaultDimensions(pose) : sized;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide() || !isOwnedBy(player)) {
            return level().isClientSide() && isOwnedBy(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof BoneGolemItem golem && needsBody()) {
            return grantBody(player, hand, golem);
        }
        if (player.isShiftKeyDown()) {
            return handleCommanding(player, hand, stack);
        }
        if (player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> ChestMenu.threeRows(containerId, inventory, this.inventory),
                    getDisplayName()));
        }
        return InteractionResult.CONSUME;
    }

    public boolean needsBody() {
        if (mobData.contains("MysticismBody")) {
            return false;
        }
        if (mobData.getBoolean("MysticismNeedsBody")) {
            return true;
        }
        ResourceLocation id = ResourceLocation.tryParse(mobData.getString("id"));
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return false;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        Entity created = type == null ? null : type.create(level());
        boolean spiritual = created instanceof ISpiritual;
        if (created != null) {
            created.discard();
        }
        mobData.putBoolean("MysticismNeedsBody", spiritual);
        return spiritual;
    }

    private InteractionResult grantBody(Player player, InteractionHand hand, BoneGolemItem golem) {
        var existence = io.github.pok8.mysticismultimates.skill.ShadowArmy.existence(this);
        double ep = existence == null ? sourceEp : existence.getEP();
        if (golem.getVariant().getEP() > ep) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "tensura.message.bone_golem.low_ep", getDisplayName(), player.getItemInHand(hand).getDisplayName())
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        EntityType<?> type = EntityType.byString("tensura:bone_golem").orElse(null);
        if (type == null || !(type.create(level()) instanceof BoneGolemEntity visual)) {
            return InteractionResult.PASS;
        }
        visual.setVariant(golem.getVariant());
        CompoundTag saved = new CompoundTag();
        visual.saveWithoutId(saved);
        saved.putString("id", "tensura:bone_golem");
        visual.discard();
        mobData.put("MysticismBody", saved);
        mobData.putBoolean("MysticismNeedsBody", false);
        entityData.set(FORM, mobData.copy());
        if (existence != null) {
            existence.setSpiritualForm(false);
            existence.markDirty();
        }
        player.getItemInHand(hand).consume(1, player);
        refreshDimensions();
        level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source, boolean recentlyHit) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                spawnAtLocation(stack);
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FORM, new CompoundTag());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FORM.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("ShadowSourceName", sourceName);
        tag.putDouble("ShadowSourceEP", sourceEp);
        tag.putDouble("ShadowSourceAttack", sourceAttack);
        tag.putFloat("ShadowSourceHealth", getMaxHealth());
        tag.put("ShadowMobData", mobData);
        net.minecraft.nbt.ListTag items = new net.minecraft.nbt.ListTag();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            entry.put("Item", stack.save(registryAccess()));
            items.add(entry);
        }
        tag.put("ShadowInventory", items);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        sourceName = tag.getString("ShadowSourceName");
        sourceEp = tag.getDouble("ShadowSourceEP");
        sourceAttack = tag.getDouble("ShadowSourceAttack");
        mobData = tag.getCompound("ShadowMobData");
        entityData.set(FORM, mobData.copy());
        inventory.clearContent();
        for (var saved : tag.getList("ShadowInventory", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            if (!(saved instanceof CompoundTag entry)) {
                continue;
            }
            int slot = entry.getByte("Slot") & 255;
            ItemStack.parse(registryAccess(), entry.getCompound("Item")).ifPresent(stack -> {
                if (slot < inventory.getContainerSize()) {
                    inventory.setItem(slot, stack);
                }
            });
        }
        if (tag.contains("ShadowSourceHealth") && getAttribute(Attributes.MAX_HEALTH) != null) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(4.0, tag.getFloat("ShadowSourceHealth")));
            setHealth(getMaxHealth());
        }
        applyCapturedStats();
        refreshDimensions();
        applyFlight();
        if (hasCustomName() && getCustomName() != null) {
            sourceName = getCustomName().getString();
        } else if (!sourceName.isEmpty()) {
            setCustomName(net.minecraft.network.chat.Component.literal(sourceName));
        }
    }

    @Override
    public void setCustomName(net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        if (name != null && !name.getString().isEmpty()) {
            sourceName = name.getString();
        }
    }
}
