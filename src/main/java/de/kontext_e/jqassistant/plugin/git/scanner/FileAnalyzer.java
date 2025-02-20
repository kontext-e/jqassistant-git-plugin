package de.kontext_e.jqassistant.plugin.git.scanner;

import de.kontext_e.jqassistant.plugin.git.scanner.cache.FileCache;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitChange;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitChangeDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;


import java.util.Date;

public class FileAnalyzer {

    private final FileCache fileCache;

    public FileAnalyzer(FileCache fileCache) {
        this.fileCache = fileCache;
    }

    void addAsGitFile(GitChange gitChange, final GitChangeDescriptor gitChangeDescriptor, final Date date) {
        final GitFileDescriptor gitFileDescriptor = fileCache.findOrCreate(gitChange.getRelativePath());

        gitChangeDescriptor.setModifies(gitFileDescriptor);

        if (isAddChange(gitChangeDescriptor)) {
            addAsAddChange(gitChangeDescriptor, date, gitFileDescriptor);
        } else if (isUpdateChange(gitChangeDescriptor)) {
            addAsUpdateChange(gitChangeDescriptor, date, gitFileDescriptor);
        } else if (isDeleteChange(gitChangeDescriptor)) {
            addAsDeleteChange(gitChangeDescriptor, date, gitFileDescriptor);
        } else if (isRenameChange(gitChangeDescriptor)) {
            addAsRenameChange(gitChangeDescriptor, date, gitChange);
        } else if (isCopyChange(gitChangeDescriptor)) {
            addAsCopyChange(gitChangeDescriptor, date, gitChange);
        }
    }

    boolean isAddChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "A".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private void addAsAddChange(GitChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        gitFileDescriptor.updateCreationTime(date);
        gitChangeDescriptor.setCreates(gitFileDescriptor);
    }

    boolean isUpdateChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "M".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private void addAsUpdateChange(GitChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        gitFileDescriptor.updateLastModificationTime(date);
        gitChangeDescriptor.setUpdates(gitFileDescriptor);
    }

    boolean isDeleteChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "D".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private void addAsDeleteChange(GitChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        gitFileDescriptor.updateDeletionTime(date);
        gitChangeDescriptor.setDeletes(gitFileDescriptor);
    }

    boolean isRenameChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "R".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private void addAsRenameChange(GitChangeDescriptor gitChangeDescriptor, Date date, GitChange gitChange) {
        final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
        final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());

        oldFile.setHasNewName(newFile);
        gitChangeDescriptor.setRenames(oldFile);

        gitChangeDescriptor.setDeletes(oldFile);
        oldFile.updateDeletionTime(date);

        gitChangeDescriptor.setCreates(newFile);
        newFile.updateCreationTime(date);
    }

    boolean isCopyChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "C".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private void addAsCopyChange(GitChangeDescriptor gitChangeDescriptor, Date date, GitChange gitChange) {
        final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
        final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());

        newFile.setCopyOf(oldFile);
        gitChangeDescriptor.setCopies(oldFile);
        gitChangeDescriptor.setCreates(newFile);
        newFile.updateCreationTime(date);
    }
}
