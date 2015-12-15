package com.enonic.cms2xp.migrate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.apache.commons.io.FileUtils;
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
import com.enonic.cms2xp.export.ContentKeyResolver;
import com.enonic.cms2xp.export.ContentTypeExporter;
import com.enonic.cms2xp.export.PrincipalKeyResolver;
import com.enonic.cms2xp.export.SiteExporter;
import com.enonic.cms2xp.export.UserStoreExporter;
import com.enonic.cms2xp.hibernate.CategoryRetriever;
import com.enonic.cms2xp.hibernate.ContentTypeRetriever;
import com.enonic.cms2xp.hibernate.HibernateSessionProvider;
import com.enonic.cms2xp.hibernate.SiteRetriever;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.Input;
import com.enonic.xp.icon.Icon;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.framework.blob.file.FileBlobStore;

import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.structure.SiteEntity;

public final class ExportData
{
    private final static Logger logger = LoggerFactory.getLogger( ExportData.class );

    private static final Form SECTION_FORM = Form.create().
        addFormItem( Input.create().
            name( "sectionContents" ).
            label( "Contents" ).
            helpText( "Add contents to section" ).
            inputType( InputTypeName.CONTENT_SELECTOR ).
            required( false ).
            multiple( true ).
            build() ).
        build();

    private final MainConfig config;

    private final NodeExporter nodeExporter;

    private final NodeExporter userNodeExporter;

    private final ApplicationKey applicationKey;

    private final ContentKeyResolver contentKeyResolver;

    private final PrincipalKeyResolver principalKeyResolver;

    public ExportData( final MainConfig config )
    {
        this.config = config;
        this.applicationKey = ApplicationKey.from( config.target.applicationName );
        this.contentKeyResolver = new ContentKeyResolver();
        this.principalKeyResolver = new PrincipalKeyResolver();

        final Path nodeTargetDirectory = this.config.target.exportDir.toPath();
        nodeExporter = NodeExporter.create().
            sourceNodePath( NodePath.ROOT ).
            rootDirectory( nodeTargetDirectory ).
            targetDirectory( nodeTargetDirectory ).
            exportNodeIds( true ).
            build();

        final Path userNodeTargetDirectory = this.config.target.userExportDir.toPath();
        userNodeExporter = NodeExporter.create().
            sourceNodePath( NodePath.ROOT ).
            rootDirectory( userNodeTargetDirectory ).
            targetDirectory( userNodeTargetDirectory ).
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
            //Retrieves, converts and exports user stores
            exportUserStores( session );

            //Retrieves, converts and exports the ContentTypes
            final ContentTypeConverter contentTypeConverter = new ContentTypeConverter( this.applicationKey );
            exportContentTypes( session, contentTypeConverter );

            //Retrieves, converts and exports the Categories
            exportCategories( session, contentTypeConverter );

            //Retrieves, converts and exports the Sites
            exportSites( session );

            //Exports the resources
            exportResources();

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
        ImmutableList<ContentType> contentTypeList = contentTypeConverter.export( contentTypeEntities );

        //Adds the Content Type page
        final ContentType pageContentType = ContentType.create().
            name( ContentTypeName.from( this.applicationKey, "page" ) ).
            displayName( "Page" ).
            description( "" ).
            createdTime( Instant.now() ).
            superType( ContentTypeName.structured() ).
            icon( loadIcon( "page" ) ).
            build();
        //Adds the Content Type section
        final ContentType sectionContentType = ContentType.create().
            name( ContentTypeName.from( this.applicationKey, "section" ) ).
            displayName( "Section" ).
            description( "" ).
            createdTime( Instant.now() ).
            superType( ContentTypeName.structured() ).
            form( SECTION_FORM ).
            icon( loadIcon( "section" ) ).
            build();
        final ContentType fragmentContentType = ContentType.create().
            name( ContentTypeName.from( this.applicationKey, "fragment" ) ).
            displayName( "Fragment" ).
            description( "" ).
            createdTime( Instant.now() ).
            superType( ContentTypeName.structured() ).
            icon( loadIcon( "fragment" ) ).
            build();

        ImmutableList.Builder<ContentType> contentTypeListBuilder = ImmutableList.builder();
        contentTypeList = contentTypeListBuilder.addAll( contentTypeList ).
            add( pageContentType ).add( sectionContentType ).add( fragmentContentType ).
            build();

        //Exports the ContentTypes
        logger.info( "Exporting content types..." );
        final Path contentTypesPath = config.target.applicationDir.toPath().resolve( "src/main/resources/site/content-types" );
        new ContentTypeExporter( contentTypesPath ).export( contentTypeList );
    }

    private Icon loadIcon( final String name )
    {
        final InputStream resource = getClass().getResourceAsStream( "/icons/" + name + ".png" );
        if ( resource == null )
        {
            return null;
        }
        return Icon.from( resource, "image/png", Instant.now() );
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
        //TODO Refactor this part.
        final FileBlobStore fileBlobStore = new FileBlobStore();
        fileBlobStore.setDirectory( config.source.blobStoreDir );

        final ContentNodeConverter contentNodeConverter = new ContentNodeConverter( contentTypeResolver, this.principalKeyResolver );
        final ContentExporter contentExporter =
            new ContentExporter( nodeExporter, fileBlobStore, contentNodeConverter, this.contentKeyResolver );
        final CategoryExporter exporter = new CategoryExporter( nodeExporter, contentExporter );

        exporter.export( categories, ContentConstants.CONTENT_ROOT_PATH );
    }

    private void exportSites( Session session )
    {
        //Retrieves the SiteEntities
        logger.info( "Retrieving sites..." );
        final List<SiteEntity> siteEntities = new SiteRetriever().retrieveSites( session );
        logger.info( siteEntities.size() + " sites retrieved." );

        //Converts and exports the Sites
        logger.info( "Exporting sites and children..." );
        final File pagesDirectory = new File( config.target.applicationDir, "src/main/resources/site/pages" );
        final File partsDirectory = new File( config.target.applicationDir, "src/main/resources/site/parts" );
        new SiteExporter( session, nodeExporter, pagesDirectory, partsDirectory, this.applicationKey, this.contentKeyResolver ).
            export( siteEntities, ContentConstants.CONTENT_ROOT_PATH );
    }

    private void exportResources()
    {
        File source = config.source.resourcesDir.toPath().resolve( "_public" ).toFile();
        File target = new File( config.target.applicationDir, "src/main/resources/site/assets" );
        if ( source.isDirectory() )
        {
            try
            {
                FileUtils.copyDirectory( source, target );
            }
            catch ( IOException e )
            {
                logger.error( "Error while exporting resource.", e );
            }
        }
    }

    private void exportUserStores( final Session session )
    {
        final UserStoreExporter exporter = new UserStoreExporter( session, userNodeExporter, this.principalKeyResolver );
        exporter.export();
    }
}
