package io.github.pok8.mysticismultimates.mixin;

import io.github.pok8.mysticismultimates.client.TimeSkillsClient;
import io.github.pok8.mysticismultimates.skill.TimeSkills;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    public int getMaxLocalRawBrightness(BlockPos pos) {
        Level self = (Level) (Object) this;
        Long customTime = TimeSkills.getEffectiveTimeForBlockPos(self, pos);
        if (customTime != null && customTime >= 0L) {
            int skyDarken = (customTime < 12000L) ? 0 : 11;
            return self.getMaxLocalRawBrightness(pos, skyDarken);
        }
        return self.getMaxLocalRawBrightness(pos, self.getSkyDarken());
    }

    @Inject(method = "isDay", at = @At("HEAD"), cancellable = true)
    private void mysticism$isDay(CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        if (self.isClientSide()) {
            Long clientOverride = TimeSkillsClient.getClientEffectiveTime();
            if (clientOverride != null && clientOverride >= 0L) {
                cir.setReturnValue(clientOverride < 12000L);
                return;
            }
            long time = self.getDayTime() % 24000L;
            if (time < 0) time += 24000L;
            cir.setReturnValue(time < 12000L);
            return;
        }
        Boolean override = TimeSkills.getContextIsDay(self);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "isNight", at = @At("HEAD"), cancellable = true)
    private void mysticism$isNight(CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        if (self.isClientSide()) {
            Long clientOverride = TimeSkillsClient.getClientEffectiveTime();
            if (clientOverride != null && clientOverride >= 0L) {
                cir.setReturnValue(clientOverride >= 12000L);
                return;
            }
            long time = self.getDayTime() % 24000L;
            if (time < 0) time += 24000L;
            cir.setReturnValue(time >= 12000L);
            return;
        }
        Boolean override = TimeSkills.getContextIsDay(self);
        if (override != null) {
            cir.setReturnValue(!override);
        }
    }

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void mysticism$getDayTime(CallbackInfoReturnable<Long> cir) {
        Level self = (Level) (Object) this;
        if (!self.isClientSide()) {
            Long override = TimeSkills.getContextDayTime(self);
            if (override != null) {
                cir.setReturnValue(override);
            }
        } else {
            Long clientOverride = TimeSkillsClient.getClientEffectiveTime();
            if (clientOverride != null && clientOverride >= 0L) {
                cir.setReturnValue(clientOverride);
            }
        }
    }

    @Inject(method = "getSkyDarken", at = @At("HEAD"), cancellable = true)
    private void mysticism$getSkyDarken(CallbackInfoReturnable<Integer> cir) {
        Level self = (Level) (Object) this;
        if (self.isClientSide()) {
            Long clientOverride = TimeSkillsClient.getClientEffectiveTime();
            if (clientOverride != null && clientOverride >= 0L) {
                cir.setReturnValue(clientOverride < 12000L ? 0 : 11);
                return;
            }
            long time = self.getDayTime() % 24000L;
            if (time < 0) time += 24000L;
            cir.setReturnValue(time < 12000L ? 0 : 11);
            return;
        }
        Boolean override = TimeSkills.getContextIsDay(self);
        if (override != null) {
            cir.setReturnValue(override ? 0 : 11);
        }
    }
}
