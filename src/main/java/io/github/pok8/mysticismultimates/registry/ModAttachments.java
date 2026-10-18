package io.github.pok8.mysticismultimates.registry;

import com.mojang.serialization.Codec;
import io.github.pok8.mysticismultimates.MysticismUltimates;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MysticismUltimates.MOD_ID);

    // -1L means "use normal world time". Any value >= 0 locks the chunk to that tick (e.g., 6000L = noon, 18000L = midnight)
    public static final Supplier<AttachmentType<Long>> CHUNK_TIME = ATTACHMENTS.register(
            "chunk_time", () -> AttachmentType.builder(() -> -1L)
                    .serialize(Codec.LONG)
                    .build()
    );

    private ModAttachments() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    public static long getChunkTime(LevelChunk chunk) {
        if (chunk instanceof IAttachmentHolder holder) {
            return holder.getData(CHUNK_TIME);
        }
        return -1L;
    }

    public static void setChunkTime(LevelChunk chunk, long time) {
        if (chunk instanceof IAttachmentHolder holder) {
            holder.setData(CHUNK_TIME, time);
        }
    }
}
