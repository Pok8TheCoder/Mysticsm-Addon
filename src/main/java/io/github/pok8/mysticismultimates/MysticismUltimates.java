package io.github.pok8.mysticismultimates;

import io.github.pok8.mysticismultimates.client.UltimateClient;
import io.github.pok8.mysticismultimates.registry.ShadowEffects;
import io.github.pok8.mysticismultimates.registry.UltimateEntities;
import io.github.pok8.mysticismultimates.registry.UniqueShadowSkills;
import io.github.pok8.mysticismultimates.registry.UltimateMenus;
import io.github.pok8.mysticismultimates.registry.UltimateSkills;
import io.github.pok8.mysticismultimates.skill.BeelzebubSkill;
import io.github.pok8.mysticismultimates.skill.LavernaSkill;
import io.github.pok8.mysticismultimates.skill.ShadowArmy;
import io.github.pok8.mysticismultimates.skill.ShadowSpirits;
import io.github.pok8.mysticismultimates.skill.VicielSkill;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MysticismUltimates.MOD_ID)
public final class MysticismUltimates {
    public static final String MOD_ID = "mysticism_ultimates";

    public MysticismUltimates(IEventBus modBus) {
        UltimateGamerules.bootstrap();
        UltimateSlotStorage.init();
        ShadowEffects.EFFECTS.register(modBus);
        UniqueShadowSkills.registerEntities();
        UltimateEntities.ENTITIES.register(modBus);
        UltimateMenus.MENUS.register(modBus);
        modBus.addListener(UltimateEntities::registerAttributes);
        UltimateSkills.register();
        UniqueShadowSkills.registerSkills();
        io.github.pok8.mysticismultimates.registry.CustomSkills.registerSkills();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(UltimateClient::registerRenderers);
            modBus.addListener(UltimateClient::registerScreens);
        }
        NeoForge.EVENT_BUS.addListener(UltimateEvolution::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(LavernaSkill::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(UltimateCommands::register);
        NeoForge.EVENT_BUS.addListener(ShadowArmy::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(ShadowArmy::onEntityJoinLevel);
        io.github.manasmods.tensura.event.TensuraEntityEvents.ADD_FRESH.register(ShadowArmy::onTensuraAddFresh);
        io.github.pok8.mysticismultimates.registry.ModAttachments.register(modBus);
        NeoForge.EVENT_BUS.addListener(io.github.pok8.mysticismultimates.skill.TimeSkills::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(io.github.pok8.mysticismultimates.skill.TimeSkills::onEntityTickPre);
        NeoForge.EVENT_BUS.addListener(io.github.pok8.mysticismultimates.skill.TimeSkills::onEntityTickPost);
        NeoForge.EVENT_BUS.addListener(io.github.pok8.mysticismultimates.skill.TimeSkills::onEnteringChunk);
        NeoForge.EVENT_BUS.addListener(io.github.pok8.mysticismultimates.skill.TimeSkills::onCanPlayerSleep);
        NeoForge.EVENT_BUS.addListener(io.github.pok8.mysticismultimates.skill.TimeSkills::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(io.github.pok8.mysticismultimates.skill.TimeSkills::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(io.github.pok8.mysticismultimates.skill.TimeSkills::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(io.github.pok8.mysticismultimates.skill.TimeSkills::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(io.github.pok8.mysticismultimates.skill.TimeSkills::onPlayerChangeDimension);
        ShadowSpirits.register();
        BeelzebubSkill.register();
        VicielSkill.register();
        io.github.pok8.mysticismultimates.skill.HadesSkill.register();
        io.github.pok8.mysticismultimates.skill.UrielSkill.register();
        NeoForge.EVENT_BUS.addListener(MysticismUltimates::onServerStarting);
        injectStartingSkills();
    }

    public static void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        injectStartingSkills();
    }

    public static void injectStartingSkills() {
        try {
            if (io.github.manasmods.tensura.menu.ReincarnationMenu.CONFIG != null
                    && io.github.manasmods.tensura.menu.ReincarnationMenu.CONFIG.Skills != null) {
                java.util.List<String> starting = new java.util.ArrayList<>(
                        io.github.manasmods.tensura.menu.ReincarnationMenu.CONFIG.Skills.startingSkills);
                java.util.List<String> second = new java.util.ArrayList<>(
                        io.github.manasmods.tensura.menu.ReincarnationMenu.CONFIG.Skills.secondSkills);
                java.util.List<String> toAdd = java.util.List.of(
                        "mysticism_ultimates:plunderer",
                        "mysticism_ultimates:vainglory",
                        "mysticism_ultimates:otherworldly"
                );
                boolean changedStarting = false;
                boolean changedSecond = false;
                for (String s : toAdd) {
                    if (!starting.contains(s)) {
                        starting.add(s);
                        changedStarting = true;
                    }
                    if (!second.contains(s)) {
                        second.add(s);
                        changedSecond = true;
                    }
                }
                if (changedStarting) {
                    io.github.manasmods.tensura.menu.ReincarnationMenu.CONFIG.Skills.startingSkills = starting;
                }
                if (changedSecond) {
                    io.github.manasmods.tensura.menu.ReincarnationMenu.CONFIG.Skills.secondSkills = second;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
