package org.figuramc.figura;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

// Helpful for testing with renderdoc, enable the boolean
public class FiguraModPreLaunch implements PreLaunchEntrypoint {

    private static final boolean ENABLE_RENDERDOC = true;

    @Override
    public void onPreLaunch() {
        if (ENABLE_RENDERDOC) System.loadLibrary("renderdoc");
    }
}
