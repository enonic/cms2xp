package com.enonic.cms2xp.hibernate;

import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.config.SourceConfig;

public final class HibernateSessionProvider
{
    private final SessionFactory sessionFactory;

    public HibernateSessionProvider( final MainConfig config )
    {
        final Properties connectionProperties = getConnectionProperties( config );
        this.sessionFactory = new Configuration().
            configure().
            addProperties( connectionProperties ).
            buildSessionFactory();
    }

    private Properties getConnectionProperties( final MainConfig config )
    {
        final SourceConfig cfgSource = config.source;
        final Properties properties = new Properties();
        properties.setProperty( "hibernate.connection.driver_class", cfgSource.jdbcDriver );
        properties.setProperty( "hibernate.connection.url", cfgSource.jdbcUrl );
        properties.setProperty( "hibernate.connection.username", cfgSource.jdbcUser );
        properties.setProperty( "hibernate.connection.password", cfgSource.jdbcPassword );
        return properties;
    }

    public Session getSession()
    {
        return sessionFactory.openSession();
    }
}
