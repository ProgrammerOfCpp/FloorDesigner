package com.daniils.floordesigner.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daniils.floordesigner.R;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateProjectList();
    }

    private void updateProjectList() {
        ((LinearLayout) findViewById(R.id.projects_layout)).removeAllViews();
        for (File f : getFilesDir().listFiles()) {
            String[] toks = f.getName().split(Pattern.quote("."));
            if (toks.length == 0)
                continue;
            String extension = toks[toks.length - 1];
            String name = "";
            for (int i = 0; i < toks.length - 1; i++) {
                name += toks[i];
                if (i < toks.length - 2)
                    name += ".";
                if (f.isFile() && extension.equals("proj")) {
                    createProjectButton(name);
                }
            }
        }
    }

    private void createProjectButton(String name) {
        Button button = new Button(this);
        button.setText(name);
        button.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout)findViewById(R.id.projects_layout)).addView(button);
        final String path = getFilesDir().getAbsolutePath() + "/" + name;
        final MainActivity context = this;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, EditorActivity.class);
                intent.putExtra("path", path);
                startActivity(intent);
            }
        });
    }
    public void onCreateProjectClick(View v) {
        String name = ((TextView)findViewById(R.id.create_project_name)).getText().toString();
        if (!name.isEmpty()) {
            String path = getFilesDir().getAbsolutePath() + "/" + name + ".proj";
            File file = new File(path);
            if (file.exists()) {
                Toast.makeText(this, R.string.project_exists_toast, Toast.LENGTH_LONG).show();
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                Toast.makeText(this, R.string.cant_create_file_error, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            updateProjectList();
        }
    }
}
