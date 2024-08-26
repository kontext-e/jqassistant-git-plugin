package de.kontext_e.jqassistant.plugin.git.scanner;

import de.kontext_e.jqassistant.plugin.git.scanner.model.GitBranch;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitCommit;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitTag;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JGitRepositoryGitMockBuilder {
    private List<GitBranch> branches = new LinkedList<>();
    private List<GitTag> tags = new LinkedList<>();
    private List<GitCommit> commits = new LinkedList<>();
    private String currentlyCheckedOutBranch = "master";
    private GitBranch head;

    public JGitRepositoryGitMockBuilder withBranches(GitBranch... branches)  {
        this.branches = new LinkedList<>(List.of(branches));
        return this;
    }

    public JGitRepositoryGitMockBuilder withTags(GitTag... tags) throws IOException {
        this.tags = new LinkedList<>(List.of(tags));
        return this;
    }

    public JGitRepositoryGitMockBuilder withCommits(GitCommit... commits) throws IOException {
        this.commits = new LinkedList<>(List.of(commits));
        return this;
    }

    public JGitRepositoryGitMockBuilder withHead(GitBranch head) throws IOException {
        this.head = head;
        return this;
    }

    public JGitRepositoryGitMockBuilder withCurrentlyCheckedOutBranch(String currentlyCheckedOutBranch) {
        this.currentlyCheckedOutBranch = currentlyCheckedOutBranch;
        return this;
    }

    JGitRepository build() throws IOException {
        JGitRepository jGitRepository = mock(JGitRepository.class);

        if (head == null) {
            head = mock();
            when(head.getCommitSha()).thenReturn("5629e0cfd6c404e02ca3c656fb91bdd874e1a196");
        }

        when(jGitRepository.findBranches()).thenReturn(branches);
        when(jGitRepository.findTags()).thenReturn(tags);
        when(jGitRepository.findCommits(any())).thenReturn(commits);
        when(jGitRepository.getCurrentlyCheckedOutBranch()).thenReturn(currentlyCheckedOutBranch);
        when(jGitRepository.findHead()).thenReturn(head);

        return jGitRepository;
    }

}
