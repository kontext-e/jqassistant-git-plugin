package de.kontext_e.jqassistant.plugin.git.store.descriptor;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.scanner.GitRepositoryScanner;

import java.util.Date;

@Label("File")
public interface GitFileDescriptor extends GitDescriptor {

    @Property("relativePath")
    String getRelativePath();
    void setRelativePath(String relativePath);

    @Property("createdAt")
    String getCreatedAt();
    void setCreatedAt(String relativePath);

    @Property("deletedAt")
    String getDeletedAt();
    void setDeletedAt(String deletedAt);

    @Property("lastModificationAt")
    String getLastModificationAt();
    void setLastModificationAt(String lastModificationAt);

    @Property("createdAtEpoch")
    Long getCreatedAtEpoch();
    void setCreatedAtEpoch(Long relativePathEpoch);

    @Property("deletedAtEpoch")
    Long getDeletedAtEpoch();
    void setDeletedAtEpoch(Long deletedAtEpoch);

    @Property("lastModificationAtEpoch")
    Long getLastModificationAtEpoch();
    void setLastModificationAtEpoch(Long lastModificationAtEpoch);

    @Relation("HAS_NEW_NAME")
    GitFileDescriptor getHasNewName();
    void setHasNewName(GitFileDescriptor gitFileDescriptor);

    @Relation("COPY_OF")
    GitFileDescriptor getCopyOf();
    void setCopyOf(GitFileDescriptor oldFile);

    default void updateDeletionTime(Date date) {
        //Always take latest delete Change
        if (getDeletedAtEpoch() == null || date.getTime() < getDeletedAtEpoch()) {
            setDeletedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
            setDeletedAtEpoch(date.getTime());
        }
    }

    default void updateCreationTime(Date date) {
        //Always take earliest create change
        if (getCreatedAt() == null || date.getTime() > getCreatedAtEpoch()) {
            setCreatedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
            setCreatedAtEpoch(date.getTime());
        }
    }

    default void updateLastModificationTime(Date date) {
        //Always take latest update change
        if (getLastModificationAtEpoch() == null || date.getTime() < getLastModificationAtEpoch()) {
            setLastModificationAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
            setLastModificationAtEpoch(date.getTime());
        }
    }
}
