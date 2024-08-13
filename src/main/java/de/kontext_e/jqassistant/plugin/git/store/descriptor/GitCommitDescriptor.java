package de.kontext_e.jqassistant.plugin.git.store.descriptor;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Commit")
public interface GitCommitDescriptor extends GitDescriptor {

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
