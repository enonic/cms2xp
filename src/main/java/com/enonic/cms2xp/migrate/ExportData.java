package com.enonic.cms2xp.migrate;

import java.nio.file.Path;
import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.converter.ContentNodeConverter;
import com.enonic.cms2xp.converter.ContentTypeConverter;
import com.enonic.cms2xp.converter.ContentTypeResolver;
import com.enonic.cms2xp.export.CategoryExporter;
import com.enonic.cms2xp.export.ContentExporter;
import com.enonic.cms2xp.export.ContentTypeExporter;
import com.enonic.cms2xp.hibernate.CategoryRetriever;
import com.enonic.cms2xp.hibernate.ContentTypeRetriever;
import com.enonic.cms2xp.hibernate.HibernateSessionProvider;
import com.enonic.cms2xp.hibernate.SiteRetriever;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.schema.content.ContentType;

import com.enonic.cms.framework.blob.file.FileBlobStore;

import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.structure.SiteEntity;

public final class ExportData
{
    private final static Logger logger = LoggerFactory.getLogger( ExportData.class );

    private final MainConfig config;

    private final NodeExporter nodeExporter;

    public ExportData( final MainConfig config )
    {
        this.config = config;

        final Path nodeTargetDirectory = this.config.target.exportDir.toPath();
        nodeExporter = NodeExporter.create().
            sourceNodePath( NodePath.ROOT ).
            rootDirectory( nodeTargetDirectory ).
            targetDirectory( nodeTargetDirectory ).
            exportNodeIds( true ).
            build();
    }

    public void execute()
        throws Exception
    {

        //Opens an Hibernate Session
        final Session session = new HibernateSessionProvider( config ).getSession();
        if ( !session.isConnected() )
        {
            logger.error( "Cannot connect to database: [{}, {}]", config.source.jdbcUrl, config.source.jdbcDriver );
            return;
        }

        try
        {
            //Retrieves, converts and exports the ContentTypes
            final ContentTypeConverter contentTypeConverter = new ContentTypeConverter( ApplicationKey.from( "com.enonic.xp.app.myApp" ) );
            exportContentTypes( session, contentTypeConverter );

            //Retrieves, converts and exports the Categories
            exportCategories( session, contentTypeConverter );

            //Retrieves, converts and exports the Sites
            exportSites( session );

            //Writes additional export information
            nodeExporter.writeExportProperties( "6.0.0" );
        }
        finally
        {
            session.close();
        }
    }

    private void exportContentTypes( Session session, final ContentTypeConverter contentTypeConverter )
    {
        //Retrieves the ContentTypeEntities
        logger.info( "Retrieving content types..." );
        final List<ContentTypeEntity> contentTypeEntities = new ContentTypeRetriever().retrieveContentTypes( session );
        logger.info( contentTypeEntities.size() + " content types retrieved." );

        //Converts the ContentTypeEntities to ContentTypes
        final ImmutableList<ContentType> contentTypeList = contentTypeConverter.export( contentTypeEntities );

        //Exports the ContentTypes
        logger.info( "Exporting content types..." );
        final Path contentTypesPath = config.target.applicationDir.toPath().resolve( "src/main/resources/site/content-types" );
        new ContentTypeExporter( contentTypesPath ).export( contentTypeList );
    }

    private void exportCategories( Session session, final ContentTypeResolver contentTypeResolver )
    {
        //Retrieves the CategoryEntities
        logger.info( "Retrieving root categories..." );
        final List<CategoryEntity> categoryEntities = new CategoryRetriever().retrieveRootCategories( session );
        logger.info( categoryEntities.size() + " root categories retrieved." );

        //Converts and exports the CategoryEntities
        logger.info( "Exporting root categories and children..." );
        exportCategories( categoryEntities, contentTypeResolver );
    }

    private void exportCategories( final List<CategoryEntity> categories, final ContentTypeResolver contentTypeResolver )
    {
        final FileBlobStore fileBlobStore = new FileBlobStore();
        fileBlobStore.setDirectory( config.source.blobStoreDir );

        final ContentNodeConverter contentNodeConverter = new ContentNodeConverter( contentTypeResolver );
        final ContentExporter contentExporter = new ContentExporter( nodeExporter, fileBlobStore, contentNodeConverter );
        final CategoryExporter exporter = new CategoryExporter( nodeExporter, contentExporter );

        exporter.export( categories, ContentConstants.CONTENT_ROOT_PATH );
    }

    private void exportSites( Session session )
    {
        //Retrieves the SiteEntities
        logger.info( "Retrieving sites..." );
        final List<SiteEntity> siteEntities = new SiteRetriever().retrieveSites( session );
        logger.info( siteEntities.size() + " sites retrieved." );
    }
}
