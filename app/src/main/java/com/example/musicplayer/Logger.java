package com.example.musicplayer;

import android.os.Environment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {

    private static final String LOGGING_DIRECTORY = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/MusicPlayerLogs";
    private static final String LOG_EXTENSION = ".txt";
    private static final String LOG_PREFIX = "crashlog";
    private static final boolean enableLogging = true; // set true to enable error logging, false to disable

    public static void logException(Exception exception){
        logException(exception, null);
    }

    public static void logException(Exception exception, String prefix){
        if (enableLogging) {
            try {
                File directory = new File(LOGGING_DIRECTORY);
                if (!directory.exists()) {
                    directory.mkdir();
                }
                long exceptionTime = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM-dd-yyyy-HH-mm", Locale.US);
                Date exceptionDate = new Date(exceptionTime);

                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                exception.printStackTrace(printWriter);
                String exceptionString = stringWriter.toString();

                String filename = LOGGING_DIRECTORY + '/' + LOG_PREFIX + "_";
                if (prefix != null) {
                    filename += prefix + "_" + sdf.format(exceptionDate) + "-" + System.currentTimeMillis() + LOG_EXTENSION;
                } else {
                    filename += sdf.format(exceptionDate) + "-" + System.currentTimeMillis() + LOG_EXTENSION;
                }

                BufferedWriter writer = new BufferedWriter(new FileWriter(filename, false));
                writer.append(exceptionString);
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            exception.printStackTrace();
        }
    }
}