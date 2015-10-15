package com.enonic.cms2xp.converter;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.custom.DataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;

public final class ContentNodeConverter
    extends AbstractNodeConverter
{
    private static final String SUPER_USER_KEY = "user:system:su";

    private NodeIdRegistry nodeIdRegistry = new NodeIdRegistry();

    private DataEntryValuesConverter dataEntryValuesConverter = new DataEntryValuesConverter( nodeIdRegistry );

    private static final Map<ContentHandlerName, ContentTypeName> TYPES = ImmutableMap.<ContentHandlerName, ContentTypeName>builder().
        put( ContentHandlerName.CUSTOM, ContentTypeName.unstructured() ).
        put( ContentHandlerName.IMAGE, ContentTypeName.imageMedia() ).
        put( ContentHandlerName.FILE, ContentTypeName.unknownMedia() ).
        build();

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
            final ContentData contentData = mainVersion.getContentData();
            if ( contentData instanceof DataEntry )
            {
                DataEntry dataEntry = (DataEntry) contentData;
                data.setValues( "data", dataEntryValuesConverter.toValue( dataEntry ) );
            }
        }
        return data;
    }

    public ContentTypeName convertType( final ContentEntity content )
    {
        return TYPES.getOrDefault( content.getContentType().getContentHandlerName(), ContentTypeName.unstructured() );
    }
}
