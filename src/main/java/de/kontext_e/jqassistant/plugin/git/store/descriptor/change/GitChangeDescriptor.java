package de.kontext_e.jqassistant.plugin.git.store.descriptor.change;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;

@Label("Change")
public interface GitChangeDescriptor extends GitDescriptor {

    @Property("modificationKind")
    String getModificationKind();
    void setModificationKind(String modificationKind);

    @Relation("MODIFIES")
    GitFileDescriptor getModifies();
    void setModifies(GitFileDescriptor gitFileDescriptor);

}
