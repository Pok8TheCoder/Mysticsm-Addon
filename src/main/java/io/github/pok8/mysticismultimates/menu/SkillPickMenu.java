package io.github.pok8.mysticismultimates.menu;

import dev.architectury.registry.menu.MenuRegistry;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.pok8.mysticismultimates.registry.UltimateMenus;
import io.github.pok8.mysticismultimates.skill.BeelzebubSkill;
import io.github.pok8.mysticismultimates.skill.HiddenRulerHandler;
import io.github.pok8.mysticismultimates.skill.VicielSkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SkillPickMenu extends AbstractContainerMenu {
    private final ManasSkill ability;
    private final int mode;
    private final UUID target;
    private final List<ManasSkill> skills;

    public SkillPickMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId,
                SkillAPI.getSkillRegistry().get(buffer.readResourceLocation()),
                buffer.readVarInt(),
                buffer.readUUID(),
                buffer.readList(FriendlyByteBuf::readResourceLocation));
    }

    public SkillPickMenu(int containerId, ManasSkill ability, int mode, UUID target, List<ResourceLocation> skillIds) {
        super(UltimateMenus.SKILL_PICK.get(), containerId);
        this.ability = ability;
        this.mode = mode;
        this.target = target;
        List<ManasSkill> resolved = new ArrayList<>();
        for (ResourceLocation id : skillIds) {
            ManasSkill skill = id == null ? null : SkillAPI.getSkillRegistry().get(id);
            if (skill != null) {
                resolved.add(skill);
            }
        }
        resolved.sort((left, right) -> left.getName().getString().compareToIgnoreCase(right.getName().getString()));
        this.skills = List.copyOf(resolved);
    }

    public static void open(ServerPlayer player, ManasSkillInstance instance, int mode, UUID target, List<ResourceLocation> skillIds) {
        UUID marked = target == null ? new UUID(0L, 0L) : target;
        List<ResourceLocation> ids = List.copyOf(skillIds);
        MenuConstructor constructor = (containerId, inventory, ignored) -> new SkillPickMenu(containerId, instance.getSkill(), mode, marked, ids);
        MenuProvider provider = new net.minecraft.world.SimpleMenuProvider(constructor, instance.getSkill().getName());
        MenuRegistry.openExtendedMenu(player, provider, buffer -> {
            buffer.writeResourceLocation(instance.getSkillId());
            buffer.writeVarInt(mode);
            buffer.writeUUID(marked);
            buffer.writeCollection(ids, FriendlyByteBuf::writeResourceLocation);
        });
    }

    public ManasSkill getAbility() {
        return ability;
    }

    public List<ManasSkill> getSkills() {
        return skills;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Override
    public boolean clickMenuButton(Player player, int index) {
        if (index < 0 || index >= skills.size() || !(player instanceof ServerPlayer serverPlayer) || ability == null) {
            return false;
        }
        SkillStorage storage = SkillAPI.getSkillsFrom(serverPlayer);
        if (storage == null || ability.getRegistryName() == null) {
            return false;
        }
        ManasSkillInstance instance = storage.getSkill(ability.getRegistryName()).orElse(null);
        if (instance == null || instance.onCoolDown(mode)) {
            return false;
        }
        ManasSkill chosen = skills.get(index);
        boolean granted = switch (ability) {
            case BeelzebubSkill beelzebub -> beelzebub.grantPicked(serverPlayer, instance, mode, target, chosen);
            case VicielSkill viciel -> viciel.grantPicked(serverPlayer, instance, mode, target, chosen);
            default -> {
                if (ability.getRegistryName() != null && "hidden_ruler".equals(ability.getRegistryName().getPath())) {
                    yield HiddenRulerHandler.grantPicked(serverPlayer, instance, mode, target, chosen);
                }
                yield false;
            }
        };
        if (granted) {
            serverPlayer.closeContainer();
        }
        return granted;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
