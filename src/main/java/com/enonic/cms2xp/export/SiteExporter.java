package com.enonic.cms2xp.export;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;

import com.enonic.cms2xp.converter.MenuItemNodeConverter;
import com.enonic.cms2xp.converter.NodeIdRegistry;
import com.enonic.cms2xp.converter.SiteNodeConverter;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.index.ChildOrder;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.Nodes;

import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;

public class SiteExporter
{
    private final NodeExporter nodeExporter;

    private final SiteNodeConverter siteNodeConverter;

    private final TemplateExporter templateExporter;

    private final MenuItemNodeConverter menuItemNodeConverter;

    private final PortletExporter portletExporter;

    public SiteExporter( final Session session, final NodeExporter nodeExporter, final File pageDirectory, final File partDirectory,
                         final ApplicationKey applicationKey, final ContentKeyResolver contentKeyResolver,
                         final NodeIdRegistry nodeIdRegistry )
    {
        final PageTemplateResolver pageTemplateResolver = new PageTemplateResolver();
        this.nodeExporter = nodeExporter;
        this.siteNodeConverter = new SiteNodeConverter( applicationKey );
        this.portletExporter = new PortletExporter( session, partDirectory, nodeExporter, applicationKey );
        this.templateExporter =
            new TemplateExporter( nodeExporter, pageDirectory, applicationKey, pageTemplateResolver, this.portletExporter );
        this.menuItemNodeConverter =
            new MenuItemNodeConverter( applicationKey, contentKeyResolver, pageTemplateResolver, this.portletExporter, nodeIdRegistry );
    }

    public void export( final List<SiteEntity> siteEntities, final NodePath parentNodePath )
    {
        Node topSiteNode = createSiteParent( parentNodePath );

        for ( SiteEntity siteEntity : siteEntities )
        {
            //Converts the site to a node
            Node siteNode = siteNodeConverter.convertToNode( siteEntity );
            siteNode = Node.create( siteNode ).
                parentPath( topSiteNode.path() ).
                childOrder( ChildOrder.manualOrder() ).
                build();

            //Exports the node
            nodeExporter.exportNode( siteNode );

            final Node portletsNode = portletExporter.export( siteEntity, siteNode.path() );

            final Node templatesNode = templateExporter.export( siteEntity, siteNode.path() );

            //Export site menu items
            final List<Node> menuNodes = exportMenuItems( siteNode, siteEntity.getTopMenuItems() );
            menuNodes.add( portletsNode );
            menuNodes.add( templatesNode );
            nodeExporter.writeNodeOrderList( siteNode, Nodes.from( menuNodes ) );
        }
    }

    private Node createSiteParent( final NodePath parentNodePath )
    {
        Node topSiteNode = siteNodeConverter.topSiteFolderToNode();
        topSiteNode = Node.create( topSiteNode ).
            parentPath( parentNodePath ).
            build();

        nodeExporter.exportNode( topSiteNode );

        return topSiteNode;
    }

    private List<Node> exportMenuItems( final Node parentNode, final Collection<MenuItemEntity> menuItems )
    {
        final List<Node> nodes = new ArrayList<>();
        for ( MenuItemEntity menuItemEntity : menuItems )
        {
            //Converts the menu item to a node
            Node menuItemNode = menuItemNodeConverter.convertToNode( menuItemEntity );
            final long order = menuItemEntity.getOrder().longValue();
            menuItemNode = Node.create( menuItemNode ).
                parentPath( parentNode.path() ).
                childOrder( ChildOrder.manualOrder() ).
                manualOrderValue( order ).
                build();

            //Exports the node
            nodeExporter.exportNode( menuItemNode );
            nodes.add( menuItemNode );

            final List<Node> menuNodes = exportMenuItems( menuItemNode, menuItemEntity.getChildren() );
            nodeExporter.writeNodeOrderList( menuItemNode, Nodes.from( menuNodes ) );
        }
        nodes.sort( ( n1, n2 ) -> Long.compare( n1.getManualOrderValue(), n2.getManualOrderValue() ) );
        return nodes;
    }

}
