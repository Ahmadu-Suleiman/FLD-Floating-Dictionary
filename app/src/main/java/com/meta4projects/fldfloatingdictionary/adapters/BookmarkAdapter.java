package com.meta4projects.fldfloatingdictionary.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.room.database.BookmarkDatabase;
import com.meta4projects.fldfloatingdictionary.room.entities.BookMark;

import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.BookmarkHolder> {

    private final List<BookMark> bookmarks;
    private final Context context;
    private BookmarkListener bookmarkListener;

    public BookmarkAdapter(List<BookMark> bookmarks, Context context) {
        this.bookmarks = bookmarks;
        this.context = context;
    }

    public void setBookmarkListener(BookmarkListener bookmarkListener) {
        this.bookmarkListener = bookmarkListener;
    }

    @NonNull
    @Override
    public BookmarkHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_bookmark, parent, false);
        return new BookmarkHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkHolder holder, int position) {
        holder.setEntry(bookmarks.get(position), context, position);
    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    public interface BookmarkListener {
        void bookmarkWordClicked(String word);
    }

    class BookmarkHolder extends RecyclerView.ViewHolder {

        final TextView textViewBookmarkWord;
        final ImageView imageViewDeleteBookmark;

        public BookmarkHolder(@NonNull View itemView) {
            super(itemView);
            textViewBookmarkWord = itemView.findViewById(R.id.textView_bookmark_word);
            imageViewDeleteBookmark = itemView.findViewById(R.id.imageView_bookmark_delete);
        }

        private void setEntry(BookMark bookMark, final Context context, int position) {
            textViewBookmarkWord.setText(bookMark.getBookmarkWord());
            textViewBookmarkWord.setOnClickListener(v -> {
                if (bookmarkListener != null)
                    bookmarkListener.bookmarkWordClicked(bookMark.getBookmarkWord());
            });

            imageViewDeleteBookmark.setOnClickListener(v -> {
                bookmarks.remove(bookMark);
                BookmarkAdapter.this.notifyItemRemoved(position);
                AsyncTask.execute(() -> BookmarkDatabase.getINSTANCE(context).bookMarkDao().deleteBookMark(bookMark));
            });
        }
    }
}
