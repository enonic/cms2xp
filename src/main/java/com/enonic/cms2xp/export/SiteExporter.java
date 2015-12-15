package com.enonic.cms2xp.export;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;

import com.enonic.cms2xp.converter.MenuItemNodeConverter;
import com.enonic.cms2xp.converter.SiteNodeConverter;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;

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
                         final ApplicationKey applicationKey, final ContentKeyResolver contentKeyResolver )
    {
        final PageTemplateResolver pageTemplateResolver = new PageTemplateResolver();
        this.nodeExporter = nodeExporter;
        this.siteNodeConverter = new SiteNodeConverter( applicationKey );
        this.portletExporter = new PortletExporter( session, partDirectory, nodeExporter, applicationKey );
        this.templateExporter =
            new TemplateExporter( nodeExporter, pageDirectory, applicationKey, pageTemplateResolver, this.portletExporter );
        this.menuItemNodeConverter =
            new MenuItemNodeConverter( applicationKey, contentKeyResolver, pageTemplateResolver, this.portletExporter );
    }

    public void export( final List<SiteEntity> siteEntities, final NodePath parentNodePath )
    {
        for ( SiteEntity siteEntity : siteEntities )
        {
            //Converts the site to a node
            Node siteNode = siteNodeConverter.convertToNode( siteEntity );
            siteNode = Node.create( siteNode ).
                parentPath( parentNodePath ).
                build();

            //Exports the node
            nodeExporter.exportNode( siteNode );

            portletExporter.export( siteEntity, siteNode.path() );

            templateExporter.export( siteEntity, siteNode.path() );

            //Export site menu items
            exportMenuItems( siteNode, siteEntity.getTopMenuItems() );
        }
    }

    private void exportMenuItems( final Node parentNode, final Collection<MenuItemEntity> menuItems )
    {
        for ( MenuItemEntity menuItemEntity : menuItems )
        {
            //Converts the menu item to a node
            Node node = menuItemNodeConverter.convertToNode( menuItemEntity );
            node = Node.create( node ).
                parentPath( parentNode.path() ).
                build();

            //Exports the node
            nodeExporter.exportNode( node );

            exportMenuItems( node, menuItemEntity.getChildren() );
        }
    }

}
