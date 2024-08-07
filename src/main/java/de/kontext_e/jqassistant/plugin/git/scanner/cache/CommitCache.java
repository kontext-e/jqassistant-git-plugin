package de.kontext_e.jqassistant.plugin.git.scanner.cache;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitCommitDescriptor;

import java.util.HashMap;
import java.util.Map;

import static de.kontext_e.jqassistant.plugin.git.scanner.JQAssistantDB.getCommitDescriptorFromDB;

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
            GitCommitDescriptor commitDescriptor = getCommitDescriptorFromDB(store, sha);
            addToCache(commitDescriptor);
            return commitDescriptor;
        }
    }

}
