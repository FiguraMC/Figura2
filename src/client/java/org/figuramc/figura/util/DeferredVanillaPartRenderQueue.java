package org.figuramc.figura.util;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.model.part.VanillaRootModelPart;
import org.figuramc.figura.model.renderers.FiguraRenderers;
import org.figuramc.figura.script_hooks.ScriptError;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;

/**
 * An optimized data structure for deferring vanilla model part drawing as necessitated by the mixins
 */
public class DeferredVanillaPartRenderQueue {

    private static final ArrayList<QueueEntry> ENTRIES = new ArrayList<>();
    private static int curIndex;

    public static void insert(Avatar<?> avatar, VanillaRootModelPart modelPart, Matrix4f posMatrix, Matrix3f normalMatrix, int light, int overlay) {
        if (curIndex >= ENTRIES.size())
            ENTRIES.add(new QueueEntry());
        QueueEntry entry = ENTRIES.get(curIndex++);
        entry.avatar = avatar;
        entry.modelPart = modelPart;
        entry.posMatrix.set(posMatrix);
        entry.normalMatrix.set(normalMatrix);
        entry.light = light;
        entry.overlay = overlay;
    }

    private static final FiguraTransformStack MATRIX_STACK = new FiguraTransformStack();
    public static void flush(MultiBufferSource bufferSource, float tickDelta) {
        for (int i = 0; i < curIndex; i++) {
            QueueEntry entry = ENTRIES.get(i);
            MATRIX_STACK.peekPosition().set(entry.posMatrix);
            MATRIX_STACK.peekNormal().set(entry.normalMatrix);

            // If the avatar is errored, this call will do nothing.
            entry.avatar.tryRenderModelPart(entry.modelPart, bufferSource, MATRIX_STACK, tickDelta, entry.light, entry.overlay);

            // No memory leaks
            entry.avatar = null;
            entry.modelPart = null;
        }
        curIndex = 0;
    }


    private static class QueueEntry {
        private Avatar<?> avatar;
        private VanillaRootModelPart modelPart;
        private final Matrix4f posMatrix = new Matrix4f();
        private final Matrix3f normalMatrix = new Matrix3f();
        private int light, overlay;
    }

}
