package de.kontext_e.jqassistant.plugin.git.store.descriptor;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Update")
public interface GitUpdateChangeDescriptor extends GitChangeDescriptor {

    @Relation("UPDATES")
    GitFileDescriptor getUpdates();
    void setUpdates(GitFileDescriptor gitFileDescriptor);

}
