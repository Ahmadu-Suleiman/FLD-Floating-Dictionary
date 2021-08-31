package com.meta4projects.fldfloatingdictionary.activities;

import static com.meta4projects.fldfloatingdictionary.others.Util.WORD_EXTRA;
import static com.meta4projects.fldfloatingdictionary.others.Util.loadNativeAd;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ShareCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.ads.nativetemplates.TemplateView;
import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.adapters.BookmarkAdapter;
import com.meta4projects.fldfloatingdictionary.room.database.BookmarkDatabase;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BookmarkActivity extends AppCompatActivity {

    private RecyclerView recyclerViewBookmark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);

        ConstraintLayout layoutShare, layoutDelete;
        recyclerViewBookmark = findViewById(R.id.bookmark_recyclerview);
        layoutShare = findViewById(R.id.layoutShareBookmarks);
        layoutDelete = findViewById(R.id.layoutDeleteBookmarks);

        recyclerViewBookmark.setLayoutManager(new LinearLayoutManager(BookmarkActivity.this));
        setBookmarks();

        layoutShare.setOnClickListener(v -> shareBookmarks());
        layoutDelete.setOnClickListener(v -> deleteAllBookmarks());

        TemplateView templateView = findViewById(R.id.native_ad_bookmark);
        loadNativeAd(this, templateView, getString(R.string.native_ad_bookmark));

    }


    private void setBookmarks() {
        Single.fromCallable(() -> BookmarkDatabase.getINSTANCE(BookmarkActivity.this).bookMarkDao().getAllBookMarks()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(bookMarks -> {
            BookmarkAdapter bookmarkAdapter = new BookmarkAdapter(bookMarks, BookmarkActivity.this);
            bookmarkAdapter.setBookmarkListener(word -> startActivity(new Intent(BookmarkActivity.this, MainActivity.class).putExtra(WORD_EXTRA, word)));
            recyclerViewBookmark.setAdapter(bookmarkAdapter);
        }).subscribe();
    }

    private void shareBookmarks() {
        Single.fromCallable(() -> BookmarkDatabase.getINSTANCE(BookmarkActivity.this).bookMarkDao().getAllBookMarkWords()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(bookMarksWords -> {
            StringBuilder words = new StringBuilder();
            for (String word : bookMarksWords) words.append(word).append("\n");
            new ShareCompat.IntentBuilder(this).setType("text/plain").setSubject("bookmarks").setChooserTitle("share using...").setText(words.toString()).startChooser();
        }).subscribe();
    }

    private void deleteAllBookmarks() {
        AsyncTask.execute(() -> {
            BookmarkDatabase.getINSTANCE(BookmarkActivity.this).bookMarkDao().deleteAllBookMarks();
            runOnUiThread(this::setBookmarks);
        });
    }
}