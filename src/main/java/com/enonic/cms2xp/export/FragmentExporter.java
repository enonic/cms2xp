package com.enonic.cms2xp.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms2xp.converter.FragmentsNodeConverter;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.core.structure.portlet.PortletEntity;

public class FragmentExporter
    extends AbstractAppComponentExporter
{
    private final static Logger logger = LoggerFactory.getLogger( FragmentExporter.class );

    private final NodeExporter nodeExporter;

    private final FragmentsNodeConverter fragmentsNodeConverter;

    public FragmentExporter( final NodeExporter nodeExporter, final ApplicationKey appKey )
    {
        this.nodeExporter = nodeExporter;
        this.fragmentsNodeConverter = new FragmentsNodeConverter( appKey );
    }

    public void export( final Iterable<PortletEntity> portletEntities, final NodePath parentNode )
    {

        Node templateFolderNode = fragmentsNodeConverter.getFragmentsNode();
        templateFolderNode = Node.create( templateFolderNode ).
            parentPath( parentNode ).
            build();
        nodeExporter.exportNode( templateFolderNode );

        for ( PortletEntity portlet : portletEntities )
        {
            Node contentNode = fragmentsNodeConverter.toNode( portlet );
            contentNode = Node.create( contentNode ).
                parentPath( templateFolderNode.path() ).
                build();

            try
            {
                nodeExporter.exportNode( contentNode );
            }
            catch ( Exception e )
            {
                logger.warn( "Could not export node '" + contentNode.path() + "'", e );
            }
        }
    }
}
