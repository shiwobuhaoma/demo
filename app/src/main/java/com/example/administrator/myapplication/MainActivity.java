package com.example.administrator.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.vector.update_app.HttpManager;
import com.vector.update_app.UpdateAppManager;
import com.vector.update_app.utils.AppUpdateUtils;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> list = new ArrayList<>();
    private ArrayList<ContactsInfo> conList = new ArrayList<>();
    private ContactsAdapter adapter;
    private String mUpdateUrl = "http://127.0.0.1:8080/app-debug.apk";
    private String url = "http://127.0.0.1:8080/json.txt";
    private Map<String, String> map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkVersion();
        new UpdateAppManager
                .Builder()
                .setActivity(this)
                //更新地址
                .setUpdateUrl(mUpdateUrl)
                //实现httpManager接口的对象
                .setHttpManager(new UpdateAppHttpUtil())
                .build()
                .update();
        ListView listView = (ListView) findViewById(R.id.listView);
        ListView contact_listView = (ListView) findViewById(R.id.contact_listView);
        Button button = (Button) findViewById(R.id.button);
        Button btn_jump = (Button) findViewById(R.id.btn_jump);
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add("5");
        ItemAdapter<String> itemAdapter = new ItemAdapter<>
                (this, list);
        listView.setAdapter(itemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showInfo(position);
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 *ContextCompat.checkSelfPermission，主要用于检测某个权限是否已经被授予，
                 * 方法返回值为PackageManager.PERMISSION_DENIED或者PackageManager.PERMISSION_GRANTED。
                 * 当返回DENIED就需要进行申请授权了。
                 * ActivityCompat.requestPermissions，该方法是异步的，第一个参数是Context；
                 * 第二个参数是需要申请的权限的字符串数组；
                 * 第三个参数为requestCode，主要用于回调的时候检测。可以从方法名requestPermissions以及第二个参数看出，
                 * 是支持一次性申请多个权限的，系统会通过对话框逐一询问用户是否授权。
                 */
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_CONTACTS}, 1);
                } else {

                    obtionContacts();
                }
            }
        });

        adapter = new ContactsAdapter(conList, this);
        contact_listView.setAdapter(adapter);
        btn_jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(intent);
            }
        });
    }

    private void checkVersion() {
        int versionCode = AppUpdateUtils.getVersionCode(this);
        new UpdateAppHttpUtil().asyncPost(url, map, new HttpManager.Callback() {
            @Override
            public void onResponse(String result) {
                Toast.makeText(MainActivity.this, "接口访问成功", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "接口访问失败", Toast.LENGTH_LONG).show();
            }
        });

    }

    /**
     * listview中点击条目弹出对话框
     */
    public void showInfo(final int position) {
        new AlertDialog.Builder(this)
                .setTitle(position + "")
                .setMessage("我点击了item")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (position == 2)
                            startActivity(new Intent(MainActivity.this, SecondActivity.class));
                    }
                })
                .show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //权限申请成功
                obtionContacts();
            } else {
                Toast.makeText(MainActivity.this, "获取联系人的权限申请失败", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void obtionContacts() {
        //获取手机通讯录联系人
//        conList.clear();
        Cursor cursor = managedQuery(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            ContactsInfo contactsInfo = new ContactsInfo();
            if (cursor.moveToFirst()) {
                String hasPhone = cursor
                        .getString(cursor
                                .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                String id = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.Contacts._ID));
                if (hasPhone.equalsIgnoreCase("1")) {
                    hasPhone = "true";
                } else {
                    hasPhone = "false";
                }
                if (Boolean.parseBoolean(hasPhone)) {
                    Cursor myphones = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                    + " = " + id, null, null);
                    if (myphones != null) {
                        while (myphones.moveToNext()) {
                            //默认获取第一个手机号
                            String phoneNumber = myphones.getString(myphones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            String name = myphones.getString(myphones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            if (phoneNumber.contains("+86")) {
                                phoneNumber = phoneNumber.substring(3, phoneNumber.length());
                            }
                            if (phoneNumber.length() <= 11) {
                                phoneNumber = phoneNumber.substring(0, 3) + " " + phoneNumber.substring(3, 7) + " " + phoneNumber.substring(7, 11);
                            }
                            contactsInfo.setNumber(phoneNumber);
                            contactsInfo.setName(name);
                            conList.add(contactsInfo);
                            adapter.notifyDataSetChanged();
                        }
                        myphones.close();
                    }
                } else {
                    Toast.makeText(this, "手机中没有存储联系人", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

}
