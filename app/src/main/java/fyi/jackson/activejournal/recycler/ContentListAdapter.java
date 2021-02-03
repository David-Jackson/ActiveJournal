package fyi.jackson.activejournal.recycler;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

import fyi.jackson.activejournal.R;
import fyi.jackson.activejournal.data.entities.Activity;
import fyi.jackson.activejournal.data.entities.Content;
import fyi.jackson.activejournal.recycler.helper.ItemTouchHelperAdapter;
import fyi.jackson.activejournal.recycler.helper.OnStartDragListener;
import fyi.jackson.activejournal.recycler.holder.ContentEditTextViewHolder;
import fyi.jackson.activejournal.recycler.holder.ContentImageViewHolder;
import fyi.jackson.activejournal.recycler.holder.ContentTextViewHolder;
import fyi.jackson.activejournal.recycler.holder.NewContentViewHolder;
import fyi.jackson.activejournal.ui.ImageRequester;
import fyi.jackson.activejournal.ui.NewContentRowClickListener;
import fyi.jackson.activejournal.ui.ContentChangeListener;
import fyi.jackson.activejournal.util.Validator;

public class ContentListAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ItemTouchHelperAdapter, NewContentRowClickListener, ContentChangeListener {

    private static final String TAG = ContentListAdapter.class.getSimpleName();

    public static final int VIEW_TYPE_TEXT_CONTENT = Content.TYPE_TEXT;
    public static final int VIEW_TYPE_IMAGE_CONTENT = Content.TYPE_IMAGE;
    public static final int VIEW_TYPE_NEW_CONTENT = 418;
    public static final int VIEW_TYPE_EDIT_TEXT_CONTENT = 370;

    private ViewGroup parent;
    private Snackbar editSnackbar;

    private Activity currentActivity;
    private List<Content> contents;
    private OnStartDragListener onStartDragListener;
    private ContentChangeListener contentChangeListener;
    private ImageRequester imageRequester;

    private boolean editMode = false;

    public ContentListAdapter(Activity currentActivity,
                              OnStartDragListener onStartDragListener,
                              ContentChangeListener contentChangeListener,
                              ImageRequester imageRequester) {
        this.currentActivity = currentActivity;
        this.onStartDragListener = onStartDragListener;
        this.contentChangeListener = contentChangeListener;
        this.imageRequester = imageRequester;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.parent = parent;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v;
        RecyclerView.ViewHolder viewHolder;
        switch (viewType) {
            case VIEW_TYPE_TEXT_CONTENT:
                v = inflater.inflate(R.layout.view_holder_content_text, parent, false);
                viewHolder = new ContentTextViewHolder(v);
                break;
            case VIEW_TYPE_IMAGE_CONTENT:
                v = inflater.inflate(R.layout.view_holder_content_image, parent, false);
                viewHolder = new ContentImageViewHolder(v);
                break;
            case VIEW_TYPE_EDIT_TEXT_CONTENT:
                v = inflater.inflate(R.layout.view_holder_content_edit_text, parent, false);
                viewHolder = new ContentEditTextViewHolder(v);
                break;
            default: // VIEW_TYPE_NEW_CONTENT
                v = inflater.inflate(R.layout.view_holder_new_content, parent, false);
                viewHolder = new NewContentViewHolder(v);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case VIEW_TYPE_TEXT_CONTENT:
                ((ContentTextViewHolder) viewHolder).bindTo(contents.get(position), onStartDragListener);
                break;
            case VIEW_TYPE_IMAGE_CONTENT:
                ((ContentImageViewHolder) viewHolder).bindTo(contents.get(position), onStartDragListener);
                break;
            case VIEW_TYPE_EDIT_TEXT_CONTENT:
                ((ContentEditTextViewHolder) viewHolder).bindTo(contents.get(position), this, onStartDragListener);
                break;
            case VIEW_TYPE_NEW_CONTENT:
                ((NewContentViewHolder) viewHolder).bindTo(this);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return (contents == null ? 0 : contents.size() + 1);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == contents.size()) {
            return VIEW_TYPE_NEW_CONTENT;
        }
        int viewType = contents.get(position).getType();

        if (editMode && viewType == VIEW_TYPE_TEXT_CONTENT) {
            viewType = VIEW_TYPE_EDIT_TEXT_CONTENT;
        }

        return viewType;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(contents, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);

        updateContentPositions();
        contentChangeListener.onChange(contents);

        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        Content contentToRemove = contents.remove(position);
        notifyItemRemoved(position);
        contentChangeListener.onRemove(contentToRemove);
    }

    private void updateContentPositions() {
        for (int i = 0; i < contents.size(); i++) {
            contents.get(i).setPosition(i);
        }
    }

    @Override
    public void onClick(int interactionType) {
        switch (interactionType) {
            case TYPE_ADD_TEXT:
                addTextContent();
                enterEditMode();
                focusOnContent(contents.size() - 1);
                break;
            case TYPE_ADD_IMAGE:
                imageRequester.onRequestImage();
                break;
            case TYPE_EDIT_CONTENT:
                toggleEditMode();
                break;
        }
    }

    public void onDestroy() {
        if (editMode) {
            exitEditMode();
        }
    }

    private void addTextContent() {
        Content newContent = new Content();
        newContent.setPosition(contents.size());
        newContent.setType(Content.TYPE_TEXT);
        newContent.setActivityId(currentActivity.getActivityId());
        newContent.setValue("");
        contentChangeListener.onInsert(newContent);
    }

    private void focusOnContent(int position) {
        // TODO: 9/21/2018 implement functionality that allows keyboard focus on certain view holder
    }

    private void toggleEditMode() {
        if (editMode) {
            exitEditMode();
        } else {
            enterEditMode();
        }
    }

    private void enterEditMode() {
        editMode = true;
        notifyDataSetChanged();
        showSnackbar();
    }

    private void exitEditMode() {
        editMode = false;
        notifyDataSetChanged();
        editSnackbar.dismiss();
        validateContents();
        contentChangeListener.onChange(contents);
    }

    private void showSnackbar() {
        editSnackbar = Snackbar.make(parent, "Editing", Snackbar.LENGTH_INDEFINITE)
                .setAction("Done", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        exitEditMode();
                    }
                });
        editSnackbar.show();
    }

    private void validateContents() {
        for (int i = 0; i < contents.size(); i++) {
            if (!Validator.checkContent(contents.get(i))) {
                onItemDismiss(i);
            }
        }
    }

    @Override
    public void onChange(List<Content> updatedContents) {
        for (Content c : updatedContents) {
            onChange(c);
        }
    }

    @Override
    public void onChange(Content updatedContent) {
        for (int i = 0 ; i < contents.size(); i++) {
            Content c = contents.get(i);
            if (c.getUid() == updatedContent.getUid()) {
                contents.set(i, updatedContent);
                break;
            }
        }
    }

    @Override
    public void onInsert(Content newContent) {

    }

    @Override
    public void onRemove(Content contentToRemove) {

    }
}
