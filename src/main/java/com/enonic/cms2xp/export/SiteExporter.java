package com.enonic.cms2xp.export;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.enonic.cms2xp.converter.MenuItemNodeConverter;
import com.enonic.cms2xp.converter.SiteNodeConverter;
import com.enonic.cms2xp.hibernate.PortletRetriever;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.portlet.PortletEntity;

public class SiteExporter
{
    private final NodeExporter nodeExporter;

    private final SiteNodeConverter siteNodeConverter;

    private final TemplateExporter templateExporter;

    private final MenuItemNodeConverter menuItemNodeConverter;

    private final ApplicationKey applicationKey;

    private final PortletRetriever portletRetriever;

    private final PageTemplateResolver pageTemplateResolver;

    public SiteExporter( final NodeExporter nodeExporter, final File pageDirectory, final ApplicationKey applicationKey,
                         final ContentKeyResolver contentKeyResolver, final PortletRetriever portletRetriever )
    {
        this.pageTemplateResolver = new PageTemplateResolver();
        this.nodeExporter = nodeExporter;
        this.siteNodeConverter = new SiteNodeConverter( applicationKey );
        this.templateExporter = new TemplateExporter( nodeExporter, pageDirectory, applicationKey, this.pageTemplateResolver );
        this.menuItemNodeConverter = new MenuItemNodeConverter( applicationKey, contentKeyResolver, this.pageTemplateResolver );
        this.portletRetriever = portletRetriever;
        this.applicationKey = applicationKey;
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

            //Calls the export on the page templates
            templateExporter.export( siteEntity, siteNode.path() );

            //Export site menu items
            exportMenuItems( siteNode, siteEntity.getTopMenuItems() );

            //Export portlets as fragments
            exportFragments( siteEntity, siteNode );
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

    private void exportFragments( final SiteEntity siteEntity, final Node siteNode )
    {
        final FragmentExporter fragmentExporter = new FragmentExporter( nodeExporter, applicationKey );
        final List<PortletEntity> portletEntities = portletRetriever.retrievePortlets( siteEntity.getKey() );

        fragmentExporter.export( portletEntities, siteNode.path() );
    }
}
