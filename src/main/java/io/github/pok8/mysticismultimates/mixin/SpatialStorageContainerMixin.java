package io.github.pok8.mysticismultimates.mixin;

import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tensura spatial storage can hold stacks above 999 in memory, but {@link ItemStack#save} only allows 1..999.
 * Persist the real count on the slot compound and write the stack with count 1.
 */
@Mixin(value = SpatialStorageContainer.class, remap = false)
public abstract class SpatialStorageContainerMixin {
    @Unique
    private static final String REAL_COUNT = "mysticism_ultimates:RealCount";

    @Unique
    private CompoundTag mysticism$loadEntry;

    @Unique
    private boolean mysticism$loadingTag;

    @Redirect(
            method = "createTag",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;save(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;"
            ),
            remap = true
    )
    private Tag mysticism_saveLargeStack(ItemStack stack, HolderLookup.Provider provider, Tag prefix) {
        if (stack.getCount() <= 999) {
            return stack.save(provider, prefix);
        }
        int count = stack.getCount();
        ItemStack safe = stack.copy();
        safe.setCount(1);
        Tag saved = safe.save(provider, prefix);
        if (prefix instanceof CompoundTag entry) {
            entry.putInt(REAL_COUNT, count);
        }
        return saved;
    }

    @Inject(method = "fromTag", at = @At("HEAD"))
    private void mysticism_beginLoad(ListTag list, HolderLookup.Provider provider, CallbackInfo ci) {
        mysticism$loadingTag = true;
        mysticism$loadEntry = null;
    }

    @Inject(method = "fromTag", at = @At("RETURN"))
    private void mysticism_endLoad(CallbackInfo ci) {
        mysticism$loadingTag = false;
        mysticism$loadEntry = null;
    }

    @Redirect(
            method = "fromTag",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/ListTag;getCompound(I)Lnet/minecraft/nbt/CompoundTag;"
            ),
            remap = true
    )
    private CompoundTag mysticism_captureEntry(ListTag list, int index) {
        CompoundTag entry = list.getCompound(index);
        if (mysticism$loadingTag) {
            mysticism$loadEntry = entry;
        }
        return entry;
    }

    @Inject(method = "setItem", at = @At("HEAD"), remap = false)
    private void mysticism_restoreLargeStack(int slot, ItemStack stack, CallbackInfo ci) {
        if (!mysticism$loadingTag) {
            return;
        }
        if (mysticism$loadEntry != null && mysticism$loadEntry.contains(REAL_COUNT) && !stack.isEmpty()) {
            stack.setCount(mysticism$loadEntry.getInt(REAL_COUNT));
        }
        mysticism$loadEntry = null;
    }
}
