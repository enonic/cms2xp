package com.enonic.cms2xp.export;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import com.enonic.cms2xp.converter.ContentNodeConverter;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.core.impl.content.ContentTypeFromMimeTypeResolver;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.index.ChildOrder;
import com.enonic.xp.index.IndexPath;
import com.enonic.xp.name.NamePrettyfier;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.query.expr.FieldOrderExpr;
import com.enonic.xp.query.expr.OrderExpr;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.util.BinaryReference;
import com.enonic.xp.util.MediaTypes;

import com.enonic.cms.framework.blob.BlobKey;
import com.enonic.cms.framework.blob.BlobRecord;
import com.enonic.cms.framework.blob.file.FileBlobStore;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.binary.BinaryDataEntity;
import com.enonic.cms.core.content.binary.BinaryDataKey;
import com.enonic.cms.core.content.contentdata.legacy.LegacyFileContentData;
import com.enonic.cms.core.content.contentdata.legacy.LegacyImageContentData;

import static com.enonic.xp.content.ContentPropertyNames.MODIFIED_TIME;

public class ContentExporter
{
    private final static Logger logger = LoggerFactory.getLogger( ContentExporter.class );

    private static final OrderExpr DEFAULT_ORDER = FieldOrderExpr.create( IndexPath.from( MODIFIED_TIME ), OrderExpr.Direction.DESC );

    private static final ChildOrder DEFAULT_CHILD_ORDER = ChildOrder.create().add( DEFAULT_ORDER ).build();

    private final NodeExporter nodeExporter;

    private final FileBlobStore fileBlobStore;

    private final ContentNodeConverter contentNodeConverter;

    private final ContentFilter contentFilter;

    public ContentExporter( final NodeExporter nodeExporter, final FileBlobStore fileBlobStore,
                            final ContentNodeConverter contentNodeConverter, final ContentFilter contentFilter )
    {
        this.nodeExporter = nodeExporter;
        this.fileBlobStore = fileBlobStore;
        this.contentNodeConverter = contentNodeConverter;
        this.contentFilter = contentFilter;
    }

    public Node export( final ContentEntity content, final NodePath parentNode )
    {
        if ( contentFilter.skipContent( content ) )
        {
            return null;
        }
        //Converts the content to a node
        Node contentNode = contentNodeConverter.toNode( content );
        contentNode = Node.create( contentNode ).
            parentPath( parentNode ).
            childOrder( DEFAULT_CHILD_ORDER ).
            build();
        contentNode = nodeExporter.renameIfDuplicated( contentNode );

        //Exports attachments
        final ContentTypeName contentType = contentNodeConverter.convertType( content );
        if ( contentType.isImageMedia() )
        {
            exportImageAttachments( content, contentNode );
            contentNode = updateImageMetadata( contentNode, content );
        }
        else
        {
            final ContentTypeName mediaType = exportAttachments( content, contentNode );
            if ( mediaType != null && mediaType.isDocumentMedia() )
            {
                contentNode.data().setString( ContentPropertyNames.TYPE, mediaType.toString() );
                contentNode = updateDocumentMetadata( contentNode, content );
            }
            else if ( contentType.isUnknownMedia() && mediaType != null )
            {
                contentNode.data().setString( ContentPropertyNames.TYPE, mediaType.toString() );
                contentNode = updateMediaMetadata( contentNode, content );
            }
        }

        //Exports the node
        try
        {
            contentNode = nodeExporter.exportNode( contentNode );
        }
        catch ( Exception e )
        {
            logger.warn( "Could not export node '" + contentNode.path() + "'", e );
        }

        return contentNode;
    }

    private Node updateImageMetadata( final Node contentNode, final ContentEntity content )
    {
        final Node.Builder imageNode = Node.create( contentNode );

        final LegacyImageContentData data = (LegacyImageContentData) content.getMainVersion().getContentData();

        Element contentDataEl = data.getContentDataXml().getRootElement();
        String description = contentDataEl.getChildText( "description" );
        String artist = null;
        String copyright = contentDataEl.getChildText( "copyright" );
        String tags = contentDataEl.getChildText( "keywords" );

        Element photographerEl = contentDataEl.getChild( "photographer" );
        if ( photographerEl != null )
        {
            artist = photographerEl.getAttribute( "name" ).getValue();
        }

        // /node/data/data/
        final PropertySet contentData = contentNode.data().getSet( ContentPropertyNames.DATA, 0 );
        contentData.setString( "caption", description );
        contentData.setString( "artist", artist );
        contentData.setString( "copyright", copyright );
        if ( StringUtils.isNotBlank( tags ) )
        {
            Stream.of( tags.split( "\\s+" ) ).forEach( ( tag ) -> contentData.addString( "tags", tag ) );
        }

        return imageNode.build();
    }

    private Node updateDocumentMetadata( final Node contentNode, final ContentEntity content )
    {
        final Node.Builder documentNode = Node.create( contentNode );

        if ( !( content.getMainVersion().getContentData() instanceof LegacyFileContentData ) )
        {
            logger.warn( "Expected LegacyFileContentData for document content: " + content.getPathAsString() );
            return contentNode;
        }
        final LegacyFileContentData data = (LegacyFileContentData) content.getMainVersion().getContentData();

        Element contentDataEl = data.getContentDataXml().getRootElement();
        String description = contentDataEl.getChildText( "description" );
        String tags = contentDataEl.getChildText( "keywords" );
        final PropertySet contentData = contentNode.data().getSet( ContentPropertyNames.DATA, 0 );
        if ( StringUtils.isNotBlank( tags ) )
        {
            Stream.of( tags.split( "\\s+" ) ).forEach( ( tag ) -> contentData.addString( "tags", tag ) );
        }
        contentData.setString( "abstract", description );

        return documentNode.build();
    }

    private Node updateMediaMetadata( final Node contentNode, final ContentEntity content )
    {
        final Node.Builder imageNode = Node.create( contentNode );

        if ( !( content.getMainVersion().getContentData() instanceof LegacyFileContentData ) )
        {
            logger.warn( "Expected LegacyFileContentData for media content: " + content.getPathAsString() );
            return contentNode;
        }
        final LegacyFileContentData data = (LegacyFileContentData) content.getMainVersion().getContentData();

        Element contentDataEl = data.getContentDataXml().getRootElement();
        String description = contentDataEl.getChildText( "description" );
        String tags = contentDataEl.getChildText( "keywords" );

        final PropertySet contentData = contentNode.data().getSet( ContentPropertyNames.DATA, 0 );
        if ( StringUtils.isNotBlank( tags ) )
        {
            Stream.of( tags.split( "\\s+" ) ).forEach( ( tag ) -> contentData.addString( "tags", tag ) );
        }
        contentData.setString( "description", description );

        return imageNode.build();
    }

    private Collection<BinaryDataKey> filterAttachmentDuplicates( final ContentVersionEntity main, final Set<BinaryDataKey> binaryKeys )
    {
        // in case of duplicated attachments(same name), take the latest created one
        if ( binaryKeys.size() == 1 )
        {
            return binaryKeys;
        }

        final Map<String, Date> duplicates = new HashMap<>();
        final Map<String, BinaryDataKey> keys = new HashMap<>();
        for ( BinaryDataKey key : binaryKeys )
        {
            final BinaryDataEntity binaryData = main.getBinaryData( key );
            final String binaryName = NamePrettyfier.create( binaryData.getName() );
            if ( !duplicates.containsKey( binaryName ) )
            {
                duplicates.put( binaryName, binaryData.getCreatedAt() );
                keys.put( binaryName, key );
            }
            else
            {
                final Date existingBinaryCreated = duplicates.get( binaryName );
                if ( existingBinaryCreated.before( binaryData.getCreatedAt() ) )
                {
                    duplicates.put( binaryName, binaryData.getCreatedAt() );
                    keys.put( binaryName, key );
                }
            }
        }
        return keys.values();
    }

    private ContentTypeName exportAttachments( final ContentEntity content, final Node contentNode )
    {
        final ContentVersionEntity main = content.getMainVersion();
        final Set<BinaryDataKey> binaryKeys = main.getContentBinaryDataKeys();

        ContentTypeName mediaType = null;

        for ( BinaryDataKey key : filterAttachmentDuplicates( main, binaryKeys ) )
        {
            final BinaryDataEntity binaryData = main.getBinaryData( key );

            final BlobKey blobKey = new BlobKey( binaryData.getBlobKey() );
            final BlobRecord blob = fileBlobStore.getRecord( blobKey );
            if ( blob == null )
            {
                logger.warn(
                    "Could not find Blob with key [" + binaryData.getBlobKey() + "] in content '" + content.getPathAsString() + "'" );
                continue;
            }

            final ByteSource byteSource = Files.asByteSource( blob.getAsFile() );
            final String binaryName = NamePrettyfier.create( binaryData.getName() );
            final BinaryReference reference = BinaryReference.from( binaryName );

            nodeExporter.exportNodeBinary( contentNode, reference, byteSource );

            final String mimeType = getMimeType( binaryData.getName() );
            if ( mediaType == null )
            {
                mediaType = ContentTypeFromMimeTypeResolver.resolve( mimeType );
            }

            String label = main.getContentBinaryData( key ).getLabel();
            if ( label == null )
            {
                label = binaryKeys.size() == 1 ? "source" : "";
            }
            addAttachmentData( contentNode.data(), label, binaryData, reference, mimeType );
        }
        return mediaType;
    }

    private void addAttachmentData( final PropertyTree nodeData, final String label, final BinaryDataEntity binaryData,
                                    final BinaryReference reference, final String mimeType )
    {
        final String binaryName = NamePrettyfier.create( binaryData.getName() );

        // /node/data/attachment
        final PropertySet attachmentSet = nodeData.addSet( ContentPropertyNames.ATTACHMENT );
        attachmentSet.setString( "name", binaryName );
        attachmentSet.setString( "label", label );
        attachmentSet.setBinaryReference( "binary", reference );
        attachmentSet.setString( "mimeType", mimeType );
        attachmentSet.setLong( "size", (long) binaryData.getSize() );

        // /node/data/data/media
        final PropertySet contentData = nodeData.getSet( ContentPropertyNames.DATA, 0 );
        final PropertySet media = contentData.addSet( ContentPropertyNames.MEDIA );
        media.setString( "attachment", reference.toString() );
        final PropertySet focalPoint = media.addSet( ContentPropertyNames.MEDIA_FOCAL_POINT );
        focalPoint.setDouble( ContentPropertyNames.MEDIA_FOCAL_POINT_X, 0.5 );
        focalPoint.setDouble( ContentPropertyNames.MEDIA_FOCAL_POINT_Y, 0.5 );
    }

    private void exportImageAttachments( final ContentEntity content, final Node contentNode )
    {
        final ContentVersionEntity main = content.getMainVersion();
        final BinaryDataEntity sourceBinaryData = main.getBinaryData( "source" );
        BinaryDataKey sourceBinaryKey = sourceBinaryData != null ? sourceBinaryData.getBinaryDataKey() : null;
        if ( sourceBinaryKey == null )
        {
            final LegacyImageContentData imageData = (LegacyImageContentData) main.getContentData();
            sourceBinaryKey = getSourceImageKey( imageData.getContentDataXml() );
        }
        if ( sourceBinaryKey == null )
        {
            return;
        }

        final BinaryDataEntity binaryData = main.getBinaryData( sourceBinaryKey );
        if ( binaryData == null )
        {
            return;
        }

        final BlobKey blobKey = new BlobKey( binaryData.getBlobKey() );
        final BlobRecord blob = fileBlobStore.getRecord( blobKey );
        if ( blob == null )
        {
            logger.warn( "Could not find Blob with key [" + binaryData.getBlobKey() + "] in content '" + content.getPathAsString() + "'" );
            return;
        }

        final ByteSource byteSource = Files.asByteSource( blob.getAsFile() );
        final String binaryName = NamePrettyfier.create( binaryData.getName() );
        if ( StringUtils.isBlank( binaryName ) )
        {
            logger.warn( "Invalid binary name [" + binaryData.getName() + "] in content '" + content.getPathAsString() + "'" );
            return;
        }
        final BinaryReference reference = BinaryReference.from( binaryName );

        nodeExporter.exportNodeBinary( contentNode, reference, byteSource );

        final String mimeType = getMimeType( binaryData.getName() );
        addAttachmentData( contentNode.data(), "source", binaryData, reference, mimeType );
    }

    private String getMimeType( final String fileName )
    {
        return MediaTypes.instance().fromFile( fileName.toLowerCase() ).toString();
    }

    private BinaryDataKey getSourceImageKey( final Document contentDataXml )
    {
        Element contentDataEl = contentDataXml.getRootElement();
        Element sourceImageEl = contentDataEl.getChild( "sourceimage" );
        if ( sourceImageEl == null )
        {
            return null;
        }
        Element binaryDataEl = sourceImageEl.getChild( "binarydata" );

        Attribute keyAttr = binaryDataEl.getAttribute( "key" );
        return new BinaryDataKey( keyAttr.getValue() );
    }
}
