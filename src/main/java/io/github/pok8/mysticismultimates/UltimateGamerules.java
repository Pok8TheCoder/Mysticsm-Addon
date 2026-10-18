package io.github.pok8.mysticismultimates;

import net.minecraft.world.level.GameRules;

import java.lang.reflect.Method;

public final class UltimateGamerules {
    public static final GameRules.Key<GameRules.BooleanValue> SOUL_QUALITY_MODE = rule("mysticismUltimatesSoulQuality", true);
    public static final GameRules.Key<GameRules.BooleanValue> ENABLE_ACQUISITION = rule("mysticismUltimatesAcquisition", true);
    public static final GameRules.Key<GameRules.BooleanValue> LOSE_UNIQUE = rule("mysticismUltimatesLoseUnique", false);
    public static final GameRules.Key<GameRules.BooleanValue> PLUNDERER_DARKNESS_SPIRIT = rule("mysticismUltimatesPlundererDarknessSpirit", true);

    private UltimateGamerules() {
    }

    public static void bootstrap() {
        // Loading this class registers the three gamerules.
    }

    @SuppressWarnings("unchecked")
    private static GameRules.Key<GameRules.BooleanValue> rule(String name, boolean defaultValue) {
        try {
            Method create = GameRules.BooleanValue.class.getDeclaredMethod("create", boolean.class);
            create.setAccessible(true);
            Object type = create.invoke(null, defaultValue);
            Method register = GameRules.class.getDeclaredMethod("register", String.class, GameRules.Category.class, GameRules.Type.class);
            register.setAccessible(true);
            return (GameRules.Key<GameRules.BooleanValue>) register.invoke(null, name, GameRules.Category.PLAYER, type);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not register a Mysticism Ultimates gamerule", e);
        }
    }
}
