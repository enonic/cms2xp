package com.enonic.cms2xp.export;

import java.util.List;

import com.enonic.cms2xp.converter.SiteNodeConverter;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.core.structure.SiteEntity;

public class SiteExporter
{
    private final NodeExporter nodeExporter;

    private final SiteNodeConverter siteNodeConverter;

    private final TemplateExporter templateExporter;

    public SiteExporter( final NodeExporter nodeExporter, final ApplicationKey applicationKey )
    {
        this.nodeExporter = nodeExporter;
        this.siteNodeConverter = new SiteNodeConverter( applicationKey );
        this.templateExporter = new TemplateExporter( nodeExporter );
    }

    public void export( final List<SiteEntity> siteEntities, final NodePath parentNodePath )
    {
        for ( SiteEntity siteEntity : siteEntities )
        {
            //Converts the site to a node
            Node node = siteNodeConverter.convertToNode( siteEntity );
            node = Node.create( node ).
                parentPath( parentNodePath ).
                build();

            //Exports the node
            nodeExporter.exportNode( node );

            //Calls the export on the page templates
            templateExporter.export( siteEntity, node.path() );
        }
    }
}
