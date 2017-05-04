package com.example.hp.speechproject;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.*;

public class MainActivity extends AppCompatActivity {

    private MediaRecorder mRecorder = null;
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String mFileName = null;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    // file,audiofile;
    private Button btnRecord,btnTone;

    int bufferSize;
    float[] audioBuffer;
    AudioRecord record;

    final int SAMPLE_RATE=44100;
    final int BUFFER_SIZE=7056;

    MediaPlayer mediaPlayer;
    final ArrayList<NoteFrequency> notes=new ArrayList<>();


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if(!permissionToRecordAccepted) finish();
    }

    @TargetApi(Build.VERSION_CODES.M)
    float[] recordAudio() {


        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT);
        Log.v("buffersize",bufferSize+"");

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE)
            bufferSize = SAMPLE_RATE * 2;

        audioBuffer = new float[440832];

        record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e("audio init", "Audio Record can't initialize!");

        }


        record.startRecording();

        Log.v("start", "Start recording");

        long floatReads = 0;

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime <= 10000) {

            int noOfFloat = record.read(audioBuffer, 0, audioBuffer.length, AudioRecord.READ_BLOCKING);
            floatReads += noOfFloat;

        }


        record.stop();
        Log.v("Read","Recording stopped. Samples read: "+ floatReads + " audiobuffer "+audioBuffer.length);
        return audioBuffer;




    }

    int toneCounter;
    float[] floatArray=null;
    int[] noteArray=null;
    float[] pitchArray=null;
    int lengthNoteArray=0;
    MediaPlayer mp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRecord=(Button) findViewById(R.id.btnRecord);
        btnTone=(Button)findViewById(R.id.btnTone);

        btnRecord.setText("Record audio");

        //file = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        /*
        if(!file.exists())
        file.mkdir();*/
        addAllNotesFrequency(notes);
        //mFileName= Environment.getExternalStorageDirectory().getAbsolutePath() + "/speech.mp3";

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        lengthNoteArray=0;
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                floatArray = recordAudio();
                McLeodMethod mpm = new McLeodMethod(SAMPLE_RATE, BUFFER_SIZE);
                int n;
                n = 1000;

                pitchArray = new float[(floatArray.length / n) + 1];
                noteArray=new int[(floatArray.length / n) + 3];

                Log.v("size of pitcharray",noteArray.length+" float "+floatArray.length);

                for (int i = 0; i < floatArray.length; i = i + n) {
                    float[] subArray = Arrays.copyOfRange(floatArray, i, i + n);
                    PitchDetectionResult result = mpm.getPitch(subArray);
                    float pitchInHertz = result.getPitch();
                    pitchArray[i / n] = pitchInHertz;


                        //search the corresponding note in the note array
                        int start = 0;
                        int end = notes.size() - 1;
                        int mid;
                        mid = start + end / 2;

                        while (mid != end && mid != start) {
                            if (notes.get(mid).frequency <= pitchInHertz) {
                                //         Log.e("fas",start+" "+end+" "+mid);
                                start = mid;
                                mid = (start + end) / 2;
                            } else {
                                //       Log.e("fasfas",start+" "+end+" "+mid);
                                end = mid;
                                mid = (start + end) / 2;
                            }
                        }
                        if (Math.abs(pitchInHertz - notes.get(start).frequency) < Math.abs(pitchInHertz - notes.get(end).frequency)) {
                            mid = start;
                        } else
                            mid = end;

                        Log.v("mid",""+mid);

                        int id = getResources().getIdentifier("note" + mid, "raw", getPackageName());
                        noteArray[i/n] = id;
                        //lengthNoteArray++;
                    }
                    Log.v("Note array length",""+noteArray.length);
                    //Log.v("Note array",""+noteArray[2200]);

                record.release();
            }
        });


        btnTone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                for(toneCounter=0;toneCounter<noteArray.length;toneCounter++) {

                            mp= MediaPlayer.create(getApplicationContext(),noteArray[toneCounter]);
                            mp.start();
                            long startTime=System.currentTimeMillis();
                            int time=0;
                            while(System.currentTimeMillis()-startTime<500)
                            {

                            }
                            mp.stop();
                            mp.release();
                }
            }

        });

        //(mFileName,SAMPLE_RATE, BUFFER_SIZE, 0);
        //new AndroidFFMPEGLocator(MainActivity.this);
      /* AudioDispatcher disp = AudioDispatcherFactory.fromPipe(mFileName,SAMPLE_RATE, BUFFER_SIZE, 0);


        PitchDetectionHandler pdh = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                float pitchInHertz = pitchDetectionResult.getPitch();
                Log.e("pitch",String.valueOf(pitchInHertz));
            }
        };*/





        /*AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.MPM, SAMPLE_RATE, BUFFER_SIZE, pdh);
        disp.addAudioProcessor(p);*/
    }

    void addAllNotesFrequency(ArrayList<NoteFrequency> notes){
        notes.add(new NoteFrequency(16.35,"C 0"));
        notes.add(new NoteFrequency(17.32,"C# 0"));
        notes.add(new NoteFrequency(18.35,"D 0"));
        notes.add(new NoteFrequency(19.45,"D# 0"));
        notes.add(new NoteFrequency(20.60,"E 0"));
        notes.add(new NoteFrequency(21.83,"F 0"));
        notes.add(new NoteFrequency(23.12,"F# 0"));
        notes.add(new NoteFrequency(24.50,"G 0"));
        notes.add(new NoteFrequency(25.96,"G# 0"));
        notes.add(new NoteFrequency(27.50,"A 0"));
        notes.add(new NoteFrequency(29.14,"A# 0"));
        notes.add(new NoteFrequency(30.87,"B 0"));
        notes.add(new NoteFrequency(32.70,"C 1"));
        notes.add(new NoteFrequency(34.65,"C# 1"));
        notes.add(new NoteFrequency(36.71,"D 1"));
        notes.add(new NoteFrequency(38.89,"D# 1"));
        notes.add(new NoteFrequency(41.20,"E 1"));
        notes.add(new NoteFrequency(43.65,"F 1"));
        notes.add(new NoteFrequency(46.25,"F# 1"));
        notes.add(new NoteFrequency(49.00,"G 1"));
        notes.add(new NoteFrequency(51.91,"G# 1"));
        notes.add(new NoteFrequency(55.00,"A 1"));
        notes.add(new NoteFrequency(58.27,"A# 1"));
        notes.add(new NoteFrequency(61.74,"B 1"));
        notes.add(new NoteFrequency(65.41,"C 2"));
        notes.add(new NoteFrequency(69.30,"C# 2"));
        notes.add(new NoteFrequency(73.42,"D 2"));
        notes.add(new NoteFrequency(77.78,"D# 2"));
        notes.add(new NoteFrequency(82.41,"E 2"));
        notes.add(new NoteFrequency(87.31,"F 2"));
        notes.add(new NoteFrequency(92.50,"F# 2"));
        notes.add(new NoteFrequency(98.00,"G 2"));
        notes.add(new NoteFrequency(103.83,"G# 2"));
        notes.add(new NoteFrequency(110.00,"A 2"));
        notes.add(new NoteFrequency(116.54,"A# 2"));
        notes.add(new NoteFrequency(123.47,"B 2"));
        notes.add(new NoteFrequency(130.81,"C 3"));
        notes.add(new NoteFrequency(138.59,"C# 3"));
        notes.add(new NoteFrequency(146.83,"D 3"));
        notes.add(new NoteFrequency(155.56,"D# 3"));
        notes.add(new NoteFrequency(164.81,"E 3"));
        notes.add(new NoteFrequency(174.61,"F 3"));
        notes.add(new NoteFrequency(185.00,"F# 3"));
        notes.add(new NoteFrequency(196.00,"G 3"));
        notes.add(new NoteFrequency(207.65,"G# 3"));
        notes.add(new NoteFrequency(220.00,"A 3"));
        notes.add(new NoteFrequency(233.08,"A# 3"));
        notes.add(new NoteFrequency(246.94,"B 3"));
        notes.add(new NoteFrequency(261.63,"C 4"));
        notes.add(new NoteFrequency(277.18,"C# 4"));
        notes.add(new NoteFrequency(293.66,"D 4"));
        notes.add(new NoteFrequency(311.13,"D# 4"));
        notes.add(new NoteFrequency(329.63,"E 4"));
        notes.add(new NoteFrequency(349.23,"F 4"));
        notes.add(new NoteFrequency(369.99,"F# 4"));
        notes.add(new NoteFrequency(392.00,"G 4"));
        notes.add(new NoteFrequency(415.30,"G# 4"));
        notes.add(new NoteFrequency(440.00,"A 4"));
        notes.add(new NoteFrequency(466.16,"A# 4"));
        notes.add(new NoteFrequency(493.88,"B 4"));
        notes.add(new NoteFrequency(523.25,"C 5"));
        notes.add(new NoteFrequency(554.37,"C# 5"));
        notes.add(new NoteFrequency(587.33,"D 5"));
        notes.add(new NoteFrequency(622.25,"D# 5"));
        notes.add(new NoteFrequency(659.25,"E 5"));
        notes.add(new NoteFrequency(698.46,"F 5"));
        notes.add(new NoteFrequency(739.99,"F# 5"));
        notes.add(new NoteFrequency(783.99,"G 5"));
        notes.add(new NoteFrequency(830.61,"G# 5"));
        notes.add(new NoteFrequency(880.00,"A 5"));
        notes.add(new NoteFrequency(932.33,"A# 5"));
        notes.add(new NoteFrequency(987.77,"B 5"));
        notes.add(new NoteFrequency(1046.50,"C 6"));
        notes.add(new NoteFrequency(1108.73,"C# 6"));
        notes.add(new NoteFrequency(1174.66,"D 6"));
        notes.add(new NoteFrequency(1244.51,"D# 6"));
        notes.add(new NoteFrequency(1318.51,"E 6"));
        notes.add(new NoteFrequency(1396.91,"F 6"));
        notes.add(new NoteFrequency(1479.98,"F# 6"));
        notes.add(new NoteFrequency(1567.98,"G 6"));
        notes.add(new NoteFrequency(1661.22,"G# 6"));
        notes.add(new NoteFrequency(1760.00,"A 6"));
        notes.add(new NoteFrequency(1864.66,"A# 6"));
        notes.add(new NoteFrequency(1975.53,"B 6"));
        notes.add(new NoteFrequency(2093.00,"C 7"));
        notes.add(new NoteFrequency(2217.46,"C# 7"));
        notes.add(new NoteFrequency(2349.32,"D 7"));
        notes.add(new NoteFrequency(2489.02,"D# 7"));
        notes.add(new NoteFrequency(2637.02,"E 7"));
        notes.add(new NoteFrequency(2793.83,"F 7"));
        notes.add(new NoteFrequency(2959.96,"F# 7"));
        notes.add(new NoteFrequency(3135.96,"G 7"));
        notes.add(new NoteFrequency(3322.44,"G# 7"));
        notes.add(new NoteFrequency(3520.00,"A 7"));
        notes.add(new NoteFrequency(3729.31,"A# 7"));
        notes.add(new NoteFrequency(3951.07,"B 7"));
        notes.add(new NoteFrequency(4186.01,"C 8"));
        notes.add(new NoteFrequency(4434.92,"C# 8"));
        notes.add(new NoteFrequency(4698.63,"D 8"));
        notes.add(new NoteFrequency(4978.03,"D# 8"));
        notes.add(new NoteFrequency(5274.04,"E 8"));
        notes.add(new NoteFrequency(5587.65,"F 8"));
        notes.add(new NoteFrequency(5919.91,"F# 8"));
        notes.add(new NoteFrequency(6271.93,"G 8"));
        notes.add(new NoteFrequency(6644.88,"G# 8"));
        notes.add(new NoteFrequency(7040.00,"A 8"));
        notes.add(new NoteFrequency(7458.62,"A# 8"));

    }
}
