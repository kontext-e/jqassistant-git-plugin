package de.kontext_e.jqassistant.plugin.git.store.descriptor.change;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitUpdateRelation;

@Label("Update")
public interface GitUpdateChangeDescriptor extends GitChangeDescriptor {

    @Relation.Outgoing
    GitUpdateRelation getUpdates();
    void setUpdates(GitUpdateRelation gitFileDescriptor);

}
