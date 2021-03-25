package com.meta4projects.fldfloatingdictionary.others;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.meta4projects.fldfloatingdictionary.R;

public class Util {
    
    public static String toTitleCase(String word) {
        if ((word.contains("-") && word.indexOf("-") > 1) || word.length() > 1) {
            word = word.toLowerCase();
            char c = word.charAt(0);
            word = ("" + c).toUpperCase() + word.substring(1);
        }
        return word;
    }
    
    public static String getPartOfSpeech(String abbreviation) {
        String partOfSpeech = abbreviation;
        
        switch (abbreviation) {
            case "n.":
                partOfSpeech = "noun";
                break;
            case "prep.":
                partOfSpeech = "preposition";
                break;
            case "p.":
            case "a.":
                partOfSpeech = "adjective";
                break;
            case "adv.":
                partOfSpeech = "adverb";
                break;
            case "pron.":
                partOfSpeech = "pronoun";
                break;
            case "v.":
                partOfSpeech = "verb";
                break;
            case "conj.":
                partOfSpeech = "conjunction";
                break;
            case "interj.":
                partOfSpeech = "interjection";
                break;
        }
        
        return partOfSpeech;
    }
    
    public static void showToast(String text, Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_custom_toast, null);
        
        TextView message = view.findViewById(R.id.textView_toast_message);
        message.setText(text);
        
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.show();
    }
}
