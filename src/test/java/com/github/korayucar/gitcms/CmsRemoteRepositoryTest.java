package com.github.korayucar.gitcms;

import org.junit.Test;

import java.io.File;

/**
 * Created by koray2 on 11/14/15.
 */
public class CmsRemoteRepositoryTest {
    
    @Test
    public void testGetUrl() throws Exception {
        File temp = TestUtil.createTempDirectory();
        TestUtil.createTestRepository(temp);
        
    }
}