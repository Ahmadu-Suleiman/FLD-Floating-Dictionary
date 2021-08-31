package com.meta4projects.fldfloatingdictionary.fragments;

import static com.meta4projects.fldfloatingdictionary.others.Util.WORD_EXTRA;
import static com.meta4projects.fldfloatingdictionary.others.Util.hideKeyboard;
import static com.meta4projects.fldfloatingdictionary.others.Util.loadNativeAd;
import static com.meta4projects.fldfloatingdictionary.others.Util.showToast;
import static com.meta4projects.fldfloatingdictionary.others.Util.textToSpeech;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.adapters.EntryAdapter;
import com.meta4projects.fldfloatingdictionary.others.EntryUtil;
import com.meta4projects.fldfloatingdictionary.room.database.BookmarkDatabase;
import com.meta4projects.fldfloatingdictionary.room.database.DictionaryDatabase;
import com.meta4projects.fldfloatingdictionary.room.entities.BookMark;
import com.meta4projects.fldfloatingdictionary.room.entities.Entry;

import java.util.ArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DictionaryFragment extends Fragment {

    private final ArrayList<String> entryWords = new ArrayList<>();
    private EntryAdapter entryAdapter;
    private AutoCompleteTextView editText;
    private ImageView speaker, speakerWhole, bookmark;
    private FloatingActionButton random;
    private ViewPager2 viewPager;
    private TextView textViewLoading;
    private boolean bookmarked;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dictionary, container, false);
        editText = view.findViewById(R.id.search_input_view_big);
        speaker = view.findViewById(R.id.speaker_big);
        speakerWhole = view.findViewById(R.id.speaker_whole);
        bookmark = view.findViewById(R.id.bookmark);
        random = view.findViewById(R.id.random_word);
        viewPager = view.findViewById(R.id.viewpager_big);
        textViewLoading = view.findViewById(R.id.loading_dictionary);
        TemplateView templateView = view.findViewById(R.id.native_ad_dictionary);
        loadNativeAd(requireActivity(), templateView, getString(R.string.native_ad_dictionary));

        if (getArguments() != null) initialiseWindow(getArguments().getString(WORD_EXTRA));
        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void initialiseWindow(String wordGotten) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.layout_suggestion);
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
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = editText.getText().toString().trim();
                if (!query.isEmpty()) {
                    setWord(query);
                    editText.setText(query);
                    editText.dismissDropDown();
                    hideKeyboard(requireContext(), editText);
                    return true;
                }
            }
            return false;
        });
        editText.setOnItemClickListener((parent, view, position, id) -> {
            String query = parent.getItemAtPosition(position).toString();
            setWord(query);
            hideKeyboard(requireContext(), editText);
        });

        setViewPager();
        speaker.setOnClickListener(v -> {
            String word = entryWords.get(viewPager.getCurrentItem());
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, "speech_id");
        });
        speakerWhole.setOnClickListener(v -> {
            if (textToSpeech.isSpeaking()) {
                textToSpeech.stop();
                speakerWhole.setImageResource(R.drawable.ic_play);
            } else
                Single.fromCallable(this::getAttributes).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(attributes -> textToSpeech.speak(attributes, TextToSpeech.QUEUE_FLUSH, null, "whole_speech_id")).subscribe();
        });
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                speakerWhole.setImageResource(R.drawable.ic_stop);
            }

            @Override
            public void onDone(String utteranceId) {
                speakerWhole.setImageResource(R.drawable.ic_play);
            }

            @Override
            public void onError(String utteranceId) {
                speakerWhole.setImageResource(R.drawable.ic_play);
            }
        });
        bookmark.setOnClickListener(v -> {
            String currentWord = entryWords.get(viewPager.getCurrentItem());
            if (bookmarked) {
                removeBookmark(currentWord);
                showToast("bookmark removed", requireContext());
            } else {
                addBookmark(currentWord);
                showToast("bookmark added", requireContext());
            }
        });
        random.setOnClickListener(v -> Single.fromCallable(() -> DictionaryDatabase.getINSTANCE(requireContext()).entryWordsDao().getRandomWords()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(words -> {
            entryWords.clear();
            entryWords.addAll(words);
            viewPager.setCurrentItem(entryWords.size() / 2, true);
            entryAdapter.notifyDataSetChanged();
            checkBookmark();
        }).subscribe());

        if (wordGotten.isBlank()) requireActivity().finish();
        else {
            setWord(wordGotten);
            editText.setText(wordGotten);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setViewPager() {
        entryAdapter = new EntryAdapter(entryWords, requireContext(), true);
        viewPager.setAdapter(entryAdapter);
        entryAdapter.setLinkClickListener(this::setWord);
        entryAdapter.notifyDataSetChanged();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                textToSpeech.stop();
                speakerWhole.setImageResource(R.drawable.ic_play);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                checkBookmark();
                int firstPosition = 0, lastPosition = entryWords.size() - 1;

                if ((position == firstPosition) || (position == lastPosition)) {
                    String currentWord = entryWords.get(viewPager.getCurrentItem());
                    Single.fromCallable(() -> DictionaryDatabase.getINSTANCE(requireContext()).entryWordsDao().getEntryWords(currentWord)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(currentWords -> {
                        entryWords.clear();
                        entryWords.addAll(currentWords);
                        viewPager.setCurrentItem(entryWords.indexOf(currentWord), false);
                        entryAdapter.notifyDataSetChanged();
                    }).subscribe();
                }
            }
        });
    }

    private void checkBookmark() {
        String word = entryWords.get(viewPager.getCurrentItem());
        Single.fromCallable(() -> BookmarkDatabase.getINSTANCE(requireContext()).bookMarkDao().bookmarkExists(word)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(bookMarkExist -> {
            if (bookMarkExist) {
                bookmark.setImageResource(R.drawable.ic_bookmark);
                bookmarked = true;
            } else {
                bookmark.setImageResource(R.drawable.ic_unbookmark);
                bookmarked = false;
            }
        }).subscribe();
    }

    private void addBookmark(String word) {
        BookMark bookMark = new BookMark(word);
        AsyncTask.execute(() -> BookmarkDatabase.getINSTANCE(requireContext()).bookMarkDao().insertBookMark(bookMark));
        bookmark.setImageResource(R.drawable.ic_bookmark);
        bookmarked = true;
    }

    private void removeBookmark(String word) {
        Single.fromCallable(() -> BookmarkDatabase.getINSTANCE(requireContext()).bookMarkDao().getBookmark(word)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(bookMark -> {
            AsyncTask.execute(() -> BookmarkDatabase.getINSTANCE(requireContext()).bookMarkDao().deleteBookMark(bookMark));
            bookmark.setImageResource(R.drawable.ic_unbookmark);
            bookmarked = false;
        }).subscribe();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setWord(String word) {
        Single.fromCallable(() -> DictionaryDatabase.getINSTANCE(requireContext()).entryWordsDao().getEntryWords(word)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(words -> {
            if (words.isEmpty()) showToast("could not find such word!", requireContext());
            else {
                textViewLoading.setVisibility(View.GONE);
                entryWords.clear();
                entryWords.addAll(words);
                viewPager.setCurrentItem(entryWords.indexOf(word), true);
                checkBookmark();
                entryAdapter.notifyDataSetChanged();
            }
        }).subscribe();
    }

    private void replaceSearchEntryWords(String word, ArrayAdapter<String> adapter) {
        Single.fromCallable(() -> DictionaryDatabase.getINSTANCE(requireContext()).entryWordsDao().getSimilarEntryWords(word)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(words -> {
            adapter.clear();
            adapter.addAll(words);
            adapter.notifyDataSetChanged();
            adapter.getFilter().filter(word, editText);
        }).subscribe();
    }

    private String getAttributes() {
        StringBuilder attributes = new StringBuilder();
        String currentWord = entryWords.get(viewPager.getCurrentItem());
        ArrayList<Entry> entries = new ArrayList<>(DictionaryDatabase.getINSTANCE(requireContext()).entryDao().getAllEntriesForWord(currentWord));
        entries.forEach(entry -> {
            attributes.append(entry.getWord()).append(".").append(EntryUtil.getPartOfSpeech(entry.getPartOfSpeech())).append(".");

            if (entry.getPlural() != null) attributes.append(entry.getPlural()).append(".");
            if (entry.getTenses() != null) attributes.append(entry.getTenses()).append(".");
            if (entry.getCompare() != null) attributes.append(entry.getCompare());
            attributes.append(".definition.");

            for (String definition : entry.getDefinitions())
                attributes.append(definition).append(".");

            if (!entry.getSynonyms().isEmpty()) {

                attributes.append("synonyms.");
                entry.getSynonyms().forEach(synonym -> attributes.append(synonym).append("."));
            }

            if (!entry.getAntonyms().isEmpty()) {
                attributes.append("antonyms.");
                entry.getAntonyms().forEach(antonym -> attributes.append(antonym).append("."));
            }

            if (!entry.getHypernyms().isEmpty()) {
                attributes.append("hypernyms.");
                entry.getHypernyms().forEach(hypernym -> attributes.append(hypernym).append("."));
            }

            if (!entry.getHyponyms().isEmpty()) {
                attributes.append("hyponyms.");
                entry.getHyponyms().forEach(hyponym -> attributes.append(hyponym).append("."));
            }

            if (!entry.getHomophones().isEmpty()) {
                attributes.append("homophones.");
                entry.getHomophones().forEach(homophone -> attributes.append(homophone).append("."));
            }
        });
        return attributes.toString();
    }

    @Override
    public void onDestroyView() {
        textToSpeech.stop();
        textToSpeech.setOnUtteranceProgressListener(null);
        super.onDestroyView();
    }
}