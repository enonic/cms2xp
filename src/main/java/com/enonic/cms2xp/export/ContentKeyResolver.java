package com.enonic.cms2xp.export;

import java.util.HashMap;
import java.util.Map;

import com.enonic.xp.node.NodeId;

import com.enonic.cms.core.content.ContentKey;

public final class ContentKeyResolver
{
    private final Map<ContentKey, NodeId> table;

    public ContentKeyResolver()
    {
        table = new HashMap<>();
    }

    public void add( final ContentKey contentKey, final NodeId nodeId )
    {
        this.table.put( contentKey, nodeId );
    }

    public NodeId resolve( final ContentKey contentKey )
    {
        return this.table.get( contentKey );
    }
}
