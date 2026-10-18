package io.github.pok8.mysticismultimates.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class GhostBuffers implements MultiBufferSource {
    private static final Method STATE = method("net.minecraft.client.renderer.RenderType$CompositeRenderType", "state");
    private static final Field TEXTURE = field("net.minecraft.client.renderer.RenderType$CompositeState", "textureState");
    private static final Method CUTOUT = method("net.minecraft.client.renderer.RenderStateShard$TextureStateShard", "cutoutTexture");
    private final MultiBufferSource inner;
    private final int alpha;
    private final Map<RenderType, RenderType> mapped = new HashMap<>();

    GhostBuffers(MultiBufferSource inner, int alpha) {
        this.inner = inner;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        RenderType translucent = mapped.computeIfAbsent(renderType, GhostBuffers::translucent);
        VertexConsumer buffer = inner.getBuffer(translucent == null ? renderType : translucent);
        return translucent == null ? buffer : new AlphaConsumer(buffer, alpha);
    }

    private static RenderType translucent(RenderType type) {
        ResourceLocation texture = texture(type);
        if (texture == null) {
            return null;
        }
        String name = String.valueOf(type);
        if (name.contains("eyes") || name.contains("emissive")) {
            return RenderType.entityTranslucentEmissive(texture);
        }
        return RenderType.entityTranslucent(texture);
    }

    @SuppressWarnings("unchecked")
    private static ResourceLocation texture(RenderType type) {
        if (STATE == null || TEXTURE == null || CUTOUT == null) {
            return null;
        }
        try {
            Object state = STATE.invoke(type);
            Object textureState = TEXTURE.get(state);
            Object cutout = CUTOUT.invoke(textureState);
            if (cutout instanceof Optional<?> optional && optional.orElse(null) instanceof ResourceLocation location) {
                return location;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private static Method method(String className, String name) {
        try {
            Method method = Class.forName(className).getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Field field(String className, String name) {
        try {
            Field field = Class.forName(className).getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static final class AlphaConsumer implements VertexConsumer {
        private final VertexConsumer inner;
        private final int alpha;

        private AlphaConsumer(VertexConsumer inner, int alpha) {
            this.inner = inner;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return inner.addVertex(x, y, z);
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return inner.setColor(red, green, blue, (alpha * this.alpha) / 255);
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return inner.setUv(u, v);
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return inner.setUv1(u, v);
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return inner.setUv2(u, v);
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return inner.setNormal(x, y, z);
        }
    }
}
