package com.meta4projects.fldfloatingdictionary.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.adapters.BookmarkAdapter.BookmarkHolder
import com.meta4projects.fldfloatingdictionary.others.Util
import com.meta4projects.fldfloatingdictionary.room.database.BookmarkDatabase.Companion.getINSTANCE
import com.meta4projects.fldfloatingdictionary.room.entities.BookMark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarkAdapter(private val bookmarks: ArrayList<BookMark>, private val context: Context) :
    RecyclerView.Adapter<BookmarkHolder>() {

    private var bookmarkListener: BookmarkListener? = null
    fun setBookmarkListener(bookmarkListener: BookmarkListener?) {
        this.bookmarkListener = bookmarkListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_bookmark, parent, false)
        return BookmarkHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkHolder, position: Int) {
        holder.setEntry(bookmarks[position], context, position)
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }

    fun interface BookmarkListener {
        fun bookmarkWordClicked(word: String?)
    }

    inner class BookmarkHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewBookmarkWord: TextView
        private val imageViewDeleteBookmark: ImageView

        init {
            textViewBookmarkWord = itemView.findViewById(R.id.textView_bookmark_word)
            imageViewDeleteBookmark = itemView.findViewById(R.id.imageView_bookmark_delete)
        }

        fun setEntry(bookMark: BookMark, context: Context, position: Int) {
            textViewBookmarkWord.text = bookMark.bookmarkWord
            textViewBookmarkWord.setOnClickListener { if (bookmarkListener != null) bookmarkListener!!.bookmarkWordClicked(bookMark.bookmarkWord) }
            imageViewDeleteBookmark.setOnClickListener {
                bookmarks.remove(bookMark)
                notifyItemRemoved(position)
                CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
                    getINSTANCE(context).bookMarkDao().deleteBookMark(bookMark)
                }
            }
        }
    }
}