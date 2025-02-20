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
            addAsRenameChange(gitChange, gitChangeDescriptor);
        } else if (isCopyChange(gitChangeDescriptor)) {
            addAsCopyChange(gitChange, gitChangeDescriptor);
        }
    }

    boolean isAddChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "A".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private void addAsAddChange(GitChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        gitFileDescriptor.setCreatedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
        gitFileDescriptor.setCreatedAtEpoch(date.getTime());
        gitChangeDescriptor.setCreates(gitFileDescriptor);
    }

    boolean isUpdateChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "M".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private void addAsUpdateChange(GitChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        gitChangeDescriptor.setUpdates(gitFileDescriptor);
        if (gitFileDescriptor.getLastModificationAtEpoch() == null || date.getTime() < gitFileDescriptor.getLastModificationAtEpoch()) {
            gitFileDescriptor.setLastModificationAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
            gitFileDescriptor.setLastModificationAtEpoch(date.getTime());
        }
    }

    boolean isDeleteChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "D".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private void addAsDeleteChange(GitChangeDescriptor gitChangeDescriptor, Date date, GitFileDescriptor gitFileDescriptor) {
        gitFileDescriptor.setDeletedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
        gitFileDescriptor.setDeletedAtEpoch(date.getTime());
        gitChangeDescriptor.setDeletes(gitFileDescriptor);
    }

    boolean isRenameChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "R".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private void addAsRenameChange(GitChange gitChange, GitChangeDescriptor gitChangeDescriptor) {
        final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
        final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());
        oldFile.setHasNewName(newFile);
        gitChangeDescriptor.setRenames(oldFile);
        gitChangeDescriptor.setDeletes(oldFile);
        gitChangeDescriptor.setCreates(newFile);
    }

    boolean isCopyChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "C".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    private void addAsCopyChange(GitChange gitChange, GitChangeDescriptor gitChangeDescriptor) {
        final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
        final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());
        newFile.setCopyOf(oldFile);
        gitChangeDescriptor.setCopies(oldFile);
        gitChangeDescriptor.setCreates(newFile);
    }
}
