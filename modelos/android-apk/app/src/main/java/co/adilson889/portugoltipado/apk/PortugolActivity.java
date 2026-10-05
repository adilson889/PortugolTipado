package co.adilson889.portugoltipado.apk;

import org.libsdl.app.SDLActivity;

/** Arranca o programa PortugolTipado compilado em libmain.so (via SDL_main). */

public class PortugolActivity extends SDLActivity {
    @Override
    protected String[] getLibraries() {
        return new String[] {
            "SDL2",
            "SDL2_ttf",
            "main"
        };
    }
}