package com.meta4projects.fldfloatingdictionary.others;

import static com.meta4projects.fldfloatingdictionary.others.Util.hideKeyboard;
import static com.meta4projects.fldfloatingdictionary.others.Util.showSoftKeyboard;
import static com.meta4projects.fldfloatingdictionary.others.Util.showToast;
import static com.meta4projects.fldfloatingdictionary.others.Util.textToSpeech;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.adapters.EntryAdapter;
import com.meta4projects.fldfloatingdictionary.room.database.DictionaryDatabase;

import java.util.ArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Window implements View.OnTouchListener {

    private final Context context;
    private final WindowManager windowManager;
    private ArrayList<String> entryWords;
    private ViewPager2 viewPager;
    private EntryAdapter entryAdapter;
    private View rootView;
    private WindowManager.LayoutParams layoutParams;
    private ImageView dragView;
    private ConstraintLayout layoutContainer;
    private AutoCompleteTextView editText;
    private boolean wasInFocus = true;
    private int pointerStartX = 0;
    private int pointerStartY = 0;
    private int initialX = 0;
    private int initialY = 0;

    public Window(Context context) {
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        setEntries();
    }

    private void setEntries() {
        Single.fromCallable(() -> DictionaryDatabase.getINSTANCE(context).entryWordsDao().getRandomWords()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(words -> {
            entryWords = new ArrayList<>(words);
            setLayoutParams();
            initialiseWindow();
        }).subscribe();
    }

    private void setLayoutParams() {
        layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.END;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.x = 0;
        layoutParams.y = 100;
    }

    @SuppressLint("InflateParams")
    private void initialiseWindow() {
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        rootView = Util.isNightMode ? layoutInflater.inflate(R.layout.layout_floating_dictionary_night, null) : layoutInflater.inflate(R.layout.layout_floating_dictionary, null);
        dragView = rootView.findViewById(R.id.drag_view);
        layoutContainer = rootView.findViewById(R.id.container_layout);
        rootView.setOnTouchListener(this);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        int deviceWidth = displayMetrics.widthPixels;
        int deviceHeight = displayMetrics.heightPixels;
        int height = (int) (deviceHeight * 0.5);

        ViewGroup.LayoutParams layoutParamsContainer = layoutContainer.getLayoutParams();
        layoutParamsContainer.width = deviceWidth;
        layoutParamsContainer.height = height;
        layoutContainer.setLayoutParams(layoutParamsContainer);

        editText = rootView.findViewById(R.id.search_input_view);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, Util.isNightMode ? R.layout.layout_suggestion_night : R.layout.layout_suggestion);
        editText.setAdapter(adapter);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                replaceSearchEntryWords(s.toString().trim(), adapter);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        editText.setOnClickListener(v -> {
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
            layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
            update();
            wasInFocus = true;
            showSoftKeyboard(context, v);
        });

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = editText.getText().toString().trim();
                if (!query.isEmpty()) {
                    setWord(query);
                    editText.setText(query);
                    editText.dismissDropDown();
                    hideKeyboard(context, editText);
                    return true;
                }
            }
            return false;
        });

        editText.setOnItemClickListener((parent, view, position, id) -> {
            String word = parent.getItemAtPosition(position).toString();
            setWord(word);
            hideKeyboard(context, editText);
        });


        ImageView speaker = rootView.findViewById(R.id.speaker);
        speaker.setOnClickListener(v -> {
            String word = entryWords.get(viewPager.getCurrentItem());
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, "speech_id");
        });

        setViewPager();
        open();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setViewPager() {
        viewPager = rootView.findViewById(R.id.viewpager);
        entryAdapter = new EntryAdapter(entryWords, context);
        viewPager.setAdapter(entryAdapter);
        entryAdapter.setLinkClickListener(this::setWord);
        entryAdapter.notifyDataSetChanged();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int firstPosition = 0, lastPosition = entryWords.size() - 1;

                if ((position == firstPosition) || (position == lastPosition)) {
                    String currentWord = entryWords.get(viewPager.getCurrentItem());
                    Single.fromCallable(() -> DictionaryDatabase.getINSTANCE(context).entryWordsDao().getEntryWords(currentWord)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(currentWords -> {
                        entryWords.clear();
                        entryWords.addAll(currentWords);
                        viewPager.setCurrentItem(entryWords.indexOf(currentWord), false);
                        entryAdapter.notifyDataSetChanged();
                    }).subscribe();
                }
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setWord(String word) {
        Single.fromCallable(() -> DictionaryDatabase.getINSTANCE(context).entryWordsDao().getEntryWords(word)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(words -> {
            if (words.isEmpty()) showToast("could not find such word!", context);
            else {
                entryWords.clear();
                entryWords.addAll(words);
                viewPager.setCurrentItem(entryWords.indexOf(word), true);
                entryAdapter.notifyDataSetChanged();
            }
        }).subscribe();
    }

    private void replaceSearchEntryWords(String word, ArrayAdapter<String> adapter) {
        Single.fromCallable(() -> DictionaryDatabase.getINSTANCE(context).entryWordsDao().getSimilarEntryWords(word)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(words -> {
            adapter.clear();
            adapter.addAll(words);
            adapter.notifyDataSetChanged();
            adapter.getFilter().filter(word, editText);
        }).subscribe();
    }

    private void collapseViews() {
        if (layoutContainer.getVisibility() == View.VISIBLE) {
            layoutContainer.setVisibility(View.GONE);
            dragView.setAlpha(0.6f);
            editTextDontReceiveFocus();
        } else {
            layoutContainer.setVisibility(View.VISIBLE);
            dragView.setAlpha(1.0f);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Point position = new Point();
        if (isViewInBounds(rootView, (int) (event.getRawX()), (int) (event.getRawY())))
            editTextReceiveFocus();
        else editTextDontReceiveFocus();

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
            Util.showToast("could not move view!", context);
            e.printStackTrace();
        }
    }

    private void update() {
        try {
            windowManager.updateViewLayout(rootView, layoutParams);
        } catch (Exception e) {
            e.printStackTrace();
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

    public void open() {
        try {
            windowManager.addView(rootView, layoutParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            windowManager.removeView(rootView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
