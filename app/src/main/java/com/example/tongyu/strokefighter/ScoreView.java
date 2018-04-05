package com.example.tongyu.strokefighter;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.example.tongyu.strokefighter.Services.StrokeProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tongyu on 11/17/17.
 */

public class ScoreView extends AppCompatActivity {

    ListView listView;
    ImageView test;
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_score_view);

         test = (ImageView) findViewById(R.id.img_socre_board);
         listView = (ListView) findViewById(R.id.listview_score);

         SimpleAdapter adapter = new SimpleAdapter(this,getData(),
                 R.layout.content_score_item,
                 new String [] {"score", "date"},
                 new int[]{R.id.tv_content_score,R.id.tv_content_score_date});

         listView.setAdapter(adapter);
         listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

             @Override
             public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                 Log.d("Hello!", "Y u no see me? ");
             }

         });
     }

    private List<Map<String, Object>> getData() {

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();

        Cursor myCursor = getContentResolver().query(StrokeProvider.CONTENT_URI, StrokeProvider.projection, null, null, null);

        myCursor.moveToLast();
        map.put("id", myCursor.getInt(myCursor.getColumnIndex(StrokeProvider.STROKE_TABLE_ID)) + "");
        int score = 0;
        String correctNumber = myCursor.getString(myCursor.getColumnIndex(StrokeProvider.STROKE_TABLE_CORRECTNESS));
        for(int i = 0; i < correctNumber.length(); i++ )
        {
            if(correctNumber.charAt(i) == '1') {
                score++;
                Log.d("score" , "" + score);
            }
        }
        map.put("score",  score+ "/" +correctNumber.length());
        map.put("date",  myCursor.getString(myCursor.getColumnIndex(StrokeProvider.STROKE_TABLE_DATE)));
        list.add(map);
        map = new HashMap<String, Object>();

        while(myCursor.moveToPrevious())
        {
            int idIndex = myCursor.getColumnIndex(StrokeProvider.STROKE_TABLE_ID);
            int movementIndex = myCursor.getColumnIndex(StrokeProvider.STROKE_TABLE_MOVEMENTS_TESTED);
            int correctIndex = myCursor.getColumnIndex(StrokeProvider.STROKE_TABLE_CORRECTNESS);
            int dateIndex = myCursor.getColumnIndex(StrokeProvider.STROKE_TABLE_DATE);
            // getting the data using the indexs
            int id = myCursor.getInt(idIndex);
            String movement = myCursor.getString(movementIndex);
            String correct = myCursor.getString(correctIndex);
            String date = myCursor.getString(dateIndex);
            score = 0;

            Log.d("score" , correct);
            for(int i = 0; i < correct.length(); i++ )
            {
                if(correct.charAt(i) == '1') {
                    score++;
                    Log.d("score" , "" + score);
                }
            }

            map.put("id", id+ "");
            map.put("score", score + "/" + correct.length());
            map.put("date", date);
            list.add(map);
            map = new HashMap<String, Object>();
        }
        return list;
    }

    public void onClickHomeScoreView(View v)
    {
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        startActivity(upIntent);
    }
}
