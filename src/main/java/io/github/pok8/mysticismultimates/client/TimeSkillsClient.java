package io.github.pok8.mysticismultimates.client;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.pok8.mysticismultimates.registry.CustomSkills;
import io.github.pok8.mysticismultimates.skill.TimeSkills;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class TimeSkillsClient {
    private static volatile Long clientLockedTime = null;

    private TimeSkillsClient() {}

    public static Long getClientLockedTime() {
        return clientLockedTime;
    }

    public static void setClientLockedTime(Long time) {
        clientLockedTime = time;
    }

    public static Long getClientEffectiveTime() {
        try {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null) {
                return clientLockedTime;
            }

            // 1. First check if the local client player has True Daylight toggled
            if (CustomSkills.TRUE_DAYLIGHT != null && CustomSkills.TRUE_DAYLIGHT.get() != null) {
                Optional<ManasSkillInstance> daylight = SkillAPI.getSkillsFrom(player).getSkill(CustomSkills.TRUE_DAYLIGHT.get());
                if (daylight.isPresent() && daylight.get().isToggled()) {
                    clientLockedTime = TimeSkills.DAY_TIME;
                    return TimeSkills.DAY_TIME;
                }
            }

            // 2. Check if the local client player has True Nightlight toggled
            if (CustomSkills.TRUE_NIGHTLIGHT != null && CustomSkills.TRUE_NIGHTLIGHT.get() != null) {
                Optional<ManasSkillInstance> nightlight = SkillAPI.getSkillsFrom(player).getSkill(CustomSkills.TRUE_NIGHTLIGHT.get());
                if (nightlight.isPresent() && nightlight.get().isToggled()) {
                    clientLockedTime = TimeSkills.NIGHT_TIME;
                    return TimeSkills.NIGHT_TIME;
                }
            }

            // 3. In singleplayer / local server, check active chunk fields
            ChunkPos pos = player.chunkPosition();
            ResourceKey<Level> dim = mc.level.dimension();
            for (TimeSkills.ChunkTimeField field : TimeSkills.getActiveFields().values()) {
                if (field.dimension.equals(dim) && field.activeChunks.contains(pos)) {
                    clientLockedTime = field.customTime;
                    return field.customTime;
                }
            }

            // If in singleplayer and player is neither toggled nor in active fields, clear the lock
            if (mc.hasSingleplayerServer()) {
                clientLockedTime = null;
                return null;
            }

            // In multiplayer, return the time locked by server packets if present
            return clientLockedTime;
        } catch (Throwable ignored) {
            return clientLockedTime;
        }
    }
}
