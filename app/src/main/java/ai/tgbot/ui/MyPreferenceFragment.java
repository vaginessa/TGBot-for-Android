package ai.tgbot.ui;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import ai.tgbot.R;

/**
 * Created by draplater on 2017/10/17.
 */

public class MyPreferenceFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}