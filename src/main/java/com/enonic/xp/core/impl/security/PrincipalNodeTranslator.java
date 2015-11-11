package com.enonic.xp.core.impl.security;

import java.time.Instant;

import com.google.common.base.Preconditions;

import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.index.ChildOrder;
import com.enonic.xp.node.CreateNodeParams;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeName;
import com.enonic.xp.node.NodeType;
import com.enonic.xp.security.Principal;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.PrincipalRelationship;
import com.enonic.xp.security.User;

public abstract class PrincipalNodeTranslator
{
    public static Node toNode( final Principal principal )
    {
        return createNode( toCreateNodeParams( principal ) );
    }

    public static Node toNode( final Principal principal, final PrincipalKeys members )
    {
        final Node node = createNode( toCreateNodeParams( principal ) );

        final PropertyTree nodeData = node.data();
        for ( PrincipalKey member : members )
        {
            final String relationshipToKey = member.toString();
            nodeData.addString( PrincipalPropertyNames.MEMBER_KEY, relationshipToKey );
        }

        return Node.create( node ).data( nodeData ).build();
    }

    private static CreateNodeParams toCreateNodeParams( final Principal principal )
    {
        Preconditions.checkNotNull( principal );

        final CreateNodeParams.Builder builder = CreateNodeParams.create().
            name( PrincipalKeyNodeTranslator.toNodeName( principal.getKey() ).toString() ).
            parent( principal.getKey().toPath().getParentPath() ).
            setNodeId( NodeId.from( principal.getKey() ) ).
            inheritPermissions( true ).
            indexConfigDocument( PrincipalIndexConfigFactory.create() );

        final PropertyTree data = new PropertyTree();
        data.setString( PrincipalPropertyNames.DISPLAY_NAME_KEY, principal.getDisplayName() );
        data.setString( PrincipalPropertyNames.PRINCIPAL_TYPE_KEY, principal.getKey().getType().toString() );
        if ( !principal.getKey().isRole() )
        {
            data.setString( PrincipalPropertyNames.USER_STORE_KEY, principal.getKey().getUserStore().toString() );
        }

        switch ( principal.getKey().getType() )
        {
            case USER:
                populateUserData( data.getRoot(), (User) principal );
                break;
        }

        builder.data( data );

        return builder.build();
    }

    private static void populateUserData( final PropertySet data, final User user )
    {
        data.setString( PrincipalPropertyNames.EMAIL_KEY, user.getEmail() );
        data.setString( PrincipalPropertyNames.LOGIN_KEY, user.getLogin() );
        data.setString( PrincipalPropertyNames.AUTHENTICATION_HASH_KEY, user.getAuthenticationHash() );
    }

    private static Node createNode( final CreateNodeParams params )
    {
        return Node.create().
            id( params.getNodeId() != null ? params.getNodeId() : new NodeId() ).
            parentPath( params.getParent() ).
            name( NodeName.from( params.getName() ) ).
            data( params.getData() ).
            indexConfigDocument( params.getIndexConfigDocument() ).
            childOrder( params.getChildOrder() != null ? params.getChildOrder() : ChildOrder.defaultOrder() ).
            inheritPermissions( params.inheritPermissions() ).
            nodeType( params.getNodeType() != null ? params.getNodeType() : NodeType.DEFAULT_NODE_COLLECTION ).
            timestamp( Instant.now() ).
            build();
        // manualOrderValue( manualOrderValue ).permissions( permissions ).
    }

    private static Node addRelationshipToUpdateNode( final PrincipalRelationship relationship, final Node node )
    {
        final PropertyTree nodeData = node.data();
        final String relationshipToKey = relationship.getTo().toString();
        nodeData.addString( PrincipalPropertyNames.MEMBER_KEY, relationshipToKey );

        return Node.create( node ).data( nodeData ).build();
    }
}
