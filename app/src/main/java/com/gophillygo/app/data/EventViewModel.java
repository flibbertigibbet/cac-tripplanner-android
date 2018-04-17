package com.gophillygo.app.data;

import android.arch.lifecycle.LiveData;

import com.gophillygo.app.data.models.Event;
import com.gophillygo.app.data.models.EventInfo;
import com.gophillygo.app.data.networkresource.Resource;

import java.util.List;

import javax.inject.Inject;


public class EventViewModel extends AttractionViewModel {

    private final LiveData<Resource<List<EventInfo>>> events;

    @Inject
    public EventViewModel(DestinationRepository destinationRepository) {
        super(destinationRepository);
        events = destinationRepository.loadEvents();
    }

    public LiveData<EventInfo> getEvent(long eventId) {
        return destinationRepository.getEvent(eventId);
    }

    public void updateEvent(Event event) {
        destinationRepository.updateEvent(event);
    }

    public void updateMultipleEvents(List<Event> events) {
        destinationRepository.updateMultipleEvents(events);
    }

    public LiveData<Resource<List<EventInfo>>> getEvents() {
        return events;
    }
}
