package com.gophillygo.app.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;


@Entity
public class Destination {
    @PrimaryKey
    private final int id;

    private final int placeID;
    private final String name;
    private final boolean accessible;
    private final String image;
    private final String city;
    private final boolean cycling;
    private final String description;
    private final int priority;
    private final String state;
    private final String address;

    @Embedded
    private final DestinationLocation location;

    @Embedded
    private final DestinationAttributes attributes;

    @ColumnInfo(name = "watershed_alliance")
    private final boolean watershedAlliance;

    @ColumnInfo(name = "website_url")
    private final String websiteUrl;

    @ColumnInfo(name = "wide_image")
    private final String wideImage;

    @ColumnInfo(name = "is_event")
    private final boolean isEvent;

    @ColumnInfo(name = "zipcode")
    private final String zipCode;

    public Destination(int id, int placeID, String name, boolean accessible, String image,
                       String city, boolean cycling, String zipCode, String description,
                       int priority, String state, String address, DestinationLocation location,
                       DestinationAttributes attributes, boolean watershedAlliance, String websiteUrl,
                       String wideImage, boolean isEvent) {
        this.id = id;
        this.placeID = placeID;
        this.name = name;
        this.accessible = accessible;
        this.image = image;
        this.city = city;
        this.cycling = cycling;
        this.zipCode = zipCode;
        this.description = description;
        this.priority = priority;
        this.state = state;
        this.address = address;
        this.watershedAlliance = watershedAlliance;
        this.websiteUrl = websiteUrl;
        this.wideImage = wideImage;
        this.isEvent = isEvent;

        this.location = location;
        this.attributes = attributes;
    }

    public int getId() {
        return id;
    }

    public int getPlaceID() {
        return placeID;
    }

    public String getName() {
        return name;
    }

    public boolean isAccessible() {
        return accessible;
    }

    public String getImage() {
        return image;
    }

    public String getCity() {
        return city;
    }

    public boolean isCycling() {
        return cycling;
    }

    public String getZipcode() {
        return zipCode;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
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

    public boolean isWatershedAlliance() {
        return watershedAlliance;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public String getWideImage() {
        return wideImage;
    }

    public boolean isEvent() {
        return isEvent;
    }

    public String getZipCode() {
        return zipCode;
    }
}
