package com.enonic.cms2xp.export;

import com.enonic.cms.core.structure.portlet.PortletEntity;

public interface PortletToPartResolver
{
    String partNameFromPortlet( PortletEntity portlet );
}
