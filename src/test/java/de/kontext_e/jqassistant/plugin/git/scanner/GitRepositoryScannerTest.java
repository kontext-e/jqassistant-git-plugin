package de.kontext_e.jqassistant.plugin.git.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.core.test.plugin.AbstractPluginIT;
import com.buschmais.xo.api.Query;
import com.buschmais.xo.api.ResultIterator;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitBranch;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitChange;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitCommit;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitTag;
import de.kontext_e.jqassistant.plugin.git.scanner.repositories.JGitRepository;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.*;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.change.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

class GitRepositoryScannerTest extends AbstractPluginIT {

    GitRepositoryDescriptor gitRepositoryDescriptor;

    @BeforeEach
    public void beginTransaction() {
        store.beginTransaction();
        gitRepositoryDescriptor = spy(store.create(GitRepositoryDescriptor.class));
        gitRepositoryDescriptor.setFileName(".git");
    }

    @AfterEach
    public void commitTransaction() {
        store.commitTransaction();
        store.beginTransaction();
        store.executeQuery("Match (n) DETACH DELETE n").close();
        store.commitTransaction();
    }

    @Test
    void testScanEmptyGitRepository() throws IOException {
        Store store = spy(super.store);
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store, never()).create(any());
    }

    @Test
    void testScanNewBranches() throws IOException {
        Store store = spy(super.store);
        when(store.executeQuery("Match (b:Branch) return b")).thenThrow(NoSuchElementException.class);
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder()
                .withBranches(
                    new GitBranch("master", "1234"),
                    new GitBranch("develop", "5678"))
                .build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store, times(2)).create(GitBranchDescriptor.class);
        verify(store).executeQuery(eq("MATCH (repo:Git:Repository)-[*]->(branch:Branch) WHERE repo.fileName = $path RETURN branch"), anyMap());
        verify(store).executeQuery("MATCH (c:Commit) where c.sha = $sha return c", Map.of("sha", "1234"));
        verify(store).executeQuery("MATCH (c:Commit) where c.sha = $sha return c", Map.of("sha", "5678"));
    }

    @Test
    void testScanExistingBranches() throws IOException {
        Store store = spy(super.store);

        GitBranchDescriptor gitBranchDescriptor = super.store.create(GitBranchDescriptor.class);
        gitBranchDescriptor.setName("master");
        GitCommitDescriptor headDescriptor = super.store.create(GitCommitDescriptor.class);
        headDescriptor.setSha("1234");
        gitRepositoryDescriptor.setHead(headDescriptor);
        gitRepositoryDescriptor.getBranches().add(gitBranchDescriptor);

        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder()
                .withBranches(new GitBranch("master", "1234"))
                .build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store, never()).create(GitBranchDescriptor.class);
        verify(store).executeQuery(eq("MATCH (repo:Git:Repository)-[*]->(branch:Branch) WHERE repo.fileName = $path RETURN branch"), anyMap());
    }

    @Test
    void testScanNewTags() throws IOException {
        Store store = spy(super.store);
        when(store.executeQuery("Match (t:Tag) return t")).thenThrow(NoSuchElementException.class);
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withTags(
                new GitTag("master", "1234"),
                new GitTag("develop", "5678")
        ).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store, times(2)).create(GitTagDescriptor.class);
        verify(store).executeQuery(eq("MATCH (repo:Git:Repository)-[*]->(t:Tag) WHERE repo.fileName = $path RETURN t"), anyMap());
        verify(store).executeQuery("MATCH (c:Commit) where c.sha = $sha return c", Map.of("sha", "1234"));
        verify(store).executeQuery("MATCH (c:Commit) where c.sha = $sha return c", Map.of("sha", "5678"));
    }

    @Test
    void testScanExistingTags() throws IOException {
        Store store = spy(super.store);

        GitTagDescriptor gitBranchDescriptor = super.store.create(GitTagDescriptor.class);
        gitBranchDescriptor.setLabel("master");
        GitCommitDescriptor headDescriptor = super.store.create(GitCommitDescriptor.class);
        headDescriptor.setSha("1234");
        gitRepositoryDescriptor.setHead(headDescriptor);

        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder()
                .withTags(new GitTag("master", "1234"))
                .build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store, never()).create(GitBranchDescriptor.class);
        verify(store).executeQuery(eq("MATCH (repo:Git:Repository)-[*]->(t:Tag) WHERE repo.fileName = $path RETURN t"), anyMap());
    }

    @Test
    void testAddCommit() throws IOException {
        Store store = spy(super.store);

        GitCommit gitCommit = CommitBuilder.builder()
                .shortMessage("Short Message")
                .message("Message")
                .encoding("UTF-8")
                .build();

        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(gitCommit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store).create(GitCommitDescriptor.class);
        verify(gitRepositoryDescriptor).getCommits();
    }

    @Test
    void testAddAuthor() throws IOException {
        Store store = spy(super.store);
        //Also verify that apostrophes in the authors' name work
        when(store.executeQuery("MATCH (a:Author) where a.identString = $ident return a", Map.of("ident", "Au'thor<Au'thor@e-mail.com>"))).thenThrow(NoSuchElementException.class);
        GitCommit gitCommit = CommitBuilder.builder().author("Au'thor<Au'thor@e-mail.com>").build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(gitCommit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store).create(GitAuthorDescriptor.class);
        verify(store).executeQuery("MATCH (a:Author) where a.identString = $ident return a", Map.of("ident", "Au'thor<Au'thor@e-mail.com>"));
        verify(gitRepositoryDescriptor).getAuthors();
    }

    @Test
    void testExistingAuthor() throws IOException {
        Store store = spy(super.store);
        GitAuthorDescriptor authorDescriptor = super.store.create(GitAuthorDescriptor.class);
        authorDescriptor.setIdentString("Author<Author@e-mail.com>");
        GitCommit gitCommit = CommitBuilder.builder().author("Author<Author@e-mail.com>").build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(gitCommit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store, never()).create(GitAuthorDescriptor.class);
    }

    @Test
    void testCommitParentRelation() throws IOException {
        Store store = spy(super.store);
        GitCommit parentCommit = CommitBuilder.builder().sha("1234").build();
        GitCommit childCommit = CommitBuilder.builder().sha("5678").parents(List.of(parentCommit)).build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(parentCommit, childCommit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store, times(2)).create(GitCommitDescriptor.class);
        ResultIterator<Query.Result.CompositeRowObject> iterator = store.executeQuery("Match (c:Git:Commit)-[r:HAS_PARENT]->(p:Git:Commit) return r").iterator();
        assertThat(iterator.hasNext()).isTrue();
    }

    @Test
    void testAlreadyStoredParent() throws IOException {
        Store store = spy(super.store);
        // Parent Commit saved in DB already
        GitCommit parentCommit = CommitBuilder.builder().sha("1234").build();
        GitCommitDescriptor parentDescriptor = store.create(GitCommitDescriptor.class);
        parentDescriptor.setSha(parentCommit.getSha());
        // Child Commit is new, has reference to parent and is returned by jgit
        GitCommit childCommit = CommitBuilder.builder().sha("5678").parents(List.of(parentCommit)).build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(childCommit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        // times(2) because method is called during test-setup
        verify(store, times(2)).create(GitCommitDescriptor.class);
        ResultIterator<Query.Result.CompositeRowObject> iterator = store.executeQuery("Match (c:Git:Commit)-[r:HAS_PARENT]->(p:Git:Commit) return r").iterator();
        assertThat(iterator.hasNext()).isTrue();
    }

    @Test
    void testAddCommitter() throws IOException {
        Store store = spy(super.store);
        when(store.executeQuery("MATCH (c:Committer) where c.identString = $ident return c", Map.of("ident", "Committer<Committer@e-mail.com>"))).thenThrow(NoSuchElementException.class);
        GitCommit gitCommit = CommitBuilder.builder().committer("Committer<Committer@e-mail.com>").build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(gitCommit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store).create(GitCommitterDescriptor.class);
        verify(store).executeQuery("MATCH (c:Committer) where c.identString = $ident return c", Map.of("ident", "Committer<Committer@e-mail.com>"));
        verify(gitRepositoryDescriptor).getCommits();
    }

    @Test
    void testExistingCommitter() throws IOException {
        Store store = spy(super.store);
        GitCommitterDescriptor committerDescriptor = super.store.create(GitCommitterDescriptor.class);
        committerDescriptor.setIdentString("Committer<Committer@e-mail.com>");
        GitCommit gitCommit = CommitBuilder.builder().author("Committer<Committer@e-mail.com>").build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(gitCommit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store, never()).create(GitCommitterDescriptor.class);
    }

    @Test
    void testModificationChange() throws IOException {
        store = spy(super.store);
        GitChange change = new GitChange("M", "Old/Path", "Old/Path");
        GitCommit commit = CommitBuilder.builder().gitChanges(List.of(change)).build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(commit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store).create(GitUpdateChangeDescriptor.class);
    }

    @Test
    void testAddChange() throws IOException {
        store = spy(super.store);
        GitChange change = new GitChange("A", "Old/Path", "Old/Path");
        GitCommit commit = CommitBuilder.builder().gitChanges(List.of(change)).build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(commit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store).create(GitAddChangeDescriptor.class);
    }

    @Test
    void testDeleteChange() throws IOException {
        store = spy(super.store);
        GitChange change = new GitChange("D", "Old/Path", "Old/Path");
        GitCommit commit = CommitBuilder.builder().gitChanges(List.of(change)).build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(commit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store).create(GitDeleteChangeDescriptor.class);
    }

    @Test
    void testRenameChange() throws IOException {
        store = spy(super.store);
        GitChange change = new GitChange("R", "Old/Path", "Old/Path");
        GitCommit commit = CommitBuilder.builder().gitChanges(List.of(change)).build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(commit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store).create(GitRenameChangeDescriptor.class);
    }

    @Test
    void testCopyChange() throws IOException {
        store = spy(super.store);
        GitChange change = new GitChange("C", "Old/Path", "Old/Path");
        GitCommit commit = CommitBuilder.builder().gitChanges(List.of(change)).build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(commit).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository, false).scanGitRepo();

        verify(store).create(GitCopyChangeDescriptor.class);
    }

    @Test
    void testNormalRange() throws IOException {
        store = spy(super.store);
        String range = "12345..67890";
        GitCommit commit1 = CommitBuilder.builder().sha("12345").build();
        GitCommit commit2 = CommitBuilder.builder().sha("34567").parents(List.of(commit1)).build();
        GitCommit commit3 = CommitBuilder.builder().sha("67890").parents(List.of(commit2)).build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder()
                .withCommits(commit1, commit2, commit3)
                .build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, range, jGitRepository, false).scanGitRepo();

        verify(store, times(3)).create(GitCommitDescriptor.class);
    }

    @Test
    void testNormalRangeWithExistingCommits() throws IOException {
        store = spy(super.store);
        String range = "12345..67890";
        GitCommitDescriptor commitDescriptor = store.create(GitCommitDescriptor.class);
        commitDescriptor.setSha("12345");
        GitCommit commit1 = CommitBuilder.builder().sha("12345").build();
        GitCommit commit2 = CommitBuilder.builder().sha("34567").parents(List.of(commit1)).build();
        GitCommit commit3 = CommitBuilder.builder().sha("67890").parents(List.of(commit2)).build();
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder()
                .withCommits(commit1, commit2, commit3)
                .build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, range, jGitRepository, false).scanGitRepo();

        verify(store, times(4)).create(GitCommitDescriptor.class);
    }

    @Test
    void testRangeWithExistingBranchName() throws IOException {
        store = spy(super.store);
        String range = "12345..main";
        GitCommitDescriptor commitDescriptor = store.create(GitCommitDescriptor.class);
        commitDescriptor.setSha("34567");
        GitBranchDescriptor branchDescriptor = store.create(GitBranchDescriptor.class);
        branchDescriptor.setName("main");
        branchDescriptor.setHead(commitDescriptor);
        gitRepositoryDescriptor.getBranches().add(branchDescriptor);
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().withCommits(
                CommitBuilder.builder().sha("67890").build()
        ).build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, range, jGitRepository, false).scanGitRepo();

        // times(2) because head commit of main is already created in test
        verify(store, times(2)).create(GitCommitDescriptor.class);
        verify(jGitRepository).findCommits("34567..main");
    }

    @Test
    void testRangeWithNewBranchName() throws IOException {
        store = spy(super.store);
        when(store.executeQuery("Match (b:Branch) return b")).thenThrow(NoSuchElementException.class);
        String range = "12345..dev";
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder()
                .withBranches(new GitBranch("dev", "12345"))
                .withCommits(CommitBuilder.builder().sha("1234").build())
                .build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, range, jGitRepository, false).scanGitRepo();

        verify(store).create(GitCommitDescriptor.class);
        verify(store).create(GitBranchDescriptor.class);
    }

    @Test
    void testTranslateHeadIntoActualBranchName() throws IOException {
        store = spy(super.store);
        String range = "12345..HEAD";
        GitBranchDescriptor branchDescriptor = store.create(GitBranchDescriptor.class);
        branchDescriptor.setName("branch");
        gitRepositoryDescriptor.getBranches().add(branchDescriptor);
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder()
                .withCurrentlyCheckedOutBranch("refs/branch")
                .build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, range, jGitRepository, false).scanGitRepo();

        verify(jGitRepository).findCommits("12345..HEAD");
        verify(store).executeQuery("MATCH (b:Branch)-[:HAS_HEAD]->(n:Commit) where b.name = $sha return n.sha", Map.of("sha", "branch"));
    }

    @Test
    void testEmptyUntilRange() throws IOException {
        store = spy(super.store);
        String range = "12345..";

        GitBranchDescriptor branchDescriptor = store.create(GitBranchDescriptor.class);
        branchDescriptor.setName("branch");
        gitRepositoryDescriptor.getBranches().add(branchDescriptor);
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder()
                .withCurrentlyCheckedOutBranch("refs/branch")
                .build();

        new GitRepositoryScanner(store, gitRepositoryDescriptor, range, jGitRepository, false).scanGitRepo();

        verify(jGitRepository).findCommits("12345..HEAD");
        verify(store).executeQuery("MATCH (b:Branch)-[:HAS_HEAD]->(n:Commit) where b.name = $sha return n.sha", Map.of("sha", "branch"));
    }
}
