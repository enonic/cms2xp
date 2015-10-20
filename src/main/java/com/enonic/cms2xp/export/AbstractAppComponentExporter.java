package com.enonic.cms2xp.export;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.samskivert.mustache.Mustache;

public class AbstractAppComponentExporter
{
    protected void copy( String sourcePath, File target, Map<String, Object> mapping )
        throws IOException
    {
        final URL sourceUrl = getClass().getResource( sourcePath );
        String content = Resources.asCharSource( sourceUrl, Charsets.UTF_8 ).read();

        if ( mapping != null )
        {
            content = Mustache.compiler().compile( content ).execute( mapping );
        }

        Files.createParentDirs( target );
        Files.asCharSink( target, Charsets.UTF_8 ).write( content );
    }
}
