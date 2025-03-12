package de.kontext_e.jqassistant.plugin.git.scanner.cache;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.kontext_e.jqassistant.plugin.git.scanner.repositories.JQAssistantGitRepository.getFileDescriptorFromDB;

public class FileCache {

    private final Map<String, GitFileDescriptor> files = new HashMap<>();
    private final Store store;

    public FileCache(Store store) {
        this.store = store;
    }

    public List<GitFileDescriptor> getFiles() {
        return new ArrayList<>(files.values());
    }

    public GitFileDescriptor findOrCreate(String relativePath) {
        GitFileDescriptor descriptor = find(relativePath);
        if (descriptor != null) {
            return descriptor;
        }
        return createFileDescriptor(relativePath);
    }

    public GitFileDescriptor find(String relativePath) {
        if (files.containsKey(relativePath)) {
            return files.get(relativePath);
        } else {
            if (isFreshScan) return null;
            GitFileDescriptor gitFileDescriptor = getFileDescriptorFromDB(store, relativePath);
            if (gitFileDescriptor != null) {
                addToCache(gitFileDescriptor);
            }
            return gitFileDescriptor;
        }
    }

    private void addToCache(GitFileDescriptor gitFileDescriptor) {
        files.put(gitFileDescriptor.getRelativePath(), gitFileDescriptor);
    }

    private GitFileDescriptor createFileDescriptor(String relativePath) {
        GitFileDescriptor gitFileDescriptor = store.create(GitFileDescriptor.class);

        gitFileDescriptor.setRelativePath(relativePath);

        addToCache(gitFileDescriptor);
        return gitFileDescriptor;
    }

}
