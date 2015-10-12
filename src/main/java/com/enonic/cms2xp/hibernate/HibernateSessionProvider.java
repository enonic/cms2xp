package com.enonic.cms2xp.hibernate;

import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.config.SourceConfig;

public final class HibernateSessionProvider
{
    private final SessionFactory sessionFactory;

    public HibernateSessionProvider( final MainConfig config )
    {
        final Configuration hibConfiguration = new Configuration().configure();
        final Properties connectionProperties = getConnectionProperties( config );

        final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().
            applySettings( hibConfiguration.getProperties() ).
            applySettings( connectionProperties ).
            build();

        this.sessionFactory = hibConfiguration.buildSessionFactory( serviceRegistry );
    }

    private Properties getConnectionProperties( final MainConfig config )
    {
        final SourceConfig cfgSource = config.source;
        final Properties properties = new Properties();
        properties.setProperty( "connection.driver_class", cfgSource.jdbcDriver );
        properties.setProperty( "connection.url", cfgSource.jdbcUrl );
        properties.setProperty( "connection.username", cfgSource.jdbcUser );
        properties.setProperty( "connection.password", cfgSource.jdbcPassword );
        return properties;
    }

    public Session getSession()
    {
        return sessionFactory.withOptions().openSession();
    }
}
