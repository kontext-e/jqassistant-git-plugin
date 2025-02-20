package de.kontext_e.jqassistant.plugin.git.scanner;

import de.kontext_e.jqassistant.plugin.git.scanner.cache.FileCache;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitChange;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.*;


import java.util.Date;

public class FileAnalyzer {

    private final FileCache fileCache;

    public FileAnalyzer(FileCache fileCache) {
        this.fileCache = fileCache;
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
        gitChangeDescriptor.setCreates(gitFileDescriptor);
    }

    private void addAsUpdateChange(GitUpdateChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        updateLastModificationTime(gitFileDescriptor, date);
        gitChangeDescriptor.setUpdates(gitFileDescriptor);
    }

    private void addAsDeleteChange(GitDeleteChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        updateDeletionTime(gitFileDescriptor, date);
        gitChangeDescriptor.setDeletes(gitFileDescriptor);
    }

    private void addAsRenameChange(GitRenameChangeDescriptor gitChangeDescriptor, Date date, GitChange gitChange) {
        final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
        final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());

        oldFile.setHasNewName(newFile);
        gitChangeDescriptor.setRenames(oldFile);

        gitChangeDescriptor.setDeletes(oldFile);
        updateDeletionTime(oldFile, date);

        gitChangeDescriptor.setCreates(newFile);
        updateCreationTime(newFile, date);
    }

    private void addAsCopyChange(GitCopyChangeDescriptor gitChangeDescriptor, Date date, GitChange gitChange) {
        final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
        final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());

        newFile.setCopyOf(oldFile);
        gitChangeDescriptor.setCopies(oldFile);
        gitChangeDescriptor.setCreates(newFile);
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
