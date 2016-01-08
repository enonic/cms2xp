package com.enonic.cms2xp.converter;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import com.enonic.cms2xp.export.ContentKeyResolver;
import com.enonic.cms2xp.export.PageTemplateResolver;
import com.enonic.cms2xp.export.PortletToPartResolver;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.core.impl.content.ContentPathNameGenerator;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.util.Reference;

import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
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

    private final ContentKeyResolver contentKeyResolver;

    private final PageTemplateResolver pageTemplateResolver;

    private final PortletToPartResolver portletToPartResolver;

    private final NodeIdRegistry nodeIdRegistry;

    public MenuItemNodeConverter( final ApplicationKey applicationKey, final ContentKeyResolver contentKeyResolver,
                                  final PageTemplateResolver pageTemplateResolver, final PortletToPartResolver portletToPartResolver,
                                  final NodeIdRegistry nodeIdRegistry )
    {
        this.applicationKey = applicationKey;
        this.contentKeyResolver = contentKeyResolver;
        this.pageTemplateResolver = pageTemplateResolver;
        this.portletToPartResolver = portletToPartResolver;
        this.nodeIdRegistry = nodeIdRegistry;
    }

    public Node convertToNode( final MenuItemEntity menuItemEntity )
    {
        return createNode( nodeIdRegistry.getNodeId( menuItemEntity.getKey() ), menuItemEntity.getName(), toData( menuItemEntity ) );
    }

    private PropertyTree toData( final MenuItemEntity menuItem )
    {
        final ContentTypeName type = ContentTypeName.from( this.applicationKey, menuItem.isSection() ? "section" : "page" );

        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, menuItem.getName() );
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
        final boolean isSite = menuItem.isRootPage();
        if ( isSite )
        {
            final PropertySet siteConfig = new PropertySet();
            siteConfig.setProperty( "applicationKey", ValueFactory.newString( applicationKey.toString() ) );
            siteConfig.setProperty( "config", ValueFactory.newPropertySet( new PropertySet() ) );

            subData.setProperty( "description", ValueFactory.newString( menuItem.getName() ) ); //TODO No description?
            subData.setProperty( "siteConfig", ValueFactory.newPropertySet( siteConfig ) );
        }
        else if ( menuItem.isSection() )
        {
            final Set<SectionContentEntity> sectionContents = menuItem.getSectionContents();
            final Comparator<SectionContentEntity> byOrder = ( s1, s2 ) -> Integer.compare( s1.getOrder(), s2.getOrder() );
            final List<SectionContentEntity> sortedSections = sectionContents.stream().sorted( byOrder ).collect( Collectors.toList() );

            for ( SectionContentEntity sectionContent : sortedSections )
            {
                final NodeId sectionContentId = this.contentKeyResolver.resolve( sectionContent.getContent().getKey() );
                if ( sectionContentId == null )
                {
                    logger.warn( "Cannot resolve NodeId for content with key '" + sectionContent.getContent().getKey() +
                                     "', published in section '" + sectionContent.getKey() + "'" );
                    continue;
                }
                subData.addProperty( "sectionContents", ValueFactory.newReference( Reference.from( sectionContentId.toString() ) ) );
            }
        }

        final PageEntity page = menuItem.getPage();
        if ( page != null )
        {
            final PropertySet pageData = new PropertySet();
            final PageTemplateKey pageTemplate = page.getTemplate().getPageTemplateKey();
            pageData.setProperty( "controller", ValueFactory.newString( null ) );
            final NodeId templateNodeId = this.pageTemplateResolver.resolve( pageTemplate );
            pageData.setProperty( "template", ValueFactory.newReference( Reference.from( templateNodeId.toString() ) ) );

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
        data.setSet( ContentPropertyNames.DATA, subData );

        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        return data;
    }

    private String nameOf( final String value )
    {
        return new ContentPathNameGenerator().generatePathName( value );
    }
}
