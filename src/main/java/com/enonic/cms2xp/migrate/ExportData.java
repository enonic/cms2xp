package com.enonic.cms2xp.migrate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.converter.ContentNodeConverter;
import com.enonic.cms2xp.export.CategoryExporter;
import com.enonic.cms2xp.export.ContentExporter;
import com.enonic.cms2xp.export.ContentTypeExporter;
import com.enonic.cms2xp.export.ContentTypeResolver;
import com.enonic.cms2xp.export.xml.XmlContentTypeSerializer;
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

    public ExportData( final MainConfig config )
    {
        this.config = config;
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
            final ContentTypeExporter contentTypeExporter = new ContentTypeExporter( ApplicationKey.from( "com.enonic.xp.app.myApp" ) );
            exportContentTypes( session, contentTypeExporter );

            //Retrieves, converts and exports the Categories
            exportCategories( session, contentTypeExporter );

            //Retrieves, converts and exports the Sites
            exportSites( session );
        }
        finally
        {
            session.close();
        }
    }

    private void exportContentTypes( Session session, final ContentTypeExporter contentTypeExporter )
    {
        logger.info( "Retrieving content types..." );
        final List<ContentTypeEntity> contentTypes = new ContentTypeRetriever().retrieveContentTypes( session );
        logger.info( contentTypes.size() + " content types retrieved." );
        final ImmutableList<ContentType> contentTypeList = contentTypeExporter.export( contentTypes );
        logger.info( "Exporting content types..." );
        exportContentTypes( contentTypeList );
    }

    private void exportCategories( Session session, final ContentTypeExporter contentTypeExporter )
    {
        logger.info( "Retrieving root categories..." );
        final List<CategoryEntity> categories = new CategoryRetriever().retrieveRootCategories( session );
        logger.info( categories.size() + " root categories retrieved." );
        logger.info( "Exporting root categories and children..." );
        export( categories, contentTypeExporter );
    }

    private void exportSites( Session session )
    {
        logger.info( "Retrieving sites..." );
        final List<SiteEntity> siteEntities = new SiteRetriever().retrieveSites( session );
        logger.info( siteEntities.size() + " sites retrieved." );
    }

    private void export( final List<CategoryEntity> categories, final ContentTypeResolver contentTypeResolver )
    {
        final Path targetDirectory = this.config.target.exportDir.toPath();

        final NodeExporter nodeExporter = NodeExporter.create().
            sourceNodePath( NodePath.ROOT ).
            rootDirectory( targetDirectory ).
            targetDirectory( targetDirectory ).
            exportNodeIds( true ).
            build();

        final FileBlobStore fileBlobStore = new FileBlobStore();
        fileBlobStore.setDirectory( config.source.blobStoreDir );

        final ContentNodeConverter contentNodeConverter = new ContentNodeConverter( contentTypeResolver );
        final ContentExporter contentExporter = new ContentExporter( nodeExporter, fileBlobStore, contentNodeConverter );
        final CategoryExporter exporter = new CategoryExporter( nodeExporter, contentExporter );

        exporter.export( categories, ContentConstants.CONTENT_ROOT_PATH );

        nodeExporter.writeExportProperties( "6.0.0" );
    }

    private void exportContentTypes( final Iterable<ContentType> contentTypes )
    {
        final Path contentTypesPath = config.target.applicationDir.toPath().resolve( "src/main/resources/site/content-types" );
        for ( ContentType contentType : contentTypes )
        {
            final String ct = new XmlContentTypeSerializer().contentType( contentType ).serialize();

            final String ctName = contentType.getName().getLocalName();
            try
            {
                final Path dir = Files.createDirectory( contentTypesPath.resolve( ctName ) );
                Files.write( dir.resolve( ctName + ".xml" ), ct.getBytes( StandardCharsets.UTF_8 ) );
            }
            catch ( Exception e )
            {
                logger.error( "Cannot write content type XML '{}'", ctName );
            }
        }
    }
}
