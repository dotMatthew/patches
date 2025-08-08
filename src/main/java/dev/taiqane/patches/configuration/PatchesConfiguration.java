package dev.taiqane.patches.configuration;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import dev.taiqane.patches.internal.TempStorage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatchesConfiguration {
    public static final String EXAMPLE_URL = "no://op";
    private transient final String gitRepoDirectory = "_workdir";

    private String baseRepoUrl;
    private String baseRepoRef;
    private String patchesDirectoryPath;

    private static Optional<PatchesConfiguration> load(File file) {
        try {
            PatchesConfiguration configuration = new Toml().read(file).to(PatchesConfiguration.class);
            return Optional.of(configuration);
        } catch (Exception ex) {
            log.error("Failed to load patches configuration", ex);
            return Optional.empty();
        }
    }

    private static Optional<PatchesConfiguration> createExampleConfiguration(TempStorage storage, String url) {
        PatchesConfiguration configuration = new PatchesConfiguration(url, "main", "patches");
        TomlWriter writer = new TomlWriter();
        try {
            writer.write(configuration, new File("patches.toml"));
            storage.setFileInThisRunCreated(true);
            return Optional.of(configuration);
        } catch (IOException e) {
            log.error("Failed to write patches.toml", e);
            return Optional.empty();
        }
    }

    public static Optional<PatchesConfiguration> existAndLoadOrCreateAndLoad(File file, TempStorage storage, String url) {
        if (Files.exists(file.toPath())) {
            return load(file);
        } else {
            return createExampleConfiguration(storage, url);
        }
    }
}
