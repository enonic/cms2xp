package com.enonic.cms2xp.export;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.converter.CategoryNodeConverter;
import com.enonic.cms2xp.converter.SiteNodeConverter;
import com.enonic.cms2xp.hibernate.CategoryRetriever;
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
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.structure.menuitem.ContentHomeEntity;

import static com.google.common.collect.Iterables.getFirst;

public class CategoryExporter
{
    private final static Logger logger = LoggerFactory.getLogger( CategoryExporter.class );

    private final NodeExporter nodeExporter;

    private final ContentExporter contentExporter;

    private final CategoryNodeConverter categoryNodeConverter;

    private final SiteNodeConverter siteNodeConverter;

    private final MainConfig config;

    private final Session session;

    public CategoryExporter( final Session session, final NodeExporter nodeExporter, final ContentExporter contentExporter,
                             final ApplicationKey applicationKey, final MainConfig config )
    {
        this.session = session;
        this.nodeExporter = nodeExporter;
        this.contentExporter = contentExporter;
        this.siteNodeConverter = new SiteNodeConverter( applicationKey );
        this.config = config;
        this.categoryNodeConverter = new CategoryNodeConverter( applicationKey, config );
    }

    public void export( final List<CategoryKey> categories, final NodePath parentNode )
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
        for ( CategoryKey categoryKey : categories )
        {
            session.clear(); // release memory
            CategoryEntity category = CategoryRetriever.retrieveCategory( session, categoryKey );
            logger.info( "Exporting category '" + category.getPathAsString() + "'" );
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
                int contentCount = 0;
                for ( ContentEntity content : contents )
                {
                    contentCount++;
                    if ( contentCount % 20 == 0 )
                    {
                        logger.info( contentCount + " contents exported" );
                    }
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
                if ( contentCount % 20 != 0 )
                {
                    logger.info( contentCount + " contents exported" );
                }
            }

            //Calls the export on the children
            final List<CategoryKey> subCategories = CategoryRetriever.retrieveSubCategories( session, categoryKey );
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
