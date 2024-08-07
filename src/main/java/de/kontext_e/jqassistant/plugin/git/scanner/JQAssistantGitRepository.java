package de.kontext_e.jqassistant.plugin.git.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.xo.api.Query.Result;
import com.buschmais.xo.api.Query.Result.CompositeRowObject;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitBranchDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitCommitDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitRepositoryDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class JQAssistantDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantDB.class);

    static Map<String, GitBranchDescriptor> importExistingBranchesFromStore(Store store) {
        String query = "Match (b:Branch) return b";
        try (Result<CompositeRowObject> result = store.executeQuery(query)){
            Map<String, GitBranchDescriptor> branches = new HashMap<>();
            for (CompositeRowObject row : result) {
                GitBranchDescriptor descriptor = row.get("b", GitBranchDescriptor.class);
                branches.put(descriptor.getName(), descriptor);
            }
            return branches;
        } catch (Exception e) {
            LOGGER.error("Error while importing existing git branches", e);
        }
        return new HashMap<>();
    }

    static GitCommitDescriptor getLatestScannedCommit(Store store) {
        String query = "MATCH (c:Commit) return c order by c.epoch desc limit 1";
        try (Result<CompositeRowObject> queryResult = store.executeQuery(query)){
            return queryResult.iterator().next().get("c", GitCommitDescriptor.class);
        } catch (Exception e) {
            LOGGER.error("Error while looking for most recent scanned commit: "+ e);
            return null;
        }
    }

    static GitRepositoryDescriptor getExistingRepositoryDescriptor(Store store, String absolutePath) {
        String query = String.format("MATCH (c:Repository) where c.fileName = '%s' return c", absolutePath);
        try (Result<CompositeRowObject> result = store.executeQuery(query)){
            return result.iterator().next().get("c", GitRepositoryDescriptor.class);
        } catch (Exception e) {
            LOGGER.error("Error while looking for existing git repository: "+ e);
            return null;
        }
    }

}