package de.kontext_e.jqassistant.plugin.git.scanner.cache;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitCommit;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitCommitDescriptor;

import java.util.HashMap;
import java.util.Map;

import static de.kontext_e.jqassistant.plugin.git.scanner.GitRepositoryScanner.DATE_FORMAT;
import static de.kontext_e.jqassistant.plugin.git.scanner.GitRepositoryScanner.TIME_FORMAT;
import static de.kontext_e.jqassistant.plugin.git.scanner.GitScannerPlugin.isFreshScan;
import static de.kontext_e.jqassistant.plugin.git.scanner.JQAssistantGitRepository.getCommitDescriptorFromDB;

public class CommitCache {

    private final Map<String, GitCommitDescriptor> commits = new HashMap<>();
    private final Store store;

    public CommitCache(final Store store) {
        this.store = store;
    }

    public void addToCache(final GitCommitDescriptor commit) {
        commits.put(commit.getSha(), commit);
    }

    public GitCommitDescriptor get(final String sha) {
        if (commits.containsKey(sha)) {
            return commits.get(sha);
        } else {
            if (isFreshScan) return null;
            GitCommitDescriptor commitDescriptor = getCommitDescriptorFromDB(store, sha);
            if (commitDescriptor != null) {
                addToCache(commitDescriptor);
            }
            return commitDescriptor;
        }
    }

    public GitCommitDescriptor createDescriptorForCommit(GitCommit gitCommit) {
        GitCommitDescriptor gitCommitDescriptor = store.create(GitCommitDescriptor.class);
        gitCommitDescriptor.setSha(gitCommit.getSha());
        gitCommitDescriptor.setAuthor(gitCommit.getAuthor());
        gitCommitDescriptor.setCommitter(gitCommit.getCommitter());
        gitCommitDescriptor.setDate(DATE_FORMAT.format(gitCommit.getDate()));
        gitCommitDescriptor.setMessage(gitCommit.getMessage());
        gitCommitDescriptor.setShortMessage(gitCommit.getShortMessage());
        gitCommitDescriptor.setEpoch(gitCommit.getDate().getTime());
        gitCommitDescriptor.setTime(TIME_FORMAT.format(gitCommit.getDate()));
        gitCommitDescriptor.setEncoding(gitCommit.getEncoding());

        addToCache(gitCommitDescriptor);

        return gitCommitDescriptor;
    }

}
