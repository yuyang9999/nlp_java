package baidu;

import com.baidu.aip.nlp.AipNlp;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuyang on 9/10/17.
 */

public class NlpService {
    public static String accessToken;

    private static String appId = "";
    private static String appKey = "";
    private static String appToken = "";

    static {
        //access token file not exists
        InputStream inputStream = NlpService.class.getResourceAsStream("/service.config");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line = "";
            appId = reader.readLine();
            appKey = reader.readLine();
            appToken = reader.readLine();

        } catch (IOException e) {
            System.out.println(e);
        }


    }

    public static void sentimentAnalysis1(String sentence) {
        AipNlp nlp = new AipNlp(appId, appKey, appToken);
        JSONObject ret = nlp.sentimentClassify(sentence);
        System.out.println(ret);
    }

    public static void dependencyParser(String sentence) {
        AipNlp nlp = new AipNlp(appId, appKey, appToken);
        JSONObject ret = nlp.depParser(sentence, null);
        System.out.println(ret);
    }
}
