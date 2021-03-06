package org.gophillygo.app.data;

import androidx.lifecycle.LiveData;

import org.gophillygo.app.data.models.DestinationQueryResponse;
import org.gophillygo.app.data.models.UserFlagPost;
import org.gophillygo.app.data.models.UserFlagPostResponse;
import org.gophillygo.app.data.networkresource.ApiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Use Retrofit to query the server API for destinations.
 */

public interface DestinationWebservice {

    // base URL; should end in a trailing slash
    String WEBSERVICE_URL = "https://gophillygo.org/";


    @GET("api/destinations/search?text=")
    LiveData<ApiResponse<DestinationQueryResponse>> getDestinations();

    @POST("api/user_flag/")
    Call<UserFlagPostResponse> postUserFlag(@Body UserFlagPost post);
}
