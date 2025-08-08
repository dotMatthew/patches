package dev.taiqane.patches.internal.git;

import dev.taiqane.patches.configuration.PatchesConfiguration;
import dev.taiqane.patches.internal.TempStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Getter
@RequiredArgsConstructor
public class GitService {
    private final PatchesConfiguration configuration;
    private final TempStorage tempStorage;

    public int downloadRepository() {
        try {
            log.info("Start to download the repository");
            Git.cloneRepository()
                    .setURI(configuration.getBaseRepoUrl())
                    .setBranch(configuration.getBaseRepoRef())
                    .setDirectory(new File("_workdir"))
                    .call();
            log.info("Successfully downloaded the repository");
            return 0;
        } catch (GitAPIException e) {
            log.error("An error occurred at downloading the repository", e);
            return 42;
        }
    }

    public int createGitPatch(String patchFileName)  {
        File repoDir = new File(this.getConfiguration().getGitRepoDirectory());

        log.info("Starting to create a patch");
        try (Repository repository = Git.open(repoDir).getRepository()) {
            try (Git git = new Git(repository)) {
                ObjectId oldCommitId = repository.resolve("HEAD~1");
                ObjectId newCommitId = repository.resolve("HEAD");

                AbstractTreeIterator oldTreeIter = prepareTreeParser(repository, oldCommitId);
                AbstractTreeIterator newTreeIter = prepareTreeParser(repository, newCommitId);

                if (oldTreeIter == null || newTreeIter == null) {
                    log.error("AbstractTreeIterator is null but should not be!");
                    return 42;
                }

                List<DiffEntry> diffs = git.diff()
                        .setOldTree(oldTreeIter)
                        .setNewTree(newTreeIter)
                        .call();

                File patchFile = new File(this.getConfiguration().getPatchesDirectoryPath() + "/" + patchFileName + ".patch");

                try (OutputStream fos = new FileOutputStream(patchFile, false); // false = überschreiben
                     OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                     DiffFormatter formatter = new DiffFormatter(fos)) {

                    formatter.setRepository(repository);
                    formatter.setDiffComparator(RawTextComparator.DEFAULT);
                    formatter.setDetectRenames(true);

                    for (DiffEntry entry : diffs) {
                        formatter.format(entry);
                    }
                }

                log.info("Patch successfully created and saved to disk! ({}.patch)", patchFileName);
                return 0;
            }
        } catch (GitAPIException | IOException e) {
            log.error("An error occurred at creating this patch", e);
            return 42;
        }
    }

    private AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            ObjectId treeId = commit.getTree().getId();

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (var reader = repository.newObjectReader()) {
                treeParser.reset(reader, treeId);
            }
            walk.dispose();
            return treeParser;
        } catch (IncorrectObjectTypeException e) {
            log.error("Incorrect type given", e);
        } catch (MissingObjectException e) {
            log.error("Missing object given", e);
        } catch (IOException e) {
            log.error("An error occurred at creating this patch", e);
        }
        return null;
    }

    public int applyPatches() {
        File[] patchFiles = new File(this.getConfiguration().getPatchesDirectoryPath()).listFiles((dir, name) -> name.toLowerCase().endsWith(".patch"));

        if (patchFiles == null || patchFiles.length == 0) {
            log.error("No patches to apply found");
            return 42;
        }

        try (Git git = Git.open(new File(this.getConfiguration().getGitRepoDirectory()))) {
            for (File file : patchFiles) {
                log.info("Applying patch {}", file.getName());
                try (FileInputStream fis = new FileInputStream(file)) {
                    git.apply()
                            .setPatch(fis)
                            .call();
                    log.info("Applied patch {} successfully", file.getName());
                } catch (GitAPIException | IOException e) {
                    log.error("An error occurred at applying patch {} ", file.getName(), e);
                    return 42;
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("An error occurred at applying patch", e);
            return 42;
        }
    }
}
