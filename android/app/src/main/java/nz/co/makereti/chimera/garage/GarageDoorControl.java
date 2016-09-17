package nz.co.makereti.chimera.garage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class GarageDoorControl {
    private static IGarageApi get(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String hostname = sharedPreferences.getString("garageServerHostname", "");
        int port = Integer.parseInt(sharedPreferences.getString("garageServerPort", ""));
        String url = "http://" + hostname + ":" + Integer.toString(port) + "/";

        String username = sharedPreferences.getString("garageUsername", "");
        String password = sharedPreferences.getString("garagePassword", "");

        final String userAndPassword = username + ":" + password;
        final String basic = "Basic " + Base64.encodeToString(userAndPassword.getBytes(), Base64.NO_WRAP);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request().newBuilder().addHeader("Authorization", basic).build();
                return chain.proceed(request);
            }
        });
        final OkHttpClient httpClient = builder.build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();
        return retrofit.create(IGarageApi.class);
    }

    public static void openDoor(final Context context) {
        Log.d("ControlActivity", "Open Door");
        Call<ApiResult> result = GarageDoorControl.get(context).open();
        result.enqueue(new Callback<ApiResult>() {
            @Override
            public void onResponse(Call<ApiResult> call, retrofit2.Response<ApiResult> response) {
                Toast.makeText(context, "The garage door was opened for you.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<ApiResult> call, Throwable t) {
                Toast.makeText(context, "Hmmmm. The garage door refuses to open.", Toast.LENGTH_LONG).show();
                Log.wtf("ControlActivity", "Failed to open the door.", t);
            }
        });
    }

    public static void closeDoor(final Context context) {
        Log.d("ControlActivity", "Close Door");
        Call<ApiResult> result = GarageDoorControl.get(context).close();
        result.enqueue(new Callback<ApiResult>() {
            @Override
            public void onResponse(Call<ApiResult> call, retrofit2.Response<ApiResult> response) {
                Toast.makeText(context, "The garage door was closed for you.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<ApiResult> call, Throwable t) {
                Toast.makeText(context, "Hmmmm. The garage door refuses to close.", Toast.LENGTH_LONG).show();
                Log.wtf("ControlActivity", "Failed to close the door.", t);
            }
        });
    }

    public static void getDoorStatus(final Context context, final IGarageStatus garageStatus) {
        Call<ApiResult> result = GarageDoorControl.get(context).door();
        result.enqueue(new Callback<ApiResult>() {
            @Override
            public void onResponse(Call<ApiResult> call, retrofit2.Response<ApiResult> response) {
                garageStatus.onDoorStatus(DoorStatus.valueOf(response.body().state));
            }

            @Override
            public void onFailure(Call<ApiResult> call, Throwable t) {
                garageStatus.onDoorStatus(DoorStatus.Closed);
            }
        });
    }
}
