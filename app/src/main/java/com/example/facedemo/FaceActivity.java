package com.example.facedemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;


public class FaceActivity extends Activity {

    private String time = "";
    private static int RESULT_LOAD_IMAGE = 1;
    private GeoCoder mSearch = null;
    public class SDKReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String s = intent.getAction();
            TextView text = (TextView) findViewById(R.id.txtView);
            text.setTextColor(Color.RED);
            if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {
                text.setText("key 验证出错! 请在 AndroidManifest.xml 文件中检查 key 设置");
            } else if (s
                    .equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
                text.setText("网络出错");
            }
        }
    }
    private SDKReceiver mReceiver;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SDKInitializer.initialize(this.getApplication());
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
            @Override
            public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {

                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                    return;
                }
                TextView tv = (TextView)findViewById(R.id.txtView);
                tv.setText(time + "\n" + result.getAddress());
            }
        });
        Button buttonLoadImage = (Button) findViewById(R.id.buttonLoadPicture);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_GET_CONTENT);
                i.setType("image/*");

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });
        // 注册 SDK 广播监听者
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        mReceiver = new SDKReceiver();
        registerReceiver(mReceiver, iFilter);
        Log.e("sha1", sHA1(this));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);

            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            ImageView imageView = (ImageView) findViewById(R.id.imgView);
            Bitmap bitmap;
            ContentResolver cr = this.getContentResolver();
            try {
                bitmap = BitmapFactory.decodeStream(cr.openInputStream(selectedImage));
                imageView.setImageBitmap(bitmap);
                try {
                    getExifInfo(picturePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);


    }
    /* 目前Android SDK定义的Tag有:
   TAG_DATETIME 时间日期
   TAG_FLASH 闪光灯
   TAG_GPS_LATITUDE 纬度
   TAG_GPS_LATITUDE_REF 纬度参考
   TAG_GPS_LONGITUDE 经度
   TAG_GPS_LONGITUDE_REF 经度参考
   TAG_IMAGE_LENGTH 图片长
   TAG_IMAGE_WIDTH 图片宽
   TAG_MAKE 设备制造商
   TAG_MODEL 设备型号
   TAG_ORIENTATION 方向
   TAG_WHITE_BALANCE 白平衡
   */
    protected void getExifInfo(String path) throws IOException {
        ExifInterface exifInterface = new ExifInterface(path);
        time = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
        if (exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null){
            String longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            if (longitude.isEmpty() || latitude.isEmpty())
            {
                TextView tv = (TextView)findViewById(R.id.txtView);
                tv.setText("未获取到图片的经纬度信息");
                return;
            }
            Double longitudeDb = getDoubleTypeFromString(longitude) +0.012691;
            Double latitudeDb = getDoubleTypeFromString(latitude) +0.007080;
            LatLng ptCenter = new LatLng(latitudeDb,longitudeDb);
            mSearch.reverseGeoCode(new ReverseGeoCodeOption()
                    .location(ptCenter));
        }


        // 反Geo搜索

    }
    protected double getDoubleTypeFromString(String str)
    {
        String strS[] = str.split(",");
        String duS[] = strS[0].split("/");
        String fenS[] = strS[1].split("/");
        String miaoS[] = strS[2].split("/");
        Double du = Double.valueOf(duS[0]) / Double.valueOf(duS[1]);
        Double fen = Double.valueOf(fenS[0]) / Double.valueOf(fenS[1]);
        Double miao = Double.valueOf(miaoS[0]) / Double.valueOf(miaoS[1]);
        if(du<0)
            return -(Math.abs(du)+(fen+(miao/60))/60);
        return du+(fen+(miao/60))/60;
    }

    public static String sHA1(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                hexString.append(":");
            }
            String result = hexString.toString();
            return result.substring(0, result.length()-1);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
