package com.enonic.cms2xp.converter;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.structure.menuitem.MenuItemEntity;

public class MenuItemNodeConverter
    extends AbstractNodeConverter
{
    private final ApplicationKey applicationKey;

    public MenuItemNodeConverter( final ApplicationKey applicationKey )
    {
        this.applicationKey = applicationKey;
    }

    public Node convertToNode( final MenuItemEntity menuItemEntity )
    {
        return createNode( new NodeId(), menuItemEntity.getName(), toData( menuItemEntity ) );
    }

    private PropertyTree toData( final MenuItemEntity menuItem )
    {
        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, menuItem.getName() );
        data.setString( ContentPropertyNames.TYPE, ContentTypeName.folder().toString() );
        if ( menuItem.getLanguage() != null )
        {
            data.setString( ContentPropertyNames.LANGUAGE, menuItem.getLanguage().getCode() );
        }
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, menuItem.getTimestamp().toInstant() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        //TODO No created time info?
        data.setInstant( ContentPropertyNames.CREATED_TIME, menuItem.getTimestamp().toInstant() );

        final PropertySet siteConfig = new PropertySet();
        siteConfig.setProperty( "applicationKey", ValueFactory.newString( applicationKey.toString() ) );
        siteConfig.setProperty( "config", ValueFactory.newPropertySet( new PropertySet() ) );
        final PropertySet subData = new PropertySet();
        subData.setProperty( "description", ValueFactory.newString( menuItem.getName() ) ); //TODO No description?
        subData.setProperty( "siteConfig", ValueFactory.newPropertySet( siteConfig ) );
        data.setSet( ContentPropertyNames.DATA, subData );

        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        return data;
    }

}
