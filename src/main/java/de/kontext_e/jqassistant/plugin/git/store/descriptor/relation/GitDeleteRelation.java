package de.kontext_e.jqassistant.plugin.git.store.descriptor.relation;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.change.GitChangeDescriptor;

@Relation("DELETES")
public interface GitDeleteRelation extends Descriptor {

    @Relation.Outgoing
    GitChangeDescriptor getDeletingChange();
    void setDeletingChange(GitChangeDescriptor deletingChange);

    @Relation.Incoming
    GitFileDescriptor getDeletedFile();
    void setDeletedFile(GitFileDescriptor deletedFile);

    @Property("deletedAt")
    String getDeletedAt();
    void setDeletedAt(String deletedAt);

    @Property("deletedAtEpoch")
    Long getDeletedAtEpoch();
    void setDeletedAtEpoch(Long deletedAtEpoch);
}
