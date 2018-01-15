package com.enonic.cms2xp.converter;

import java.util.Set;

import com.enonic.cms2xp.export.PageTemplateResolver;
import com.enonic.cms2xp.export.PortletToPartResolver;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.region.FragmentComponent;
import com.enonic.xp.region.PartComponent;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.util.Reference;

import com.enonic.cms.core.structure.page.template.PageTemplateEntity;
import com.enonic.cms.core.structure.page.template.PageTemplatePortletEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateRegionEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateType;
import com.enonic.cms.core.structure.portlet.PortletEntity;
import com.enonic.cms.core.structure.portlet.PortletKey;

import static com.enonic.cms2xp.migrate.ExportData.PAGE_TYPE;
import static com.enonic.cms2xp.migrate.ExportData.SECTION_TYPE;

public final class PageTemplateNodeConverter
    extends AbstractNodeConverter
{
    private static final String FRAGMENT_COMPONENT = FragmentComponent.class.getSimpleName();

    private static final String PART_COMPONENT = PartComponent.class.getSimpleName();

    private final ApplicationKey applicationKey;

    private final PortletToPartResolver portletToPartResolver;

    private final PageTemplateResolver pageTemplateResolver;

    public PageTemplateNodeConverter( final ApplicationKey applicationKey, final PortletToPartResolver portletToPartResolver,
                                      final PageTemplateResolver pageTemplateResolver )
    {
        this.applicationKey = applicationKey;
        this.portletToPartResolver = portletToPartResolver;
        this.pageTemplateResolver = pageTemplateResolver;
    }

    public Node toNode( final PageTemplateEntity pageTemplateEntity, final String pageName, final Set<PortletKey> singleUsePortlets )
    {
        if ( pageTemplateEntity.getType() == PageTemplateType.PAGE )
        {
            final PropertySet pageData = getPageData( pageTemplateEntity, pageName, singleUsePortlets );
            this.pageTemplateResolver.add( pageTemplateEntity.getPageTemplateKey(), pageData.toTree().copy() );
            return null;
        }
        return createNode( new NodeId(), pageTemplateEntity.getName(), toData( pageTemplateEntity, pageName, singleUsePortlets ) );
    }

    private PropertyTree toData( final PageTemplateEntity pageTemplateEntity, final String pageName,
                                 final Set<PortletKey> singleUsePortlets )
    {
        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, pageTemplateEntity.getName() );
        data.setString( ContentPropertyNames.TYPE, ContentTypeName.pageTemplate().toString() );
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, pageTemplateEntity.getTimestamp().toInstant() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        data.setInstant( ContentPropertyNames.CREATED_TIME, pageTemplateEntity.getTimestamp().toInstant() );

        final PropertySet subData = new PropertySet();
        subData.setProperty( "supports", ValueFactory.newString( applicationKey.toString() + ":" + PAGE_TYPE ) );
        if ( pageTemplateEntity.getType() == PageTemplateType.SECTIONPAGE )
        {
            subData.addProperty( "supports", ValueFactory.newString( applicationKey.toString() + ":" + SECTION_TYPE ) );
        }

        data.setSet( ContentPropertyNames.DATA, subData );

        final PropertySet pageData = getPageData( pageTemplateEntity, pageName, singleUsePortlets );
        data.setSet( ContentPropertyNames.PAGE, pageData );

        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        return data;
    }

    private PropertySet getPageData( final PageTemplateEntity pageTemplateEntity, final String pageName,
                                     final Set<PortletKey> singleUsePortlets )
    {
        final PropertySet pageData = new PropertySet();
        pageData.setProperty( "controller", ValueFactory.newString( applicationKey.toString() + ":" + pageName ) );
        pageData.setProperty( "template", ValueFactory.newReference( null ) );
        pageData.setProperty( "config", ValueFactory.newPropertySet( new PropertySet() ) );

        for ( PageTemplateRegionEntity region : pageTemplateEntity.getPageTemplateRegions() )
        {
            final PropertySet regionsData = new PropertySet();
            regionsData.setString( "name", region.getName() );

            final Set<PageTemplatePortletEntity> regionPortlets = region.getPortlets();
            for ( PageTemplatePortletEntity portletTemplate : regionPortlets )
            {
                final PortletEntity portlet = portletTemplate.getPortlet();
                final boolean makeFragment = !singleUsePortlets.contains( portlet.getPortletKey() );

                final PropertySet componentData = new PropertySet();
                if ( makeFragment )
                {
                    componentData.setString( "type", FRAGMENT_COMPONENT );
                    final PropertySet fragmentData = new PropertySet();
                    fragmentData.setString( "name", portlet.getName() );
                    fragmentData.setSet( "config", new PropertySet() );
                    final NodeId fragmentNodeId = portletToPartResolver.fragmentReferenceFromPortlet( portlet.getPortletKey() );
                    fragmentData.setReference( "fragment", new Reference( fragmentNodeId ) );
                    componentData.setSet( FRAGMENT_COMPONENT, fragmentData );
                }
                else
                {
                    componentData.setString( "type", PART_COMPONENT );
                    final PropertySet partComponentData = new PropertySet();
                    partComponentData.setString( "name", portlet.getName() );
                    partComponentData.setSet( "config", new PropertySet() );
                    final String partName = portletToPartResolver.partNameFromPortlet( portlet );
                    partComponentData.setString( "template", applicationKey.toString() + ":" + partName );
                    componentData.setSet( PART_COMPONENT, partComponentData );
                }
                regionsData.addSet( "component", componentData );
            }
            pageData.addProperty( "region", ValueFactory.newPropertySet( regionsData ) );
        }
        return pageData;
    }
}
