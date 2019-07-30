package com.daniils.floordesigner.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.daniils.floordesigner.R;

public class AssetsManager {
    public Bitmap moveVertexIcon, moveLineIcon;

    public void load(Context c) {
        Bitmap moveVertexIconNS = BitmapFactory.decodeResource(c.getResources(), R.raw.move_vertex_icon);
        moveVertexIcon = Bitmap.createScaledBitmap(moveVertexIconNS,
                (int)DrawingView.VERTEX_BUTTON_RADIUS * 2,
                (int)DrawingView.VERTEX_BUTTON_RADIUS * 2, false);
        moveVertexIconNS.recycle();
        Bitmap moveLineIconNS = BitmapFactory.decodeResource(c.getResources(), R.raw.move_line_icon);
        moveLineIcon = Bitmap.createScaledBitmap(moveLineIconNS,
                (int)DrawingView.SEGM_BUTTON_RADIUS * 2,
                (int)DrawingView.SEGM_BUTTON_RADIUS * 2, false);
        moveLineIconNS.recycle();
    }

    public void dispose() {
        moveVertexIcon.recycle();
        moveLineIcon.recycle();
    }
}
