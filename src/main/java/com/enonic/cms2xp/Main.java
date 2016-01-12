package com.enonic.cms2xp;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.config.MainConfigLoader;
import com.enonic.cms2xp.migrate.ExportData;
import com.enonic.xp.toolbox.app.InitAppCommand;

public final class Main
{

    private final static String DEFAULT_APPLICATION_NAME = "com.enonic.xp.app.myApp";

    public static final String DEFAULT_APP_REPO = "starter-base";

    public static void main( final String... args )
        throws Exception
    {
        if ( args.length == 0 )
        {
            System.out.println( "usage: cms2xp <config_path>\n" );
            System.out.println( "       Example of config.xml:\r\n" );
            final URL configUrl = Main.class.getResource( "/templates/config_example.xml" );
            final List<String> configLines = Resources.asCharSource( configUrl, Charsets.UTF_8 ).readLines();
            configLines.stream().map( ( l ) -> "       " + l ).forEach( System.out::println );
            System.out.println();
            System.exit( 1 );
        }
        //Retrieves the config
        final File configFile = new File( args[0] );
        final MainConfigLoader loader = new MainConfigLoader( configFile.toURI().toURL() );
        final MainConfig config = loader.load();
        validateConfig( config );

        //TODO Remove
        FileUtils.deleteDirectory( config.target.exportDir );
        FileUtils.deleteDirectory( config.target.userExportDir );
        FileUtils.deleteDirectory( config.target.applicationDir );

        //Initiates the application structure
        initApp( config );

        //Exports the data
        new ExportData( config ).execute();
    }

    private static void initApp( final MainConfig config )
    {
        final InitAppCommand initAppCommand = new InitAppCommand();
        initAppCommand.destination = config.target.applicationDir.getAbsolutePath();
        initAppCommand.name = config.target.applicationName;
        initAppCommand.repository = config.target.applicationRepo;
        initAppCommand.run();
    }

    private static void validateConfig( final MainConfig config )
    {
        Preconditions.checkArgument( StringUtils.isNotBlank( config.source.jdbcUrl ), "Missing parameter 'jdbcUrl' in config.xml" );
        Preconditions.checkArgument( StringUtils.isNotBlank( config.source.jdbcDriver ), "Missing parameter 'jdbcDriver' in config.xml" );
        Preconditions.checkArgument( config.source.blobStoreDir != null, "Missing parameter 'blobStoreDir' in config.xml" );
        Preconditions.checkArgument( config.source.blobStoreDir.exists(), "Blob store directory '%s' not found",
                                     config.source.blobStoreDir.getAbsolutePath() );

        Preconditions.checkArgument( config.target.applicationDir != null, "Missing parameter 'applicationDir' in config.xml" );
        Preconditions.checkArgument( config.target.exportDir != null, "Missing parameter 'exportDir' in config.xml" );

        if ( config.target.applicationName == null )
        {
            config.target.applicationName = DEFAULT_APPLICATION_NAME;
        }
        if ( config.target.applicationRepo == null )
        {
            config.target.applicationRepo = DEFAULT_APP_REPO;
        }
    }
}
