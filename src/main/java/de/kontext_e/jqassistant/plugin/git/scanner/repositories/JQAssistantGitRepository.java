package de.kontext_e.jqassistant.plugin.git.scanner.repositories;

import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.xo.api.Query.Result;
import com.buschmais.xo.api.Query.Result.CompositeRowObject;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Lookups of already scanned nodes.
 * <p>
 * Every query is anchored on the repository being scanned. None of the
 * identifying properties is globally unique: the same commit SHA occurs in
 * forks and mirrors, a relative path such as {@code pom.xml} occurs in almost
 * every repository, and branch names like {@code heads/master} occur in all of
 * them. Since each lookup returns the first row, an unanchored query would
 * hand the scanner an arbitrary node belonging to some other repository.
 */
public class JQAssistantGitRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantGitRepository.class);

    public static Map<String, GitBranchDescriptor> importExistingBranchesFromStore(Store store, GitRepositoryDescriptor gitRepositoryDescriptor) {
        String query = "MATCH (repo:Git:Repository)-[:HAS_BRANCH]->(branch:Branch) WHERE repo.fileName = $path RETURN branch";
        try (Result<CompositeRowObject> result = store.executeQuery(query, Map.of("path", gitRepositoryDescriptor.getFileName()))){
            Map<String, GitBranchDescriptor> branches = new HashMap<>();
            for (CompositeRowObject row : result) {
                GitBranchDescriptor descriptor = row.get("branch", GitBranchDescriptor.class);
                branches.put(descriptor.getName(), descriptor);
            }
            return branches;
        } catch (Exception e) {
            LOGGER.error("Error while importing existing git branches", e);
        }
        return new HashMap<>();
    }

    public static Map<String, GitTagDescriptor> importExistingTagsFromStore(Store store, GitRepositoryDescriptor gitRepositoryDescriptor) {
        String query = "MATCH (repo:Git:Repository)-[:HAS_TAG]->(t:Tag) WHERE repo.fileName = $path RETURN t";
        try (Result<CompositeRowObject> result = store.executeQuery(query,  Map.of("path", gitRepositoryDescriptor.getFileName()))){
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

    public static String findShaOfLatestScannedCommitOfBranch(Store store, GitRepositoryDescriptor gitRepositoryDescriptor, String branch) {
        String query = "MATCH (repo:Git:Repository)-[:HAS_BRANCH]->(b:Branch)-[:HAS_HEAD]->(n:Commit) " +
                       "WHERE repo.fileName = $path AND b.name = $name RETURN n.sha";
        try (Result<CompositeRowObject> result = store.executeQuery(query, Map.of("path", gitRepositoryDescriptor.getFileName(), "name", branch))) {
            return result.iterator().next().get("n.sha", String.class);
        } catch (Exception e) {
            LOGGER.debug("Error while looking for most recent scanned commit: {}", String.valueOf(e));
            return null;
        }
    }

    public static GitRepositoryDescriptor getExistingRepositoryDescriptor(Store store, String absolutePath) {
        String query = "MATCH (c:Git:Repository) where c.fileName = $path return c";
        try (Result<CompositeRowObject> result = store.executeQuery(query, Map.of("path", absolutePath))) {
            return result.iterator().next().get("c", GitRepositoryDescriptor.class);
        } catch (Exception e) {
            LOGGER.debug("Error while looking for existing git repository: {}", String.valueOf(e));
            return null;
        }
    }

    public static GitCommitDescriptor getCommitDescriptorFromDB(Store store, GitRepositoryDescriptor gitRepositoryDescriptor, String sha) {
        String query = "MATCH (repo:Git:Repository)-[:HAS_COMMIT]->(c:Commit) WHERE repo.fileName = $path AND c.sha = $sha RETURN c";
        try (Result<CompositeRowObject> result = store.executeQuery(query, Map.of("path", gitRepositoryDescriptor.getFileName(), "sha", sha))) {
            return result.iterator().next().get("c", GitCommitDescriptor.class);
        } catch (NoSuchElementException e){
            return null;
        }
    }

    public static GitAuthorDescriptor getAuthorDescriptorFromDB(Store store, GitRepositoryDescriptor gitRepositoryDescriptor, String identString) {
        String query = "MATCH (repo:Git:Repository)-[:HAS_AUTHOR]->(a:Author) WHERE repo.fileName = $path AND a.identString = $ident RETURN a";
        try (Result<CompositeRowObject> result = store.executeQuery(query, Map.of("path", gitRepositoryDescriptor.getFileName(), "ident", identString))) {
            return result.iterator().next().get("a", GitAuthorDescriptor.class);
        } catch (NoSuchElementException e){
            return null;
        }
    }

    public static GitCommitterDescriptor getCommitterDescriptorFromDB(Store store, GitRepositoryDescriptor gitRepositoryDescriptor, String identString) {
        String query = "MATCH (repo:Git:Repository)-[:HAS_COMMITTER]->(c:Committer) WHERE repo.fileName = $path AND c.identString = $ident RETURN c";
        try (Result<CompositeRowObject> result = store.executeQuery(query, Map.of("path", gitRepositoryDescriptor.getFileName(), "ident", identString))) {
            return result.iterator().next().get("c", GitCommitterDescriptor.class);
        } catch (NoSuchElementException e){
            return null;
        }
    }

    public static GitFileDescriptor getFileDescriptorFromDB(Store store, GitRepositoryDescriptor gitRepositoryDescriptor, String relativePath) {
        String query = "MATCH (repo:Git:Repository)-[:HAS_FILE]->(f:Git:File) WHERE repo.fileName = $path AND f.relativePath = $relativePath RETURN f";
        try (Result<CompositeRowObject> result = store.executeQuery(query, Map.of("path", gitRepositoryDescriptor.getFileName(), "relativePath", relativePath))) {
            return result.iterator().next().get("f", GitFileDescriptor.class);
        } catch (NoSuchElementException e){
            return null;
        }
    }

}
