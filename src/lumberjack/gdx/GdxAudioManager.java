package lumberjack.gdx;

import java.util.EnumMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

import lumberjack.audio.MusicTrack;

/**
 * Streams music and caches short SFX. Music is loaded on demand and kept while
 * needed; prefer {@link Music} for songs and {@link Sound} for short clips.
 *
 * <p>Files live under {@code resources/audio/music/} and {@code resources/audio/sfx/}.
 */
public final class GdxAudioManager {

    private final Map<MusicTrack, Music> musicByTrack = new EnumMap<>(MusicTrack.class);
    private final Map<String, Sound> soundsByPath = new java.util.HashMap<>();

    private MusicTrack currentTrack;
    private float musicVolume = 0.7f;
    private float soundVolume = 0.8f;

    /** Play a track, looping. No-ops if that track is already playing. */
    public void playMusic(MusicTrack track, boolean loop) {
        if (track == null) {
            return;
        }
        if (currentTrack == track) {
            Music current = musicByTrack.get(track);
            if (current != null && current.isPlaying()) {
                return;
            }
        }
        stopMusic();
        Music music = musicByTrack.computeIfAbsent(track, this::loadMusic);
        if (music == null) {
            return;
        }
        music.setLooping(loop);
        music.setVolume(musicVolume);
        music.play();
        currentTrack = track;
    }

    public void stopMusic() {
        if (currentTrack == null) {
            return;
        }
        Music music = musicByTrack.get(currentTrack);
        if (music != null) {
            music.stop();
        }
        currentTrack = null;
    }

    public void pauseMusic() {
        Music music = currentMusic();
        if (music != null && music.isPlaying()) {
            music.pause();
        }
    }

    public void resumeMusic() {
        Music music = currentMusic();
        if (music != null) {
            music.play();
        }
    }

    public MusicTrack getCurrentTrack() {
        return currentTrack;
    }

    public void setMusicVolume(float volume) {
        musicVolume = clamp(volume);
        Music music = currentMusic();
        if (music != null) {
            music.setVolume(musicVolume);
        }
    }

    public void setSoundVolume(float volume) {
        soundVolume = clamp(volume);
    }

    /** Play a short SFX from {@code audio/sfx/...}. Safe if the file is missing. */
    public void playSound(String assetPath) {
        if (assetPath == null || assetPath.isBlank()) {
            return;
        }
        Sound sound = soundsByPath.computeIfAbsent(assetPath, this::loadSound);
        if (sound != null) {
            sound.play(soundVolume);
        }
    }

    public void dispose() {
        stopMusic();
        for (Music music : musicByTrack.values()) {
            if (music != null) {
                music.dispose();
            }
        }
        musicByTrack.clear();
        for (Sound sound : soundsByPath.values()) {
            if (sound != null) {
                sound.dispose();
            }
        }
        soundsByPath.clear();
        currentTrack = null;
    }

    private Music currentMusic() {
        return currentTrack == null ? null : musicByTrack.get(currentTrack);
    }

    private Music loadMusic(MusicTrack track) {
        FileHandle handle = Gdx.files.internal(track.getAssetPath());
        if (!handle.exists()) {
            Gdx.app.error("Audio", "Missing music: " + track.getAssetPath());
            return null;
        }
        try {
            return Gdx.audio.newMusic(handle);
        } catch (RuntimeException exception) {
            Gdx.app.error("Audio", "Failed loading music: " + track.getId(), exception);
            return null;
        }
    }

    private Sound loadSound(String assetPath) {
        FileHandle handle = Gdx.files.internal(assetPath);
        if (!handle.exists()) {
            Gdx.app.error("Audio", "Missing sound: " + assetPath);
            return null;
        }
        try {
            return Gdx.audio.newSound(handle);
        } catch (RuntimeException exception) {
            Gdx.app.error("Audio", "Failed loading sound: " + assetPath, exception);
            return null;
        }
    }

    private static float clamp(float volume) {
        if (volume < 0f) {
            return 0f;
        }
        if (volume > 1f) {
            return 1f;
        }
        return volume;
    }
}
