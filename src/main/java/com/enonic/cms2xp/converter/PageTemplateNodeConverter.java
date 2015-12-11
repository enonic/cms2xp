package com.enonic.cms2xp.converter;

import java.util.Set;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.core.impl.content.ContentPathNameGenerator;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.structure.page.template.PageTemplateEntity;
import com.enonic.cms.core.structure.page.template.PageTemplatePortletEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateRegionEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateType;
import com.enonic.cms.core.structure.portlet.PortletEntity;

public final class PageTemplateNodeConverter
    extends AbstractNodeConverter
{
    private final ApplicationKey applicationKey;

    public PageTemplateNodeConverter( final ApplicationKey applicationKey )
    {
        this.applicationKey = applicationKey;
    }

    public Node toNode( final PageTemplateEntity pageTemplateEntity, final String pageName )
    {
        return createNode( new NodeId(), pageTemplateEntity.getName(), toData( pageTemplateEntity, pageName ) );
    }

    private PropertyTree toData( final PageTemplateEntity pageTemplateEntity, final String pageName )
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
        subData.setProperty( "supports", ValueFactory.newString( applicationKey.toString() + ":page" ) );
        if ( pageTemplateEntity.getType() == PageTemplateType.SECTIONPAGE )
        {
            subData.addProperty( "supports", ValueFactory.newString( applicationKey.toString() + ":section" ) );
        }

        data.setSet( ContentPropertyNames.DATA, subData );

        final PropertySet pageData = new PropertySet();
        pageData.setProperty( "controller", ValueFactory.newString( applicationKey.toString() + ":" + pageName ) );
        pageData.setProperty( "template", ValueFactory.newReference( null ) );
        for ( PageTemplateRegionEntity region : pageTemplateEntity.getPageTemplateRegions() )
        {
            final PropertySet regionsData = new PropertySet();
            regionsData.setString( "name", region.getName() );

            final Set<PageTemplatePortletEntity> regionPortlets = region.getPortlets();
            for ( PageTemplatePortletEntity portletTemplate : regionPortlets )
            {
                final PortletEntity portlet = portletTemplate.getPortlet();
                final PropertySet componentData = new PropertySet();
                componentData.setString( "type", "PartComponent" );
                final PropertySet partComponentData = new PropertySet();
                partComponentData.setString( "name", portlet.getName() );
                partComponentData.setString( "template", applicationKey.toString() + ":" + nameOf( portlet.getName() ) );
                partComponentData.setSet( "config", new PropertySet() );
                componentData.setSet( "PartComponent", partComponentData );
                regionsData.addSet( "component", componentData );
            }
            pageData.addProperty( "region", ValueFactory.newPropertySet( regionsData ) );
        }
        pageData.setSet( "config", new PropertySet() );
        data.setSet( ContentPropertyNames.PAGE, pageData );

        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        return data;
    }

    private String nameOf( final String value )
    {
        return new ContentPathNameGenerator().generatePathName( value );
    }
}
