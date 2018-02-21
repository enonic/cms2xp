package com.enonic.cms2xp.converter;

import java.util.HashMap;
import java.util.Map;

import com.enonic.xp.node.NodeId;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;

public class NodeIdRegistry
{
    private final Map<ContentKey, NodeId> nodeIdByContentKeyMap = new HashMap<>();

    private final Map<MenuItemKey, NodeId> nodeIdByMenuKeyMap = new HashMap<>();

    private final Map<CategoryKey, NodeId> nodeIdByCategoryKeyMap = new HashMap<>();

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

    public NodeId getNodeId( MenuItemKey menuItemKey )
    {
        NodeId nodeId = nodeIdByMenuKeyMap.get( menuItemKey );
        if ( nodeId == null )
        {
            nodeId = new NodeId();
            nodeIdByMenuKeyMap.put( menuItemKey, nodeId );
        }
        return nodeId;
    }

    public NodeId getNodeId( final CategoryKey categoryKey )
    {
        NodeId nodeId = nodeIdByCategoryKeyMap.get( categoryKey );
        if ( nodeId == null )
        {
            nodeId = new NodeId();
            nodeIdByCategoryKeyMap.put( categoryKey, nodeId );
        }
        return nodeId;
    }
}
