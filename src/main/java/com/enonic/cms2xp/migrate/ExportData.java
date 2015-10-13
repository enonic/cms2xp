package com.enonic.cms2xp.migrate;

import org.hibernate.Session;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.hibernate.CategoryExporter;
import com.enonic.cms2xp.hibernate.HibernateSessionProvider;

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
        final Session session = new HibernateSessionProvider( config ).getSession();

        System.out.println( "DB connected: " + session.isConnected() );
        CategoryExporter.retrieveRootCategories( session );
        session.close();

        System.out.println( this.config );
    }
}
