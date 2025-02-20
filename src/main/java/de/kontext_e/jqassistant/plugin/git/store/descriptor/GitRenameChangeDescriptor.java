package de.kontext_e.jqassistant.plugin.git.store.descriptor;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Rename")
public interface GitRenameChangeDescriptor extends GitChangeDescriptor {

    @Relation("RENAMES")
    GitFileDescriptor getRenames();
    void setRenames(GitFileDescriptor gitFileDescriptor);

    @Relation("DELETES")
    GitFileDescriptor getDeletes();
    void setDeletes(GitFileDescriptor gitFileDescriptor);

    @Relation("CREATES")
    GitFileDescriptor getCreates();
    void setCreates(GitFileDescriptor gitFileDescriptor);
}
