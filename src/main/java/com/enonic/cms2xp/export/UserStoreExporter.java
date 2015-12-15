package com.enonic.cms2xp.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import com.enonic.cms2xp.converter.UserStoreConverter;
import com.enonic.cms2xp.hibernate.UserStoreRetriever;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.node.Node;
import com.enonic.xp.security.Group;
import com.enonic.xp.security.Principal;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.Principals;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserStore;

import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupKey;
import com.enonic.cms.core.security.group.GroupType;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.security.userstore.UserStoreEntity;
import com.enonic.cms.core.security.userstore.UserStoreKey;

public class UserStoreExporter
{

    public static final com.enonic.xp.security.UserStoreKey GLOBAL_USER_STORE = com.enonic.xp.security.UserStoreKey.from( "global" );

    private final NodeExporter nodeExporter;

    private final UserStoreConverter converter;

    private final Map<UserStoreKey, UserStore> userStores;

    private final Map<UserStore, Principals> userStoreMembers;

    private final Multimap<PrincipalKey, PrincipalKey> members;

    private final Session session;

    private final PrincipalKeyResolver principalKeyResolver;

    public UserStoreExporter( final Session session, final NodeExporter nodeExporter, final PrincipalKeyResolver principalKeyResolver )
    {
        this.session = session;
        this.nodeExporter = nodeExporter;
        this.converter = new UserStoreConverter();
        this.userStores = new HashMap<>();
        this.userStoreMembers = new HashMap<>();
        this.members = ArrayListMultimap.create();
        this.principalKeyResolver = principalKeyResolver;
    }

    public void export()
    {
        final UserStoreRetriever usr = new UserStoreRetriever();
        final List<UserStoreEntity> userStoreEntities = usr.retrieveUserStores( session );

        final Multimap<GroupKey, UserKey> groupUserMembers = ArrayListMultimap.create();
        final Multimap<GroupKey, GroupKey> groupMembers = ArrayListMultimap.create();

        exportGlobalGroups( groupUserMembers, groupMembers );

        for ( UserStoreEntity us : userStoreEntities )
        {
            final UserStore userStore = converter.convert( us );
            final UserStoreKey key = us.getKey();
            this.userStores.put( key, userStore );

            final List<Principal> principals = new ArrayList<>();

            // users
            final List<UserEntity> userEntities = usr.retrieveUsers( session, key );
            for ( UserEntity userEntity : userEntities )
            {
                final User user = converter.convert( userEntity );
                principalKeyResolver.add( userEntity.getKey(), user.getKey() );
                principalKeyResolver.add( userEntity.getUserGroupKey(), user.getKey() );
                principals.add( user );
            }

            // groups
            final List<GroupEntity> groupEntities = usr.retrieveGroups( session, key );

            for ( GroupEntity groupEntity : groupEntities )
            {
                if ( groupEntity.getType() == GroupType.AUTHENTICATED_USERS )
                {
                    continue;
                }

                // group members
                final Set<GroupEntity> members = groupEntity.getMembers( false );
                for ( GroupEntity member : members )
                {
                    if ( member.getType() == GroupType.USER )
                    {
                        groupUserMembers.put( groupEntity.getGroupKey(), member.getUser().getKey() );
                    }
                    else
                    {
                        groupMembers.put( groupEntity.getGroupKey(), member.getGroupKey() );
                    }
                }

                if ( groupEntity.getType() != GroupType.USER )
                {
                    final Group group = converter.convert( groupEntity );
                    principals.add( group );
                    principalKeyResolver.add( groupEntity.getGroupKey(), group.getKey() );
                }
            }
            addMemberships( groupUserMembers, groupMembers );

            final Principals members = Principals.from( principals );
            userStoreMembers.put( userStore, members );
        }

        exportNodes();
    }

    private void exportGlobalGroups( final Multimap<GroupKey, UserKey> groupUserMembers, final Multimap<GroupKey, GroupKey> groupMembers )
    {
        final UserStore userStore = UserStore.create().
            key( GLOBAL_USER_STORE ).
            displayName( "Global groups User Store" ).build();
        final UserStoreKey key = new UserStoreKey( Integer.MAX_VALUE );
        this.userStores.put( key, userStore );

        final UserStoreRetriever usr = new UserStoreRetriever();
        final List<Principal> principals = new ArrayList<>();

        // groups
        final List<GroupEntity> groupEntities = usr.retrieveGlobalGroups( session );

        for ( GroupEntity groupEntity : groupEntities )
        {
            // group members
            final Set<GroupEntity> members = groupEntity.getMembers( false );
            for ( GroupEntity member : members )
            {
                groupMembers.put( groupEntity.getGroupKey(), member.getGroupKey() );
            }
            final Group group = converter.convert( groupEntity );
            principals.add( group );
            principalKeyResolver.add( groupEntity.getGroupKey(), group.getKey() );
        }

        // user memberships
        addMemberships( groupUserMembers, groupMembers );

        final Principals members = Principals.from( principals );
        userStoreMembers.put( userStore, members );
    }

    private void addMemberships( final Multimap<GroupKey, UserKey> groupUserMembers, final Multimap<GroupKey, GroupKey> groupMembers )
    {
        // user memberships
        for ( GroupKey groupKey : groupUserMembers.keys() )
        {
            final Collection<UserKey> memberKeys = groupUserMembers.get( groupKey );
            for ( UserKey memberKey : memberKeys )
            {
                final PrincipalKey member = principalKeyResolver.getPrincipal( memberKey );
                final PrincipalKey group = principalKeyResolver.getPrincipal( groupKey );
                if ( group != null && member != null )
                {
                    this.members.put( group, member );
                }
            }
        }
        // group memberships
        for ( GroupKey groupKey : groupMembers.keys() )
        {
            final Collection<GroupKey> memberKeys = groupMembers.get( groupKey );
            for ( GroupKey memberKey : memberKeys )
            {
                final PrincipalKey member = principalKeyResolver.getPrincipal( memberKey );
                final PrincipalKey group = principalKeyResolver.getPrincipal( groupKey );
                if ( group != null && member != null )
                {
                    this.members.put( group, member );
                }
            }
        }
    }

    private void exportNodes()
    {
        for ( UserStore userStore : userStores.values() )
        {
            final List<Node> userStoreNodes = converter.convertToNode( userStore );
            for ( Node userStoreNode : userStoreNodes )
            {
                nodeExporter.exportNode( userStoreNode );
            }

            for ( Principal principal : userStoreMembers.get( userStore ) )
            {
                final Collection<PrincipalKey> principalMembers = this.members.get( principal.getKey() );
                final PrincipalKeys memberKeys = principalMembers == null ? PrincipalKeys.empty() : PrincipalKeys.from( principalMembers );
                nodeExporter.exportNode( converter.convertToNode( principal, memberKeys ) );
            }
        }
    }

}