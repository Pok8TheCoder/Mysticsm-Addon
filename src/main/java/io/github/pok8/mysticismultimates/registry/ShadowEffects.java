package io.github.pok8.mysticismultimates.registry;

import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.effect.LunarBoostEffect;
import io.github.pok8.mysticismultimates.effect.ShadowGraspEffect;
import io.github.pok8.mysticismultimates.effect.SolarBoostEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ShadowEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MysticismUltimates.MOD_ID);
    public static final DeferredHolder<MobEffect, LunarBoostEffect> LUNAR_BOOST = EFFECTS.register("lunar_boost", LunarBoostEffect::new);
    public static final DeferredHolder<MobEffect, SolarBoostEffect> SOLAR_BOOST = EFFECTS.register("solar_boost", SolarBoostEffect::new);
    public static final DeferredHolder<MobEffect, ShadowGraspEffect> SHADOW_GRASP = EFFECTS.register("shadow_grasp", ShadowGraspEffect::new);

    private ShadowEffects() {
    }
}
