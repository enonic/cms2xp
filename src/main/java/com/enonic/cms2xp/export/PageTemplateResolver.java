package com.enonic.cms2xp.export;

import java.util.HashMap;
import java.util.Map;

import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.NodeId;

import com.enonic.cms.core.structure.page.template.PageTemplateKey;

public final class PageTemplateResolver
{
    private final Map<PageTemplateKey, NodeId> templateNodeIdTable;

    private final Map<PageTemplateKey, PropertyTree> templatePageDataTable;

    public PageTemplateResolver()
    {
        templateNodeIdTable = new HashMap<>();
        templatePageDataTable = new HashMap<>();
    }

    public void add( final PageTemplateKey pageTemplate, final NodeId nodeId )
    {
        this.templateNodeIdTable.put( pageTemplate, nodeId );
    }

    public void add( final PageTemplateKey pageTemplate, final PropertyTree pageData )
    {
        this.templatePageDataTable.put( pageTemplate, pageData );
    }

    public NodeId resolveTemplateNodeId( final PageTemplateKey pageTemplate )
    {
        return this.templateNodeIdTable.get( pageTemplate );
    }

    public PropertyTree resolveTemplatePageData( final PageTemplateKey pageTemplate )
    {
        return this.templatePageDataTable.get( pageTemplate );
    }
}
