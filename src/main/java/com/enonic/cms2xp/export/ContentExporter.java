package com.enonic.cms2xp.export;

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
import com.enonic.xp.core.impl.content.ContentPathNameGenerator;
import com.enonic.xp.core.impl.content.ContentTypeFromMimeTypeResolver;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
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

public class ContentExporter
{
    private final static Logger logger = LoggerFactory.getLogger( ContentExporter.class );

    private final NodeExporter nodeExporter;

    private final FileBlobStore fileBlobStore;

    private final ContentNodeConverter contentNodeConverter;

    private final ContentKeyResolver contentKeyResolver;

    public ContentExporter( final NodeExporter nodeExporter, final FileBlobStore fileBlobStore,
                            final ContentNodeConverter contentNodeConverter, final ContentKeyResolver contentKeyResolver )
    {
        this.nodeExporter = nodeExporter;
        this.fileBlobStore = fileBlobStore;
        this.contentNodeConverter = contentNodeConverter;
        this.contentKeyResolver = contentKeyResolver;
    }

    public void export( final Iterable<ContentEntity> contents, final NodePath parentNode )
    {
        for ( ContentEntity content : contents )
        {
            //Converts the content to a node
            Node contentNode = contentNodeConverter.toNode( content );
            contentNode = Node.create( contentNode ).
                parentPath( parentNode ).
                build();

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
                nodeExporter.exportNode( contentNode );
            }
            catch ( Exception e )
            {
                logger.warn( "Could not export node '" + contentNode.path() + "'", e );
            }
            contentKeyResolver.add( content.getKey(), contentNode.id() );
        }
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
            return contentNode;
        }
        final LegacyFileContentData data = (LegacyFileContentData) content.getMainVersion().getContentData();

        Element contentDataEl = data.getContentDataXml().getRootElement();
        String description = contentDataEl.getChildText( "description" );
        String tags = contentDataEl.getChildText( "keywords" );
        // /node/data/data/
        final PropertySet contentData = contentNode.data().getSet( ContentPropertyNames.DATA, 0 );
        if ( StringUtils.isNotBlank( tags ) )
        {
            Stream.of( tags.split( "\\s+" ) ).forEach( ( tag ) -> contentData.addString( "tags", tag ) );
        }
        contentData.setString( "description", description );

        return imageNode.build();
    }

    private ContentTypeName exportAttachments( final ContentEntity content, final Node contentNode )
    {
        final ContentVersionEntity main = content.getMainVersion();
        final Set<BinaryDataKey> binaryKeys = main.getContentBinaryDataKeys();

        ContentTypeName mediaType = null;
        for ( BinaryDataKey key : binaryKeys )
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
            final String binaryName = new ContentPathNameGenerator().generatePathName( binaryData.getName() );
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
        final String binaryName = new ContentPathNameGenerator().generatePathName( binaryData.getName() );

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
        final LegacyImageContentData imageData = (LegacyImageContentData) main.getContentData();

        final BinaryDataKey key = getSourceImageKey( imageData.getContentDataXml() );
        if ( key == null )
        {
            return;
        }
        final BinaryDataEntity binaryData = main.getBinaryData( key );

        final BlobKey blobKey = new BlobKey( binaryData.getBlobKey() );
        final BlobRecord blob = fileBlobStore.getRecord( blobKey );
        if ( blob == null )
        {
            logger.warn( "Could not find Blob with key [" + binaryData.getBlobKey() + "] in content '" + content.getPathAsString() + "'" );
            return;
        }

        final ByteSource byteSource = Files.asByteSource( blob.getAsFile() );
        final String binaryName = new ContentPathNameGenerator().generatePathName( binaryData.getName() );
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
