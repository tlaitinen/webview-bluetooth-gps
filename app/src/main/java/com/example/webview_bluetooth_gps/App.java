package com.example.webview_bluetooth_gps;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.TimeZone;

import android.app.Application;
import android.content.Intent;

import android.util.Log;

import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.GSTSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.SentenceValidator;
import net.sf.marineapi.nmea.util.Position;

import org.json.JSONArray;
import org.json.JSONObject;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;


public class App extends Application {

    public static App appInstance;

    private BluetoothSPP bt;
    private PipedOutputStream pipedOut;
    private static TimeZone tz = TimeZone.getTimeZone("UTC");
    private PipedInputStream pipedIn;
    private JSONArray positions;
    private synchronized void addPosition(JSONObject p) {
        positions.put(p);
    }
    public synchronized JSONArray getPositions() {
        JSONArray ps = positions;
        positions = new JSONArray();
        return ps;
    }

    public class MultiSentenceListener implements SentenceListener {

        private double accuracy = 1000;
        private double altitudeAccuracy = 1000;
        public MultiSentenceListener() {
        }
        @Override
        public void readingPaused() {
        }
        @Override
        public void readingStarted() {
        }
        @Override
        public void readingStopped() {
        }
        @Override
        public void sentenceRead(SentenceEvent event) {
            Sentence s = event.getSentence();
            try {
                if ("GST".equals(s.getSentenceId())) {
                    GSTSentence gst = (GSTSentence) s;
                    accuracy = gst.getErrorEllipseSemiMajorAxis1SigmaError();
                    altitudeAccuracy = gst.getHeight1SigmaError();

                } else if ("GGA".equals(s.getSentenceId())) {
                    GGASentence gga = (GGASentence) s;


                        JSONObject o = new JSONObject();
                        Position p = gga.getPosition();
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                        df.setTimeZone(tz);

                        o.put("longitude", p.getLongitude());
                        o.put("latitude", p.getLatitude());
                        o.put("altitude", p.getAltitude());
                        o.put("accuracy", accuracy);
                        o.put("altitudeAccuracy", altitudeAccuracy);
                        o.put("fixQuality", gga.getFixQuality().toString());
                        o.put("time", df.format(new Date()));
                        App.this.addPosition(o);
                }
            } catch (Exception e) {
                Log.e("Example", e.toString());
            }
        }
    }

    public void connectBluetooth(Intent data) {
        bt.connect(data);

    }
    public void enableBluetooth() {
        if (!bt.isBluetoothEnabled()) {
            bt.enable();
        }
        bt.setupService();
        bt.startService(BluetoothState.DEVICE_OTHER);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        bt = new BluetoothSPP(this);
        enableBluetooth();
        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                Log.d("Example", message);
            }
        });

        pipedOut = new PipedOutputStream();
        pipedIn = new PipedInputStream();
        positions = new JSONArray();
        try {
            pipedIn.connect(pipedOut);
        } catch (IOException e) {
            Log.e("Example", e.toString());
        }
        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                if (SentenceValidator.isValid(message)) {
                    try {
                        pipedOut.write((message + "\r\n").getBytes("UTF-8"));
                    } catch (IOException e) {
                        Log.e("Example", e.toString());
                    }
                }
            }
        });

        SentenceReader sentenceReader = new SentenceReader(pipedIn);
        sentenceReader.addSentenceListener(new MultiSentenceListener());
        sentenceReader.start();

        appInstance = this;

    }




}

