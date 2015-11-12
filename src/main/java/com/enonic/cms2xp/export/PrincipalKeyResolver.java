package com.enonic.cms2xp.export;

import java.util.HashMap;
import java.util.Map;

import com.enonic.xp.security.PrincipalKey;

import com.enonic.cms.core.security.group.GroupKey;
import com.enonic.cms.core.security.user.UserKey;

public final class PrincipalKeyResolver
{
    private final Map<GroupKey, PrincipalKey> groupTable;

    private final Map<UserKey, PrincipalKey> usersTable;

    public PrincipalKeyResolver()
    {
        this.groupTable = new HashMap<>();
        this.usersTable = new HashMap<>();
    }

    public void add( final UserKey userKey, final PrincipalKey principal )
    {
        this.usersTable.put( userKey, principal );
    }

    public void add( final GroupKey groupKey, final PrincipalKey principal )
    {
        this.groupTable.put( groupKey, principal );
    }

    public PrincipalKey getPrincipal( final UserKey userKey )
    {
        return this.usersTable.get( userKey );
    }

    public PrincipalKey getPrincipal( final GroupKey groupKey )
    {
        return this.groupTable.get( groupKey );
    }

}
