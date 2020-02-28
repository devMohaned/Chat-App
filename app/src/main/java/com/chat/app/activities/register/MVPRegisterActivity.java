package com.chat.app.activities.register;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chat.app.utils.AppUtils;
import com.chat.app.utils.Constants;
import com.chat.app.R;
import com.chat.app.activities.login.MVPLoginActivity;
import com.chat.app.model.User;
import com.chat.app.presenters.RegisterPresenterMVP;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MVPRegisterActivity extends AppCompatActivity implements RegisterPresenterMVP.View {
    private static final String TAG = "Reg_activity";
    Context mContext = MVPRegisterActivity.this;

    @BindView(R.id.ID_reg_first_name)
    EditText firstName;
    @BindView(R.id.ID_reg_last_name)
    EditText lastName;
    @BindView(R.id.ID_reg_email)
    EditText email;
    @BindView(R.id.ID_reg_password)
    EditText passwordEditText;
    @BindView(R.id.ID_reg_register_button)
    Button registerButton;
    @BindView(R.id.ID_reg_progress_bar)
    ProgressBar progressBar;

    private RegisterPresenterMVP presenter;

    @BindView(R.id.ID_setting_up_your_account)
    TextView settingUpAccount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.ID_register_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_left_arrow);

        setUpWidgits();
        presenter = new RegisterPresenterMVP(this);
        setupFirebaseAuth();

    }


    private void setUpWidgits() {
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRegistering();
            }
        });

        firstName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String errorOrNoError = AppUtils.Validators.validateName(mContext, editable.toString());
                checkForError(firstName, errorOrNoError);
            }
        });


        lastName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String errorOrNoError = AppUtils.Validators.validateName(mContext, editable.toString());
                checkForError(lastName, errorOrNoError);
            }
        });

        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String errorOrNoError = AppUtils.Validators.validateEmail(mContext, editable.toString());
                checkForError(email, errorOrNoError);
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String errorOrNoError = AppUtils.Validators.validatePassword(mContext, editable.toString());
                checkForError(passwordEditText, errorOrNoError);
            }
        });


        email.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if ((actionId == EditorInfo.IME_ACTION_DONE)) {
                    AppUtils.hideSoftKeyboard(MVPRegisterActivity.this);
                    onRegistering();
                    return true;

                }
                return false;
            }
        });


    }


    private void checkForError(EditText editText, String error) {
        if (!error.equals(Constants.ERROR_FREE)) {
            editText.setError(error);
        } else {
            editText.setError(null);
        }
    }


    private void onRegistering() {

        // obtain & convert everyText inserted in the userName/email/password/password confirmation into a String type.
        String emailText = email.getText().toString();
        String userPassword = passwordEditText.getText().toString();

        // Several condition 1/ if username is empty
        if (firstName.getText().toString().equals("")) {
            firstName.setError(getString(R.string.empty_user_name));
        } else if (firstName.getText().toString().length() <= 2) {
            firstName.setError(getString(R.string.small_name));
        } else if (firstName.getText().toString().length() > 20) {
            firstName.setError(getString(R.string.large_name));
        } else {
            firstName.setError(null);
        }

        // if email is empty
        if (emailText.matches("")) {
            email.setError(getString(R.string.empty_email));
        }
        // if userpassword is empty
        else if (userPassword.contains(" ")) {
            passwordEditText.setError(getString(R.string.password_cannot_have_space));
        }
        // if confirmpassword is empty
        // if userpassword is empty
        else if (userPassword.matches("")) {
            passwordEditText.setError(getString(R.string.password_is_empty));
            //   Toast.makeText(Registeration.this, "You cannot leave Passwords empty.", Toast.LENGTH_LONG).show();
        }
        // if confirmpassword is empty
        else if (userPassword.trim().length() < 8) {
            passwordEditText.setError(getString(R.string.password_must_be_above_eight));
        }
        // if all the above is wrong(Passwords are not matched), thus:

        if (lastName.getText().toString().equals("")) {
            lastName.setError(getString(R.string.empty_user_name));
        } else if (lastName.getText().toString().length() <= 2) {
            lastName.setError(getString(R.string.small_name));
        } else if (lastName.getText().toString().length() > 20) {
            lastName.setError(getString(R.string.large_name));
        } else {
            lastName.setError(null);
        }


        if (firstName.getText().toString().length() > 2 && firstName.getText().toString().length() < 20
                && lastName.getText().toString().length() > 2 && lastName.getText().toString().length() < 20
                && emailText.trim().length() > 0
                && userPassword.trim().length() > 7) {


            registerButton.setEnabled(false);
            AppUtils.hideSoftKeyboard(MVPRegisterActivity.this);
            User newUser = new User();
            newUser.setEmail(email.getText().toString());
            newUser.setPassword(userPassword);
            newUser.setName(firstName.getText().toString() + " " + lastName.getText().toString());

            showProgressBar();
            presenter.performRegisterProcess(newUser);
        }
    }


    /*
       ------------------------------------ Firebase ---------------------------------------------
        */
    private FirebaseAuth mAuth;

    private void setupFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void registerUser(User user) {
        registerButton.setEnabled(false);
        registerNewEmail(user.getEmail(), user.getPassword(), user);
    }

    @Override
    public void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);

    }

    @Override
    public void hideProgressBar() {
        progressBar.setVisibility(View.GONE);

    }


    void settingUpAccountViewsVisible() {
        settingUpAccount.setVisibility(View.VISIBLE);
    }

    void settingUpAccountViewsInVisible() {
        settingUpAccount.setVisibility(View.GONE);
    }

    /*
     ------------------------------------ Firebase ---------------------------------------------
      */


    private void registerNewEmail(final String email, final String password, final User registeredUser) {
        settingUpAccountViewsVisible();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // If sign up fails, display a message to the user. If sign up succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(MVPRegisterActivity.this, R.string.auth_register_failed,
                                    Toast.LENGTH_LONG).show();
                            registerButton.setEnabled(true);
                            settingUpAccount.setVisibility(View.VISIBLE);
                            settingUpAccount.setText(getString(R.string.registered_failed_try_again));
                        } else if (task.isSuccessful()) {


                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(registeredUser.getName()).build();
                            firebaseUser.updateProfile(profileUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Create a new user with a first and last name
                                    Map<String, Object> user = new HashMap<>();
                                    user.put("name", firebaseUser.getDisplayName());
                                    user.put("email", firebaseUser.getEmail());
                                    user.put("profile_image", firebaseUser.getPhotoUrl());
                                    user.put("firebase_uid", firebaseUser.getUid());

                                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                                    db.collection("users").document(firebaseUser.getUid())
                                            .set(user);
                                }
                            });

                            new android.os.Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Do something after 100ms
                                    Intent intent = new Intent(MVPRegisterActivity.this, MVPLoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            }, 1000);


                        }
                        settingUpAccountViewsInVisible();
                    }
                });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                supportFinishAfterTransition();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}

