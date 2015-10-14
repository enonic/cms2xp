package com.enonic.cms2xp.config;

import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.*;

public class MainConfigTest
{
    @Test
    public void testConfig()
        throws Exception
    {
        final URL url = getClass().getResource( "test-config.xml" );
        final MainConfig config = new MainConfigLoader( url ).load();

        assertNotNull( config );
        assertSource( config.source );
        assertTarget( config.target );
    }

    private void assertSource( final SourceConfig config )
    {
        assertNotNull( config );

        assertEquals( "org.h2.Driver", config.jdbcDriver );
        assertEquals( "jdbc:h2:~/test", config.jdbcUrl );
        assertEquals( "user", config.jdbcUser );
        assertEquals( "password", config.jdbcPassword );

        assertNotNull( config.blobStoreDir );
        assertEquals( "blobs", config.blobStoreDir.getName() );

        assertNotNull( config.resourcesDir );
        assertEquals( "resources", config.resourcesDir.getName() );
    }

    private void assertTarget( final TargetConfig config )
    {
        assertNotNull( config );

        assertNotNull( config.exportDir );
        assertEquals( "export", config.exportDir.getName() );
        assertEquals( "application", config.applicationDir.getName() );
    }
}
