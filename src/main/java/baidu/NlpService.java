package baidu;

import com.baidu.aip.nlp.AipNlp;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.map.util.JSONPObject;
import org.json.JSONObject;
import org.springframework.util.StringUtils;
import sun.net.www.http.HttpClient;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuyang on 9/10/17.
 */

public class NlpService {
    public static String accessToken;

    static {
        //load the access token from file if exists
        try {
            FileReader fileReader = new FileReader("/tmp/baidu_crediential/access_token.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            accessToken = bufferedReader.readLine();
        } catch(IOException e) {
            //access token file not exists
            InputStream inputStream = NlpService.class.getResourceAsStream("/baidu_services.config");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String clientId = reader.readLine();
                String secret = reader.readLine();
                URL url = new URL("https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&" +
                        "client_id=" + clientId + "&client_secret=" + secret);
                URLConnection connection = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                inputLine = in.readLine();

                JSONObject jsonObject = new JSONObject(inputLine);
                System.out.println(jsonObject);
                accessToken = (String)jsonObject.get("access_token");
                //write to the file
                FileWriter fw = new FileWriter("/tmp/baidu_crediential/access_token.txt");
                fw.write(accessToken);
                fw.close();

                in.close();

            } catch (IOException ee) {
                System.out.println(ee);
            }

        }
    }


    public static void sentimentAnalysis(String setence) throws Exception {
        String url = "https://aip.baidubce.com/rpc/2.0/nlp/v1/sentiment_classify?access_token=" + accessToken;
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("text", setence);
        JSONObject obj = new JSONObject(parameters);
        String content = obj.toString();

        StringEntity entity = new StringEntity(content);

        httpPost.setEntity(entity);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Content-Encoding", "GBK");

        CloseableHttpResponse response = client.execute(httpPost);
        HttpEntity entity1 =  response.getEntity();
        System.out.println(entity1);
        InputStream inputStream = entity1.getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String respStr = reader.readLine();
        JSONObject respObj = new JSONObject(respStr);
        System.out.println(respObj);
        if (respObj.has("items")) {
            System.out.println(respObj.get("items"));
        } else {
            System.out.println("error");
        }
    }

    public static void sentimentAnalysis1(String sentence) {
        AipNlp nlp = new AipNlp("10217961", "NDjYLBttRsq730a47j38FWMM", "QR66hFds8x7CSWfLPPWPSMpxpIIQ52gq");
        JSONObject ret = nlp.sentimentClassify(sentence);
        System.out.println(ret);
    }

}
