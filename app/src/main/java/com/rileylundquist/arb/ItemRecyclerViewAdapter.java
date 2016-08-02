package com.rileylundquist.arb;

import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.vision.text.Text;
import com.rileylundquist.arb.ItemFragment.OnListFragmentInteractionListener;
import com.rileylundquist.arb.dummy.DummyContent.DummyItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ItemRecyclerViewAdapter extends RecyclerView.Adapter<ItemRecyclerViewAdapter.CardHolder> {

    private final List<ArbItem> mItems;
    private final OnListFragmentInteractionListener mListener;

    public ItemRecyclerViewAdapter(List<ArbItem> items, OnListFragmentInteractionListener listener) {
//        mMarkers = markers;
        mItems = items;
        mListener = listener;
    }

    @Override
    public CardHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_card, parent, false);
        return new CardHolder(view);
    }

    @Override
    public void onBindViewHolder(final CardHolder holder, int position) {

        if (position < mItems.size()) {
            holder.mItem = mItems.get(position);
            holder.mName.setText(mItems.get(position).getName());
            holder.mScientificName.setText(mItems.get(position).getScientificName());
            holder.mDescription.setText(mItems.get(position).getDescription());
            holder.mImage.setImageURI(Uri.parse(mItems.get(position).getImage()));

//            holder.mView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (null != mListener) {
//                        // Notify the active callbacks interface (the activity, if the
//                        // fragment is attached to one) that an item has been selected.
//                        mListener.onListFragmentInteraction(holder.mItem);
//                    }
//                }
//            });
        }

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public class CardHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public ArbItem mItem;
        public TextView mName;
        public TextView mScientificName;
        public TextView mDescription;
        public ImageView mImage;

        public CardHolder(View view) {
            super(view);
            mView = view;
            mName = (TextView) view.findViewById(R.id.item_name);
            mScientificName = (TextView) view.findViewById(R.id.item_scientific_name);
            mDescription = (TextView) view.findViewById(R.id.item_description);
            mImage = (ImageView) view.findViewById(R.id.item_image);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mName.getText() + "'";
        }
    }
}
