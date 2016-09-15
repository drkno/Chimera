package nz.co.makereti.chimera.garage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class GarageDoorControl {

    public static GarageApi get(Context context) {
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
        return retrofit.create(GarageApi.class);
    }
}
