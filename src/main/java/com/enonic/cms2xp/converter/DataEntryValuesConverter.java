package com.enonic.cms2xp.converter;

import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.Value;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.util.Reference;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.contentdata.custom.BooleanDataEntry;
import com.enonic.cms.core.content.contentdata.custom.DataEntry;
import com.enonic.cms.core.content.contentdata.custom.DataEntrySet;
import com.enonic.cms.core.content.contentdata.custom.DateDataEntry;
import com.enonic.cms.core.content.contentdata.custom.KeywordsDataEntry;
import com.enonic.cms.core.content.contentdata.custom.MultipleChoiceAlternative;
import com.enonic.cms.core.content.contentdata.custom.MultipleChoiceDataEntry;
import com.enonic.cms.core.content.contentdata.custom.RelationDataEntry;
import com.enonic.cms.core.content.contentdata.custom.relationdataentrylistbased.AbstractRelationDataEntryListBasedInputDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.AbstractStringBasedInputDataEntry;
import com.enonic.cms.core.content.contentdata.custom.xmlbased.AbstractXmlBasedInputDataEntry;

public class DataEntryValuesConverter
{
    private final NodeIdRegistry nodeIdRegistry;

    public DataEntryValuesConverter( final NodeIdRegistry nodeIdRegistry )
    {
        this.nodeIdRegistry = nodeIdRegistry;
    }

    public Iterable<Value> toValue( DataEntry dataEntry )
    {
        if ( dataEntry instanceof DataEntrySet )
        {
            final Value dataEntrySetAsValue = toValue( ( (DataEntrySet) dataEntry ).getEntries() );
            return Collections.singleton( dataEntrySetAsValue );
        }

        Value value = ValueFactory.newString( "" );
        switch ( dataEntry.getType() )
        {
            case BINARY: //TODO
                break;
            case BOOLEAN:
                final Boolean booleanValue = ( (BooleanDataEntry) dataEntry ).getValueAsBoolean();
                value = ValueFactory.newBoolean( booleanValue );
                break;
            case DATE:
                final Date valueDate = ( (DateDataEntry) dataEntry ).getValue();
                value = ValueFactory.newLocalDate( valueDate.toInstant().atZone( ZoneId.systemDefault() ).toLocalDate() );
                break;
            case GROUP: //TODO
                break;
            case FILES:
            case IMAGES:
            case RELATED_CONTENTS:
                final List<RelationDataEntry> relationDataEntries =
                    ( (AbstractRelationDataEntryListBasedInputDataEntry<RelationDataEntry>) dataEntry ).getEntries();
                value = toValue( relationDataEntries );
                break;
            case KEYWORDS:
                return ( (KeywordsDataEntry) dataEntry ).
                    getKeywords().
                    stream().
                    map( ValueFactory::newString ).
                    collect( Collectors.toList() );
            case MULTIPLE_CHOICE:
                final MultipleChoiceDataEntry multipleChoiceDataEntry = (MultipleChoiceDataEntry) dataEntry;
                final String multipleChoiceText = multipleChoiceDataEntry.getText();
                final List<MultipleChoiceAlternative> alternatives = multipleChoiceDataEntry.getAlternatives();

                final PropertySet multipleChoicePropertySet = new PropertySet();
                multipleChoicePropertySet.setProperty( "text", ValueFactory.newString( multipleChoiceText ) );
                alternatives.stream().
                    map( multipleChoiceAlternative -> {
                        PropertySet multipleChoiceAlternativePropertySet = new PropertySet();
                        multipleChoiceAlternativePropertySet.setProperty( "alternativeText", ValueFactory.newString(
                            multipleChoiceAlternative.getAlternativeText() ) );
                        multipleChoiceAlternativePropertySet.setProperty( "correct", ValueFactory.newBoolean(
                            multipleChoiceAlternative.isCorrect() ) );
                        return multipleChoiceAlternativePropertySet;
                    } ).
                    forEach( multipleChoiceAlternativePropertySet -> {
                        final Value multipleChoiceAlternativeValue = ValueFactory.newPropertySet( multipleChoiceAlternativePropertySet );
                        multipleChoicePropertySet.addProperty( "alternative", multipleChoiceAlternativeValue );
                    } );

                value = ValueFactory.newPropertySet( multipleChoicePropertySet );
                break;
            case FILE:
            case IMAGE:
            case RELATED_CONTENT:
                final ContentKey contentKey = ( (RelationDataEntry) dataEntry ).getContentKey();
                if ( contentKey != null )
                { //TODO Why could that be null
                    final NodeId nodeId = nodeIdRegistry.getNodeId( contentKey );
                    value = ValueFactory.newReference( new Reference( nodeId ) );
                }
                break;
            case HTML_AREA:
            case TEXT_AREA:
            case SELECTOR:
            case TEXT:
            case URL:
                final String valueString = ( (AbstractStringBasedInputDataEntry) dataEntry ).getValue();
                value = ValueFactory.newString( valueString );
                break;
            case XML:
                final String valueXml = ( (AbstractXmlBasedInputDataEntry) dataEntry ).getValueAsString();
                value = ValueFactory.newXml( valueXml );
                break;
        }

        return Collections.singleton( value );
    }

    public Value toValue( Iterable<? extends DataEntry> dataEntries )
    {
        final PropertySet propertySet = new PropertySet();
        for ( DataEntry dataEntry : dataEntries )
        {
            final String entryPathName = StringUtils.substringAfterLast( dataEntry.getXPath(), "/" );
            final String name = StringUtils.isBlank( entryPathName ) ? dataEntry.getName() : entryPathName;
            propertySet.setValues( name, toValue( dataEntry ) );
        }

        return ValueFactory.newPropertySet( propertySet );
    }

}
