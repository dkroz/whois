package com.dkroz.whois;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.apache.http.protocol.HTTP;


public class RootActivity extends Activity {
    private static final String TAG = "RootActivity";

    private RelativeLayout searchBlock;
    private EditText searchBox;
    private TextView searchResult;
    private ProgressBar searchProgress;
    private RelativeLayout searchResultIcons;

    private Tracker myTracker;
    private AdView mAdView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);

        initTracker();
        sendView("Home");

        searchBlock = (RelativeLayout)findViewById(R.id.root_search_block);
        searchBox = (EditText)findViewById(R.id.root_search_box);
        final ImageView searchIcon = (ImageView)findViewById(R.id.root_ic_search);
        final ImageView clearIcon = (ImageView)findViewById(R.id.root_ic_clear);
        searchProgress = (ProgressBar)findViewById(R.id.root_ic_progress);
        searchResult = (TextView)findViewById(R.id.root_search_result);
        searchResultIcons = (RelativeLayout)findViewById(R.id.root_ic_result_block);

        searchBox.setImeActionLabel("Lookup", KeyEvent.KEYCODE_ENTER);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                searchIcon.setVisibility(s.length()==0 ? View.GONE : View.VISIBLE);
                clearIcon.setVisibility(s.length()==0 ? View.GONE : View.VISIBLE);
            }
        });
        searchBox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction()==KeyEvent.ACTION_DOWN) {
                        startLookup();
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });

        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLookup();
            }
        });

        clearIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBox.setText("");
            }
        });

        searchResult.setOnTouchListener(new View.OnTouchListener() {
            final float step = 200;
            float mRatio = 1.0f;
            int mBaseDist;
            float mBaseRatio;
            float fontSize = 12;

            int getDistance(MotionEvent event) {
                int dx = (int) (event.getX(0) - event.getX(1));
                int dy = (int) (event.getY(0) - event.getY(1));
                return (int) (Math.sqrt(dx * dx + dy * dy));
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 2) {
                    int action = event.getAction() & MotionEvent.ACTION_MASK;
                    if (action == MotionEvent.ACTION_POINTER_DOWN) {
                        mBaseDist = getDistance(event);
                        mBaseRatio = mRatio;
                    } else {
                        float delta = (getDistance(event) - mBaseDist) / step;
                        float multi = (float) Math.pow(2, delta);
                        mRatio = Math.min(1024.0f, Math.max(0.1f, mBaseRatio * multi));
                        searchResult.setTextSize(mRatio + fontSize);
                    }
                }
                return true;
            }
        });

        findViewById(R.id.root_ic_result_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                //ClipData clipData = ClipData.newPlainText(searchBox.getText().toString(), );
                clipboardManager.setText(searchResult.getText().toString());
                Toast.makeText(getApplicationContext(), "Result was copied to clipboard", Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.root_ic_result_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchResult.setText("");
                moveSearchBlock(Move.DOWN);
            }
        });

        // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
        // values/strings.xml.
        mAdView = (AdView) findViewById(R.id.adView);

        // Create an ad request. Check logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        AdRequest adRequest = new AdRequest.Builder()
                //.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);

    }

    @Override
    protected void onResume() {
        if (searchResult.getText().toString().length()>0) {
            moveSearchBlock(Move.UP);
        }
        if (mAdView != null) {
            mAdView.resume();
        }
        super.onResume();
        //GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
        //GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    private void startLookup() {
        searchProgress.setVisibility(View.VISIBLE);
        searchBox.setText(searchBox.getText().toString().trim().replace(" ", ""));
        MyIntentService.lookup(RootActivity.this,
                searchBox.getText().toString(),
                new Messenger(new ServiceHandler()));
    }

    public class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            if (message!=null) {
                Bundle extras = message.getData();
                if (extras!=null && extras.containsKey("action")) {
                    if (extras.getString("action").equals(MyIntentService.ACTION_LOOKUP)) {
                        String result = extras.getString("result");
                        if (result!=null) {
                            if (result.contains("<h1>Error:</h1>")) {
                                result = result.substring(result.indexOf("<p>") + 3,
                                        result.indexOf("</p>"));
                            } else {
                                result = result.substring(result.indexOf("<pre class='content'>") + 21,
                                        result.indexOf("</pre>"));
                            }
                        } else {
                            result = "Error has occurred...";
                        }
                        searchResult.setText(result);
                        moveSearchBlock(Move.UP);
                        closeKeyboard();
                        searchProgress.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private enum Move {UP, DOWN}
    private void moveSearchBlock(Move move) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)searchBlock.getLayoutParams();
        switch (move) {
            case UP:
                params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
                findViewById(R.id.root_search_scroll).setVisibility(View.VISIBLE);
                sendView("Result");
                break;
            case DOWN:
                params.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                findViewById(R.id.root_search_scroll).setVisibility(View.GONE);
                sendView("Home");
                break;
        }
        searchBlock.setLayoutParams(params);
    }

    private void closeKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                (null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }


    /*************************
     * Google Analytics part *
     *************************/
    synchronized void initTracker() {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        myTracker = analytics.newTracker(R.xml.app_tracker);
        myTracker.enableAdvertisingIdCollection(true);
        myTracker.enableExceptionReporting(true);

        //return t;
    }

    synchronized void sendView(String name) {
        if (myTracker==null) {
            initTracker();
        }
        try {
            myTracker.setScreenName(name);
            myTracker.send(new HitBuilders.AppViewBuilder().build());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.root, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
*/

}
