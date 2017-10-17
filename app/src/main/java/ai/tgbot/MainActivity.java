package ai.tgbot;

import android.app.*;
import android.os.*;
import android.view.*;
import android.content.Intent;

/*
TGBot for Android
Agus Ibrahim
http://fb.me/mynameisagoes

License: GPLv2
http://www.gnu.org/licenses/gpl-2.0.html
*/
public class MainActivity extends Activity 
{
	View txtlabel;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		txtlabel=findViewById(R.id.mainTextView1);
		Intent startIntent = new Intent(MainActivity.this, TGPoll.class);
		startService(startIntent);
    }
}
