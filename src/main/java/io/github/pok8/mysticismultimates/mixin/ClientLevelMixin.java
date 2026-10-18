package io.github.pok8.mysticismultimates.mixin;

import io.github.pok8.mysticismultimates.client.TimeSkillsClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "setDayTime", at = @At("HEAD"), cancellable = true)
    private void mysticism$onSetDayTimeHead(long dayTime, CallbackInfo ci) {
        ClientLevel self = (ClientLevel) (Object) this;
        Long clientOverride = TimeSkillsClient.getClientEffectiveTime();
        if (clientOverride != null && clientOverride >= 0L) {
            // When custom daylight/nightlight is active:
            // Block any packet that attempts to set positive (vanilla daylight cycle) time or a conflicting time!
            if (dayTime != -clientOverride) {
                ci.cancel();
                self.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                self.getLevelData().setDayTime(clientOverride);
                self.updateSkyBrightness();
            } else {
                TimeSkillsClient.setClientLockedTime(clientOverride);
            }
        } else {
            // When custom time is inactive, if normal positive time is received, unlock client time
            if (dayTime >= 0L) {
                TimeSkillsClient.setClientLockedTime(null);
            }
        }
    }

    @Inject(method = "setDayTime", at = @At("RETURN"))
    private void mysticism$onSetDayTimeReturn(long dayTime, CallbackInfo ci) {
        ((ClientLevel) (Object) this).updateSkyBrightness();
    }
}
