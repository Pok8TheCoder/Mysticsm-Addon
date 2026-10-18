package io.github.pok8.mysticismultimates.mixin;

import io.github.manasmods.tensura.entity.human.DwarfEntity;
import io.github.pok8.mysticismultimates.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DwarfEntity.class, remap = false)
public abstract class DwarfEntityMixin {

    @Inject(method = "wantsToTrade", at = @At("HEAD"), cancellable = true)
    private void mysticism$wantsToTrade(CallbackInfoReturnable<Boolean> cir) {
        DwarfEntity dwarf = (DwarfEntity) (Object) this;
        if (dwarf.level() instanceof ServerLevel serverLevel) {
            LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(dwarf.chunkPosition().x, dwarf.chunkPosition().z);
            if (chunk != null) {
                long customTime = ModAttachments.getChunkTime(chunk);
                if (customTime >= 0L && customTime < 12000L) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "startSleeping", at = @At("HEAD"), cancellable = true)
    private void mysticism$onStartSleeping(BlockPos pos, CallbackInfo ci) {
        DwarfEntity dwarf = (DwarfEntity) (Object) this;
        if (dwarf.level() instanceof ServerLevel serverLevel) {
            LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(dwarf.chunkPosition().x, dwarf.chunkPosition().z);
            if (chunk != null) {
                long customTime = ModAttachments.getChunkTime(chunk);
                if (customTime >= 0L && customTime < 12000L) {
                    ci.cancel();
                }
            }
        }
    }
}
