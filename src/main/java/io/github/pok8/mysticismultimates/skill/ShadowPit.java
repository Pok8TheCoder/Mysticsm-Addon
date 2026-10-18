package io.github.pok8.mysticismultimates.skill;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.subclass.ISpatialStorage;
import io.github.manasmods.tensura.menu.SpatialStorageMenu;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.item.TensuraMaterialItems;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.pok8.mysticismultimates.entity.ShadowSubordinateEntity;
import io.github.pok8.mysticismultimates.registry.UltimateSkills;
import io.github.pok8.mysticismultimates.registry.UniqueShadowSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.lang.reflect.Field;
import java.util.UUID;

public final class ShadowPit {
    public static final int VAINGLORY_SLOTS = 27 * 3;
    public static final int LAVERNA_SLOTS = 27 * 9;
    public static final double SUMMON_COST = 500.0;
    private static final String ARMY = "ArmyOut";
    private static Field menuContainer;
    private static Field menuSkill;

    private ShadowPit() {
    }

    public static SpatialStorageContainer load(ManasSkillInstance instance, HolderLookup.Provider provider, int slots) {
        ShadowPitContainer container = new ShadowPitContainer(slots);
        container.fromTag(instance.getOrCreateTag().getList("SpatialStorage", Tag.TAG_COMPOUND), provider);
        return container;
    }

    public static boolean unlocked(ServerPlayer player, ManasSkillInstance instance) {
        ManasSkill skill = instance.getSkill();
        if (skill == null || skill.getRegistryName() == null) {
            return false;
        }
        String path = skill.getRegistryName().getPath();
        if ("laverna".equals(path)) {
            return true;
        }
        return "vainglory".equals(path) && instance.isMastered(player);
    }

    public static void open(ServerPlayer player, ManasSkillInstance instance) {
        if (!unlocked(player, instance) || !(instance.getSkill() instanceof ISpatialStorage storage)) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.shadow_pit.locked").withStyle(ChatFormatting.RED), false);
            return;
        }
        storage.openSpatialStorage(player, instance);
    }

    public static void police(ServerPlayer player, ManasSkillInstance instance) {
        if (!unlocked(player, instance) || !(instance.getSkill() instanceof ISpatialStorage storage)) {
            return;
        }
        SimpleContainer live = openContainer(player, instance.getSkill());
        if (live != null) {
            eject(player, live);
            return;
        }
        SpatialStorageContainer container = storage.getSpatialStorage(instance, player.registryAccess());
        if (eject(player, container)) {
            storage.saveContainer(instance, player, container);
        }
    }

    public static boolean keepCapture(ServerPlayer player, LivingEntity victim) {
        ManasSkillInstance pit = destination(player, null);
        if (pit == null) {
            return false;
        }
        ItemStack stack = ShadowArmy.captureItem(player, victim);
        if (stack.isEmpty()) {
            return false;
        }
        if (!store(player, pit, stack)) {
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.shadow_pit.full").withStyle(ChatFormatting.RED), false);
        }
        return true;
    }

    public static boolean offer(ServerPlayer player, ManasSkillInstance acting, ItemStack stack) {
        ManasSkillInstance pit = destination(player, acting);
        if (pit == null) {
            return false;
        }
        if (store(player, pit, stack)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.shadow_pit.full").withStyle(ChatFormatting.RED), false);
        return false;
    }

    public static void toggleArmy(ServerPlayer player, ManasSkillInstance instance) {
        if (!unlocked(player, instance)) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.shadow_pit.locked").withStyle(ChatFormatting.RED), false);
            return;
        }
        if (recall(player, instance)) {
            return;
        }
        summon(player, instance);
    }

    public static void clearArmy(ServerPlayer player) {
        clearArmy(ShadowArmy.skill(player, UniqueShadowSkills.VAINGLORY.get()));
        clearArmy(UltimateSkills.LAVERNA == null ? null : ShadowArmy.skill(player, UltimateSkills.LAVERNA.get()));
    }

    public static boolean isShadowCopy(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(TensuraMaterialItems.SHADOW_STORAGE.get())) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean("MysticismShadowSubordinate") || tag.getBoolean("MysticismGenericSubordinate");
    }

    private static void summon(ServerPlayer player, ManasSkillInstance instance) {
        if (!(instance.getSkill() instanceof ISpatialStorage storage)) {
            return;
        }
        ExistenceStorage existence = ShadowArmy.existence(player);
        SpatialStorageContainer container = storage.getSpatialStorage(instance, player.registryAccess());
        int summoned = 0;
        int heldBack = 0;
        ListTag army = new ListTag();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!isShadowCopy(stack)) {
                continue;
            }
            if (existence == null || existence.getMagicule() < SUMMON_COST) {
                heldBack++;
                continue;
            }
            existence.setMagicule(existence.getMagicule() - SUMMON_COST);
            LivingEntity released = release(player, stack, summoned);
            if (released == null) {
                existence.setMagicule(existence.getMagicule() + SUMMON_COST);
                heldBack++;
                continue;
            }
            container.setItem(slot, ItemStack.EMPTY);
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", released.getUUID());
            army.add(entry);
            summoned++;
        }
        storage.saveContainer(instance, player, container);
        instance.getOrCreateTag().put(ARMY, army);
        instance.markDirty();
        if (existence != null) {
            existence.markDirty();
        }
        if (summoned == 0 && heldBack == 0) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.shadow_pit.empty").withStyle(ChatFormatting.RED), false);
            return;
        }
        player.displayClientMessage(Component.translatable(
                heldBack > 0 ? "mysticism_ultimates.skill.shadow_pit.summon_partial" : "mysticism_ultimates.skill.shadow_pit.summoned",
                summoned, (long) (summoned * SUMMON_COST), heldBack).withStyle(ChatFormatting.DARK_PURPLE), false);
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean recall(ServerPlayer player, ManasSkillInstance instance) {
        ListTag army = instance.getOrCreateTag().getList(ARMY, Tag.TAG_COMPOUND);
        if (army.isEmpty()) {
            return false;
        }
        int recalled = 0;
        for (int index = 0; index < army.size(); index++) {
            Entity entity = find(player, army.getCompound(index).getUUID("id"));
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            ItemStack stack = living instanceof ShadowSubordinateEntity soldier
                    ? ShadowArmy.storageItem(soldier)
                    : ShadowArmy.genericStorage(player, living);
            if (!store(player, instance, stack) && !player.addItem(stack)) {
                player.drop(stack, false);
            }
            living.discard();
            recalled++;
        }
        instance.getOrCreateTag().remove(ARMY);
        instance.markDirty();
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.shadow_pit.recalled", recalled).withStyle(ChatFormatting.DARK_PURPLE), false);
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static LivingEntity release(ServerPlayer player, ItemStack stack, int index) {
        CompoundTag custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CustomData bucket = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        if (bucket == null) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(custom.getString("EntityType"));
        EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            return null;
        }
        Entity entity = type.create(player.serverLevel());
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        living.load(bucket.copyTag());
        double angle = index * 0.7;
        living.moveTo(player.getX() + Math.cos(angle) * 2.0, player.getY(), player.getZ() + Math.sin(angle) * 2.0, player.getYRot(), 0.0F);
        if (living instanceof TamableAnimal animal && animal.getOwnerUUID() == null) {
            animal.tame(player);
        }
        if (living instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        double ep = custom.getDouble("ShadowEP");
        double shp = custom.getDouble("ShadowSHP");
        if (living instanceof ShadowSubordinateEntity soldier) {
            if (ep > 0.0) {
                soldier.store(custom.getString("ShadowName"), ep, (float) custom.getDouble("ShadowHP"), soldier.sourceAttack(), custom.getCompound("ShadowData"));
            } else {
                soldier.applyCapturedStats();
            }
        } else if (ep > 0.0) {
            restoreGenericStats(living, ep, shp);
        }
        player.serverLevel().addFreshEntity(living);
        return living;
    }

    public static void restoreGenericStats(LivingEntity living, double ep, double shp) {
        if (living == null || ep <= 0.0) {
            return;
        }
        if (living.getAttribute(TensuraAttributes.MAX_AURA) != null) {
            living.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(ep / 2.0);
        }
        if (living.getAttribute(TensuraAttributes.MAX_MAGICULE) != null) {
            living.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(ep / 2.0);
        }
        EnergyHelper.setBaseMaxEP(living, ep);
        if (shp > 0.0 && living.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH) != null) {
            living.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(shp);
        }
        ExistenceStorage existence = ShadowArmy.existence(living);
        if (existence != null) {
            existence.setAura(ep / 2.0);
            existence.setMagicule(ep / 2.0);
            existence.setEP(ep);
            if (shp > 0.0) {
                existence.setSpiritualHealth(shp);
            }
            existence.setSkippingEPDrop(true);
            existence.markDirty();
        }
    }

    private static boolean store(ServerPlayer player, ManasSkillInstance pit, ItemStack stack) {
        if (!isShadowCopy(stack) || !(pit.getSkill() instanceof ISpatialStorage storage)) {
            return false;
        }
        if (!storage.addItemToSpatialStorage(pit, player, stack)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.shadow_pit.stored", stack.getHoverName()).withStyle(ChatFormatting.DARK_PURPLE), false);
        return true;
    }

    private static ManasSkillInstance destination(ServerPlayer player, ManasSkillInstance acting) {
        if (acting != null && unlocked(player, acting)) {
            return acting;
        }
        ManasSkillInstance laverna = UltimateSkills.LAVERNA == null ? null : ShadowArmy.skill(player, UltimateSkills.LAVERNA.get());
        if (laverna != null) {
            return laverna;
        }
        ManasSkillInstance vainglory = ShadowArmy.skill(player, UniqueShadowSkills.VAINGLORY.get());
        return vainglory != null && unlocked(player, vainglory) ? vainglory : null;
    }

    private static boolean eject(ServerPlayer player, SimpleContainer container) {
        boolean removed = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || isShadowCopy(stack)) {
                continue;
            }
            container.setItem(slot, ItemStack.EMPTY);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
            removed = true;
        }
        if (removed) {
            player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.shadow_pit.rejected").withStyle(ChatFormatting.RED), false);
        }
        return removed;
    }

    private static SimpleContainer openContainer(ServerPlayer player, ManasSkill skill) {
        if (!(player.containerMenu instanceof SpatialStorageMenu menu)) {
            return null;
        }
        try {
            if (menuSkill == null) {
                menuSkill = SpatialStorageMenu.class.getDeclaredField("skill");
                menuSkill.setAccessible(true);
                menuContainer = SpatialStorageMenu.class.getDeclaredField("container");
                menuContainer.setAccessible(true);
            }
            if (menuSkill.get(menu) != skill) {
                return null;
            }
            Object container = menuContainer.get(menu);
            return container instanceof SimpleContainer simple ? simple : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Entity find(ServerPlayer player, UUID id) {
        for (ServerLevel level : player.server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static void clearArmy(ManasSkillInstance instance) {
        if (instance != null) {
            instance.getOrCreateTag().remove(ARMY);
            instance.markDirty();
        }
    }

    public static final class ShadowPitContainer extends SpatialStorageContainer {
        public ShadowPitContainer(int slots) {
            super(slots, 1);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return isShadowCopy(stack);
        }

        @Override
        public boolean canAddItem(ItemStack stack) {
            return isShadowCopy(stack) && super.canAddItem(stack);
        }
    }
}
