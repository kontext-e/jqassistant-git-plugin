package de.kontext_e.jqassistant.plugin.git.scanner.cache;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitCommitterDescriptor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static de.kontext_e.jqassistant.plugin.git.scanner.GitScannerPlugin.isFreshScan;

public class CommitterCache {

    private final Map<String, GitCommitterDescriptor> committers = new HashMap<>();
    private final Store store;

    public CommitterCache(final Store store) {
        this.store = store;
    }

    public GitCommitterDescriptor findOrCreate(final String identString) {
        GitCommitterDescriptor committerDescriptor = find(identString);
        if (committerDescriptor != null) {
            return committerDescriptor;
        }
        return createCommitterDescriptor(identString);
    }

    public List<GitCommitterDescriptor> getCommitters(){
        return new LinkedList<>(committers.values());
    }

    public GitCommitterDescriptor find(final String identString) {
        if (committers.containsKey(identString)) {
            return committers.get(identString);
        } else {
            if (isFreshScan) return null;
            GitCommitterDescriptor committerDescriptor = getCommitterDescriptorFromDB(store, identString);
            if (committerDescriptor != null) {
                addToCache(committerDescriptor);
            }
            return committerDescriptor;
        }
    }

    private void addToCache(final GitCommitterDescriptor committer){
        committers.put(committer.getIdentString(), committer);
    }

    private GitCommitterDescriptor createCommitterDescriptor(final String identString) {
        if (identString == null) { throw new IllegalArgumentException("Committers' identity string is Null"); }
        GitCommitterDescriptor committerDescriptor = store.create(GitCommitterDescriptor.class);

        committerDescriptor.setIdentString(identString);
        committerDescriptor.setName(nameFrom(identString));
        committerDescriptor.setEmail(emailFrom(identString));

        addToCache(committerDescriptor);
        return committerDescriptor;
    }

    private String emailFrom(String author) {
        return author.substring(author.indexOf("<")+1, author.indexOf(">")).trim();
    }

    private String nameFrom(String author) {
        return author.substring(0, author.indexOf("<")).trim();
    }
}
