package de.kontext_e.jqassistant.plugin.git.store.descriptor.relation;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.change.GitChangeDescriptor;

@Relation("UPDATES")
public interface GitUpdateRelation extends Descriptor {

    @Relation.Outgoing
    GitChangeDescriptor getUpdatingChange();
    void setUpdatingChange(GitChangeDescriptor updatingChange);

    @Relation.Incoming
    GitFileDescriptor getUpdatedFile();
    void setUpdatedFile(GitFileDescriptor updatedFile);


    @Property("modifiedAt")
    String getModificationAt();
    void setModificationAt(String modificationAt);


    @Property("modifiedAtEpoch")
    Long getModificationAtEpoch();
    void setModificationAtEpoch(Long modificationAtEpoch);

}
