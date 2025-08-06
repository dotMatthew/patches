package dev.taiqane.patches.cli.command;

import org.eclipse.jgit.api.Git;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;

@Command(
        name = "apply",
        description = "Applies the patches to the base repository"
)
public class ApplyCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        try (Git git = Git.open(new File("workdir/"))) {

            git

        } catch (Exception ex) {

        }
        return 0;
    }
}
