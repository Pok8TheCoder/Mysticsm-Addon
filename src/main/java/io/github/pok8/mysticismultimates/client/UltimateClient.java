package io.github.pok8.mysticismultimates.client;

import io.github.pok8.mysticismultimates.entity.SkillAnimationVariant;
import io.github.pok8.mysticismultimates.menu.SkillPickMenu;
import io.github.pok8.mysticismultimates.registry.UltimateEntities;
import io.github.pok8.mysticismultimates.registry.UltimateMenus;
import io.github.pok8.mysticismultimates.registry.UniqueShadowSkills;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class UltimateClient {
    private UltimateClient() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        try {
            Class<?> constructor = Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            CallSite site = LambdaMetafactory.metafactory(
                    lookup,
                    "create",
                    MethodType.methodType(constructor),
                    MethodType.methodType(Screen.class, AbstractContainerMenu.class, Inventory.class, Component.class),
                    lookup.findStatic(UltimateClient.class, "createSkillPick", MethodType.methodType(SkillPickScreen.class, SkillPickMenu.class, Inventory.class, Component.class)),
                    MethodType.methodType(SkillPickScreen.class, SkillPickMenu.class, Inventory.class, Component.class));
            RegisterMenuScreensEvent.class
                    .getMethod("register", MenuType.class, constructor)
                    .invoke(event, UltimateMenus.SKILL_PICK.get(), site.getTarget().invoke());
        } catch (Throwable failure) {
            throw new IllegalStateException("Could not register the skill pick screen", failure);
        }
    }

    public static SkillPickScreen createSkillPick(SkillPickMenu menu, Inventory inventory, Component title) {
        return new SkillPickScreen(menu, inventory, title);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (SkillAnimationVariant variant : SkillAnimationVariant.values()) {
            event.registerEntityRenderer(UltimateEntities.type(variant), context -> new GeoEntityRenderer<>(context, new SkillAnimationModel()));
        }
        event.registerEntityRenderer(UniqueShadowSkills.SHADOW_SOUL.get(), ShadowMobRenderer::new);
        event.registerEntityRenderer(UniqueShadowSkills.SHADOW_SUBORDINATE.get(), context -> new GeoEntityRenderer<>(context, new ShadowSoldierModel()));
    }
}
