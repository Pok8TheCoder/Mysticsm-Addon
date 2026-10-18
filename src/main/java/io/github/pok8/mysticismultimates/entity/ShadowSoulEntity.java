package io.github.pok8.mysticismultimates.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ShadowSoulEntity extends Mob {
    public static final int LIFE = 30 * 20;
    public static final EntityDataAccessor<CompoundTag> FORM = SynchedEntityData.defineId(ShadowSoulEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private CompoundTag mobData = new CompoundTag();
    private UUID owner = new UUID(0L, 0L);
    private String sourceName = "";
    private double sourceEp;
    private float sourceHealth = 20.0F;
    private double sourceAttack = 2.0;
    private int age;

    public ShadowSoulEntity(EntityType<? extends ShadowSoulEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
        setCustomNameVisible(true);
        noPhysics = true;
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public void store(UUID owner, String name, double ep, float health, double attack, CompoundTag mobData) {
        this.owner = owner;
        this.sourceName = name;
        this.sourceEp = ep;
        this.sourceHealth = health;
        this.sourceAttack = attack;
        this.mobData = mobData.copy();
        entityData.set(FORM, this.mobData);
        setCustomName(net.minecraft.network.chat.Component.literal(name.isEmpty() ? "Shadow Soul" : name));
        refreshDimensions();
    }

    public CompoundTag shadowForm() {
        return entityData.get(FORM);
    }

    public UUID owner() {
        return owner;
    }

    public String sourceName() {
        return sourceName;
    }

    public double sourceEp() {
        return sourceEp;
    }

    public float sourceHealth() {
        return sourceHealth;
    }

    public double sourceAttack() {
        return sourceAttack;
    }

    public CompoundTag mobData() {
        return mobData.copy();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && ++age > LIFE) {
            discard();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions sized = ShadowForms.dimensions(entityData.get(FORM));
        return sized == null ? super.getDefaultDimensions(pose) : sized;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
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
        tag.putLong("ShadowOwnerMost", owner.getMostSignificantBits());
        tag.putLong("ShadowOwnerLeast", owner.getLeastSignificantBits());
        tag.putString("ShadowSourceName", sourceName);
        tag.putDouble("ShadowSourceEP", sourceEp);
        tag.putFloat("ShadowSourceHealth", sourceHealth);
        tag.putDouble("ShadowSourceAttack", sourceAttack);
        tag.put("ShadowMobData", mobData);
        tag.putInt("ShadowAge", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = new UUID(tag.getLong("ShadowOwnerMost"), tag.getLong("ShadowOwnerLeast"));
        sourceName = tag.getString("ShadowSourceName");
        sourceEp = tag.getDouble("ShadowSourceEP");
        sourceHealth = tag.getFloat("ShadowSourceHealth");
        sourceAttack = tag.getDouble("ShadowSourceAttack");
        mobData = tag.getCompound("ShadowMobData");
        entityData.set(FORM, mobData.copy());
        age = tag.getInt("ShadowAge");
        refreshDimensions();
        if (!sourceName.isEmpty()) {
            setCustomName(net.minecraft.network.chat.Component.literal(sourceName));
        }
    }

    public static MobCategory category() {
        return MobCategory.MISC;
    }
}
