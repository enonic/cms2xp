package com.enonic.xp.core.impl.export;

import java.nio.file.Path;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import com.enonic.xp.core.impl.export.writer.FileExportWriter;
import com.enonic.xp.core.impl.export.writer.NodeExportPathResolver;
import com.enonic.xp.core.impl.export.xml.XmlNodeSerializer;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.Nodes;
import com.enonic.xp.util.BinaryReference;

public class NodeExporter
{
    private final static String LINE_SEPARATOR = System.getProperty( "line.separator" );

    private final NodePath sourceNodePath;

    private final FileExportWriter exportWriter;

    private final Path rootDirectory;

    private final Path targetDirectory;

    private final boolean exportNodeIds;

    private NodeExporter( final Builder builder )
    {
        this.sourceNodePath = builder.sourceNodePath;
        this.rootDirectory = builder.rootDirectory;
        this.targetDirectory = builder.targetDirectory;
        this.exportNodeIds = builder.exportNodeIds;

        this.exportWriter = new FileExportWriter();
    }

    public static Builder create()
    {
        return new Builder();
    }

    private void exportNodes( final Nodes nodes )
    {
        for ( Node node : nodes )
        {
            writeNode( node );
        }
        // writeNodeOrderList( parentNode, allCurrentLevelChildren.build() );
    }

    public void exportNode( final Node node )
    {
        writeNode( node );
    }

    public void exportNodeBinary( final Node node, final BinaryReference reference, final ByteSource byteSource )
    {
        final Path nodeDataFolder = resolveNodeDataFolder( node );
        this.exportWriter.writeSource( NodeExportPathResolver.resolveBinaryPath( nodeDataFolder, reference ), byteSource );
    }

    public void exportNodeBinary( final Node node, final Path binaryPath )
    {
        exportNodeBinary( node, BinaryReference.from( binaryPath.getFileName().toString() ), Files.asByteSource( binaryPath.toFile() ) );
    }

    private void writeNode( final Node node )
    {
        final NodePath newParentPath = resolveNewParentPath( node );

        final Node relativeNode = Node.create( node ).parentPath( newParentPath ).build();

        final XmlNodeSerializer serializer = new XmlNodeSerializer();
        serializer.exportNodeIds( this.exportNodeIds );
        serializer.node( relativeNode );
        final String serializedNode = serializer.serialize();

        final Path nodeDataFolder = resolveNodeDataFolder( node );

        final Path nodeXmlPath = NodeExportPathResolver.resolveNodeXmlPath( nodeDataFolder );
        exportWriter.writeElement( nodeXmlPath, serializedNode );
    }

    private NodePath resolveNewParentPath( final Node node )
    {
        final NodePath newParentPath;

        if ( node.path().equals( this.sourceNodePath ) )
        {
            newParentPath = NodePath.ROOT;
        }
        else
        {
            newParentPath = node.parentPath().removeFromBeginning( this.sourceNodePath );
        }
        return newParentPath;
    }

    public void writeNodeOrderList( final Node parent, final Nodes children )
    {
        if ( parent == null || parent.getChildOrder() == null || !parent.getChildOrder().isManualOrder() )
        {
            return;
        }

        final StringBuilder builder = new StringBuilder();

        for ( final Node node : children )
        {
            builder.append( node.name().toString() ).append( LINE_SEPARATOR );
        }

        final Path nodeOrderListPath = NodeExportPathResolver.resolveOrderListPath( resolveNodeDataFolder( parent ) );

        exportWriter.writeElement( nodeOrderListPath, builder.toString() );
    }

    public void writeExportProperties( final String xpVersion )
    {
        if ( xpVersion != null )
        {
            final Path exportPropertiesPath = NodeExportPathResolver.resolveExportPropertiesPath( this.rootDirectory );

            exportWriter.writeElement( exportPropertiesPath, "xp.version = " + xpVersion );
        }
    }

    private Path resolveNodeDataFolder( final Node node )
    {
        final Path nodeBasePath = NodeExportPathResolver.resolveNodeBasePath( this.targetDirectory, node.path(), sourceNodePath );
        return NodeExportPathResolver.resolveNodeDataFolder( nodeBasePath );
    }

    public static final class Builder
    {
        private NodePath sourceNodePath;

        private Path rootDirectory;

        private Path targetDirectory;

        private boolean exportNodeIds = true;

        private Builder()
        {
        }

        public Builder sourceNodePath( NodePath exportRootNode )
        {
            this.sourceNodePath = exportRootNode;
            return this;
        }

        public Builder rootDirectory( Path rootDirectory )
        {
            this.rootDirectory = rootDirectory;
            return this;
        }

        public Builder targetDirectory( Path targetDirectory )
        {
            this.targetDirectory = targetDirectory;
            return this;
        }

        public Builder exportNodeIds( final boolean exportNodeIds )
        {
            this.exportNodeIds = exportNodeIds;
            return this;
        }

        public NodeExporter build()
        {
            return new NodeExporter( this );
        }
    }
}
