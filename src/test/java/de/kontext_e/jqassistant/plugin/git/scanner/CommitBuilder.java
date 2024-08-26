package de.kontext_e.jqassistant.plugin.git.scanner;

import de.kontext_e.jqassistant.plugin.git.scanner.model.GitChange;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitCommit;
import lombok.Builder;

import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


class CommitBuilder {

    @Builder
    public static GitCommit newCommit(String sha, String author, String committer, Date date, String message, String shortMessage,
                                      String encoding, List<GitChange> gitChanges, List<GitCommit> parents) {

        if (sha == null || sha.isEmpty()) { sha = randomSha(); }
        if (date == null) { date = Time.valueOf(LocalTime.now()); }
        if (gitChanges == null) { gitChanges = new ArrayList<>(); }
        if (parents == null) { parents = new ArrayList<>(); }

        GitCommit gitCommit = new GitCommit(sha);
        gitCommit.setAuthor(author);
        gitCommit.setCommitter(committer);
        gitCommit.setDate(date);
        gitCommit.setMessage(message);
        gitCommit.setShortMessage(shortMessage);
        gitCommit.setEncoding(encoding);
        gitCommit.getGitChanges().addAll(gitChanges);
        gitCommit.getParents().addAll(parents);

        return gitCommit;
    }

    private static String randomSha(){
        return String.valueOf((int) (Math.random() * (99999 - 10000) + 10000));
    }

}
