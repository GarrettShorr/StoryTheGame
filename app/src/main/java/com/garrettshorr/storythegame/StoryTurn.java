package com.garrettshorr.storythegame;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic turn data. It's just a blank data string and a turn number counter.
 *
 * @author wolff
 *
 */

public class StoryTurn {
    public static final String TAG = "EBTurn";

    public String data = "";
    public int turnCounter;

    private List<String> nouns, verbs, prepositions, adjs, adverbs, interjections;

    public StoryTurn() {
        nouns = new ArrayList<>();
        verbs = new ArrayList<>();
        prepositions = new ArrayList<>();
        adjs = new ArrayList<>();
        adverbs = new ArrayList<>();
        interjections = new ArrayList<>();
    }

    public void setWords(String n, String adj, String adv, String prep, String v, String inter) {
        try {
            JSONObject ns = new JSONObject(n);
            JSONArray nArr = ns.getJSONArray("nouns");
            for (int i = 0; i < nArr.length(); i++) {
                nouns.add((String)nArr.get(i));
            }

            JSONObject as = new JSONObject(adj);
            JSONArray adArr = as.getJSONArray("adjs");
            for (int i = 0; i < adArr.length(); i++) {
                adjs.add((String)adArr.get(i));
            }

            JSONObject av = new JSONObject(adv);
            JSONArray advArr = av.getJSONArray("adverbs");
            for (int i = 0; i < advArr.length(); i++) {
                adverbs.add((String)advArr.get(i));
            }

            JSONObject p = new JSONObject(prep);
            JSONArray pArr = p.getJSONArray("prepositions");
            for (int i = 0; i < pArr.length(); i++) {
                prepositions.add((String)pArr.get(i));
            }

            JSONObject ver = new JSONObject(v);
            JSONArray vArr = ver.getJSONArray("verbs");
            for (int i = 0; i < vArr.length(); i++) {
                JSONObject verbTypes = vArr.getJSONObject(i);
                verbs.add((String) verbTypes.get("present"));
            }

            JSONObject in = new JSONObject(inter);
            JSONArray inArr = in.getJSONArray("interjections");
            for (int i = 0; i < pArr.length(); i++) {
                interjections.add((String)inArr.get(i));
            }


            //Log.d(TAG, "setWords: " + verbs.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "setWords: FAIL");
        }
    }

    public List<String> getWords() {
        List<String> words = new ArrayList<>();
        words.add(".");
        words.add("!");
        words.add(",");
        words.add("?");
        words.add("the");
        words.add("a");
        words.add("an");
        words.add("and");
        words.add("or");
        for (int i = 0; i < 5; i++) {
            words.add(nouns.get((int)(Math.random()*nouns.size())));
        }
        for (int i = 0; i < 5; i++) {
            words.add(adjs.get((int)(Math.random()*adjs.size())));
        }
        for (int i = 0; i < 5; i++) {
            words.add(adverbs.get((int)(Math.random()*adverbs.size())));
        }
        for (int i = 0; i < 5; i++) {
            words.add(prepositions.get((int)(Math.random()*prepositions.size())));
        }
        for (int i = 0; i < 5; i++) {
            words.add(verbs.get((int)(Math.random()*verbs.size())));
        }
        for (int i = 0; i < 5; i++) {
            words.add(interjections.get((int)(Math.random()*interjections.size())));
        }
        return words;
    }


    // This is the byte array we will write out to the TBMP API.
    public byte[] persist() {
        JSONObject retVal = new JSONObject();

        try {
            retVal.put("data", data);
            retVal.put("turnCounter", turnCounter);

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String st = retVal.toString();

        Log.d(TAG, "==== PERSISTING\n" + st);

        return st.getBytes(Charset.forName("UTF-8"));
    }

    // Creates a new instance of SkeletonTurn.
    static public StoryTurn unpersist(byte[] byteArray) {

        if (byteArray == null) {
            Log.d(TAG, "Empty array---possible bug.");
            return new StoryTurn();
        }

        String st = null;
        try {
            st = new String(byteArray, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }

        Log.d(TAG, "====UNPERSIST \n" + st);

        StoryTurn retVal = new StoryTurn();

        try {
            JSONObject obj = new JSONObject(st);

            if (obj.has("data")) {
                retVal.data = obj.getString("data");
            }
            if (obj.has("turnCounter")) {
                retVal.turnCounter = obj.getInt("turnCounter");
            }

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return retVal;
    }
}
