package com.kanade.mentionedittextdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import io.github.luckyandyzhang.mentionedittext.MentionEditText;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private MentionEditText met;
    private Button add;
    private Button print;
    private Button af;
    private TextView textView;
    private int i = 0;
    private boolean autoformat = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        met = (MentionEditText) findViewById(R.id.met);
        add = (Button) findViewById(R.id.add);
        af = (Button) findViewById(R.id.autoformat);
        print = (Button) findViewById(R.id.print);
        textView = (TextView) findViewById(R.id.tv);

        met.addPattern("!", "![\\u4e00-\\u9fa5\\w\\-]+");
        met.addPattern("hahaha", "hahaha[\\u4e00-\\u9fa5\\w\\-]+");
        met.setOnMentionInputListener(new MentionEditText.OnMentionInputListener() {
            @Override
            public void onMentionCharacterInput(String tag) {
                Log.d(TAG, tag);
            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                met.addMentionString(i, "Test" + i, false);
                i++;
            }
        });

        print.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!autoformat) {
                    String text = met.formatMentionString("[Mention:%s, %s]");
                    textView.setText(text);
                } else {
                    List<String> list = met.getMentionList(true);
                    StringBuilder builder = new StringBuilder();
                    for (String s : list) {
                        builder.append(s)
                                .append('\n');
                    }
                    textView.setText(builder.toString());
                }
            }
        });

        af.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                met.setAutoFormat(autoformat);
                Toast.makeText(MainActivity.this, "now autoformat is " + autoformat, Toast.LENGTH_LONG).show();
                autoformat = !autoformat;
            }
        });
    }
}
