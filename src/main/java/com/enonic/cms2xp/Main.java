package com.enonic.cms2xp;

import java.io.File;

import javax.inject.Inject;

import io.airlift.airline.Command;
import io.airlift.airline.HelpOption;
import io.airlift.airline.Option;
import io.airlift.airline.SingleCommand;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.config.MainConfigLoader;
import com.enonic.cms2xp.migrate.ExportData;

@Command(name = "cms2xp", description = "Migrates data from CMS to XP")
public final class Main
{
    @Inject
    public HelpOption helpOption;

    @Option(name = "-c", description = "Config file to use.", required = true)
    public File configFile;

    public static void main( final String... args )
        throws Exception
    {
        final Main main = SingleCommand.singleCommand( Main.class ).parse( args );
        if ( main.helpOption.showHelpIfRequested() )
        {
            return;
        }

        main.run();
    }

    public void run()
        throws Exception
    {
        final MainConfigLoader loader = new MainConfigLoader( this.configFile.toURI().toURL() );
        final MainConfig config = loader.load();
        new ExportData( config ).execute();
    }
}
