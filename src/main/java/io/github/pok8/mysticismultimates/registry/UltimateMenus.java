package io.github.pok8.mysticismultimates.registry;

import io.github.pok8.mysticismultimates.MysticismUltimates;
import io.github.pok8.mysticismultimates.menu.SkillPickMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import dev.architectury.registry.menu.MenuRegistry;

public final class UltimateMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MysticismUltimates.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<SkillPickMenu>> SKILL_PICK = MENUS.register(
            "skill_pick",
            () -> MenuRegistry.ofExtended(SkillPickMenu::new));

    private UltimateMenus() {
    }
}
