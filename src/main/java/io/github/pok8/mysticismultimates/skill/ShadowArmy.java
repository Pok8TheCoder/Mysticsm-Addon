package io.github.pok8.mysticismultimates.skill;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.entity.template.subclass.IElementalSpirit;
import io.github.manasmods.tensura.entity.template.subclass.IFlying;
import io.github.manasmods.tensura.data.existence.EntityExistenceData;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.data.TensuraCustomData;
import io.github.manasmods.tensura.registry.item.TensuraMaterialItems;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.registry.skill.ResistanceSkills;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.world.TensuraGameRules;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.entity.ShadowForms;
import io.github.pok8.mysticismultimates.entity.ShadowSoulEntity;
import io.github.pok8.mysticismultimates.entity.ShadowSubordinateEntity;
import io.github.pok8.mysticismultimates.registry.ShadowEffects;
import io.github.pok8.mysticismultimates.registry.UltimateSkills;
import io.github.pok8.mysticismultimates.registry.UniqueShadowSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.UUID;

public final class ShadowArmy {
    public static final String MERGED = "ShadowMerged";
    public static final String GARDEN = "ShadowGarden";
    public static final String SAVED_ITEM = "SavedShadowItem";
    public static final String APPLIED_EP = "ShadowAppliedEP";
    private static final String TEMPORARY_SKILLS = "TemporarySkills";
    private static final TagKey<EntityType<?>> NO_MIND_CONTROL = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("tensura", "no_mind_control"));
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
    private static final TagKey<EntityType<?>> CLONES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("tensura", "clones"));
    private static final TagKey<EntityType<?>> MONSTERS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("tensura", "monster"));
    private static final String APPLIED_AURA = "ShadowAppliedAura";
    private static final String APPLIED_MAGICULE = "ShadowAppliedMagicule";
    private static final String APPLIED_CAP = "ShadowAppliedCap";
    private static final ResourceLocation ATTACK_ID = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "shadow_merge_attack");
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "shadow_merge_health");

    private ShadowArmy() {
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        if (victim instanceof ServerPlayer) {
            onPlayerDeath(event);
            return;
        }
        if (victim instanceof Player) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer player)) {
            return;
        }
        if (victim instanceof ShadowSubordinateEntity || player.equals(victim)) {
            return;
        }
        if (mindControlBlocked(player, victim)) {
            return;
        }
        ManasSkillInstance laverna = skill(player, UltimateSkills.LAVERNA == null ? null : UltimateSkills.LAVERNA.get());
        if (laverna != null && laverna.getOrCreateTag().getBoolean(GARDEN)) {
            recruitGarden(player, laverna, victim);
            return;
        }
        int chance = laverna != null ? 100 : soulChance(player);
        if (chance > 0 && player.getRandom().nextInt(100) < chance && !ShadowPit.keepCapture(player, victim)) {
            spawnSoul(player, victim);
        }
    }

    private static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (rewriteFate(player, event)) {
            return;
        }
        releaseOnDeath(player);
        io.github.pok8.mysticismultimates.skill.LavernaSkill.restoreDomain(player);
    }

    private static void releaseOnDeath(ServerPlayer player) {
        ManasSkillInstance plunderer = skill(player, UniqueShadowSkills.PLUNDERER.get());
        if (plunderer != null && plunderer.getOrCreateTag().getBoolean(MERGED)) {
            spendShadow(player, plunderer, false);
        }
        ManasSkillInstance vainglory = skill(player, UniqueShadowSkills.VAINGLORY.get());
        if (vainglory != null && vainglory.getOrCreateTag().getBoolean(MERGED)) {
            spendShadow(player, vainglory, false);
        }
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (entity instanceof ShadowSubordinateEntity soldier && player.getUUID().equals(soldier.getOwnerUUID())) {
                soldier.kill();
            }
        }
        ShadowPit.clearArmy(player);
    }

    private static boolean rewriteFate(ServerPlayer player, LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity killer) || hasSusanoo(killer)) {
            return false;
        }
        ManasSkillInstance laverna = skill(player, UltimateSkills.LAVERNA == null ? null : UltimateSkills.LAVERNA.get());
        ManasSkillInstance vainglory = skill(player, UniqueShadowSkills.VAINGLORY.get());
        ManasSkillInstance save = laverna != null ? laverna : vainglory;
        if (save == null || save.getMastery() < 100.0) {
            return false;
        }
        ExistenceStorage existence = existence(player);
        if (existence == null) {
            return false;
        }
        boolean mastered = save.isMastered(player);
        double rate = laverna != null ? (mastered ? 0.10 : 0.15) : (mastered ? 0.20 : 0.25);
        double cost = Math.max(0.0, EnergyHelper.getMaxMagicule(player) * rate - 1.0);
        if (existence.getMagicule() < cost) {
            return false;
        }
        existence.setMagicule(existence.getMagicule() - cost);
        existence.markDirty();
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        slipAway(player, killer);
        player.displayClientMessage(Component.translatable("mysticism_ultimates.skill.vainglory.death_prevented")
                .withStyle(ChatFormatting.GOLD), false);
        save.addMasteryPoint(player);
        return true;
    }

    private static void slipAway(ServerPlayer player, LivingEntity killer) {
        Level level = player.serverLevel();
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.0F);
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = player.getRandom().nextDouble() * 5.0;
            BlockPos pos = BlockPos.containing(
                    killer.getX() + Math.cos(angle) * distance,
                    killer.getY(),
                    killer.getZ() + Math.sin(angle) * distance);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                return;
            }
        }
    }

    private static boolean hasSusanoo(LivingEntity entity) {
        for (ManasSkillInstance instance : SkillAPI.getSkillsFrom(entity).getLearnedSkills()) {
            ManasSkill skill = instance.getSkill();
            if (skill != null && skill.getRegistryName() != null && "susanoo".equals(skill.getRegistryName().getPath())) {
                return true;
            }
        }
        return false;
    }

    private static boolean mindControlBlocked(ServerPlayer player, LivingEntity victim) {
        boolean blocked = false;
        if (victim instanceof Player target) {
            if (TensuraGameRules.noPlayerMindControl(victim.level()) || target.getAbilities().instabuild) {
                blocked = true;
            }
        }
        EntityType<?> type = victim.getType();
        if (type.is(NO_MIND_CONTROL) || type.is(BOSSES) || type.is(CLONES)) {
            blocked = true;
        }
        String description = type.toString() + " " + BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (description.toLowerCase(java.util.Locale.ROOT).contains("dummy")) {
            blocked = true;
        }
        ManasSkill nullification = ResistanceSkills.SPIRITUAL_ATTACK_NULLIFICATION.get();
        if (nullification != null && SkillUtils.isSkillToggled(victim, nullification)) {
            blocked = true;
        }
        if (blocked) {
            tell(player, "tensura.ability.activation_failed", ChatFormatting.RED);
        }
        return blocked;
    }

    private static void recruitGarden(ServerPlayer player, ManasSkillInstance laverna, LivingEntity victim) {
        double cost = getEffectiveEP(victim) * 0.05;
        ExistenceStorage existence = existence(player);
        if (existence == null || existence.getMagicule() < cost) {
            tell(player, "mysticism_ultimates.skill.plunderer.shadow_warrior.fail", ChatFormatting.RED);
            return;
        }
        existence.setMagicule(existence.getMagicule() - cost);
        existence.markDirty();
        ShadowSubordinateEntity soldier = spawnSoldier(player, victim, victim.getX(), victim.getY(), victim.getZ(), victim.getYRot());
        if (soldier == null) {
            return;
        }
        laverna.addMasteryPoint(player, 5);
        player.level().playSound(null, soldier.blockPosition(), net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CONVERTED, net.minecraft.sounds.SoundSource.NEUTRAL, 2.0F, 1.0F);
    }

    public static void teachShadowMotion(ServerPlayer player) {
        ManasSkill motion = ExtraSkills.SHADOW_MOTION.get();
        if (motion == null || SkillUtils.hasSkill(player, motion)) {
            return;
        }
        if (!player.isCreative()) {
            ExistenceStorage existence = existence(player);
            double cost = motion instanceof io.github.manasmods.tensura.ability.skill.Skill skill
                    ? skill.getDefaultAcquiringMagiculeCost() * player.serverLevel().getGameRules().getInt(TensuraGameRules.MP_SKILL_COST) / 100.0
                    : 0.0;
            if (existence != null && cost > 0.0) {
                existence.setMagicule(Math.max(0.0, existence.getMagicule() - cost));
                existence.markDirty();
            }
        }
        if (SkillHelper.learnSkill(player, motion)) {
            player.displayClientMessage(Component.translatable("tensura.skill.acquire", motion.getName()).withStyle(ChatFormatting.GOLD), false);
        }
    }

    public static void summonWarrior(ServerPlayer player, ManasSkillInstance instance) {
        ShadowSoulEntity soul = look(player, ShadowSoulEntity.class, 5.0);
        if (soul == null || !soul.owner().equals(player.getUUID())) {
            soul = nearestSoul(player, 5.0);
        }
        if (soul == null) {
            tell(player, "tensura.targeting.not_targeted", ChatFormatting.RED);
            return;
        }
        ExistenceStorage existence = existence(player);
        double magicule = existence == null ? 0.0 : existence.getMagicule();
        if (soul.sourceEp() > magicule) {
            tell(player, "mysticism_ultimates.skill.plunderer.shadow_warrior.fail", ChatFormatting.RED);
            return;
        }
        if (existence != null) {
            existence.setMagicule(magicule - soul.sourceEp());
        }
        ShadowSubordinateEntity soldier = UniqueShadowSkills.SHADOW_SUBORDINATE.get().create(player.serverLevel());
        if (soldier == null) {
            return;
        }
        soldier.moveTo(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), 0.0F);
        soldier.store(soul.sourceName(), soul.sourceEp(), soul.sourceHealth(), soul.sourceAttack(), soul.mobData());
        soldier.tame(player);
        player.serverLevel().addFreshEntity(soldier);
        soul.discard();
        instance.addMasteryPoint(player, 5);
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CONVERTED, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void storeSubordinate(ServerPlayer player, ManasSkillInstance instance) {
        LivingEntity target = look(player, LivingEntity.class, 5.0);
        if (target == null) {
            tell(player, "tensura.targeting.not_targeted", ChatFormatting.RED);
            return;
        }
        ItemStack stack;
        if (target instanceof ShadowSubordinateEntity soldier) {
            if (!player.getUUID().equals(soldier.getOwnerUUID())) {
                tell(player, "tensura.targeting.not_allowed", ChatFormatting.RED);
                return;
            }
            stack = storageItem(soldier);
        } else if (!canSeal(player, target)) {
            tell(player, "tensura.targeting.not_allowed", ChatFormatting.RED);
            return;
        } else {
            stack = genericStorage(player, target);
        }
        if (!ShadowPit.offer(player, instance, stack) && !player.addItem(stack)) {
            player.drop(stack, false);
        }
        target.discard();
        instance.addMasteryPoint(player);
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean canSeal(ServerPlayer player, LivingEntity target) {
        if (target instanceof Player || !ownedBy(player, target)) {
            return false;
        }
        ExistenceStorage existence = existence(target);
        if (target instanceof IElementalSpirit && existence != null && existence.getSummoner() != null && existence.getSummonedSecond() <= 0) {
            return false;
        }
        return target.getType().is(MONSTERS) || (existence != null && existence.isSpiritualForm());
    }

    private static boolean ownedBy(ServerPlayer player, LivingEntity target) {
        if (target instanceof TamableAnimal animal && player.getUUID().equals(animal.getOwnerUUID())) {
            return true;
        }
        ExistenceStorage existence = existence(target);
        return existence != null && (player.getUUID().equals(existence.getPermanentOwner())
                || player.getUUID().equals(existence.getTemporaryOwner())
                || player.getUUID().equals(existence.getSummoner()));
    }

    public static void toggleMerge(ServerPlayer player, ManasSkillInstance instance) {
        CompoundTag tag = instance.getOrCreateTag();
        if (tag.getBoolean(MERGED)) {
            spendShadow(player, instance, true);
            player.displayClientMessage(Component.translatable(
                    "mysticism_ultimates.skill.plunderer.shadow_merge.end",
                    tag.getString("ShadowName")).withStyle(ChatFormatting.DARK_PURPLE), false);
            return;
        }
        ItemStack held = player.getMainHandItem();
        CompoundTag itemTag = shadowTag(held);
        if (itemTag == null || !itemTag.getBoolean("MysticismShadowSubordinate")) {
            tell(player, "mysticism_ultimates.skill.plunderer.shadow_merge.error", ChatFormatting.RED);
            return;
        }
        double ep = itemTag.getDouble("ShadowEP");
        float health = (float) itemTag.getDouble("ShadowHP");
        tag.putBoolean(MERGED, true);
        tag.putString("ShadowName", itemTag.getString("ShadowName"));
        tag.putDouble(APPLIED_EP, ep);
        tag.put(SAVED_ITEM, held.save(player.registryAccess()));
        held.shrink(1);
        applyMerge(player, instance, itemTag);
        ExistenceStorage existence = existence(player);
        if (existence != null) {
            grantExistence(player, existence, tag, ep);
            existence.setSpiritualHealth(existence.getSpiritualHealth() + itemTag.getDouble("ShadowSHP"));
            existence.markDirty();
        }
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + health));
        instance.markDirty();
        player.displayClientMessage(Component.translatable(
                "mysticism_ultimates.skill.plunderer.shadow_merge.success",
                tag.getString("ShadowName")).withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    public static void maintain(ServerPlayer player, ManasSkillInstance instance) {
        ShadowPit.police(player, instance);
        repairStoredShadows(player);
        boostAtNight(player, instance);
        if (!instance.getOrCreateTag().getBoolean(MERGED)) {
            return;
        }
        CompoundTag saved = instance.getOrCreateTag().getCompound(SAVED_ITEM);
        CompoundTag item = shadowTag(ItemStack.parseOptional(player.registryAccess(), saved));
        if (item == null) {
            return;
        }
        applyMergeAttributes(player, item.getCompound("ShadowData").getCompound("MysticismAttributes"));
    }

    private static int soulChance(ServerPlayer player) {
        ManasSkillInstance vainglory = skill(player, UniqueShadowSkills.VAINGLORY.get());
        if (vainglory != null && vainglory.isToggled()) {
            return 100;
        }
        ManasSkillInstance plunderer = skill(player, UniqueShadowSkills.PLUNDERER.get());
        if (plunderer == null || !plunderer.isToggled()) {
            return 0;
        }
        return plunderer.isMastered(player) ? 50 : 25;
    }

    private static void spawnSoul(ServerPlayer player, LivingEntity victim) {
        ShadowSoulEntity soul = UniqueShadowSkills.SHADOW_SOUL.get().create(player.serverLevel());
        if (soul == null) {
            return;
        }
        CompoundTag mob = ShadowForms.capture(victim);
        remember(victim, mob);
        double attack = victim.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 2.0 : victim.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double ep = getEffectiveEP(victim);
        soul.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0.0F);
        soul.store(player.getUUID(), victim.getName().getString(), ep, victim.getMaxHealth(), attack, mob);
        player.serverLevel().addFreshEntity(soul);
    }

    private static ShadowSubordinateEntity spawnSoldier(ServerPlayer player, LivingEntity victim, double x, double y, double z, float yaw) {
        CompoundTag mob = ShadowForms.capture(victim);
        remember(victim, mob);
        double attack = victim.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 2.0 : victim.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double ep = getEffectiveEP(victim);
        ShadowSubordinateEntity soldier = UniqueShadowSkills.SHADOW_SUBORDINATE.get().create(player.serverLevel());
        if (soldier == null) {
            return null;
        }
        soldier.moveTo(x, y, z, yaw, 0.0F);
        soldier.store(victim.getName().getString(), ep, victim.getMaxHealth(), attack, mob);
        soldier.tame(player);
        player.serverLevel().addFreshEntity(soldier);
        return soldier;
    }

    private static void remember(LivingEntity victim, CompoundTag mob) {
        CompoundTag attributes = new CompoundTag();
        for (AttributeInstance instance : victim.getAttributes().getSyncableAttributes()) {
            ResourceLocation key = BuiltInRegistries.ATTRIBUTE.getKey(instance.getAttribute().value());
            if (key != null) {
                attributes.putDouble(key.toString(), instance.getBaseValue());
            }
        }
        mob.put("MysticismAttributes", attributes);
        ListTag skills = new ListTag();
        for (ManasSkillInstance instance : SkillAPI.getSkillsFrom(victim).getLearnedSkills()) {
            ManasSkill learned = instance.getSkill();
            if (instance.isTemporarySkill() || instance.getMastery() < 0.0 || learned == null || learned.getRegistryName() == null) {
                continue;
            }
            CompoundTag skill = new CompoundTag();
            skill.putString("id", learned.getRegistryName().toString());
            skill.putDouble("mastery", instance.getMastery());
            skills.add(skill);
        }
        mob.put("MysticismSkills", skills);
        ExistenceStorage existence = existence(victim);
        mob.putDouble("MysticismSpiritual", existence == null ? 0.0 : existence.getSpiritualHealth());
        mob.putBoolean("MysticismCanFly", flies(victim));
        mob.putBoolean("MysticismNeedsBody", victim instanceof io.github.manasmods.tensura.entity.template.subclass.ISpiritual
                || (existence != null && existence.isSpiritualForm()));
    }

    private static boolean flies(LivingEntity victim) {
        return victim instanceof IFlying
                || victim instanceof net.minecraft.world.entity.FlyingMob
                || victim instanceof net.minecraft.world.entity.animal.FlyingAnimal
                || victim.isNoGravity();
    }

    private static boolean spendShadow(ServerPlayer player, ManasSkillInstance instance, boolean returnItem) {
        CompoundTag tag = instance.getOrCreateTag();
        if (tag.getBoolean(MERGED)) {
            clearMerge(player, instance);
            ExistenceStorage existence = existence(player);
            if (existence != null) {
                revokeExistence(player, existence, tag);
            }
            if (returnItem) {
                ItemStack stack = ItemStack.parseOptional(player.registryAccess(), tag.getCompound(SAVED_ITEM));
                if (!stack.isEmpty() && !player.addItem(stack)) {
                    player.drop(stack, false);
                }
            }
            tag.putBoolean(MERGED, false);
            tag.remove(SAVED_ITEM);
            tag.putDouble(APPLIED_EP, 0.0);
            tag.remove(APPLIED_AURA);
            tag.remove(APPLIED_MAGICULE);
            tag.remove(APPLIED_CAP);
            instance.markDirty();
            return true;
        }
        if (returnItem) {
            return false;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            CompoundTag item = shadowTag(stack);
            if (item != null && item.getBoolean("MysticismShadowSubordinate")) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void grantExistence(Player player, ExistenceStorage existence, CompoundTag tag, double ep) {
        double half = Math.max(0.0, ep) / 2.0;
        double auraBefore = existence.getAura();
        double magiculeBefore = existence.getMagicule();
        EnergyHelper.gainAura(player, half, EnergyHelper.GainType.MAX);
        EnergyHelper.gainMagicule(player, half, EnergyHelper.GainType.MAX);
        tag.putDouble(APPLIED_CAP, half);
        tag.putDouble(APPLIED_AURA, Math.max(0.0, existence.getAura() - auraBefore));
        tag.putDouble(APPLIED_MAGICULE, Math.max(0.0, existence.getMagicule() - magiculeBefore));
    }

    private static void revokeExistence(Player player, ExistenceStorage existence, CompoundTag tag) {
        if (tag.contains(APPLIED_CAP)) {
            shrinkMax(player, TensuraAttributes.MAX_AURA, tag.getDouble(APPLIED_CAP));
            shrinkMax(player, TensuraAttributes.MAX_MAGICULE, tag.getDouble(APPLIED_CAP));
            existence.setAura(Math.max(0.0, existence.getAura() - tag.getDouble(APPLIED_AURA)));
            existence.setMagicule(Math.max(0.0, existence.getMagicule() - tag.getDouble(APPLIED_MAGICULE)));
        } else {
            double half = Math.max(0.0, tag.getDouble(APPLIED_EP)) / 2.0;
            existence.setAura(Math.max(0.0, existence.getAura() - half));
            existence.setMagicule(Math.max(0.0, existence.getMagicule() - half));
        }
        existence.markDirty();
    }

    private static void shrinkMax(Player player, Holder<Attribute> attribute, double amount) {
        if (amount <= 0.0 || player.getAttribute(attribute) == null) {
            return;
        }
        var instance = player.getAttribute(attribute);
        instance.setBaseValue(Math.max(0.0, instance.getBaseValue() - amount));
    }

    private static void applyMerge(ServerPlayer player, ManasSkillInstance instance, CompoundTag item) {
        applyMergeAttributes(player, item.getCompound("ShadowData").getCompound("MysticismAttributes"));
        ListTag learned = new ListTag();
        for (Tag entry : item.getCompound("ShadowData").getList("MysticismSkills", Tag.TAG_COMPOUND)) {
            if (!(entry instanceof CompoundTag saved)) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(saved.getString("id"));
            ManasSkill skill = id == null ? null : SkillAPI.getSkillRegistry().get(id);
            if (skill == null || skill == instance.getSkill()) {
                continue;
            }
            if (SkillHelper.learnSkill(player, skill, -2)) {
                SkillAPI.getSkillsFrom(player).getSkill(skill).ifPresent(learnedSkill -> learnedSkill.setRemoveTime(-2));
                learned.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
                player.displayClientMessage(Component.translatable("tensura.skill.temporary.success_drain", skill.getName()).withStyle(ChatFormatting.GREEN), false);
            } else if (SkillUtils.hasSkill(player, skill)) {
                player.displayClientMessage(Component.translatable("tensura.skill.temporary.already_have", skill.getName()).withStyle(ChatFormatting.RED), false);
            }
        }
        instance.getOrCreateTag().put(TEMPORARY_SKILLS, learned);
    }

    private static void applyMergeAttributes(Player player, CompoundTag attributes) {
        for (String key : attributes.getAllKeys()) {
            Holder<Attribute> attribute = attributeHolder(key);
            if (attribute == null || excluded(attribute) || player.getAttribute(attribute) == null) {
                continue;
            }
            player.getAttribute(attribute).addOrUpdateTransientModifier(new AttributeModifier(
                    modifierId(key), attributes.getDouble(key), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void clearMerge(Player player, ManasSkillInstance instance) {
        if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(ATTACK_ID);
        }
        if (player.getAttribute(Attributes.MAX_HEALTH) != null) {
            player.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_ID);
        }
        CompoundTag saved = shadowTag(ItemStack.parseOptional(player.registryAccess(), instance.getOrCreateTag().getCompound(SAVED_ITEM)));
        if (saved != null) {
            for (String key : saved.getCompound("ShadowData").getCompound("MysticismAttributes").getAllKeys()) {
                Holder<Attribute> attribute = attributeHolder(key);
                if (attribute != null && player.getAttribute(attribute) != null) {
                    player.getAttribute(attribute).removeModifier(modifierId(key));
                }
            }
        }
        for (Tag entry : instance.getOrCreateTag().getList(TEMPORARY_SKILLS, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            ManasSkill skill = id == null ? null : SkillAPI.getSkillRegistry().get(id);
            if (skill == null) {
                continue;
            }
            ManasSkillInstance learned = SkillAPI.getSkillsFrom(player).getSkill(skill).orElse(null);
            if (learned != null && learned.isTemporarySkill()) {
                SkillAPI.getSkillsFrom(player).forgetSkill(skill, Component.translatable("tensura.skill.temporary.remove", skill.getName()).withStyle(ChatFormatting.YELLOW));
            }
        }
        instance.getOrCreateTag().remove(TEMPORARY_SKILLS);
    }

    private static void boostAtNight(ServerPlayer player, ManasSkillInstance instance) {
        long time = player.level().getDayTime() % 24000L;
        if (time < 13000L || time > 23000L || ShadowEffects.LUNAR_BOOST.get() == null) {
            return;
        }
        MobEffectInstance boost = new MobEffectInstance(ShadowEffects.LUNAR_BOOST, 200, 0, false, false, false);
        AABB area = player.getBoundingBox().inflate(30.0);
        for (ShadowSubordinateEntity soldier : player.level().getEntitiesOfClass(ShadowSubordinateEntity.class, area, entity -> player.getUUID().equals(entity.getOwnerUUID()))) {
            soldier.addEffect(boost);
        }
        if (instance.getOrCreateTag().getBoolean(MERGED)) {
            player.addEffect(new MobEffectInstance(ShadowEffects.LUNAR_BOOST, 200, 0, false, false, false));
        }
    }

    private static boolean excluded(Holder<Attribute> attribute) {
        return attribute == Attributes.MAX_HEALTH
                || attribute == Attributes.FOLLOW_RANGE
                || attribute == Attributes.LUCK
                || attribute == Attributes.GRAVITY
                || attribute == TensuraAttributes.MAX_AURA
                || attribute == TensuraAttributes.MAX_MAGICULE
                || attribute == TensuraAttributes.HEIGHT_MULTIPLIER
                || attribute == TensuraAttributes.WIDTH_MULTIPLIER
                || attribute == io.github.manasmods.manascore.attribute.api.ManasCoreAttributes.CRITICAL_ATTACK_CHANCE
                || attribute == io.github.manasmods.manascore.attribute.api.ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER;
    }

    private static Holder<Attribute> attributeHolder(String key) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        return id == null ? null : BuiltInRegistries.ATTRIBUTE.getHolder(id).orElse(null);
    }

    private static ResourceLocation modifierId(String attributeKey) {
        String path = attributeKey.toLowerCase(java.util.Locale.ROOT).replace(':', '_').replace('.', '_').replace('/', '_');
        return ResourceLocation.fromNamespaceAndPath(MysticismUltimates.MOD_ID, "merge_" + path);
    }

    static ItemStack captureItem(ServerPlayer player, LivingEntity victim) {
        ShadowSubordinateEntity soldier = UniqueShadowSkills.SHADOW_SUBORDINATE.get().create(player.serverLevel());
        if (soldier == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag mob = ShadowForms.capture(victim);
        remember(victim, mob);
        double attack = victim.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 2.0 : victim.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double ep = getEffectiveEP(victim);
        soldier.store(victim.getName().getString(), ep, victim.getMaxHealth(), attack, mob);
        soldier.tame(player);
        ItemStack stack = storageItem(soldier);
        soldier.discard();
        return stack;
    }

    static ItemStack genericStorage(ServerPlayer player, LivingEntity target) {
        ItemStack stack = new ItemStack(TensuraMaterialItems.SHADOW_STORAGE.get());
        double ep = getEffectiveEP(target);
        double hp = target.getHealth();
        double shp = getEffectiveSpiritualHealth(target);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("MysticismGenericSubordinate", true);
        tag.putBoolean("MysticismShadowSubordinate", true);
        CompoundTag mob = ShadowForms.capture(target);
        remember(target, mob);
        tag.put("ShadowData", mob);
        tag.putDouble("ShadowEP", ep);
        tag.putDouble("ShadowHP", hp);
        tag.putDouble("ShadowSHP", shp);
        double attack = target.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 2.0 : target.getAttributeValue(Attributes.ATTACK_DAMAGE);
        tag.putDouble("ShadowAttack", attack);
        tag.putString("ShadowName", target.getName().getString());
        tag.putString("EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        setTagEP(target, ep, shp);
        CompoundTag saved = new CompoundTag();
        target.saveWithoutId(saved);
        saved.remove("Pos");
        saved.remove("Motion");
        saved.remove("Passengers");
        ListTag targetTags = saved.getList("Tags", Tag.TAG_STRING);
        targetTags.add(net.minecraft.nbt.StringTag.valueOf("MysticismShadowEP:" + ep));
        targetTags.add(net.minecraft.nbt.StringTag.valueOf("MysticismShadowSHP:" + shp));
        saved.put("Tags", targetTags);
        CompoundTag manasCore = saved.getCompound("ManasCoreStorage");
        CompoundTag exTag = manasCore.getCompound("tensura:existence_storage");
        exTag.putDouble("aura", ep / 2.0);
        exTag.putDouble("magicule", ep / 2.0);
        exTag.putDouble("spiritualHealth", shp);
        exTag.putBoolean("skipEPDrop", true);
        manasCore.put("tensura:existence_storage", exTag);
        saved.put("ManasCoreStorage", manasCore);
        stack.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(saved));

        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("tooltip.tensura.shadow_storage.name", target.getName().getString())
                .withStyle(ChatFormatting.GOLD));
        return stack;
    }

    static ItemStack storageItem(ShadowSubordinateEntity soldier) {
        ItemStack stack = new ItemStack(TensuraMaterialItems.SHADOW_STORAGE.get());
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("MysticismShadowSubordinate", true);
        tag.put("ShadowData", soldier.mobData());
        double ep = Math.max(soldier.sourceEp(), getEffectiveEP(soldier));
        tag.putDouble("ShadowEP", ep);
        tag.putDouble("ShadowHP", soldier.getHealth());
        tag.putDouble("ShadowSHP", getEffectiveSpiritualHealth(soldier));
        tag.putDouble("ShadowAttack", soldier.sourceAttack());
        tag.putString("ShadowName", soldier.sourceName());
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(soldier.getType());
        if (type != null) {
            tag.putString("EntityType", type.toString());
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(soldierRecord(soldier)));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("tooltip.tensura.shadow_storage.name", soldier.sourceName())
                .withStyle(ChatFormatting.GOLD));
        return stack;
    }

    private static CompoundTag soldierRecord(ShadowSubordinateEntity soldier) {
        setTagEP(soldier, soldier.sourceEp(), soldier.mobData().getDouble("MysticismSpiritual"));
        CompoundTag saved = new CompoundTag();
        soldier.saveWithoutId(saved);
        saved.remove("Pos");
        saved.remove("Motion");
        saved.remove("Passengers");
        double ep = Math.max(soldier.sourceEp(), getEffectiveEP(soldier));
        double shp = soldier.mobData().getDouble("MysticismSpiritual");
        ListTag soldierTags = saved.getList("Tags", Tag.TAG_STRING);
        soldierTags.add(net.minecraft.nbt.StringTag.valueOf("MysticismShadowEP:" + ep));
        soldierTags.add(net.minecraft.nbt.StringTag.valueOf("MysticismShadowSHP:" + shp));
        saved.put("Tags", soldierTags);
        CompoundTag manasCore = saved.getCompound("ManasCoreStorage");
        CompoundTag exTag = manasCore.getCompound("tensura:existence_storage");
        exTag.putDouble("aura", ep / 2.0);
        exTag.putDouble("magicule", ep / 2.0);
        exTag.putDouble("spiritualHealth", shp);
        exTag.putBoolean("skipEPDrop", true);
        exTag.putBoolean("nameable", true);
        manasCore.put("tensura:existence_storage", exTag);
        saved.put("ManasCoreStorage", manasCore);
        return saved;
    }

    private static void repairStoredShadows(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            repairShadowItem(player.getInventory().getItem(slot), player.getUUID());
        }
    }

    private static void repairShadowItem(ItemStack stack, UUID owner) {
        CompoundTag item = shadowTag(stack);
        if (item == null || !item.getBoolean("MysticismShadowSubordinate") || stack.has(DataComponents.BUCKET_ENTITY_DATA)) {
            return;
        }
        double ep = item.getDouble("ShadowEP");
        double shp = item.getDouble("ShadowSHP");
        CompoundTag entity = new CompoundTag();
        entity.putString("ShadowSourceName", item.getString("ShadowName"));
        entity.putDouble("ShadowSourceEP", ep);
        entity.putDouble("ShadowSourceAttack", item.getDouble("ShadowAttack"));
        entity.putFloat("ShadowSourceHealth", (float) item.getDouble("ShadowHP"));
        entity.put("ShadowMobData", item.getCompound("ShadowData"));
        entity.putUUID("Owner", owner);
        ListTag tags = new ListTag();
        tags.add(net.minecraft.nbt.StringTag.valueOf("MysticismShadowEP:" + ep));
        tags.add(net.minecraft.nbt.StringTag.valueOf("MysticismShadowSHP:" + shp));
        entity.put("Tags", tags);
        CompoundTag manasCore = new CompoundTag();
        CompoundTag exTag = new CompoundTag();
        exTag.putDouble("aura", ep / 2.0);
        exTag.putDouble("magicule", ep / 2.0);
        exTag.putDouble("spiritualHealth", shp);
        exTag.putBoolean("skipEPDrop", true);
        exTag.putBoolean("nameable", true);
        manasCore.put("tensura:existence_storage", exTag);
        entity.put("ManasCoreStorage", manasCore);
        stack.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(entity));
    }

    private static CompoundTag shadowTag(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(TensuraMaterialItems.SHADOW_STORAGE.get())) {
            return null;
        }
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static ShadowSoulEntity nearestSoul(Player player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        ShadowSoulEntity best = null;
        double bestDistance = range * range;
        for (ShadowSoulEntity soul : player.level().getEntitiesOfClass(ShadowSoulEntity.class, box, soul -> soul.owner().equals(player.getUUID()))) {
            double distance = soul.distanceToSqr(player);
            if (distance < bestDistance) {
                best = soul;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static <T extends Entity> T look(Player player, Class<T> type, double range) {
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0);
        T best = null;
        double bestDot = 0.85;
        for (T entity : player.level().getEntitiesOfClass(type, box, entity -> entity != player && entity.isAlive())) {
            double dot = entity.getEyePosition().subtract(player.getEyePosition()).normalize().dot(player.getLookAngle());
            if (dot > bestDot && player.distanceTo(entity) <= range) {
                best = entity;
                bestDot = dot;
            }
        }
        return best;
    }

    static ManasSkillInstance skill(LivingEntity entity, ManasSkill skill) {
        if (skill == null) {
            return null;
        }
        return SkillAPI.getSkillsFrom(entity).getSkill(skill).orElse(null);
    }

    public static void setTagEP(LivingEntity entity, double ep, double shp) {
        if (entity == null) {
            return;
        }
        entity.getTags().removeIf(t -> t.startsWith("MysticismShadowEP:") || t.startsWith("MysticismShadowSHP:"));
        entity.addTag("MysticismShadowEP:" + ep);
        entity.addTag("MysticismShadowSHP:" + shp);
    }

    public static double getTagEP(LivingEntity entity) {
        if (entity == null) {
            return 0.0;
        }
        for (String tag : entity.getTags()) {
            if (tag.startsWith("MysticismShadowEP:")) {
                try {
                    return Double.parseDouble(tag.substring("MysticismShadowEP:".length()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0.0;
    }

    public static double getTagSHP(LivingEntity entity) {
        if (entity == null) {
            return 0.0;
        }
        for (String tag : entity.getTags()) {
            if (tag.startsWith("MysticismShadowSHP:")) {
                try {
                    return Double.parseDouble(tag.substring("MysticismShadowSHP:".length()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0.0;
    }

    public static double getEffectiveEP(LivingEntity victim) {
        if (victim == null) {
            return 0.0;
        }
        double ep = 0.0;
        if (victim instanceof ShadowSubordinateEntity soldier) {
            ep = Math.max(ep, soldier.sourceEp());
        }
        ExistenceStorage ex = existence(victim);
        if (ex != null) {
            ep = Math.max(ep, ex.getEP());
        }
        ep = Math.max(ep, EnergyHelper.getMaxEP(victim));
        ep = Math.max(ep, getTagEP(victim));
        if (ep <= 100.0 && victim.level() != null) {
            try {
                var registryOpt = victim.level().registryAccess().registry(TensuraCustomData.ENTITY_EXISTENCE);
                if (registryOpt.isPresent()) {
                    ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
                    EntityExistenceData data = registryOpt.get().get(id);
                    if (data != null) {
                        double dataEp = Math.max(data.maxAura(), data.minAura()) + Math.max(data.maxMagicule(), data.minMagicule());
                        if (dataEp > 0.0) {
                            ep = Math.max(ep, dataEp);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return Math.max(0.0, ep);
    }

    public static double getEffectiveSpiritualHealth(LivingEntity victim) {
        if (victim == null) {
            return 0.0;
        }
        ExistenceStorage ex = existence(victim);
        double shp = ex == null ? 0.0 : ex.getSpiritualHealth();
        if (victim.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH) != null) {
            shp = Math.max(shp, victim.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));
        }
        shp = Math.max(shp, getTagSHP(victim));
        if (shp <= 0.0 && victim.level() != null) {
            try {
                var registryOpt = victim.level().registryAccess().registry(TensuraCustomData.ENTITY_EXISTENCE);
                if (registryOpt.isPresent()) {
                    EntityExistenceData data = registryOpt.get().get(BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()));
                    if (data != null && data.spiritualHP() > 0) {
                        shp = data.spiritualHP();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return Math.max(0.0, shp);
    }

    public static dev.architectury.event.EventResult onTensuraAddFresh(Entity entity, net.minecraft.world.level.LevelAccessor level) {
        if (entity instanceof LivingEntity living) {
            handleFreshShadow(living);
        }
        return dev.architectury.event.EventResult.pass();
    }

    public static void onEntityJoinLevel(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            handleFreshShadow(living);
        }
    }

    private static void handleFreshShadow(LivingEntity living) {
        if (living instanceof ShadowSubordinateEntity soldier) {
            soldier.applyCapturedStats();
        } else {
            double ep = getTagEP(living);
            if (ep > 0.0) {
                double shp = getTagSHP(living);
                ShadowPit.restoreGenericStats(living, ep, shp);
            }
        }
    }

    public static ExistenceStorage existence(LivingEntity entity) {
        try {
            return ((StorageHolder) (Object) entity).manasCore$getStorage(ExistenceStorage.getKey());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void tell(Player player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), false);
    }

}
