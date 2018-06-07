package com.xiaomi.xmsf.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.xiaomi.xmsf.R;

import java.io.File;
import java.io.IOException;

import me.pqpo.librarylog4a.Level;
import me.pqpo.librarylog4a.Log4a;
import me.pqpo.librarylog4a.Logger;
import me.pqpo.librarylog4a.appender.AndroidAppender;
import me.pqpo.librarylog4a.appender.FileAppender;
import top.trumeet.common.Constants;

/**
 * Created by Trumeet on 2017/8/28.
 *
 * @author Trumeet
 */

public class LogUtils {

    public static void configureLog(Context context) {
        AndroidAppender.Builder androidBuild = new AndroidAppender.Builder();

        String log_path = getLogFile(context);
        Log4a.d("Log", log_path);
        if (!new File(log_path).exists()) {
            try {
                new File(log_path).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileAppender.Builder fileBuild = new FileAppender.Builder(context)
                .setLevel(Level.INFO)
                .setLogFilePath(log_path);

        Logger logger = new Logger.Builder()
                .enableAndroidAppender(androidBuild)
                .enableFileAppender(fileBuild)
                .create();

        Log4a.setLogger(logger);
    }

    public static String getLogFile(Context context) {
        return context.getFilesDir().getAbsolutePath().concat(Constants.LOG_FILE);
    }

    public static void clearLog(Context context) {
        File file = new File(getLogFile(context));
        if (!file.exists() || file.length() <= 0) {
            Toast.makeText(context, R.string.log_none
                    , Toast.LENGTH_SHORT).show();
            return;
        }
        try {

            java.io.FileWriter fileWriter = new java.io.FileWriter(file);
            fileWriter.write("");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.common_err,
                    e.getMessage()), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        Toast.makeText(context, context.getString(R.string.log_clear_done), Toast.LENGTH_SHORT).show();
    }

    public static void shareFile(Context context) {
        File file = new File(getLogFile(context));
        if (!file.exists() || file.length() <= 0) {
            Toast.makeText(context, R.string.log_none
                    , Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent(Intent.ACTION_SEND);
        result.setType("text/*");
        try {
            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    Constants.AUTHORITY_FILE_PROVIDER,
                    file);
            result.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            result.putExtra(Intent.EXTRA_STREAM, fileUri);
            context.startActivity(result);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.log_share_error,
                    e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }
}
