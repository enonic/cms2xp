package com.enonic.cms2xp.converter;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

    public Value toValue( final DataEntry dataEntry )
    {
        final Value[] values = this.toValues( dataEntry );
        if ( values == null )
        {
            return null;
        }
        if ( values.length > 1 )
        {
            throw new RuntimeException( "Expected single value in data entry: " + dataEntry.getXPath() );
        }
        return values[0];
    }

    public Value[] toValues( final DataEntry dataEntry )
    {
        if ( dataEntry instanceof DataEntrySet )
        {
            return toValue( ( (DataEntrySet) dataEntry ).getEntries() );
        }

        switch ( dataEntry.getType() )
        {
            case BINARY: //TODO
                break;
            case BOOLEAN:
                return single( toValue( (BooleanDataEntry) dataEntry ) );
            case DATE:
                return single( toValue( (DateDataEntry) dataEntry ) );
            case GROUP: //TODO
                break;
            case FILES:
            case IMAGES:
            case RELATED_CONTENTS:
                return toValue( (AbstractRelationDataEntryListBasedInputDataEntry<RelationDataEntry>) dataEntry );
            case KEYWORDS:
                //Obsolete
                break;
            case MULTIPLE_CHOICE:
                return single( toValue( (MultipleChoiceDataEntry) dataEntry ) );
            case FILE:
            case IMAGE:
            case RELATED_CONTENT:
                return single( toValue( (RelationDataEntry) dataEntry ) );
            case HTML_AREA:
            case TEXT_AREA:
            case SELECTOR:
            case TEXT:
            case URL:
                return single( toValue( (AbstractStringBasedInputDataEntry) dataEntry ) );
            case XML:
                return single( toValue( (AbstractXmlBasedInputDataEntry) dataEntry ) );
        }

        return null;
    }

    private Value[] single( final Value value )
    {
        if ( value == null )
        {
            return null;
        }
        return new Value[]{value};
    }

    private Value[] toValue( final List<DataEntry> dataEntries )
    {
        final PropertySet propertySet = new PropertySet();
        for ( DataEntry dataEntry : dataEntries )
        {
            final String entryPathName = StringUtils.substringAfterLast( dataEntry.getXPath(), "/" );
            final String propertyName = StringUtils.isBlank( entryPathName ) ? dataEntry.getName() : entryPathName;

            final Value[] propertyValues = toValues( dataEntry );
            if ( propertyValues == null )
            {
                continue;
            }

            for ( Value propertyValue : propertyValues )
            {
                if ( propertyValue != null && !( propertyValue.isSet() && propertyValue.asData().getPropertySize() == 0 ) )
                {
                    propertySet.addProperty( propertyName, propertyValue );
                }
            }
        }

        return propertySet.getPropertySize() == 0 ? null : single( ValueFactory.newPropertySet( propertySet ) );
    }

    private Value toValue( final BooleanDataEntry booleanDataEntry )
    {
        return ValueFactory.newBoolean( booleanDataEntry.getValueAsBoolean() );
    }

    private Value toValue( final DateDataEntry dateDataEntry )
    {
        final Date value = dateDataEntry.getValue();
        if ( value == null )
        {
            return null;
        }
        return ValueFactory.newLocalDate( value.toInstant().atZone( ZoneId.systemDefault() ).toLocalDate() );
    }

    private Value[] toValue( final AbstractRelationDataEntryListBasedInputDataEntry<RelationDataEntry> relationDataEntryListBasedDataEntry )
    {
        final List<RelationDataEntry> relationDataEntries = relationDataEntryListBasedDataEntry.getEntries();
        if ( relationDataEntries.isEmpty() )
        {
            return null;
        }
        return relationDataEntries.stream().map( this::toValue ).filter( Objects::nonNull ).toArray( Value[]::new );
    }

    private Value toValue( final MultipleChoiceDataEntry multipleChoiceDataEntry )
    {
        final String multipleChoiceText = multipleChoiceDataEntry.getText();
        final List<MultipleChoiceAlternative> alternatives = multipleChoiceDataEntry.getAlternatives();

        final PropertySet multipleChoicePropertySet = new PropertySet();
        multipleChoicePropertySet.setProperty( "text", ValueFactory.newString( multipleChoiceText ) );
        alternatives.stream().
            map( multipleChoiceAlternative -> {
                PropertySet multipleChoiceAlternativePropertySet = new PropertySet();
                multipleChoiceAlternativePropertySet.setProperty( "alternativeText", ValueFactory.newString(
                    multipleChoiceAlternative.getAlternativeText() ) );
                multipleChoiceAlternativePropertySet.setProperty( "correct",
                                                                  ValueFactory.newBoolean( multipleChoiceAlternative.isCorrect() ) );
                return multipleChoiceAlternativePropertySet;
            } ).
            forEach( multipleChoiceAlternativePropertySet -> {
                final Value multipleChoiceAlternativeValue = ValueFactory.newPropertySet( multipleChoiceAlternativePropertySet );
                multipleChoicePropertySet.addProperty( "alternative", multipleChoiceAlternativeValue );
            } );

        return ValueFactory.newPropertySet( multipleChoicePropertySet );
    }

    private Value toValue( final RelationDataEntry relationDataEntry )
    {
        final ContentKey contentKey = relationDataEntry.getContentKey();
        if ( contentKey != null )
        { //TODO Why could that be null
            final NodeId nodeId = nodeIdRegistry.getNodeId( contentKey );
            return ValueFactory.newReference( new Reference( nodeId ) );
        }
        return null;
    }

    private Value toValue( final AbstractStringBasedInputDataEntry stringBasedInputDataEntry )
    {
        return ValueFactory.newString( stringBasedInputDataEntry.getValue() );
    }

    private Value toValue( final AbstractXmlBasedInputDataEntry xmlBasedInputDataEntry )
    {
        return ValueFactory.newXml( xmlBasedInputDataEntry.getValueAsString() );
    }
}
