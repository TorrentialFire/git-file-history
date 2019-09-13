package com.aboyandhiscode.gitfilehistory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Hello world!
 *
 */
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

    private Path repoLocation;

    public GitFileHistory(Path repoLocation) {
        this.repoLocation = repoLocation;
    }

    public void mapHistory() {
        try {
            Git git = Git.open(repoLocation.toFile());
            Iterable<RevCommit> logResult = git.log().all().call();
            Iterator<RevCommit> commitItr = logResult.iterator();
            
            while(commitItr.hasNext()) {
                RevCommit commit = commitItr.next();

                System.out.println(commit.getShortMessage());
            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }
}
