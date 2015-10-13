package com.enonic.cms2xp.migrate;

import java.nio.file.Path;
import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.export.CategoryExporter;
import com.enonic.cms2xp.hibernate.CategoryRetriever;
import com.enonic.cms2xp.hibernate.HibernateSessionProvider;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.core.content.category.CategoryEntity;

public final class ExportData
{
    private final static Logger logger = LoggerFactory.getLogger( ExportData.class );

    private final MainConfig config;

    public ExportData( final MainConfig config )
    {
        this.config = config;
    }

    public void execute()
        throws Exception
    {

        final Session session = new HibernateSessionProvider( config ).getSession();

        logger.info( "DB connected: " + session.isConnected() );
        logger.info( "Retrieving categories..." );
        final List<CategoryEntity> categories = CategoryRetriever.retrieveRootCategories( session );
        logger.info( categories.size() + " root categories retrieved." );
        logger.info( "Exporting categories..." );
        export( categories );

        session.close();
    }

    private void export( final List<CategoryEntity> categories )
    {
        final Path targetDirectory = this.config.target.exportDir.toPath();

        final NodeExporter nodeExporter = NodeExporter.create().
            sourceNodePath( NodePath.ROOT ).
            rootDirectory( targetDirectory ).
            targetDirectory( targetDirectory ).
            exportNodeIds( true ).
            build();

        CategoryExporter.export( nodeExporter, categories, ContentConstants.CONTENT_ROOT_PATH );

        nodeExporter.writeExportProperties( "6.0.0" );
    }

}
