package com.aboyandhiscode.gitfilehistory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

public class GitFileHistory {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please supply a root directory for a git repository.");
            return;
        }

        Path repoLocation = Paths.get("", args);
        System.out.println(repoLocation.toString());
        GitFileHistory gfh = new GitFileHistory(repoLocation);
        gfh.mapHistory();
    }

    private List<String> headers = new ArrayList<>(Arrays.asList(new String[] {"Path", "Extension"}));

    private Path repoLocation;

    public GitFileHistory(Path repoLocation) {
        this.repoLocation = repoLocation;
    }

    public void mapHistory() {
        try {
            Git git = Git.open(repoLocation.toFile());
            /* This code doesn't allow us to specify reverse-chronological order, so let's use some code that does... */
            // Iterable<RevCommit> logResult = git.log().all().call();
            // Iterator<RevCommit> commitItr = logResult.iterator();
            
            // while(commitItr.hasNext()) {
            //     RevCommit commit = commitItr.next();

            //     System.out.println(commit.getShortMessage());
            // }
            
            Repository repo = git.getRepository();
            RevWalk revWalk = new RevWalk(repo);
            revWalk.markStart(revWalk.parseCommit(repo.resolve("HEAD")));
            revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
            revWalk.sort(RevSort.REVERSE, true);
            RevCommit commit = null;
            while((commit = revWalk.next()) != null) {
                String msg = commit.getShortMessage();
                String authorEmail = commit.getCommitterIdent().getEmailAddress();
                String date = commit.getCommitterIdent().getWhen()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .toString();
                System.out.println(date + ": " + msg + " - " +authorEmail);
            }

            revWalk.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
