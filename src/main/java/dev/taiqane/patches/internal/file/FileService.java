package dev.taiqane.patches.internal.file;

import dev.taiqane.patches.configuration.PatchesConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
@Getter
@RequiredArgsConstructor
public class FileService {
    private final PatchesConfiguration configuration;

    private int deleteDirectoryRecursively(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        this.deleteDirectoryRecursively(file);
                    } else {
                        boolean deleted = file.delete();
                        if (!deleted) {
                            log.error("Unable to delete file {}", file.getName());
                        }
                    }
                }
            }

            boolean isDirDeleted = dir.delete();
            if (!isDirDeleted) {
                log.error("Unable to delete workdir directory: {}", dir.getName());
            }
            return 0;
        }
        log.error("{} does not exist", dir.getName());
        return 42;
    }

    public int cleanWorkDir() {
        File file = new File(this.getConfiguration().getGitRepoDirectory());
        if (file.exists() && file.isDirectory()) {
            return this.deleteDirectoryRecursively(file);
        } else {
            log.error("Git repo dir specified in config is not a directory or does not exist?");
            return 42;
        }
    }
}
