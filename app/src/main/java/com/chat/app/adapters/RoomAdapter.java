package com.chat.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chat.app.R;
import com.chat.app.interfaces.ClickListenerInterface;
import com.chat.app.model.Room;
import com.chat.app.utils.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.ViewHolder> {


    public ClickListenerInterface clickListenerInterface;
    private List<Room> mDialogsChatList;
    private Context mContext;


    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mName, mDate;
        public ImageView mImageView, mSeenImage;
        public ClickListenerInterface clickListenerInterface;

        public ViewHolder(final View itemView, ClickListenerInterface clickListenerInterface) {
            super(itemView);
            mName = itemView.findViewById(R.id.ID_item_name_of_messenger);
            mImageView = itemView.findViewById(R.id.ID_item_image_of_messenge);
            mDate = itemView.findViewById(R.id.ID_item_date);
            mSeenImage = itemView.findViewById(R.id.ID_item_seen);


            this.clickListenerInterface = clickListenerInterface;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListenerInterface.ClickListener(getAdapterPosition());
                }
            });

        }


    }

    public RoomAdapter(Context context, List<Room> dialogList,ClickListenerInterface clickListenerInterface) {
        mDialogsChatList = dialogList;
        mContext = context;
        this.clickListenerInterface = clickListenerInterface;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_dialog,
                parent, false);
        ViewHolder evh = new ViewHolder(v,clickListenerInterface);
        return evh;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final Room currentItem = mDialogsChatList.get(position);
        holder.mName.setText(currentItem.getRecieverName());
        Picasso.get().load(currentItem.getRecieverPhoto()).into(holder.mImageView);

    }


    @Override
    public int getItemCount() {
        return mDialogsChatList.size();
    }
}