package net.minecraft.client.gui.screens;

import java.lang.invoke.MethodHandle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class MysticismSkillPickHook implements MenuScreens.ScreenConstructor<AbstractContainerMenu, Screen> {
    public static MethodHandle FACTORY;

    @Override
    public Screen create(AbstractContainerMenu menu, Inventory inventory, Component title) {
        try {
            return (Screen) FACTORY.invoke(menu, inventory, title);
        } catch (Throwable failure) {
            throw new RuntimeException(failure);
        }
    }
}
