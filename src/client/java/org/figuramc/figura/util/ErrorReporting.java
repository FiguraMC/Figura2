package org.figuramc.figura.util;

import com.demonwav.mcdev.annotations.Translatable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.data.AvatarImportingException;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.util.exception.FiguraException;

/**
 * Class with helpers for reporting errors at various stages in Figura lifecycle.
 * Error reporters are statically typed in order to give good messages and formatting.
 *
 * Note: This class and related error-handling code could probably be improved A LOT...
 * this is better than the original version of the error handling code, but it's still
 * very clunky to deal with.
 */
public class ErrorReporting {

    private static void helper(FiguraException ex, String javaString, @Translatable String translateString, Object... translateArgs) {
        MutableComponent hoverMessage = ex.getComponent().withStyle(ChatFormatting.RED);
        if (ex.showCause && ex.getCause() != null) {
            // If we want to show the cause, add more info to the hover message caused-by.
            if (!ex.getComponent().getString().isEmpty()) hoverMessage = hoverMessage.append("\n"); // Add a newline only if there was text at all
            hoverMessage = hoverMessage.append(Component.translatable("figura.error.caused_by").append("\n")
                    .withStyle(ChatFormatting.AQUA)
                    .append((switch (ex.getCause()) {
                            case FiguraException fig -> fig.getComponent();
                            case Throwable t when t.getMessage() != null -> Component.literal(t.getMessage());
                            default -> Component.translatable("figura.error.check_stacktrace");
                        }).withStyle(ChatFormatting.RED)));
        }
        Component result = Component.translatable(translateString, translateArgs).append(" ")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal("[").append(Component.translatable("figura.error.hover_for_reason")).append(Component.literal("]"))
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent.ShowText(hoverMessage))
                        ));
        // Report to chat and to the console
        Minecraft.getInstance().gui.getChat().addMessage(result);
        FiguraMod.LOGGER.error(javaString, ex);
    }

    public static void avatarImporting(AvatarImportingException ex) { helper(ex, "Error during avatar importing", "figura.error.importing"); }
    public static void avatarLoading(AvatarLoadingException ex) { helper(ex, "Error during avatar loading", "figura.error.loading"); }
    public static void avatarRuntimeError(AvatarError ex) { helper(ex, "Error in running avatar", ex.translationKey, ex.args); }

    public static void unexpectedError(Throwable unexpectedError) {
        Component c = Component.translatable("figura.error.internal.unexpected").withStyle(ChatFormatting.RED);
        Minecraft.getInstance().gui.getChat().addMessage(c);
        FiguraMod.LOGGER.error("INTERNAL FIGURA ERROR: PLEASE REPORT TO DEVS!", unexpectedError);
    }

}
