package com.enonic.cms2xp.config;

import java.io.File;

public final class SourceConfig
{
    public String jdbcDriver;

    public String jdbcUrl;

    public String jdbcUser;

    public String jdbcPassword;

    public File blobStoreDir;

    public File resourcesDir;

    public boolean ignoreDrafts = false;

    public ExcludeConfig exclude;

    public IncludeConfig include;

}
