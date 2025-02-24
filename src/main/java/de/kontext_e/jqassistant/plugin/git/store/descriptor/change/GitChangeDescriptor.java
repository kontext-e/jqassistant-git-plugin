package de.kontext_e.jqassistant.plugin.git.store.descriptor.change;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitAddRelation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitDeleteRelation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitUpdateRelation;

@Label("Change")
public interface GitChangeDescriptor extends GitDescriptor {

    @Property("modificationKind")
    String getModificationKind();
    void setModificationKind(String modificationKind);

    @Relation("MODIFIES")
    GitFileDescriptor getModifies();
    void setModifies(GitFileDescriptor gitFileDescriptor);

    @Relation.Outgoing
    GitAddRelation getCreates();
    void setCreates(GitAddRelation gitAddRelation);

    @Relation.Outgoing
    GitDeleteRelation getDeletes();
    void setDeletes(GitDeleteRelation gitDeleteChangeDescriptor);

    @Relation.Outgoing
    GitUpdateRelation getUpdates();
    void setUpdates(GitUpdateRelation gitFileDescriptor);
}
