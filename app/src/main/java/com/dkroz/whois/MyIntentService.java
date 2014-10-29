package com.dkroz.whois;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class MyIntentService extends IntentService {
    private static final String TAG = "MyIntentService";

    public static final String ACTION_LOOKUP = "com.dkroz.whois.action.REQUEST";
    private static final String EXTRA_DOMAIN = "com.dkroz.whois.extra.DOMAIN";
    private static Messenger messenger;


    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void lookup(Context context, String domain, Messenger m) {
        Intent intent = new Intent(context, MyIntentService.class);
        intent.setAction(ACTION_LOOKUP);
        intent.putExtra(EXTRA_DOMAIN, domain);
        context.startService(intent);
        messenger = m;
    }

    public MyIntentService() { super("MyIntentService"); }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LOOKUP.equals(action)) {
                final String domain = intent.getStringExtra(EXTRA_DOMAIN);
                handleActionLookup(domain);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionLookup(String domain) {
        /*try {
            URL url = new URL(String.format(getResources().getString(R.string.lookup_url), domain));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String result = Utils.convertStreamToString(in);
                Log.i(TAG, result);
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }*/
        Bundle bundle = new Bundle();
        bundle.putString("action", ACTION_LOOKUP);
        try {
            HttpClient httpClient = MyHttpClient.getInstance();
            HttpGet httpGet = new HttpGet(String.format(getResources().getString(R.string.lookup_url), domain));
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode()==200) {
                String result = new String(EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8));
                bundle.putString("result", result);
            } else {
                bundle.putString("result", "Unable to perform request");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Message message = Message.obtain();
            message.setData(bundle);
            try {
                messenger.send(message);
            } catch (RemoteException re) {

            }
        }
    }

}