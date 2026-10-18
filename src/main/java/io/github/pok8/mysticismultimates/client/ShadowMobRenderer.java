package io.github.pok8.mysticismultimates.client;

import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.entity.ShadowSoulEntity;
import io.github.pok8.mysticismultimates.entity.ShadowSubordinateEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class ShadowMobRenderer<T extends Mob> extends EntityRenderer<T> {
    private static final int GHOST_ALPHA = 64;
    private final Map<Integer, Cached> visuals = new HashMap<>();

    public ShadowMobRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        Entity visual = visual(entity);
        if (visual != null) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(visual);
            return renderer.getTextureLocation(visual);
        }
        return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "textures/entity/shadow_imp.png");
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity visual = visual(entity);
        if (visual != null) {
            copyPose(entity, visual);
            if (entity instanceof ShadowSoulEntity) {
                renderDirect(visual, yaw, partialTick, poseStack, new GhostBuffers(buffer, GHOST_ALPHA), packedLight);
            } else {
                renderLikeWorld(entity, visual, yaw, partialTick, poseStack, buffer, packedLight);
            }
        }
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderLikeWorld(T entity, Entity visual, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double x = Mth.lerp(partialTick, entity.xo, entity.getX()) - camera.x;
        double y = Mth.lerp(partialTick, entity.yo, entity.getY()) - camera.y;
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ()) - camera.z;
        poseStack.pushPose();
        poseStack.translate(-x, -y, -z);
        Minecraft.getInstance().getEntityRenderDispatcher().render(visual, x, y, z, yaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private void renderDirect(Entity visual, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        @SuppressWarnings({"rawtypes", "unchecked"})
        EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(visual);
        renderer.render(visual, yaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private void copyPose(T entity, Entity visual) {
        visual.setPos(entity.getX(), entity.getY(), entity.getZ());
        visual.xo = entity.xo;
        visual.yo = entity.yo;
        visual.zo = entity.zo;
        visual.setYRot(entity.getYRot());
        visual.yRotO = entity.yRotO;
        visual.setXRot(entity.getXRot());
        visual.xRotO = entity.xRotO;
        visual.setYHeadRot(entity.getYHeadRot());
        visual.tickCount = entity.tickCount;
        if (visual instanceof LivingEntity living && entity instanceof LivingEntity host) {
            living.yHeadRotO = host.yHeadRotO;
            living.setYBodyRot(host.yBodyRot);
            living.yBodyRotO = host.yBodyRotO;
            living.attackAnim = host.attackAnim;
            living.oAttackAnim = host.oAttackAnim;
            living.hurtTime = host.hurtTime;
            living.walkAnimation.setSpeed(host.walkAnimation.speed());
            Cached cached = visuals.get(entity.getId());
            if (cached != null && cached.tick != entity.tickCount) {
                living.walkAnimation.update(host.walkAnimation.speed(), 1.0F);
                cached.tick = entity.tickCount;
            }
        }
    }

    private Entity visual(T entity) {
        CompoundTag form = form(entity);
        String id = form.getString("id");
        if (id.isEmpty()) {
            return null;
        }
        Cached cached = visuals.get(entity.getId());
        if (cached != null && cached.id.equals(id)) {
            return cached.entity;
        }
        if (visuals.size() > 64) {
            visuals.clear();
        }
        Entity created = null;
        try {
            created = EntityType.create(form.copy(), entity.level()).orElse(null);
        } catch (RuntimeException ignored) {
            created = null;
        }
        if (created instanceof ShadowSoulEntity || created instanceof ShadowSubordinateEntity) {
            created = null;
        }
        if (created instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setCustomNameVisible(false);
            mob.noPhysics = true;
        }
        visuals.put(entity.getId(), new Cached(id, created, -1));
        return created;
    }

    private static CompoundTag form(Mob entity) {
        if (entity instanceof ShadowSoulEntity soul) {
            return soul.shadowForm();
        }
        if (entity instanceof ShadowSubordinateEntity soldier) {
            CompoundTag form = soldier.shadowForm();
            return form.contains("MysticismBody") ? form.getCompound("MysticismBody") : form;
        }
        return new CompoundTag();
    }

    private static final class Cached {
        private final String id;
        private final Entity entity;
        private int tick;

        private Cached(String id, Entity entity, int tick) {
            this.id = id;
            this.entity = entity;
            this.tick = tick;
        }

        private static Cached failed(String id) {
            return new Cached(id, null, -1);
        }
    }
}
