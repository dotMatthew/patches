package dev.taiqane.patches.cli.command;

import dev.taiqane.patches.configuration.PatchesConfiguration;
import dev.taiqane.patches.internal.TempStorage;
import dev.taiqane.patches.internal.git.GitService;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

@Getter
@Command(name = "init",
        description = "Create a new patches file if not existing and downloads the base repository as specified in .patches.toml, but does not apply any patches."
)
public class InitCommand implements Callable<Integer> {
    private final TempStorage storage = new TempStorage();
    private PatchesConfiguration configuration;

    @Option(names = {"-F", "--config"},
            description = "Path to patches config file. Defaults to .patches.toml in the current directory if not specificied")
    private File configFilePath = new File(".patches.toml");

    @Override
    public Integer call() throws Exception {
        PatchesConfiguration.existAndLoadOrCreateAndLoad(configFilePath, storage)
                .ifPresent(patchesConfiguration -> configuration = patchesConfiguration);

        if (configuration == null) {
            System.err.println("No valid patches configuration found?");
            return 42;
        }

        String baseRepoUrl = configuration.getBaseRepoUrl();

        if (baseRepoUrl.equals(PatchesConfiguration.EXAMPLE_URL)) {
            if (storage.isFileInThisRunCreated()) {
                System.out.println("Create new base .patches.toml file! Happy working :)");
                return 0;
            } else {
                System.err.println("Example url " + PatchesConfiguration.EXAMPLE_URL + " cannot be used to download the base repository! Aborting!");
                return 42;
            }
        }

        GitService gitService = new GitService(this.getConfiguration(), this.getStorage());
        return gitService.downloadRepository();
    }
}
