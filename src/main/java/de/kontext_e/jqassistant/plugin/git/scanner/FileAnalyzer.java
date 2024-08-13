package de.kontext_e.jqassistant.plugin.git.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import de.kontext_e.jqassistant.plugin.git.scanner.cache.FileCache;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitChange;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitCommit;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitChangeDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitCommitDescriptor;
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
            gitFileDescriptor.setCreatedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
            gitFileDescriptor.setCreatedAtEpoch(date.getTime());
            gitChangeDescriptor.setCreates(gitFileDescriptor);
        } else if (isUpdateChange(gitChangeDescriptor)) {
            gitChangeDescriptor.setUpdates(gitFileDescriptor);
            if (gitFileDescriptor.getLastModificationAtEpoch() == null || date.getTime() < gitFileDescriptor.getLastModificationAtEpoch()) {
                gitFileDescriptor.setLastModificationAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
                gitFileDescriptor.setLastModificationAtEpoch(date.getTime());
            }
        } else if (isDeleteChange(gitChangeDescriptor)) {
            gitFileDescriptor.setDeletedAt(GitRepositoryScanner.DATE_TIME_FORMAT.format(date));
            gitFileDescriptor.setDeletedAtEpoch(date.getTime());
            gitChangeDescriptor.setDeletes(gitFileDescriptor);
        } else if (isRenameChange(gitChangeDescriptor)) {
            final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
            final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());
            oldFile.setHasNewName(newFile);
            gitChangeDescriptor.setRenames(oldFile);
            gitChangeDescriptor.setDeletes(oldFile);
            gitChangeDescriptor.setCreates(newFile);
        } else if (isCopyChange(gitChangeDescriptor)) {
            final GitFileDescriptor oldFile = fileCache.findOrCreate(gitChange.getOldPath());
            final GitFileDescriptor newFile = fileCache.findOrCreate(gitChange.getNewPath());
            newFile.setCopyOf(oldFile);
            gitChangeDescriptor.setCopies(oldFile);
            gitChangeDescriptor.setCreates(newFile);
        }
    }

    boolean isCopyChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "C".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    boolean isRenameChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "R".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    boolean isDeleteChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "D".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    boolean isUpdateChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "M".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

    boolean isAddChange(final GitChangeDescriptor gitChangeDescriptor) {
        return "A".equalsIgnoreCase(gitChangeDescriptor.getModificationKind());
    }

}