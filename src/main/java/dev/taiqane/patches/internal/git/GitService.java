package dev.taiqane.patches.internal.git;

import dev.taiqane.patches.configuration.PatchesConfiguration;
import dev.taiqane.patches.internal.TempStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;

@Getter
@RequiredArgsConstructor
public class GitService {
    private final PatchesConfiguration configuration;
    private final TempStorage tempStorage;

    public int downloadRepository() {
        try {
            System.out.println("Start to download the repository");
            Git.cloneRepository()
                    .setURI(configuration.getBaseRepoUrl())
                    .setBranch(configuration.getBaseRepoRef())
                    .setDirectory(new File("workdir"))
                    .call();
            System.out.println("Successfully downloaded the repository");
            return 0;
        } catch (GitAPIException e) {
            System.err.println("An error occurred at downloading the repository: " + e.getMessage());
            return 42;
        }
    }
}
