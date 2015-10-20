package com.enonic.cms2xp.converter;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.custom.DataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeKey;

public final class ContentNodeConverter
    extends AbstractNodeConverter
{
    private final static Logger logger = LoggerFactory.getLogger( ContentNodeConverter.class );

    private final NodeIdRegistry nodeIdRegistry;

    private final DataEntryValuesConverter dataEntryValuesConverter;

    private final ContentTypeResolver contentTypeResolver;

    private static final Map<ContentHandlerName, ContentTypeName> SYSTEM_TYPES =
        ImmutableMap.<ContentHandlerName, ContentTypeName>builder().
            put( ContentHandlerName.CUSTOM, ContentTypeName.unstructured() ).
            put( ContentHandlerName.IMAGE, ContentTypeName.imageMedia() ).
            put( ContentHandlerName.FILE, ContentTypeName.unknownMedia() ).
            build();

    public ContentNodeConverter( final ContentTypeResolver contentTypeResolver )
    {
        this.contentTypeResolver = contentTypeResolver;
        this.nodeIdRegistry = new NodeIdRegistry();
        this.dataEntryValuesConverter = new DataEntryValuesConverter( this.nodeIdRegistry );
    }

    public Node toNode( final ContentEntity content )
    {
        return createNode( nodeIdRegistry.getNodeId( content.getKey() ), content.getName(), toData( content ) );
    }

    private PropertyTree toData( final ContentEntity content )
    {
        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, content.getName() );
        data.setString( ContentPropertyNames.TYPE, convertType( content ).toString() );
        data.setString( ContentPropertyNames.LANGUAGE, content.getLanguage().getCode() );
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, content.getTimestamp().toInstant() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        data.setInstant( ContentPropertyNames.CREATED_TIME, content.getCreatedAt().toInstant() );
        data.setSet( ContentPropertyNames.DATA, new PropertySet() );
        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );

        final ContentVersionEntity mainVersion = content.getMainVersion();
        if ( mainVersion != null )
        {
            try
            {
                final ContentData contentData = mainVersion.getContentData();
                if ( contentData instanceof DataEntry )
                {
                    DataEntry dataEntry = (DataEntry) contentData;
                    data.setProperty( ContentPropertyNames.DATA, dataEntryValuesConverter.toValue( dataEntry ) );
                }
            }
            catch ( Exception e )
            {
                logger.warn( "Cannot get ContentData from '" + content.getPathAsString() + "'", e );
            }
        }
        return data;
    }

    public ContentTypeName convertType( final ContentEntity content )
    {
        final ContentTypeKey key = content.getContentType().getContentTypeKey();
        final ContentType contentType = this.contentTypeResolver.getContentType( key );
        if ( contentType != null )
        {
            return contentType.getName();
        }
        return SYSTEM_TYPES.getOrDefault( content.getContentType().getContentHandlerName(), ContentTypeName.unstructured() );
    }
}
