package com.enonic.cms2xp.converter;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.export.PageTemplateResolver;
import com.enonic.cms2xp.export.PortletToPartResolver;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.util.Reference;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemType;
import com.enonic.cms.core.structure.menuitem.section.SectionContentEntity;
import com.enonic.cms.core.structure.page.PageEntity;
import com.enonic.cms.core.structure.page.PageWindowEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateKey;
import com.enonic.cms.core.structure.page.template.PageTemplateRegionEntity;
import com.enonic.cms.core.structure.portlet.PortletEntity;

public class MenuItemNodeConverter
    extends AbstractNodeConverter
{
    private final static Logger logger = LoggerFactory.getLogger( MenuItemNodeConverter.class );

    private final ApplicationKey applicationKey;

    private final PageTemplateResolver pageTemplateResolver;

    private final PortletToPartResolver portletToPartResolver;

    private final NodeIdRegistry nodeIdRegistry;

    private final ContentTypeName sectionType;

    private final ContentTypeName pageType;

    private final ContentTypeName urlType;

    private final MainConfig config;

    public MenuItemNodeConverter( final ApplicationKey applicationKey, final PageTemplateResolver pageTemplateResolver,
                                  final PortletToPartResolver portletToPartResolver, final NodeIdRegistry nodeIdRegistry,
                                  final MainConfig config )
    {
        this.applicationKey = applicationKey;
        this.pageTemplateResolver = pageTemplateResolver;
        this.portletToPartResolver = portletToPartResolver;
        this.nodeIdRegistry = nodeIdRegistry;
        this.sectionType = ContentTypeName.from( this.applicationKey, "section" );
        this.pageType = ContentTypeName.from( this.applicationKey, "page" );
        this.urlType = ContentTypeName.from( this.applicationKey, "url" );
        this.config = config;
    }

    public Node convertToNode( final MenuItemEntity menuItemEntity )
    {
        return createNode( nodeIdRegistry.getNodeId( menuItemEntity.getKey() ), menuItemEntity.getName(), toData( menuItemEntity ), false );
    }

    private PropertyTree toData( final MenuItemEntity menuItem )
    {
        final boolean isShortcut = menuItem.getType() == MenuItemType.SHORTCUT;
        final boolean isUrl = menuItem.getType() == MenuItemType.URL;
        final boolean isDefaultPage = menuItem.getType() == MenuItemType.PAGE;
        final ContentTypeName type;
        if ( menuItem.isSection() )
        {
            type = sectionType;
        }
        else if ( isUrl )
        {
            type = urlType;
        }
        else
        {
            type = isShortcut ? ContentTypeName.shortcut() : pageType;
        }

        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, menuItem.getDisplayName() );
        data.setString( ContentPropertyNames.TYPE, type.toString() );
        if ( menuItem.getLanguage() != null )
        {
            data.setString( ContentPropertyNames.LANGUAGE, menuItem.getLanguage().getCode() );
        }
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, menuItem.getTimestamp().toInstant() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        //TODO No created time info?
        data.setInstant( ContentPropertyNames.CREATED_TIME, menuItem.getTimestamp().toInstant() );

        final PropertySet subData = new PropertySet();
        final boolean isSite = type.isSite(); // menuItem.isRootPage();
        final PageEntity page = menuItem.getPage();
        if ( isSite )
        {
            final PropertySet siteConfig = new PropertySet();
            siteConfig.setProperty( "applicationKey", ValueFactory.newString( applicationKey.toString() ) );
            siteConfig.setProperty( "config", ValueFactory.newPropertySet( new PropertySet() ) );

            subData.setProperty( "description", ValueFactory.newString( menuItem.getName() ) );
            subData.setProperty( "siteConfig", ValueFactory.newPropertySet( siteConfig ) );
        }
        else if ( isShortcut )
        {
            final MenuItemEntity shortcutMenu = menuItem.getMenuItemShortcut();
            if ( shortcutMenu != null )
            {
                final NodeId forwardNodeId = nodeIdRegistry.getNodeId( shortcutMenu.getKey() );
                subData.setProperty( "target", ValueFactory.newReference( Reference.from( forwardNodeId.toString() ) ) );
            }
        }
        else if ( isUrl )
        {
            final String url = menuItem.getUrl();
            if ( url != null )
            {
                subData.setString( "url", url );
            }
        }
        else if ( isDefaultPage )
        {
            final PageTemplateKey pageTemplate = page.getTemplate().getPageTemplateKey();
            final PropertyTree pageData = this.pageTemplateResolver.resolveTemplatePageData( pageTemplate );
            if ( pageData != null )
            {
                data.setSet( ContentPropertyNames.PAGE, pageData.copy().getRoot() );
            }
        }
        else if ( menuItem.isSection() )
        {
            final Set<SectionContentEntity> sectionContents = menuItem.getSectionContents();
            final Comparator<SectionContentEntity> byOrder = Comparator.comparingInt( SectionContentEntity::getOrder );
            final List<SectionContentEntity> sortedSections = sectionContents.stream().sorted( byOrder ).collect( Collectors.toList() );

            for ( SectionContentEntity sectionContent : sortedSections )
            {
                final NodeId sectionContentId = nodeIdRegistry.getNodeId( sectionContent.getContent().getKey() );
                if ( sectionContentId == null )
                {
//                    logger.warn( "Cannot resolve NodeId for content with key '" + sectionContent.getContent().getKey() +
//                                     "', published in section '" + sectionContent.getKey() + "'" );
                    continue;
                }
                subData.addProperty( "sectionContents", ValueFactory.newReference( Reference.from( sectionContentId.toString() ) ) );
            }
        }

        if ( !isDefaultPage && page != null )
        {
            final PropertySet pageData = new PropertySet();
            final PageTemplateKey pageTemplate = page.getTemplate().getPageTemplateKey();
            pageData.setProperty( "controller", ValueFactory.newString( null ) );
            final NodeId templateNodeId = this.pageTemplateResolver.resolveTemplateNodeId( pageTemplate );
            if ( templateNodeId == null )
            {
                pageData.setProperty( "template", ValueFactory.newReference( null ) );
            }
            else
            {
                pageData.setProperty( "template", ValueFactory.newReference( Reference.from( templateNodeId.toString() ) ) );
            }

            final Multimap<String, PortletEntity> regionPortlets = ArrayListMultimap.create();
            for ( PageWindowEntity pageWindow : menuItem.getPage().getPageWindows() )
            {
                final PageTemplateRegionEntity region = pageWindow.getPageTemplateRegion();
                final PortletEntity portlet = pageWindow.getPortlet();
                regionPortlets.put( region.getName(), portlet );
            }

            for ( String regionName : regionPortlets.keySet() )
            {
                final PropertySet regionsData = new PropertySet();
                regionsData.setString( "name", regionName );

                for ( final PortletEntity portlet : regionPortlets.get( regionName ) )
                {
                    final PropertySet componentData = new PropertySet();
                    componentData.setString( "type", "PartComponent" );
                    final PropertySet partComponentData = new PropertySet();
                    partComponentData.setString( "name", portlet.getName() );

                    final String partName = portletToPartResolver.partNameFromPortlet( portlet );
                    partComponentData.setString( "template", applicationKey.toString() + ":" + partName );
                    partComponentData.setSet( "config", new PropertySet() );
                    componentData.setSet( "PartComponent", partComponentData );
                    regionsData.addSet( "component", componentData );
                }
                pageData.addProperty( "region", ValueFactory.newPropertySet( regionsData ) );
            }

            data.setSet( ContentPropertyNames.PAGE, pageData );
        }
        addPageParameters( menuItem, subData );
        data.setSet( ContentPropertyNames.DATA, subData );

        final PropertySet extraData = new PropertySet();
        if ( config.target.exportMenuMixin )
        {
            toMenuItemExtraData( menuItem, extraData );
        }
        if ( config.target.exportCmsMenuKeyMixin )
        {
            toCmsMenuKeyExtraData( menuItem, extraData );
        }
        data.setSet( ContentPropertyNames.EXTRA_DATA, extraData );

        return data;
    }

    private void toCmsMenuKeyExtraData( final MenuItemEntity menuItem, final PropertySet extraData )
    {
        final String appId = this.applicationKey.toString().replace( ".", "-" );

        final PropertySet cmsContent = new PropertySet();
        final String contentKey = menuItem.getKey().toString();
        cmsContent.setString( "menuKey", contentKey );

        PropertySet appData = extraData.getSet( appId );
        if ( appData == null )
        {
            appData = new PropertySet();
            extraData.addSet( appId, appData );
        }

        if ( menuItem.getType() == MenuItemType.CONTENT && menuItem.getContent() != null )
        {
            final ContentKey contentParamKey = menuItem.getContent().getKey();
            final NodeId contentParamId = nodeIdRegistry.getNodeId( contentParamKey );
            cmsContent.setReference( "content", Reference.from( contentParamId.toString() ) );
        }

        appData.setSet( "cmsMenu", cmsContent );
    }

    private void addPageParameters( final MenuItemEntity menuItem, final PropertySet appData )
    {
        if ( menuItem.getRequestParameters().isEmpty() )
        {
            return;
        }

        menuItem.getRequestParameters().
            forEach( ( key, item ) -> {
                PropertySet paramData = new PropertySet();
                paramData.setString( "name", item.getName() );
                paramData.setString( "value", item.getValue() );
                paramData.setBoolean( "override", item.isOverridableByRequest() );
                appData.addSet( "parameters", paramData );
            } );
    }

    private void toMenuItemExtraData( final MenuItemEntity menuItem, final PropertySet extraData )
    {
        final String appId = this.applicationKey.toString().replace( ".", "-" );
        final PropertySet menuItemData = new PropertySet();
        if ( menuItem.getMenuName() != null )
        {
            menuItemData.setString( "menuName", menuItem.getMenuName() );
        }
        menuItemData.setBoolean( "menuItem", menuItem.showInMenu() );

        boolean newWindow = false;
        try
        {
            newWindow = menuItem.isOpenNewWindowForURL();
        }
        catch ( Exception ignored )
        {
            // DO NOTHING
        }
        if ( menuItem.getType() == MenuItemType.URL )
        {
            menuItemData.setBoolean( "newWindow", newWindow );
        }

        final PropertySet appData = new PropertySet();
        appData.setSet( "menu-item", menuItemData );

        extraData.addSet( appId, appData );
    }
}
