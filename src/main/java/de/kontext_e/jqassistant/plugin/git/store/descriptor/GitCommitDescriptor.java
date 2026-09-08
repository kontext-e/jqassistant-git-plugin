package de.kontext_e.jqassistant.plugin.git.store.descriptor;

import com.buschmais.xo.neo4j.api.annotation.Indexed;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.change.GitChangeDescriptor;

import java.util.List;

@Label("Commit")
public interface GitCommitDescriptor extends GitDescriptor {

    // Indexed: JQAssistantGitRepository#getCommitDescriptorFromDB looks commits up by
    // SHA ("MATCH (c:Commit) WHERE c.sha = $sha"). Without an index every lookup is a
    // full scan of the :Commit label, which dominates incremental scans and any
    // downstream plugin resolving commits from the store. Not unique: the same SHA
    // legitimately appears in several repositories (forks, shared history).
    @Indexed
    @Property("sha")
    String getSha();
    void setSha(String sha);

    @Property("author")
    String getAuthor();
    void setAuthor(String author);

    @Property("committer")
    String getCommitter();
    void setCommitter(String committer);

    @Property("date")
    String getDate();
    void setDate(String date);

    @Property("time")
    String getTime();
    void setTime(String time);

    @Property("epoch")
    Long getEpoch();
    void setEpoch(Long epoch);

    @Property("message")
    String getMessage();
    void setMessage(String message);

    @Property("shortMessage")
    String getShortMessage();
    void setShortMessage(String message);

    @Property("encoding")
    String getEncoding();
    void setEncoding(String encoding);

    @Relation("CONTAINS_CHANGE")
    List<GitChangeDescriptor> getChanges();

    @Relation("HAS_PARENT")
    List<GitCommitDescriptor> getParents();
}
