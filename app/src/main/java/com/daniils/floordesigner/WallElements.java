package com.daniils.floordesigner;

import com.daniils.floordesigner.windows.Door;
import com.daniils.floordesigner.windows.Window;


public class WallElements {

    public static class WallElement {
        public String name;
        public Class<?> aClass;

        public WallElement(String name, Class<?> aClass) {
            this.name = name;
            this.aClass = aClass;
        }
    }


    public static WallElement[] wallElements = {
            new WallElement("Window", Window.class),
            new WallElement("Door", Door.class)
    };
}
