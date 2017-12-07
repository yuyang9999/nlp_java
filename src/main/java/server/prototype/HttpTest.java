package server.prototype;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;

import java.io.IOException;

/**
 * Created by yuyang on 7/12/17.
 */
public class HttpTest {
    private static class TokenInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            Response originalResponse = chain.proceed(request);

            String responseBody = originalResponse.body().toString();
            if (responseBody != null) {

            }

            return originalResponse;
        }
    }

    public static void main(String[] args) {
        String baseURL = "";

        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(new TokenInterceptor());
        Retrofit retrofit = new Retrofit.Builder().baseUrl(baseURL)
                .client(client)
                .build();
    }

}
