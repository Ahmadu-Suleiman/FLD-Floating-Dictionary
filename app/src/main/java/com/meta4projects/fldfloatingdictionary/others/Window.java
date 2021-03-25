package com.meta4projects.fldfloatingdictionary.others;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.adapters.EntryAdapter;
import com.meta4projects.fldfloatingdictionary.database.DictionaryDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class Window implements View.OnTouchListener {
    
    private final Context context;
    private final WindowManager windowManager;
    private final ArrayList<String> entryWords;
    private ViewPager2 viewPager;
    private EntryAdapter entryAdapter;
    private int LAYOUT_FLAG;
    private View rootView;
    private WindowManager.LayoutParams layoutParams;
    private ImageView dragView;
    private ConstraintLayout layoutContainer;
    private AutoCompleteTextView editText;
    private TextToSpeech textToSpeech;
    private boolean wasInFocus = true;
    private int pointerStartX = 0;
    private int pointerStartY = 0;
    private int initialX = 0;
    private int initialY = 0;
    
    
    public Window(Context context, ArrayList<String> entryWords) {
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.entryWords = entryWords;
        
        setEntries();
        setFlag();
        setLayoutParams();
        initialiseWindow();
    }
    
    private void setEntries() {
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                final List<String> entryWordList = DictionaryDatabase.getINSTANCE(context.getApplicationContext()).entryDao().getAllEntryWords();
                
                entryWords.addAll(entryWordList);
                entryAdapter.notifyDataSetChanged();
            }
        });
    }
    
    private void setFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
    }
    
    private void setLayoutParams() {
        layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        
        layoutParams.gravity = Gravity.TOP | Gravity.END;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.x = 0;
        layoutParams.y = 100;
    }
    
    private void initialiseWindow() {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        rootView = layoutInflater.inflate(R.layout.layout_dictionary_widget, null);
        
        dragView = rootView.findViewById(R.id.drag_view);
        
        rootView.setOnTouchListener(this);
        
        layoutContainer = rootView.findViewById(R.id.container_layout);
        
        editText = rootView.findViewById(R.id.search_input_view);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
                update();
                wasInFocus = true;
                showSoftKeyboard(v);
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = editText.getText().toString().trim().toLowerCase();
                    query = query.trim().toLowerCase();
                    if (entryWords.contains(query)) {
                        setWord(query);
                    } else {
                        Util.showToast("couldn't find such word!", context);
                    }
                    return true;
                }
                return false;
            }
        });
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.layout_suggestion, entryWords);
        editText.setAdapter(adapter);
        editText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String word = parent.getItemAtPosition(position).toString();
                setWord(word);
            }
        });
        
        textToSpeech = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });
        
        ImageView speaker = rootView.findViewById(R.id.speaker);
        speaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String word = entryWords.get(viewPager.getCurrentItem());
                textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
        
        viewPager = rootView.findViewById(R.id.viewpager);
        entryAdapter = new EntryAdapter(entryWords, context);
        viewPager.setAdapter(entryAdapter);
    }
    
    private void setWord(String word) {
        viewPager.setCurrentItem(entryWords.indexOf(word));
        entryAdapter.notifyDataSetChanged();
    }
    
    private void collapseViews() {
        if (layoutContainer.getVisibility() == View.VISIBLE) {
            layoutContainer.setVisibility(View.GONE);
            dragView.setAlpha(0.6f);
        } else {
            layoutContainer.setVisibility(View.VISIBLE);
            dragView.setAlpha(1.0f);
        }
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Point position = new Point();
        
        if (isViewInBounds(rootView, (int) (event.getRawX()), (int) (event.getRawY()))) {
            editTextReceiveFocus();
        } else {
            editTextDontReceiveFocus();
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pointerStartX = (int) event.getRawX();
                pointerStartY = (int) event.getRawY();
                
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                return true;
            case MotionEvent.ACTION_UP:
                collapseViews();
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = pointerStartX - event.getRawX();
                float deltaY = event.getRawY() - pointerStartY;
                
                position.x = (int) (initialX + deltaX);
                position.y = (int) (initialY + deltaY);
                
                setPosition(position);
                break;
        }
        return true;
    }
    
    private void setPosition(Point position) {
        try {
            layoutParams.x = position.x;
            layoutParams.y = position.y;
            update();
        } catch (Exception e) {
            Toast.makeText(context, "could not move view!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void update() {
        try {
            windowManager.updateViewLayout(rootView, layoutParams);
        } catch (Exception e) {
            Toast.makeText(context, "could not update view", Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean isViewInBounds(View view, int x, int y) {
        Rect outRect = new Rect();
        int[] location = new int[2];
        view.getDrawingRect(outRect);
        view.getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return outRect.contains(x, y);
    }
    
    private void editTextReceiveFocus() {
        if (!wasInFocus) {
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
            update();
            wasInFocus = true;
        }
    }
    
    private void editTextDontReceiveFocus() {
        if (wasInFocus) {
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
            update();
            wasInFocus = false;
            hideKeyboard(context, editText);
        }
    }
    
    private void hideKeyboard(Context context, View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    
    public void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    public void open() {
        try {
            windowManager.addView(rootView, layoutParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public void close() {
        textToSpeech.shutdown();
        windowManager.removeView(rootView);
    }
}
