package com.enonic.cms2xp.converter;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;

import com.enonic.xp.core.impl.content.ContentPathNameGenerator;
import com.enonic.xp.core.impl.security.PrincipalNodeTranslator;
import com.enonic.xp.core.impl.security.UserStoreNodeTranslator;
import com.enonic.xp.core.impl.security.UserStorePropertyNames;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.security.Group;
import com.enonic.xp.security.Principal;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.Role;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserStore;
import com.enonic.xp.security.UserStoreKey;
import com.enonic.xp.security.acl.AccessControlList;
import com.enonic.xp.security.acl.UserStoreAccessControlEntry;
import com.enonic.xp.security.acl.UserStoreAccessControlList;

import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.userstore.UserStoreEntity;

import static com.enonic.xp.security.acl.UserStoreAccess.ADMINISTRATOR;
import static com.enonic.xp.security.acl.UserStoreAccess.READ;

public final class UserStoreConverter
    extends AbstractNodeConverter
{

    public UserStoreConverter()
    {
    }

    public UserStore convert( final UserStoreEntity userStoreEntity )
    {
        final com.enonic.xp.security.UserStoreKey key = com.enonic.xp.security.UserStoreKey.from( userStoreEntity.getName().toLowerCase() );

        return UserStore.create().
            displayName( userStoreEntity.getName() ).
            key( key ).build();
    }

    public Group convert( final GroupEntity groupEntity )
    {
        final com.enonic.xp.security.UserStoreKey key;
        if ( groupEntity.getUserStore() != null )
        {
            key = com.enonic.xp.security.UserStoreKey.from( generateName( groupEntity.getUserStore().getName() ) );
        }
        else
        {
            key = UserStoreKey.system();
        }

        final String groupId = generateName( groupEntity.getName() );
        final PrincipalKey groupKey = PrincipalKey.ofGroup( key, groupId );

        String displayName = groupEntity.getDescription();
        displayName = StringUtils.isBlank( displayName ) ? groupEntity.getName() : displayName;
        return Group.create().
            displayName( displayName ).
            key( groupKey ).
            build();
    }

    public Role convertToRole( final GroupEntity groupEntity )
    {
        final String groupId = generateName( groupEntity.getName() );
        final PrincipalKey roleKey = PrincipalKey.ofRole( groupId );

        String displayName = groupEntity.getDescription();
        displayName = StringUtils.isBlank( displayName ) ? groupEntity.getName() : displayName;
        return Role.create().
            displayName( displayName ).
            key( roleKey ).
            build();
    }

    public User convert( final UserEntity userEntity )
    {
        final com.enonic.xp.security.UserStoreKey key =
            com.enonic.xp.security.UserStoreKey.from( generateName( userEntity.getUserStore().getName() ) );

        final String userId = generateName( userEntity.getName() );
        final PrincipalKey userKey = PrincipalKey.ofUser( key, userId );

        return User.create().
            login( userId ).
            email( userEntity.getEmail() ).
            displayName( userEntity.getDisplayName() ).
            key( userKey ).
            build();
    }

    public List<Node> convertToNode( final UserStore userStore )
    {
        final PropertyTree data = new PropertyTree();
        data.setString( UserStorePropertyNames.DISPLAY_NAME_KEY, userStore.getDisplayName() );

        final UserStoreAccessControlList permissions =
            UserStoreAccessControlList.of( UserStoreAccessControlEntry.create().principal( RoleKeys.ADMIN ).access( ADMINISTRATOR ).build(),
                                           UserStoreAccessControlEntry.create().principal( RoleKeys.AUTHENTICATED ).access(
                                               READ ).build() );

        AccessControlList userStoreNodePermissions = UserStoreNodeTranslator.userStorePermissionsToUserStoreNodePermissions( permissions );
        AccessControlList usersNodePermissions = UserStoreNodeTranslator.userStorePermissionsToUsersNodePermissions( permissions );
        AccessControlList groupsNodePermissions = UserStoreNodeTranslator.userStorePermissionsToGroupsNodePermissions( permissions );

        final Node userStoreNode = Node.create().
            id( new NodeId() ).
            parentPath( UserStoreNodeTranslator.getUserStoresParentPath() ).
            name( userStore.getKey().toString() ).
            data( data ).
            permissions( userStoreNodePermissions ).
            build();

        final Node usersNode = Node.create().
            parentPath( userStoreNode.path() ).
            name( UserStoreNodeTranslator.USER_FOLDER_NODE_NAME ).
            permissions( usersNodePermissions ).
            build();

        final Node groupsNode = Node.create().
            parentPath( userStoreNode.path() ).
            name( UserStoreNodeTranslator.GROUP_FOLDER_NODE_NAME ).
            permissions( groupsNodePermissions ).
            build();

        return Lists.newArrayList( userStoreNode, usersNode, groupsNode );
    }

    public Node rolesNode()
    {
        final NodePath rolesNodePath = UserStoreNodeTranslator.getRolesNodePath();
        return Node.create().
            parentPath( rolesNodePath.getParentPath() ).
            name( rolesNodePath.getLastElement().toString() ).
            inheritPermissions( true ).
            build();
    }

    public Node convertToNode( final Principal principal, final PrincipalKeys members )
    {
        if ( principal.getKey().isUser() )
        {
            return PrincipalNodeTranslator.toNode( principal );
        }
        else
        {
            return PrincipalNodeTranslator.toNode( principal, members );
        }
    }

    private String generateName( final String value )
    {
        return new ContentPathNameGenerator().generatePathName( value ).replace( "ø", "o" ).replace( "å", "aa" ).replace( "æ", "ae" );
    }
}
