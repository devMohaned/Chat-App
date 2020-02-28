package com.chat.app.activities.messages;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.chat.app.activities.AllUsersActivity;
import com.chat.app.R;
import com.chat.app.adapters.RoomAdapter;
import com.chat.app.fragment.MessagingActivity;
import com.chat.app.interfaces.ClickListenerInterface;
import com.chat.app.activities.login.MVPLoginActivity;
import com.chat.app.model.Room;
import com.chat.app.utils.AppUtils;
import com.chat.app.utils.Constants;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.chat.app.utils.FileUtils.getFileExtension;


public class UserListMessagesActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, ClickListenerInterface {
    private static final int PICK_IMAGE_REQUEST = 25;
    private static final int MY_EXT_SOURCE_REQUEST_CODE = 990;
    private RoomAdapter mChatAdapter;
    SwipeRefreshLayout mSwipeRefreshLayout;
    TextView mEmptyUserList;

    FirebaseApp app;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list_messages);
        AppUtils.handlingSecondFirebaseDatabase(this);
        app = FirebaseApp.getInstance("secondary");
        setupUserlistMessagesViews();
        setupToolbar();
        addOnlineListener();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.ID_user_list_messages_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        CircleImageView profileImage = findViewById(R.id.ID_profile_img);
        Uri photoUrl = FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl();
        Picasso.get().load(photoUrl).into(profileImage);
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
            }
        });

    }

    private void showFileChooser() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    , MY_EXT_SOURCE_REQUEST_CODE);
        } else {
            selectImage();
        }

    }


    private void selectImage() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageIntent.setType("image/*");
        pickImageIntent.putExtra("aspectX", 1);
        pickImageIntent.putExtra("aspectY", 1);
        pickImageIntent.putExtra("scale", true);
        pickImageIntent.putExtra("outputFormat",
                Bitmap.CompressFormat.JPEG.toString());
        if (pickImageIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(pickImageIntent, PICK_IMAGE_REQUEST);
        else
            Toast.makeText(this, getString(R.string.you_do_not_have_gallery_app), Toast.LENGTH_LONG).show();
    }


    private void setupUserlistMessagesViews() {
        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.ID_user_list_swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */
        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                // Fetching data from server
                getChatsRoomIds();
            }
        });


        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mChatAdapter = new RoomAdapter(getApplicationContext(), rooms,this);
        RecyclerView recyclerView = findViewById(R.id.ID_user_list_messages_recyclerView);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                mLayoutManager.getOrientation());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(mChatAdapter);

        mEmptyUserList = findViewById(R.id.ID_empty_user_list_text);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = auth.getCurrentUser();
                Intent intent = new Intent(UserListMessagesActivity.this, AllUsersActivity.class);
                intent.putExtra("my_name", currentUser.getDisplayName());
                intent.putExtra("my_firebase_uid",currentUser.getUid());
                intent.putExtra("my_photo",currentUser.getPhotoUrl());
                Toast.makeText(UserListMessagesActivity.this, "Go to another activity", Toast.LENGTH_SHORT).show();
                startActivity(intent);
            }
        });

    }

    private void stopRefreshing(SwipeRefreshLayout swipeRefreshLayout) {
        swipeRefreshLayout.setRefreshing(false);
    }


    List<String> roomsIds = new ArrayList<>();
    private void getChatsRoomIds(){
        rooms.clear();
        roomsIds.clear();
        if (mChatAdapter != null) {
            mChatAdapter.notifyDataSetChanged();
        }


        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference roomsIdRef = firestore.collection("rooms_id");
        roomsIdRef.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Map<String, Object> rooms = documentSnapshot.getData();
                        if (rooms != null) {
                            Object[] keys = rooms.keySet().toArray();
                            for (int i = 0; i < rooms.size(); i++) {
                                String currentRoomId = String.valueOf(rooms.get(keys[i]));
                                roomsIds.add(currentRoomId);
                            }
                        }

                    }
                })
        .addOnCompleteListener(task -> getChats());
    }

    List<Room> rooms = new ArrayList<>();
    private void getChats() {


            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            for (String roomId : roomsIds) {
                firestore.collection("rooms").document(roomId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Room room = documentSnapshot.toObject(Room.class);
                        rooms.add(room);
                    }
                }).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        mChatAdapter.notifyDataSetChanged();

                    }
                });

            }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
            case R.id.menu_logout:
                deleteTokenAndSignOut();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            //getting the image Uri
            Uri fileUri = data.getData();
            CircleImageView profileImage = findViewById(R.id.ID_profile_img);
            Picasso.get().load(fileUri).into(profileImage);
            uploadFileVer1("name" + System.currentTimeMillis(), fileUri);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_EXT_SOURCE_REQUEST_CODE) {

            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                //do something like displaying a message that he didn`t allow the app to access gallery and you wont be able to let him select from gallery
                Toast.makeText(this, getString(R.string.permission_rejected), Toast.LENGTH_LONG).show();
            }


        }
    }


    @Override
    public void onRefresh() {
        getChatsRoomIds();
    }






    @Override
    public void ClickListener(int position) {
       FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Room currentRoom = rooms.get(position);


        Intent intent = new Intent(this, MessagingActivity.class);
        intent.putExtra("my_name",currentUser.getDisplayName());
        intent.putExtra("my_pic",currentUser.getPhotoUrl());
        intent.putExtra("my_uid",currentUser.getUid());

        intent.putExtra(Constants.INTENT_ROOM_ID,currentRoom.getRoom_id());
        intent.putExtra(Constants.INTENT_RECIEVER_ID,currentRoom.getRoom_reciever_id());
        intent.putExtra(Constants.INTENT_RECEIVER_NAME,currentRoom.getRecieverName());
        intent.putExtra(Constants.INTENT_RECIEVER_PHOTO,currentRoom.getRecieverPhoto());
        startActivity(intent);
    }

    private void uploadFileVer1(String fileName, Uri fileUri) {
        Context mContext = this;
        if (fileUri != null) {
            String uniqueName = FirebaseDatabase.getInstance(app).getReference().push().getKey();

            String nameWithExtentsion = fileName + uniqueName + "." + getFileExtension(mContext, fileUri);

            FirebaseStorage storage = FirebaseStorage.getInstance();
// Create a storage reference from our app
            StorageReference storageRef = storage.getReference();

// Create a reference to "mountains.jpg"
            StorageReference fileStorageRef = storageRef.child(nameWithExtentsion);


            UploadTask uploadTask = fileStorageRef.putFile(fileUri);
            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    return fileStorageRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setPhotoUri(downloadUri).build();
                        firebaseUser.updateProfile(profileUpdates);


                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(firebaseUser.getUid())
                                .update("profile_image",downloadUri.toString());
                    } else {
                        // Handle failures
                        // ...
                    }
                }
            });
        }

    }



    private void addOnlineListener() {
            DatabaseReference connectedRef = FirebaseDatabase.getInstance(app).getReference()
                    .child(Constants.FIREBASE_DB_ONLINE_STATUS)
                    .child(        FirebaseAuth.getInstance()
                            .getUid());
            connectedRef.setValue(Constants.FIREBASE_DB_ONLINE_STATUS_IS_ONLINE);
            connectedRef.onDisconnect().setValue(Constants.FIREBASE_DB_ONLINE_STATUS_IS_OFFLINE);



    }


    private void deleteTokenAndSignOut() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPostExecute(Void aVoid) {
                FirebaseAuth.getInstance()
                        .signOut();
                Intent intent = new Intent(UserListMessagesActivity.this, MVPLoginActivity.class);
                startActivity(intent);
                finish();
                super.onPostExecute(aVoid);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    FirebaseInstanceId.getInstance().deleteInstanceId();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;

            }
        }.execute();
    }




}


