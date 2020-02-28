package com.chat.app.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.chat.app.R;
import com.chat.app.adapters.UserAdapter;
import com.chat.app.fragment.MessagingActivity;
import com.chat.app.interfaces.ClickListenerInterface;
import com.chat.app.model.Room;
import com.chat.app.model.User;
import com.chat.app.utils.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllUsersActivity extends AppCompatActivity implements ClickListenerInterface {

    String mMyName, mMyFirebaseUid, mMyPhoto;
    List<User> userList = new ArrayList<>();
    UserAdapter userAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_left_arrow);


        Bundle extras = getIntent().getExtras();
        mMyName = extras.getString("my_name");
        mMyFirebaseUid = extras.getString("my_firebase_uid");
        mMyPhoto = extras.getString("my_photo");

         userAdapter = new UserAdapter(this,this,userList);
        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());

        RecyclerView recyclerView = findViewById(R.id.ID_users_recyclerview);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                mLayoutManager.getOrientation());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(userAdapter);
        getAllUsers(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.users_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint("Full Name");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String name) {
                if (name.length() > 0)
                {
                    getAllUsers(name);
                }else{
                    getAllUsers(null);
                }
                return true;
            }
        });


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
            case R.id.menu_search:

                Toast.makeText(this, "Searching", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void ClickListener(int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        User clickedUser = userList.get(position);

        DocumentReference senderRoomWithReference = db.collection("rooms_with").document(mMyFirebaseUid);
        senderRoomWithReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Map<String, Object> allDocuments = documentSnapshot.getData()  ;
                if (allDocuments != null && allDocuments.containsKey(clickedUser.getFirebase_uid()))
                {
                    String roomId = allDocuments.get(clickedUser.getFirebase_uid()).toString();
                    // TODO: Go to Chatting
                    Intent intent = new Intent(AllUsersActivity.this, MessagingActivity.class);
                    intent.putExtra("my_name",mMyName);
                    intent.putExtra("my_pic",mMyPhoto);
                    intent.putExtra("my_uid",mMyFirebaseUid);

                    intent.putExtra(Constants.INTENT_ROOM_ID,roomId);
                    intent.putExtra(Constants.INTENT_RECIEVER_ID,clickedUser.getFirebase_uid());
                    intent.putExtra(Constants.INTENT_RECEIVER_NAME,clickedUser.getName());
                    intent.putExtra(Constants.INTENT_RECIEVER_PHOTO,clickedUser.getProfile_image());
                    startActivity(intent);
                    Toast.makeText(AllUsersActivity.this, "You've already talked with him.", Toast.LENGTH_SHORT).show();


                }else{

                    String commonChatRoomId = FirebaseDatabase.getInstance().getReference().push().getKey();

                    String senderRoomId = FirebaseDatabase.getInstance().getReference().push().getKey();
                    Room senderRoom = new Room();
                    senderRoom.setRoom_sender_id(mMyFirebaseUid);
                    senderRoom.setSenderName(mMyName);
                    senderRoom.setRoom_reciever_id(clickedUser.getFirebase_uid());
                    senderRoom.setRecieverName(clickedUser.getName());
                    senderRoom.setRecieverPhoto(clickedUser.getProfile_image());
                    senderRoom.setRoom_created_at(System.currentTimeMillis());
                    senderRoom.setRoom_created_by(mMyFirebaseUid);
                    senderRoom.setRoom_id(commonChatRoomId);


                    String recieverRoomId = FirebaseDatabase.getInstance().getReference().push().getKey();
                    Room recieverRoom = new Room();
                    recieverRoom.setRoom_sender_id(clickedUser.getFirebase_uid());
                    recieverRoom.setSenderName(clickedUser.getName());
                    recieverRoom.setRoom_reciever_id(mMyFirebaseUid);
                    recieverRoom.setRecieverName(mMyName);
                    recieverRoom.setRecieverPhoto(mMyPhoto);
                    recieverRoom.setRoom_created_at(System.currentTimeMillis());
                    recieverRoom.setRoom_created_by(mMyFirebaseUid);
                    recieverRoom.setRoom_id(commonChatRoomId);

                    CollectionReference roomsReference = db.collection("rooms");
                    roomsReference.document(senderRoomId).set(senderRoom);

                    roomsReference.document(recieverRoomId).set(recieverRoom);

                    DocumentReference senderReference = db.collection("rooms_id").document(mMyFirebaseUid);
                    DocumentReference recieverReference = db.collection("rooms_id").document(clickedUser.getFirebase_uid());

                    Map<String, Object> senderMap = new HashMap<>();
                    senderMap.put(FirebaseDatabase.getInstance().getReference().push().getKey() /* To Be Unique Key*/,
                            senderRoomId);
                    senderReference.set(senderMap, SetOptions.merge());

                    Map<String, Object> recieverMap = new HashMap<>();
                    recieverMap.put(FirebaseDatabase.getInstance().getReference().push().getKey() /* To Be Unique Key*/,
                            recieverRoomId);
                    recieverReference.set(recieverMap, SetOptions.merge());


                    DocumentReference senderRoomWithReference = db.collection("rooms_with").document(mMyFirebaseUid);
                    Map<String, Object> senderRoomWithMap = new HashMap<>();
                    senderRoomWithMap.put(clickedUser.getFirebase_uid(), commonChatRoomId);
                    senderRoomWithReference.set(senderRoomWithMap);

                    DocumentReference recieverRoomWithReference = db.collection("rooms_with").document(clickedUser.getFirebase_uid());
                    Map<String, Object> recieverRoomWithMap = new HashMap<>();
                    recieverRoomWithMap.put(mMyFirebaseUid, commonChatRoomId);
                    recieverRoomWithReference.set(recieverRoomWithMap);




                    Intent intent = new Intent(AllUsersActivity.this, MessagingActivity.class);
                    intent.putExtra("my_name",mMyName);
                    intent.putExtra("my_pic",mMyPhoto);
                    intent.putExtra("my_uid",mMyFirebaseUid);

                    intent.putExtra(Constants.INTENT_ROOM_ID,commonChatRoomId);
                    intent.putExtra(Constants.INTENT_RECIEVER_ID,clickedUser.getFirebase_uid());
                    intent.putExtra(Constants.INTENT_RECEIVER_NAME,clickedUser.getName());
                    intent.putExtra(Constants.INTENT_RECIEVER_PHOTO,clickedUser.getProfile_image());
                    startActivity(intent);

                    Toast.makeText(AllUsersActivity.this, "NEW CHAT HORAI", Toast.LENGTH_SHORT).show();
                }
            }
        });



    }

    private void getAllUsers(String name){
        userList.clear();
        userAdapter.notifyDataSetChanged();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Query userReference;
        if (name == null) {
            userReference = firestore.collection("users");
        }else{
            userReference = firestore.collection("users").whereEqualTo("name",name);
        }
        userReference.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List<DocumentSnapshot> users =  queryDocumentSnapshots.getDocuments();
                for (DocumentSnapshot documentSnapshotUser : users)
                {
                    User user = documentSnapshotUser.toObject(User.class);
                    userList.add(user);
                }
            }
        }).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                userAdapter.notifyDataSetChanged();
            }
        });
    }

}
