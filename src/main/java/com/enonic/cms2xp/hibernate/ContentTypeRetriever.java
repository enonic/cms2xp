package com.enonic.cms2xp.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms.core.content.contenttype.ContentTypeEntity;

public final class ContentTypeRetriever
{
    private final static Logger logger = LoggerFactory.getLogger( ContentTypeRetriever.class );

    public List<ContentTypeEntity> retrieveContentTypes( final Session session )
    {
        session.beginTransaction();
        List<ContentTypeEntity> contentTypes = session.getNamedQuery( "ContentTypeEntity.getAll" ).list();
        for ( ContentTypeEntity contentType : contentTypes )
        {
            logger.info( "Content type loaded: " + contentType.getName() );
        }
        session.getTransaction().commit();

        return contentTypes;
    }
}
