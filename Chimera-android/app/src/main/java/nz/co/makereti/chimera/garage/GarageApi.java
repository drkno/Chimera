package nz.co.makereti.chimera.garage;

import retrofit2.Call;
import retrofit2.http.GET;

public interface GarageApi {
    @GET("api/open")
    Call<ApiResult> open();
    @GET("api/close")
    Call<ApiResult> close();
    @GET("api/door")
    Call<ApiResult> door();
}
