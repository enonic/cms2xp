package com.enonic.cms2xp.export;

import com.enonic.xp.node.NodeId;

import com.enonic.cms.core.structure.portlet.PortletEntity;
import com.enonic.cms.core.structure.portlet.PortletKey;

public interface PortletToPartResolver
{
    String partNameFromPortlet( PortletEntity portlet );

    NodeId fragmentReferenceFromPortlet( PortletKey portletKey );
}
