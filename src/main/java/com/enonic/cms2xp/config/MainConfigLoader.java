package com.enonic.cms2xp.config;

import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

public final class MainConfigLoader
{
    private final URL url;

    public MainConfigLoader( final URL url )
    {
        this.url = url;
    }

    public MainConfig load()
        throws Exception
    {
        final JAXBContext context = JAXBContext.newInstance( MainConfig.class );
        final Unmarshaller unmarshaller = context.createUnmarshaller();

        return MainConfig.class.cast( unmarshaller.unmarshal( this.url ) );
    }
}
