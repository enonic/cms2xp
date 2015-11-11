package com.enonic.xp.core.impl.security;

import java.util.ArrayList;
import java.util.List;

import com.enonic.xp.node.NodePath;
import com.enonic.xp.security.acl.AccessControlEntry;
import com.enonic.xp.security.acl.AccessControlList;
import com.enonic.xp.security.acl.UserStoreAccessControlEntry;
import com.enonic.xp.security.acl.UserStoreAccessControlList;

import static com.enonic.xp.security.acl.Permission.CREATE;
import static com.enonic.xp.security.acl.Permission.DELETE;
import static com.enonic.xp.security.acl.Permission.MODIFY;
import static com.enonic.xp.security.acl.Permission.PUBLISH;
import static com.enonic.xp.security.acl.Permission.READ;
import static com.enonic.xp.security.acl.Permission.READ_PERMISSIONS;
import static com.enonic.xp.security.acl.Permission.WRITE_PERMISSIONS;
import static com.enonic.xp.security.acl.UserStoreAccess.ADMINISTRATOR;

public abstract class UserStoreNodeTranslator
{
    public final static String USER_FOLDER_NODE_NAME = "users";

    public final static String GROUP_FOLDER_NODE_NAME = "groups";

    public static NodePath getUserStoresParentPath()
    {
        return NodePath.ROOT;
    }

    public static AccessControlList userStorePermissionsToUserStoreNodePermissions( final UserStoreAccessControlList userStorePermissions )
    {
        final List<AccessControlEntry> entries = new ArrayList<>();
        for ( UserStoreAccessControlEntry entry : userStorePermissions )
        {
            if ( entry.getAccess() == ADMINISTRATOR )
            {
                final AccessControlEntry ace = AccessControlEntry.create().principal( entry.getPrincipal() ).
                    allow( READ, CREATE, MODIFY, DELETE, PUBLISH, READ_PERMISSIONS, WRITE_PERMISSIONS ).build();
                entries.add( ace );
            }
        }
        return AccessControlList.create().addAll( entries ).build();
    }

    public static AccessControlList userStorePermissionsToUsersNodePermissions( final UserStoreAccessControlList userStorePermissions )
    {
        final List<AccessControlEntry> entries = new ArrayList<>();
        for ( UserStoreAccessControlEntry entry : userStorePermissions )
        {
            final AccessControlEntry ace;
            switch ( entry.getAccess() )
            {
                case CREATE_USERS:
                    ace = AccessControlEntry.create().principal( entry.getPrincipal() ).
                        allow( CREATE ).build();
                    entries.add( ace );
                    break;
                case WRITE_USERS:
                    ace = AccessControlEntry.create().principal( entry.getPrincipal() ).
                        allow( READ, CREATE, MODIFY, DELETE ).build();
                    entries.add( ace );
                    break;
                case USER_STORE_MANAGER:
                    ace = AccessControlEntry.create().principal( entry.getPrincipal() ).
                        allow( READ, CREATE, MODIFY, DELETE ).build();
                    entries.add( ace );
                    break;
                case ADMINISTRATOR:
                    ace = AccessControlEntry.create().principal( entry.getPrincipal() ).
                        allow( READ, CREATE, MODIFY, DELETE, PUBLISH, READ_PERMISSIONS, WRITE_PERMISSIONS ).build();
                    entries.add( ace );
                    break;
                case READ:
                    ace = AccessControlEntry.create().principal( entry.getPrincipal() ).
                        allow( READ ).build();
                    entries.add( ace );
                    break;
            }
        }
        return AccessControlList.create().addAll( entries ).build();
    }

    public static AccessControlList userStorePermissionsToGroupsNodePermissions( final UserStoreAccessControlList userStorePermissions )
    {
        final List<AccessControlEntry> entries = new ArrayList<>();
        for ( UserStoreAccessControlEntry entry : userStorePermissions )
        {
            final AccessControlEntry ace;
            switch ( entry.getAccess() )
            {
                case USER_STORE_MANAGER:
                    ace = AccessControlEntry.create().principal( entry.getPrincipal() ).
                        allow( READ, CREATE, MODIFY, DELETE ).build();
                    entries.add( ace );
                    break;
                case ADMINISTRATOR:
                    ace = AccessControlEntry.create().principal( entry.getPrincipal() ).
                        allow( READ, CREATE, MODIFY, DELETE, PUBLISH, READ_PERMISSIONS, WRITE_PERMISSIONS ).build();
                    entries.add( ace );
                    break;
            }
        }
        return AccessControlList.create().addAll( entries ).build();
    }

}
