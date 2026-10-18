package io.github.pok8.mysticismultimates.entity;

import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.pok8.mysticismultimates.registry.UltimateEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SkillAnimationEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private java.util.UUID ownerId;
    private float damage;
    private double pull;
    private double radius;
    private int lifeOverride = -1;

    public SkillAnimationEntity(EntityType<? extends SkillAnimationEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public SkillAnimationVariant variant() {
        return UltimateEntities.variant(getType());
    }

    public void arm(java.util.UUID owner, float damage, double pull, double radius, int lifetime) {
        this.ownerId = owner;
        this.damage = damage;
        this.pull = pull;
        this.radius = radius;
        this.lifeOverride = lifetime;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop(variant().animation()))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void tick() {
        super.tick();
        if (variant().projectile()) {
            setPos(position().add(getDeltaMovement()));
        }
        if (!level().isClientSide() && damage > 0.0f && tickCount % 10 == 1) {
            strike();
        }
        int life = lifeOverride > 0 ? lifeOverride : variant().lifetime();
        if (tickCount > life) {
            discard();
        }
    }

    private void strike() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Entity owner = ownerId == null ? null : level.getEntity(ownerId);
        AABB box = getBoundingBox().inflate(radius);
        var source = TensuraDamageTypes.getIndirectEntityDamageSource(level, TensuraDamageTypes.GRAVITY_ELEMENTAL, this, owner);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, living -> living.isAlive() && living != owner)) {
            if (owner instanceof LivingEntity livingOwner && livingOwner.isAlliedTo(target)) {
                continue;
            }
            TensuraDamageHelper.hurtSplitElemental(target, source, damage * 0.9f, damage / 10.0f);
            Vec3 between = position().subtract(target.position());
            if (between.lengthSqr() > 1.0E-4 && pull != 0.0) {
                target.setDeltaMovement(target.getDeltaMovement().add(between.normalize().scale(pull)));
                target.hasImpulse = true;
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
