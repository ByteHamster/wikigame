package com.bytehamster.wikipediagame;

import android.graphics.Bitmap;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ActivityMain extends AppCompatActivity {

    enum State {
        RANDOM, SELECT, SELECTED, FIND, FOUND
    }
    State state = State.SELECT;
    String findURL = "";
    int steps = -2;
    WebView wv;
    final String RANDOM_ARTICLE = "https://de.m.wikipedia.org/wiki/Spezial:Zuf%C3%A4llige_Seite";
    String history = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        state = State.RANDOM;

        wv = (WebView) findViewById(R.id.webview);
        wv.loadUrl(RANDOM_ARTICLE);
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if(findURL.equals("") && !url.equals(RANDOM_ARTICLE)) {
                    findURL = url;
                }
                if(!url.contains("m.wikipedia.org")) {
                    Toast.makeText(getBaseContext(), "Externer Link blockiert", Toast.LENGTH_LONG).show();
                    wv.loadUrl(wv.getUrl());
                }
                if (state == State.RANDOM) {
                    state = State.SELECT;
                } else if (state == State.SELECT) {
                    state = State.SELECTED;
                } else if(state == State.SELECTED && !url.equals(findURL)) {
                    wv.loadUrl(findURL);
                    Toast.makeText(getBaseContext(), "Starten bitte", Toast.LENGTH_LONG).show();
                } else if(state == State.SELECTED) {
                    //ignore
                } else if(state == State.FOUND && !url.equals(findURL)) {
                    Toast.makeText(getBaseContext(), "Bereits gewonnen", Toast.LENGTH_LONG).show();
                } else if(url.equals(findURL)) {
                    state = State.FOUND;
                    AlertDialog.Builder b = new AlertDialog.Builder(ActivityMain.this);
                    b.setMessage("Gefunden in " + steps + " Schritten.\n\n\nSCHRITTE:\n\n" + history);
                    b.show();
                } else {
                    steps++;
                    getSupportActionBar().setTitle("Wikipedia Game (" + steps + ")");
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.loadUrl("javascript:$(\".header-container\").hide(0);");

                if(!view.getTitle().equals(RANDOM_ARTICLE) && !view.getTitle().equals(findURL)) {
                    history += view.getTitle() + "\n\n";
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
}
