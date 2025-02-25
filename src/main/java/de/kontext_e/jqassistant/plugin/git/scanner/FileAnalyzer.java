package de.kontext_e.jqassistant.plugin.git.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.git.scanner.cache.FileCache;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitChange;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.change.*;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitAddRelation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitDeleteRelation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitUpdateRelation;

import java.util.Date;

public class FileAnalyzer {

    private final FileCache fileCache;
    private final Store store;

    public FileAnalyzer(FileCache fileCache, Store store) {
        this.fileCache = fileCache;
        this.store = store;
    }

    void addAsGitFile(GitChange gitChange, final GitChangeDescriptor gitChangeDescriptor, final Date date) {
        final GitFileDescriptor gitFileDescriptor = fileCache.findOrCreate(gitChange.getRelativePath());

        gitChangeDescriptor.setModifies(gitFileDescriptor);

        if (gitChangeDescriptor instanceof GitAddChangeDescriptor) {
            addAsAddChange((GitAddChangeDescriptor) gitChangeDescriptor, date, gitFileDescriptor);
        } else if (gitChangeDescriptor instanceof GitUpdateChangeDescriptor) {
            addAsUpdateChange((GitUpdateChangeDescriptor) gitChangeDescriptor, date, gitFileDescriptor);
        } else if (gitChangeDescriptor instanceof GitDeleteChangeDescriptor) {
            addAsDeleteChange((GitDeleteChangeDescriptor) gitChangeDescriptor, date, gitFileDescriptor);
        } else if (gitChangeDescriptor instanceof GitRenameChangeDescriptor) {
            addAsRenameChange((GitRenameChangeDescriptor) gitChangeDescriptor, date, gitChange);
        } else if (gitChangeDescriptor instanceof GitCopyChangeDescriptor) {
            addAsCopyChange((GitCopyChangeDescriptor) gitChangeDescriptor, date, gitChange);
        }
    }

    private void addAsAddChange(GitAddChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        updateCreationTime(gitFileDescriptor, date);
        GitAddRelation gitAddRelation = store.create(gitChangeDescriptor, GitAddRelation.class, gitFileDescriptor);
        gitAddRelation.setCreatedAtEpoch(date.getTime());
        gitAddRelation.setCreatedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
    }

    private void addAsUpdateChange(GitUpdateChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        updateLastModificationTime(gitFileDescriptor, date);
        GitUpdateRelation updateChangeDescriptor = store.create(gitChangeDescriptor, GitUpdateRelation.class, gitFileDescriptor);
        updateChangeDescriptor.setModifiedAtEpoch(date.getTime());
        updateChangeDescriptor.setModifiedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
    }

    private void addAsDeleteChange(GitDeleteChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        updateDeletionTime(gitFileDescriptor, date);
        GitDeleteRelation deleteRelation = store.create(gitChangeDescriptor, GitDeleteRelation.class, gitFileDescriptor);
        deleteRelation.setDeletedAtEpoch(date.getTime());
        deleteRelation.setDeletedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
    }

    private void addAsRenameChange(GitRenameChangeDescriptor gitChangeDescriptor, Date date, GitChange gitChange) {
        final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
        final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());

        oldFile.setHasNewName(newFile);
        gitChangeDescriptor.setRenames(oldFile);

        GitDeleteRelation deleteRelation = store.create(gitChangeDescriptor, GitDeleteRelation.class, oldFile);
        deleteRelation.setDeletedAtEpoch(date.getTime());
        deleteRelation.setDeletedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
        updateDeletionTime(oldFile, date);

        GitAddRelation gitAddRelation = store.create(gitChangeDescriptor, GitAddRelation.class, newFile);
        gitAddRelation.setCreatedAtEpoch(date.getTime());
        gitAddRelation.setCreatedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
        updateCreationTime(newFile, date);
    }

    private void addAsCopyChange(GitCopyChangeDescriptor gitChangeDescriptor, Date date, GitChange gitChange) {
        final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
        final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());

        newFile.setCopyOf(oldFile);
        gitChangeDescriptor.setCopies(oldFile);

        GitAddRelation gitAddRelation = store.create(gitChangeDescriptor, GitAddRelation.class, newFile);
        gitAddRelation.setCreatedAtEpoch(date.getTime());
        gitAddRelation.setCreatedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
        updateCreationTime(newFile, date);
    }

    private void updateDeletionTime(GitFileDescriptor descriptor, Date date) {
        //Always take latest delete Change
        if (descriptor.getDeletedAtEpoch() == null || date.getTime() < descriptor.getDeletedAtEpoch()) {
            descriptor.setDeletedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
            descriptor.setDeletedAtEpoch(date.getTime());
        }
    }

    private void updateCreationTime(GitFileDescriptor descriptor, Date date) {
        //Always take earliest create change
        if (descriptor.getCreatedAt() == null || date.getTime() > descriptor.getCreatedAtEpoch()) {
            descriptor.setCreatedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
            descriptor.setCreatedAtEpoch(date.getTime());
        }
    }

    private void updateLastModificationTime(GitFileDescriptor descriptor, Date date) {
        //Always take latest update change
        if (descriptor.getLastModificationAtEpoch() == null || date.getTime() < descriptor.getLastModificationAtEpoch()) {
            descriptor.setLastModificationAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
            descriptor.setLastModificationAtEpoch(date.getTime());
        }
    }
}
