package com.enonic.cms2xp.export;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.converter.CategoryNodeConverter;
import com.enonic.cms2xp.converter.SiteNodeConverter;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.index.ChildOrder;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeIndexPath;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.query.expr.FieldOrderExpr;
import com.enonic.xp.query.expr.OrderExpr;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.structure.menuitem.ContentHomeEntity;

import static com.google.common.collect.Iterables.getFirst;

public class CategoryExporter
{
    private final static Logger logger = LoggerFactory.getLogger( CategoryExporter.class );

    private final NodeExporter nodeExporter;

    private final ContentExporter contentExporter;

    private final CategoryNodeConverter categoryNodeConverter = new CategoryNodeConverter();

    private final SiteNodeConverter siteNodeConverter;

    private final MainConfig config;

    public CategoryExporter( final NodeExporter nodeExporter, final ContentExporter contentExporter, final ApplicationKey applicationKey,
                             final MainConfig config )
    {
        this.nodeExporter = nodeExporter;
        this.contentExporter = contentExporter;
        this.siteNodeConverter = new SiteNodeConverter( applicationKey );
        this.config = config;
    }

    public void export( final List<CategoryEntity> categories, final NodePath parentNode )
    {
        final NodePath parentPath;
        if ( parentNode.equals( ContentConstants.CONTENT_ROOT_PATH ) )
        {
            final Node topArchiveNode = createArchiveParent( parentNode );
            parentPath = topArchiveNode.path();
        }
        else
        {
            parentPath = parentNode;
        }

        final FieldOrderExpr orderByName = FieldOrderExpr.create( NodeIndexPath.NAME, OrderExpr.Direction.ASC );
        final ChildOrder childOrder = ChildOrder.create().add( orderByName ).build();
        for ( CategoryEntity category : categories )
        {
            //Converts the category to a node
            Node categoryNode = categoryNodeConverter.toNode( category );
            categoryNode = Node.create( categoryNode ).
                parentPath( parentPath ).
                childOrder( childOrder ).
                build();

            //Exports the node
            nodeExporter.exportNode( categoryNode );

            //Calls the export on the contents
            final Set<ContentEntity> contents = category.getContents();
            if ( !contents.isEmpty() )
            {
                for ( ContentEntity content : contents )
                {
                    if ( config.target.moveHomeContentToSection )
                    {
                        final Collection<ContentHomeEntity> homes = content.getContentHomes();
                        final ContentHomeEntity home = homes.size() == 1 ? getFirst( homes, null ) : null;
                        if ( home != null )
                        {
                            continue; // skip; content with a single section Home will be added under the section
                        }
                    }

                    try
                    {
                        contentExporter.export( content, categoryNode.path() );
                    }
                    catch ( Exception e )
                    {
                        logger.warn( "Could not export content: " + content.getPathAsString(), e );
                    }
                }
            }

            //Calls the export on the children
            final List<CategoryEntity> subCategories = category.getChildren();
            if ( !subCategories.isEmpty() )
            {
                export( subCategories, categoryNode.path() );
            }
        }
    }


    private Node createArchiveParent( final NodePath parentNodePath )
    {
        Node topArchiveNode = siteNodeConverter.topArchiveSiteToNode();
        topArchiveNode = Node.create( topArchiveNode ).
            parentPath( parentNodePath ).
            build();

        nodeExporter.exportNode( topArchiveNode );

        return topArchiveNode;
    }
}
