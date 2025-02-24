package de.kontext_e.jqassistant.plugin.git.store.descriptor.change;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitAddRelation;

@Label("Create")
public interface GitAddChangeDescriptor extends GitChangeDescriptor {

    @Relation.Outgoing
    GitAddRelation getCreates();
    void setCreates(GitAddRelation gitAddRelation);

}
