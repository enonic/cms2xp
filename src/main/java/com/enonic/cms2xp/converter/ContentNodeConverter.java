package com.enonic.cms2xp.converter;

import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.custom.DataEntry;

public final class ContentNodeConverter
{
    private static final String SUPER_USER_KEY = "user:system:su";

    public static Node toNode( final ContentEntity content )
    {
        return NodeFactory.createNode( content.getName(), toData( content ) );
    }

    private static PropertyTree toData( final ContentEntity content )
    {
        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, content.getName() );
        data.setString( ContentPropertyNames.TYPE, ContentTypeName.unstructured().toString() );
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
                data.setValues( "data", DataEntryValuesConverter.toValue( dataEntry ) );
            }
        }
        return data;
    }
}
