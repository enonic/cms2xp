package com.enonic.cms2xp.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms.core.structure.SiteEntity;

public class SiteRetriever
{
    private final static Logger logger = LoggerFactory.getLogger( SiteRetriever.class );

    public static List<SiteEntity> retrieveSites( final Session session )
    {

        session.beginTransaction();
        List<SiteEntity> siteEntities = session.getNamedQuery( "SiteEntity.findAll" ).list();
        for ( SiteEntity siteEntity : siteEntities )
        {
            logger.info( "Retrieved site: " + siteEntity.toString() );
        }
        session.getTransaction().commit();

        return siteEntities;
    }
}
