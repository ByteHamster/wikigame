package com.bytehamster.wikigame;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class ActivityMain extends AppCompatActivity {

    enum State {
        RANDOM, SELECT, SELECTED, FIND, FOUND
    }
    State state = State.SELECT;
    String findURL = "";
    int steps = -2;
    WebView wv;
    String RANDOM_ARTICLE = "";
    String history = "";
    final String magicNumber = "Δ";
    boolean shouldAppendHistory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RANDOM_ARTICLE = getString(R.string.random_article);
        wv = (WebView) findViewById(R.id.webview);

        String action = getIntent().getAction();
        if (Intent.ACTION_VIEW.equals(action)){
            Uri uri = getIntent().getData();
            String status = uri.toString().replace("wikigame://", "");
            deserializeState(status);
        } else if (savedInstanceState != null) {
            steps = savedInstanceState.getInt("steps");
            history = savedInstanceState.getString("history");
            findURL = savedInstanceState.getString("findURL");
            wv.loadUrl(savedInstanceState.getString("current"));
            state = toState(savedInstanceState.getInt("state"));

            if (state == State.FIND) {
                findViewById(R.id.fab).setVisibility(View.GONE);
                getSupportActionBar().setTitle(getString(R.string.app_name) + " (" + steps + ")");
                steps--;
            }
        } else {
            state = State.RANDOM;
            wv.loadUrl(RANDOM_ARTICLE);
        }

        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        wv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        wv.setLongClickable(false);

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                wv.setEnabled(false);
                wv.setAlpha(0.2f);

                if(findURL.equals("") && !url.equals(RANDOM_ARTICLE)) {
                    findURL = url;
                }

                if (state == State.RANDOM) {
                    state = State.SELECT;
                } else if (state == State.SELECT) {
                    state = State.SELECTED;
                } else if(state == State.SELECTED) {
                    //ignore
                } else if(url.equals(findURL)) {
                    steps++;
                    state = State.FOUND;
                    shouldAppendHistory = true;
                } else {
                    steps++;
                    shouldAppendHistory = true;
                    getSupportActionBar().setTitle(getString(R.string.app_name) + " (" + steps + ")");
                }

                if (url.equals(wv.getUrl())) {
                    shouldAppendHistory = false;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(!url.contains("m.wikipedia.org")) {
                    Toast.makeText(getBaseContext(), R.string.blocked_external, Toast.LENGTH_LONG).show();
                    return true;
                }
                if(state == State.SELECTED && !url.equals(findURL)) {
                    Toast.makeText(getBaseContext(), R.string.please_start, Toast.LENGTH_LONG).show();
                    return true;
                } else if(state == State.FOUND && !url.equals(findURL)) {
                    Toast.makeText(getBaseContext(), R.string.already_won, Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.loadUrl("javascript:$(\".header-container\").hide(0);");
                findViewById(R.id.progressBar).setVisibility(View.GONE);
                wv.setEnabled(true);
                wv.setAlpha(1.0f);

                if(!view.getTitle().equals(RANDOM_ARTICLE) && !url.equals(findURL) && shouldAppendHistory) {
                    history += view.getTitle().replace(" – Wikipedia", "") + "\n";
                    shouldAppendHistory = false;
                } else if(state == State.FOUND && shouldAppendHistory) {
                    shouldAppendHistory = false;
                    history += view.getTitle().replace(" – Wikipedia", "") + "\n";
                    AlertDialog.Builder b = new AlertDialog.Builder(ActivityMain.this);
                    b.setTitle(getString(R.string.won_title, steps));
                    b.setMessage(history);
                    b.setCancelable(false);
                    b.setNegativeButton(android.R.string.ok, null);
                    b.setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String shareBody = getString(R.string.share_body, steps, history);
                            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                            sharingIntent.setType("text/plain");
                            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                            startActivity(Intent.createChooser(sharingIntent, getString(R.string.share)));
                        }
                    });
                    b.show();
                }
            }
        });

        final FloatingActionButton b = (FloatingActionButton) findViewById(R.id.fab);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wv.loadUrl(RANDOM_ARTICLE);
                state = State.FIND;
                b.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.restart) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setMessage(R.string.restart_warning);
            b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    state = State.RANDOM;
                    history = "";
                    findURL = "";
                    steps = -2;
                    shouldAppendHistory = false;
                    getSupportActionBar().setTitle(getString(R.string.app_name));
                    wv.loadUrl(RANDOM_ARTICLE);
                    findViewById(R.id.fab).setVisibility(View.VISIBLE);
                }
            });
            b.setNegativeButton(android.R.string.cancel, null);
            b.show();
            return true;
        } else if(item.getItemId() == R.id.goal) {
            AlertDialog.Builder b = new AlertDialog.Builder(ActivityMain.this);
            b.setTitle(R.string.target);
            final WebView v = new WebView(this);
            v.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Toast.makeText(getBaseContext(), R.string.blocked_click, Toast.LENGTH_LONG).show();
                    return true;
                }
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    view.loadUrl("javascript:$(\".header-container\").hide(0);");
                }
            });
            v.loadUrl(findURL);
            b.setView(v);
            b.show();
            return true;
        } else if (item.getItemId() == R.id.send) {
            if(state != State.FIND) {
                Toast.makeText(this, R.string.please_start, Toast.LENGTH_SHORT).show();
                return true;
            }

            try {
                Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.putExtra("ENCODE_FORMAT", "QR_CODE");
                intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
                intent.putExtra("ENCODE_DATA", "wikigame://" + serializeState());
                intent.putExtra("ENCODE_SHOW_CONTENTS", false);
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_qr, Toast.LENGTH_SHORT).show();
            }
        } else if (item.getItemId() == R.id.history) {
            AlertDialog.Builder b = new AlertDialog.Builder(ActivityMain.this);
            b.setTitle(getString(R.string.history));
            b.setMessage(history);
            b.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setMessage(R.string.close_warning);
        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        b.setNegativeButton(android.R.string.cancel, null);
        b.show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("state", toInt(state));
        savedInstanceState.putInt("steps", steps);
        savedInstanceState.putString("current", wv.getUrl());
        savedInstanceState.putString("history", history);
        savedInstanceState.putString("findURL", findURL);
        super.onSaveInstanceState(savedInstanceState);
    }

    String serializeState() {
        String str = steps + magicNumber + toInt(state) + magicNumber + wv.getUrl() + magicNumber
                + history + magicNumber + findURL + magicNumber;
        try {
            return Base64.encodeToString(str.getBytes("UTF-8"), Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            return "error";
        }
    }

    void deserializeState(String string) {
        try {
            string = new String(Base64.decode(string, Base64.DEFAULT), "UTF-8");
            String data[] = string.split(magicNumber);
            steps = Integer.valueOf(data[0]);
            state = toState(Integer.valueOf(data[1]));
            wv.loadUrl(data[2]);
            history = data[3];
            findURL = data[4];

            if (state == State.FIND) {
                findViewById(R.id.fab).setVisibility(View.GONE);
                getSupportActionBar().setTitle(getString(R.string.app_name) + " (" + steps + ")");
                steps--;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private int toInt(State state) {
        switch (state) {
            case FIND:
                return 0;
            case FOUND:
                return 1;
            case RANDOM:
                return 2;
            case SELECT:
                return 3;
            case SELECTED:
                return 4;
            default:
                return 5;
        }
    }

    private State toState(int state) {
        switch (state) {
            case 0:
                return State.FIND;
            case 1:
                return State.FOUND;
            case 2:
                return State.RANDOM;
            case 3:
                return State.SELECT;
            case 4:
                return State.SELECTED;
            default:
                return State.RANDOM;
        }
    }
}
