package com.meta4projects.fldfloatingdictionary.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ShareCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ads.nativetemplates.TemplateView
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.adapters.BookmarkAdapter
import com.meta4projects.fldfloatingdictionary.others.Util
import com.meta4projects.fldfloatingdictionary.others.Util.WORD_EXTRA
import com.meta4projects.fldfloatingdictionary.others.Util.loadNativeAd
import com.meta4projects.fldfloatingdictionary.room.database.BookmarkDatabase
import com.meta4projects.fldfloatingdictionary.room.entities.BookMark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarkActivity : AppCompatActivity() {
    private lateinit var recyclerViewBookmark: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark)
        recyclerViewBookmark = findViewById(R.id.bookmark_recyclerview)
        val layoutShare: ConstraintLayout = findViewById(R.id.layoutShareBookmarks)
        val layoutDelete: ConstraintLayout = findViewById(R.id.layoutDeleteBookmarks)
        recyclerViewBookmark.layoutManager = LinearLayoutManager(this@BookmarkActivity)
        setBookmarks()
        layoutShare.setOnClickListener { shareBookmarks() }
        layoutDelete.setOnClickListener { deleteAllBookmarks() }
        val templateView: TemplateView = findViewById(R.id.native_ad_bookmark)
        loadNativeAd(this, templateView, getString(R.string.native_ad_bookmark))
    }

    private fun setBookmarks() {
        CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
            val bookmarks = BookmarkDatabase.getINSTANCE(this@BookmarkActivity).bookMarkDao().getAllBookMarks()
            withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                val bookmarkAdapter = BookmarkAdapter(bookmarks as ArrayList<BookMark>, this@BookmarkActivity)
                bookmarkAdapter.setBookmarkListener { word: String? -> startActivity(Intent(this@BookmarkActivity, MainActivity::class.java).putExtra(WORD_EXTRA, word)) }
                recyclerViewBookmark.adapter = bookmarkAdapter
            }
        }
    }

    private fun shareBookmarks() {
        CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
            val bookMarksWords = BookmarkDatabase.getINSTANCE(this@BookmarkActivity).bookMarkDao().getAllBookMarkWords()
            withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                val words = StringBuilder()
                for (word in bookMarksWords) words.append(word).append("\n")
                ShareCompat.IntentBuilder(this@BookmarkActivity).setType("text/plain").setSubject("bookmarks").setChooserTitle("share using...").setText(words.toString()).startChooser()
            }
        }
    }

    private fun deleteAllBookmarks() {
        CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
            BookmarkDatabase.getINSTANCE(this@BookmarkActivity).bookMarkDao().deleteAllBookMarks()
            withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                setBookmarks()
            }
        }
    }
}