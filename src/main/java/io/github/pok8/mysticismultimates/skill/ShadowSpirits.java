package io.github.pok8.mysticismultimates.skill;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.ability.magic.spiritual.SpiritualMagic;
import io.github.manasmods.tensura.event.TensuraSpiritEvents;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.spirit.ISpiritWielder;
import io.github.pok8.mysticismultimates.UltimateGamerules;
import io.github.pok8.mysticismultimates.registry.UniqueShadowSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ShadowSpirits {
    private ShadowSpirits() {
    }

    public static void register() {
        TensuraSpiritEvents.SPIRIT_UPDATE.register(ShadowSpirits::onSpiritUpdate);
    }

    public static boolean hasGreaterDarkness(LivingEntity entity) {
        ISpiritWielder spirits = TensuraStorages.getSpiritFrom(entity);
        if (spirits == null) {
            return false;
        }
        return spirits.getSpiritLevelId(Element.DARKNESS) >= SpiritualMagic.SpiritLevel.GREATER.getId();
    }

    private static EventResult onSpiritUpdate(LivingEntity entity, Changeable elementValue, Changeable levelValue) {
        if (!(entity instanceof ServerPlayer player) || !(elementValue.get() instanceof Element element) || element != Element.DARKNESS) {
            return EventResult.pass();
        }
        if (!(levelValue.get() instanceof SpiritualMagic.SpiritLevel level)) {
            return EventResult.pass();
        }
        ISpiritWielder spirits = TensuraStorages.getSpiritFrom(player);
        if (spirits == null || spirits.getSpiritLevelId(Element.LIGHT) <= 0 || level.getId() <= spirits.getSpiritLevelId(Element.DARKNESS)) {
            return EventResult.pass();
        }
        if (player.serverLevel().getGameRules().getBoolean(UltimateGamerules.PLUNDERER_DARKNESS_SPIRIT) && hasPlunderer(player)) {
            return EventResult.pass();
        }
        player.displayClientMessage(Component.translatable("mysticism_ultimates.spirit.darkness_rejected").withStyle(ChatFormatting.RED), false);
        return EventResult.interruptFalse();
    }

    private static boolean hasPlunderer(ServerPlayer player) {
        ManasSkill plunderer = UniqueShadowSkills.PLUNDERER == null ? null : UniqueShadowSkills.PLUNDERER.get();
        return plunderer != null && SkillUtils.hasSkill(player, plunderer);
    }
}
