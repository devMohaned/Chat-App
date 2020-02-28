package com.chat.app.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.chat.app.R;
import com.chat.app.model.ChatMessage;
import com.chat.app.model.User;
import com.chat.app.utils.Constants;
import com.chat.app.utils.FullScreenActivity;
import com.devlomi.record_view.OnBasketAnimationEnd;
import com.devlomi.record_view.OnRecordListener;
import com.devlomi.record_view.RecordButton;
import com.devlomi.record_view.RecordView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nguyenhoanglam.imagepicker.model.Config;
import com.nguyenhoanglam.imagepicker.model.Image;
import com.nguyenhoanglam.imagepicker.ui.imagepicker.ImagePicker;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.chat.app.utils.FileUtils.getFileExtension;


public class MessagingActivity extends AppCompatActivity {

    private static final int ADD_IMAGE = 320;
    private static final int MY_PERMISSION_SOURCE_REQUEST_CODE = 300;

    private EditText mMessageEditText;
    private String roomId;
    private LinearLayout layout;
    DatabaseReference mRoomChatDatabaseReference;
    private ImageView mUploadImage;
    private NestedScrollView mScrollView;
    private String receiverID;
    private String receiverName;
    private String recieverPhoto;
    private boolean canAddImage = true;
    private long last_sent = 0;


    User currentUser;
    FirebaseApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);
        app = FirebaseApp.getInstance("secondary");


        Bundle extras = getIntent().getExtras();
        String myName = extras.getString("my_name");
        String myFirebaseUid = extras.getString("my_uid");
        String myProfilePic = extras.getString("my_pic");
        currentUser = new User();
        currentUser.setName(myName);
        currentUser.setFirebase_uid(myFirebaseUid);
        currentUser.setProfile_image(myProfilePic);


        roomId = extras.getString(Constants.INTENT_ROOM_ID);
        receiverID = extras.getString(Constants.INTENT_RECIEVER_ID);
        receiverName = extras.getString(Constants.INTENT_RECEIVER_NAME);
        recieverPhoto = extras.getString(Constants.INTENT_RECIEVER_PHOTO);

        setupWidgits();
        setupDatabase();
        setupMic();

    }

    long delay = 5000; // 5 seconds after user stops typing
    long last_text_edit = 0;
    Handler handler = new Handler();

    private Runnable input_finish_checker = new Runnable() {
        public void run() {
            if (System.currentTimeMillis() > (last_text_edit + delay - 500)) {
                setOnlineStatusBackToOnline();
            }
        }
    };

    TextView mName, mStatus;

    private void setupWidgits() {
        mName = findViewById(R.id.ID_reciever_name);
        mStatus = findViewById(R.id.ID_rec_status);
        mName.setText(receiverName);
        getUserStatus(receiverID);

        mMessageEditText = (EditText) findViewById(R.id.ID_message_edit_text);
        mMessageEditText.setImeOptions(EditorInfo.IME_ACTION_NONE);
        mMessageEditText.setRawInputType(InputType.TYPE_CLASS_TEXT);

        mMessageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if ((actionId == EditorInfo.IME_ACTION_NONE)) {
                    if (mMessageEditText != null) {
                        if (mMessageEditText.getText() != null && mMessageEditText.getText().toString().length() > 0) {
                            mMessageEditText.setText(mMessageEditText.getText().toString() + "\n");
                            int position = mMessageEditText.length();
                            Editable etext = mMessageEditText.getText();
                            Selection.setSelection(etext, position);
                        }
                    }
                    return true;
                }
                return false;
            }
        });


        mMessageEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count,
                                                  int after) {
                    }

                    @Override
                    public void onTextChanged(final CharSequence s, int start, int before,
                                              int count) {
                        //You need to remove MessagingActivity.this to run only once
                        handler.removeCallbacks(input_finish_checker);
                        setOnlineStatusToTyping();
                    }

                    @Override
                    public void afterTextChanged(final Editable s) {
                        //avoid triggering event when text is empty
                        if (s.length() > 0) {
                            last_text_edit = System.currentTimeMillis();
                            handler.postDelayed(input_finish_checker, delay);
                            recordButton.setVisibility(View.GONE);
                        } else {
                            handler.postDelayed(input_finish_checker, delay);
                            recordButton.setVisibility(View.VISIBLE);
                        }
                    }
                }

        );


        layout = (LinearLayout) findViewById(R.id.ID_messaging_linear_layout);
        mScrollView = findViewById(R.id.ID_messaging_scroll_view);

        mUploadImage = findViewById(
                R.id.ID_message_upload_img
        );
        mUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMultipleFileChooser(ADD_IMAGE);

            }
        });

        RelativeLayout mDoneButton = (RelativeLayout) findViewById(R.id.ID_done_my_message);
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mMessageEditText.getText().toString().trim().length() > 0) {
                    addMessageLocally(mMessageEditText.getText().toString().trim());
                    sendMessage();
                    setOnlineStatusBackToOnline();
                }
            }
        });

    }


    private void openImageIntent(int requestId) {

        ImagePicker.with(this)                         //  Initialize ImagePicker with activity or fragment context
                .setStatusBarColor("#00255e")       // Status BarColor SDK 21+
                .setToolbarColor("#004c8c")         //  Toolbar color
                .setToolbarTextColor("#FFFFFF")     //  Toolbar text color (Title and Done button)
                .setToolbarIconColor("#FFFFFF")     //  Toolbar icon color (Back and Camera button)
                .setProgressBarColor("#0277bd")     //  ProgressBar color
                .setBackgroundColor("#FFFFFF")      //  Background color
                .setCameraOnly(false)               //  Camera mode
                .setMultipleMode(true)              //  Select multiple images or single image
                .setFolderMode(true)                //  Folder mode
                .setShowCamera(true)                //  Show camera button
                .setFolderTitle(getString(R.string.images))           //  Folder title (works with FolderMode = true)
                .setImageTitle(getString(R.string.galleries))         //  Image title (works with FolderMode = false)
                .setDoneTitle(getString(R.string.done))               //  Done button title
                .setLimitMessage(getString(R.string.you_have_reached_your_max))    // Selection limit message
                .setMaxSize(1)                     //  Max images can be selected
                .setSavePath("ImagePicker")         //  Image capture folder name
                //              .setSelectedImages(images)          //  Selected images
                .setAlwaysShowDoneButton(true)      //  Set always show done button in multiple mode
                .setRequestCode(requestId)                //  Set request code, default Config.RC_PICK_IMAGES
                .setKeepScreenOn(true)              //  Keep screen on when selecting images
                .start();                           //  Start ImagePicker


    }


    private void showMultipleFileChooser(int requestId) {
        if (ContextCompat.checkSelfPermission(MessagingActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}
                    , MY_PERMISSION_SOURCE_REQUEST_CODE);
        } else {
            openImageIntent(requestId);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == ADD_IMAGE) {
                ArrayList<Image> images = data.getParcelableArrayListExtra(Config.EXTRA_IMAGES);
                if (images.size() > 0) {
                    Uri uri = Uri.fromFile(new File(images.get(0).getPath()));

                    ProgressBar progressBar = new ProgressBar(MessagingActivity.this);
                    progressBar.setId((int) (System.currentTimeMillis() / 100000));
                    progressBar.setLayoutParams(new LinearLayout.LayoutParams(48, 48));


//                    mRoomChatDatabaseReference.addChildEventListener(childEventListener);


                    uploadFile(uri, progressBar);

                }
            }
        }
    }

    int failedCount = 0;

    private void uploadFile(Uri fileUri, ProgressBar pb) {
        Context mContext = MessagingActivity.this;
        pb.setVisibility(View.VISIBLE);
        if (fileUri != null) {
            String uniqueName = FirebaseDatabase.getInstance(app).getReference().push().getKey();
            String fileName = "temp" + System.currentTimeMillis();

            String nameWithExtentsion =/* fileName +*/ uniqueName + "." + getFileExtension(mContext, fileUri);

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
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

                        sendImageMessage(downloadUri.toString());

                    } else {
                        // Handle failures
                        // ...
                    }

                    pb.setVisibility(View.GONE);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(mContext, getString(R.string.uploading_failed), Toast.LENGTH_SHORT).show();
                    if (failedCount <= 3) uploadFile(fileUri, pb);
                    failedCount++;
                }
            });


        }
    }


    private void sendImageMessage(String url) {
        // Do what you want when you send a message
        String pushedKey = mRoomChatDatabaseReference.push().getKey();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChatText(url);
        chatMessage.setChatSenderUid(currentUser.getFirebase_uid());
        chatMessage.setType(Constants.MESSAGE_TYPE_IMAGE_MSG);
        mRoomChatDatabaseReference.child(pushedKey).setValue(chatMessage);
    }


    private void setOnlineStatusBackToOnline() {
        isTyping = false;
        DatabaseReference connectedRef = FirebaseDatabase.getInstance(app).getReference()
                .child(Constants.FIREBASE_DB_ONLINE_STATUS)
                .child(currentUser.getFirebase_uid());
        connectedRef.setValue(Constants.FIREBASE_DB_ONLINE_STATUS_IS_ONLINE);
    }

    boolean isTyping = false;

    private void setOnlineStatusToTyping() {
        if (!isTyping) {
            isTyping = true;
            DatabaseReference connectedRef = FirebaseDatabase.getInstance(app).getReference()
                    .child(Constants.FIREBASE_DB_ONLINE_STATUS)
                    .child(currentUser.getFirebase_uid());
            connectedRef.setValue(receiverID);
        }
    }


    int id = 10;
    HashMap<String, ProgressBar> progressBarHashMap = new HashMap<>();

    private void addMessageLocally(String messageText) {
        id++;
        ProgressBar pb = new ProgressBar(MessagingActivity.this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(56, 56);
        params.gravity = Gravity.END;
        params.setLayoutDirection(Gravity.END);
        params.setMargins(4, 4, 4, 4);
        pb.setLayoutParams(params);
        pb.setId(id);
        progressBarHashMap.put(messageText, pb);
        layout.addView(pb);
    }

    private void removeMessageLocally(String messageText) {
        if (progressBarHashMap.containsKey(messageText)) {
            ProgressBar pb = progressBarHashMap.get(messageText);
            layout.removeView(pb);
            progressBarHashMap.remove(messageText);
        }

    }

    private void sendMessage() {


        // Do what you want when you send a message
        String pushedKey = mRoomChatDatabaseReference.push().getKey();
        String chatText = mMessageEditText.getText().toString().trim();

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChatText(chatText);
        chatMessage.setChatSenderUid(currentUser.getFirebase_uid());
        chatMessage.setType(Constants.MESSAGE_TYPE_NORMAL_MSG);


        if (!mMessageEditText.getText().toString().trim().isEmpty()) {
            mRoomChatDatabaseReference.child(pushedKey).setValue(chatMessage);
            mMessageEditText.setText("");
        }
    }

    private void sendAudioMessage(String audioLink) {


        // Do what you want when you send a message
        String pushedKey = mRoomChatDatabaseReference.push().getKey();

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChatText(audioLink);
        chatMessage.setChatSenderUid(currentUser.getFirebase_uid());
        chatMessage.setType(Constants.MESSAGE_TYPE_AUDIO_MSG);


        mRoomChatDatabaseReference.child(pushedKey).setValue(chatMessage);

    }

    @Override
    protected void onResume() {
        if (receiverID != null && listener != null) {
            FirebaseDatabase.getInstance(app).getReference().child(Constants.FIREBASE_DB_ONLINE_STATUS)
                    .child(receiverID).addValueEventListener(listener);
        }

        super.onResume();
    }

    @Override
    public void onStop() {
       /* if (mRoomChatDatabaseReference != null && childEventListener != null)
            mRoomChatDatabaseReference.removeEventListener(childEventListener);*/
        if (receiverID != null && listener != null) {
            FirebaseDatabase.getInstance(app).getReference().child(Constants.FIREBASE_DB_ONLINE_STATUS)
                    .child(receiverID).removeEventListener(listener);
        }

        super.onStop();
    }

    String date = "2018/01/10";

    ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
            ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
            if (chatMessage != null) {
                if (chatMessage.timeStamp > 0) {
                    String currentDate = getDateUntilDay(chatMessage.timeStamp);
                    if (!date.equals(currentDate)) {
                        addNewTimelineSeprator(chatMessage.timeStamp);
                        date = currentDate;
                    }
                }
                if (chatMessage.getType() == null || chatMessage.getType().equals(Constants.MESSAGE_TYPE_NORMAL_MSG)) {
                    addNewMessageWithPhoto(dataSnapshot.getKey(), chatMessage);
                } else if (chatMessage.getType().equals(Constants.MESSAGE_TYPE_AUDIO_MSG)) {
                    addAudioMessage(dataSnapshot.getKey(), chatMessage);
                } else if (chatMessage.getType().equals(Constants.MESSAGE_TYPE_IMAGE_MSG)) {
                    addImageMessage(dataSnapshot.getKey(), chatMessage);
                }
                checkSeenity(dataSnapshot.getKey(), chatMessage);
                mScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!mMessageEditText.hasFocus()) {
                            mScrollView.fullScroll(View.FOCUS_DOWN);
                        } else {
                            mScrollView.fullScroll(View.FOCUS_DOWN);
                            mMessageEditText.requestFocus();
                        }
                    }
                });

//                Log.e("Datasnapshot Key: ", dataSnapshot.getKey());

            }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
            if (hashMap.containsKey(dataSnapshot.getKey())) {
                int changedId = hashMap.get(dataSnapshot.getKey());
                try {
                    ImageView seenImage = layout.findViewById(changedId).findViewById(R.id.ID_item_message_seen);
                    try {
                        ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                        if (chatMessage != null) {


                            if (chatMessage.seen) {
                                seenImage.setImageResource(R.drawable.ic_seen);
                            } else {
                                seenImage.setImageResource(R.drawable.ic_unseen);

                            }
                        }
                    } catch (NullPointerException exce) {

                    }
                } catch (NullPointerException e) {

                }
            } else {
                // I am editing other's seen ability so i should edit other's ability not mine
                //Ignroe MessagingActivity.this case
            }
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    };

    private void setupDatabase() {
        mRoomChatDatabaseReference = FirebaseDatabase.getInstance(app)
                .getReference()
                .child(Constants.FIREBASE_DB_MESSAGES)
                .child(roomId);

        mRoomChatDatabaseReference.addChildEventListener(childEventListener);
    }

    HashMap<String, Integer> hashMap = new HashMap<>();
    int seenId = 0;

    private void addNewMessageWithPhoto(String dataSnapShotKey, ChatMessage chatMessageVer2) {
        last_sent = chatMessageVer2.timeStamp;
        try {
            if (!chatMessageVer2.getChatSenderUid().equals(currentUser.getFirebase_uid())) {

                try {
                    LayoutInflater inflater = (LayoutInflater) MessagingActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    RelativeLayout rel = (RelativeLayout) inflater.inflate(R.layout.item_message, null);


                    TextView txt = (TextView) rel.findViewById(R.id.message_text);
                    txt.setText(chatMessageVer2.getChatText());

                    TextView timestampTextView = rel.findViewById(R.id.ID_message_timestamp);
                    try {
                        if (chatMessageVer2.timeStamp/*getTime()*/ > 0) {
                            timestampTextView.setText(getTimeInReadableFormate(chatMessageVer2.timeStamp/*getTime()*/));
                        }
                    } catch (NullPointerException e) {
                        timestampTextView.setVisibility(View.GONE);
                    }


                    CircleImageView img = (CircleImageView) rel.findViewById(R.id.img);
                    if (canAddImage) {
                        Picasso.get().load(recieverPhoto).into(img, new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError(Exception e) {
                                img.setImageResource(R.mipmap.ic_launcher_round);
                            }
                        });


                    } else {

                        img.setVisibility(View.INVISIBLE);
                    }


                    layout.addView(rel);
                    canAddImage = false;

                } catch (NullPointerException e) {

                }
            } else {
                canAddImage = true;
                // It's me who's chatting
                addChatMessageAsMe(dataSnapShotKey, chatMessageVer2);

            }
        } catch (NullPointerException e) {
            Toast.makeText(MessagingActivity.this, getString(R.string.something_is_wrong), Toast.LENGTH_SHORT).show();
        }
    }

    private void addChatMessageAsMe(String dataSnapShotKey, ChatMessage chatMessageVer2) {

        try {
            LayoutInflater inflater = (LayoutInflater) MessagingActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout rel = (RelativeLayout) inflater.inflate(R.layout.item_message_me, null);

            rel.setId(seenId);
            hashMap.put(dataSnapShotKey, seenId);
            seenId++;


            TextView txt = (TextView) rel.findViewById(R.id.message_text);
            txt.setText(chatMessageVer2.getChatText());

            TextView timestampTextView = rel.findViewById(R.id.ID_message_timestamp);
            try {
                if (chatMessageVer2.timeStamp/*getTime()*/ > 0) {
                    timestampTextView.setText(getTimeInReadableFormate(chatMessageVer2.timeStamp/*getTime()*/));
                }
            } catch (NullPointerException e) {
                timestampTextView.setVisibility(View.GONE);
            }


            AppCompatImageView seenImage = rel.findViewById(R.id.ID_item_message_seen);
            if (chatMessageVer2.seen) {
                seenImage.setImageResource(R.drawable.ic_seen);
            } else {
                seenImage.setImageResource(R.drawable.ic_unseen);
            }

            removeMessageLocally(chatMessageVer2.getChatText());
            rel.setSoundEffectsEnabled(true);
            rel.playSoundEffect(SoundEffectConstants.CLICK);
            layout.addView(rel);

        } catch (NullPointerException e) {

        }
    }


    private void addAudioMessage(String dataSnapShotKey, ChatMessage chatMessageVer2) {
        last_sent = chatMessageVer2.timeStamp;
        try {
            if (!chatMessageVer2.getChatSenderUid().equals(currentUser.getFirebase_uid())) {

                try {
                    LayoutInflater inflater = (LayoutInflater) MessagingActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    RelativeLayout rel = (RelativeLayout) inflater.inflate(R.layout.item_message_audio, null);

                    TextView startingTime = rel.findViewById(R.id.ID_audio_start_time);
                    TextView audioTime = rel.findViewById(R.id.ID_audio_time);

                    SeekBar audioSeekbar = rel.findViewById(R.id.ID_audio_seekbar);

                    try {
                        Uri myUri = Uri.parse(chatMessageVer2.getChatText()); // initialize Uri here
                        MediaPlayer mPlayer = new MediaPlayer();
                        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mPlayer.setDataSource(MessagingActivity.this.getApplicationContext(), myUri);
                        mPlayer.prepareAsync();

                        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                int sTime = 0, eTime = 0;
                                Handler hdlr = new Handler();
                                eTime = mPlayer.getDuration();
                                sTime = mPlayer.getCurrentPosition();
                                audioSeekbar.setMax(eTime);

                                audioTime.setText(String.format("%d:%d", TimeUnit.MILLISECONDS.toMinutes(eTime),
                                        TimeUnit.MILLISECONDS.toSeconds(eTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(eTime))));
                                startingTime.setText(String.format("%d:%d", TimeUnit.MILLISECONDS.toMinutes(sTime),
                                        TimeUnit.MILLISECONDS.toSeconds(sTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sTime))));


                                final int[] finalSTime = {sTime};
                                Runnable UpdateSongTime = new Runnable() {
                                    @Override
                                    public void run() {
                                        finalSTime[0] = mPlayer.getCurrentPosition();
                                        startingTime.setText(String.format("%d:%d", TimeUnit.MILLISECONDS.toMinutes(finalSTime[0]),
                                                TimeUnit.MILLISECONDS.toSeconds(finalSTime[0]) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalSTime[0]))));
                                        audioSeekbar.setProgress(finalSTime[0]);
                                        hdlr.postDelayed(this, 100);
                                    }
                                };


                                ImageView playBtn = rel.findViewById(R.id.ID_audio_play);
                                playBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        if (!mPlayer.isPlaying()) {
                                            mPlayer.start();
                                            playBtn.setImageResource(R.drawable.ic_pause_1);
                                            audioSeekbar.setProgress(finalSTime[0]);
                                            hdlr.postDelayed(UpdateSongTime, 100);

                                        } else {
                                            mPlayer.pause();
                                            playBtn.setImageResource(R.drawable.ic_media_play_1);

                                        }
                                    }
                                });


                                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        playBtn.setImageResource(R.drawable.ic_media_play_1);
                                    }
                                });

                                audioSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    @Override
                                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                    }

                                    @Override
                                    public void onStartTrackingTouch(SeekBar seekBar) {

                                    }

                                    @Override
                                    public void onStopTrackingTouch(SeekBar seekBar) {
                                        mPlayer.seekTo(seekBar.getProgress());
                                    }
                                });
                            }
                        });


                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    TextView timestampTextView = rel.findViewById(R.id.ID_message_timestamp);
                    try {
                        if (chatMessageVer2.timeStamp > 0) {
                            timestampTextView.setText(getTimeInReadableFormate(chatMessageVer2.timeStamp/*getTime()*/));
                        }
                    } catch (NullPointerException e) {
                        timestampTextView.setVisibility(View.GONE);
                    }


                    CircleImageView img = (CircleImageView) rel.findViewById(R.id.img);
                    if (canAddImage) {
                        Picasso.get().load(recieverPhoto).into(img, new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError(Exception e) {
                                img.setImageResource(R.mipmap.ic_launcher_round);
                            }
                        });


                    } else {

                        img.setVisibility(View.INVISIBLE);
                    }


                    layout.addView(rel);
                    canAddImage = false;

                } catch (NullPointerException e) {

                }
            } else {
                canAddImage = true;
                // It's me who's chatting
                addAudioMessageAsMe(dataSnapShotKey, chatMessageVer2);

            }
        } catch (NullPointerException e) {
            Toast.makeText(MessagingActivity.this, getString(R.string.something_is_wrong), Toast.LENGTH_SHORT).show();
        }
    }

    private void addAudioMessageAsMe(String dataSnapShotKey, ChatMessage chatMessageVer2) {

        try {
            LayoutInflater inflater = (LayoutInflater) MessagingActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout rel = (RelativeLayout) inflater.inflate(R.layout.item_message_audio_me, null);


            TextView startingTime = rel.findViewById(R.id.ID_audio_start_time);
            TextView audioTime = rel.findViewById(R.id.ID_audio_time);

            SeekBar audioSeekbar = rel.findViewById(R.id.ID_audio_seekbar);

            try {
                Uri myUri = Uri.parse(chatMessageVer2.getChatText()); // initialize Uri here
                MediaPlayer mPlayer = new MediaPlayer();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(MessagingActivity.this.getApplicationContext(), myUri);
                mPlayer.prepareAsync();

                mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        int sTime = 0, eTime = 0;
                        Handler hdlr = new Handler();
                        eTime = mPlayer.getDuration();
                        sTime = mPlayer.getCurrentPosition();
                        audioSeekbar.setMax(eTime);

                        audioTime.setText(String.format("%d:%d", TimeUnit.MILLISECONDS.toMinutes(eTime),
                                TimeUnit.MILLISECONDS.toSeconds(eTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(eTime))));
                        startingTime.setText(String.format("%d:%d", TimeUnit.MILLISECONDS.toMinutes(sTime),
                                TimeUnit.MILLISECONDS.toSeconds(sTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sTime))));

                        final int[] finalSTime = {sTime};
                        Runnable UpdateSongTime = new Runnable() {
                            @Override
                            public void run() {
                                finalSTime[0] = mPlayer.getCurrentPosition();
                                startingTime.setText(String.format("%d:%d", TimeUnit.MILLISECONDS.toMinutes(finalSTime[0]),
                                        TimeUnit.MILLISECONDS.toSeconds(finalSTime[0]) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalSTime[0]))));
                                audioSeekbar.setProgress(finalSTime[0]);
                                hdlr.postDelayed(this, 100);
                            }
                        };


                        ImageView playBtn = rel.findViewById(R.id.ID_audio_play);
                        playBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (!mPlayer.isPlaying()) {
                                    mPlayer.start();
                                    playBtn.setImageResource(R.drawable.ic_pause);
                                    audioSeekbar.setProgress(finalSTime[0]);
                                    hdlr.postDelayed(UpdateSongTime, 100);

                                } else {
                                    mPlayer.pause();
                                    playBtn.setImageResource(R.drawable.ic_media_play);

                                }
                            }
                        });

                        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                playBtn.setImageResource(R.drawable.ic_media_play);
                            }
                        });

                        audioSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {

                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                                mPlayer.seekTo(seekBar.getProgress());
                            }
                        });
                    }
                });


            } catch (IOException e) {
                e.printStackTrace();
            }


            TextView timestampTextView = rel.findViewById(R.id.ID_message_timestamp);
            try {
                if (chatMessageVer2.timeStamp > 0) {
                    timestampTextView.setText(getTimeInReadableFormate(chatMessageVer2.timeStamp/*getTime()*/));
                }
            } catch (NullPointerException e) {
                timestampTextView.setVisibility(View.GONE);
            }


            AppCompatImageView seenImage = rel.findViewById(R.id.ID_item_message_seen);
            if (chatMessageVer2.seen) {
                seenImage.setImageResource(R.drawable.ic_seen);
            } else {
                seenImage.setImageResource(R.drawable.ic_unseen);
            }

            removeMessageLocally(chatMessageVer2.getChatText());
            rel.setSoundEffectsEnabled(true);
            rel.playSoundEffect(SoundEffectConstants.CLICK);

            rel.setId(seenId);
            hashMap.put(dataSnapShotKey, seenId);
            seenId++;

            layout.addView(rel);


        } catch (NullPointerException e) {

        }
    }

    private void addImageMessage(String dataSnapShotKey, ChatMessage chatMessageVer2) {
        last_sent = chatMessageVer2.timeStamp;
        try {
            if (!chatMessageVer2.getChatSenderUid().equals(currentUser.getFirebase_uid())) {

                try {
                    LayoutInflater inflater = (LayoutInflater) MessagingActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    RelativeLayout rel = (RelativeLayout) inflater.inflate(R.layout.item_message_image, null);


                    ImageView image = (ImageView) rel.findViewById(R.id.message_image);
                    Picasso.get().load(chatMessageVer2.getChatText())
                            // Add .resize to solve OutOfMemory Exception (Large Files) OR use Compressor when uploading
                            .into(image, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError(Exception e) {
                                    image.setImageResource(R.drawable.ic_error_triangle);
                                }
                            });

                    image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // OPEN IT IN FULL SCREEN
                            openFullScreen(chatMessageVer2.getChatText());
                        }
                    });

                    TextView timestampTextView = rel.findViewById(R.id.ID_message_timestamp);
                    try {
                        if (chatMessageVer2.timeStamp > 0) {
                            timestampTextView.setText(getTimeInReadableFormate(chatMessageVer2.timeStamp));
                        }
                    } catch (NullPointerException e) {
                        timestampTextView.setVisibility(View.GONE);
                    }


                    CircleImageView img = (CircleImageView) rel.findViewById(R.id.img);
                    if (canAddImage) {
                        Picasso.get().load(recieverPhoto).into(img, new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError(Exception e) {
                                img.setImageResource(R.mipmap.ic_launcher_round);
                            }
                        });


                    } else {

                        img.setVisibility(View.INVISIBLE);
                    }


                    layout.addView(rel);
                    canAddImage = false;

                } catch (NullPointerException e) {

                }
            } else {
                canAddImage = true;
                // It's me who's chatting
                addImageMessageAsMe(dataSnapShotKey, chatMessageVer2);

            }
        } catch (NullPointerException e) {
            Toast.makeText(MessagingActivity.this, getString(R.string.something_is_wrong), Toast.LENGTH_SHORT).show();
        }
    }

    private void addImageMessageAsMe(String dataSnapShotKey, ChatMessage chatMessageVer2) {

        try {
            LayoutInflater inflater = (LayoutInflater) MessagingActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout rel = (RelativeLayout) inflater.inflate(R.layout.item_message_image_me, null);

            rel.setId(seenId);
            hashMap.put(dataSnapShotKey, seenId);
            seenId++;


            ImageView image = (ImageView) rel.findViewById(R.id.message_image);
            Picasso.get().load(chatMessageVer2.getChatText())
                    // Add .resize to solve OutOfMemory Exception (Large Files) OR use Compressor when uploading
                    .into(image, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            image.setImageResource(R.drawable.ic_error_triangle);
                        }
                    });

            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // OPEN IT IN FULL SCREEN
                    openFullScreen(chatMessageVer2.getChatText());
                }
            });


            TextView timestampTextView = rel.findViewById(R.id.ID_message_timestamp);
            try {
                if (chatMessageVer2.timeStamp/*getTime()*/ > 0) {
                    timestampTextView.setText(getTimeInReadableFormate(chatMessageVer2.timeStamp/*getTime()*/));
                }
            } catch (NullPointerException e) {
                timestampTextView.setVisibility(View.GONE);
            }


            AppCompatImageView seenImage = rel.findViewById(R.id.ID_item_message_seen);
            if (chatMessageVer2.seen) {
                seenImage.setImageResource(R.drawable.ic_seen);
            } else {
                seenImage.setImageResource(R.drawable.ic_unseen);
            }

            removeMessageLocally(chatMessageVer2.getChatText());
            rel.setSoundEffectsEnabled(true);
            rel.playSoundEffect(SoundEffectConstants.CLICK);
            layout.addView(rel);

        } catch (NullPointerException e) {

        }
    }

    private void openFullScreen(String chatText) {
        Intent intent = new Intent(MessagingActivity.this, FullScreenActivity.class);
        intent.putExtra("img_url", chatText);
        startActivity(intent);
    }


    private String getDateUntilDay(long timeInMilliSeconds) {
        String dateFormat = "yyyy/MM/dd";
// Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMilliSeconds);
        return formatter.format(calendar.getTime());
    }


    private String getTimeInReadableFormate(long timeInMilliSeconds) {
        String dateFormat = "h:mm a";
// Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMilliSeconds);
        return formatter.format(calendar.getTime());
    }

    private void addNewTimelineSeprator(long timeInMillseconds) {
        // It's me who's chatting
        TextView textView = new TextView(MessagingActivity.this);
        textView.setText(getReadableDate(timeInMillseconds));
        textView.setTextColor(getResources().getColor(R.color.grey_light));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextAppearance(android.R.style.TextAppearance_Medium);
        } else {
            textView.setTextAppearance(MessagingActivity.this, android.R.style.TextAppearance_Medium);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(15, 5, 15, 10);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        textView.setLayoutParams(lp);
        textView.setBackgroundResource(R.drawable.timeline_shape);
        layout.addView(textView);
        canAddImage = true;
    }

    private String getReadableDate(long timeInMillseconds) {
        Calendar c = Calendar.getInstance();
        //Set time in milliseconds
        c.setTimeInMillis(timeInMillseconds);
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);
       /* int hr = c.get(Calendar.HOUR);
        int min = c.get(Calendar.MINUTE);
        int sec = c.get(Calendar.SECOND);*/
        String date;

        long currentTime = Calendar.getInstance().getTimeInMillis();
        if (currentTime - timeInMillseconds < 172800000 && currentTime - timeInMillseconds > 86400000) {
            date = getString(R.string.yesterday);
        } else if (currentTime - timeInMillseconds <= 86400000) {
            date = getString(R.string.today);
        } else {
            String[] months = getResources().getStringArray(R.array.months);
            String month = months[mMonth];
            date = month + " " + mDay + ", " + mYear;
        }

        return date;


    }


    private void checkSeenity(String dataSnapShotKey, ChatMessage chatMessageVer2) {
        if (!chatMessageVer2.getChatSenderUid().equals(currentUser.getFirebase_uid())) {
            if (!chatMessageVer2.seen) {
                addSeenToDatabase(dataSnapShotKey);
            }
        }
    }

    private void addSeenToDatabase(String dataSnapShotKey) {
        FirebaseDatabase.getInstance(app)
                .getReference()
                .child(Constants.FIREBASE_DB_MESSAGES)
                .child(roomId)
                .child(dataSnapShotKey)
                .child("seen")
                .setValue(true);
    }


    // MICCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC

    MediaRecorder myAudioRecorder = new MediaRecorder();
    String outputFile = "";
    String fileName;
    RecordButton recordButton;

    private void setupMic() {
        RecordView recordView = (RecordView) findViewById(R.id.record_view);
        recordButton = (RecordButton) findViewById(R.id.record_button);

        //IMPORTANT
        recordButton.setRecordView(recordView);

        recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                //Start Recording..
                try {
                    startRecording();
                    mMessageEditText.setVisibility(View.INVISIBLE);
                    mUploadImage.setVisibility(View.INVISIBLE);
                } catch (IllegalStateException e) {
                    mMessageEditText.setVisibility(View.VISIBLE);
                    mUploadImage.setVisibility(View.VISIBLE);
                } catch (IllegalArgumentException e) {
                    mMessageEditText.setVisibility(View.VISIBLE);
                    mUploadImage.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancel() {
                //On Swipe To Cancel
                stopRecording();
                mMessageEditText.setVisibility(View.VISIBLE);
                mUploadImage.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFinish(long recordTime) {
                //Stop Recording..
//                String time = getHumanTimeText(recordTime);
                Log.d("RecordView", "onFinish");
                mMessageEditText.setVisibility(View.VISIBLE);
                mUploadImage.setVisibility(View.VISIBLE);
                stopRecording();
                if ((recordTime / 1000) > 120) {
                    Toast.makeText(MessagingActivity.this, getString(R.string.max_voice_note_2_mins), Toast.LENGTH_SHORT).show();
                } else {
                    uploadFileToAmazonS3(outputFile, fileName);
                }
                Log.d("RecordTime", recordTime + "SF");
            }

            @Override
            public void onLessThanSecond() {
                //When the record time is less than One Second
                Log.d("RecordView", "onLessThanSecond");
                mMessageEditText.setVisibility(View.VISIBLE);
            }
        });

        recordView.setOnBasketAnimationEndListener(new OnBasketAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                mMessageEditText.setVisibility(View.VISIBLE);
                Log.d("RecordView", "Basket Animation Finished");
            }
        });
        recordView.setCancelBounds(8);//dp

        //disable Sounds
        recordView.setSoundEnabled(true);

        //prevent recording under one Second (it's false by default)
        recordView.setLessThanSecondAllowed(false);

        //set Custom sounds onRecord
        //you can pass 0 if you don't want to play sound in certain state
        recordView.setCustomSounds(R.raw.record_start, R.raw.record_finished, 0);

     /*   //change slide To Cancel Text Color
        recordView.setSlideToCancelTextColor(Color.parseColor("#ff0000"));
        //change slide To Cancel Arrow Color
        recordView.setSlideToCancelArrowColor(Color.parseColor("#ff0000"));
        //change Counter Time (Chronometer) color
        recordView.setCounterTimeColor(Color.parseColor("#ff0000"));*/

    }

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_RECORD_AUDIO_FILE_PERMISSION = 201;
    private String[] recordPermissions = {Manifest.permission.RECORD_AUDIO};

    void startRecording() {
        if (ContextCompat.checkSelfPermission(MessagingActivity.this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, recordPermissions, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            fileName = FirebaseDatabase.getInstance(app).getReference().push().getKey() + "_record.3gp";
            outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
            myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            myAudioRecorder.setOutputFile(outputFile);

            try {
                myAudioRecorder.prepare();
                myAudioRecorder.start();
            } catch (IllegalStateException ise) {
                // make something ...
            } catch (IOException ioe) {
                // make something
            }
        }

    }

    void stopRecording() {
        myAudioRecorder.stop();
        myAudioRecorder.release();
        myAudioRecorder = new MediaRecorder();
    }

    int repeatedCount = 0;

    void uploadFileToAmazonS3(String fileOutput, String nameOfFile) {
        if (ContextCompat.checkSelfPermission(MessagingActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    , REQUEST_RECORD_AUDIO_FILE_PERMISSION);
        } else {
            Context mContext = this;
            File file = new File(fileOutput);
            Uri fileUri = Uri.fromFile(file);
            String uniqueName = FirebaseDatabase.getInstance(app).getReference().push().getKey();

            String nameWithExtentsion = file.getName() + uniqueName + "." + getFileExtension(mContext, fileUri);

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
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
                        file.delete();
                        repeatedCount = 0;
                        sendAudioMessage(downloadUri.toString());


                    } else {
                        // Handle failures
                        // ...
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (repeatedCount < 3) {
                        uploadFileToAmazonS3(fileOutput, nameOfFile);
                        Toast.makeText(MessagingActivity.this, getString(R.string.failed_to_send_voice_note), Toast.LENGTH_SHORT).show();
                    }
                    repeatedCount++;
                }
            });

        }
    }


    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToUploadFileAccepted = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case REQUEST_RECORD_AUDIO_FILE_PERMISSION:
                permissionToUploadFileAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case MY_PERMISSION_SOURCE_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImageIntent(ADD_IMAGE);
                } else {
                    //do something like displaying a message that he didn`t allow the app to access gallery and you wont be able to let him select from gallery
                    Toast.makeText(MessagingActivity.this, getString(R.string.permission_rejected), Toast.LENGTH_LONG).show();
                }
                break;
        }
        if (!permissionToRecordAccepted)
            Toast.makeText(MessagingActivity.this, getString(R.string.couldnot_record_voice_note), Toast.LENGTH_SHORT).show();
        if (!permissionToUploadFileAccepted)
            Toast.makeText(MessagingActivity.this, getString(R.string.couldnot_upload_voice_note), Toast.LENGTH_SHORT).show();

    }


    ValueEventListener listener;

    private void getUserStatus(String userID) {
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    switch (dataSnapshot.getValue(Integer.class)) {
                        case 0:
                            mStatus.setText(getString(R.string.offline));
                            mStatus.setTextColor(getResources().getColor(R.color.cherry_red));
//                            mStatus.setVisibility(View.GONE);
                            break;
                        case 1:
                            mStatus.setText(getString(R.string.online));
                            mStatus.setTextColor(getResources().getColor(R.color.white));
                            mStatus.setVisibility(View.VISIBLE);
                            break;
                        default:
                            mStatus.setText(getString(R.string.unavailable));
                            mStatus.setTextColor(getResources().getColor(R.color.cherry_red));
                            mStatus.setVisibility(View.GONE);
                            break;
                    }
                } catch (DatabaseException e) {
                    if (dataSnapshot.getValue().equals(currentUser.getFirebase_uid())) {
                        mStatus.setText(getString(R.string.typing));
                        mStatus.setTextColor(getResources().getColor(R.color.white));
                        mStatus.setVisibility(View.VISIBLE);
                    } else {
                        mStatus.setText(getString(R.string.online));
                        mStatus.setTextColor(getResources().getColor(R.color.white));
                        mStatus.setVisibility(View.VISIBLE);
                    }
                } catch (NullPointerException e) {
                    mStatus.setText(getString(R.string.online));
                    mStatus.setTextColor(getResources().getColor(R.color.white));
                    mStatus.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };


        if (userID != null && listener != null) {
            FirebaseDatabase.getInstance(app).getReference().child(Constants.FIREBASE_DB_ONLINE_STATUS)
                    .child(userID).addValueEventListener(listener);
        }
    }


}






