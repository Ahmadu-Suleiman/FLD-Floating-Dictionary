package com.meta4projects.fldfloatingdictionary.adapters;

import static android.text.TextUtils.isEmpty;
import static com.meta4projects.fldfloatingdictionary.others.EntryUtil.getPartOfSpeech;
import static com.meta4projects.fldfloatingdictionary.others.Util.copyText;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.room.database.DictionaryDatabase;
import com.meta4projects.fldfloatingdictionary.room.entities.Entry;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryHolder> {

    private final List<String> entryWords;
    private final Context context;
    private LinkClickListener linkClickListener;
    private boolean isBig;

    public EntryAdapter(List<String> entryWords, Context context) {
        this.entryWords = entryWords;
        this.context = context;
    }

    public EntryAdapter(List<String> entryWords, Context context, boolean isBig) {
        this.entryWords = entryWords;
        this.context = context;
        this.isBig = isBig;
    }

    @NonNull
    @Override
    public EntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_entry_view, parent, false);
        return new EntryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryHolder holder, int position) {
        holder.setEntry(entryWords.get(position), context);
    }

    @Override
    public int getItemCount() {
        return entryWords.size();
    }

    public void setLinkClickListener(LinkClickListener linkClickListener) {
        this.linkClickListener = linkClickListener;
    }

    public interface LinkClickListener {
        void onLinkClicked(String link);
    }

    class EntryHolder extends RecyclerView.ViewHolder {

        final LinearLayout layoutView;
        final ScrollView scrollView;

        public EntryHolder(@NonNull View itemView) {
            super(itemView);

            layoutView = itemView.findViewById(R.id.layout_view);
            scrollView = itemView.findViewById(R.id.entry_scrollview);
        }

        private void setEntry(String entryWord, final Context context) {
            if (entryWord != null) {
                Single.fromCallable(() -> DictionaryDatabase.getINSTANCE(context).entryDao().getAllEntriesForWord(entryWord)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(entries -> {
                    layoutView.removeAllViews();

                    for (Entry entry : entries)
                        layoutView.addView(createEntryView(entry, context, layoutView));
                    scrollView.fullScroll(View.FOCUS_UP);
                }).subscribe();
            }
        }

        private View createEntryView(Entry entry, Context context, ViewGroup root) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View viewEntry = inflater.inflate(R.layout.layout_entry, root, false);

            TextView textViewWord = viewEntry.findViewById(R.id.textView_word);
            TextView textViewPlural = viewEntry.findViewById(R.id.textView_plural);
            TextView textViewPartOfSpeech = viewEntry.findViewById(R.id.textView_part_of_speech);
            TextView textViewTenses = viewEntry.findViewById(R.id.textView_tenses);
            TextView textViewCompare = viewEntry.findViewById(R.id.textView_compare);
            TextView textViewDefinitions = viewEntry.findViewById(R.id.textView_definitions);
            TextView textViewSynonymsHeading = viewEntry.findViewById(R.id.textView_synonyms_heading);
            TextView textViewSynonyms = viewEntry.findViewById(R.id.textView_synonyms);
            TextView textViewAntonymsHeading = viewEntry.findViewById(R.id.textView_antonyms_heading);
            TextView textViewAntonyms = viewEntry.findViewById(R.id.textView_antonyms);
            TextView textViewHypernyms = viewEntry.findViewById(R.id.textView_hypernyms);
            TextView textViewHypernymsHeading = viewEntry.findViewById(R.id.textView_hypernyms_heading);
            TextView textViewHyponyms = viewEntry.findViewById(R.id.textView_hyponyms);
            TextView textViewHyponymsHeading = viewEntry.findViewById(R.id.textView_hyponyms_heading);
            TextView textViewHomophones = viewEntry.findViewById(R.id.textView_homophones);
            TextView textViewHomophonesHeading = viewEntry.findViewById(R.id.textView_homophones_heading);

            textViewWord.setText(entry.getWord());
            textViewPartOfSpeech.setText(getPartOfSpeech(entry.getPartOfSpeech()));

            if (isEmpty(entry.getPlural())) textViewPlural.setVisibility(View.GONE);
            else textViewPlural.setText(entry.getPlural());

            if (isEmpty(entry.getTenses())) textViewTenses.setVisibility(View.GONE);
            else textViewTenses.setText(entry.getTenses());

            if (isEmpty(entry.getCompare())) textViewCompare.setVisibility(View.GONE);
            else textViewCompare.setText(entry.getCompare());

            StringBuilder definitions = new StringBuilder();
            for (String definition : entry.getDefinitions())
                definitions.append("- ").append(definition).append("\n \n");

            textViewDefinitions.setText(definitions.toString().trim().concat("\n"));

            for (String synonym : entry.getSynonyms()) {
                SpannableString spannableSynonym = new SpannableString(synonym);
                spannableSynonym.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        if (linkClickListener != null) {
                            linkClickListener.onLinkClicked(synonym);
                            scrollView.fullScroll(View.FOCUS_UP);
                        }
                    }
                }, 0, synonym.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                CharSequence synonymText = TextUtils.concat("- ", spannableSynonym, "\n");
                textViewSynonyms.append(synonymText);
                textViewSynonyms.setMovementMethod(LinkMovementMethod.getInstance());
            }

            for (String antonym : entry.getAntonyms()) {
                SpannableString spannableAntonym = new SpannableString(antonym);
                spannableAntonym.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        if (linkClickListener != null) {
                            linkClickListener.onLinkClicked(antonym);
                            scrollView.fullScroll(View.FOCUS_UP);
                        }
                    }
                }, 0, antonym.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                CharSequence antonymText = TextUtils.concat("- ", spannableAntonym, "\n");
                textViewAntonyms.append(antonymText);
                textViewAntonyms.setMovementMethod(LinkMovementMethod.getInstance());
            }

            for (String hypernym : entry.getHypernyms()) {
                SpannableString spannableHypernym = new SpannableString(hypernym);
                spannableHypernym.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        if (linkClickListener != null) {
                            linkClickListener.onLinkClicked(hypernym);
                            scrollView.fullScroll(View.FOCUS_UP);
                        }
                    }
                }, 0, hypernym.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                CharSequence hypernymText = TextUtils.concat("- ", spannableHypernym, "\n");
                textViewHypernyms.append(hypernymText);
                textViewHypernyms.setMovementMethod(LinkMovementMethod.getInstance());
            }

            for (String hyponym : entry.getHyponyms()) {
                SpannableString spannableHyponym = new SpannableString(hyponym);
                spannableHyponym.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        if (linkClickListener != null) {
                            linkClickListener.onLinkClicked(hyponym);
                            scrollView.fullScroll(View.FOCUS_UP);
                        }
                    }
                }, 0, hyponym.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                CharSequence hyponymText = TextUtils.concat("- ", spannableHyponym, "\n");
                textViewHyponyms.append(hyponymText);
                textViewHyponyms.setMovementMethod(LinkMovementMethod.getInstance());
            }

            for (String homophone : entry.getHomophones()) {
                SpannableString spannableHomophone = new SpannableString(homophone);
                spannableHomophone.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        if (linkClickListener != null) {
                            linkClickListener.onLinkClicked(homophone);
                            scrollView.fullScroll(View.FOCUS_UP);
                        }
                    }
                }, 0, homophone.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                CharSequence homophoneText = TextUtils.concat("- ", spannableHomophone, "\n");
                textViewHomophones.append(homophoneText);
                textViewHomophones.setMovementMethod(LinkMovementMethod.getInstance());
            }

            if (entry.getSynonyms().isEmpty()) {
                textViewSynonymsHeading.setVisibility(View.GONE);
                textViewSynonyms.setVisibility(View.GONE);
            }

            if (entry.getAntonyms().isEmpty()) {
                textViewAntonymsHeading.setVisibility(View.GONE);
                textViewAntonyms.setVisibility(View.GONE);
            }

            if (entry.getHypernyms().isEmpty()) {
                textViewHypernyms.setVisibility(View.GONE);
                textViewHypernymsHeading.setVisibility(View.GONE);
            }

            if (entry.getHyponyms().isEmpty()) {
                textViewHyponyms.setVisibility(View.GONE);
                textViewHyponymsHeading.setVisibility(View.GONE);
            }

            if (entry.getHomophones().isEmpty()) {
                textViewHomophones.setVisibility(View.GONE);
                textViewHomophonesHeading.setVisibility(View.GONE);
            }

            if (!isBig) {
                textViewWord.setOnLongClickListener(v -> {
                    copyText(entry.getWord(), "word", context);
                    return true;
                });

                textViewPlural.setOnLongClickListener(v -> {
                    copyText(entry.getPlural(), "plural", context);
                    return true;
                });

                textViewPartOfSpeech.setOnLongClickListener(v -> {
                    copyText(entry.getPartOfSpeech(), "part of speech", context);
                    return true;
                });

                textViewDefinitions.setOnLongClickListener(v -> {
                    copyText(definitions.toString(), "definitions", context);
                    return true;
                });
            }
            return viewEntry;
        }
    }
}
