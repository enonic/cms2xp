package com.enonic.cms2xp.export;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.converter.MenuItemNodeConverter;
import com.enonic.cms2xp.converter.NodeIdRegistry;
import com.enonic.cms2xp.converter.SiteNodeConverter;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.index.ChildOrder;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.Nodes;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.menuitem.ContentHomeEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.section.SectionContentEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;
import com.enonic.cms.core.structure.portlet.PortletKey;

import static com.google.common.collect.Iterables.getFirst;

public class SiteExporter
{
    private final static Logger logger = LoggerFactory.getLogger( SiteExporter.class );

    private final NodeExporter nodeExporter;

    private final SiteNodeConverter siteNodeConverter;

    private final TemplateExporter templateExporter;

    private final MenuItemNodeConverter menuItemNodeConverter;

    private final PortletExporter portletExporter;

    private final ContentExporter contentExporter;

    private final MainConfig config;

    private final Set<ContentKey> contentKeysMovedToSection;

    private int menuCount = 0;

    public SiteExporter( final Session session, final NodeExporter nodeExporter, final ContentExporter contentExporter,
                         final File pageDirectory, final File partDirectory, final ApplicationKey applicationKey,
                         final NodeIdRegistry nodeIdRegistry, final MainConfig config )
    {
        final PageTemplateResolver pageTemplateResolver = new PageTemplateResolver();
        this.nodeExporter = nodeExporter;
        this.contentExporter = contentExporter;
        this.siteNodeConverter = new SiteNodeConverter( applicationKey );
        this.portletExporter = new PortletExporter( session, partDirectory, nodeExporter, applicationKey );
        this.templateExporter =
            new TemplateExporter( nodeExporter, pageDirectory, applicationKey, pageTemplateResolver, this.portletExporter, nodeIdRegistry );
        this.menuItemNodeConverter =
            new MenuItemNodeConverter( applicationKey, pageTemplateResolver, this.portletExporter, nodeIdRegistry, config );
        this.config = config;
        this.contentKeysMovedToSection = new HashSet<>();
    }

    public void export( final List<SiteEntity> siteEntities, final NodePath parentNodePath )
    {
        Node topSiteNode = createSiteParent( parentNodePath );

        for ( SiteEntity siteEntity : siteEntities )
        {
            logger.info( "Exporting site " + siteEntity.getName() );

            //Converts the site to a node
            Node siteNode = siteNodeConverter.convertToNode( siteEntity );
            siteNode = Node.create( siteNode ).
                parentPath( topSiteNode.path() ).
                childOrder( ChildOrder.manualOrder() ).
                build();

            //Exports the node
            siteNode = nodeExporter.exportNode( siteNode );

            final Set<PortletKey> singleUsePortlets = findSingleUsePortlets( siteEntity.getPageTemplates() );

            final Node portletsNode = portletExporter.export( siteEntity, siteNode.path(), singleUsePortlets );
            logger.info( "Exporting site portlets (" + siteEntity.getName() + ")" );

            final Node templatesNode = templateExporter.export( siteEntity, siteNode.path(), singleUsePortlets );
            logger.info( "Exporting site templates (" + siteEntity.getName() + ")" );

            //Export site menu items
            logger.info( "Exporting site menu items (" + siteEntity.getName() + ")" );
            menuCount = 0;
            final List<Node> menuNodes = exportMenuItems( siteNode, siteEntity.getTopMenuItems() );

            menuNodes.add( portletsNode );
            menuNodes.add( templatesNode );
            nodeExporter.writeNodeOrderList( siteNode, Nodes.from( menuNodes ) );
        }
    }

    private Set<PortletKey> findSingleUsePortlets( final Collection<PageTemplateEntity> pageTemplates )
    {
        final Map<PortletKey, Integer> portletCount = new HashMap<>();
        for ( PageTemplateEntity pageTemplateEntity : pageTemplates )
        {
            pageTemplateEntity.getPortlets().forEach( ( portlet ) -> {
                final PortletKey key = portlet.getPortlet().getPortletKey();
                portletCount.put( key, portletCount.getOrDefault( key, 0 ) + 1 );
            } );
        }
        return portletCount.entrySet().stream().
            filter( ( entry ) -> entry.getValue() == 1 ).
            map( Map.Entry::getKey ).
            collect( Collectors.toSet() );
    }

    private Node createSiteParent( final NodePath parentNodePath )
    {
        Node topSiteNode = siteNodeConverter.topSiteFolderToNode();
        topSiteNode = Node.create( topSiteNode ).
            parentPath( parentNodePath ).
            build();

        topSiteNode = nodeExporter.exportNode( topSiteNode );

        return topSiteNode;
    }

    private List<Node> exportMenuItems( final Node parentNode, final Collection<MenuItemEntity> menuItems )
    {
        final List<Node> nodes = new ArrayList<>();
        for ( MenuItemEntity menuItemEntity : menuItems )
        {
            menuCount++;
            if ( menuCount % 20 == 0 )
            {
                logger.info( menuCount + " menu items exported" );
            }

            //Converts the menu item to a node
            Node menuItemNode = menuItemNodeConverter.convertToNode( menuItemEntity );
            final long order = menuItemEntity.getOrder().longValue();
            menuItemNode = Node.create( menuItemNode ).
                parentPath( parentNode.path() ).
                childOrder( ChildOrder.manualOrder() ).
                manualOrderValue( order ).
                build();

            //Exports the node
            menuItemNode = nodeExporter.exportNode( menuItemNode );
            nodes.add( menuItemNode );
            List<Node> sectionHomeContent = null;

            final List<Node> menuNodes = exportMenuItems( menuItemNode, menuItemEntity.getChildren() );

            if ( config.target.moveHomeContentToSection && menuItemEntity.isSection() )
            {
                sectionHomeContent = exportSingleHomeSectionContent( menuItemEntity, menuItemNode.path() );
            }
            if ( sectionHomeContent != null )
            {
                menuNodes.addAll( sectionHomeContent );
            }
            nodeExporter.writeNodeOrderList( menuItemNode, Nodes.from( menuNodes ) );
        }
        nodes.sort( Comparator.comparingLong( Node::getManualOrderValue ) );
        return nodes;
    }

    private List<Node> exportSingleHomeSectionContent( final MenuItemEntity menuItemEntity, final NodePath parentPath )
    {
        final Set<Integer> addedContentKeys = new HashSet<>();
        final List<Node> sectionNodesAdded = new ArrayList<>();
        for ( SectionContentEntity sectionContent : menuItemEntity.getSectionContents() )
        {
            final Collection<ContentHomeEntity> homes = sectionContent.getContent().getContentHomes();
            final ContentHomeEntity home = homes.size() == 1 ? getFirst( homes, null ) : null;
            if ( home != null )
            {
                final ContentEntity content = home.getContent();
                if ( contentKeysMovedToSection.contains( content.getKey() ) )
                {
                    continue;
                }
                if ( addedContentKeys.contains( content.getKey().toInt() ) )
                {
                    continue;
                }

                final Node node = contentExporter.export( content, parentPath );
                addedContentKeys.add( content.getKey().toInt() );
                contentKeysMovedToSection.add( content.getKey() );

                if ( node != null )
                {
                    sectionNodesAdded.add( node );
                }
            }
        }
        return sectionNodesAdded;
    }

}
