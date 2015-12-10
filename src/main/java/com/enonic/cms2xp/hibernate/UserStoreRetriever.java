package com.enonic.cms2xp.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupType;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.userstore.UserStoreEntity;
import com.enonic.cms.core.security.userstore.UserStoreKey;

public class UserStoreRetriever
{
    private final static Logger logger = LoggerFactory.getLogger( UserStoreRetriever.class );

    public List<UserStoreEntity> retrieveUserStores( final Session session )
    {

        session.beginTransaction();
        List<UserStoreEntity> userStores = session.getNamedQuery( "UserStoreEntity.findAll" ).list();
        for ( UserStoreEntity userStore : userStores )
        {
            logger.info( "Retrieved user store: " + userStore.getName() );
        }
        session.getTransaction().commit();

        return userStores;
    }

    public List<UserEntity> retrieveUsers( final Session session, final UserStoreKey userStoreKey )
    {
        final List<UserEntity> users;
        session.beginTransaction();
        try
        {
            users = session.getNamedQuery( "UserEntity.findByUserStoreKey" ).
                setParameter( "userStoreKey", userStoreKey.toInt() ).
                setParameter( "deleted", 0 ).
                list();
        }
        finally
        {
            session.getTransaction().commit();
        }

        return users;
    }

    public List<GroupEntity> retrieveGroups( final Session session, final UserStoreKey userStoreKey )
    {
        final List<GroupEntity> groups;
        session.beginTransaction();
        try
        {
            groups = session.getNamedQuery( "GroupEntity.findByUserStore" ).
                setParameter( "userStoreKey", userStoreKey.toInt() ).
                setParameter( "deleted", 0 ).
                list();
        }
        finally
        {
            session.getTransaction().commit();
        }

        return groups;
    }

    public List<GroupEntity> retrieveGlobalGroups( final Session session)
    {
        final List<GroupEntity> groups;
        session.beginTransaction();
        try
        {
            groups = session.getNamedQuery( "GroupEntity.findByGroupType" ).
                setParameter( "groupType", GroupType.GLOBAL_GROUP.toInteger() ).
                list();
        }
        finally
        {
            session.getTransaction().commit();
        }

        return groups;
    }
}
