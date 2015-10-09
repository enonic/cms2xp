package com.enonic.cms2xp.config;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "config")
public final class MainConfig
{
    public SourceConfig source;

    public TargetConfig target;
}
