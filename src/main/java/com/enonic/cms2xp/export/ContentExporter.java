package com.enonic.cms2xp.export;

import com.enonic.cms2xp.converter.ContentNodeConverter;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.core.content.ContentEntity;

public class ContentExporter
{
    public static void export( final NodeExporter nodeExporter, final Iterable<ContentEntity> contents, final NodePath parentNode )
    {
        for ( ContentEntity content : contents )
        {
            //Converts the category to a node
            Node categoryNode = ContentNodeConverter.toNode( content );
            categoryNode = Node.create( categoryNode ).
                parentPath( parentNode ).
                build();

            //Exports the node
            nodeExporter.exportNode( categoryNode );
        }
    }
}
