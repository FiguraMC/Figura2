package org.figuramc.figura.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import org.figuramc.figura.FiguraMod;
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
    public static void reportErrorWithReason(MutableComponent primaryMessage, @Nullable Throwable reason) {
        MutableComponent msg = primaryMessage.withStyle(ChatFormatting.RED);
        Throwable firstReason = reason;
        if (reason == null)
            msg = msg.append(" ").append(Component.literal("[No reason given]").withStyle(ChatFormatting.AQUA)); // TODO translatable
        else {
            String reasonMsg = reason.getMessage() != null ? reason.getMessage() : "[No reason given]";
            MutableComponent hoverMessage = Component.literal(reasonMsg.replace("\t", "  ")).withStyle(ChatFormatting.RED);
            reason = reason.getCause();
            while (reason != null) {
                reasonMsg = reason.getMessage() != null ? reason.getMessage() : "[No reason given]";
                hoverMessage.append(
                        Component.literal("\nCaused by:\n").withStyle(ChatFormatting.AQUA).append(
                        Component.literal(reasonMsg.replace("\t", "  ")).withStyle(ChatFormatting.RED)
                ));
                reason = reason.getCause();
            }

            msg = msg.append(" ").append(Component.literal("[Hover for reason]") // TODO translatable
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.AQUA)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage))
                    ));
        }
        Minecraft.getInstance().gui.getChat().addMessage(msg);
        // Also report to logger
        FiguraMod.LOGGER.error(primaryMessage.tryCollapseToString(), firstReason);
    }

    public static void unexpectedError(String during, Throwable reason) {
        reportErrorWithReason(Component.literal("Unexpected internal error during " + during), reason);
    }

}
