package com.example.syamantak.toonify_10;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.Core.addWeighted;
import static org.opencv.core.Core.bitwise_not;
import static org.opencv.core.Core.divide;
import static org.opencv.core.Core.multiply;
import static org.opencv.core.Core.split;

public class MainActivity extends Activity {

    private static final String TAG = "syamantak debug";

    static
    {
        if(!OpenCVLoader.initDebug())
        {
            Log.d(TAG,"OpenCV not loaded");
        }
        else
        {
            Log.d(TAG,"OpenCV loaded");
        }
    }

    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private Button btnSelect, cartoonify, pencilSketch,filter;
    private ImageView ivImage;
    private String userChoosenTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSelect = (Button) findViewById(R.id.btnSelectPhoto);
        btnSelect.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
        cartoonify = (Button) findViewById(R.id.cartoonify);
        cartoonify.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                doCartoonify();
            }
        });
        pencilSketch = (Button) findViewById(R.id.pencilSketch);
        pencilSketch.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                doPencillSketch();
            }
        });
        filter = (Button) findViewById(R.id.filter);
        filter.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                applyFilter();
            }
        });
        ivImage = (ImageView) findViewById(R.id.ivImage);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if(userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                } else {
                    //code for deny
                }
                break;
        }
    }

    private void selectImage() {
        final CharSequence[] items = { "Take Photo", "Choose from Library",
                "Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result=Utility.checkPermission(MainActivity.this);

                if (items[item].equals("Take Photo")) {
                    userChoosenTask ="Take Photo";
                    if(result)
                        cameraIntent();

                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask ="Choose from Library";
                    if(result)
                        galleryIntent();

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();

    }

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }

    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        }
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");

        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ivImage.setImageBitmap(thumbnail);
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {

        Bitmap bm=null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ivImage.setImageBitmap(bm);
    }


    public void doCartoonify(){
        Bitmap bitmap = ((BitmapDrawable)ivImage.getDrawable()).getBitmap();
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();


        Mat InputImage = new Mat();
        Mat imgGray;
        Mat imgCanny;
        Mat edgeImage;
        Mat dstMat;
        Mat mrgbk;
        Mat FinalImage;


        edgeImage = new Mat(height, width, CvType.CV_8UC4);
        imgGray = new Mat(height, width, CvType.CV_8UC4);
        imgCanny = new Mat(height, width, CvType.CV_8UC4);
        FinalImage = new Mat(height,width,CvType.CV_8UC4);
        dstMat = new Mat(height, width, CvType.CV_8UC4);
        mrgbk = new Mat(height, width, CvType.CV_8UC4);


        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // Input Image
        Utils.bitmapToMat(bmp32, InputImage);


//!-- Colour quantisation

//		for(int i=0;i<height;i++)
//		{
//			for(int j=0;j<width;j++)
//			{
//					double [] bgrarray = InputImage.get(i,j);
//					bgrarray[0] = 24 * (int)(bgrarray[0]/24);
//					bgrarray[1] = 24 * (int)(bgrarray[1]/24);
//					bgrarray[2] = 24 * (int)(bgrarray[2]/24);
//					InputImage.put(i,j,bgrarray);
//			}
//		}



// Performing Cartoonisation of image

        // Converting to gray scale
        Imgproc.cvtColor(InputImage, imgGray, Imgproc.COLOR_RGB2GRAY);

        // Performing Canny edge detection on Gray scaled image
        Imgproc.medianBlur(imgGray,imgGray, 7);
        Imgproc.Canny(imgGray, imgCanny, 50, 150);
        Imgproc.cvtColor(imgCanny,imgCanny,Imgproc.COLOR_GRAY2RGB);
        Core.bitwise_not(imgCanny,edgeImage);


        // Performing Bilateral Filter
        Imgproc.cvtColor(InputImage, mrgbk, Imgproc.COLOR_BGRA2BGR);
        for(int i=0;i<14;i++)
        {
            if(i%2 == 0)Imgproc.bilateralFilter(mrgbk, dstMat, 9, 9, 7);

            else Imgproc.bilateralFilter(dstMat, mrgbk, 9, 9, 7);
        }
        Imgproc.bilateralFilter(mrgbk, dstMat, 9, 7, 7);
        Imgproc.cvtColor(dstMat, dstMat, Imgproc.COLOR_RGB2RGBA);
        Imgproc.cvtColor(edgeImage,edgeImage,Imgproc.COLOR_RGB2RGBA);
        Core.bitwise_and(dstMat,edgeImage,FinalImage);
        org.opencv.core.Size s = new Size(7,7);
        Imgproc.GaussianBlur(FinalImage, FinalImage, s, 0, 0);

        Utils.matToBitmap(FinalImage, bitmap);
        ivImage.setImageBitmap(bitmap);
    }

    public void doPencillSketch(){

        doCartoonify();

        Bitmap bitmap = ((BitmapDrawable)ivImage.getDrawable()).getBitmap();
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        //Image variables
        Mat InputImage = new Mat();
        Mat imgGray;
        Mat Inverted_Gray;
        Mat Dodge_Image;

        Inverted_Gray = new Mat(height,width,CvType.CV_8UC4);
        imgGray = new Mat(height, width, CvType.CV_8UC4);
        Dodge_Image = new Mat(height,width,CvType.CV_8UC4);

        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        //Input Image
        Utils.bitmapToMat(bmp32, InputImage);

        // Converting to gray scale
        Imgproc.cvtColor(InputImage, imgGray, Imgproc.COLOR_RGB2GRAY);

        // Converting to pencil sketch
        Core.bitwise_not(imgGray,Inverted_Gray);
        org.opencv.core.Size s = new Size(7,7);
        Imgproc.GaussianBlur(Inverted_Gray, Inverted_Gray, s, 0, 0);
        divide(imgGray,Inverted_Gray,Dodge_Image,256);

        Utils.matToBitmap(Dodge_Image, bitmap);
        ivImage.setImageBitmap(bitmap);
    }

    public void applyFilter(){

        Bitmap bitmap = ((BitmapDrawable)ivImage.getDrawable()).getBitmap();
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        Mat InputImage;
        InputImage = new Mat();

        Mat Temp1 = new Mat(height, width, CvType.CV_8UC4);
        Mat Temp2 = new Mat(height, width, CvType.CV_8UC4);
        Mat Temp3 = new Mat(height, width, CvType.CV_8UC4);
        Mat Temp4 = new Mat(height, width, CvType.CV_8UC4);
        Mat Temp5 = new Mat(height, width, CvType.CV_8UC4);
        Mat Temp6 = new Mat(height, width, CvType.CV_8UC4);
        Mat Temp7 = new Mat(height, width, CvType.CV_8UC4);
        Mat Temp8 = new Mat(height, width, CvType.CV_8UC4);
        Mat Temp9 = new Mat(height, width, CvType.CV_8UC4);

        Utils.bitmapToMat(bitmap, InputImage);
        List<Mat> Channels = new ArrayList<Mat>();


        Core.split(InputImage,Channels);

        Mat Output_Red = new Mat(height, width, CvType.CV_8UC4);
        Mat Output_Green = new Mat(height, width, CvType.CV_8UC4);
        Mat Output_Blue = new Mat(height, width, CvType.CV_8UC4);
        Mat dst = new Mat(height, width, CvType.CV_8UC4);

        Scalar alpha1 = new Scalar(.393);
        Scalar alpha2 = new Scalar(.769);
        Scalar alpha3 = new Scalar(.189);
        Scalar alpha4 = new Scalar(.349);
        Scalar alpha5 = new Scalar(.686);
        Scalar alpha6 = new Scalar(.168);
        Scalar alpha7 = new Scalar(.272);
        Scalar alpha8 = new Scalar(.534);
        Scalar alpha9 = new Scalar(.131);


        Core.multiply(Channels.get(0),alpha1,Temp1);
        Core.multiply(Channels.get(1),alpha2,Temp2);
        Core.multiply(Channels.get(2),alpha3,Temp3);
        Core.multiply(Channels.get(0),alpha4,Temp4);
        Core.multiply(Channels.get(1),alpha5,Temp5);
        Core.multiply(Channels.get(2),alpha6,Temp6);
        Core.multiply(Channels.get(0),alpha7,Temp7);
        Core.multiply(Channels.get(1),alpha8,Temp8);
        Core.multiply(Channels.get(2),alpha9,Temp9);


        Core.addWeighted(Temp1,1,Temp2,1,0,Output_Blue);
        Core.addWeighted(Output_Blue,1,Temp3,1,0,Output_Blue);
        Core.addWeighted(Temp4,1,Temp5,1,0,Output_Green);
        Core.addWeighted(Output_Green,1,Temp6,1,0,Output_Red);
        Core.addWeighted(Temp7,1,Temp8,1,0,Output_Red);
        Core.addWeighted(Output_Red,1,Temp9,1,0,Output_Red);

        List<Mat> lst = Arrays.asList(Output_Blue, Output_Green, Output_Red);
        Core.merge(lst, dst);


        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Imgproc.cvtColor(dst, dst, Imgproc.COLOR_BGR2BGRA);


        Utils.matToBitmap(dst, bmp32);
        ivImage.setImageBitmap(bmp32);

    }

}
