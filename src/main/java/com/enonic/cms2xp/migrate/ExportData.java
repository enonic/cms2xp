package com.enonic.cms2xp.migrate;

import java.nio.file.Path;
import java.util.List;

import org.hibernate.Session;

import com.enonic.cms2xp.config.MainConfig;
import com.enonic.cms2xp.converter.CategoryNodeConverter;
import com.enonic.cms2xp.hibernate.CategoryExporter;
import com.enonic.cms2xp.hibernate.HibernateSessionProvider;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.core.content.category.CategoryEntity;

public final class ExportData
{
    private final MainConfig config;

    public ExportData( final MainConfig config )
    {
        this.config = config;
    }

    public void execute()
        throws Exception
    {
        final Session session = new HibernateSessionProvider( config ).getSession();

        System.out.println( "DB connected: " + session.isConnected() );
        final List<CategoryEntity> categories = CategoryExporter.retrieveRootCategories( session );
        System.out.println( "Exporting " + categories.size() + " categories..." );

        exportCategories( categories );

        session.close();
    }

    private void exportCategories( final List<CategoryEntity> categories )
    {
        final Path targetDirectory = this.config.target.exportDir.toPath();
        final String repoName = "cms-repo";
        final String branch = "draft";
        final Path rootDirectory = targetDirectory.resolve( repoName ).resolve( branch );

        final NodeExporter nodeExporter = NodeExporter.create().
            sourceNodePath( NodePath.ROOT ).
            rootDirectory( rootDirectory ).
            targetDirectory( targetDirectory ).
            exportNodeIds( true ).
            build();

        final CategoryNodeConverter categoryConverter = new CategoryNodeConverter();
        for ( CategoryEntity category : categories )
        {
            Node categoryNode = categoryConverter.toNode( category );
            categoryNode = Node.create( categoryNode ).parentPath( ContentConstants.CONTENT_ROOT_PATH ).build();

            nodeExporter.exportNode( categoryNode );
        }
    }

}
