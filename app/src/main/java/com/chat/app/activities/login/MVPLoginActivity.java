package com.chat.app.activities.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.chat.app.utils.AppUtils;
import com.chat.app.R;
import com.chat.app.activities.messages.UserListMessagesActivity;
import com.chat.app.model.User;
import com.chat.app.presenters.LoginPresenterMVP;
import com.chat.app.activities.register.MVPRegisterActivity;
import com.chat.app.activities.register.ResetPasswordActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * A login screen that offers login via email/password.
 */
public class MVPLoginActivity extends AppCompatActivity implements LoginPresenterMVP.View {


    private EditText mEmail;
    private EditText mPassword;
    private LoginPresenterMVP presenter;
    private ProgressBar progressBar;
    private Button mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        setupWidgits();


    }


    private void setupWidgits() {
        presenter = new LoginPresenterMVP(this);
        progressBar = findViewById(R.id.ID_log_progress_bar);
        mEmail = (EditText) findViewById(R.id.ID_log_email);
        mPassword = (EditText) findViewById(R.id.ID_log_password);
        mLoginButton = (Button) findViewById(R.id.email_sign_in_button);
        TextView mDontHaveAccount = (TextView) findViewById(R.id.ID_donthaveAccount_register);
        mDontHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MVPLoginActivity.this, MVPRegisterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
        setupFirebaseAuth();

        mPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_DONE)) {
                    logIn();
                    return true;
                }
                return false;
            }
        });

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logIn();

            }
        });


        TextView resetPassword = findViewById(R.id.ID_reset_password);
        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MVPLoginActivity.this, ResetPasswordActivity.class);
                startActivity(intent);
            }
        });

    }

    private void logIn() {
        if (mEmail.getText().toString().length() > 0 && mPassword.getText().toString().length() > 0) {
            mLoginButton.setEnabled(false);
            AppUtils.hideSoftKeyboard(MVPLoginActivity.this);

            presenter.performLoginProcess(
                    new User(
                            String.valueOf(mEmail.getText()),
                            String.valueOf(mPassword.getText())));

        } else {
            Toast.makeText(this, getString(R.string.something_is_wrong), Toast.LENGTH_SHORT).show();
        }
    }


    //--------------------------------Firebase-----------------------
    // -------------------------------------Firebase---------------------------------

    private static final String TAG = "HomePage:";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;


    private void setupFirebaseAuth() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Intent intent = new Intent(MVPLoginActivity.this, UserListMessagesActivity.class);
                    startActivity(intent);
                    finish();


                } else {
                    // User is signed out
                    //            Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);


    }


    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void IsEmailVerified() {
        mLoginButton.setEnabled(true);
        Intent intent = new Intent(MVPLoginActivity.this, UserListMessagesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    @Override
    public void logsIn(final User user) {
        if (AppUtils.isStringNull(user.getEmail()) || AppUtils.isStringNull(user.getPassword())) {
            Toast.makeText(MVPLoginActivity.this, getString(R.string.all_fields_are_requried), Toast.LENGTH_LONG).show();
            mLoginButton.setEnabled(true);
        } else {
            showProgressBar();

            if (user.getEmail().contains("@")) {
                mAuth.signInWithEmailAndPassword(user.getEmail(), user.getPassword())
                        .addOnCompleteListener(MVPLoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (!task.isSuccessful()) {
                                    mLoginButton.setEnabled(true);
                                    Toast.makeText(MVPLoginActivity.this, getString(R.string.couldnot_sign_in), Toast.LENGTH_LONG).show();
                                } else {
                                    IsEmailVerified();
                                }
                                hideProgressBar();
                            }
                        });
            } else {
                mEmail.setError(getString(R.string.invalid_email));
            }
        }
    }

    @Override
    public void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }


}

