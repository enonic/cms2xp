package com.enonic.cms2xp.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms.core.structure.portlet.PortletEntity;

public class PortletRetriever
{
    private final static Logger logger = LoggerFactory.getLogger( PortletRetriever.class );

    public List<PortletEntity> retrievePortlets( final Session session )
    {

        session.beginTransaction();
        List<PortletEntity> portletEntities = session.getNamedQuery( "PortletEntity.findAll" ).list();
        for ( PortletEntity portletEntity : portletEntities )
        {
            logger.debug( "Retrieved portlet: " + portletEntity.toString() );
        }
        session.getTransaction().commit();

        return portletEntities;
    }
}
