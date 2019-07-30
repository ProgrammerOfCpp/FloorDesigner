package com.daniils.floordesigner.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.daniils.floordesigner.Polygon;
import com.daniils.floordesigner.R;
import com.daniils.floordesigner.Selectable;
import com.daniils.floordesigner.view.AssetsManager;
import com.daniils.floordesigner.view.DrawingView;

import java.io.IOException;

public class EditorActivity extends AppCompatActivity {
    private DrawingView drawingView;
    private String path;
    public AssetsManager assetsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        assetsManager = new AssetsManager();

        LinearLayout content = findViewById(R.id.content_layout);

        path = getIntent().getExtras().getString("path");

        ((EditText)findViewById(R.id.labelText)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String text = ((EditText)findViewById(R.id.labelText)).getText().toString();
                for (Selectable s : drawingView.selection) {
                    if (s instanceof Polygon) {
                        ((Polygon) s).label = text;
                    }
                }
                drawingView.invalidate();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        drawingView = new DrawingView(this, path);
        drawingView.setLayoutParams(new LinearLayout.LayoutParams(1024, 1024));
        content.addView(drawingView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        assetsManager.load(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        assetsManager.dispose();
    }

    public void onDrawingButtonClick(View v) {
        Button b = (Button)v;
        String on = getString(R.string.pen_on);
        String off = getString(R.string.pen_off);
        if (b.getText().equals(on)) {
            b.setText(off);
            drawingView.setDrawing(true);
        } else {
            b.setText(on);
            drawingView.setDrawing(false);
        }
    }

    public void onSaveButtonClick(View v) {
        try {
            drawingView.save(path);
            Toast.makeText(this, R.string.saved_successfylly_toast, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.cant_create_file_error, Toast.LENGTH_LONG).show();
        }
    }

    public void onDeleteClick(View v) {
        drawingView.deleteSelected();
    }

    public void onZoomPlusClick(View v) {
        drawingView.scaleFactor /= 0.8;
        drawingView.invalidate();
        drawingView.scaleFactor = Math.max(0.4, Math.min(drawingView.scaleFactor, 2.5));
    }

    public void onZoomMinusClick(View v) {
        drawingView.scaleFactor *= 0.8;
        drawingView.invalidate();
        drawingView.scaleFactor = Math.max(0.4, Math.min(drawingView.scaleFactor, 2.5));
    }

    public void onPlaceSquareButtonClick(View view) {
        Toast.makeText(this, "Drag and drop to create square", Toast.LENGTH_LONG).show();
        drawingView.setPlacingSquare(true);
    }
}
