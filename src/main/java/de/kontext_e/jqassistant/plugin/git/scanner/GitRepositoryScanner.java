package de.kontext_e.jqassistant.plugin.git.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.git.scanner.cache.*;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitBranch;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitChange;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitCommit;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitTag;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import static de.kontext_e.jqassistant.plugin.git.scanner.JQAssistantGitRepository.findShaOfLatestScannedCommitOfBranch;

public class GitRepositoryScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitRepositoryScanner.class);

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss Z");
    public static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    private final Store store;
    private final GitRepositoryDescriptor gitRepositoryDescriptor;
    private final CommitCache commitCache;
    private final AuthorCache authorCache;
    private final CommitterCache committerCache;
    private final FileAnalyzer fileAnalyzer;
    private final FileCache fileCache;
    private final TagCache tagCache;
    private final BranchCache branchCache;
    private String range;

    GitRepositoryScanner(final Store store, final GitRepositoryDescriptor gitRepositoryDescriptor, final String range) {
        this.store = store;
        this.gitRepositoryDescriptor = gitRepositoryDescriptor;
        this.range = range;

        this.commitCache = new CommitCache(store);
        this.authorCache = new AuthorCache(store);
        this.committerCache = new CommitterCache(store);
        this.fileCache = new FileCache(store);
        this.tagCache = new TagCache(store);
        this.branchCache = new BranchCache(store);

        this.fileAnalyzer = new FileAnalyzer(fileCache);
    }

    void scanGitRepo() throws IOException {
        JGitRepository jGitRepository = new JGitRepository(gitRepositoryDescriptor.getFileName());

        checkForExistingCommitsAndAdjustRangeAccordingly();

        storeCommits(jGitRepository.findCommits(range));
        storeBranches(jGitRepository.findBranches());
        storeTags(jGitRepository.findTags());

        addAdditionalRelations();
        adjustGitHead(jGitRepository);
    }

    private void checkForExistingCommitsAndAdjustRangeAccordingly() {
        if (range == null) return;

        String untilString = range.substring(range.lastIndexOf(".") + 1);
        GitBranchDescriptor gitBranchDescriptor = branchCache.find(untilString);

        if (gitBranchDescriptor != null) {
            String sha = findShaOfLatestScannedCommitOfBranch(store, gitBranchDescriptor.getName());
            if (sha != null) {
                range = sha + ".." + untilString;
                LOGGER.info("Found already scanned commit with SHA: {} using it as range...", sha);
            } else {
                LOGGER.warn("Could not find head of specified branch in database.");
                LOGGER.warn("Optimized scanning of commits thus not available, scan will do a full scan according to specified range.");
                LOGGER.warn("Consider doing a full scan of the git repository or specify a range using commit SHAs that include the current head of the desired branch.");
            }
        } else {
            LOGGER.info("No commit found - Repository was not yet scanned, doing scan according to specified range");
        }
    }

    private void addAdditionalRelations() {
        authorCache.getAuthors().forEach(gitAuthor -> gitRepositoryDescriptor.getAuthors().add(gitAuthor));
        committerCache.getCommiters().forEach(gitCommitter -> gitRepositoryDescriptor.getCommitters().add(gitCommitter));
        fileCache.getFiles().forEach(gitFile -> gitRepositoryDescriptor.getFiles().add(gitFile));
    }

    private void adjustGitHead(JGitRepository jGitRepository) throws IOException {
        GitBranch head = jGitRepository.findHead();
        GitCommitDescriptor headDescriptor = commitCache.get(head.getCommitSha());
        gitRepositoryDescriptor.setHead(headDescriptor);
    }

    private void storeCommits(List<GitCommit> newCommits) {
        storeCommitNodes(newCommits);
        addParentRelationship(newCommits);
    }

    private void storeCommitNodes(List<GitCommit> newCommits) {
        for (GitCommit gitCommit : newCommits) {
            GitCommitDescriptor descriptor = commitCache.createDescriptorForCommit(gitCommit);

            addCommitForRepository(descriptor);
            addCommitForAuthor(gitCommit.getAuthor(), descriptor);
            addCommitForCommitter(gitCommit.getCommitter(), descriptor);

            addCommitChanges(gitCommit, descriptor);
        }
    }

    private void addCommitForRepository(GitCommitDescriptor descriptor) {
        gitRepositoryDescriptor.getCommits().add(descriptor);
    }

    private void addCommitForAuthor(final String author, final GitCommitDescriptor gitCommit) {
        if (author == null) return;

        GitAuthorDescriptor authorDescriptor = authorCache.findOrCreate(author);
        authorDescriptor.getCommits().add(gitCommit);
    }


    private void addCommitForCommitter(final String committer, final GitCommitDescriptor gitCommit) {
        if (committer == null) return;

        GitCommitterDescriptor committerDescriptor = committerCache.findOrCreate(committer);
        committerDescriptor.getCommits().add(gitCommit);
    }


    void addCommitChanges(final GitCommit gitCommit, final GitCommitDescriptor gitCommitDescriptor) {
        for (GitChange gitChange : gitCommit.getGitChanges()) {
            GitChangeDescriptor gitChangeDescriptor = store.create(GitChangeDescriptor.class);
            gitChangeDescriptor.setModificationKind(gitChange.getModificationKind());
            gitCommitDescriptor.getChanges().add(gitChangeDescriptor);
            fileAnalyzer.addAsGitFile(gitChange, gitChangeDescriptor, gitCommit.getDate());
        }
    }

    private void addParentRelationship(List<GitCommit> newCommits) {
        for (GitCommit gitCommit : newCommits) {
            String sha = gitCommit.getSha();
            GitCommitDescriptor gitCommitDescriptor = commitCache.get(sha);
            for (GitCommit parent : gitCommit.getParents()) {
                String parentSha = parent.getSha();
                GitCommitDescriptor parentCommit = commitCache.get(parentSha);
                if (null == parentCommit) {
                    LOGGER.warn ("Cannot add (parent) commit with SHA '{}' (excluded by range?)", parentSha);
                } else {
                    gitCommitDescriptor.getParents().add(parentCommit);
                }
            }
        }
    }

    private void storeBranches(List<GitBranch> branches) {
        for (GitBranch gitBranch : branches) {
            GitBranchDescriptor gitBranchDescriptor = branchCache.findOrCreate(gitBranch);
            GitCommitDescriptor gitCommitDescriptor = commitCache.get(gitBranch.getCommitSha());
            if (null == gitCommitDescriptor) {
                LOGGER.warn ("Cannot retrieve commit '{}' for branch '{}'", gitBranch.getCommitSha(), gitBranchDescriptor.getName());
            }
            gitBranchDescriptor.setHead(gitCommitDescriptor);
            if (!gitRepositoryDescriptor.getBranches().contains(gitBranchDescriptor)){
                gitRepositoryDescriptor.getBranches().add(gitBranchDescriptor);
            }
        }
    }

    private void storeTags(List<GitTag> tags) {
        for (GitTag gitTag : tags) {
            GitTagDescriptor gitTagDescriptor = tagCache.findOrCreate(gitTag);
            GitCommitDescriptor gitCommitDescriptor = commitCache.get(gitTag.getCommitSha());
            if (null == gitCommitDescriptor) {
                LOGGER.warn ("Cannot retrieve commit '{}' for tag '{}'", gitTag.getCommitSha(), gitTagDescriptor.getLabel());
            }
            gitTagDescriptor.setCommit(gitCommitDescriptor);
            gitRepositoryDescriptor.getTags().add(gitTagDescriptor);
        }
    }

}
