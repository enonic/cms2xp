package com.enonic.cms2xp.converter;

import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.structure.SiteEntity;

public class SiteNodeConverter
    extends AbstractNodeConverter
{
    public Node convertToNode( final SiteEntity siteEntity )
    {
        return createNode( new NodeId(), siteEntity.getName(), toData( siteEntity ) );
    }

    private PropertyTree toData( final SiteEntity siteEntity )
    {
        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, siteEntity.getName() );
        data.setString( ContentPropertyNames.TYPE, ContentTypeName.site().toString() );
        data.setString( ContentPropertyNames.LANGUAGE, siteEntity.getLanguage().getCode() );
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, siteEntity.getTimestamp().toInstant() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        //TODO No created time info?
        data.setInstant( ContentPropertyNames.CREATED_TIME, siteEntity.getTimestamp().toInstant() );
        //data.setInstant( ContentPropertyNames.CREATED_TIME, siteEntity.getCreated().toInstant() );
        data.setSet( ContentPropertyNames.DATA, new PropertySet() );
        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        return data;
    }
}
