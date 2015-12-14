package com.github.korayucar.gitcms;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.Daemon;
import org.eclipse.jgit.transport.DaemonClient;
import org.eclipse.jgit.transport.ServiceMayNotContinueException;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by koray2 on 11/26/15.
 */
public class CmsGitHelper {
    
    
    private static Map<String, InMemoryRepository> repositories = new HashMap<String, InMemoryRepository>();
    
    
    public static Repository createInMemoryClone(File root) throws IOException {
    
        Daemon server = new Daemon(new InetSocketAddress(9418));
        boolean uploadsEnabled = true;
        server.getService("git-receive-pack").setEnabled(uploadsEnabled);
        server.setRepositoryResolver(new RepositoryResolverImplementation());
        server.start();
        return null;
    }

    public static Set<String> getAllAvailableBranches(File repository) throws IOException, GitAPIException {
        List<Ref> branchList = Git.open(repository).branchList().call();
        Set<String> branchSet = new HashSet<String>();
        for (Ref ref: branchList){
            String [] refPieces =  ref.getName().split("/");
            branchSet.add(refPieces[refPieces.length-1]);
        }
        return branchSet;
    }

    /**
     *
     * returns last min( maxNum, commits made after latest merge, commits exist) commits
     *
     * @return
     */
    public static Iterable<RevCommit> getLatestCommits(String branchName , File repositoryDir, Integer maxNum) throws IOException, GitAPIException {

        Repository repository = Git.open(repositoryDir).getRepository();
        ObjectId objectId = repository.resolve(branchName);
        return Git.open(repositoryDir).log().add(objectId).setMaxCount(maxNum).call();

    }

    public static void revertBranchToPreviousCommit(File file, String commitRef, String branch) throws IOException, GitAPIException {
        Git.open(file).reset().setMode(ResetCommand.ResetType.HARD).setRef(commitRef).call();
        Git.open(file).push().setForce(true).call();
    }

    public static void updateBranch(String applicationRoot, String branchName, File originalRepository, String branchName1) throws IOException {
        try {
            Git.open(Paths.get(applicationRoot, branchName).toFile()).pull().call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        try {
            Git.open(Paths.get(applicationRoot, branchName).toFile()).push().setForce(true).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        try {
            Git.open(Paths.get(applicationRoot, branchName).toFile()).pull().call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    public static void mergeBranches(File branchRepository, File remoteRepository, String remoteBranchName , String localBranchName) throws IOException, GitAPIException {

//        Git.open(branchRepository).fetch().setRemote("origin").setRefSpecs(new RefSpec("refs/heads/"+remoteBranchName)).call();
        Git.open(branchRepository).branchCreate()
                .setStartPoint("origin/" + remoteBranchName)
                .setName(remoteBranchName)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .call();
//        Git.open(branchRepository).checkout().
//                setCreateBranch(true).
//                setName(remoteBranchName).
//                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
//                setStartPoint("origin/" + remoteBranchName).
//                call();
        MergeCommand merge = Git.open(branchRepository)
                .merge();
        MergeResult result= merge.setStrategy(MergeStrategy.THEIRS)
                .include( merge.getRepository().getRef(remoteBranchName))
                .setMessage("Merged from " + remoteBranchName + " into " + localBranchName)
                .setCommit(true)
                .call();

        Git.open(branchRepository).push().setForce(true).call();
    }


    private static final class RepositoryResolverImplementation implements
            RepositoryResolver<DaemonClient> {
        @Override
        public Repository open(DaemonClient client, String name)
                throws RepositoryNotFoundException,
                ServiceNotAuthorizedException, ServiceNotEnabledException,
                ServiceMayNotContinueException {
            InMemoryRepository repo = repositories.get(name);
            if (repo == null) {
                repo = new InMemoryRepository(
                        new DfsRepositoryDescription(name));
                repositories.put(name, repo);
            }
            return repo;
        }
    }
    
    public static File createTempDirectory()
            throws IOException
    {
        final File temp;
        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if(!(temp.delete()))
        {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if(!(temp.mkdir()))
        {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return (temp);
    }   
    
    public static void cloneAndCheckoutBranch( String rootPath , String folderName , File repository , String branchName)
            throws IOException, GitAPIException {
        
        final File temp;
        temp = Files.createDirectory(Paths.get(rootPath , folderName)).toFile();
        if(!(temp.delete())){
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if(!(temp.mkdir())){
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        try {
            Git.cloneRepository()
                    .setDirectory(temp)
                    .setCloneSubmodules(true)
                    .setURI(repository.getAbsolutePath())
                    .call()
                    .checkout()
                    .setForce(true)
                    .setCreateBranch(true)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                    .setName(branchName)
                    .call();
             updateBranch(rootPath , branchName , repository , null );
        }catch (RefAlreadyExistsException e ){
            e.printStackTrace();
        }
    }

    public static void stageAll(String rootPath, String branchName, File repository) throws IOException, GitAPIException {
        Git.open(repository).add().addFilepattern(".").setUpdate(true).call();
    }

    public static void commitAllChanges(String rootPath, String branchName, File repository , String commitMessage, String authorName , String authorMail) throws IOException, GitAPIException {
        stageAll(rootPath,branchName,repository);
        if(commitMessage == null || StringUtils.isEmpty(commitMessage))
            commitMessage = "no commit message available";
        Git.open(repository)
                .commit()
                .setMessage(commitMessage)
                .setAuthor(authorName , authorMail)
                .setCommitter(authorName , authorMail)
                .call();
        if(!branchName.equals("master") ){
            Git.open(repository).push().call();
        }
    }

    static final void createTestRepository(File file) throws GitAPIException {
        if(!file.isDirectory())
            throw new IllegalArgumentException("The argument to create a directory must be a directory");
        Git.init().setDirectory(file).call();

    }
}
