package com.github.korayucar.gitcms;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by koray2 on 11/14/15.
 */
public class TestUtil {
    
    /**
     * creates an empty repository inside the directory file
     * @param file
     */
    static final void createTestRepository(File file) throws GitAPIException {
        if(!file.isDirectory())
            throw new IllegalArgumentException("The argument to create a directory must be a directory");
        Git git = Git.init().setDirectory(file).call();
        
    }
    
    static final void cloneTestRepository(URL url){
        
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
}
