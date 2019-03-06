package com.example.user.teseshagdesctitization;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager manager;
    Button buttonStart;
    Button buttonStop;
    EditText editAlpha;
    EditText editK;
    boolean isRunning;
    final String TAG = "SensorLog";
    FileWriter writer;
    Button shareButton;
    Timer timer;
    private SensorData data = new SensorData();
    Sensor sensorAccel;
    Sensor sensorGiros;
    StringBuilder sb = new StringBuilder();
    TextView tvText;
    public String state = "DEFAULTG";
    EditText editTextShag;
    int v;
    float frequency;
    long t;
    float vx,vy,vz;
    float pxaf, pyaf, pzaf;
    float Sx, Sy, Sz;
    float f;
    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainp);
        editTextShag=(EditText)findViewById(R.id.editShag);
        editTextShag.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                v=Integer.parseInt(editable.toString());
                //frequency=Integer.parseInt(editable.toString());
            }
        });
        shareButton = (Button) findViewById(R.id.buttonShare);
        shareButton.setOnClickListener(new View.OnClickListener() {


                                           @Override
                                           public void onClick(View v) {
                                               share();
                                           }
                                       }
        );
        isRunning = false;
        //   tvText = (TextView) findViewById(R.id.tvText);
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStop = (Button) findViewById(R.id.buttonStop);
        editAlpha = (EditText) findViewById(R.id.editAlpha);
        editK = (EditText) findViewById(R.id.editK);

        buttonStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);

                try {
                    float alpha = Float.parseFloat(editAlpha.getText().toString());
                    float k = Float.parseFloat(editK.getText().toString());

                    data = new SensorData();
                    data.setParams(alpha, k);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Данные введены не верно", Toast.LENGTH_LONG).show();
                }

                File file = new File(getStorageDir(), "sensors.csv");
                if (file.exists())
                    file.delete();

                Log.d(TAG, "Writing to " + getStorageDir());
                try {
                    writer = new FileWriter(file);

                    //writer.write("TIME;ACC X;ACC Y;ACC Z;ACC XF;ACC YF;ACC ZF;GYR X; GYR Y; GYR Z; GYR XF; GYR YF; GYR ZF;\n");
                    //    writer.write("TIME;ACC X;ACC Y;ACC Z;ACC XF;ACC YF;ACC ZF;GYR X; GYR Y; GYR Z; GYR XF; GYR YF; GYR ZF;  VX);
                    writer.write("TIME; dT; ACC X;ACC Y;ACC Z;ACC XF;ACC YF;ACC ZF;GYR X; GYR Y; GYR Z; GYR XF; GYR YF; GYR ZF;  VX; VY; VZ; VxFiltr;  VyFiltr; VzFiltr; Sx; Sy; Sz; shag; f\n");

                } catch (IOException e) {
                    e.printStackTrace();
                }

           //     manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), v*1000);//было
          //      manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), v*1000);///было
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), (int) v*1000);//выносить
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), (int) v*1000);///
                isRunning = true;
                return true;
            }
        });

        buttonStop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);
                isRunning = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    manager.flush(MainActivity.this);
                }
                manager.unregisterListener(MainActivity.this);
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }

    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
    }

   // @Override
    public void onFlushCompleted(Sensor sensor) {
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.unregisterListener(listener);
        timer.cancel();
    }

    String format(float values[]) {
        return String.format("%1$.1f\t\t%2$.1f\t\t%3$.1f", values[0], values[1],
                values[2]);
    }

    void showInfo() {
        sb.setLength(0);
        sb.append("Accelerometer: " + format(valuesAccel))
                .append("\n\nAccel motion: " + format(valuesAccel))
                .append("\nAccel gravity : " + format(valuesGiroscope));

//        tvText.setText(sb);
    }

    @Override
    protected void onResume() {
        super.onResume();
        manager.registerListener(listener, sensorAccel, (int) v);
        manager.registerListener(listener, sensorGiros, (int) v);


        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //  showInfo();
                    }
                });
            }
        };

        timer.schedule(task, 0, 400);
    }

    float[] valuesAccel = new float[3];
    float[] valuesGiroscope = new float[3];


    SensorEventListener listener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent hardEvent) {
            long currentTime=System.currentTimeMillis();

                if (timestamp != 0) {
                    final float dT = (hardEvent.timestamp - timestamp) * NS2S;
                    for (int index = 0; index < 3; ++index) ;
                    {
                        vx += hardEvent.values[0] * dT;
                        Sx += vx * dT * 10000;
                        vy += hardEvent.values[1]*dT;
                        Sy += vy * dT * 10000;
                        vz += hardEvent.values[2]*dT;
                        Sz += vz * dT * 10000;
                    }
                }



                MySensorEvent event = new MySensorEvent(hardEvent,currentTime);
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        for (int i = 0; i < 3; i++) {
                            valuesAccel[i] = event.values[i];
                        }
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        for (int i = 0; i < 3; i++) {
                            valuesGiroscope[i] = event.values[i];
                        }
                        break;

                }
                timestamp = event.timestamp;
           // }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

    };

    @Override
    public void onSensorChanged(SensorEvent event) {


        if (isRunning) {
            long currentTime=System.currentTimeMillis();
            if(t==0){
                t=currentTime;

            }
            MySensorEvent evt=new MySensorEvent(event, currentTime);
            long s=currentTime-t;

            //final float dT = (long) ((event.timestamp - timestamp) * NS2S);

            try {
                switch (evt.sensor.getType()) {
                    case Sensor.TYPE_GYROSCOPE:
                        //evt.timestamp=date;

                        // Calendar cal = Calendar.getInstance();
                        // cal.setTimeInMillis( System.currentTimeMillis() / 1000000L);
                        // String date = DateFormat.format("dd-MM-yyyy hh:mm:ss", cal).toString();

//                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
//                        String date = sdf.format(new Date());
//
                        data.setGyr(evt);
                        if (data.isAccDataExists()) {
                            writer.write(data.getStringData(s));

                            //data.clear();
                        }

                        break;
                    case Sensor.TYPE_ACCELEROMETER:
                        //    evt.timestamp=date;
                        data.setAcc(evt);
                        if (data.isGyrDataExists()) {
                            writer.write(data.getStringData(s));

                        }
                        break;

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void share() {
        File dir = getExternalFilesDir(null);
        File zipFile = new File(dir, "accel.zip");
        if (zipFile.exists()) {
            zipFile.delete();
        }
        File[] fileList = dir.listFiles();
        try {
            zipFile.createNewFile();
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
            for (File file : fileList) {
                zipFile(out, file);
            }
            out.close();
            sendBundleInfo(zipFile);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Can't send file!", Toast.LENGTH_LONG).show();
        }
    }

    private static void zipFile(ZipOutputStream zos, File file) throws IOException {
        zos.putNextEntry(new ZipEntry(file.getName()));
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[10000];
        int byteCount = 0;
        try {
            while ((byteCount = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, byteCount);
            }
        } finally {
            safeClose(fis);
        }
        zos.closeEntry();
    }

    private static void safeClose(FileInputStream fis) {
        try {
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBundleInfo(File file) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file));
        startActivity(Intent.createChooser(emailIntent, "Send data"));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
//}

    class SensorData {


        private MySensorEvent gyrEvent;
        private MySensorEvent accEvent;


        private float xaf, yaf, zaf;
        private float xgf, ygf, zgf;
        private float alpha = 0.05f;
        private float k = 0.5f;

        float pxaf, pyaf, pzaf;
        float shag;
        float timestamp;
        private MySensorEvent prefaccEvent;
        private Time prefTime;///++
        private float vxfit, vyfit, vzfit;
        private float Sxfit, Syfit, Szfit;
        float vx,vy,vz;
        public void setParams(float alpha, float k) {
            this.alpha = alpha;
            this.k = k;
        }

        public void setGyr(MySensorEvent gyrEvent) {
            this.gyrEvent = gyrEvent;
        }

        public void setAcc(MySensorEvent accEvent) {
            this.prefaccEvent=this.accEvent;
            this.accEvent = accEvent;
            this.prefTime=prefTime;//++
        }

        public boolean isAccDataExists() {
            return accEvent != null;
        }

        public boolean isGyrDataExists() {
            return gyrEvent != null;
        }

        public void clear() {
            gyrEvent = null;
            accEvent = null;
        }
        public String getStringData(long date) { //update
//
            xaf = xaf + alpha * (accEvent.values[0] - xaf);
            yaf = yaf + alpha * (accEvent.values[1] - yaf);
            zaf = zaf + alpha * (accEvent.values[2] - zaf);
//
            xgf = ((1-k)*gyrEvent.values[0])+(k*accEvent.values[0]);
            ygf = ((1-k)*gyrEvent.values[1])+(k*accEvent.values[1]);
            zgf = ((1-k)*gyrEvent.values[2])+(k*accEvent.values[2]);
            float dT = 0;
            float dTS =0;
            if(this.prefaccEvent!=null){
                dT=this.accEvent.time-this.prefaccEvent.time;
         //   };//миллисекунды

               dTS= (float) (dT/1000.0); //сек Шаг
                   /// if (timestamp != 0) {


                        for (int index = 0; index < 3; ++index) ;
                        {
                           // vx += (accEvent.values[0]+prefaccEvent.values[0])/2.0 * dTS;
                            if(dTS!=0) {
                                vx = (float) ((accEvent.values[0] + prefaccEvent.values[0]) / 2.0* dTS);
                               // vx = (float) ((accEvent.values[0]));

                                Sx = vx * dTS;

                                vy += (accEvent.values[1] + prefaccEvent.values[1]) / 2.0 * dTS;
                                Sy += vy * dTS;

                                vz += (accEvent.values[2] + prefaccEvent.values[2]) / 2.0 * dTS;
                                Sz += vz * dTS;

                                vxfit += ((xaf + pxaf) / 2.0) * dTS;
                                Sxfit += vxfit * dTS;

                                vyfit += ((yaf + pyaf) / 2.0) * dTS;
                                Syfit += vyfit * dTS;

                                vzfit += ((zaf + pzaf) / 2.0) * dTS;
                                Szfit += vzfit * dTS;

                            }

                        }
                    }
            pxaf=xaf;
            pyaf=yaf;
            pzaf=zaf;
            return String.format(
                    "%d; " + " %f;"+
                            " %f; %f; %f;" +
                            " %f; %f; %f;" +
                            " %f; %f; %f;" +
                            " %f; %f; %f;" +
                            " %f; %f; %f; " +
                            " %f; %f; %f;" +
                            " %f; %f; %f;" +
                            " %f; %f; %f \n",
                     date, dTS,
                    accEvent.values[0], accEvent.values[1], accEvent.values[2],
                    xaf,yaf,zaf,
                    gyrEvent.values[0], gyrEvent.values[1], gyrEvent.values[2],
                    xgf, ygf, zgf,
                    vx,vy,vz,
                    vxfit, vyfit, vzfit,
                    Sx, Sy, Sz,
                    Sxfit, Syfit, Szfit);

        }
    }
}