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

        gitFileDescriptor.updateCreationTime(date);
    private void addAsAddChange(GitAddChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        gitChangeDescriptor.setCreates(gitFileDescriptor);
    }

        gitFileDescriptor.updateLastModificationTime(date);
    private void addAsUpdateChange(GitUpdateChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        gitChangeDescriptor.setUpdates(gitFileDescriptor);
    }

        gitFileDescriptor.updateDeletionTime(date);
    private void addAsDeleteChange(GitDeleteChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        gitChangeDescriptor.setDeletes(gitFileDescriptor);
    }

    private void addAsRenameChange(GitRenameChangeDescriptor gitChangeDescriptor, Date date, GitChange gitChange) {
        final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
        final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());

        oldFile.setHasNewName(newFile);
        gitChangeDescriptor.setRenames(oldFile);

        gitChangeDescriptor.setDeletes(oldFile);
        oldFile.updateDeletionTime(date);

        gitChangeDescriptor.setCreates(newFile);
        newFile.updateCreationTime(date);
    }

    private void addAsCopyChange(GitCopyChangeDescriptor gitChangeDescriptor, Date date, GitChange gitChange) {
        final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
        final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());

        newFile.setCopyOf(oldFile);
        gitChangeDescriptor.setCopies(oldFile);
        gitChangeDescriptor.setCreates(newFile);
        newFile.updateCreationTime(date);
    }
}
