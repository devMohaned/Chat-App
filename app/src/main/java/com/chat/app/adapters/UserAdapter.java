package com.chat.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chat.app.R;
import com.chat.app.interfaces.ClickListenerInterface;
import com.chat.app.model.User;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {



    private ClickListenerInterface clickListenerInterface;
    private List<User> mUsersList;
    private Context mContext;


    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mName, mEmail;
        public CircleImageView  mProfileImage;
        public ClickListenerInterface clickListenerInterface;

        public ViewHolder(final View itemView,ClickListenerInterface clickListenerInterface) {
            super(itemView);
            mName = itemView.findViewById(R.id.ID_item_user_name);
            mEmail = itemView.findViewById(R.id.ID_item_user_email);
            mProfileImage = itemView.findViewById(R.id.ID_item_user_profile_img);

            this.clickListenerInterface = clickListenerInterface;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListenerInterface.ClickListener(getAdapterPosition());
                }
            });
        }


    }

    public UserAdapter(ClickListenerInterface clickListenerInterface, Context context, List<User> dialogList) {
        this.clickListenerInterface = clickListenerInterface;
        mUsersList = dialogList;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user,
                parent, false);
        ViewHolder evh = new ViewHolder(v,clickListenerInterface);
        return evh;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final User currentItem = mUsersList.get(position);
        holder.mName.setText(currentItem.getName());
        holder.mEmail.setText(currentItem.getEmail());
        Picasso.get().load(currentItem.getProfile_image()).into( holder.mProfileImage);


    }


    @Override
    public int getItemCount() {
        return mUsersList.size();
    }
}