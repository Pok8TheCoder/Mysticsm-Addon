package io.github.pok8.mysticismultimates.skill;

import io.github.manasmods.tensura.entity.human.DwarfEntity;
import io.github.pok8.mysticismultimates.registry.ShadowEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.minecraft.core.Direction;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GoldLuckHandler {
    private static final Map<Container, ActiveChestAlignment> ACTIVE_ALIGNMENTS = new HashMap<>();
    private static final Set<Container> PROCESSED_CONTAINERS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<UUID, Long> LAST_EVASION = new HashMap<>();
    private static final Map<UUID, PendingChest> PENDING_CHESTS = new ConcurrentHashMap<>();

    private static class PendingChest {
        final BlockPos pos;
        final ResourceKey<LootTable> lootTable;
        final long timestamp;

        PendingChest(BlockPos pos, ResourceKey<LootTable> lootTable, long timestamp) {
            this.pos = pos;
            this.lootTable = lootTable;
            this.timestamp = timestamp;
        }
    }

    public static boolean hasGoldLuck(LivingEntity entity) {
        if (entity == null) return false;
        if (entity.hasEffect(ShadowEffects.GOLD_LUCK)) return true;
        if (entity instanceof ServerPlayer player) {
            try {
                if (io.github.pok8.mysticismultimates.registry.CustomSkills.OTHERWORLDLY != null
                        && io.github.pok8.mysticismultimates.registry.CustomSkills.OTHERWORLDLY.get() != null) {
                    var opt = io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(player)
                            .getSkill(io.github.pok8.mysticismultimates.registry.CustomSkills.OTHERWORLDLY.get());
                    if (opt.isPresent() && opt.get().getTag().getBoolean("gold_luck_active")) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private static Field lootTablePoolsField;
    private static Field lootPoolEntriesField;
    private static Field singletonWeightField;
    private static Field lootItemHolderField;

    static {
        try {
            lootTablePoolsField = LootTable.class.getDeclaredField("pools");
            lootTablePoolsField.setAccessible(true);
        } catch (Throwable ignored) {}
        try {
            lootPoolEntriesField = LootPool.class.getDeclaredField("entries");
            lootPoolEntriesField.setAccessible(true);
        } catch (Throwable ignored) {}
        try {
            singletonWeightField = LootPoolSingletonContainer.class.getDeclaredField("weight");
            singletonWeightField.setAccessible(true);
        } catch (Throwable ignored) {}
        try {
            lootItemHolderField = LootItem.class.getDeclaredField("item");
            lootItemHolderField.setAccessible(true);
        } catch (Throwable ignored) {}
    }

    private GoldLuckHandler() {}

    /* =========================================================================
     * 1. Golden Evasion (25% chance, 5s internal cooldown, particles & chime)
     * ========================================================================= */
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasGoldLuck(player)) {
            return;
        }

        long currentTick = player.level().getGameTime();
        long lastEvasion = LAST_EVASION.getOrDefault(player.getUUID(), 0L);
        if (currentTick - lastEvasion < 100) { // 5s = 100 ticks
            return;
        }

        if (player.getRandom().nextFloat() < 0.25f) {
            event.setCanceled(true);
            LAST_EVASION.put(player.getUUID(), currentTick);

            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.8F);

            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
                level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.3, 0.4, 0.3, 0.05);
            }
            player.displayClientMessage(
                    Component.translatable("mysticism_ultimates.gold_luck.evasion").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    true
            );
        }
    }

    /* =========================================================================
     * 2. Virtual Fortune V & Foliage Drop Boost (Sapling/Apple 5x-10x, no new items)
     * ========================================================================= */
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasGoldLuck(player)) {
            return;
        }

        RandomSource random = player.getRandom();

        // Foliage handling
        if (event.getState().is(BlockTags.LEAVES)) {
            boolean hasSaplingOrApple = false;
            for (ItemEntity dropEntity : event.getDrops()) {
                ItemStack stack = dropEntity.getItem();
                if (stack.is(ItemTags.SAPLINGS) || stack.is(Items.APPLE)) {
                    hasSaplingOrApple = true;
                    // 5x - 10x higher yield
                    int multiplier = 5 + random.nextInt(6);
                    stack.setCount(Math.min(stack.getMaxStackSize(), stack.getCount() * multiplier));
                    dropEntity.setItem(stack);
                }
            }

            // If vanilla did not drop any sapling, roll 50% chance (10x vanilla 5%) to drop that leaf's natural sapling
            if (!hasSaplingOrApple) {
                Item naturalSapling = getLeavesSapling(event.getState().getBlock());
                if (naturalSapling != null && random.nextFloat() < 0.50f) {
                    BlockPos pos = event.getPos();
                    ItemEntity saplingDrop = new ItemEntity(
                            event.getLevel(),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            new ItemStack(naturalSapling, 1 + random.nextInt(2))
                    );
                    event.getDrops().add(saplingDrop);
                }
                // Apple check for oak / dark oak
                if (isOakLeaves(event.getState().getBlock()) && random.nextFloat() < 0.10f) {
                    BlockPos pos = event.getPos();
                    ItemEntity appleDrop = new ItemEntity(
                            event.getLevel(),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            new ItemStack(Items.APPLE, 1)
                    );
                    event.getDrops().add(appleDrop);
                }
            }
            return;
        }

        // Virtual Fortune V for ores, minerals, and fortune-affected block breaks
        for (ItemEntity dropEntity : event.getDrops()) {
            ItemStack stack = dropEntity.getItem();
            if (stack.isEmpty() || stack.getMaxStackSize() <= 1) {
                continue;
            }
            if (event.getState().is(net.neoforged.neoforge.common.Tags.Blocks.ORES)
                    || stack.is(net.neoforged.neoforge.common.Tags.Items.RAW_MATERIALS)
                    || isMineralDrop(stack.getItem())) {
                int bonus = 1 + random.nextInt(5); // Fortune V roll: +1 to +5 items
                stack.grow(bonus);
                dropEntity.setItem(stack);
            }
        }
    }

    private static boolean isMineralDrop(Item item) {
        return item == Items.DIAMOND || item == Items.EMERALD || item == Items.RAW_IRON
                || item == Items.RAW_GOLD || item == Items.RAW_COPPER || item == Items.COAL
                || item == Items.REDSTONE || item == Items.LAPIS_LAZULI || item == Items.QUARTZ
                || item == Items.AMETHYST_SHARD;
    }

    private static boolean isOakLeaves(net.minecraft.world.level.block.Block block) {
        String name = block.getDescriptionId().toLowerCase(Locale.ROOT);
        return name.contains("oak");
    }

    private static Item getLeavesSapling(net.minecraft.world.level.block.Block block) {
        String name = block.getDescriptionId().toLowerCase(Locale.ROOT);
        if (name.contains("dark_oak")) return Items.DARK_OAK_SAPLING;
        if (name.contains("oak")) return Items.OAK_SAPLING;
        if (name.contains("spruce")) return Items.SPRUCE_SAPLING;
        if (name.contains("birch")) return Items.BIRCH_SAPLING;
        if (name.contains("jungle")) return Items.JUNGLE_SAPLING;
        if (name.contains("acacia")) return Items.ACACIA_SAPLING;
        if (name.contains("cherry")) return Items.CHERRY_SAPLING;
        if (name.contains("mangrove")) return Items.MANGROVE_PROPAGULE;
        if (name.contains("azalea")) return Items.AZALEA;
        return null;
    }

    /* =========================================================================
     * 3. Virtual Looting V (Mob drops +1 to +5 count bonus)
     * ========================================================================= */
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasGoldLuck(player)) {
            return;
        }

        RandomSource random = player.getRandom();
        for (ItemEntity dropEntity : event.getDrops()) {
            ItemStack stack = dropEntity.getItem();
            if (!stack.isEmpty() && stack.getMaxStackSize() > 1) {
                int bonus = 1 + random.nextInt(5); // Looting V roll
                stack.grow(bonus);
                dropEntity.setItem(stack);
            }
        }
    }

    /* =========================================================================
     * 4. Dwarf & Villager Job / Trade Optimization ("OLucked" Tag)
     * ========================================================================= */
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasGoldLuck(player)) {
            return;
        }

        Entity target = event.getTarget();
        if (target instanceof Villager || target instanceof DwarfEntity || target instanceof AbstractVillager) {
            target.addTag("OLucked");
            if (target instanceof Merchant merchant) {
                optimizeMerchant(merchant);
            }
        }
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getLevel().getServer() == null) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof Villager || entity instanceof DwarfEntity) {
            Level level = event.getLevel();
            MinecraftServer server = level.getServer();
            if (server == null) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.level() == level && player.distanceToSqr(entity) <= 256.0) {
                    if (hasGoldLuck(player)) {
                        entity.addTag("OLucked");
                        if (entity instanceof Merchant merchant) {
                            optimizeMerchant(merchant);
                        }
                        break;
                    }
                }
            }
        }
    }

    public static void optimizeMerchant(Merchant merchant) {
        if (merchant == null) return;
        try {
            for (MerchantOffer offer : merchant.getOffers()) {
                // Set buy price to minimum possible (1) via discount (low charge)
                int baseCount = offer.getBaseCostA().getCount();
                if (baseCount > 1) {
                    offer.setSpecialPriceDiff(-baseCount + 1);
                } else if (offer.getSpecialPriceDiff() > 0) {
                    offer.setSpecialPriceDiff(0); // Never upcharge
                }
                // Demand can never be positive (prevents upcharging from sales patterns)
                if (offer.getDemand() > 0) {
                    setField(offer, "demand", 0);
                }
                // Set max uses high (32)
                if (offer.getMaxUses() < 32) {
                    setField(offer, "maxUses", 32);
                }
                // Boost sell yield to maximum possible
                ItemStack result = offer.getResult();
                if (result.isStackable() && result.getMaxStackSize() > 1 && result.getCount() < result.getMaxStackSize()) {
                    int boosted = Math.min(result.getMaxStackSize(), Math.max(result.getCount() * 2, 8));
                    result.setCount(boosted);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void setField(Object obj, String fieldName, Object val) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, val);
        } catch (Throwable ignored) {}
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasGoldLuck(player)) return;

        BlockPos pos = event.getPos();
        Level level = event.getLevel();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        ResourceKey<LootTable> lootTable = null;

        // 1. Lootr Block Entity Support
        if (be.getClass().getName().contains("Lootr")) {
            try {
                Method hasLootMethod = null;
                for (Method m : be.getClass().getMethods()) {
                    if (m.getName().equals("hasLootAvailable") && m.getParameterCount() == 1) {
                        hasLootMethod = m;
                        break;
                    }
                }
                boolean available = true;
                if (hasLootMethod != null) {
                    available = (boolean) hasLootMethod.invoke(be, player);
                }
                if (available) {
                    for (String mName : List.of("getInfoLootTable", "getLootTable")) {
                        try {
                            Method getTableMethod = be.getClass().getMethod(mName);
                            lootTable = (ResourceKey<LootTable>) getTableMethod.invoke(be);
                            if (lootTable != null) break;
                        } catch (NoSuchMethodException ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Vanilla RandomizableContainerBlockEntity Support
        if (lootTable == null && be instanceof RandomizableContainerBlockEntity rc) {
            lootTable = rc.getLootTable();
        }

        // 3. Double Chest Support
        if (lootTable == null && be instanceof ChestBlockEntity chestBe) {
            BlockState state = chestBe.getBlockState();
            if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                Direction dir = ChestBlock.getConnectedDirection(state);
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor instanceof RandomizableContainerBlockEntity nrc) {
                    lootTable = nrc.getLootTable();
                }
            }
        }

        if (lootTable != null) {
            PENDING_CHESTS.put(player.getUUID(), new PendingChest(pos, lootTable, System.currentTimeMillis()));
        }
    }

    /* =========================================================================
     * 5. Chest Loot "Slot Overflow" & 2-Second Delay & Same-Odds Drop-Hit System
     * ========================================================================= */
    public static void onPlayerOpenContainer(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasGoldLuck(player)) {
            return;
        }

        Container container = findTargetChestContainer(event.getContainer().slots, player);
        if (container == null || PROCESSED_CONTAINERS.contains(container)) {
            return;
        }

        PendingChest pending = PENDING_CHESTS.remove(player.getUUID());
        ResourceKey<LootTable> lootTableKey = null;
        BlockPos chestPos = null;

        if (pending != null && (System.currentTimeMillis() - pending.timestamp) < 5000) {
            lootTableKey = pending.lootTable;
            chestPos = pending.pos;
        }

        if (lootTableKey == null) {
            lootTableKey = resolveLootTableKey(container, player);
        }

        if (lootTableKey == null) {
            return;
        }

        if (chestPos == null) {
            chestPos = resolveContainerPos(container, player);
        }

        // Mark as processed so it only triggers on first opening
        PROCESSED_CONTAINERS.add(container);

        // If vanilla randomizable container, ensure normal loot is unpacked first (no common loot lost)
        if (container instanceof RandomizableContainer rc) {
            rc.unpackLootTable(player);
        }

        // Collect empty slot indices
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                emptySlots.add(i);
            }
        }
        int totalEmpty = emptySlots.size();
        if (totalEmpty == 0) {
            return;
        }

        // Candidate overflow slots: N = floor(E / 1.5) rounded down
        int candidateCount = Math.min(totalEmpty, Math.max(1, (int) Math.floor(totalEmpty / 1.5)));
        Collections.shuffle(emptySlots, new Random(player.getRandom().nextLong()));
        List<Integer> candidateSlots = new ArrayList<>(emptySlots.subList(0, candidateCount));

        // Extract loot table items with baseline drop rate < 25%
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        LootTable table = server.reloadableRegistries().getLootTable(lootTableKey);
        List<ItemCandidate> candidates = extractRareCandidates(table);

        // Group into equal-probability tiers for Same-Odds Drop-Hit optimization
        List<RarityTier> tiers = buildTiers(candidates);

        // Pre-roll overflow rewards
        Map<Integer, ItemStack> finalRolledItems = new HashMap<>();
        RandomSource random = player.getRandom();
        for (int slotIndex : candidateSlots) {
            ItemStack stack = rollOverflowSlot(tiers, random);
            finalRolledItems.put(slotIndex, stack);
        }

        // Place placeholder alignment items in candidate slots for the 2-second delay
        ItemStack placeholder = createPlaceholderItem();
        for (int slotIndex : candidateSlots) {
            container.setItem(slotIndex, placeholder.copy());
        }

        if (player.containerMenu != null) {
            player.containerMenu.broadcastChanges();
        }

        ACTIVE_ALIGNMENTS.put(container, new ActiveChestAlignment(
                player.getUUID(),
                container,
                candidateSlots,
                finalRolledItems,
                chestPos,
                40 // 2 seconds = 40 ticks
        ));

        player.displayClientMessage(
                Component.translatable("mysticism_ultimates.gold_luck.aligning").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC),
                true
        );
    }

    public static void onPlayerCloseContainer(PlayerContainerEvent.Close event) {
        if (ACTIVE_ALIGNMENTS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Container, ActiveChestAlignment>> it = ACTIVE_ALIGNMENTS.entrySet().iterator();
        while (it.hasNext()) {
            ActiveChestAlignment alignment = it.next().getValue();
            if (alignment.playerUUID.equals(event.getEntity().getUUID())) {
                flushAlignment(alignment, (ServerPlayer) event.getEntity(), false);
                it.remove();
            }
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE_ALIGNMENTS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Container, ActiveChestAlignment>> it = ACTIVE_ALIGNMENTS.entrySet().iterator();
        while (it.hasNext()) {
            ActiveChestAlignment alignment = it.next().getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(alignment.playerUUID);

            boolean playerInvalid = (player == null || !player.isAlive() || player.containerMenu == null);
            if (playerInvalid || alignment.remainingTicks <= 0) {
                flushAlignment(alignment, player, alignment.remainingTicks <= 0);
                it.remove();
                continue;
            }

            alignment.remainingTicks--;

            // Protection: if player carried placeholder on cursor, clear it
            if (player.containerMenu != null && isPlaceholder(player.containerMenu.getCarried())) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
                player.containerMenu.broadcastChanges();
            }

            // Subtle chime every 10 ticks
            if (alignment.remainingTicks % 10 == 0) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 1.2F + (40 - alignment.remainingTicks) * 0.02F);
            }

            if (alignment.remainingTicks == 0) {
                flushAlignment(alignment, player, true);
                it.remove();
            }
        }
    }

    private static void flushAlignment(ActiveChestAlignment alignment, ServerPlayer player, boolean success) {
        for (int slot : alignment.candidateSlots) {
            ItemStack finalStack = alignment.finalItems.get(slot);
            alignment.container.setItem(slot, (finalStack != null && !finalStack.isEmpty()) ? finalStack : ItemStack.EMPTY);
        }
        // Safety: remove any leftover placeholders in the container
        for (int i = 0; i < alignment.container.getContainerSize(); i++) {
            if (isPlaceholder(alignment.container.getItem(i))) {
                alignment.container.setItem(i, ItemStack.EMPTY);
            }
        }
        if (player != null) {
            if (player.containerMenu != null) {
                if (isPlaceholder(player.containerMenu.getCarried())) {
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                }
                player.containerMenu.broadcastChanges();
            }
            // Safety: clean placeholders from player inventory if any leaked
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (isPlaceholder(player.getInventory().getItem(i))) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }
        if (success && player != null) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
            if (player.level() instanceof ServerLevel level && alignment.pos != null) {
                level.sendParticles(ParticleTypes.END_ROD, alignment.pos.getX() + 0.5, alignment.pos.getY() + 0.8, alignment.pos.getZ() + 0.5, 24, 0.4, 0.3, 0.4, 0.05);
                level.sendParticles(ParticleTypes.GLOW, alignment.pos.getX() + 0.5, alignment.pos.getY() + 0.8, alignment.pos.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.05);
            }
            player.displayClientMessage(
                    Component.translatable("mysticism_ultimates.gold_luck.fates_aligned").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    true
            );
        }
    }

    public static ItemStack createPlaceholderItem() {
        ItemStack placeholder = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
        placeholder.set(DataComponents.CUSTOM_NAME,
                Component.literal("✦ Otherworldly: Aligning Fates..... ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        placeholder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return placeholder;
    }

    public static boolean isPlaceholder(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.LIGHT_GRAY_STAINED_GLASS_PANE)) {
            return false;
        }
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        return name != null && name.getString().contains("Otherworldly: Aligning Fates");
    }

    private static Container findTargetChestContainer(List<Slot> slots, ServerPlayer player) {
        for (Slot slot : slots) {
            Container cont = slot.container;
            if (cont == null) continue;
            if (cont == player.getInventory() || cont instanceof net.minecraft.world.entity.player.Inventory) continue;
            return cont;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<LootTable> resolveLootTableKey(Container container, ServerPlayer player) {
        // 1. Lootr Support via reflection
        if (container.getClass().getName().contains("Lootr")) {
            try {
                Method getInfoMethod = container.getClass().getMethod("getInfo");
                Object info = getInfoMethod.invoke(container);
                if (info != null) {
                    Method getLootTableMethod = null;
                    for (String mName : List.of("getInfoLootTable", "getLootTable")) {
                        try {
                            getLootTableMethod = info.getClass().getMethod(mName);
                            break;
                        } catch (NoSuchMethodException ignored) {}
                    }
                    if (getLootTableMethod != null) {
                        return (ResourceKey<LootTable>) getLootTableMethod.invoke(info);
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Vanilla RandomizableContainer
        if (container instanceof RandomizableContainer rc) {
            ResourceKey<LootTable> key = rc.getLootTable();
            if (key != null) {
                return key;
            }
        }

        return null;
    }

    private static BlockPos resolveContainerPos(Container container, ServerPlayer player) {
        if (container instanceof RandomizableContainer rc) {
            return rc.getBlockPos();
        }
        if (container.getClass().getName().contains("Lootr")) {
            try {
                Method getInfoMethod = container.getClass().getMethod("getInfo");
                Object info = getInfoMethod.invoke(container);
                if (info != null) {
                    Method getInfoPosMethod = info.getClass().getMethod("getInfoPos");
                    return (BlockPos) getInfoPosMethod.invoke(info);
                }
            } catch (Throwable ignored) {}
        }
        return player.blockPosition();
    }

    /* =========================================================================
     * Same-Odds Drop-Hit Optimization Classes & Engine
     * ========================================================================= */
    public static class ItemCandidate {
        public final Item item;
        public final double baselineChance;
        public final int baseCount;

        public ItemCandidate(Item item, double baselineChance, int baseCount) {
            this.item = item;
            this.baselineChance = baselineChance;
            this.baseCount = baseCount;
        }
    }

    public static class RarityTier {
        public final double chance;
        public final List<ItemCandidate> allItems;
        public final List<ItemCandidate> remainingPool;

        public RarityTier(double chance, List<ItemCandidate> items) {
            this.chance = chance;
            this.allItems = new ArrayList<>(items);
            this.remainingPool = new ArrayList<>(items);
        }

        /**
         * Drop-Hit Method:
         * If an item from this tier is rolled, randomly pick from remainingPool and remove it.
         * The next hit in this tier is guaranteed to drop the remaining item(s).
         * When remainingPool empties, it refills from allItems and repeats.
         */
        public ItemCandidate rollDrop(RandomSource random) {
            if (remainingPool.isEmpty()) {
                remainingPool.addAll(allItems);
            }
            int index = random.nextInt(remainingPool.size());
            return remainingPool.remove(index);
        }
    }

    public static List<RarityTier> buildTiers(List<ItemCandidate> candidates) {
        // Group candidates by rounded chance (4 decimal precision)
        Map<Long, List<ItemCandidate>> tierMap = new LinkedHashMap<>();
        for (ItemCandidate cand : candidates) {
            long key = Math.round(cand.baselineChance * 10000.0);
            tierMap.computeIfAbsent(key, k -> new ArrayList<>()).add(cand);
        }

        List<RarityTier> tiers = new ArrayList<>();
        for (Map.Entry<Long, List<ItemCandidate>> entry : tierMap.entrySet()) {
            double tierChance = entry.getKey() / 10000.0;
            tiers.add(new RarityTier(tierChance, entry.getValue()));
        }

        // Sort rarest first (lowest probability first)
        tiers.sort(Comparator.comparingDouble(t -> t.chance));
        return tiers;
    }

    public static ItemStack rollOverflowSlot(List<RarityTier> tiers, RandomSource random) {
        for (RarityTier tier : tiers) {
            double boostedChance = tier.chance + 0.15; // flat +15% boosted probability
            if (random.nextDouble() < boostedChance) {
                ItemCandidate chosen = tier.rollDrop(random);
                int count = Math.max(1, (int) Math.floor(chosen.baseCount * 1.15));
                return new ItemStack(chosen.item, count);
            }
        }
        return ItemStack.EMPTY;
    }

    @SuppressWarnings("unchecked")
    private static List<ItemCandidate> extractRareCandidates(LootTable table) {
        List<ItemCandidate> list = new ArrayList<>();
        if (table != null && lootTablePoolsField != null && lootPoolEntriesField != null) {
            try {
                List<LootPool> pools = (List<LootPool>) lootTablePoolsField.get(table);
                if (pools != null) {
                    for (LootPool pool : pools) {
                        List<LootPoolEntryContainer> entries = (List<LootPoolEntryContainer>) lootPoolEntriesField.get(pool);
                        if (entries == null || entries.isEmpty()) continue;

                        int totalWeight = 0;
                        for (LootPoolEntryContainer entry : entries) {
                            if (entry instanceof LootPoolSingletonContainer singleton && singletonWeightField != null) {
                                totalWeight += singletonWeightField.getInt(singleton);
                            }
                        }
                        if (totalWeight <= 0) totalWeight = 100;

                        for (LootPoolEntryContainer entry : entries) {
                            if (entry instanceof LootItem lootItem && lootItemHolderField != null && singletonWeightField != null) {
                                Holder<Item> holder = (Holder<Item>) lootItemHolderField.get(lootItem);
                                if (holder != null) {
                                    int weight = singletonWeightField.getInt(lootItem);
                                    double chance = (double) weight / (double) totalWeight;
                                    // Baseline drop rate < 25% qualifies as rare treasure
                                    if (chance < 0.25) {
                                        list.add(new ItemCandidate(holder.value(), chance, 1));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // Fallback treasures if table reflection was empty
        if (list.isEmpty()) {
            list.add(new ItemCandidate(Items.DIAMOND, 0.025, 2));
            list.add(new ItemCandidate(Items.EMERALD, 0.025, 2));
            list.add(new ItemCandidate(Items.GOLDEN_APPLE, 0.05, 1));
            list.add(new ItemCandidate(Items.ENCHANTED_GOLDEN_APPLE, 0.015, 1));
            list.add(new ItemCandidate(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 0.02, 1));
            list.add(new ItemCandidate(Items.ANCIENT_DEBRIS, 0.02, 1));
            list.add(new ItemCandidate(Items.DIAMOND_HORSE_ARMOR, 0.05, 1));
            list.add(new ItemCandidate(Items.IRON_INGOT, 0.125, 4));
            list.add(new ItemCandidate(Items.GOLD_INGOT, 0.125, 4));
            list.add(new ItemCandidate(Items.REDSTONE, 0.15, 6));
            list.add(new ItemCandidate(Items.LAPIS_LAZULI, 0.15, 6));
        }
        return list;
    }

    private static class ActiveChestAlignment {
        final UUID playerUUID;
        final Container container;
        final List<Integer> candidateSlots;
        final Map<Integer, ItemStack> finalItems;
        final BlockPos pos;
        int remainingTicks;

        ActiveChestAlignment(UUID playerUUID, Container container, List<Integer> candidateSlots, Map<Integer, ItemStack> finalItems, BlockPos pos, int ticks) {
            this.playerUUID = playerUUID;
            this.container = container;
            this.candidateSlots = candidateSlots;
            this.finalItems = finalItems;
            this.pos = pos;
            this.remainingTicks = ticks;
        }
    }
}
