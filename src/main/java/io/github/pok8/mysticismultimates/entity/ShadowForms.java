package io.github.pok8.mysticismultimates.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class ShadowForms {
    private ShadowForms() {
    }

    public static CompoundTag capture(LivingEntity victim) {
        CompoundTag tag = new CompoundTag();
        victim.saveWithoutId(tag);
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (key != null) {
            tag.putString("id", key.toString());
        }
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("UUID");
        tag.remove("Passengers");
        tag.remove("Brain");
        tag.remove("Leash");
        tag.remove("ActiveEffects");
        return tag;
    }

    public static EntityDimensions dimensions(CompoundTag form) {
        if (form == null || !form.contains("id")) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(form.getString("id"));
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        return type == null ? null : type.getDimensions();
    }
}
