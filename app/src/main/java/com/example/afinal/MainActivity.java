package com.example.afinal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private WebView webview;
    private WebSettings webSettings;
    static final int GET_ACCOUNT_NAME_REQUEST = 1;  // 自訂帳號狀態
    private Button doSetDate;
    private Button doSetTime;
    private TextView textDateTime;
    private String textDate = "";
    private String textTime = "";
    private String tTime = "";
    private String forDateTime = "";
    private String calendar_id = "";
    private Date dateTime;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webview = (WebView) findViewById(R.id.webview);
        webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webview.setWebViewClient(new WebViewClient());
        webview.loadUrl("http://www.wikicfp.com/cfp/");

        GregorianCalendar calendar = new GregorianCalendar();
        doSetDate = (Button) findViewById(R.id.buttonDate);
        textDateTime = (TextView) findViewById(R.id.datetext);

        datePickerDialog = new DatePickerDialog(this, new OnDateSetListener() {
            //將設定的日期顯示出來
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                textDate=(year + "-" + (monthOfYear+1) + "-" + dayOfMonth);
                if(textTime != ""){
                    forDateTime = (textDate+"T"+tTime+"Z");
                    textDateTime.setText(textDate + "   " + textTime);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    try {
                        dateTime = format.parse(forDateTime);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            //將時間轉為12小時製顯示出來
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                textTime=((hourOfDay > 12 ? hourOfDay - 12 : hourOfDay)+ ":" + minute + " " + (hourOfDay > 12 ? "PM" : "AM"));
                tTime=(hourOfDay+":"+minute+":00");
                if(textDate != ""){
                    forDateTime = (textDate+"T"+tTime+"Z");
                    textDateTime.setText(textDate + "   " + textTime);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    try {
                        dateTime = format.parse(forDateTime);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(calendar.MINUTE), false);
    }

    @Override
    public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
            return;
        } else {
            super.onBackPressed();
        }
    }

    public void getAccountName(View v) {
        try {
            // 請求權限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR,
                            Manifest.permission.WRITE_CALENDAR}, 1);
            // get user google account
            Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                    new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE }, false, null, null, null, null);
            startActivityForResult(intent, GET_ACCOUNT_NAME_REQUEST );  //GET_ACCOUNT_NAME_REQUEST是一個自訂的int, 用作分辨所返回的結果
        } catch (ActivityNotFoundException e) {
            // TODO
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GET_ACCOUNT_NAME_REQUEST && resultCode == RESULT_OK) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME); // 使用者所選的帳戶名稱
            Button AA = (Button) findViewById(R.id.sign_in);
            AA.setText(accountName);
        }
    }

    public void setDate(View v){
        datePickerDialog.show();
    }

    public void setTime(View v){
        timePickerDialog.show();
    }

    public void query_calendar(View view) {
        // 設定要返回的資料
        String[] EVENT_PROJECTION = new String[]{
            CalendarContract.Calendars._ID,                   // 0 日歷ID
            CalendarContract.Calendars.ACCOUNT_NAME,          // 1 日歷所屬的帳戶名稱
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, // 2 日歷名稱
            CalendarContract.Calendars.OWNER_ACCOUNT,         // 3 日歷擁有者
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, // 4 對此日歷所擁有的權限
        };
        // 根據上面的設定，定義各資料的索引，提高代碼的可讀性
        int PROJECTION_ID_INDEX = 0;
        int PROJECTION_ACCOUNT_NAME_INDEX = 1;
        int PROJECTION_DISPLAY_NAME_INDEX = 2;
        int PROJECTION_OWNER_ACCOUNT_INDEX = 3;
        int PROJECTION_CALENDAR_ACCESS_LEVEL = 4;
        // 取得在EditText的帳戶名稱
        String targetAccount = ((Button) findViewById(R.id.sign_in)).getText().toString();
        // 查詢日歷
        Cursor cur;
        ContentResolver cr = getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        // 定義查詢條件，找出屬於上面Google帳戶及可以完全控制的日歷
        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("
                + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?) AND ("
                + CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " = ?))";
        String[] selectionArgs = new String[]{targetAccount,
                "com.google",
                Integer.toString(CalendarContract.Calendars.CAL_ACCESS_OWNER)};
        // Apps運行時檢查權限
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CALENDAR);
        // 建立List來暫存查詢的結果
        final List<String> accountNameList = new ArrayList<>();
        final List<Integer> calendarIdList = new ArrayList<>();
        // 如果使用者給了權限便開始查詢日歷
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            cur = cr.query(uri, EVENT_PROJECTION, selection, selectionArgs, null);
            if (cur != null) {
                while (cur.moveToNext()) {
                    long calendarId = 0;
                    String accountName = null;
                    String displayName = null;
                    String ownerAccount = null;
                    int accessLevel = 0;
                    // 取得資料
                    calendarId = cur.getLong(PROJECTION_ID_INDEX);
                    accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX);
                    displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
                    ownerAccount = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX);
                    accessLevel = cur.getInt(PROJECTION_CALENDAR_ACCESS_LEVEL);
                    Log.i("query_calendar", String.format("calendarId=%s", calendarId));
                    Log.i("query_calendar", String.format("accountName=%s", accountName));
                    Log.i("query_calendar", String.format("displayName=%s", displayName));
                    Log.i("query_calendar", String.format("ownerAccount=%s", ownerAccount));
                    Log.i("query_calendar", String.format("accessLevel=%s", accessLevel));
                    // 暫存資料讓使用者選擇
                    accountNameList.add(displayName);
                    calendarIdList.add((int) calendarId);
                    calendar_id = Long.toString(calendarId);
                }
                cur.close();
            }
            if (calendarIdList.size() != 0) {
                // 建立一個Dialog讓使用者選擇日歷
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                CharSequence items[] = accountNameList.toArray(new CharSequence[accountNameList.size()]);
                adb.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                adb.setNegativeButton("CANCEL", null);
                adb.show();
            }
            else {
                Toast toast = Toast.makeText(this, "找不到日歷", Toast.LENGTH_LONG);
                toast.show();
            }
        }
        else {
            Toast toast = Toast.makeText(this, "沒有所需的權限", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void insert_event(View view) {
        // 取得日曆資料
        query_calendar(view);
        // 取得日歷ID
        String targetCalendarId = calendar_id;
        long calendarId = Long.parseLong(targetCalendarId);
        // 取得輸入的時間作為活動開始時間
        long currentTimeMillis = dateTime.getTime();
        // 設定活動結束時間為1小時後
        long endTimeMillis = currentTimeMillis + 3600000;
        String description = ((EditText) findViewById(R.id.event_message)).getText().toString();
        // 新增活動
        ContentResolver cr     = getContentResolver();
        ContentValues   values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, currentTimeMillis);
        values.put(CalendarContract.Events.DTEND, endTimeMillis);
        values.put(CalendarContract.Events.TITLE, "CFP Event");
        values.put(CalendarContract.Events.DESCRIPTION, description);
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getDisplayName());
        // 因為targetSDK=25，所以要在Apps運行時檢查權限
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_CALENDAR);
        // 如果使用者給了權限便開始新增日歷
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
        }
    }
}