package com.enonic.cms2xp.export;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms2xp.converter.PageTemplateNodeConverter;
import com.enonic.cms2xp.converter.SiteTemplatesNodeConverter;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.core.impl.content.ContentPathNameGenerator;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateRegionEntity;

public class TemplateExporter
    extends AbstractAppComponentExporter
{
    private final static Logger logger = LoggerFactory.getLogger( ContentTypeExporter.class );

    private final NodeExporter nodeExporter;

    private final File pageDirectory;

    private final SiteTemplatesNodeConverter siteTemplatesNodeConverter = new SiteTemplatesNodeConverter();

    private final PageTemplateNodeConverter pageTemplateNodeConverter;

    public TemplateExporter( final NodeExporter nodeExporter, final File pageDirectory, final ApplicationKey applicationKey )
    {
        this.nodeExporter = nodeExporter;
        this.pageTemplateNodeConverter = new PageTemplateNodeConverter( applicationKey );
        this.pageDirectory = pageDirectory;
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

                exportAsPage( pageTemplateEntity );
            }
        }
    }

    private void exportAsPage( final PageTemplateEntity pageTemplateEntity )
    {
        final String pageTemplateDisplayName = pageTemplateEntity.getName();
        final String pageTemplateName = new ContentPathNameGenerator().generatePathName( pageTemplateDisplayName );

        final List<String> pageTemplateRegions = pageTemplateEntity.getPageTemplateRegions().
            stream().
            map( PageTemplateRegionEntity::getName ).
            sorted().
            collect( Collectors.toList() );

        Map<String, Object> mapping = new HashMap<>();
        mapping.put( "name", pageTemplateName );
        mapping.put( "displayName", pageTemplateDisplayName );
        mapping.put( "regions", pageTemplateRegions );

        try
        {
            copy( "/templates/page/page.html", new File( pageDirectory, pageTemplateName + "/" + pageTemplateName + ".html" ), mapping );
            copy( "/templates/page/page.js", new File( pageDirectory, pageTemplateName + "/" + pageTemplateName + ".js" ), mapping );
            copy( "/templates/page/page.xml", new File( pageDirectory, pageTemplateName + "/" + pageTemplateName + ".xml" ), mapping );
        }
        catch ( IOException e )
        {
            logger.error( "Cannot write page \"" + pageTemplateName + "\"", e );
        }
    }
}
