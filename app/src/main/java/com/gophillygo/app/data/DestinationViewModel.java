package com.gophillygo.app.data;

import android.arch.lifecycle.LiveData;

import com.gophillygo.app.data.models.Destination;
import com.gophillygo.app.data.models.DestinationInfo;
import com.gophillygo.app.data.models.Filter;
import com.gophillygo.app.data.networkresource.Resource;

import java.util.List;

import javax.inject.Inject;


public class DestinationViewModel extends AttractionViewModel {
    private final LiveData<Resource<List<DestinationInfo>>> destinations;

    @Inject
    public DestinationViewModel(DestinationRepository destinationRepository) {
        super(destinationRepository);
        destinations = destinationRepository.loadDestinations();
    }

    public LiveData<DestinationInfo> getDestination(long destinationId) {
        return destinationRepository.getDestination(destinationId);
    }

    public void updateDestination(Destination destination) {
        destinationRepository.updateDestination(destination);
    }

    public void updateMultipleDestinations(List<Destination> destinations) {
        destinationRepository.updateMultipleDestinations(destinations);
    }

    public LiveData<Resource<List<DestinationInfo>>> getDestinations() {
        return destinations;
    }

    public LiveData<Resource<List<DestinationInfo>>> getFilteredDestinations(Filter filter) {
        return destinationRepository.loadDestinations(filter);
    }
}
