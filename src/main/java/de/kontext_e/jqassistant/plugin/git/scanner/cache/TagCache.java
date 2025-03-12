package de.kontext_e.jqassistant.plugin.git.scanner.cache;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitTag;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitRepositoryDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitTagDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static de.kontext_e.jqassistant.plugin.git.scanner.repositories.JQAssistantGitRepository.importExistingTagsFromStore;

public class TagCache {

    private final Logger LOGGER = LoggerFactory.getLogger(TagCache.class);
    private final Map<String, GitTagDescriptor> tags;
    private final Store store;

    public TagCache(Store store, GitRepositoryDescriptor gitRepositoryDescriptor) {
        this.store = store;
        this.tags = importExistingTagsFromStore(store, gitRepositoryDescriptor);
    }

    public GitTagDescriptor findOrCreate(GitTag gitTag){
        String tagLabel = gitTag.getLabel().replaceFirst("refs/tags/", "");

        if (tags.containsKey(tagLabel)) {
            return tags.get(tagLabel);
        }

        return createTagDescriptor(tagLabel);
    }

    private GitTagDescriptor createTagDescriptor(String label) {
        LOGGER.debug("Adding new Tag '{}'", label);
        GitTagDescriptor gitTagDescriptor = store.create(GitTagDescriptor.class);

        gitTagDescriptor.setLabel(label);

        tags.put(label, gitTagDescriptor);
        return gitTagDescriptor;
    }

}
