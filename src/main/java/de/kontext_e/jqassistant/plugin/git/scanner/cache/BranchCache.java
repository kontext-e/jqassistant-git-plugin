package de.kontext_e.jqassistant.plugin.git.scanner.cache;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitBranch;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitBranchDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static de.kontext_e.jqassistant.plugin.git.scanner.JQAssistantGitRepository.importExistingBranchesFromStore;

public class BranchCache {

    private final Logger LOGGER = LoggerFactory.getLogger(BranchCache.class);
    private final Map<String, GitBranchDescriptor> branches;
    private final Store store;

    public BranchCache(Store store) {
        this.store = store;
        this.branches = importExistingBranchesFromStore(store);
    }

    public GitBranchDescriptor findOrCreate(GitBranch gitBranch) {
        String name = gitBranch.getName().replaceFirst("refs/", "");

        if (branches.containsKey(name)){
            return branches.get(name);
        }

        return createBranchDescriptor(gitBranch, name);
    }

    public GitBranchDescriptor find(String name){
        if (branches.containsKey(name)){
            return branches.get(name);
        }

        if (branches.containsKey("heads/" + name)) {
            return branches.get("heads/" + name);
        }

        return null;
    }

    private GitBranchDescriptor createBranchDescriptor(GitBranch gitBranch, String name) {
        LOGGER.debug ("Adding new Branch '{}' with Head '{}'", name, gitBranch.getCommitSha());
        GitBranchDescriptor gitBranchDescriptor = store.create(GitBranchDescriptor.class);

        String sha = gitBranch.getCommitSha();
        gitBranchDescriptor.setName(name);

        branches.put(name, gitBranchDescriptor);
        return gitBranchDescriptor;
    }

}
