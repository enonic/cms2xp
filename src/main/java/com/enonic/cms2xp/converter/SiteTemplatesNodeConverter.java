package com.enonic.cms2xp.converter;

import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeName;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.structure.SiteEntity;

public final class SiteTemplatesNodeConverter
    extends AbstractNodeConverter
{
    public Node toNode( final SiteEntity siteEntity )
    {
        final Node node = createNode( new NodeId(), "_templates", toData( siteEntity ) );
        return Node.create( node ).name( NodeName.from( "_templates" ) ).build();
    }

    private PropertyTree toData( final SiteEntity siteEntity )
    {
        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, "Templates" );
        data.setString( ContentPropertyNames.TYPE, ContentTypeName.templateFolder().toString() );
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, siteEntity.getTimestamp().toInstant() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        data.setInstant( ContentPropertyNames.CREATED_TIME, siteEntity.getTimestamp().toInstant() );
        data.setSet( ContentPropertyNames.DATA, new PropertySet() );
        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        return data;
    }
}
