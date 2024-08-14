package de.kontext_e.jqassistant.plugin.git.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.xo.api.Query.Result;
import com.buschmais.xo.api.Query.Result.CompositeRowObject;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class JQAssistantGitRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantGitRepository.class);

    public static Map<String, GitBranchDescriptor> importExistingBranchesFromStore(Store store) {
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

    public static Map<String, GitTagDescriptor> importExistingTagsFromStore(Store store) {
        String query = "Match (t:Tag) return t";
        try (Result<CompositeRowObject> result = store.executeQuery(query)){
            Map<String, GitTagDescriptor> tags = new HashMap<>();
            for (CompositeRowObject row : result) {
                GitTagDescriptor descriptor = row.get("t", GitTagDescriptor.class);
                tags.put(descriptor.getLabel(), descriptor);
            }
            return tags;
        } catch (Exception e) {
            LOGGER.error("Error while importing existing git tags", e);
        }
        return new HashMap<>();
    }

    static String findShaOfLatestScannedCommitOfBranch(Store store, String branch) {
        String query = String.format("MATCH (b:Branch)-[:HAS_HEAD]->(n:Commit) where b.name='%s' return n.sha", branch);
        try (Result<CompositeRowObject> result = store.executeQuery(query)){
            return result.iterator().next().get("n.sha", String.class);
        } catch (Exception e) {
            LOGGER.debug("Error while looking for most recent scanned commit: "+ e);
            return null;
        }
    }

    static GitRepositoryDescriptor getExistingRepositoryDescriptor(Store store, String absolutePath) {
        String query = String.format("MATCH (c:Repository) where c.fileName = '%s' return c", absolutePath);
        try (Result<CompositeRowObject> result = store.executeQuery(query)){
            return result.iterator().next().get("c", GitRepositoryDescriptor.class);
        } catch (Exception e) {
            LOGGER.debug("Error while looking for existing git repository: "+ e);
            return null;
        }
    }

    public static GitCommitDescriptor getCommitDescriptorFromDB(Store store, String sha) {
        String query = String.format("MATCH (c:Commit) where c.sha = '%s' return c", sha);
        try (Result<CompositeRowObject> result = store.executeQuery(query)){
            return result.iterator().next().get("c", GitCommitDescriptor.class);
        } catch (NoSuchElementException e){
            return null;
        }
    }

    public static GitAuthorDescriptor getAuthorDescriptorFromDB(Store store, String identString) {
        String query = String.format("MATCH (a:Author) where a.identString = '%s' return a", identString);
        try (Result<CompositeRowObject> result = store.executeQuery(query)){
            return result.iterator().next().get("a", GitAuthorDescriptor.class);
        } catch (NoSuchElementException e){
            return null;
        }
    }

    public static GitCommitterDescriptor getCommitterDescriptorFromDB(Store store, String identString) {
        String query = String.format("MATCH (c:Commiter) where c.identString = '%s' return c", identString);
        try (Result<CompositeRowObject> result = store.executeQuery(query)){
            return result.iterator().next().get("c", GitCommitterDescriptor.class);
        } catch (NoSuchElementException e){
            return null;
        }
    }

    public static GitFileDescriptor getFileDescriptorFromDB(Store store, String relativePath) {
        String query = String.format("MATCH (f:Git:File) where f.relativePath = '%s' return f", relativePath);
        try (Result<CompositeRowObject> result = store.executeQuery(query)){
            return result.iterator().next().get("f", GitFileDescriptor.class);
        } catch (NoSuchElementException e){
            return null;
        }
    }

}
