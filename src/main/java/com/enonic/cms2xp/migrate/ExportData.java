package com.enonic.cms2xp.migrate;

import com.enonic.cms2xp.config.MainConfig;

public final class ExportData
{
    private final MainConfig config;

    public ExportData( final MainConfig config )
    {
        this.config = config;
    }

    public void execute()
        throws Exception
    {
        System.out.println( this.config );
    }
}
