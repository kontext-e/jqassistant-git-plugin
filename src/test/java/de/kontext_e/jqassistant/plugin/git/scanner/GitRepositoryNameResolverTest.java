package de.kontext_e.jqassistant.plugin.git.scanner;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;

class GitRepositoryNameResolverTest {

    // ---------------------------------------------------------------------
    // Pure path heuristic: no filesystem state required
    // ---------------------------------------------------------------------

    @Test
    void resolvesNormalRepoFromPath() {
        String name = GitRepositoryNameResolver.resolveName(new File("/tmp/proj/.git/HEAD"));
        assertThat(name).isEqualTo("proj");
    }

    @Test
    void resolvesSubmoduleFromPath() {
        String name = GitRepositoryNameResolver.resolveName(new File("/tmp/main/.git/modules/sub/HEAD"));
        assertThat(name).isEqualTo("sub");
    }

    @Test
    void resolvesWorktreeFromPath() {
        String name = GitRepositoryNameResolver.resolveName(new File("/tmp/main/.git/worktrees/wt/HEAD"));
        assertThat(name).isEqualTo("wt");
    }

    @Test
    void resolvesBareRepoFromPath() {
        String name = GitRepositoryNameResolver.resolveName(new File("/tmp/proj.git/HEAD"));
        assertThat(name).isEqualTo("proj");
    }

    @Test
    void doesNotReturnLiteralModulesForSubmodule() {
        String name = GitRepositoryNameResolver.resolveName(new File("/tmp/main/.git/modules/whatever/HEAD"));
        assertThat(name).isNotEqualTo("modules");
    }

    // ---------------------------------------------------------------------
    // JGit-backed: real on-disk repositories
    // ---------------------------------------------------------------------

    @Test
    void resolvesViaJGitForRealRepository(@TempDir Path tempDir) throws Exception {
        File repoDir = tempDir.resolve("my-repo").toFile();
        try (Git ignored = Git.init().setDirectory(repoDir).call()) {
            File head = new File(repoDir, ".git/HEAD");
            assertThat(head).exists();

            String name = GitRepositoryNameResolver.resolveName(head);

            assertThat(name).isEqualTo("my-repo");
        }
    }

    @Test
    void resolvesViaJGitForSubmodule(@TempDir Path tempDir) throws Exception {
        // Set up the parent repository.
        File mainRepo = tempDir.resolve("main").toFile();
        try (Git ignored = Git.init().setDirectory(mainRepo).call()) {
            // Submodule check-out lives at <main>/libs/my-sub
            File submoduleWorkTree = new File(mainRepo, "libs/my-sub");
            assertThat(submoduleWorkTree.mkdirs()).isTrue();

            // Submodule's gitdir lives at <main>/.git/modules/my-sub (Git's standard layout).
            File submoduleGitDir = new File(mainRepo, ".git/modules/my-sub");

            // Create the submodule repository with a separate gitdir/worktree split.
            // JGit's Git.init records core.worktree pointing at the working tree.
            try (Git submodule = Git.init()
                    .setGitDir(submoduleGitDir)
                    .setDirectory(submoduleWorkTree)
                    .call()) {
                ensureWorkTreeConfigured(submoduleGitDir, submoduleWorkTree);
            }

            File head = new File(submoduleGitDir, "HEAD");
            assertThat(head).exists();

            String name = GitRepositoryNameResolver.resolveName(head);

            assertThat(name).isEqualTo("my-sub");
        }
    }

    @Test
    void resolvesViaJGitForLinkedWorktree(@TempDir Path tempDir) throws Exception {
        File mainRepo = tempDir.resolve("project").toFile();
        try (Git ignored = Git.init().setDirectory(mainRepo).call()) {
            File worktreeCheckout = tempDir.resolve("project-feature").toFile();
            assertThat(worktreeCheckout.mkdirs()).isTrue();

            // Simulate a linked worktree: <main>/.git/worktrees/feature with core.worktree
            // pointing to <tempDir>/project-feature.
            File worktreeGitDir = new File(mainRepo, ".git/worktrees/feature");
            assertThat(worktreeGitDir.mkdirs()).isTrue();
            Files.write(worktreeGitDir.toPath().resolve("HEAD"),
                    ("ref: " + Constants.R_HEADS + "main\n").getBytes(),
                    StandardOpenOption.CREATE_NEW);
            Files.write(worktreeGitDir.toPath().resolve("commondir"),
                    "../..\n".getBytes(),
                    StandardOpenOption.CREATE_NEW);
            ensureWorkTreeConfigured(worktreeGitDir, worktreeCheckout);

            File head = new File(worktreeGitDir, "HEAD");
            String name = GitRepositoryNameResolver.resolveName(head);

            // JGit may or may not be able to fully open the synthetic linked worktree;
            // either way we must not return the literal segment "worktrees".
            assertThat(name).isNotEqualTo("worktrees");
            assertThat(name).isEqualTo("project-feature");
        }
    }

    private static void ensureWorkTreeConfigured(File gitDir, File workTree) throws Exception {
        FileRepositoryBuilder builder = new FileRepositoryBuilder().setGitDir(gitDir);
        try (Repository repository = builder.build()) {
            StoredConfig config = repository.getConfig();
            config.setString("core", null, "worktree", workTree.getAbsolutePath());
            config.save();
        }
    }
}
