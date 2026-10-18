package io.github.pok8.mysticismultimates;

import io.github.manasmods.manascore.storage.api.Storage;
import io.github.manasmods.manascore.storage.api.StorageEvents;
import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.manascore.storage.api.StorageKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class UltimateSlotStorage extends Storage {
    private static StorageKey<UltimateSlotStorage> key;
    private final StorageHolder holder;
    private int soulQuality;
    private int currentUltimates;
    private boolean soulRolled;

    public UltimateSlotStorage(StorageHolder holder) {
        super(holder);
        this.holder = holder;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void init() {
        StorageEvents.RegisterStorage listener = registry -> key = registry.register(
                ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "ultimate_slots"),
                UltimateSlotStorage.class,
                Player.class::isInstance,
                UltimateSlotStorage::new);
        StorageEvents.REGISTER_ENTITY_STORAGE.register(listener);
    }

    public static UltimateSlotStorage get(LivingEntity entity) {
        if (key == null) {
            return null;
        }
        StorageHolder holder = (StorageHolder) (Object) entity;
        return holder.manasCore$getStorage(key);
    }

    public int soulQuality() {
        return soulQuality;
    }

    public int currentUltimates() {
        return currentUltimates;
    }

    public boolean soulRolled() {
        return soulRolled;
    }

    public void setSoulQuality(int value) {
        soulQuality = Math.max(0, value);
        soulRolled = true;
        sync();
    }

    public void increment() {
        currentUltimates++;
        sync();
    }

    public void decrement() {
        currentUltimates = Math.max(0, currentUltimates - 1);
        sync();
    }

    private void sync() {
        markDirty();
        if ((Object) holder instanceof ServerPlayer player) {
            ((StorageHolder) (Object) player).manasCore$sync();
        }
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putInt("soulQuality", soulQuality);
        tag.putInt("currentUltimates", currentUltimates);
        tag.putBoolean("soulRolled", soulRolled);
    }

    @Override
    public void load(CompoundTag tag) {
        soulQuality = tag.getInt("soulQuality");
        currentUltimates = tag.getInt("currentUltimates");
        soulRolled = tag.contains("soulRolled") ? tag.getBoolean("soulRolled") : soulQuality > 0;
    }
}
