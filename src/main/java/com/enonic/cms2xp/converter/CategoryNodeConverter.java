package com.enonic.cms2xp.converter;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.content.category.CategoryEntity;

public final class CategoryNodeConverter
    extends AbstractNodeConverter
{
    private final MainConfig config;

    private final ApplicationKey applicationKey;

    private final NodeIdRegistry nodeIdRegistry;

    public CategoryNodeConverter( final ApplicationKey applicationKey, final MainConfig config, final NodeIdRegistry nodeIdRegistry )
    {
        this.applicationKey = applicationKey;
        this.config = config;
        this.nodeIdRegistry = nodeIdRegistry;
    }

    public Node toNode( final CategoryEntity category )
    {
        final NodeId categoryNodeId = nodeIdRegistry.getNodeId( category.getKey() );
        return createNode( categoryNodeId, category.getName(), toData( category ) );
    }

    private PropertyTree toData( final CategoryEntity category )
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

        final PropertySet extraData = new PropertySet();
        if ( config.target.exportCmsKeyMixin )
        {
            toCmsContentExtraData( category, extraData );
        }
        data.setSet( ContentPropertyNames.EXTRA_DATA, extraData );

        return data;
    }

    private void toCmsContentExtraData( final CategoryEntity category, final PropertySet extraData )
    {
        final String appId = this.applicationKey.toString().replace( ".", "-" );

        final PropertySet cmsContent = new PropertySet();
        final String contentKey = category.getKey().toString();
        cmsContent.setString( "categoryKey", contentKey );

        PropertySet appData = extraData.getSet( appId );
        if ( appData == null )
        {
            appData = new PropertySet();
            extraData.addSet( appId, appData );
        }

        appData.setSet( "cmsContent", cmsContent );
    }
}
