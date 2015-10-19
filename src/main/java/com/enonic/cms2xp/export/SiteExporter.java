package com.enonic.cms2xp.export;

import java.util.Collection;
import java.util.List;

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

    public SiteExporter( final NodeExporter nodeExporter, final ApplicationKey applicationKey )
    {
        this.nodeExporter = nodeExporter;
        this.siteNodeConverter = new SiteNodeConverter( applicationKey );
        this.templateExporter = new TemplateExporter( nodeExporter );
        this.menuItemNodeConverter = new MenuItemNodeConverter( applicationKey );
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

            //Export site menu items
            exportMenuItems( node, siteEntity.getTopMenuItems() );
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
