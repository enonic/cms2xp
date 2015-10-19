package com.enonic.cms2xp.export;

import java.util.Set;

import com.enonic.cms2xp.converter.PageTemplateNodeConverter;
import com.enonic.cms2xp.converter.SiteTemplatesNodeConverter;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;

public class TemplateExporter
{

    private final NodeExporter nodeExporter;

    private final SiteTemplatesNodeConverter siteTemplatesNodeConverter = new SiteTemplatesNodeConverter();

    private final PageTemplateNodeConverter pageTemplateNodeConverter;

    public TemplateExporter( final NodeExporter nodeExporter, final ApplicationKey applicationKey )
    {
        this.nodeExporter = nodeExporter;
        this.pageTemplateNodeConverter = new PageTemplateNodeConverter( applicationKey );

    }

    public void export( final SiteEntity siteEntity, final NodePath parentNodePath )
    {

        Node templateFolderNode = siteTemplatesNodeConverter.toNode( siteEntity );
        templateFolderNode = Node.create( templateFolderNode ).
            parentPath( parentNodePath ).
            build();
        nodeExporter.exportNode( templateFolderNode );

        final Set<PageTemplateEntity> pageTemplateEntities = siteEntity.getPageTemplates();
        if ( pageTemplateEntities != null )
        {
            for ( PageTemplateEntity pageTemplateEntity : pageTemplateEntities )
            {
                Node pageTemplateNode = pageTemplateNodeConverter.toNode( pageTemplateEntity );
                pageTemplateNode = Node.create( pageTemplateNode ).
                    parentPath( templateFolderNode.path() ).
                    build();
                nodeExporter.exportNode( pageTemplateNode );
            }
        }

    }
}
