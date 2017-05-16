package com.enonic.cms2xp.migrate;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.converter.ContentNodeConverter;
import com.enonic.cms2xp.converter.ContentTypeConverter;
import com.enonic.cms2xp.converter.ContentTypeResolver;
import com.enonic.cms2xp.converter.ImageDescriptionResolver;
import com.enonic.cms2xp.converter.NodeIdRegistry;
import com.enonic.cms2xp.export.CategoryExporter;
import com.enonic.cms2xp.export.ContentExporter;
import com.enonic.cms2xp.export.ContentFilter;
import com.enonic.cms2xp.export.ContentKeyResolver;
import com.enonic.cms2xp.export.ContentTypeExporter;
import com.enonic.cms2xp.export.PrincipalKeyResolver;
import com.enonic.cms2xp.export.SiteExporter;
import com.enonic.cms2xp.export.SiteFilter;
import com.enonic.cms2xp.export.UserStoreExporter;
import com.enonic.cms2xp.hibernate.CategoryRetriever;
import com.enonic.cms2xp.hibernate.ContentTypeRetriever;
import com.enonic.cms2xp.hibernate.HibernateSessionProvider;
import com.enonic.cms2xp.hibernate.SiteRetriever;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.Input;
import com.enonic.xp.icon.Icon;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.framework.blob.file.FileBlobStore;

import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.structure.SiteEntity;

public final class ExportData
{
    private final static Logger logger = LoggerFactory.getLogger( ExportData.class );

    private static final String LIB_MENU_VERSION = "1.2.0";

    private static final String LIB_MENU = "com.enonic.lib:menu:" + LIB_MENU_VERSION;

    private final MainConfig config;

    private final NodeExporter nodeExporter;

    private final NodeExporter userNodeExporter;

    private final ApplicationKey applicationKey;

    private final ContentKeyResolver contentKeyResolver;

    private final PrincipalKeyResolver principalKeyResolver;

    private final NodeIdRegistry nodeIdRegistry;

    private ContentExporter contentExporter;

    public ExportData( final MainConfig config )
    {
        this.config = config;
        this.applicationKey = ApplicationKey.from( config.target.applicationName );
        this.contentKeyResolver = new ContentKeyResolver();
        this.principalKeyResolver = new PrincipalKeyResolver();
        this.nodeIdRegistry = new NodeIdRegistry();

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

            // export Mixins
            exportMixins();

            // include external Libs
            includeLibs();

            //Retrieves, converts and exports the Categories
            exportCategories( session, contentTypeConverter );

            //Retrieves, converts and exports the Sites
            exportSites( session );

            //Exports the resources
            exportResources();

            //Writes additional export information
            nodeExporter.writeExportProperties( "6.0.0" );

            logger.info( "Export completed successfully" );
        }
        catch ( Throwable t )
        {
            logger.error( "Export failed.", t );
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
        logger.info( contentTypeEntities.size() + " content types loaded." );

        //Converts the ContentTypeEntities to ContentTypes
        ImmutableList<ContentType> contentTypeList = contentTypeConverter.export( contentTypeEntities );

        final FormItemSet parameters = FormItemSet.create().
            name( "parameters" ).
            label( "Menu item parameters" ).
            addFormItem( Input.create().
                name( "name" ).
                label( "Parameter name" ).
                inputType( InputTypeName.TEXT_LINE ).
                required( true ).
                multiple( false ).
                build() ).
            addFormItem( Input.create().
                name( "value" ).
                label( "Parameter value" ).
                inputType( InputTypeName.TEXT_LINE ).
                required( false ).
                multiple( false ).
                build() ).
            addFormItem( Input.create().
                name( "override" ).
                label( "Override" ).
                helpText( "Can be overridden by the request" ).
                inputType( InputTypeName.CHECK_BOX ).
                required( false ).
                multiple( false ).
                build() ).
            build();

        //Adds the Content Type page
        final Form pageForm = Form.create().
            addFormItem( parameters.copy() ).
            build();

        final ContentType pageContentType = ContentType.create().
            name( ContentTypeName.from( this.applicationKey, "page" ) ).
            displayName( "Page" ).
            description( "" ).
            createdTime( Instant.now() ).
            superType( ContentTypeName.structured() ).
            form( pageForm ).
            icon( loadIcon( "page" ) ).
            build();
        //Adds the Content Type section
        final Form sectionForm = Form.create().
            addFormItem( Input.create().
                name( "sectionContents" ).
                label( "Contents" ).
                helpText( "Add contents to section" ).
                inputType( InputTypeName.CONTENT_SELECTOR ).
                required( false ).
                multiple( true ).
                build() ).
            addFormItem( parameters.copy() ).
            build();

        final ContentType sectionContentType = ContentType.create().
            name( ContentTypeName.from( this.applicationKey, "section" ) ).
            displayName( "Section" ).
            description( "" ).
            createdTime( Instant.now() ).
            superType( ContentTypeName.structured() ).
            form( sectionForm ).
            icon( loadIcon( "section" ) ).
            build();

        ImmutableList.Builder<ContentType> contentTypeListBuilder = ImmutableList.builder();
        contentTypeList = contentTypeListBuilder.addAll( contentTypeList ).
            add( pageContentType ).add( sectionContentType ).
            build();

        //Exports the ContentTypes
        logger.info( "Exporting content types..." );
        final Path contentTypesPath = config.target.applicationDir.toPath().resolve( "src/main/resources/site/content-types" );
        new ContentTypeExporter( contentTypesPath ).export( contentTypeList );
    }

    private void includeLibs()
        throws IOException
    {
        logger.info( "Including external libs..." );
        if ( config.target.exportMenuMixin )
        {
            includeLib( LIB_MENU );
        }
    }

    private void exportMixins()
        throws IOException
    {
        logger.info( "Exporting mixins..." );
        if ( config.target.exportPublishDateMixin )
        {
            exportMixin( "publishDate" );
        }
        if ( config.target.exportMenuMixin )
        {
            exportMixin( "menu-item" );
        }
        if ( config.target.exportCmsKeyMixin )
        {
            exportMixin( "cmsContent" );
        }
        if ( config.target.exportCmsMenuKeyMixin )
        {
            exportMixin( "cmsMenu" );
        }
    }

    private void exportMixin( final String name )
        throws IOException
    {
        final Path mixinsPath = config.target.applicationDir.toPath().resolve( "src/main/resources/site/mixins/" + name );
        mixinsPath.toFile().mkdirs();
        final Path mixinsFile = mixinsPath.resolve( name + ".xml" );

        final InputStream resource = getClass().getResourceAsStream( "/templates/mixins/" + name + ".xml" );
        Files.copy( resource, mixinsFile );

        // add line to site.xml <x-data mixin="_name_"/>
        final Path siteXml = config.target.applicationDir.toPath().resolve( "src/main/resources/site/site.xml" );
        final List<String> siteXmlLines = CharStreams.readLines( new FileReader( siteXml.toFile() ) );
        for ( int i = siteXmlLines.size() - 1; i > 0; i-- )
        {
            final String line = siteXmlLines.get( i );
            if ( line.trim().equals( "</site>" ) )
            {
                siteXmlLines.add( i, "  <x-data mixin=\"" + name + "\"/>" );
                break;
            }
        }
        Files.write( siteXml, siteXmlLines, StandardCharsets.UTF_8 );
    }

    private void includeLib( final String libArtifactId )
        throws IOException
    {
        final Path buildGradlePath = config.target.applicationDir.toPath().resolve( "build.gradle" );

        // add line to build.gradle: include "${libArtifactId}"
        final List<String> gradleLines = CharStreams.readLines( new FileReader( buildGradlePath.toFile() ) );
        int level = 0;
        boolean inDependenciesBlock = false;
        for ( int i = 0; i < gradleLines.size(); i++ )
        {
            final String line = gradleLines.get( i );
            if ( level == 0 && line.trim().equals( "dependencies {" ) )
            {
                inDependenciesBlock = true;
            }

            if ( line.trim().endsWith( "{" ) )
            {
                level++;
            }
            else if ( line.trim().endsWith( "}" ) )
            {
                level--;
            }

            if ( level == 0 && inDependenciesBlock )
            {
                gradleLines.add( i, "    include \"" + libArtifactId + "\"" );
                break;
            }
        }
        Files.write( buildGradlePath, gradleLines, StandardCharsets.UTF_8 );
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

    private void exportCategories( final Session session, final ContentTypeResolver contentTypeResolver )
    {
        //Retrieves the CategoryEntities
        logger.info( "Retrieving root categories..." );
        final List<CategoryKey> categoryEntities = CategoryRetriever.retrieveRootCategories( session );

        //Converts and exports the CategoryEntities
        logger.info( "Exporting root categories and children..." );
        exportCategories( session, categoryEntities, contentTypeResolver );
    }

    private void exportCategories( final Session session, final List<CategoryKey> categories,
                                   final ContentTypeResolver contentTypeResolver )
    {
        final FileBlobStore fileBlobStore = new FileBlobStore();
        fileBlobStore.setDirectory( config.source.blobStoreDir );

        final ImageDescriptionResolver imageDescriptionResolver = new ImageDescriptionResolver( session );
        final ContentFilter contentFilter = new ContentFilter( config.source.exclude, config.source.include );
        final ContentNodeConverter contentNodeConverter =
            new ContentNodeConverter( contentTypeResolver, this.principalKeyResolver, this.nodeIdRegistry, this.applicationKey, this.config,
                                      imageDescriptionResolver );
        this.contentExporter =
            new ContentExporter( nodeExporter, fileBlobStore, contentNodeConverter, this.contentKeyResolver, contentFilter );
        final CategoryExporter exporter =
            new CategoryExporter( session, nodeExporter, this.contentExporter, applicationKey, this.config, contentFilter );

        exporter.export( categories, ContentConstants.CONTENT_ROOT_PATH );
    }

    private void exportSites( Session session )
    {
        //Retrieves the SiteEntities
        logger.info( "Loading sites..." );
        final SiteFilter siteFilter = new SiteFilter( config.source.exclude, config.source.include );
        final List<SiteEntity> siteEntities = new SiteRetriever().retrieveSites( session ).stream().
            filter( siteFilter::includeSite ).
            collect( Collectors.toList() );
        logger.info( siteEntities.size() + " sites loaded." );

        //Converts and exports the Sites
        logger.info( "Exporting sites and children..." );
        final File pagesDirectory = new File( config.target.applicationDir, "src/main/resources/site/pages" );
        final File partsDirectory = new File( config.target.applicationDir, "src/main/resources/site/parts" );
        new SiteExporter( session, nodeExporter, this.contentExporter, pagesDirectory, partsDirectory, this.applicationKey,
                          this.contentKeyResolver, this.nodeIdRegistry, config ).
            export( siteEntities, ContentConstants.CONTENT_ROOT_PATH );
    }

    private void exportResources()
    {
        logger.info( "Exporting resources..." );
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
