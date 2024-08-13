package de.kontext_e.jqassistant.plugin.git.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.git.scanner.cache.AuthorCache;
import de.kontext_e.jqassistant.plugin.git.scanner.cache.CommitCache;
import de.kontext_e.jqassistant.plugin.git.scanner.cache.CommitterCache;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitBranch;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitChange;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitCommit;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitTag;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitAuthorDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitBranchDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitChangeDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitCommitDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitCommitterDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitRepositoryDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitTagDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.kontext_e.jqassistant.plugin.git.scanner.JQAssistantGitRepository.*;

public class GitRepositoryScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitRepositoryScanner.class);

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss Z");
    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    private final Store store;
    private final GitRepositoryDescriptor gitRepositoryDescriptor;
    private final CommitCache commitCache;
    private final AuthorCache authorCache;
    private final CommitterCache committerCache;
    private String range;
    private final Map<String, GitFileDescriptor> files = new HashMap<>();
    private final Map<String, GitBranchDescriptor> branches;
    private final Map<String, GitTagDescriptor> tags;

    GitRepositoryScanner(final Store store, final GitRepositoryDescriptor gitRepositoryDescriptor, final String range) {
        this.store = store;
        this.gitRepositoryDescriptor = gitRepositoryDescriptor;
        this.range = range;

        this.commitCache = new CommitCache(store);
        this.authorCache = new AuthorCache(store);
        this.committerCache = new CommitterCache(store);

        this.branches = importExistingBranchesFromStore(store);
        this.tags = importExistingTagsFromStore(store);
    }

    void scanGitRepo() throws IOException {
        GitCommitDescriptor latestScannedCommit = getLatestScannedCommit(store);
        if (latestScannedCommit != null) {
            //Override range with last scanned Commit to avoid unnecessary scanning.
            range = latestScannedCommit.getSha() + ".."; //TODO make behaviour configurable
            LOGGER.debug("Found already scanned commit with sha: " + latestScannedCommit.getSha() + " using it as range...");
        }
        LOGGER.debug("No commit found - Repository was not yet scanned, doing scan according to specified range");

        JGitRepository jGitRepository = new JGitRepository(gitRepositoryDescriptor.getFileName(), range);

        List<GitCommit> newCommits = jGitRepository.findCommits();

        storeCommits(newCommits);
        addBranches(jGitRepository.findBranches());
        addTags(jGitRepository.findTags());

        authorCache.getAuthors().forEach(gitAuthor -> gitRepositoryDescriptor.getAuthors().add(gitAuthor));
        committerCache.getCommiters().forEach(gitCommitter -> gitRepositoryDescriptor.getCommitters().add(gitCommitter));
        //TODO Avoid duplicate scanning
        files.values().forEach(gitFile -> gitRepositoryDescriptor.getFiles().add(gitFile));

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
            addCommitFiles(gitCommit, descriptor);
        }
    }

    private void addCommitForRepository(GitCommitDescriptor descriptor) {
        gitRepositoryDescriptor.getCommits().add(descriptor);
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

    private void addBranches(List<GitBranch> branches) {
        for (GitBranch gitBranch : branches) {
            GitBranchDescriptor gitBranchDescriptor = findOrCreateGitBranchDescriptor(gitBranch);
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

    private GitBranchDescriptor findOrCreateGitBranchDescriptor(GitBranch gitBranch) {
        String name = gitBranch.getName().replaceFirst("refs/", "");
        if (branches.containsKey(name)){
            return branches.get(name);
        }
        GitBranchDescriptor gitBranchDescriptor = store.create(GitBranchDescriptor.class);
        String sha = gitBranch.getCommitSha();
        LOGGER.debug ("Adding new Branch '{}' with Head '{}'", name, sha);
        gitBranchDescriptor.setName(name);
        return gitBranchDescriptor;
    }

    private void addTags(List<GitTag> tags) {
        for (GitTag gitTag : tags) {
            GitTagDescriptor gitTagDescriptor = findOrCreateTagDescriptor(gitTag);
            GitCommitDescriptor gitCommitDescriptor = commitCache.get(gitTag.getCommitSha());
            if (null == gitCommitDescriptor) {
                LOGGER.warn ("Cannot retrieve commit '{}' for tag '{}'", gitTag.getCommitSha(), gitTagDescriptor.getLabel());
            }
            gitTagDescriptor.setCommit(gitCommitDescriptor);
            gitRepositoryDescriptor.getTags().add(gitTagDescriptor);
        }
    }

    private GitTagDescriptor findOrCreateTagDescriptor(GitTag gitTag){
        String label = gitTag.getLabel().replaceFirst("refs/tags/", "");
        if (tags.containsKey(label)){
            return tags.get(label);
        }
        GitTagDescriptor gitTagDescriptor = store.create(GitTagDescriptor.class);
        String sha = gitTag.getCommitSha();
        LOGGER.debug ("Adding new Tag '{}' with Commit '{}'", label, sha);
        gitTagDescriptor.setLabel(label);
        return gitTagDescriptor;
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

    private void addCommitFiles(final GitCommit gitCommit, final GitCommitDescriptor gitCommitDescriptor) {
        for (GitChange gitChange : gitCommit.getGitChanges()) {
            GitChangeDescriptor gitChangeDescriptor = store.create(GitChangeDescriptor.class);
            gitChangeDescriptor.setModificationKind(gitChange.getModificationKind());
            gitCommitDescriptor.getChanges().add(gitChangeDescriptor);
            addAsGitFile(gitChange, gitChangeDescriptor, gitCommit.getDate());
        }
    }

    private void addAsGitFile(GitChange gitChange, final GitChangeDescriptor gitChangeDescriptor, final Date date) {
        final GitFileDescriptor gitFileDescriptor = getOrCreateGitFileDescriptor(gitChange.getRelativePath());

        gitChangeDescriptor.setModifies(gitFileDescriptor);

        if(isAddChange(gitChangeDescriptor)) {
            gitFileDescriptor.setCreatedAt(DATE_TIME_FORMAT.format(date));
            gitFileDescriptor.setCreatedAtEpoch(date.getTime());
            gitChangeDescriptor.setCreates(gitFileDescriptor);
        } else if(isUpdateChange(gitChangeDescriptor)) {
            gitChangeDescriptor.setUpdates(gitFileDescriptor);
            if (gitFileDescriptor.getLastModificationAtEpoch() == null || date.getTime() < gitFileDescriptor.getLastModificationAtEpoch()){
                gitFileDescriptor.setLastModificationAt(DATE_TIME_FORMAT.format(date));
                gitFileDescriptor.setLastModificationAtEpoch(date.getTime());
            }
        } else if(isDeleteChange(gitChangeDescriptor)) {
            gitFileDescriptor.setDeletedAt(DATE_TIME_FORMAT.format(date));
            gitFileDescriptor.setDeletedAtEpoch(date.getTime());
            gitChangeDescriptor.setDeletes(gitFileDescriptor);
        } else if(isRenameChange(gitChangeDescriptor)) {
            final GitFileDescriptor oldFile = getOrCreateGitFileDescriptor(gitChange.getOldPath());
            final GitFileDescriptor newFile = getOrCreateGitFileDescriptor(gitChange.getNewPath());
            oldFile.setHasNewName(newFile);
            gitChangeDescriptor.setRenames(oldFile);
            gitChangeDescriptor.setDeletes(oldFile);
            gitChangeDescriptor.setCreates(newFile);
        } else if(isCopyChange(gitChangeDescriptor)) {
            final GitFileDescriptor oldFile = getOrCreateGitFileDescriptor(gitChange.getOldPath());
            final GitFileDescriptor newFile = getOrCreateGitFileDescriptor(gitChange.getNewPath());
            newFile.setCopyOf(oldFile);
            gitChangeDescriptor.setCopies(oldFile);
            gitChangeDescriptor.setCreates(newFile);
        }
    }

    private boolean isCopyChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "C".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private boolean isRenameChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "R".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private boolean isDeleteChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "D".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private boolean isUpdateChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "M".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private boolean isAddChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "A".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private GitFileDescriptor getOrCreateGitFileDescriptor(String relativePath) {
        GitFileDescriptor gitFileDescriptor = files.get(relativePath);
        if(gitFileDescriptor == null) {
            gitFileDescriptor = store.create(GitFileDescriptor.class);
            gitFileDescriptor.setRelativePath(relativePath);
            files.put(relativePath, gitFileDescriptor);
        }
        return gitFileDescriptor;
    }

}
