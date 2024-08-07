package de.kontext_e.jqassistant.plugin.git.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.xo.api.Query;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitBranchDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitCommitDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class JQAssistantDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantDB.class);

    static Map<String, GitBranchDescriptor> importExistingBranchesFromStore(Store store) {
        String query = "Match (b:Branch) return b";
        try (Query.Result<Query.Result.CompositeRowObject> result = store.executeQuery(query)){
            Map<String, GitBranchDescriptor> branches = new HashMap<>();
            for (Query.Result.CompositeRowObject row : result) {
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
        try (Query.Result<Query.Result.CompositeRowObject> queryResult = store.executeQuery(query)){
            return queryResult.iterator().next().get("c", GitCommitDescriptor.class);
        } catch (Exception e) {
            LOGGER.error("Error while looking for most recent scanned commit: "+ e);
            return null;
        }
    }

}
