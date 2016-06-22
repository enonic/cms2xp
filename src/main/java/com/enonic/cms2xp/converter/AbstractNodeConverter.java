package com.enonic.cms2xp.converter;


import java.time.Instant;

import org.apache.commons.lang.StringUtils;

import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.index.ChildOrder;
import com.enonic.xp.name.NamePrettyfier;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeName;
import com.enonic.xp.node.NodeState;

public abstract class AbstractNodeConverter
{
    protected static final String SUPER_USER_KEY = "user:system:su";

    public Node createNode( NodeId nodeId, String name, PropertyTree data )
    {
        String pathName = generatePathName( name );
        if ( StringUtils.isBlank( pathName ) )
        {
            pathName = "_noname_";
        }
        NodeName nodeName;
        try
        {
            nodeName = NodeName.from( pathName );
        }
        catch ( IllegalArgumentException e )
        {
            nodeName = NodeName.from( "_noname_" );
        }
        return Node.create().
            nodeType( ContentConstants.CONTENT_NODE_COLLECTION ).
            nodeState( NodeState.DEFAULT ).
            id( nodeId ).
            timestamp( Instant.now() ).
            name( nodeName ).
            data( data ).
            inheritPermissions( true ).
            childOrder( ChildOrder.defaultOrder() ).
            build();
    }

    private String generatePathName( final String name )
    {
        return NamePrettyfier.create( name );
    }
}
