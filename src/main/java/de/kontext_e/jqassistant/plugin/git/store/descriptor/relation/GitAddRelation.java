package de.kontext_e.jqassistant.plugin.git.store.descriptor.relation;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.change.GitChangeDescriptor;

@Relation("CREATES")
public interface GitAddRelation extends Descriptor {

    @Relation.Outgoing
    GitChangeDescriptor getCreatingChange();
    void setCreatingChange(GitChangeDescriptor gitChangeDescriptor);

    @Relation.Incoming
    GitFileDescriptor getCreatedFile();
    void setCreatedFile(GitFileDescriptor gitFileDescriptor);

    @Property("createdAtEpoch")
    Long getCreatedAtEpoch();
    void setCreatedAtEpoch(Long relativePathEpoch);

    @Property("createdAt")
    String getCreatedAt();
    void setCreatedAt(String relativePath);
}
