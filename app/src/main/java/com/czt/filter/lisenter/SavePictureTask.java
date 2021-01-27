package com.czt.filter.lisenter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SavePictureTask extends AsyncTask<Bitmap, Integer, Bitmap> {

    private Context mContext;
    private File mFile;
    private BitmapListener listener;
    public interface BitmapListener {
        void getBitmap(Bitmap bitmap);
    }

    public SavePictureTask(Context context, File file, BitmapListener listener){
        this.mContext = context;
        this.mFile = file;
        this.listener = listener;
    }

    @Override
    protected Bitmap doInBackground(Bitmap... bitmaps) {
        bitmap2File(mFile.getAbsolutePath(), 100, bitmaps[0]);
        return bitmaps[0];
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        listener.getBitmap(bitmap);
    }

    /**
     * Bitmap转换为文件
     * @param toFile
     * @param quality
     * @param bitmap
     * @return
     */
    public File bitmap2File(String toFile, int quality, Bitmap bitmap) {
        File captureFile = new File(toFile);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(captureFile);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return captureFile;
    }

}