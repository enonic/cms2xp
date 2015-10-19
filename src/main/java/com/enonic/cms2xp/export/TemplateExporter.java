package com.enonic.cms2xp.export;

import com.enonic.cms2xp.converter.SiteTemplatesNodeConverter;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.core.structure.SiteEntity;

public class TemplateExporter
{

    private final NodeExporter nodeExporter;

    private final SiteTemplatesNodeConverter siteTemplatesNodeConverter = new SiteTemplatesNodeConverter();

    public TemplateExporter( final NodeExporter nodeExporter )
    {
        this.nodeExporter = nodeExporter;

    }

    public void export( final SiteEntity siteEntity, final NodePath parentNodePath )
    {

        Node templateFolderNode = siteTemplatesNodeConverter.toNode( siteEntity );
        templateFolderNode = Node.create( templateFolderNode ).
            parentPath( parentNodePath ).
            build();
        nodeExporter.exportNode( templateFolderNode );

//        final Set<PageTemplateEntity> pageTemplateEntities = siteEntity.getPageTemplates();
//        if ( pageTemplateEntities != null )
//        {
//            for ( PageTemplateEntity pageTemplateEntity : pageTemplateEntities )
//            {
//                createNode( pageTemplateEntity.getName(), templateFolderNode.path() );
//            }
//        }

    }
}
