package com.enonic.xp.core.impl.export.writer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.common.io.ByteSource;

import com.enonic.xp.export.ExportNodeException;

public class FileExportWriter
{
    public void createDirectory( final Path path )
    {
        doCreateDirectories( path );
    }

    private void doCreateDirectories( final Path path )
    {
        try
        {
            Files.createDirectories( path );
        }
        catch ( IOException e )
        {
            throw new ExportNodeException( "failed to create directory with path " + path.toString() + ": " + e.toString(), e );
        }
    }

    public void writeElement( final Path itemPath, final String export )
    {
        this.doCreateDirectories( itemPath.getParent() );

        try
        {
            Files.write( itemPath, export.getBytes( StandardCharsets.UTF_8 ) );
        }
        catch ( IOException e )
        {
            throw new ExportNodeException( "failed to create file with path " + itemPath.toString() + ": " + e.toString(), e );
        }
    }

    public void writeSource( final Path itemPath, final ByteSource source )
    {
        this.doCreateDirectories( itemPath.getParent() );

        try
        {
            Files.copy( source.openStream(), itemPath, StandardCopyOption.REPLACE_EXISTING );
        }
        catch ( IOException e )
        {
            throw new ExportNodeException( "failed to write source to path " + itemPath.toString() + ": " + e.toString(), e );
        }
    }
}
