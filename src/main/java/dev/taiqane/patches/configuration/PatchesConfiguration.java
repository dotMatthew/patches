package dev.taiqane.patches.configuration;

import dev.taiqane.patches.internal.TempStorage;
import dev.taiqane.patches.internal.error.ExitCodes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatchesConfiguration {
    private static final String PATCHES_DIRECTORY_PATH_EXAMPLE = "patches";
    public static final String EXAMPLE_URL = "no://op";
    private transient final String gitRepoDirectory = "_workdir";

    private String baseRepoUrl;
    private String baseRepoRef;
    private String patchesDirectoryPath;
    private boolean includeMetaData = false;

    public static Optional<PatchesConfiguration> load(File file) {
        try {

            Properties props = new Properties();
            props.load(new FileInputStream(file));

            if (props.isEmpty()) {
                log.error("Could not load properties file");
                return Optional.empty();
            }

            PatchesConfiguration configuration = new PatchesConfiguration();

            String baseRepoUrl = props.getProperty("baseRepoUrl");
            String baseRepoRef = props.getProperty("baseRepoRef");
            String patchesDirectoryPath = props.getProperty("patchesDirectoryPath", PATCHES_DIRECTORY_PATH_EXAMPLE);
            boolean includeMetaData = Boolean.parseBoolean(props.getProperty("includeMetaData", "false"));

            if (baseRepoUrl == null || baseRepoRef == null || patchesDirectoryPath == null) {
                log.info("Keys cannot be loaded from properties file!");
                return Optional.empty();
            }

            configuration.setBaseRepoUrl(baseRepoUrl);
            configuration.setBaseRepoRef(baseRepoRef);
            configuration.setPatchesDirectoryPath(patchesDirectoryPath);
            configuration.setIncludeMetaData(includeMetaData);

            return Optional.of(configuration);
        } catch (Exception ex) {
            log.error("Failed to load patches configuration", ex);
            return Optional.empty();
        }
    }

    private static Optional<PatchesConfiguration> createExampleConfiguration(TempStorage storage, String url) {
        PatchesConfiguration configuration = new PatchesConfiguration(url, "main", PATCHES_DIRECTORY_PATH_EXAMPLE, false);
        Properties props = loadFromConfig(configuration);
        try {
            props.store(new FileOutputStream("patches.properties"), null);
            storage.setFileInThisRunCreated(true);
            return Optional.of(configuration);
        } catch (IOException e) {
            log.error("Failed to write example patches config file to disk", e);
            return Optional.empty();
        }
    }

    public static Optional<PatchesConfiguration> existAndLoadOrCreateAndLoad(File file, TempStorage storage, String url) {
        if (file.exists()) {
            return load(file);
        } else {
            return createExampleConfiguration(storage, url);
        }
    }

    private static Properties loadFromConfig(PatchesConfiguration configuration) {
        Properties props = new Properties();
        props.setProperty("baseRepoUrl", configuration.getBaseRepoUrl());
        props.setProperty("baseRepoRef", configuration.getBaseRepoRef());
        props.setProperty("patchesDirectoryPath", configuration.getPatchesDirectoryPath());
        props.setProperty("includeMetaData", String.valueOf(configuration.isIncludeMetaData()));

        return props;
    }

    public ExitCodes save(File file) {
        Properties props = loadFromConfig(this);
        try {
            props.store(new FileOutputStream(file), null);
            return ExitCodes.SUCCESSFUL;
        } catch (IOException e) {
            log.error("Failed to save patches configuration file to disk", e);
            return ExitCodes.OPERATING_SYSTEM_ERROR;
        }
    }
}
