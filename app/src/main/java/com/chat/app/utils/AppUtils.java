package com.chat.app.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.inputmethod.InputMethodManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import com.chat.app.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * Created by bestway on 09/07/2018.
 */

public class AppUtils {

    public static boolean isKeyboardShown(Activity activity) {
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                if (imm.isAcceptingText()) {
                    //            writeToLog("Software Keyboard was shown");
                    return true;
                } else {
                    //            writeToLog("Software Keyboard was not shown");
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }

    }


    public static class Validators {
        public static String validateEmail(Context context, String email) {
            if (email.contains("@") && email.contains(".")) {
                return Constants.ERROR_FREE;
            } else {
                return context.getString(R.string.enter_valid_email);
            }
        }


        public static String validateName(Context context, String name) {
            if (name.length() > 1 && name.length() <= 32) {
                return Constants.ERROR_FREE;
            } else {
                return context.getString(R.string.name_is_not_valid);
            }
        }

        public static String validatePassword(Context context, String password) {
            if (password.length() >= 8 && !password.contains(" ")) {
                return Constants.ERROR_FREE;
            } else if (password.length() < 8) {
                return context.getString(R.string.password_is_too_small);
            } else {
                return context.getString(R.string.invalid_password);
            }
        }

        public static String validatePhone(Context context, String phone) {
            if (phone.length() == 11) {
                return Constants.ERROR_FREE;
            } else {
                return context.getString(R.string.invalid_phone);
            }
        }


    }

    public static void hideSoftKeyboard(Activity activity) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e) {
//            Log.d(activity.toString(), "No Keyboard Found " + e.toString());
        }
    }


    public static String generateUniqueFileName() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        String generatedString = buffer.toString();
        return generatedString;
    }


    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isStringNull(String string) {
        if (string.equals("")) {
            return true;
        } else {
            return false;
        }
    }


    public static String getReadableDate(Context context, long timeInMillseconds) {
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
            date = context.getString(R.string.yesterday) + " " + getTimeInReadableFormate(timeInMillseconds);
        } else if (currentTime - timeInMillseconds <= 86400000) {
            date = /*getString(R.string.today) + */getTimeInReadableFormate(timeInMillseconds);
        } else {
            String[] months = context.getResources().getStringArray(R.array.months);
            String month = months[mMonth];
            date = month + " " + mDay + ", " + mYear;
        }

        return date;


    }

    private static String getTimeInReadableFormate(long timeInMilliSeconds) {
        String dateFormat = "h:mm a";
// Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMilliSeconds);
        return formatter.format(calendar.getTime());
    }

    public static boolean isInitialized = false;
    public static void handlingSecondFirebaseDatabase(Context context) {
        if(!isInitialized) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:1049243104908:android:cc76eee8d7609b319f6da9") // Required for Analytics
                    .setApiKey("AIzaSyA1041zlYDCihkW1mpaDMnpRPVkrdqX3aM") // Required for Auth.
                    .setDatabaseUrl("https://chat-app-2-for-chatting-msgs.firebaseio.com/") // Required for RTDB.
                    .build();
            FirebaseApp.initializeApp(context /* Context */, options, "secondary");
            isInitialized = true;
        }
    }


}
