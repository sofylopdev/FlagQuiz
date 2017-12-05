// MainActivity.java
// Hosts the MainActivityFragment on a phone and both the
// MainActivityFragment and SettingsActivityFragment on a tablet
package com.deitel.flagquiz;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;


public class MainActivity extends AppCompatActivity {
   // keys for reading data from SharedPreferences
   public static final String CHOICES = "pref_numberOfChoices";
   public static final String REGIONS = "pref_regionsToInclude";

   //TODO 4: The Key for the number of the questions
   public static final String NUMBER_OF_QUESTIONS = "pref_numberOfQuestions";

   private boolean phoneDevice = true; // used to force portrait mode
   private boolean preferencesChanged = true; // did preferences change?

   // configure the MainActivity
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);

      // set default values in the app's SharedPreferences
      PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

      // register listener for SharedPreferences changes
      PreferenceManager.getDefaultSharedPreferences(this).
         registerOnSharedPreferenceChangeListener(
            preferencesChangeListener);

      // determine screen size
      int screenSize = getResources().getConfiguration().screenLayout &
         Configuration.SCREENLAYOUT_SIZE_MASK;

      // if device is a tablet, set phoneDevice to false
      if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
         screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
         phoneDevice = false; // not a phone-sized device

      // if running on phone-sized device, allow only portrait orientation
      if (phoneDevice)
         setRequestedOrientation(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

   }

   // called after onCreate completes execution
   @Override
   protected void onStart() {
      super.onStart();

      if (preferencesChanged) {
         // now that the default preferences have been set,
         // initialize MainActivityFragment and start the quiz
         MainActivityFragment quizFragment = (MainActivityFragment)
            getSupportFragmentManager().findFragmentById(
               R.id.quizFragment);
         quizFragment.updateGuessRows(
            PreferenceManager.getDefaultSharedPreferences(this));
         quizFragment.updateRegions(
            PreferenceManager.getDefaultSharedPreferences(this));
         quizFragment.resetQuiz();
         preferencesChanged = false;
      }
   }

   // show menu if app is running on a phone or a portrait-oriented tablet
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // get the device's current orientation
      int orientation = getResources().getConfiguration().orientation;

      // display the app's menu only in portrait orientation
      if (orientation == Configuration.ORIENTATION_PORTRAIT) {
         // inflate the menu
         getMenuInflater().inflate(R.menu.menu_main, menu);
         return true;
      } else

         //TODO 7: Display the HelpIcon in all orientations
         getMenuInflater().inflate(R.menu.menu_main, menu);
         menu.findItem(R.id.action_help).setVisible(true);
         menu.findItem(R.id.action_settings).setVisible(false);
         return true;
   }

   // displays the SettingsActivity when running on a phone
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {

        //TODO 7: Help icon menu with dialog
       //Switch is used to determine which action menu is clicked and once the user hit the help icon
       //the user will be able to see a dialog linked to a String recourse explain how score works
      switch (item.getItemId()){
         case R.id.action_settings:
            Intent preferencesIntent = new Intent(this, SettingsActivity.class);
            startActivity(preferencesIntent);
            return true;
         case R.id.action_help:
             AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
             builder1.setMessage(getString(R.string.game_score_details));
             builder1.setCancelable(true);

             builder1.setPositiveButton(
                     R.string.dialog_ok,
                     new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int id) {
                             dialog.cancel();
                         }
                     });
             AlertDialog alert11 = builder1.create();
             alert11.show();
            return true;
      }
      return super.onOptionsItemSelected(item);
   }


   // listener for changes to the app's SharedPreferences
   private OnSharedPreferenceChangeListener preferencesChangeListener =
      new OnSharedPreferenceChangeListener() {
         // called when the user changes the app's preferences
         @Override
         public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
            preferencesChanged = true; // user changed app setting

            MainActivityFragment quizFragment = (MainActivityFragment)
               getSupportFragmentManager().findFragmentById(
                  R.id.quizFragment);


            if (key.equals(CHOICES)) { // # of choices to display changed
               quizFragment.updateGuessRows(sharedPreferences);
               quizFragment.resetQuiz();

            } else if (key.equals(REGIONS)) { // regions to include changed

               Set<String> regions =
                  sharedPreferences.getStringSet(REGIONS, null);

               if (regions != null && regions.size() > 0) {
                  quizFragment.updateRegions(sharedPreferences);
                  quizFragment.resetQuiz();

               } else {
                  // must select one region--set EU as default
                  SharedPreferences.Editor editor =
                     sharedPreferences.edit();
                  regions.add(getString(R.string.default_region));
                  editor.putStringSet(REGIONS, regions);
                  editor.apply();

                  Toast.makeText(MainActivity.this,
                     R.string.default_region_message,
                     Toast.LENGTH_SHORT).show();
               }

               //TODO 4: If the key was selected in the option, Update the number of the question
            } else if(key.equals(NUMBER_OF_QUESTIONS)){
               quizFragment.updateNumberOfQuestions(sharedPreferences);
               quizFragment.resetQuiz();
            }

            Toast.makeText(MainActivity.this,
               R.string.restarting_quiz,
               Toast.LENGTH_SHORT).show();
         }
      };


}


/*************************************************************************
 * (C) Copyright 1992-2016 by Deitel & Associates, Inc. and               *
 * Pearson Education, Inc. All Rights Reserved.                           *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 *************************************************************************/
