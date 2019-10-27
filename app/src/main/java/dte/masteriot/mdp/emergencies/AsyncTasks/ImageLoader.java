package dte.masteriot.mdp.emergencies.AsyncTasks;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import dte.masteriot.mdp.emergencies.Activities.MainActivity;

@SuppressLint("StaticFieldLeak")
public class ImageLoader extends AsyncTask<Integer, Void, Bitmap> {
    private MainActivity mainActivity;
    int pos;

    public ImageLoader(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected Bitmap doInBackground(Integer... params) {
        pos = params[0];
        String url = mainActivity.getCameraArrayList().get(pos).URL;

        URL imageUrl;
        Bitmap imagen = null;
        try{
            imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
            conn.connect();
            imagen = BitmapFactory.decodeStream(conn.getInputStream());
        }catch(IOException ex){
            ex.printStackTrace();
        }

        return imagen;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        mainActivity.setImagePosAndListener(bitmap, pos);
    }
}
