package dev.taiqane.patches.cli.command;

import dev.taiqane.patches.configuration.PatchesConfiguration;
import dev.taiqane.patches.internal.TempStorage;
import dev.taiqane.patches.internal.error.ExitCodes;
import dev.taiqane.patches.internal.git.GitService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

@Slf4j
@Getter
@NoArgsConstructor
@Command(name = "create-patch", description = "Creates a new patch from the current commit")
public class CreatePatchCommand implements Callable<Integer> {
    private final TempStorage storage = new TempStorage();
    private PatchesConfiguration configuration;

    @Option(names = {"-F", "--config"}, description = "Path to patches config file. Defaults to patches.properties")
    private File configFile = new File("patches.properties");

    @Option(names = {"-U", "--repo-url"}, description = "URL of the Base Repo")
    private String url = "no://op";

    @Override
    public Integer call() throws Exception {
        if (!this.getConfigFile().exists()) {
            log.error("No valid patches configuration found!");
            return ExitCodes.USAGE_ERROR.getCodeValue();
        }

        PatchesConfiguration.load(this.getConfigFile()).ifPresent(patchesConfiguration -> configuration = patchesConfiguration);

        log.info("Specify the name of the new patch file: (Without .patch extension)");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String fileName = reader.readLine();

            GitService gitService = new GitService(this.getConfiguration(), this.getStorage());
            return gitService.createGitPatch(fileName).getCodeValue();
        } catch (IOException e) {
            log.error("There was an error at reading from the default System input", e);
            return ExitCodes.OPERATING_SYSTEM_ERROR.getCodeValue();
        }
    }
}
