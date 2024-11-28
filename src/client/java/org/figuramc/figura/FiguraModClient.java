package org.figuramc.figura;

import com.mojang.blaze3d.systems.RenderSystem;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.model.optimized.OptimizedRendering;
import net.fabricmc.api.ClientModInitializer;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

public class FiguraModClient implements ClientModInitializer {

    // The stack of Avatars rendering. This is peeked by the vanilla ModelPart rendering mixins.
    // Push null if there's no Avatar in the circumstance.
    public static final Stack<@Nullable Avatar<?>> AVATAR_RENDERING_STACK = new Stack<>();
    // Cursed deferred rendering queue, required because of vanilla part mixin business
//    public static final Queue<Consumer<MultiBufferSource>> DEFERRED_AVATAR_RENDERING_QUEUE = new ArrayDeque<>();

    public static boolean LOADED_TEST_AVATAR = false;

    @Override
    public void onInitializeClient() {
        FiguraMod.LOGGER.info("Hello from Figura, client side!");
        ConfigManager.init();

        // Initialize OptimizedRendering on render thread later
        // Initializing it right now will crash the game with an EXCEPTION_ACCESS_VIOLATION so be careful
        RenderSystem.recordRenderCall(OptimizedRendering::init);
    }



}
