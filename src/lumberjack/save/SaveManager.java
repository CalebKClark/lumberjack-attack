package lumberjack.save;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages unlimited name-based save files in the saves directory.
 */
public final class SaveManager {

    private final Path saveDirectory;

    public SaveManager() {
        this(Paths.get("saves"));
    }

    public SaveManager(Path saveDirectory) {
        this.saveDirectory = saveDirectory;
        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create saves directory: " + saveDirectory, exception);
        }
    }

    public List<SaveSummary> listSaves() {
        List<SaveSummary> saves = new ArrayList<>();

        if (!Files.exists(saveDirectory)) {
            return saves;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(saveDirectory, "*.save")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                saves.add(readSummary(fileName));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not list save files.", exception);
        }

        saves.sort(Comparator.comparingLong(SaveSummary::getSavedAtMillis).reversed());
        return saves;
    }

    public boolean existsByTitle(String saveTitle) {
        return Files.exists(saveDirectory.resolve(SaveFileNames.toFileName(saveTitle)));
    }

    public String fileNameForTitle(String saveTitle) {
        return SaveFileNames.toFileName(saveTitle);
    }

    public String createNewSave(SaveData data) throws IOException {
        String fileName = SaveFileNames.toFileName(data.getSaveTitle());
        save(fileName, data);
        return fileName;
    }

    public void save(String fileName, SaveData data) throws IOException {
        data.setSavedAtMillis(System.currentTimeMillis());
        Path target = saveDirectory.resolve(fileName);
        Path tempFile = saveDirectory.resolve(fileName + ".tmp");

        SaveCodec.write(tempFile, data);
        atomicReplace(tempFile, target);
    }

    private void atomicReplace(Path tempFile, Path target) throws IOException {
        try {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public SaveData load(String fileName) throws IOException {
        SaveData data = SaveCodec.read(saveDirectory.resolve(fileName));

        if (data.getVersion() > SaveData.CURRENT_VERSION) {
            throw new IOException("Save file is from a newer game version.");
        }

        return data;
    }

    public void delete(String fileName) throws IOException {
        Path path = saveDirectory.resolve(fileName);

        if (!Files.exists(path)) {
            throw new IOException("Save file not found: " + fileName);
        }

        Files.delete(path);
    }

    private SaveSummary readSummary(String fileName) {
        try {
            SaveData data = load(fileName);
            return SaveSummary.fromLoadedSave(fileName, data);
        } catch (IOException exception) {
            return SaveSummary.corrupt(fileName);
        }
    }
}
