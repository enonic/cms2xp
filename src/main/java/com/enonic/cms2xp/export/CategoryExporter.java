package com.enonic.cms2xp.export;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Ordering;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.converter.CategoryNodeConverter;
import com.enonic.cms2xp.converter.NodeIdRegistry;
import com.enonic.cms2xp.converter.SiteNodeConverter;
import com.enonic.cms2xp.hibernate.CategoryRetriever;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.index.ChildOrder;
import com.enonic.xp.index.IndexPath;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.query.expr.FieldOrderExpr;
import com.enonic.xp.query.expr.OrderExpr;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.structure.menuitem.ContentHomeEntity;

import static com.enonic.xp.content.ContentPropertyNames.MODIFIED_TIME;
import static com.google.common.collect.Iterables.getFirst;

public class CategoryExporter
{
    private final static Logger logger = LoggerFactory.getLogger( CategoryExporter.class );

    private static final OrderExpr DEFAULT_ORDER = FieldOrderExpr.create( IndexPath.from( MODIFIED_TIME ), OrderExpr.Direction.DESC );

    private static final ChildOrder DEFAULT_CHILD_ORDER = ChildOrder.create().add( DEFAULT_ORDER ).build();

    private static final Ordering<String> CASE_SENSITIVE_NULL_SAFE_ORDER = Ordering.from( String::compareTo ).nullsLast();

    private final NodeExporter nodeExporter;

    private final ContentExporter contentExporter;

    private final CategoryNodeConverter categoryNodeConverter;

    private final SiteNodeConverter siteNodeConverter;

    private final MainConfig config;

    private final Session session;

    private final ContentFilter contentFilter;

    public CategoryExporter( final Session session, final NodeExporter nodeExporter, final ContentExporter contentExporter,
                             final ApplicationKey applicationKey, final MainConfig config, final ContentFilter contentFilter,
                             final NodeIdRegistry nodeIdRegistry )
    {
        this.session = session;
        this.nodeExporter = nodeExporter;
        this.contentExporter = contentExporter;
        this.siteNodeConverter = new SiteNodeConverter( applicationKey );
        this.config = config;
        this.categoryNodeConverter = new CategoryNodeConverter( applicationKey, config, nodeIdRegistry );
        this.contentFilter = contentFilter;
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

        for ( CategoryKey categoryKey : categories )
        {
            session.clear(); // release memory
            CategoryEntity category = CategoryRetriever.retrieveCategory( session, categoryKey );
            if ( contentFilter.skipCategory( category ) )
            {
                continue;
            }
            logger.info( "Exporting category '" + category.getPathAsString() + "'" );
            //Converts the category to a node
            Node categoryNode = categoryNodeConverter.toNode( category );
            categoryNode = Node.create( categoryNode ).
                parentPath( parentPath ).
                childOrder( DEFAULT_CHILD_ORDER ).
                build();

            //Exports the node
            categoryNode = nodeExporter.exportNode( categoryNode );

            //Calls the export on the children
            // It's important to export categories first so it's the content (not the category folder) that gets renamed in case 2 nodes get the same path
            final List<CategoryKey> subCategories = CategoryRetriever.retrieveSubCategories( session, categoryKey );
            if ( !subCategories.isEmpty() )
            {
                export( subCategories, categoryNode.path() );
            }

            category = CategoryRetriever.retrieveCategory( session, categoryKey );

            //Calls the export on the contents
            final Set<ContentEntity> contents = category.getContents();
            if ( !contents.isEmpty() )
            {
                final List<ContentEntity> sortedContent = contents.stream().
                    filter( ( c ) -> !c.isDeleted() ).
                    filter( ( c ) -> !ignoreDrafts( c )).
                    sorted( ( c1, c2 ) -> {
                        int res = Objects.compare( c1.getName(), c2.getName(), CASE_SENSITIVE_NULL_SAFE_ORDER );
                        if ( res == 0 )
                        {
                            // if same name, first the approved one
                            final ContentStatus st1 = c1.getMainVersion().getStatus();
                            final ContentStatus st2 = c2.getMainVersion().getStatus();
                            return st1 == st2 ? 0 : st1 == ContentStatus.APPROVED ? -1 : 1;
                        }
                        return res;
                    } ).
                    collect( Collectors.toList() );

                int contentCount = 0;
                for ( ContentEntity content : sortedContent )
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
        }
    }

    private boolean ignoreDrafts( final ContentEntity c )
    {
        if (!config.source.ignoreDrafts) return false;
        return ( c.getMainVersion().getStatus() == ContentStatus.DRAFT);
    }

    private Node createArchiveParent( final NodePath parentNodePath )
    {
        Node topArchiveNode = siteNodeConverter.topArchiveSiteToNode();
        topArchiveNode = Node.create( topArchiveNode ).
            parentPath( parentNodePath ).
            childOrder( DEFAULT_CHILD_ORDER ).
            build();

        topArchiveNode = nodeExporter.exportNode( topArchiveNode );

        return topArchiveNode;
    }
}
