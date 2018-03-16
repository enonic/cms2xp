package com.enonic.cms2xp.converter;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.xp.data.PropertySet;
import com.enonic.xp.util.Reference;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.structure.TemplateParameter;
import com.enonic.cms.core.structure.TemplateParameterType;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;
import com.enonic.cms.core.structure.portlet.PortletEntity;

public final class PortletConfigConverter
{
    private final static Logger logger = LoggerFactory.getLogger( MenuItemNodeConverter.class );

    private final NodeIdRegistry nodeIdRegistry;

    public PortletConfigConverter( final NodeIdRegistry nodeIdRegistry )
    {
        this.nodeIdRegistry = nodeIdRegistry;
    }

    public PropertySet convertPortletParametersToPartConfig( final PortletEntity portlet )
    {
        final PropertySet params = new PropertySet();
        final Map<String, TemplateParameter> templateParams = portlet.getTemplateParameters();
        for ( final String paramName : templateParams.keySet() )
        {
            final TemplateParameter templateParam = templateParams.get( paramName );
            final String value = templateParam.getValue();
            if ( value == null || value.trim().isEmpty() )
            {
                continue;
            }
            try
            {
                final TemplateParameterType type = templateParam.getType() != null ? templateParam.getType() : TemplateParameterType.OBJECT;
                switch ( type )
                {
                    case CATEGORY:
                        final CategoryKey categoryKey = new CategoryKey( value );
                        params.setReference( paramName, new Reference( nodeIdRegistry.getNodeId( categoryKey ) ) );
                        break;

                    case CONTENT:
                        final ContentKey contentKey = new ContentKey( value );
                        params.setReference( paramName, new Reference( nodeIdRegistry.getNodeId( contentKey ) ) );
                        break;

                    case PAGE:
                        final MenuItemKey menu = new MenuItemKey( value );
                        params.setReference( paramName, new Reference( nodeIdRegistry.getNodeId( menu ) ) );
                        break;

                    case OBJECT:
                        params.setString( paramName, value );
                        break;
                }
            }
            catch ( Exception e )
            {
                logger.warn( "Could not convert parameter '" + paramName + "' in portlet '" + portlet.getName() + "': " + value );
                params.setString( paramName, value );
            }
        }
        return params;
    }
}
