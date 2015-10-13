package com.enonic.cms2xp.converter;

import java.time.Instant;

import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.core.impl.content.ContentPathNameGenerator;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeState;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.content.category.CategoryEntity;

public final class CategoryNodeConverter
{
    public static final String SUPER_USER_KEY = "user:system:su";

    public Node toNode( final CategoryEntity category )
    {
        final Node folderNode = Node.create().
            nodeType( ContentConstants.CONTENT_NODE_COLLECTION ).
            nodeState( NodeState.DEFAULT ).
            id( new NodeId() ).
            timestamp( Instant.now() ).
            name( getName( category ) ).
            data( folderData( category ) ).
            inheritPermissions( true ).
            build();

        return folderNode;
    }

    private PropertyTree folderData( final CategoryEntity category )
    {
        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, category.getName() );
        data.setString( ContentPropertyNames.TYPE, ContentTypeName.folder().toString() );
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, category.getTimestamp().toInstant() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        data.setInstant( ContentPropertyNames.CREATED_TIME, category.getCreated().toInstant() );
        data.setSet( ContentPropertyNames.DATA, new PropertySet() );
        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        return data;
    }

    private String getName( final CategoryEntity category )
    {
        return new ContentPathNameGenerator().generatePathName( category.getName() );
    }
}
