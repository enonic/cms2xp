package com.enonic.cms2xp;

import java.io.File;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.config.MainConfigLoader;
import com.enonic.cms2xp.migrate.ExportData;

public final class Main
{
    public static void main( final String... args )
        throws Exception
    {
        final File configFile = new File( args[0] );
        final MainConfigLoader loader = new MainConfigLoader( configFile.toURI().toURL() );
        final MainConfig config = loader.load();
        new ExportData( config ).execute();
    }
}
