package com.enonic.cms2xp;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.config.MainConfigLoader;
import com.enonic.cms2xp.migrate.ExportData;
import com.enonic.xp.toolbox.app.InitAppCommand;

public final class Main
{
    public static void main( final String... args )
        throws Exception
    {
        //Retrieves the config
        final File configFile = new File( args[0] );
        final MainConfigLoader loader = new MainConfigLoader( configFile.toURI().toURL() );
        final MainConfig config = loader.load();

        //TODO Remove
        FileUtils.deleteDirectory( config.target.exportDir );
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
        initAppCommand.name = "com.enonic.xp.app.myApp";
        initAppCommand.repository = "starter-base";
        initAppCommand.run();
    }
}
