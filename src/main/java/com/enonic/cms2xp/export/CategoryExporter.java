package com.enonic.cms2xp.export;

import java.util.List;
import java.util.Set;

import com.enonic.cms2xp.converter.CategoryNodeConverter;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.category.CategoryEntity;

public class CategoryExporter
{
    private final NodeExporter nodeExporter;

    private final ContentExporter contentExporter;

    private final CategoryNodeConverter categoryNodeConverter = new CategoryNodeConverter();

    public CategoryExporter( final NodeExporter nodeExporter, final ContentExporter contentExporter )
    {
        this.nodeExporter = nodeExporter;
        this.contentExporter = contentExporter;
    }

    public void export( final List<CategoryEntity> categories, final NodePath parentNode )
    {
        for ( CategoryEntity category : categories )
        {
            //Converts the category to a node
            Node categoryNode = categoryNodeConverter.toNode( category );
            categoryNode = Node.create( categoryNode ).
                parentPath( parentNode ).
                build();

            //Exports the node
            nodeExporter.exportNode( categoryNode );

            //Calls the export on the contents
            final Set<ContentEntity> contents = category.getContents();
            if ( !contents.isEmpty() )
            {
                contentExporter.export( contents, categoryNode.path() );
            }

            //Calls the export on the children
            final List<CategoryEntity> subCategories = category.getChildren();
            if ( !subCategories.isEmpty() )
            {
                export( subCategories, categoryNode.path() );
            }
        }
    }


}
