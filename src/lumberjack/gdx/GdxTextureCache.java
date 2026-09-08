package lumberjack.gdx;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;

/**
 * Loads and caches LibGDX textures from the synced {@code assets/} folder.
 */
public final class GdxTextureCache {

    private final Map<String, Texture> textures = new HashMap<>();
    private Texture whitePixel;

    public Texture get(String assetPath) {
        return textures.computeIfAbsent(assetPath, this::loadTexture);
    }

    public Texture getOptional(String assetPath) {
        FileHandle handle = Gdx.files.internal(assetPath);
        if (!handle.exists()) {
            return null;
        }
        return get(assetPath);
    }

    /** 1×1 white texture for solid-color placeholder draws. */
    public Texture getWhitePixel() {
        if (whitePixel == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            whitePixel = new Texture(pixmap);
            pixmap.dispose();
            whitePixel.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        }
        return whitePixel;
    }

    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
        if (whitePixel != null) {
            whitePixel.dispose();
            whitePixel = null;
        }
    }

    private Texture loadTexture(String assetPath) {
        Texture texture = new Texture(Gdx.files.internal(assetPath));
        texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        return texture;
    }
}
