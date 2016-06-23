package com.enonic.cms2xp.config;

import java.io.File;

public final class TargetConfig
{
    public File exportDir;

    public File userExportDir;

    public File applicationDir;

    public String applicationName;

    public String applicationRepo;

    // experimental switches
    public boolean exportPublishDateMixin;

    public boolean exportMenuMixin = true;

    public boolean moveHomeContentToSection = true;

    public boolean exportCmsKeyMixin;
}
