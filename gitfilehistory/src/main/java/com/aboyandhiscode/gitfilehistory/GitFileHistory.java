package com.aboyandhiscode.gitfilehistory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
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

    private List<String> headers = new ArrayList<>(Arrays.asList(new String[] { "Path", "Extension" }));

    private Path repoLocation;

    public GitFileHistory(Path repoLocation) {
        this.repoLocation = repoLocation;
    }

    public void mapHistory() {
        try {
            Git git = Git.open(repoLocation.toFile());
            /*
             * This code doesn't allow us to specify reverse-chronological order, so let's
             * use some code that does...
             */
            // Iterable<RevCommit> logResult = git.log().all().call();
            // Iterator<RevCommit> commitItr = logResult.iterator();

            // while(commitItr.hasNext()) {
            // RevCommit commit = commitItr.next();

            // System.out.println(commit.getShortMessage());
            // }

            Repository repo = git.getRepository();
            RevWalk revWalk = new RevWalk(repo);
            /*
             * We don't need to mark the start at "HEAD" anymore because we will find all
             * leaves and work backwards from the initial commit to them.
             */
            // revWalk.markStart(revWalk.parseCommit(repo.resolve("HEAD")));
            revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
            revWalk.sort(RevSort.REVERSE, true);

            List<Ref> allRefs = repo.getRefDatabase().getRefsByPrefix(RefDatabase.ALL);
            Set<RevCommit> leafCommits = new HashSet<>();
            for (Ref ref : allRefs) {
                RevCommit leafCommit = revWalk.parseCommit(ref.getLeaf().getObjectId());
                /*
                 * We might encounter the same leaf commit multiple times when parsing all refs,
                 * but RevWalk.markStart() will simply return if a commit is seen more than
                 * once, so there is no need to track repeated leaves at this level.
                 * 
                 * However, for the purposes of logging or output we may want to maintain a set
                 * of RevCommits.
                 */

                if (leafCommits.add(leafCommit)) {
                    revWalk.markStart(leafCommit);
                    Map<ObjectId, String> revsForCommit;
                    try {
                        revsForCommit = git.nameRev().addPrefix("refs/heads").add(leafCommit.getId()).call();
                    
                        StringBuilder revs = new StringBuilder();
                        revsForCommit.values().forEach(str -> revs.append(str + ", "));
                        if(revs.length() > 3)
                            revs.replace(revs.length() - 2, revs.length() - 1, "");

                        System.out.println("Leaf commit! - " + leafCommit.getShortMessage() + " - " + revs.toString());
                    } catch (JGitInternalException | GitAPIException e) {
                        e.printStackTrace();
                    }
                }
            }
            
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

                try {
                    git.checkout().setName(commit.getName()).call();

                    System.out.println("Checked out commit: " + commit.getId().abbreviate(8).name() + " - " + commit.getShortMessage());
                    Files.walkFileTree(repoLocation.getParent(), new SimpleFileVisitor<Path>(){
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException
                        {
                            Objects.requireNonNull(dir);
                            Objects.requireNonNull(attrs);

                            FileVisitResult result = FileVisitResult.CONTINUE;
                            if(dir.getFileName().toString().equals(".git")) {
                                result = FileVisitResult.SKIP_SUBTREE;
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                        {
                            Objects.requireNonNull(file);
                            Objects.requireNonNull(attrs);

                            Path relPath = repoLocation.getParent().relativize(file);

                            System.out.println(relPath.toString());

                            return FileVisitResult.CONTINUE;
                        }
                    });
                    System.out.println("\n");
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }

                try {
                    git.checkout().setName("master").call();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
    
            }

            revWalk.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
