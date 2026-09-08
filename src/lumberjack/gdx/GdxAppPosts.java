package lumberjack.gdx;

import com.badlogic.gdx.Gdx;

/**
 * Posts runnables from background threads onto the LibGDX render thread.
 */
public final class GdxAppPosts {

    private GdxAppPosts() {
    }

    public static void post(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (Gdx.app == null) {
            runnable.run();
            return;
        }
        Gdx.app.postRunnable(runnable);
    }
}
