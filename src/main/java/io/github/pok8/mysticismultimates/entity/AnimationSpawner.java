package io.github.pok8.mysticismultimates.entity;

import io.github.pok8.mysticismultimates.registry.UltimateEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class AnimationSpawner {
    private static final Map<String, SkillAnimationVariant> BY_MODE = Map.of(
            "antaeus.attract_blue", SkillAnimationVariant.ANTAEUS_BLUE,
            "antaeus.repel_red", SkillAnimationVariant.ANTAEUS_RED,
            "antaeus.hollow_purple", SkillAnimationVariant.ANTAEUS_PURPLE,
            "gilgamesh.territory", SkillAnimationVariant.GATES,
            "gilgamesh.ea", SkillAnimationVariant.EA);

    private AnimationSpawner() {
    }

    public static boolean spawn(Player player, String modeId) {
        SkillAnimationVariant variant = BY_MODE.get(modeId);
        if (variant == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        SkillAnimationEntity entity = new SkillAnimationEntity(UltimateEntities.type(variant), level);
        Vec3 look = player.getLookAngle();
        entity.moveTo(
                player.getX() + look.x * 2.0,
                player.getEyeY() - 0.4,
                player.getZ() + look.z * 2.0,
                player.getYRot(),
                player.getXRot());
        if (variant.projectile()) {
            entity.setDeltaMovement(look.scale(1.15));
        }
        return level.addFreshEntity(entity);
    }
}
