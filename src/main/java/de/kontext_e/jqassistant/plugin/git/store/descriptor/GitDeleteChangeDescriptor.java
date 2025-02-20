package de.kontext_e.jqassistant.plugin.git.store.descriptor;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Delete")
public interface GitDeleteChangeDescriptor extends GitChangeDescriptor{

    @Relation("DELETES")
    GitFileDescriptor getDeletes();
    void setDeletes(GitFileDescriptor gitFileDescriptor);

}