package com.enonic.cms2xp.converter;

import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.content.category.CategoryEntity;

public final class CategoryNodeConverter
{
    private static final String SUPER_USER_KEY = "user:system:su";

    public static Node toNode( final CategoryEntity category )
    {
        return NodeFactory.createNode( category.getName(), toData( category ) );
    }

    private static PropertyTree toData( final CategoryEntity category )
    {
        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, category.getName() );
        data.setString( ContentPropertyNames.TYPE, ContentTypeName.folder().toString() );
        data.setString( ContentPropertyNames.LANGUAGE, category.getLanguage().getCode() );
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, category.getTimestamp().toInstant() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        data.setInstant( ContentPropertyNames.CREATED_TIME, category.getCreated().toInstant() );
        data.setSet( ContentPropertyNames.DATA, new PropertySet() );
        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        return data;
    }
}
