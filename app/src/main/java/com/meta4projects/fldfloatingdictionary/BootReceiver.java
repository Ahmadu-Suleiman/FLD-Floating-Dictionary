package com.meta4projects.fldfloatingdictionary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.meta4projects.fldfloatingdictionary.DictionaryService.startDictionaryService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        startDictionaryService(context);
    }
}
