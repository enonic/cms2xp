package com.enonic.cms2xp.converter;

import java.time.Instant;

import com.enonic.cms2xp.export.PortletToPartResolver;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeName;
import com.enonic.xp.region.PartComponent;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.structure.portlet.PortletEntity;

public final class FragmentsNodeConverter
    extends AbstractNodeConverter
{

    private static final String PART_COMPONENT = PartComponent.class.getSimpleName();

    private final ApplicationKey applicationKey;

    private final PortletToPartResolver portletToPartResolver;

    public FragmentsNodeConverter( final ApplicationKey applicationKey, final PortletToPartResolver portletToPartResolver )
    {
        this.applicationKey = applicationKey;
        this.portletToPartResolver = portletToPartResolver;
    }

    public Node toNode( final PortletEntity portlet )
    {
        return createNode( new NodeId(), portlet.getName(), toData( portlet ) );
    }

    private PropertyTree toData( final PortletEntity portlet )
    {
        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, portlet.getName() );
        data.setString( ContentPropertyNames.TYPE, ContentTypeName.fragment().toString() );
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, portlet.getCreated().toInstant() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        data.setInstant( ContentPropertyNames.CREATED_TIME, portlet.getCreated().toInstant() );
        data.setSet( ContentPropertyNames.DATA, new PropertySet() );
        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        data.setSet( ContentPropertyNames.PAGE, fragmentsPage( portlet ) );
        return data;
    }

    private PropertySet fragmentsPage( final PortletEntity portlet )
    {
        final PropertySet page = new PropertySet();
        page.setSet( "config", new PropertySet() );

        final PropertySet fragmentData = new PropertySet();
        fragmentData.setString( "type", PART_COMPONENT );
        final PropertySet partComponentData = new PropertySet();
        partComponentData.setString( "name", portlet.getName() );
        final String partName = portletToPartResolver.partNameFromPortlet( portlet );
        partComponentData.setString( "template", applicationKey.toString() + ":" + partName );
        partComponentData.setSet( "config", new PropertySet() );
        fragmentData.setSet( PART_COMPONENT, partComponentData );

        page.addProperty( "fragment", ValueFactory.newPropertySet( fragmentData ) );
        return page;
    }

    public Node getFragmentsNode()
    {
        final Node node = createNode( new NodeId(), "_portlets", fragmentsData() );
        return Node.create( node ).name( NodeName.from( "_portlets" ) ).build();
    }

    private PropertyTree fragmentsData()
    {
        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, "Portlets" );
        data.setString( ContentPropertyNames.TYPE, ContentTypeName.folder().toString() );
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, Instant.now() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        data.setInstant( ContentPropertyNames.CREATED_TIME, Instant.now() );
        data.setSet( ContentPropertyNames.DATA, new PropertySet() );
        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        return data;
    }
}
