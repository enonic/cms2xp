package com.enonic.cms2xp.export;

import java.util.List;

import com.enonic.cms2xp.converter.SiteNodeConverter;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.core.structure.SiteEntity;

public class SiteExporter
{
    private final NodeExporter nodeExporter;

    private final SiteNodeConverter siteNodeConverter = new SiteNodeConverter();

    public SiteExporter( final NodeExporter nodeExporter )
    {
        this.nodeExporter = nodeExporter;
    }

    public void export( final List<SiteEntity> siteEntities, final NodePath parentNode )
    {
        for ( SiteEntity siteEntity : siteEntities )
        {
            //Converts the site to a node
            Node node = siteNodeConverter.convertToNode( siteEntity );
            node = Node.create( node ).
                parentPath( parentNode ).
                build();

            //Exports the node
            nodeExporter.exportNode( node );
        }
    }
}
