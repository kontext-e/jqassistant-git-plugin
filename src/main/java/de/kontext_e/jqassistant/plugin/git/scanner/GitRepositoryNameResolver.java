package de.kontext_e.jqassistant.plugin.git.scanner;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

/**
 * Resolves a human-readable repository name for a Git repository given the path
 * to its {@code HEAD} file.
 *
 * <p>Handles the following layouts:</p>
 * <ul>
 *     <li>Regular repositories: {@code /proj/.git/HEAD} &rarr; {@code proj}</li>
 *     <li>Bare repositories: {@code /proj.git/HEAD} &rarr; {@code proj}</li>
 *     <li>Submodules: {@code /main/.git/modules/&lt;name&gt;/HEAD} &rarr; {@code &lt;name&gt;}
 *         (or the actual working-tree directory name when JGit can resolve it)</li>
 *     <li>Linked worktrees: {@code /main/.git/worktrees/&lt;name&gt;/HEAD} &rarr; {@code &lt;name&gt;}</li>
 * </ul>
 *
 * <p>Strategy:</p>
 * <ol>
 *     <li>Try to open the repository via JGit and read the working tree directory.
 *         For submodules and linked worktrees JGit honours {@code core.worktree}
 *         and yields the real checkout location.</li>
 *     <li>Fall back to a pure path heuristic when the filesystem state does not
 *         allow JGit to open the repository (e.g. unit tests with synthetic paths).</li>
 * </ol>
 */
final class GitRepositoryNameResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitRepositoryNameResolver.class);

    private static final String DOT_GIT = ".git";
    private static final String MODULES = "modules";
    private static final String WORKTREES = "worktrees";

    private GitRepositoryNameResolver() {
    }

    /**
     * Resolve the repository name for the given {@code HEAD} file.
     *
     * @param headFile the {@code HEAD} file inside the Git directory
     * @return the resolved repository name, never {@code null}
     */
    static String resolveName(File headFile) {
        Path headPath = headFile.toPath().toAbsolutePath().normalize();
        Path gitDir = headPath.getParent();
        if (gitDir == null) {
            return headFile.getName();
        }

        String jgitName = resolveViaJGit(gitDir);
        if (jgitName != null) {
            LOGGER.debug("Resolved repository name '{}' via JGit for gitDir '{}'", jgitName, gitDir);
            return jgitName;
        }

        String heuristicName = resolveViaPathHeuristic(gitDir);
        LOGGER.debug("Resolved repository name '{}' via path heuristic for gitDir '{}'", heuristicName, gitDir);
        return heuristicName;
    }

    private static String resolveViaJGit(Path gitDir) {
        File gitDirFile = gitDir.toFile();
        if (!gitDirFile.isDirectory()) {
            return null;
        }
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder()
                    .setGitDir(gitDirFile)
                    .readEnvironment();
            try (Repository repository = builder.build()) {
                File workTree = repository.getWorkTree();
                if (workTree != null) {
                    String name = workTree.getName();
                    if (name != null && !name.isEmpty()) {
                        return name;
                    }
                }
                // Bare repository: derive from gitDir name, stripping a trailing ".git".
                return stripDotGit(gitDirFile.getName());
            }
        } catch (Exception e) {
            LOGGER.debug("JGit could not open '{}': {}", gitDir, e.getMessage());
            return null;
        }
    }

    private static String resolveViaPathHeuristic(Path gitDir) {
        Path parent = gitDir.getParent();
        if (parent != null) {
            String parentName = nameOf(parent);
            if (MODULES.equals(parentName) || WORKTREES.equals(parentName)) {
                // /main/.git/modules/<name>/HEAD       -> <name>
                // /main/.git/worktrees/<name>/HEAD     -> <name>
                return nameOf(gitDir);
            }
        }
        if (DOT_GIT.equals(nameOf(gitDir)) && parent != null) {
            // /proj/.git/HEAD -> proj
            return nameOf(parent);
        }
        // Bare repository or unknown layout: strip trailing ".git" if present.
        return stripDotGit(nameOf(gitDir));
    }

    private static String nameOf(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? "" : fileName.toString();
    }

    private static String stripDotGit(String name) {
        if (name.endsWith(DOT_GIT) && name.length() > DOT_GIT.length()) {
            return name.substring(0, name.length() - DOT_GIT.length());
        }
        return name;
    }
}
