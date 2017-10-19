package ai.tgbot;

import android.app.*;
import android.os.*;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.content.Intent;

/*
TGBot for Android
Agus Ibrahim
http://fb.me/mynameisagoes

License: GPLv2
http://www.gnu.org/licenses/gpl-2.0.html
*/
        public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Intent startIntent = new Intent(MainActivity.this, TGPoll.class);
        startService(startIntent);
    }
}