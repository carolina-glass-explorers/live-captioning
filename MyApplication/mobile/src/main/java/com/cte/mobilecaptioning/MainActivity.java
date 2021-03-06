package com.cte.mobilecaptioning;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.client.Firebase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public boolean go = true;

    Firebase myFirebaseRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);

        Firebase.setAndroidContext(this);
        myFirebaseRef = new Firebase("https://live-captioning.firebaseio.com");


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mute();
                recognizeSpeech();
                Log.e("fff", "after starting recog");

            }
        });
    }

    @Override
    public void onPause(){
        super.onPause();
        unmute();
    }

    @Override
    public void onResume(){
        super.onResume();
        mute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void recognizeSpeech(){
        Log.e("fff", "in recognize speech");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, new Long(15000));
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, new Long(15000));
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, new Long(15000));
        SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        recognizer.setRecognitionListener(new VoiceRecognitionListener(recognizer, intent));
        recognizer.startListening(intent);
    }

    class VoiceRecognitionListener implements RecognitionListener {
        //VoiceRecognition mVoiceRecognition;
        //private static final String TAG = "VoiceRecognitionListener";
        SpeechRecognizer recognizer;
        Intent intent;

        public VoiceRecognitionListener(SpeechRecognizer recognizer, Intent intent){
            this.recognizer = recognizer;
            this.intent = intent;
        }

        public void onResults(Bundle data) {
            ArrayList<String> matches = data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.e("ffff", matches.toString());
            sendToFirebase(matches.toString());
            recognizeSpeech();
        }


        String TAG = "FFfff";
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
//            mVoiceRecognition.mText.setText("Sounding good!");

        }
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived");
        }

        public void onEndOfSpeech() {
            Log.d(TAG, "onEndofSpeech");
//            mVoiceRecognition.mText.setText("Waiting for result...");
//            recognizer.stopListening();
//            recognizeSpeech();


        }
        public void onError(int error) {
            Log.d(TAG, "error " + error);
            //error 6 = speech timeout, so start it on that again
            if(error == recognizer.ERROR_SPEECH_TIMEOUT || error == recognizer.ERROR_NO_MATCH){
                recognizeSpeech();
            }
        }

        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent " + eventType);
        }
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults");
        }
        public void onReadyForSpeech(Bundle params) {
//            Log.d(TAG, "onReadyForSpeech");
        }
        public void onRmsChanged(float rmsdB) {
//            Log.d(TAG, "onRmsChanged");
        }
    }

    private void sendToFirebase(String matches){
        Firebase postRef = myFirebaseRef.child("new_posts");
        Map<String, String> post = new HashMap<String, String>();
        post.put("matches", matches);
        post.put("time", ((Long) System.currentTimeMillis()).toString() );
        postRef.push().setValue(post);
    }


    private void mute(){
        //mute audio

        AudioManager amanager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
        amanager.setStreamMute(AudioManager.STREAM_ALARM, true);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        amanager.setStreamMute(AudioManager.STREAM_RING, true);
        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
    }
    private void unmute(){
        AudioManager amanager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);

        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
        amanager.setStreamMute(AudioManager.STREAM_ALARM, false);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        amanager.setStreamMute(AudioManager.STREAM_RING, false);
        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
    }
}
