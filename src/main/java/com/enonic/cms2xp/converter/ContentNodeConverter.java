package com.enonic.cms2xp.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.export.PrincipalKeyResolver;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.acl.AccessControlEntry;
import com.enonic.xp.security.acl.AccessControlList;
import com.enonic.xp.security.acl.Permission;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.access.ContentAccessEntity;
import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.custom.DataEntry;
import com.enonic.cms.core.content.contentdata.legacy.LegacyFileContentData;
import com.enonic.cms.core.content.contentdata.legacy.LegacyFormContentData;
import com.enonic.cms.core.content.contentdata.legacy.LegacyImageContentData;
import com.enonic.cms.core.content.contentdata.legacy.LegacyNewsletterContentData;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeKey;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupType;

public final class ContentNodeConverter
    extends AbstractNodeConverter
{
    private static final Map<ContentHandlerName, ContentTypeName> SYSTEM_TYPES =
        ImmutableMap.<ContentHandlerName, ContentTypeName>builder().
            put( ContentHandlerName.CUSTOM, ContentTypeName.unstructured() ).
            put( ContentHandlerName.IMAGE, ContentTypeName.imageMedia() ).
            put( ContentHandlerName.FILE, ContentTypeName.unknownMedia() ).
            build();

    private final static Logger logger = LoggerFactory.getLogger( ContentNodeConverter.class );

    private final NodeIdRegistry nodeIdRegistry;

    private final DataEntryValuesConverter dataEntryValuesConverter;

    private final FormValuesConverter formValuesConverter;

    private final NewsletterValuesConverter newsletterValuesConverter;

    private final ContentTypeResolver contentTypeResolver;

    private final PrincipalKeyResolver principalKeyResolver;

    private final ApplicationKey applicationKey;

    private final MainConfig config;

    public ContentNodeConverter( final ContentTypeResolver contentTypeResolver, final PrincipalKeyResolver principalKeyResolver,
                                 final NodeIdRegistry nodeIdRegistry, final ApplicationKey applicationKey, final MainConfig config )
    {
        this.contentTypeResolver = contentTypeResolver;
        this.nodeIdRegistry = nodeIdRegistry;
        this.dataEntryValuesConverter = new DataEntryValuesConverter( this.nodeIdRegistry );
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
        return Node.create( node ).permissions( permissions ).inheritPermissions( false ).build();
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
        final PropertySet extraData = new PropertySet();
        if ( config.target.exportPublishDateMixin )
        {
            toPublishExtraData( content, extraData );
        }
        data.setSet( ContentPropertyNames.EXTRA_DATA, extraData );

        final ContentVersionEntity mainVersion = content.getMainVersion();
        if ( mainVersion != null )
        {
            try
            {
                final ContentData contentData = mainVersion.getContentData();
                if ( contentData instanceof DataEntry )
                {
                    final DataEntry dataEntry = (DataEntry) contentData;
                    data.setProperty( ContentPropertyNames.DATA, dataEntryValuesConverter.toValue( dataEntry ) );
                }
                else if ( contentData instanceof LegacyFormContentData )
                {
                    final LegacyFormContentData formData = (LegacyFormContentData) contentData;
                    data.setProperty( ContentPropertyNames.DATA, formValuesConverter.toValue( formData ) );
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
                    data.setProperty( ContentPropertyNames.DATA, newsletterValuesConverter.toValue( newsletterData ) );
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
        }
        return data;
    }

    private void toPublishExtraData( final ContentEntity content, final PropertySet extraData )
    {
        final String appId = this.applicationKey.toString().replace( ".", "-" );
        final PropertySet publishData = new PropertySet();
        if ( content.getAvailableFrom() != null )
        {
            publishData.setInstant( "publishFrom", content.getAvailableFrom().toInstant() );
        }
        if ( content.getAvailableTo() != null )
        {
            publishData.setInstant( "publishTo", content.getAvailableTo().toInstant() );
        }
        final PropertySet appData = new PropertySet();
        appData.setSet( "publishDate", publishData );

        extraData.addSet( appId, appData );
    }

    private String getDisplayName( final ContentEntity content )
    {
        return content.getMainVersion().getTitle();
    }

    public ContentTypeName convertType( final ContentEntity content )
    {
        final ContentTypeKey key = content.getContentType().getContentTypeKey();
        final ContentType contentType = this.contentTypeResolver.getContentType( key );
        if ( contentType != null )
        {
            return contentType.getName();
        }
        return SYSTEM_TYPES.getOrDefault( content.getContentType().getContentHandlerName(), ContentTypeName.unstructured() );
    }
}
