package nl.mpcjanssen.simpletask;

import android.app.Activity;
import android.os.Bundle;

abstract class ThemedActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SimpletaskApplication app = (SimpletaskApplication) getApplication();
        setTheme(app.getActiveTheme());
        setTheme(app.getActiveFont());
        super.onCreate(savedInstanceState);
    }
}
