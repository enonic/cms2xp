package com.enonic.cms2xp.config;

import java.io.File;

public final class TargetConfig
{
    public File exportDir;

    public File userExportDir;

    public File applicationDir;

    public String applicationName;

    public String applicationRepo;

    @Deprecated
    public boolean exportPublishDateMixin;

    // experimental switches
    public boolean exportMenuMixin = true;

    public boolean moveHomeContentToSection = true;

    public boolean exportCmsKeyMixin;

    public boolean exportCmsMenuKeyMixin;
}
