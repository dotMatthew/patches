package dev.taiqane.patches.cli.command;

import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "clean",
        description = "Delete all work files"
)
public class CleanCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("Deleting workdir/ directory");
        try {
            File file = new File("workdir/");
            if (file.exists() && file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File deleteFile : files) {
                        var ignore = deleteFile.delete();
                    }
                }
            }
            return 0;
        } catch (Exception ex) {
            System.err.println("An error occurred at deleting the workdir directory: " + ex.getMessage());
            return 42;
        }
    }
}
