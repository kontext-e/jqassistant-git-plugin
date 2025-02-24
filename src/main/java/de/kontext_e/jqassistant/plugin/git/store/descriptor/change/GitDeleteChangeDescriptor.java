package de.kontext_e.jqassistant.plugin.git.store.descriptor.change;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitDeleteRelation;

@Label("Delete")
public interface GitDeleteChangeDescriptor extends GitChangeDescriptor{

    @Relation.Outgoing
    GitDeleteRelation getDeletes();
    void setDeletes(GitDeleteRelation gitDeleteChangeDescriptor);

}