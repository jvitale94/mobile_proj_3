package com.example.jakevitale.mobile_project_3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    Button btnCamera;
    Button showPic;
    Button postPic;
    Button getPic;
    Bitmap b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        showPic = (Button) findViewById(R.id.showPic);
        postPic = (Button) findViewById(R.id.postPic);
        getPic = (Button) findViewById(R.id.getPic);
        imageView = (ImageView) findViewById(R.id.imageView);

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 0);
            }
        });

        showPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 1);
            }
        });

        postPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postThePic();
            }
        });

        getPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPic();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0) {
            Bundle extras = data.getExtras();
            try {
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                imageView.setImageBitmap(imageBitmap);
                tryToWrite(imageBitmap);
            } catch (Exception e) {
                System.out.println("ERROR IN WRITING");
                imageView.setImageResource(android.R.drawable.presence_video_away);
            }
        } else if (requestCode == 1) {
            Uri selectedImage = data.getData();
            Bitmap bitmapImage = null;
            try {
                bitmapImage = decodeBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            // Show the Selected Image on ImageView
            imageView.setImageBitmap(bitmapImage);
        }
    }

    public void postThePic() {
        Bitmap bm = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
        byte[] b = baos.toByteArray();

        String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);

        DateFormat df = new SimpleDateFormat("yyMMddHHmmssZ");
        String date = df.format(Calendar.getInstance().getTime());
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put(date, encodedImage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //This is the picture in JSON stored as a String
        final String json = jsonObj.toString();

        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String data = "";
                HttpURLConnection httpURLConnection = null;
                try {
                    //This is my computers IP and port I chose to open. Change to another server if needed
                    URL url = new URL("http://148.85.255.58:4810");

                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("POST");

                    DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                    wr.writeBytes("PostData=" + params[0]);
                    wr.flush();
                    wr.close();

                    InputStream in = httpURLConnection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(in);

                    int inputStreamData = inputStreamReader.read();
                    while (inputStreamData != -1) {
                        char current = (char) inputStreamData;
                        inputStreamData = inputStreamReader.read();
                        data += current;
                    }

                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
                return data;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Log.e("TAG", s);
            }
        }.execute(json);
    }

    public void getPic() {
        System.out.println("GETTING PIC");

        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String str = null;
                try {
                    URL url = new URL("http://148.85.255.58:4810/Output.txt");
                    BufferedReader reader = null;
                    StringBuilder builder = new StringBuilder();
                    try {
                        reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                        for (String line; (line = reader.readLine()) != null;) {
                            builder.append(line.trim());
                        }
                    } finally {
                        if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
                    }
                    str = builder.substring(0);
                    //System.out.println(str);
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return str;
            }

            //This should work, but the encoding for some reason does not transfer back to the app correctly
            //Instead I just print out the encoded string
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                System.out.println(s.substring(31));
                byte[] decodedString = Base64.decode(s.substring(31), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imageView.setImageBitmap(decodedByte);
            }
        }.execute("1");
    }

    public void tryToWrite(Bitmap imageBitmap) {
        b = imageBitmap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            DateFormat df = new SimpleDateFormat("yyMMddHHmmssZ");
            String date = df.format(Calendar.getInstance().getTime());
            MediaStore.Images.Media.insertImage(getContentResolver(), b, "PIC " + date, date);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                DateFormat df = new SimpleDateFormat("yyMMddHHmmssZ");
                String date = df.format(Calendar.getInstance().getTime());
                MediaStore.Images.Media.insertImage(getContentResolver(), b, "PIC " + date, date);
            }
        }
    }

    public Bitmap decodeBitmap(Uri selectedImage) throws FileNotFoundException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        final int REQUIRED_SIZE = 100;

        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }
}
