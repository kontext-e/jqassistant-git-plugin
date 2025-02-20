package de.kontext_e.jqassistant.plugin.git.store.descriptor;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Copy")
public interface GitCopyChangeDescriptor extends GitChangeDescriptor {

    @Relation("COPIES")
    GitFileDescriptor getCopies();
    void setCopies(GitFileDescriptor gitFileDescriptor);

    @Relation("CREATES")
    GitFileDescriptor getCreates();
    void setCreates(GitFileDescriptor gitFileDescriptor);
}
