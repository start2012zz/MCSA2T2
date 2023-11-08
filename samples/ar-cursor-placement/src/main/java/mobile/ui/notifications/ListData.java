package mobile.ui.notifications;

import android.graphics.Bitmap;

public class ListData {
    private int imageResId;
    private String name;

    public ListData(int imageResId, String name) {
        this.imageResId = imageResId;
        this.name = name;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getName() {
        return name;
    }
}
