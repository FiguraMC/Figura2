package org.figuramc.figura.script_languages.molang.compile;

import com.demonwav.mcdev.annotations.Translatable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.util.exception.FiguraException;

public class MolangCompileException extends FiguraException {

    // Provide the problem, the source code, and the region of source where it was wrong.
    public MolangCompileException(@Translatable String reason, String source, int start, int end, Object... args) {
        super(Component.translatable(
                "figura.error.script.molang.compile",
                Component.translatable(reason, args),
                createSourceHighlight(source, start, end - start)
        ));
    }

    private static final int TOTAL_LEN = 30;

    private static Component createSourceHighlight(String source, int start, int length) {

        if (length > TOTAL_LEN) length = TOTAL_LEN; // Truncate length if needed

        int totalPadding = TOTAL_LEN - length;
        int startPadding = totalPadding / 2;
        int endPadding = totalPadding - startPadding;

        int end = start + length;

        String pre = start - startPadding >= 0 ? source.substring(start - startPadding, start) : "";
        String mid = source.substring(start, end);
        String post = end + endPadding < source.length() ? source.substring(end, end + endPadding) : "";

        return Component.literal(pre)
                .append(Component.literal(mid).withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE))
                .append(post);
    }

}
