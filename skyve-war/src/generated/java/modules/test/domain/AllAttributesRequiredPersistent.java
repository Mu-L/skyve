package modules.test.domain;

import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Geometry;
import org.skyve.CORE;
import org.skyve.domain.messages.DomainException;
import org.skyve.domain.types.DateOnly;
import org.skyve.domain.types.DateTime;
import org.skyve.domain.types.Decimal10;
import org.skyve.domain.types.Decimal2;
import org.skyve.domain.types.Decimal5;
import org.skyve.domain.types.Enumeration;
import org.skyve.domain.types.TimeOnly;
import org.skyve.domain.types.Timestamp;
import org.skyve.impl.domain.AbstractPersistentBean;
import org.skyve.impl.domain.ChangeTrackingArrayList;
import org.skyve.impl.domain.types.jaxb.DateOnlyMapper;
import org.skyve.impl.domain.types.jaxb.DateTimeMapper;
import org.skyve.impl.domain.types.jaxb.Decimal10Mapper;
import org.skyve.impl.domain.types.jaxb.Decimal2Mapper;
import org.skyve.impl.domain.types.jaxb.Decimal5Mapper;
import org.skyve.impl.domain.types.jaxb.GeometryMapper;
import org.skyve.impl.domain.types.jaxb.TimeOnlyMapper;
import org.skyve.impl.domain.types.jaxb.TimestampMapper;
import org.skyve.metadata.model.document.Bizlet.DomainValue;
import org.skyve.util.Util;

/**
 * All Persistent
 * <br/>
 * All required persistent attributes.
 * 
 * @depend - - - Enum3
 * @navhas n aggregatedCollection 1..n AllAttributesRequiredPersistent
 * @navhas n aggregatedAssociation 1 AllAttributesRequiredPersistent
 * @stereotype "persistent"
 */
@XmlType
@XmlRootElement
@Generated(value = "org.skyve.impl.generate.OverridableDomainGenerator")
public class AllAttributesRequiredPersistent extends AbstractPersistentBean {
	/**
	 * For Serialization
	 * @hidden
	 */
	private static final long serialVersionUID = 1L;

	/** @hidden */
	public static final String MODULE_NAME = "test";

	/** @hidden */
	public static final String DOCUMENT_NAME = "AllAttributesRequiredPersistent";

	/** @hidden */
	public static final String aggregatedAssociationPropertyName = "aggregatedAssociation";

	/** @hidden */
	public static final String booleanFlagPropertyName = "booleanFlag";

	/** @hidden */
	public static final String aggregatedCollectionPropertyName = "aggregatedCollection";

	/** @hidden */
	public static final String colourPropertyName = "colour";

	/** @hidden */
	public static final String datePropertyName = "date";

	/** @hidden */
	public static final String dateTimePropertyName = "dateTime";

	/** @hidden */
	public static final String decimal10PropertyName = "decimal10";

	/** @hidden */
	public static final String decimal2PropertyName = "decimal2";

	/** @hidden */
	public static final String decimal5PropertyName = "decimal5";

	/** @hidden */
	public static final String enum3PropertyName = "enum3";

	/** @hidden */
	public static final String geometryPropertyName = "geometry";

	/** @hidden */
	public static final String idPropertyName = "id";

	/** @hidden */
	public static final String normalIntegerPropertyName = "normalInteger";

	/** @hidden */
	public static final String inverseAggregatedAssociationPropertyName = "inverseAggregatedAssociation";

	/** @hidden */
	public static final String longIntegerPropertyName = "longInteger";

	/** @hidden */
	public static final String markupPropertyName = "markup";

	/** @hidden */
	public static final String memoPropertyName = "memo";

	/** @hidden */
	public static final String textPropertyName = "text";

	/** @hidden */
	public static final String timePropertyName = "time";

	/** @hidden */
	public static final String timestampPropertyName = "timestamp";

	/**
	 * Enum 3
	 **/
	@XmlEnum
	@Generated(value = "org.skyve.impl.generate.OverridableDomainGenerator")
	public static enum Enum3 implements Enumeration {
		one("one", "one"),
		two("two", "two"),
		three("three", "three");

		private String code;
		private String description;

		/** @hidden */
		private DomainValue domainValue;

		/** @hidden */
		private static List<DomainValue> domainValues = Stream.of(values()).map(Enum3::toDomainValue).collect(Collectors.toUnmodifiableList());

		private Enum3(String code, String description) {
			this.code = code;
			this.description = description;
			this.domainValue = new DomainValue(code, description);
		}

		@Override
		public String toCode() {
			return code;
		}

		@Override
		public String toLocalisedDescription() {
			return Util.i18n(description);
		}

		@Override
		public DomainValue toDomainValue() {
			return domainValue;
		}

		public static Enum3 fromCode(String code) {
			Enum3 result = null;

			for (Enum3 value : values()) {
				if (value.code.equals(code)) {
					result = value;
					break;
				}
			}

			return result;
		}

		public static Enum3 fromLocalisedDescription(String description) {
			Enum3 result = null;

			for (Enum3 value : values()) {
				if (value.toLocalisedDescription().equals(description)) {
					result = value;
					break;
				}
			}

			return result;
		}

		public static List<DomainValue> toDomainValues() {
			return domainValues;
		}
	}

	/**
	 * Aggregated Association
	 **/
	private AllAttributesRequiredPersistent aggregatedAssociation = null;

	/**
	 * Boolean Flag
	 **/
	private Boolean booleanFlag;

	/**
	 * Aggregated Collection
	 **/
	private List<AllAttributesRequiredPersistent> aggregatedCollection = new ChangeTrackingArrayList<>("aggregatedCollection", this);

	/**
	 * Colour
	 **/
	private String colour;

	/**
	 * Date
	 **/
	private DateOnly date;

	/**
	 * Date Time
	 **/
	private DateTime dateTime;

	/**
	 * Decimal 10
	 **/
	private Decimal10 decimal10;

	/**
	 * Decimal 2
	 **/
	private Decimal2 decimal2;

	/**
	 * Decimal 5
	 **/
	private Decimal5 decimal5;

	/**
	 * Enum 3
	 **/
	private Enum3 enum3;

	/**
	 * Geometry
	 **/
	private Geometry geometry;

	/**
	 * Id
	 **/
	private String id;

	/**
	 * Integer
	 **/
	private Integer normalInteger;

	/**
	 * Inverse
	 **/
	private List<AllAttributesRequiredPersistent> inverseAggregatedAssociation = new ArrayList<>();

	/**
	 * Long Integer
	 **/
	private Long longInteger;

	/**
	 * Markup
	 **/
	private String markup;

	/**
	 * Memo
	 **/
	private String memo;

	/**
	 * Text
	 **/
	private String text;

	/**
	 * Time
	 **/
	private TimeOnly time;

	/**
	 * Timestamp
	 **/
	private Timestamp timestamp;

	@Override
	@XmlTransient
	public String getBizModule() {
		return AllAttributesRequiredPersistent.MODULE_NAME;
	}

	@Override
	@XmlTransient
	public String getBizDocument() {
		return AllAttributesRequiredPersistent.DOCUMENT_NAME;
	}

	public static AllAttributesRequiredPersistent newInstance() {
		try {
			return CORE.getUser().getCustomer().getModule(MODULE_NAME).getDocument(CORE.getUser().getCustomer(), DOCUMENT_NAME).newInstance(CORE.getUser());
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new DomainException(e);
		}
	}

	@Override
	@XmlTransient
	public String getBizKey() {
		try {
			return org.skyve.util.Binder.formatMessage("{text}", this);
		}
		catch (@SuppressWarnings("unused") Exception e) {
			return "Unknown";
		}
	}

	/**
	 * {@link #aggregatedAssociation} accessor.
	 * @return	The value.
	 **/
	public AllAttributesRequiredPersistent getAggregatedAssociation() {
		return aggregatedAssociation;
	}

	/**
	 * {@link #aggregatedAssociation} mutator.
	 * @param aggregatedAssociation	The new value.
	 **/
	@XmlElement
	public void setAggregatedAssociation(AllAttributesRequiredPersistent aggregatedAssociation) {
		if (this.aggregatedAssociation != aggregatedAssociation) {
			preset(aggregatedAssociationPropertyName, aggregatedAssociation);
			AllAttributesRequiredPersistent oldAggregatedAssociation = this.aggregatedAssociation;
			this.aggregatedAssociation = aggregatedAssociation;
			if ((aggregatedAssociation != null) && (aggregatedAssociation.getInverseAggregatedAssociationElementById(getBizId()) == null)) {
				aggregatedAssociation.getInverseAggregatedAssociation().add(this);
			}
			if (oldAggregatedAssociation != null) {
				oldAggregatedAssociation.getInverseAggregatedAssociation().remove(this);
			}
		}
	}

	public void nullAggregatedAssociation() {
		this.aggregatedAssociation = null;
	}

	/**
	 * {@link #booleanFlag} accessor.
	 * @return	The value.
	 **/
	public Boolean getBooleanFlag() {
		return booleanFlag;
	}

	/**
	 * {@link #booleanFlag} mutator.
	 * @param booleanFlag	The new value.
	 **/
	@XmlElement
	public void setBooleanFlag(Boolean booleanFlag) {
		preset(booleanFlagPropertyName, booleanFlag);
		this.booleanFlag = booleanFlag;
	}

	/**
	 * {@link #aggregatedCollection} accessor.
	 * @return	The value.
	 **/
	@XmlElement
	public List<AllAttributesRequiredPersistent> getAggregatedCollection() {
		return aggregatedCollection;
	}

	/**
	 * {@link #aggregatedCollection} accessor.
	 * @param bizId	The bizId of the element in the list.
	 * @return	The value of the element in the list.
	 **/
	public AllAttributesRequiredPersistent getAggregatedCollectionElementById(String bizId) {
		return getElementById(aggregatedCollection, bizId);
	}

	/**
	 * {@link #aggregatedCollection} mutator.
	 * @param bizId	The bizId of the element in the list.
	 * @param element	The new value of the element in the list.
	 **/
	public void setAggregatedCollectionElementById(String bizId, AllAttributesRequiredPersistent element) {
		setElementById(aggregatedCollection, element);
	}

	/**
	 * {@link #aggregatedCollection} add.
	 * @param element	The element to add.
	 **/
	public boolean addAggregatedCollectionElement(AllAttributesRequiredPersistent element) {
		return aggregatedCollection.add(element);
	}

	/**
	 * {@link #aggregatedCollection} add.
	 * @param index	The index in the list to add the element to.
	 * @param element	The element to add.
	 **/
	public void addAggregatedCollectionElement(int index, AllAttributesRequiredPersistent element) {
		aggregatedCollection.add(index, element);
	}

	/**
	 * {@link #aggregatedCollection} remove.
	 * @param element	The element to remove.
	 **/
	public boolean removeAggregatedCollectionElement(AllAttributesRequiredPersistent element) {
		return aggregatedCollection.remove(element);
	}

	/**
	 * {@link #aggregatedCollection} remove.
	 * @param index	The index in the list to remove the element from.
	 **/
	public AllAttributesRequiredPersistent removeAggregatedCollectionElement(int index) {
		return aggregatedCollection.remove(index);
	}

	/**
	 * {@link #colour} accessor.
	 * @return	The value.
	 **/
	public String getColour() {
		return colour;
	}

	/**
	 * {@link #colour} mutator.
	 * @param colour	The new value.
	 **/
	@XmlElement
	public void setColour(String colour) {
		preset(colourPropertyName, colour);
		this.colour = colour;
	}

	/**
	 * {@link #date} accessor.
	 * @return	The value.
	 **/
	public DateOnly getDate() {
		return date;
	}

	/**
	 * {@link #date} mutator.
	 * @param date	The new value.
	 **/
	@XmlElement
	@XmlSchemaType(name = "date")
	@XmlJavaTypeAdapter(DateOnlyMapper.class)
	public void setDate(DateOnly date) {
		preset(datePropertyName, date);
		this.date = date;
	}

	/**
	 * {@link #dateTime} accessor.
	 * @return	The value.
	 **/
	public DateTime getDateTime() {
		return dateTime;
	}

	/**
	 * {@link #dateTime} mutator.
	 * @param dateTime	The new value.
	 **/
	@XmlElement
	@XmlSchemaType(name = "dateTime")
	@XmlJavaTypeAdapter(DateTimeMapper.class)
	public void setDateTime(DateTime dateTime) {
		preset(dateTimePropertyName, dateTime);
		this.dateTime = dateTime;
	}

	/**
	 * {@link #decimal10} accessor.
	 * @return	The value.
	 **/
	public Decimal10 getDecimal10() {
		return decimal10;
	}

	/**
	 * {@link #decimal10} mutator.
	 * @param decimal10	The new value.
	 **/
	@XmlElement
	@XmlJavaTypeAdapter(Decimal10Mapper.class)
	public void setDecimal10(Decimal10 decimal10) {
		preset(decimal10PropertyName, decimal10);
		this.decimal10 = decimal10;
	}

	/**
	 * {@link #decimal2} accessor.
	 * @return	The value.
	 **/
	public Decimal2 getDecimal2() {
		return decimal2;
	}

	/**
	 * {@link #decimal2} mutator.
	 * @param decimal2	The new value.
	 **/
	@XmlElement
	@XmlJavaTypeAdapter(Decimal2Mapper.class)
	public void setDecimal2(Decimal2 decimal2) {
		preset(decimal2PropertyName, decimal2);
		this.decimal2 = decimal2;
	}

	/**
	 * {@link #decimal5} accessor.
	 * @return	The value.
	 **/
	public Decimal5 getDecimal5() {
		return decimal5;
	}

	/**
	 * {@link #decimal5} mutator.
	 * @param decimal5	The new value.
	 **/
	@XmlElement
	@XmlJavaTypeAdapter(Decimal5Mapper.class)
	public void setDecimal5(Decimal5 decimal5) {
		preset(decimal5PropertyName, decimal5);
		this.decimal5 = decimal5;
	}

	/**
	 * {@link #enum3} accessor.
	 * @return	The value.
	 **/
	public Enum3 getEnum3() {
		return enum3;
	}

	/**
	 * {@link #enum3} mutator.
	 * @param enum3	The new value.
	 **/
	@XmlElement
	public void setEnum3(Enum3 enum3) {
		preset(enum3PropertyName, enum3);
		this.enum3 = enum3;
	}

	/**
	 * {@link #geometry} accessor.
	 * @return	The value.
	 **/
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * {@link #geometry} mutator.
	 * @param geometry	The new value.
	 **/
	@XmlElement
	@XmlJavaTypeAdapter(GeometryMapper.class)
	public void setGeometry(Geometry geometry) {
		preset(geometryPropertyName, geometry);
		this.geometry = geometry;
	}

	/**
	 * {@link #id} accessor.
	 * @return	The value.
	 **/
	public String getId() {
		return id;
	}

	/**
	 * {@link #id} mutator.
	 * @param id	The new value.
	 **/
	@XmlElement
	public void setId(String id) {
		preset(idPropertyName, id);
		this.id = id;
	}

	/**
	 * {@link #normalInteger} accessor.
	 * @return	The value.
	 **/
	public Integer getNormalInteger() {
		return normalInteger;
	}

	/**
	 * {@link #normalInteger} mutator.
	 * @param normalInteger	The new value.
	 **/
	@XmlElement
	public void setNormalInteger(Integer normalInteger) {
		preset(normalIntegerPropertyName, normalInteger);
		this.normalInteger = normalInteger;
	}

	/**
	 * {@link #inverseAggregatedAssociation} accessor.
	 * @return	The value.
	 **/
	@XmlElement
	public List<AllAttributesRequiredPersistent> getInverseAggregatedAssociation() {
		return inverseAggregatedAssociation;
	}

	/**
	 * {@link #inverseAggregatedAssociation} accessor.
	 * @param bizId	The bizId of the element in the list.
	 * @return	The value of the element in the list.
	 **/
	public AllAttributesRequiredPersistent getInverseAggregatedAssociationElementById(String bizId) {
		return getElementById(inverseAggregatedAssociation, bizId);
	}

	/**
	 * {@link #inverseAggregatedAssociation} mutator.
	 * @param bizId	The bizId of the element in the list.
	 * @param element	The new value of the element in the list.
	 **/
	public void setInverseAggregatedAssociationElementById(String bizId, AllAttributesRequiredPersistent element) {
		setElementById(inverseAggregatedAssociation, element);
	}

	/**
	 * {@link #inverseAggregatedAssociation} add.
	 * @param element	The element to add.
	 **/
	public boolean addInverseAggregatedAssociationElement(AllAttributesRequiredPersistent element) {
		boolean result = false;
		if (getElementById(inverseAggregatedAssociation, element.getBizId()) == null) {
			result = inverseAggregatedAssociation.add(element);
		}
		element.setAggregatedAssociation(this);
		return result;
	}

	/**
	 * {@link #inverseAggregatedAssociation} add.
	 * @param index	The index in the list to add the element to.
	 * @param element	The element to add.
	 **/
	public void addInverseAggregatedAssociationElement(int index, AllAttributesRequiredPersistent element) {
		inverseAggregatedAssociation.add(index, element);
		element.setAggregatedAssociation(this);
	}

	/**
	 * {@link #inverseAggregatedAssociation} remove.
	 * @param element	The element to remove.
	 **/
	public boolean removeInverseAggregatedAssociationElement(AllAttributesRequiredPersistent element) {
		boolean result = inverseAggregatedAssociation.remove(element);
		if (result) {
			element.nullAggregatedAssociation();
		}
		return result;
	}

	/**
	 * {@link #inverseAggregatedAssociation} remove.
	 * @param index	The index in the list to remove the element from.
	 **/
	public AllAttributesRequiredPersistent removeInverseAggregatedAssociationElement(int index) {
		AllAttributesRequiredPersistent result = inverseAggregatedAssociation.remove(index);
		result.nullAggregatedAssociation();
		return result;
	}

	/**
	 * {@link #longInteger} accessor.
	 * @return	The value.
	 **/
	public Long getLongInteger() {
		return longInteger;
	}

	/**
	 * {@link #longInteger} mutator.
	 * @param longInteger	The new value.
	 **/
	@XmlElement
	public void setLongInteger(Long longInteger) {
		preset(longIntegerPropertyName, longInteger);
		this.longInteger = longInteger;
	}

	/**
	 * {@link #markup} accessor.
	 * @return	The value.
	 **/
	public String getMarkup() {
		return markup;
	}

	/**
	 * {@link #markup} mutator.
	 * @param markup	The new value.
	 **/
	@XmlElement
	public void setMarkup(String markup) {
		preset(markupPropertyName, markup);
		this.markup = markup;
	}

	/**
	 * {@link #memo} accessor.
	 * @return	The value.
	 **/
	public String getMemo() {
		return memo;
	}

	/**
	 * {@link #memo} mutator.
	 * @param memo	The new value.
	 **/
	@XmlElement
	public void setMemo(String memo) {
		preset(memoPropertyName, memo);
		this.memo = memo;
	}

	/**
	 * {@link #text} accessor.
	 * @return	The value.
	 **/
	public String getText() {
		return text;
	}

	/**
	 * {@link #text} mutator.
	 * @param text	The new value.
	 **/
	@XmlElement
	public void setText(String text) {
		preset(textPropertyName, text);
		this.text = text;
	}

	/**
	 * {@link #time} accessor.
	 * @return	The value.
	 **/
	public TimeOnly getTime() {
		return time;
	}

	/**
	 * {@link #time} mutator.
	 * @param time	The new value.
	 **/
	@XmlElement
	@XmlSchemaType(name = "time")
	@XmlJavaTypeAdapter(TimeOnlyMapper.class)
	public void setTime(TimeOnly time) {
		preset(timePropertyName, time);
		this.time = time;
	}

	/**
	 * {@link #timestamp} accessor.
	 * @return	The value.
	 **/
	public Timestamp getTimestamp() {
		return timestamp;
	}

	/**
	 * {@link #timestamp} mutator.
	 * @param timestamp	The new value.
	 **/
	@XmlElement
	@XmlSchemaType(name = "dateTime")
	@XmlJavaTypeAdapter(TimestampMapper.class)
	public void setTimestamp(Timestamp timestamp) {
		preset(timestampPropertyName, timestamp);
		this.timestamp = timestamp;
	}
}
