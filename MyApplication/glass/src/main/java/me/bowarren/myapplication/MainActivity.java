package me.bowarren.myapplication;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p/>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class MainActivity extends Activity {
    Firebase myFirebaseRef;
    private List<CardBuilder> mCards;


    /**
     * {@link CardScrollView} to use as the main content view.
     */
    private CardScrollView mCardScroller;
    private CardScrollAdapter mCardScrollAdapter;

    /**
     * "Hello World!" {@link View} generated by {@link #buildView()}.
     */
    private View mView;


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Firebase.setAndroidContext(getApplicationContext());

        mCards = new ArrayList<CardBuilder>();
        mCards.add(new CardBuilder(getApplicationContext(), CardBuilder.Layout.TEXT)
                .setText("something")
                .setFootnote("I'm the footer!"));
        mCardScrollAdapter = new CardScrollAdapter() {
            @Override
            public int getCount() {
                return mCards.size();
            }

            @Override
            public Object getItem(int position) {
                return mCards.get(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mCards.get(position).getView(convertView, parent);
            }

            @Override
            public int getPosition(Object item) {
                return mCards.indexOf(item);
                //return AdapterView.INVALID_POSITION;
            }
        };


        Log.e("myapp", "in the beginning...");
        Firebase.setAndroidContext(this);
        myFirebaseRef = new Firebase("https://live-captioning.firebaseio.com");
        myFirebaseRef.child("new_posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                HashMap<String,String> newest = getLatestResponse(snapshot.getValue());
                if(newest == null)
                    return;
                String matches = newest.get("matches");
                String first_match;
                if(matches.indexOf(',') != -1)
                    first_match = matches.substring(1, matches.indexOf(','));
                else
                    first_match = matches.substring(1,matches.length()-1);

                Log.e("aaa", first_match);

                mCards.get(0).setText(first_match);
//                mCards.add(new CardBuilder(getApplicationContext(), CardBuilder.Layout.TEXT)
//                        .setText(first_match)
//                        .setFootnote("I'm the footer!"));
                mCardScrollAdapter.notifyDataSetChanged();

                archiveToFirebase(newest);

            }
            @Override public void onCancelled(FirebaseError error) { }
        });




        mView = buildView();

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(mCardScrollAdapter);
        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Plays disallowed sound to indicate that TAP actions are not supported.
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.DISALLOWED);
            }
        });

        mCardScroller.activate();
        setContentView(mCardScroller);


    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link CardBuilder} class.
     */
    private View buildView() {
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);

        //card.setText(R.string.hello_world);
        return card.getView();
    }
    

    private void archiveToFirebase(HashMap<String,String> post){
        Firebase postRef = myFirebaseRef.child("post_archive");
        postRef.push().setValue(post);
        myFirebaseRef.child("new_posts").removeValue();
    }

    private HashMap<String,String> getLatestResponse(Object snapshotvalue){

        HashMap<String,HashMap<String,String>> h_snapshot = (HashMap<String,HashMap<String,String>>) snapshotvalue;
        HashMap<String,String> newest = null;

        if (h_snapshot == null)
                return null;


        for (Map.Entry<String, HashMap<String,String>> entry : h_snapshot.entrySet()){
            String key = entry.getKey();
            HashMap<String,String> post = entry.getValue();
            if(newest == null){
                newest = post;
            }
            else{
                if(Long.parseLong(post.get("time")) > Long.parseLong(newest.get("time"))){
                    newest = post;
                }
            }
        }

        return newest;
    }
}
