package com.enonic.cms2xp.converter;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.jdom.Element;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.contentdata.legacy.LegacyImageContentData;

public final class ImageDescriptionResolver
{
    private final Map<ContentKey, String> cache;

    private final Session session;

    public ImageDescriptionResolver( final Session session )
    {
        this.session = session;
        cache = new HashMap<>();
    }

    public String getImageDescription( final ContentKey contentKey )
    {
        String description = this.cache.get( contentKey );
        if ( description == null )
        {
            final ContentEntity content = fetchContent( contentKey );
            if ( content != null )
            {
                final LegacyImageContentData data = (LegacyImageContentData) content.getMainVersion().getContentData();

                Element contentDataEl = data.getContentDataXml().getRootElement();
                description = contentDataEl.getChildText( "description" );
            }
        }

        if ( description != null && !description.trim().isEmpty() )
        {
            cache.put( contentKey, description );
            return description;
        }
        return null;
    }

    public ContentEntity fetchContent( final ContentKey key )
    {
        session.beginTransaction();
        ContentEntity content = (ContentEntity) session.get( ContentEntity.class, key );
        if ( content == null )
        {
            return null;
        }

        if ( content.isDeleted() )
        {
            return null;
        }
        return content;
    }

}
