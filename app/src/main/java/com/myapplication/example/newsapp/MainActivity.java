package com.myapplication.example.newsapp;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> title = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();

    ArrayAdapter arrayAdapter;

    SQLiteDatabase articleDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articleDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);

        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articleTable (articleID INTEGER, title VARCHAR, content VARCHAR)");



        downloadTask task = new downloadTask();
        try {
        //    task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        }catch (Exception e){
            e.printStackTrace();
        }

        ListView listView = findViewById(R.id.titles);
        arrayAdapter= new ArrayAdapter(this, android.R.layout.simple_list_item_1,title);

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content",content.get(i));

                startActivity(intent);

            }
        });
        updateListView();
    }

    public void updateListView(){

        Cursor c = articleDB.rawQuery("SELECT * FROM articleTable",null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if(c.moveToFirst()){

            title.clear();
            content.clear();

            do {
                title.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));

            }while (c.moveToNext());
            arrayAdapter.notifyDataSetChanged();

        }
    }

    public class downloadTask extends AsyncTask<String,Void,String>{


        @Override
        protected String doInBackground(String... urls) {

            String result ="";
            URL url ;
            HttpURLConnection urlConnection = null;
             try{
                 url = new URL(urls[0]);

                 urlConnection =(HttpURLConnection) url.openConnection();

                 InputStream inputStream = urlConnection.getInputStream();

                 InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                 int data = inputStreamReader.read();

                 while(data !=-1){
                     char current  = (char) data;
                     result += current;
                     data = inputStreamReader.read();
                 }

                 JSONArray jsonArray = new JSONArray(result);

                 articleDB.execSQL("DELETE FROM articleTable");

                 int numberOfItems = 20;

                 if(numberOfItems<20){
                     numberOfItems= jsonArray.length();
                 }

                 for(int i =0; i< numberOfItems;i++){

                     String articleID= jsonArray.getString(i);
                     url= new URL("https://hacker-news.firebaseio.com/v0/item/"+ articleID +".json?print=pretty");

                     urlConnection =(HttpURLConnection) url.openConnection();

                     inputStream = urlConnection.getInputStream();

                     inputStreamReader = new InputStreamReader(inputStream);

                     data = inputStreamReader.read();

                     String articleInfo = "";

                     while(data !=-1){
                         char current  = (char) data;
                         articleInfo += current;
                         data = inputStreamReader.read();
                     }

                     JSONObject jsonObject = new JSONObject(articleInfo);

                     if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                         String articleTitle = jsonObject.getString("title");

                         String articleURL = jsonObject.getString("url");

                         url = new URL(articleURL);
                         urlConnection = (HttpURLConnection) url.openConnection();
                         inputStream = urlConnection.getInputStream();
                         inputStreamReader = new InputStreamReader(inputStream);
                         data = inputStreamReader.read();
                         String articleContent="";

                         while (data != -1){
                             char current = (char) data;
                             articleContent += current;
                             data = inputStreamReader.read();
                         }

                         Log.i("HTML",articleTitle);
                         Log.i("HTML",articleContent);

                         String sql = "INSERT INTO articleTable VALUES (?,?,?)";
                         SQLiteStatement statement = articleDB.compileStatement(sql);
                         statement.bindString(1,articleID);
                         statement.bindString(2,articleTitle);
                         statement.bindString(3,articleContent);

                         statement.execute();

                     }

                 }

                 Log.i("URL Content", result);
                 return  result;

             }catch (Exception e){

             }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();

        }
    }
}
