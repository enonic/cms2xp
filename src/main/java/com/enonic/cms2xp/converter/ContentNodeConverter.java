package com.enonic.cms2xp.converter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.export.PrincipalKeyResolver;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.core.impl.content.ContentIndexConfigFactory;
import com.enonic.xp.core.impl.schema.content.BuiltinContentTypes;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.Value;
import com.enonic.xp.form.Form;
import com.enonic.xp.index.IndexConfigDocument;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.acl.AccessControlEntry;
import com.enonic.xp.security.acl.AccessControlList;
import com.enonic.xp.security.acl.Permission;
import com.enonic.xp.util.Reference;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.access.ContentAccessEntity;
import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.ContentDataParser;
import com.enonic.cms.core.content.contentdata.custom.DataEntry;
import com.enonic.cms.core.content.contentdata.legacy.LegacyFileContentData;
import com.enonic.cms.core.content.contentdata.legacy.LegacyFormContentData;
import com.enonic.cms.core.content.contentdata.legacy.LegacyImageContentData;
import com.enonic.cms.core.content.contentdata.legacy.LegacyNewsletterContentData;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.content.contenttype.ContentTypeKey;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupType;
import com.enonic.cms.core.structure.menuitem.ContentHomeEntity;

import static com.enonic.xp.content.ContentPropertyNames.PUBLISH_FROM;
import static com.enonic.xp.content.ContentPropertyNames.PUBLISH_INFO;
import static com.enonic.xp.content.ContentPropertyNames.PUBLISH_TO;

public final class ContentNodeConverter
    extends AbstractNodeConverter
{
    private static final Map<ContentHandlerName, ContentTypeName> SYSTEM_TYPES =
        ImmutableMap.<ContentHandlerName, ContentTypeName>builder().
            put( ContentHandlerName.CUSTOM, ContentTypeName.unstructured() ).
            put( ContentHandlerName.IMAGE, ContentTypeName.imageMedia() ).
            put( ContentHandlerName.FILE, ContentTypeName.unknownMedia() ).
            build();

    private static final BuiltinContentTypes BUILTIN_CONTENT_TYPES = new BuiltinContentTypes();

    private final static Logger logger = LoggerFactory.getLogger( ContentNodeConverter.class );

    private static final Form EMPTY_FORM = Form.create().build();

    private static final String PUBLISH_FIRST = "first";

    private final NodeIdRegistry nodeIdRegistry;

    private final DataEntryValuesConverter dataEntryValuesConverter;

    private final FormValuesConverter formValuesConverter;

    private final NewsletterValuesConverter newsletterValuesConverter;

    private final ContentTypeResolver contentTypeResolver;

    private final PrincipalKeyResolver principalKeyResolver;

    private final ApplicationKey applicationKey;

    private final MainConfig config;

    public ContentNodeConverter( final ContentTypeResolver contentTypeResolver, final PrincipalKeyResolver principalKeyResolver,
                                 final NodeIdRegistry nodeIdRegistry, final ApplicationKey applicationKey, final MainConfig config,
                                 final ImageDescriptionResolver imageDescriptionResolver )
    {
        this.contentTypeResolver = contentTypeResolver;
        this.nodeIdRegistry = nodeIdRegistry;
        this.dataEntryValuesConverter = new DataEntryValuesConverter( this.nodeIdRegistry, imageDescriptionResolver );
        this.formValuesConverter = new FormValuesConverter();
        this.newsletterValuesConverter = new NewsletterValuesConverter( this.nodeIdRegistry );
        this.principalKeyResolver = principalKeyResolver;
        this.applicationKey = applicationKey;
        this.config = config;
    }

    public Node toNode( final ContentEntity content )
    {
        final Node node = createNode( nodeIdRegistry.getNodeId( content.getKey() ), content.getName(), toData( content ) );
        final AccessControlList permissions = getPermissions( content.getContentAccessRights() );

        final Form form = getContentTypeForm( content );
        final ContentTypeName contentType = convertType( content );
        final IndexConfigDocument indexConfig = ContentIndexConfigFactory.create( form, contentType );

        return Node.create( node ).permissions( permissions ).inheritPermissions( false ).indexConfigDocument( indexConfig ).build();
    }

    private AccessControlList getPermissions( final Collection<ContentAccessEntity> contentAccessRights )
    {
        final AccessControlList.Builder list = AccessControlList.create();
        for ( ContentAccessEntity car : contentAccessRights )
        {
            PrincipalKey principal = resolvePrincipal( car.getGroup() );
            if ( principal != null )
            {
                final Iterable<Permission> permissions = getAllowedPermissions( car );
                final AccessControlEntry entry = AccessControlEntry.create().principal( principal ).allow( permissions ).build();
                list.add( entry );
            }
            else
            {
                if ( !car.getGroup().isBuiltIn() )
                {
                    System.err.println( "Principal not found: " + car.getGroup().getQualifiedName() );
                }
            }
        }
        return list.build();
    }

    private PrincipalKey resolvePrincipal( final GroupEntity group )
    {
        if ( group.isAnonymous() )
        {
            return PrincipalKey.ofAnonymous();
        }
        if ( group.isAdministrator() )
        {
            return RoleKeys.ADMIN;
        }
        if ( group.isOfType( GroupType.AUTHENTICATED_USERS, false ) )
        {
            return RoleKeys.AUTHENTICATED;
        }
        return this.principalKeyResolver.getPrincipal( group.getGroupKey() );
    }

    private Iterable<Permission> getAllowedPermissions( final ContentAccessEntity car )
    {
        final List<Permission> permissions = new ArrayList<>();
        permissions.add( Permission.READ_PERMISSIONS );
        if ( car.isDeleteAccess() )
        {
            permissions.add( Permission.DELETE );
        }
        if ( car.isReadAccess() )
        {
            permissions.add( Permission.READ );
        }
        if ( car.isUpdateAccess() )
        {
            permissions.add( Permission.MODIFY );
            permissions.add( Permission.WRITE_PERMISSIONS );
            permissions.add( Permission.CREATE );
            permissions.add( Permission.PUBLISH );
        }
        return permissions;
    }

    private PropertyTree toData( final ContentEntity content )
    {
        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, getDisplayName( content ) );
        data.setString( ContentPropertyNames.TYPE, convertType( content ).toString() );
        data.setString( ContentPropertyNames.LANGUAGE, content.getLanguage().getCode() );
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, content.getTimestamp().toInstant() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        data.setInstant( ContentPropertyNames.CREATED_TIME, content.getCreatedAt().toInstant() );
        data.setSet( ContentPropertyNames.DATA, new PropertySet() );
        addPublishInfo( content, data );

        final PropertySet extraData = new PropertySet();
        if ( config.target.exportCmsKeyMixin )
        {
            toCmsContentExtraData( content, extraData );
        }
        data.setSet( ContentPropertyNames.EXTRA_DATA, extraData );

        final ContentVersionEntity mainVersion = content.getMainVersion();
        if ( mainVersion == null )
        {
            return data;
        }

        try
        {
            final ContentData contentData = getContentData( mainVersion );
            if ( contentData == null )
            {
                return data;
            }
            else if ( contentData instanceof DataEntry )
            {
                final DataEntry dataEntry = (DataEntry) contentData;
                final Value value = dataEntryValuesConverter.toValue( dataEntry );
                if ( value != null )
                {
                    data.setProperty( ContentPropertyNames.DATA, value );
                }
            }
            else if ( contentData instanceof LegacyFormContentData )
            {
                final LegacyFormContentData formData = (LegacyFormContentData) contentData;
                final Value value = formValuesConverter.toValue( formData );
                if ( value != null )
                {
                    data.setProperty( ContentPropertyNames.DATA, value );
                }
            }
            else if ( contentData instanceof LegacyImageContentData )
            {

            }
            else if ( contentData instanceof LegacyFileContentData )
            {

            }
            else if ( contentData instanceof LegacyNewsletterContentData )
            {
                final LegacyNewsletterContentData newsletterData = (LegacyNewsletterContentData) contentData;
                final Value value = newsletterValuesConverter.toValue( newsletterData );
                if ( value != null )
                {
                    data.setProperty( ContentPropertyNames.DATA, value );
                }
            }
            else
            {
                logger.info( "Unsupported ContentData: " + content.getClass().getSimpleName() );
            }
        }
        catch ( Exception e )
        {
            logger.warn( "Cannot get ContentData from '" + content.getPathAsString() + "'", e );
        }
        return data;
    }

    private void addPublishInfo( final ContentEntity content, final PropertyTree contentAsData )
    {
        if ( content.getAvailableFrom() == null && content.getAvailableTo() == null )
        {
            return;
        }

        final PropertySet publishInfo = contentAsData.addSet( PUBLISH_INFO );
        if ( content.getAvailableFrom() != null )
        {
            final Instant from = content.getAvailableFrom().toInstant();
            publishInfo.addInstant( PUBLISH_FIRST, from );
            publishInfo.addInstant( PUBLISH_FROM, from );
        }
        if ( content.getAvailableTo() != null )
        {
            final Instant to = content.getAvailableTo().toInstant();
            publishInfo.addInstant( PUBLISH_TO, to );
        }
    }

    private ContentData getContentData( ContentVersionEntity contentVersion )
    {
        final Document contentDataXml = contentVersion.getContentDataAsJDomDocument();
        if ( contentDataXml == null )
        {
            return null;
        }

        final ContentTypeEntity contentType = contentVersion.getContent().getCategory().getContentType();
        return ContentDataParser.parse( contentDataXml, contentType, null );
    }

    private void toCmsContentExtraData( final ContentEntity content, final PropertySet extraData )
    {
        final String appId = this.applicationKey.toString().replace( ".", "-" );

        final PropertySet cmsContent = new PropertySet();
        final String contentKey = content.getKey().toString();
        cmsContent.setString( "contentKey", contentKey );

        PropertySet appData = extraData.getSet( appId );
        if ( appData == null )
        {
            appData = new PropertySet();
            extraData.addSet( appId, appData );
        }

        for ( ContentHomeEntity home : content.getContentHomes() )
        {
            final NodeId homeMenuItemId = nodeIdRegistry.getNodeId( home.getMenuItem().getKey() );
            cmsContent.setReference( "contentHome", Reference.from( homeMenuItemId.toString() ) );
        }

        appData.setSet( "cmsContent", cmsContent );
    }

    private String getDisplayName( final ContentEntity content )
    {
        return content.getMainVersion().getTitle();
    }

    public ContentTypeName convertType( final ContentEntity content )
    {
        if ( content.getContentType() == null )
        {
            return ContentTypeName.unstructured();
        }
        final ContentTypeKey key = content.getContentType().getContentTypeKey();
        final ContentType contentType = this.contentTypeResolver.getContentType( key );
        if ( contentType != null )
        {
            return contentType.getName();
        }
        return SYSTEM_TYPES.getOrDefault( content.getContentType().getContentHandlerName(), ContentTypeName.unstructured() );
    }

    private Form getContentTypeForm( final ContentEntity content )
    {
        if ( content.getContentType() == null )
        {
            return EMPTY_FORM;
        }
        final ContentTypeKey key = content.getContentType().getContentTypeKey();
        final ContentType contentType = this.contentTypeResolver.getContentType( key );
        if ( contentType != null )
        {
            return contentType.getForm();
        }

        final ContentTypeName xpType =
            SYSTEM_TYPES.getOrDefault( content.getContentType().getContentHandlerName(), ContentTypeName.unstructured() );
        final ContentType ctype = BUILTIN_CONTENT_TYPES.getAll().stream().
            filter( ( ct ) -> ct.getName().equals( xpType ) ).
            findFirst().
            orElse( null );
        return ctype != null ? ctype.getForm() : EMPTY_FORM;
    }
}
