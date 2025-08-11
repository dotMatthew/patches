package dev.taiqane.patches.cli.command;

import dev.taiqane.patches.configuration.PatchesConfiguration;
import dev.taiqane.patches.internal.TempStorage;
import dev.taiqane.patches.internal.file.FileService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;

@Getter
@Slf4j
@NoArgsConstructor
@Command(name = "config", description = "Changes a value in the current patches config file")
public class ConfigCommand implements Callable<Integer> {
    private final TempStorage storage = new TempStorage();
    private PatchesConfiguration configuration;

    @Option(names = {"-F", "--config"}, description = "Path to patches config file. Defaults to patches.properties")
    private File configFile = new File("patches.properties");

    @Option(names = {"--base-repo-url", "-BRU"}, description = "Set the base repository url")
    private String baseRepoUrl;

    @Option(names = {"--base-repo-ref", "-BRF"}, description = "Set the default repository ref")
    private String baseRepoRef;

    @Option(names = {"--patches-directory-path", "-PDP"}, description = "Set the path to the patches directory")
    private String patchesDirectoryPath;

    @Override
    public Integer call() throws Exception {
        if (!this.getConfigFile().exists()) {
            log.error("No patches config file found. Please run patches init first!");
            return 42;
        }

        PatchesConfiguration.load(this.getConfigFile()).ifPresent(patchesConfiguration -> configuration = patchesConfiguration);
        if (this.getBaseRepoUrl() != null) {
            log.info("Changing base repo url to '{}' from '{}'",  this.getBaseRepoUrl(), this.getConfiguration().getBaseRepoUrl());
            this.getConfiguration().setBaseRepoUrl(this.getBaseRepoUrl());
        }

        if (this.getBaseRepoRef() != null) {
            log.info("Changing base repo ref to '{}' from '{}'", this.getBaseRepoRef(), this.getConfiguration().getBaseRepoRef());
            this.getConfiguration().setBaseRepoRef(this.getBaseRepoRef());
        }

        if (this.getPatchesDirectoryPath() != null) {
            log.info("Changing patches directory path to '{}' from '{}'", this.getPatchesDirectoryPath(), this.getConfiguration().getPatchesDirectoryPath());

            FileService fileService = new FileService(this.getConfiguration());
            fileService.renamePatchesDirectory(new File(this.getConfiguration().getPatchesDirectoryPath()), new File(this.getPatchesDirectoryPath()));

            this.getConfiguration().setPatchesDirectoryPath(this.getPatchesDirectoryPath());
        }

        log.info("Saving configuration changes to disk");
        return this.getConfiguration().save(this.getConfigFile()).getCodeValue();
    }
}
