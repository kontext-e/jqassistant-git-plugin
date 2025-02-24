package de.kontext_e.jqassistant.plugin.git.store.descriptor.change;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Create")
public interface GitAddChangeDescriptor extends GitChangeDescriptor {

    @Relation("CREATES")
    GitFileDescriptor getCreates();
    void setCreates(GitFileDescriptor gitFileDescriptor);

}
