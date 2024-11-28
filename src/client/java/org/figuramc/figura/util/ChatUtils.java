package org.figuramc.figura.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

/**
 * Various helpers for printing to chat
 */
public class ChatUtils {

    /**
     * Report an error to chat, in the format:
     * "<red>primaryMessage</red> <aqua>[Hover for reason]</aqua>"
     * If no reason, prints
     * "<red>primaryMessage</red> <aqua>[No reason given]</aqua>"
     */
    public static void reportErrorWithReason(MutableComponent primaryMessage, @Nullable Throwable reason, boolean forceExpand) {
        MutableComponent msg = primaryMessage.withStyle(ChatFormatting.RED);
        if (reason == null)
            msg = msg.append(" ").append(Component.literal("[No reason given]").withStyle(ChatFormatting.AQUA)); // TODO translatable
        else
            msg = msg.append(" ").append(Component.literal("[Hover for reason]") // TODO translatable
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.AQUA)
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal(forceExpand || reason.getMessage() == null ? reason.toString() : reason.getMessage()).withStyle(ChatFormatting.RED)
                            ))
                    ));
        Minecraft.getInstance().gui.getChat().addMessage(msg);
    }

    public static void unexpectedError(Throwable reason) {
        reportErrorWithReason(Component.literal("Unexpected internal error in Figura!"), reason, true);
    }

}
