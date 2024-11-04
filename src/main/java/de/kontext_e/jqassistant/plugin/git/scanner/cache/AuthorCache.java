package de.kontext_e.jqassistant.plugin.git.scanner.cache;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitAuthorDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.kontext_e.jqassistant.plugin.git.scanner.GitScannerPlugin.isFreshScan;
import static de.kontext_e.jqassistant.plugin.git.scanner.JQAssistantGitRepository.getAuthorDescriptorFromDB;

public class AuthorCache {


    private final Map<String, GitAuthorDescriptor> authors = new HashMap<>();
    private final Store store;

    public AuthorCache(final Store store) {
        this.store = store;
    }

    public List<GitAuthorDescriptor> getAuthors() {
        return new ArrayList<>(authors.values());
    }

    public GitAuthorDescriptor findOrCreate(final String identString) {
        GitAuthorDescriptor authorDescriptor = find(identString);
        if (authorDescriptor != null) {
            return authorDescriptor;
        }
        return createAuthorDescriptor(identString);
    }

    public GitAuthorDescriptor find(final String identString) {
        if (authors.containsKey(identString)) {
            return authors.get(identString);
        } else {
            if (isFreshScan) return null;
            GitAuthorDescriptor authorDescriptor = getAuthorDescriptorFromDB(store, identString);
            if (authorDescriptor != null) {
                addToCache(authorDescriptor);
            }
            return authorDescriptor;
        }
    }

    private void addToCache(final GitAuthorDescriptor author) {
        authors.put(author.getIdentString(), author);
    }

    private GitAuthorDescriptor createAuthorDescriptor(String identString) {
        if (identString == null) { throw new IllegalArgumentException("Authors' identity String is null"); }
        GitAuthorDescriptor gitAuthor = store.create(GitAuthorDescriptor.class);

        gitAuthor.setIdentString(identString);
        gitAuthor.setName(nameFrom(identString));
        gitAuthor.setEmail(emailFrom(identString));

        addToCache(gitAuthor);
        return gitAuthor;
    }

    private String emailFrom(String author) {
        return author.substring(author.indexOf("<")+1, author.indexOf(">")).trim();
    }

    private String nameFrom(String author) {
        return author.substring(0, author.indexOf("<")).trim();
    }


}
