package com.aboyandhiscode.gitfilehistory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/*
 * Map<Path, A_CLASS>
 * A_CLASS has PathMetaData(filename, ext) "Static Data associated with the path."
 *         has a Map<RevCommit, Long> A map representing the size of the file at each point in the commit history.
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

    private List<String> headers = new ArrayList<>(Arrays.asList(new String[] { "Path", "Extension" }));

    private Path repoLocation;

    public GitFileHistory(Path repoLocation) {
        this.repoLocation = repoLocation;
    }

    public void mapHistory() {
        try {
            Git git = Git.open(repoLocation.toFile());

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
                        if (revs.length() > 3)
                            revs.replace(revs.length() - 2, revs.length() - 1, "");

                        System.out.println("Leaf commit! - " + leafCommit.getShortMessage() + " - " + revs.toString());
                    } catch (JGitInternalException | GitAPIException e) {
                        e.printStackTrace();
                    }
                }
            }

            
            TreeWalk treeWalk = new TreeWalk(repo);

            ObjectReader objectReader = repo.newObjectReader();
            RevCommit commit = null;
            while((commit = revWalk.next()) != null) {
                RevTree revTree = commit.getTree();
                
                
                treeWalk.reset(revTree);
                while(treeWalk.next()) {
                    if(treeWalk.isSubtree()) {
                        treeWalk.enterSubtree();
                        continue;
                    }
                    Path path = Paths.get(treeWalk.getPathString());

                    long size = objectReader.getObjectSize(treeWalk.getObjectId(0), Constants.OBJ_BLOB);

                    System.out.println(path.toString() + " - " + size);

                }
                System.out.println("\n");
            }

            treeWalk.close();
            revWalk.close();
            
        //     try {
        //         /* A thorough and hard cleaning of the working directory before checking out different commits. */
        //         git.clean().setForce(true).setCleanDirectories(true).setIgnore(false).call();
        //         git.reset().setMode(ResetType.HARD).call();
        //     } catch (NoWorkTreeException | GitAPIException e1) {
        //         e1.printStackTrace();
        //     }

        //     RevCommit commit = null;
        //     while((commit = revWalk.next()) != null) {
        //         String msg = commit.getShortMessage();
        //         String authorEmail = commit.getCommitterIdent().getEmailAddress();
        //         String date = commit.getCommitterIdent().getWhen()
        //             .toInstant()
        //             .atZone(ZoneId.systemDefault())
        //             .toLocalDateTime()
        //             .toString();
        //         System.out.println(date + ": " + msg + " - " +authorEmail);

        //         try {
        //             git.checkout().setName(commit.getName()).call();

        //             System.out.println("Checked out commit: " + commit.getId().abbreviate(8).name() + " - " + commit.getShortMessage());
        //             Files.walkFileTree(repoLocation.getParent(), new SimpleFileVisitor<Path>(){
        //                 @Override
        //                 public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        //                     throws IOException
        //                 {
        //                     Objects.requireNonNull(dir);
        //                     Objects.requireNonNull(attrs);

        //                     FileVisitResult result = FileVisitResult.CONTINUE;
        //                     if(dir.getFileName().toString().equals(".git")) {
        //                         result = FileVisitResult.SKIP_SUBTREE;
        //                     }

        //                     return result;
        //                 }

        //                 @Override
        //                 public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
        //                 {
        //                     Objects.requireNonNull(file);
        //                     Objects.requireNonNull(attrs);

        //                     Path relPath = repoLocation.getParent().relativize(file);

        //                     // float size = (float)relPath.toFile().length() / 1024.0f;
        //                     // NumberFormat df = DecimalFormat.getNumberInstance();
        //                     // df.setMaximumFractionDigits(2);
        //                     long size = Files.size(file);
                            
        //                     String ext = "";
        //                     String name = "";
        //                     int i = file.toString().lastIndexOf(".");
        //                     int p = file.toString().lastIndexOf(File.separator);
        //                     if(i > p) {
        //                         ext = file.toString().substring(i + 1);
        //                         name = file.toString().substring(p + 1, i);
        //                     }
        //                     if(name.isEmpty()) {
        //                         if(ext.isEmpty()) {
        //                             name = file.getFileName().toString();
        //                         } else {
        //                             name = "." + ext;
        //                         }
        //                     } else {
        //                         name += "." + ext;
        //                     }

        //                     // System.out.println(relPath.toString() + " - " + df.format(size) + " KB");
        //                     System.out.println(relPath.toString() + " - " + size + " Bytes, ext: " + ext + ", name: " + name);

        //                     return FileVisitResult.CONTINUE;
        //                 }
        //             });
        //             System.out.println("\n");
        //         } catch (GitAPIException e) {
        //             e.printStackTrace();
        //         }

        //         try {
        //             git.checkout().setName("master").call();
        //         } catch (GitAPIException e) {
        //             e.printStackTrace();
        //         }
    
        //     }

        //     revWalk.close();

            

        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
