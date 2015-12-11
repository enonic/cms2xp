package com.enonic.cms2xp.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms2xp.converter.PageTemplateNodeConverter;
import com.enonic.cms2xp.converter.SiteTemplatesNodeConverter;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.core.impl.content.ContentPathNameGenerator;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.index.ChildOrder;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.Nodes;

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

    private final PageTemplateResolver pageTemplateResolver;

    private final Map<String, String> xsltExported;

    public TemplateExporter( final NodeExporter nodeExporter, final File pageDirectory, final ApplicationKey applicationKey,
                             final PageTemplateResolver pageTemplateResolver )
    {
        this.nodeExporter = nodeExporter;
        this.pageTemplateNodeConverter = new PageTemplateNodeConverter( applicationKey );
        this.pageDirectory = pageDirectory;
        this.pageTemplateResolver = pageTemplateResolver;
        this.xsltExported = new HashMap<>();
    }

    public void export( final SiteEntity siteEntity, final NodePath parentNodePath )
    {

        Node templateFolderNode = siteTemplatesNodeConverter.toNode( siteEntity );
        templateFolderNode = Node.create( templateFolderNode ).
            parentPath( parentNodePath ).
            childOrder( ChildOrder.manualOrder() ).
            build();
        nodeExporter.exportNode( templateFolderNode );

        final Set<PageTemplateEntity> pageTemplateEntities = siteEntity.getPageTemplates();
        if ( pageTemplateEntities != null )
        {
            // sort page templates to make sure the first one (default) has regions
            Comparator<PageTemplateEntity> byOrder = Comparator.comparing( p -> p.getPageTemplateRegions().isEmpty() );
            byOrder = byOrder.thenComparing( PageTemplateEntity::getName );
            final List<PageTemplateEntity> pageTemplates = pageTemplateEntities.stream().sorted( byOrder ).collect( Collectors.toList() );

            exportPages( pageTemplates );
            final List<Node> children = new ArrayList<>();
            for ( PageTemplateEntity pageTemplateEntity : pageTemplates )
            {
                //Exports the PageTemplateEntity as a template node
                final String xsltPath = pageTemplateEntity.getStyleKey().toString();
                final String pageName = this.xsltExported.get( xsltPath );
                Node pageTemplateNode = pageTemplateNodeConverter.toNode( pageTemplateEntity, pageName );
                pageTemplateNode = Node.create( pageTemplateNode ).
                    parentPath( templateFolderNode.path() ).
                    build();
                nodeExporter.exportNode( pageTemplateNode );
                children.add( pageTemplateNode );
                pageTemplateResolver.add( pageTemplateEntity.getPageTemplateKey(), pageTemplateNode.id() );
            }

            nodeExporter.writeNodeOrderList( templateFolderNode, Nodes.from( children ) );
        }
    }

    private void exportPages( final List<PageTemplateEntity> pageTemplates )
    {
        for ( PageTemplateEntity pageTemplateEntity : pageTemplates )
        {
            final String xsltPath = pageTemplateEntity.getStyleKey().toString();
            if ( xsltExported.containsKey( xsltPath ) )
            {
                continue;
            }

            String pageName = pageNameFromXslt( pageTemplateEntity );
            int i = 1;
            while ( xsltExported.values().contains( pageName ) )
            {
                pageName = pageNameFromXslt( pageTemplateEntity ) + "-" + ( ++i );
            }

            //Exports the PageTemplateEntity as a page
            exportAsPage( pageTemplateEntity, pageName );
            xsltExported.put( xsltPath, pageName );
        }
    }

    private void exportAsPage( final PageTemplateEntity pageTemplateEntity, final String pageName )
    {
        //Prepares the mappings
        final String pageTemplateDisplayName = pageTemplateEntity.getName();

        final List<String> pageTemplateRegions = pageTemplateEntity.getPageTemplateRegions().
            stream().
            map( PageTemplateRegionEntity::getName ).
            sorted().
            collect( Collectors.toList() );

        Map<String, Object> mapping = new HashMap<>();
        mapping.put( "name", pageName );
        mapping.put( "displayName", pageTemplateDisplayName );
        mapping.put( "regions", pageTemplateRegions );

        //Copies page templates and applies the mapping on these file
        try
        {
            copy( "/templates/page/page.html", new File( pageDirectory, pageName + "/" + pageName + ".html" ), mapping );
            copy( "/templates/page/page.js", new File( pageDirectory, pageName + "/" + pageName + ".js" ), mapping );
            copy( "/templates/page/page.xml", new File( pageDirectory, pageName + "/" + pageName + ".xml" ), mapping );
        }
        catch ( IOException e )
        {
            logger.error( "Cannot write page \"" + pageName + "\"", e );
        }
    }

    private String pageNameFromXslt( final PageTemplateEntity pageTemplateEntity )
    {
        final String xsltName = FilenameUtils.removeExtension( pageTemplateEntity.getStyleKey().getName() );
        return new ContentPathNameGenerator().generatePathName( xsltName );
    }
}
