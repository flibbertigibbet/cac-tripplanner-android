package org.gophillygo.app.data.models;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;

import com.google.gson.annotations.SerializedName;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Objects;


@Entity(inheritSuperIndices = true,
        indices = {@Index("educational"), @Index("nature"), @Index("exercise")})
public class Destination extends Attraction {

    private static final String LOG_LABEL = "DestinationModel";
    private static final NumberFormat numberFormatter = NumberFormat.getNumberInstance();
    static {
        numberFormatter.setMinimumFractionDigits(0);
        numberFormatter.setMaximumFractionDigits(2);
    }

    private final String city;
    private final String state;
    private final String address;

    private final ArrayList<String> categories;

    // convenience boolean flags for whether categories are set
    @Embedded
    private DestinationCategories categoryFlags;

    @Embedded
    private final DestinationLocation location;

    @ColumnInfo(name = "watershed_alliance", index = true)
    @SerializedName("watershed_alliance")
    private final boolean watershedAlliance;

    @Embedded
    private final DestinationAttributes attributes;

    @ColumnInfo(name = "zipcode")
    @SerializedName("zipcode")
    private final String zipCode;

    // convenience property to track distance to each destination
    @ColumnInfo(index = true)
    private float distance;
    @Ignore
    private String formattedDistance;

    public Destination(int id, int placeID, String name, boolean accessible, String image,
                       String city, boolean cycling, String zipCode, String description,
                       int priority, String state, String address, DestinationLocation location,
                       DestinationAttributes attributes, boolean watershedAlliance, String websiteUrl,
                       String wideImage, boolean isEvent, ArrayList<String> activities,
                       ArrayList<String> categories, ArrayList<String> extraWideImages) {

        // initialize Attraction
        super(id, placeID, name, accessible, image, cycling, description, priority, websiteUrl,
              wideImage, isEvent, activities, extraWideImages);

        this.city = city;
        this.zipCode = zipCode;
        this.state = state;
        this.address = address;
        this.watershedAlliance = watershedAlliance;

        this.location = location;
        this.attributes = attributes;
        this.categories = categories;
    }

    public void setDistance(float distance) {
        this.distance = distance;
        // FIXME: move string constant to resources
        this.formattedDistance = numberFormatter.format(distance) + " mi";
    }

    public void setCategoryFlags(DestinationCategories categoryFlags) {
        this.categoryFlags = categoryFlags;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getAddress() {
        return address;
    }

    public DestinationLocation getLocation() {
        return location;
    }

    public DestinationAttributes getAttributes() {
        return attributes;
    }

    public String getZipCode() {
        return zipCode;
    }

    public Float getDistance() {
        return distance;
    }

    public String getFormattedDistance() {
        return formattedDistance;
    }

    public boolean isWatershedAlliance() {
        return watershedAlliance;
    }

    public ArrayList<String> getCategories() {
        return categories;
    }

    public DestinationCategories getCategoryFlags() {
        return categoryFlags;
    }

    // get a dot-separated string listing all the categories for this place (nature, exercise, etc.)
    public String getCategoriesString() {
        StringBuilder stringBuilder = new StringBuilder("");
        // separate activities with dots
        String dot = getHtmlFromString("&nbsp;&#8226;&nbsp;").toString();

        for (String category: categories) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(dot);
            }

            stringBuilder.append(category);
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Destination that = (Destination) o;
        return watershedAlliance == that.watershedAlliance &&
                Float.compare(that.distance, distance) == 0 &&
                Objects.equals(city, that.city) &&
                Objects.equals(state, that.state) &&
                Objects.equals(address, that.address) &&
                Objects.equals(categories, that.categories) &&
                Objects.equals(location, that.location) &&
                Objects.equals(attributes, that.attributes) &&
                Objects.equals(zipCode, that.zipCode) &&
                Objects.equals(formattedDistance, that.formattedDistance);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), city, state, address, categories, location, watershedAlliance, attributes, zipCode, distance, formattedDistance);
    }
}
