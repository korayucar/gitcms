package com.github.korayucar.gitcms;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Repository;
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
import java.util.HashMap;
import java.util.Map;

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
        if(!(temp.delete()))
        {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if(!(temp.mkdir()))
        {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
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

    }
    
    static final void createTestRepository(File file) throws GitAPIException {
        if(!file.isDirectory())
            throw new IllegalArgumentException("The argument to create a directory must be a directory");
        Git git = Git.init().setDirectory(file).call();
        
    }
}
