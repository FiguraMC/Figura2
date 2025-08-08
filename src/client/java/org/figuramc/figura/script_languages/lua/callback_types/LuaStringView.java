package org.figuramc.figura.script_languages.lua.callback_types;

import org.figuramc.figura.script_hooks.callback.items.StringView;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaString;

public class LuaStringView extends StringView {

    // Backing string. Set to null on revoke.
    private LuaString string;

    public LuaStringView(LuaString backingString) {
        this.string = backingString; // Save the string
    }

    @Override
    public void revoke() {
        string = null;
        super.revoke();
    }

    @Override
    public int length() {
        if (isRevoked()) throw new UnsupportedOperationException("TODO error on revoked");
        return string.length();
    }

    @Override
    public String copy() {
        if (isRevoked()) throw new UnsupportedOperationException("TODO error on revoked");
        return string.toJavaStringNoAlloc();
    }
}
