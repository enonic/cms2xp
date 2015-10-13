package com.enonic.cms2xp.converter;


import java.time.Instant;

import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.core.impl.content.ContentPathNameGenerator;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeState;

public class NodeFactory
{
    public static Node createNode( String name, PropertyTree data )
    {
        return Node.create().
            nodeType( ContentConstants.CONTENT_NODE_COLLECTION ).
            nodeState( NodeState.DEFAULT ).
            id( new NodeId() ).
            timestamp( Instant.now() ).
            name( generatePathName( name ) ).
            data( data ).
            inheritPermissions( true ).
            build();
    }

    private static String generatePathName( final String name )
    {
        return new ContentPathNameGenerator().generatePathName( name );
    }
}
