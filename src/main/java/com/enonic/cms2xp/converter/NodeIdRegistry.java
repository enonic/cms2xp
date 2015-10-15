package com.enonic.cms2xp.converter;

import java.util.HashMap;
import java.util.Map;

import com.enonic.xp.node.NodeId;

import com.enonic.cms.core.content.ContentKey;

public class NodeIdRegistry
{
    private final Map<ContentKey, NodeId> nodeIdByContentKeyMap = new HashMap<>();

    public NodeId getNodeId( ContentKey contentKey )
    {
        NodeId nodeId = nodeIdByContentKeyMap.get( contentKey );
        if ( nodeId == null )
        {
            nodeId = new NodeId();
            nodeIdByContentKeyMap.put( contentKey, nodeId );
        }
        return nodeId;
    }
}
