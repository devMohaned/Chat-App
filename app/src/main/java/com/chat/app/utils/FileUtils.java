package com.chat.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;


import com.chat.app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils
{
    public static String getFileExtension(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    public static boolean validateInputFileName(Context context, String fileName) {

        if (TextUtils.isEmpty(fileName)) {
            Toast.makeText(context, context.getString(R.string.enter_file_name), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }


}
