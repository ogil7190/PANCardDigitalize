package com.bluebulls.apps.phoenix;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Button grabPic;
    private TextRecognizer recognizer;
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        grabPic = (Button) findViewById(R.id.grabPic);
        grabPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                grabPic();
            }
        });
    }


    private void grabPic(){
        if(checkPermission()){
            takePic();
        }
        else
            requestPermission();
    }

    public static final int PERM_STORAGE_REQ_CODE = 7190;
    private void requestPermission(){
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERM_STORAGE_REQ_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERM_STORAGE_REQ_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePic();
            } else {
                Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Uri imageUri;
    public static final int PHOTO_REQ_CODE = 7160;

    private void takePic(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "pic.jpg");
        imageUri = Uri.fromFile(photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, PHOTO_REQ_CODE);
    }
    private ProgressDialog dialog;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PHOTO_REQ_CODE && resultCode == RESULT_OK) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                if (recognizer.isOperational() && bitmap != null) {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> textBlocks = recognizer.detect(frame);
                    String blocks = "";
                    ArrayList<String> lines = new ArrayList<>();
                    for (int index = 0; index < textBlocks.size(); index++) {
                        //extract scanned text blocks here
                        TextBlock block = textBlocks.valueAt(index);
                        blocks = blocks + block.getValue() + "\n" + "\n";
                        for (Text line : block.getComponents()) {
                            //extract scanned text lines here
                            String val = line.getValue();
                            if(val.equals(val.toUpperCase()))
                                lines.add(val);
                        }
                    }
                    parseData(lines);
                    lines.clear();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT).show();
                Log.e(TAG, e.toString());
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        recognizer.release();
    }

    private boolean isPAN  = false, isDOB = false, isNAME = false, isFNAME = false;
    private String PAN = "", DOB = "", NAME = "", FNAME = "";

    private void parseData(ArrayList<String> lines){
        for(String line : lines){
            if(!isPAN){
                if(line.matches("((?=.*\\d)(?=.*[A-Z]).{5,})")){
                    PAN = line;
                    isPAN = true;
                    continue;
                }
            }

            if(!isDOB){
                if(line.contains("/")){
                    DOB = line;
                    isDOB = true;
                    continue;
                }
            }
            if(line.contains("GOVT") || line.contains("TAX") || line.contains("INCOME") || line.contains("INDIA")){
                continue;
            }

            if(line.length()>=3){
                if(!isNAME) {
                    NAME = line;
                    isNAME = true;
                    continue;
                }
                if(!isFNAME){
                    FNAME = line;
                    isFNAME = true;
                    continue;
                }
            }
        }

        if(DOB.equals("") || PAN.equals("") || NAME.equals("") || FNAME.equals("")){
            Toast.makeText(getApplicationContext(), "Not a PAN Card! \nTry Again!", Toast.LENGTH_LONG).show();
        }
        else {
            Intent intent = new Intent(getApplicationContext(), DataActivity.class);
            intent.putExtra("NAME", NAME);
            intent.putExtra("FNAME", FNAME);
            intent.putExtra("DOB", DOB);
            intent.putExtra("PAN", PAN);
            startActivity(intent);
        }

        isPAN = false; isDOB = false; isNAME = false; isFNAME = false;
        PAN = ""; DOB = ""; NAME = ""; FNAME = "";
    }

    private boolean checkPermission() {
        if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        else return false;
    }
}
