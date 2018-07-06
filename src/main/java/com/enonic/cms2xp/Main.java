package com.enonic.cms2xp;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;

import com.enonic.cms2xp.config.ExcludeConfig;
import com.enonic.cms2xp.config.IncludeConfig;
import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.config.MainConfigLoader;
import com.enonic.cms2xp.config.TargetConfig;
import com.enonic.cms2xp.migrate.ExportData;
import com.enonic.xp.toolbox.app.InitAppCommand;

public final class Main
{

    private final static String DEFAULT_APPLICATION_NAME = "com.enonic.xp.app.myApp";

    public static final String DEFAULT_APP_REPO = "starter-vanilla";

    private static Logger logger;

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

        setupLogger( config );

        System.out.println( "CMS2XP - Export started" );
        logger.info( "CMS2XP - Export started" );
        System.out.println();
        System.out.println( "Config path: " + configFile.getAbsolutePath() );
        logger.info( "Config path: " + configFile.getAbsolutePath() );
        if ( config.target.logFile != null && !config.target.logFile.isEmpty() )
        {
            System.out.println( "Logging path: " + config.target.logFile );
        }
        System.out.println();

        //TODO Remove
        FileUtils.deleteDirectory( config.target.exportDir );
        FileUtils.deleteDirectory( config.target.userExportDir );
        FileUtils.deleteDirectory( config.target.applicationDir );

        //Initiates the application structure
        initApp( config );

        //Exports the data
        final Instant t1 = Instant.now();
        try
        {
            new ExportData( config ).execute();
        }
        finally
        {
            final Duration duration = Duration.between( t1, Instant.now() );
            final String durationStr = LocalTime.MIDNIGHT.plus( duration ).format( DateTimeFormatter.ofPattern( "HH:mm:ss" ) );
            System.out.println( "Export successful - Total time: " + durationStr );
            final Logger logger = LoggerFactory.getLogger( Main.class );
            logger.info( "Export completed successfully - Total time: " + durationStr );
            logger.info( "----------------------------------------------------" );
        }
    }

    private static void initApp( final MainConfig config )
    {
        if ( !config.target.exportApplication )
        {
            logger.info( "Skip generating application" );
            return;
        }
        logger.info( "Downloading application repo: " + config.target.applicationRepo );

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

        Preconditions.checkArgument( !config.target.exportApplication || config.target.applicationDir != null,
                                     "Missing parameter 'applicationDir' in config.xml" );
        Preconditions.checkArgument( config.target.exportDir != null, "Missing parameter 'exportDir' in config.xml" );

        if ( config.target.applicationName == null )
        {
            config.target.applicationName = DEFAULT_APPLICATION_NAME;
        }
        if ( config.target.applicationRepo == null )
        {
            config.target.applicationRepo = DEFAULT_APP_REPO;
        }
        config.source.include = config.source.include == null ? new IncludeConfig() : config.source.include;
        config.source.exclude = config.source.exclude == null ? new ExcludeConfig() : config.source.exclude;
        if ( config.source.include.contentPath == null )
        {
            config.source.include.contentPath = new String[0];
        }
        if ( config.source.exclude.contentPath == null )
        {
            config.source.exclude.contentPath = new String[0];
        }
        if ( config.source.include.site == null )
        {
            config.source.include.site = new String[0];
        }
        if ( config.source.exclude.site == null )
        {
            config.source.exclude.site = new String[0];
        }
        if ( config.source.exclude.userStore == null )
        {
            config.source.exclude.userStore = new String[0];
        }

        if ( config.source.include.contentPath.length > 0 && config.source.exclude.contentPath.length > 0 )
        {
            throw new IllegalArgumentException( "Config exclude and include contentPath cannot be specified at the same time" );
        }
        if ( config.source.include.site.length > 0 && config.source.exclude.site.length > 0 )
        {
            throw new IllegalArgumentException( "Config exclude and include site cannot be specified at the same time" );
        }

        if ( config.target.exportPublishDateMixin )
        {
            System.out.println(
                "Parameter 'exportPublishDateMixin' is deprecated. CMS publish dates will be exported to XP using the properties introduced in XP 6.8" );
        }
    }

    private static String formatInstant( Instant value )
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm" ).withZone( ZoneId.systemDefault() );
        return formatter.format( value );
    }

    private static void setupLogger( final MainConfig config )
    {
        final TargetConfig target = config.target;
        final String logFile = StringUtils.trimToEmpty( target.logFile );
        if ( !logFile.isEmpty() )
        {
            System.setProperty( "org.slf4j.simpleLogger.logFile", logFile );
        }

        System.setProperty( "org.slf4j.simpleLogger.showLogName", "false" );
        System.setProperty( "org.slf4j.simpleLogger.showThreadName", "false" );
        System.setProperty( "org.slf4j.simpleLogger.showDateTime", "true" );
        System.setProperty( "org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss.SSS" );
//        System.setProperty( "org.slf4j.simpleLogger.log.com.enonic", "off" );

        logger = LoggerFactory.getLogger( Main.class );
    }
}
