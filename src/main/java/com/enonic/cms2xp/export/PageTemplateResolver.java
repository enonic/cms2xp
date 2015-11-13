package com.enonic.cms2xp.export;

import java.util.HashMap;
import java.util.Map;

import com.enonic.xp.node.NodeId;

import com.enonic.cms.core.structure.page.template.PageTemplateKey;

public final class PageTemplateResolver
{
    private final Map<PageTemplateKey, NodeId> table;

    public PageTemplateResolver()
    {
        table = new HashMap<>();
    }

    public void add( final PageTemplateKey pageTemplate, final NodeId nodeId )
    {
        this.table.put( pageTemplate, nodeId );
    }

    public NodeId resolve( final PageTemplateKey pageTemplate )
    {
        return this.table.get( pageTemplate );
    }
}
