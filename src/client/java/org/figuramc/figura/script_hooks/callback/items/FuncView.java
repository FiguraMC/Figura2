package org.figuramc.figura.script_hooks.callback.items;

public abstract non-sealed class FuncView<I extends CallbackItem, O extends CallbackItem> implements CallbackItem {

    private boolean isRevoked = false;

    public void revoke() { isRevoked = true; }
    public boolean isRevoked() { return isRevoked; }

    public abstract O invoke(I args); // Call the function, or error if revoked.
}
