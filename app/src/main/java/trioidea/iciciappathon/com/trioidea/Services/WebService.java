package trioidea.iciciappathon.com.trioidea.Services;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by pritesh.gandhi on 6/7/16.
 * Base web service
 */
public class WebService {
    public static String getJSON(String url) {
        HttpURLConnection c = null;
        try {

            Log.d("API", url);
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            c.connect();
            int status = c.getResponseCode();

            /*switch (status) {
                case 200:
                case 201:*/
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString().replaceAll(" ", "");//.replace("[","").replace("]","");


        } catch (MalformedURLException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {

                }
            }
        }
    }

    public static String getFlipkartJSON(String url) {
        HttpURLConnection c = null;
        try {

            Log.d("API", url);
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.addRequestProperty("Fk-Affiliate-Id","sandeshba1");
            c.addRequestProperty("Fk-Affiliate-Token","a4a490d9ad4e4d479c44a0aa9d23ea37");
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            c.connect();
            int status = c.getResponseCode();

            /*switch (status) {
                case 200:
                case 201:*/
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString().replaceAll(" ", "");//.replace("[","").replace("]","");


        } catch (MalformedURLException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {

                }
            }
        }
    }
    public static String getXML(String url) {
        String line = null;

        try {

            HttpURLConnection httpClient = null;
            URL u = new URL(url);
            httpClient = (HttpURLConnection) u.openConnection();

            httpClient.setConnectTimeout(5000);
            httpClient.setReadTimeout(5000);
            httpClient.connect();
            BufferedReader br = new BufferedReader(new InputStreamReader(httpClient.getInputStream()));
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            line= sb.toString();
        } catch (Exception e) {
            line = "Internet Connection Error >> " + e.getMessage();
        }
    return line;
    }

}
