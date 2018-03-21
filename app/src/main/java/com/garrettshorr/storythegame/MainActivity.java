package com.garrettshorr.storythegame;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "MainActivity";

    // Client used to sign in with Google APIs
    private GoogleSignInClient googleSignInClient = null;

    // Client used to interact with the TurnBasedMultiplayer system.
    private TurnBasedMultiplayerClient turnBasedMultiplayerClient = null;

    // Client used to interact with the Invitation system.
    private InvitationsClient invitationsClient = null;

    //buttons, textViews, listView
    private Button signOutButton, startMatchButton, existingStoriesButton, finishButton;
    private SignInButton signInButton;
    private TextView storyTextView;
    private ListView availableWordList;
    private ArrayAdapter<String> adapter;
    private List<String> words;


    //for intents
    private static final int RC_SIGN_IN = 9001;
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_LOOK_AT_MATCHES = 10001;

    // Should I be showing the turn API?
    public boolean isDoingTurn = false;

    // This is the current match we're in; null if not loaded
    public TurnBasedMatch match;

    // This is the current match data after being unpersisted.
    // Do not retain references to match data once you have
    // taken an action on the match, such as takeTurn()
    private StoryTurn turnData;

    private AlertDialog alertDialog;

    private String displayName, playerId;
    private int wordsLeft;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        wireWidgets();
        setListeners();

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, new ArrayList<String>());
        availableWordList.setAdapter(adapter);
        availableWordList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                selectWord(pos);
            }
        });


        //// Create the Google API Client with access to Games
        // Create the client used to sign in.
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);


    }

    private void selectWord(int pos) {
        if(pos < 4) {
            turnData.data += words.get(pos);
        } else if (pos < 7) {
            turnData.data += " " + words.get(pos);
        } else {
            turnData.data += " " + words.remove(pos);
            adapter.clear();
            adapter.addAll(words);
            adapter.notifyDataSetChanged();
            wordsLeft--;
        }
        storyTextView.setText(turnData.data);
        if(wordsLeft == 0) {
            availableWordList.setEnabled(false);
            turnIsOver();
        }
    }

    //Thank you internet
    public String inputStreamToString(InputStream inputStream) {
        try {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, bytes.length);
            String json = new String(bytes);
            return json;
        } catch (IOException e) {
            return null;
        }
    }

    private void setListeners() {
        signInButton.setOnClickListener(this);
        signOutButton.setOnClickListener(this);
        startMatchButton.setOnClickListener(this);
        existingStoriesButton.setOnClickListener(this);
        finishButton.setOnClickListener(this);
    }

    private void wireWidgets() {
        signInButton = (SignInButton) findViewById(R.id.button_sign_in);
        signOutButton = (Button) findViewById(R.id.button_sign_out);
        startMatchButton = (Button) findViewById(R.id.startMatchButton);
        existingStoriesButton = (Button) findViewById(R.id.checkGamesButton);
        storyTextView = (TextView) findViewById(R.id.textview_story);
        availableWordList = (ListView) findViewById(R.id.listview_words);
        finishButton = (Button) findViewById(R.id.button_finish);
    }

    /**
     * Start a sign in activity.  To properly handle the result, call tryHandleSignInResult from
     * your Activity's onActivityResult function
     */
    public void startSignInIntent() {
        //verifySampleSetup(this, R.string.app_id);

        startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }


    // This function is what gets called when you return from either the Play
    // Games built-in inbox, or else the create game built-in interface.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(intent);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                onConnected(account);
            } catch (ApiException apiException) {
                String message = apiException.getMessage();
                if (message == null || message.isEmpty()) {
                    message = "some sort of error"; //getString(R.string.signin_other_error);
                }

                onDisconnected();

                new AlertDialog.Builder(this)
                        .setMessage(message)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        }

        else if (requestCode == RC_LOOK_AT_MATCHES) {
            // Returning from the 'Select Match' dialog

            if (resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "onActivityResult: "+ requestCode + " " + resultCode + " " +
                        "User cancelled returning from the 'Select Match' dialog.");
                return;
            }
            TurnBasedMatch match = intent
                    .getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);

            if (match != null) {
                updateMatch(match);
            }

            Log.d(TAG, "Match = " + match);
        } else if (requestCode == RC_SELECT_PLAYERS) {
            // Returning from 'Select players to Invite' dialog

            if (resultCode != Activity.RESULT_OK) {
                // user canceled
                Log.d(TAG, "onActivityResult: "+ requestCode + " " + resultCode + " " +
                        "User cancelled returning from 'Select players to Invite' dialog");
                return;
            }

            // get the invitee list
            ArrayList<String> invitees = intent
                    .getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

            // get automatch criteria
            Bundle autoMatchCriteria;

            int minAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            int maxAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

            if (minAutoMatchPlayers > 0) {
                autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers,
                        maxAutoMatchPlayers, 0);
            } else {
                autoMatchCriteria = null;
            }

            TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                    .addInvitedPlayers(invitees)
                    .setAutoMatchCriteria(autoMatchCriteria).build();

            // Start the match
            turnBasedMultiplayerClient.createMatch(tbmc)
                    .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                        @Override
                        public void onSuccess(TurnBasedMatch turnBasedMatch) {
                            Toast.makeText(MainActivity.this, "match created", Toast.LENGTH_SHORT).show();
                            onInitiateMatch(turnBasedMatch);
                        }
                    })
                    .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
            //showSpinner();
        }
    }

    private void onInitiateMatch(TurnBasedMatch match) {
        //dismissSpinner();

        if (match.getData() != null) {
            // This is a game that has already started, so I'll just start
            updateMatch(match);
            return;
        }

        startMatch(match);
    }

    private void onDisconnected() {


        Log.d(TAG, "onDisconnected()");

        turnBasedMultiplayerClient = null;
        invitationsClient = null;

        setViewVisibility();


    }

    // Update the visibility based on what state we're in.
    public void setViewVisibility() {
        boolean isSignedIn = turnBasedMultiplayerClient != null;

        if (!isSignedIn) {
            findViewById(R.id.login_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.button_sign_in).setVisibility(View.VISIBLE);
            findViewById(R.id.matchup_layout).setVisibility(View.GONE);
            findViewById(R.id.gameplay_layout).setVisibility(View.GONE);

            if (alertDialog != null) {
                alertDialog.dismiss();
            }
            return;
        }


//        ((TextView) findViewById(R.id.name_field)).setText(mDisplayName);
        findViewById(R.id.login_layout).setVisibility(View.GONE);

        if (isDoingTurn) {
            findViewById(R.id.matchup_layout).setVisibility(View.GONE);
            findViewById(R.id.gameplay_layout).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.matchup_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.gameplay_layout).setVisibility(View.GONE);
        }
    }

    public static boolean verifySampleSetup(Activity activity, int... resIds) {
        StringBuilder problems = new StringBuilder();
        boolean problemFound = false;
        problems.append("The following set up problems were found:\n\n");

        // Did the developer forget to change the package name?
        if (activity.getPackageName().startsWith("com.google.example.games")) {
            problemFound = true;
            problems.append("- Package name cannot be com.google.*. You need to change the "
                    + "sample's package name to your own package.").append("\n");
        }

        for (int i : resIds) {
            if (activity.getString(i).toLowerCase().contains("replaceme")) {
                problemFound = true;
                problems.append("- You must replace all " +
                        "placeholder IDs in the ids.xml file by your project's IDs.").append("\n");
                break;
            }
        }

        if (problemFound) {
            problems.append("\n\nThese problems may prevent the app from working properly.");
            showAlert(activity, problems.toString());
            return false;
        }
        Log.d(TAG, "verifySampleSetup: No problems found");
        return true;
    }

    public static void showAlert(Activity activity, String message) {
        (new AlertDialog.Builder(activity)).setMessage(message)
                .setNeutralButton(android.R.string.ok, null).create().show();
    }

    private void onConnected(GoogleSignInAccount googleSignInAccount) {
        Log.d(TAG, "onConnected(): connected to Google APIs");

        turnBasedMultiplayerClient = Games.getTurnBasedMultiplayerClient(this, googleSignInAccount);
        invitationsClient = Games.getInvitationsClient(this, googleSignInAccount);
        Games.getPlayersClient(this, googleSignInAccount)
                .getCurrentPlayer()
                .addOnSuccessListener(
                        new OnSuccessListener<Player>() {
                            @Override
                            public void onSuccess(Player player) {
                                displayName = player.getDisplayName();
                                playerId = player.getPlayerId();
                                Toast.makeText(MainActivity.this, "SUCCESS!", Toast.LENGTH_SHORT).show();
                                setViewVisibility();
                            }
                        }
                )
                .addOnFailureListener(createFailureListener("There was a problem getting the player!"));

        Log.d(TAG, "onConnected(): Connection successful");
    }


    // This is a helper functio that will do all the setup to create a simple failure message.
    // Add it to any task and in the case of an failure, it will report the string in an alert
    // dialog.
    private OnFailureListener createFailureListener(final String string) {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: " + e.getMessage());
                ;
            }
        };
    }


    public void signOut() {
        Log.d(TAG, "signOut()");

        googleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {
                            Log.d(TAG, "signOut(): success");
                        } else {
                            //handleException(task.getException(), "signOut() failed!");
                            Log.d(TAG, "onComplete: signout failed!");
                            Toast.makeText(MainActivity.this, "Something went wrong",
                                    Toast.LENGTH_SHORT).show();
                        }

                        onDisconnected();
                    }
                });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_sign_in:
                startSignInIntent();
                break;
            case R.id.button_sign_out:

                break;
            case R.id.startMatchButton:
                startMatchClicked();
                break;

            case R.id.checkGamesButton:
                checkMatches();
                break;
                
            case R.id.button_finish:
                endGame();
                break;
        }
    }

    private void endGame() {
        //showSpinner();
        turnBasedMultiplayerClient.finishMatch(match.getMatchId())
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
                        onUpdateMatch(turnBasedMatch);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem finishing the match!"));
        isDoingTurn = false;
        setViewVisibility();
    }

    private void checkMatches() {
        turnBasedMultiplayerClient.getInboxIntent()
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_LOOK_AT_MATCHES);
                    }
                })
                .addOnFailureListener(createFailureListener("can't find a game"));
    }

    private void startMatchClicked() {
        // Open the create-game UI. You will get back an onActivityResult
        // and figure out what to do.

        turnBasedMultiplayerClient.getSelectOpponentsIntent(1, 7, true)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_SELECT_PLAYERS);
                    }
                })
                .addOnFailureListener(createFailureListener("error selecting opponent"));
    }

    // startMatch() happens in response to the createTurnBasedMatch()
    // above. This is only called on success, so we should have a
    // valid match object. We're taking this opportunity to setup the
    // game, saving our initial state. Calling takeTurn() will
    // callback to OnTurnBasedMatchUpdated(), which will show the game
    // UI.
    public void startMatch(TurnBasedMatch match) {
        turnData = new StoryTurn();
        // Some basic turn data
        turnData.data = "";

        this.match = match;

        String myParticipantId = match.getParticipantId(playerId);

        //showSpinner();

        turnBasedMultiplayerClient.takeTurn(match.getMatchId(),
                turnData.persist(), myParticipantId)
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
                        updateMatch(turnBasedMatch);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem taking a turn!"));
    }


    // This is the main function that gets called when players choose a match
    // from the inbox, or else create a match and want to start it.
    public void updateMatch(TurnBasedMatch match) {
        this.match = match;

        int status = match.getStatus();
        int turnStatus = match.getTurnStatus();

        switch (status) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                showWarning("Canceled!", "This game was canceled!");
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                showWarning("Expired!", "This game is expired.  So sad!");
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                showWarning("Waiting for auto-match...",
                        "We're still waiting for an automatch partner.");
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
                    turnData = StoryTurn.unpersist(match.getData());
                    showWarning("Complete! Here's your story!",
                            turnData.data);
                    break;
                }

                // Note that in this state, you must still call "Finish" yourself,
                // so we allow this to continue.
                showWarning("Complete!",
                        "This game is over; someone finished it!  You can only finish it now.");
        }

        // OK, it's active. Check on turn status.
        switch (turnStatus) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                turnData = StoryTurn.unpersist(match.getData());
                setNewWords();
                Toast.makeText(this, "It's my turn", Toast.LENGTH_SHORT).show();
                setGameplayUI();
                return;
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                // Should return results.
                showWarning("Alas...", "It's not your turn.");
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                showWarning("Good inititative!",
                        "Still waiting for invitations.\n\nBe patient!");
        }

        turnData = null;

        setViewVisibility();
    }

    private void setNewWords() {
        String nouns = inputStreamToString(getResources().openRawResource(R.raw.nouns));
        String adverbs = inputStreamToString(getResources().openRawResource(R.raw.adverbs));
        String adjs = inputStreamToString(getResources().openRawResource(R.raw.adjs));
        String preps = inputStreamToString(getResources().openRawResource(R.raw.prepositions));
        String verbs = inputStreamToString(getResources().openRawResource(R.raw.verbs));
        String interjections = inputStreamToString(getResources().openRawResource(R.raw.interjections));
        turnData.setWords(nouns,adjs, adverbs, preps, verbs, interjections);
    }

    // Generic warning/info dialog
    public void showWarning(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(title).setMessage(message);

        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                    }
                });

        // create alert dialog
        alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    // Switch to gameplay view.
    public void setGameplayUI() {
        isDoingTurn = true;
        wordsLeft = 5;
        words = turnData.getWords();
        availableWordList.setEnabled(true);
        adapter.clear();
        adapter.addAll(words);
        adapter.notifyDataSetChanged();
        setViewVisibility();
        storyTextView.setText(turnData.data);
        //turnTextView.setText(getString(R.string.turn_label, mTurnData.turnCounter));
    }

    // Upload your new gamestate, then take a turn, and pass it on to the next
    // player.
    public void turnIsOver() {
        //showSpinner();

        String nextParticipantId = getNextParticipantId();
        // Create the next turn
        turnData.turnCounter += 1;
        turnData.data = storyTextView.getText().toString();

        turnBasedMultiplayerClient.takeTurn(match.getMatchId(),
                turnData.persist(), nextParticipantId)
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
                        onUpdateMatch(turnBasedMatch);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem taking a turn!"));

        turnData = null;
    }

    /**
     * Get the next participant. In this function, we assume that we are
     * round-robin, with all known players going before all automatch players.
     * This is not a requirement; players can go in any order. However, you can
     * take turns in any order.
     *
     * @return participantId of next player, or null if automatching
     */
    public String getNextParticipantId() {

        String myParticipantId = match.getParticipantId(playerId);

        ArrayList<String> participantIds = match.getParticipantIds();

        int desiredIndex = -1;

        for (int i = 0; i < participantIds.size(); i++) {
            if (participantIds.get(i).equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }

        if (desiredIndex < participantIds.size()) {
            return participantIds.get(desiredIndex);
        }

        if (match.getAvailableAutoMatchSlots() <= 0) {
            // You've run out of automatch slots, so we start over.
            return participantIds.get(0);
        } else {
            // You have not yet fully automatched, so null will find a new
            // person to play against.
            return null;
        }
    }

    public void onUpdateMatch(TurnBasedMatch match) {
        //dismissSpinner();

        if (match.canRematch()) {
            //askForRematch();
        }

        isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);

        if (isDoingTurn) {
            updateMatch(match);
            return;
        }

        setViewVisibility();
    }
}
