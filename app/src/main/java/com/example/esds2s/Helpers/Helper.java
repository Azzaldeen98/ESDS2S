package com.example.esds2s.Helpers;

import static java.sql.DriverManager.println;

import android.util.Log;

import java.io.File;

public class Helper {

public static  void  deleteFile(String filePath)
{
        try {

            File file =new File(filePath);
            if (file != null && file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    println("File deleted successfully");
                } else {
                    println("Failed to delete file");
                }
            } else {
                println("File does not exist");
            }
        }catch (Exception e)
        {
            Log.e("Error",e.getMessage().toString());
        }
    }
}
