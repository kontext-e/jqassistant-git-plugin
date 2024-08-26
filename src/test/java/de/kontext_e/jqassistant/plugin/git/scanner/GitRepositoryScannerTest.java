package de.kontext_e.jqassistant.plugin.git.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.core.test.plugin.AbstractPluginIT;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitRepositoryDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static org.mockito.Mockito.*;

class GitRepositoryScannerTest extends AbstractPluginIT {

    GitRepositoryDescriptor gitRepositoryDescriptor = mock();

    @BeforeEach
    public void beginTransaction() {
        store.beginTransaction();
        when(gitRepositoryDescriptor.getFileName()).thenReturn(".git");
    }

    @AfterEach
    public void commitTransaction() {
        store.commitTransaction();
    }

    @Test
    void testScanEmptyGitRepository() throws IOException {
        Store store = spy(super.store);
        JGitRepository jGitRepository = new JGitRepositoryGitMockBuilder().build();

        GitRepositoryScanner scanner = new GitRepositoryScanner(store, gitRepositoryDescriptor, null, jGitRepository);
        scanner.scanGitRepo();

        verify(store, never()).create(any());
    }

}
