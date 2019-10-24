package dte.masteriot.mdp.emergencies.Activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import dte.masteriot.mdp.emergencies.R;

@SuppressLint("StaticFieldLeak")
class ImageLoader extends AsyncTask<Integer, Void, Bitmap> {
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
    protected void onPostExecute(Bitmap result) {
        ImageView im = mainActivity.findViewById(R.id.imageView);
        im.setImageResource(R.mipmap.upmiot); //To check how to show this image bigger
        im.setImageBitmap(result);
        im.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent (v.getContext(), MapsActivity.class);
                Bundle args = new Bundle();
                args.putParcelable("coordinates", mainActivity.getCameraArrayList().get(pos).position);
                args.putString("cameraName", mainActivity.getCameraArrayList().get(pos).name);
                args.putDouble("valCont", mainActivity.getCameraArrayList().get(pos).valCont);
                intent.putExtra("bundle",args);
                mainActivity.startActivity(intent);
            }
        });
    }
}
