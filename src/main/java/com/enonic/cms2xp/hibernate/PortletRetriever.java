package com.enonic.cms2xp.hibernate;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.portlet.PortletEntity;

public class PortletRetriever
{
    private final static Logger logger = LoggerFactory.getLogger( PortletRetriever.class );

    private final Session session;

    public PortletRetriever( final Session session )
    {
        this.session = session;
    }

    public List<PortletEntity> retrievePortlets()
    {

        session.beginTransaction();
        List<PortletEntity> portletEntities = session.getNamedQuery( "PortletEntity.findAll" ).list();
        for ( PortletEntity portletEntity : portletEntities )
        {
            logger.debug( "Portlet loaded: " + portletEntity.toString() );
        }
        session.getTransaction().commit();

        return portletEntities;
    }

    public List<PortletEntity> retrievePortlets( final SiteKey siteKey )
    {
        session.beginTransaction();
        List<PortletEntity> portletEntities = session.getNamedQuery( "PortletEntity.findAll" ).list();
        portletEntities = portletEntities.stream().filter( ( p ) -> p.getSite().getKey().equals( siteKey ) ).collect( Collectors.toList() );
        for ( PortletEntity portletEntity : portletEntities )
        {
            logger.debug( "Portlet loaded: " + portletEntity.toString() );
        }
        session.getTransaction().commit();

        return portletEntities;
    }
}
