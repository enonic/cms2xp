package com.enonic.cms2xp.converter;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.structure.page.template.PageTemplateEntity;

public final class PageTemplateNodeConverter
    extends AbstractNodeConverter
{
    private final ApplicationKey applicationKey;

    public PageTemplateNodeConverter( final ApplicationKey applicationKey )
    {
        this.applicationKey = applicationKey;
    }

    public Node toNode( final PageTemplateEntity pageTemplateEntity )
    {
        return createNode( new NodeId(), pageTemplateEntity.getName(), toData( pageTemplateEntity ) );
    }

    private PropertyTree toData( final PageTemplateEntity pageTemplateEntity )
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
        data.setSet( ContentPropertyNames.DATA, subData );

        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        return data;
    }
}
