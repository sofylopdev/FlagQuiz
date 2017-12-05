// MainActivityFragment.java
// Contains the Flag Quiz logic
package com.deitel.flagquiz;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;



public class MainActivityFragment extends Fragment{
   // String used when logging error messages
   private static final String TAG = "FlagQuiz Activity";

   private static int FLAGS_IN_QUIZ = 10;

   private List<String> fileNameList; // flag file names
   private List<String> quizCountriesList; // countries in current quiz
   private Set<String> regionsSet; // world regions in current quiz
   private String correctAnswer; // correct country for the current flag
   private int totalGuesses; // number of guesses made
   private int correctAnswers; // number of correct guesses
   private int guessRows ; // number of rows displaying guess Buttons
   private SecureRandom random; // used to randomize the quiz
   private Handler handler; // used to delay loading next flag
   private Animation shakeAnimation; // animation for incorrect guess

   private LinearLayout quizLinearLayout; // layout that contains the quiz
   private TextView questionNumberTextView; // shows current question #
   private ImageView flagImageView; // displays a flag
   private LinearLayout[] guessLinearLayouts; // rows of answer Buttons
   private TextView answerTextView; // displays correct answer

   //TODO 5: Variables declaration
   private TextView tv_score;
   public  int score = 0;
   private int guessMadeByUser= 0;
   private int correctOnFirstAttempt = 0;

   //TODO 8: Variable declaration
   private TextView guessCountryTextView;

   //TODO 9: savedScore to be shared within SettingsActivityFragment
   public static int savedScore = 0;

   // configures the MainActivityFragment when its View is created
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
      super.onCreateView(inflater, container, savedInstanceState);
      View view =
         inflater.inflate(R.layout.fragment_main, container, false);

      fileNameList = new ArrayList<>();
      quizCountriesList = new ArrayList<>();
      random = new SecureRandom();
      handler = new Handler();

      // load the shake animation that's used for incorrect answers
      shakeAnimation = AnimationUtils.loadAnimation(getActivity(),
         R.anim.incorrect_shake);
      shakeAnimation.setRepeatCount(3); // animation repeats 3 times


      // get references to GUI components
      quizLinearLayout =
         (LinearLayout) view.findViewById(R.id.quizLinearLayout);
      questionNumberTextView =
         (TextView) view.findViewById(R.id.questionNumberTextView);

      //TODO 5: Finding the view by its ID
      tv_score = (TextView) view.findViewById(R.id.tv_score);

      //TODO 8: Finding the view by its ID
      guessCountryTextView = (TextView) view.findViewById(R.id.guessCountryTextView);

      flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
      guessLinearLayouts = new LinearLayout[4];
      guessLinearLayouts[0] =
         (LinearLayout) view.findViewById(R.id.row1LinearLayout);
      guessLinearLayouts[1] =
         (LinearLayout) view.findViewById(R.id.row2LinearLayout);
      guessLinearLayouts[2] =
         (LinearLayout) view.findViewById(R.id.row3LinearLayout);
      guessLinearLayouts[3] =
         (LinearLayout) view.findViewById(R.id.row4LinearLayout);
      answerTextView = (TextView) view.findViewById(R.id.answerTextView);

      // configure listeners for the guess Buttons
      for (LinearLayout row : guessLinearLayouts) {
         for (int column = 0; column < row.getChildCount(); column++) {
            Button button = (Button) row.getChildAt(column);
            button.setOnClickListener(guessButtonListener);
         }
      }

      // set questionNumberTextView's text
      questionNumberTextView.setText(
         getString(R.string.question, 1, FLAGS_IN_QUIZ));


      return view; // return the fragment's view for display
   }

   // update guessRows based on value in SharedPreferences
   public void updateGuessRows(SharedPreferences sharedPreferences) {
      // get the number of guess buttons that should be displayed
      String choices =
         sharedPreferences.getString(MainActivity.CHOICES, null);
      guessRows = Integer.parseInt(choices) / 2;

      // hide all quess button LinearLayouts
      for (LinearLayout layout : guessLinearLayouts)
         layout.setVisibility(View.GONE);

      // display appropriate guess button LinearLayouts
      for (int row = 0; row < guessRows; row++)
         guessLinearLayouts[row].setVisibility(View.VISIBLE);

   }

   // update world regions for quiz based on values in SharedPreferences
   public void updateRegions(SharedPreferences sharedPreferences) {
      regionsSet =
         sharedPreferences.getStringSet(MainActivity.REGIONS, null);
   }



   //TODO 4: Update the Number of the questions based on the value in SharedPreferences
   public void updateNumberOfQuestions(SharedPreferences sharedPreferences){
      String numberOfQuestions =
              sharedPreferences.getString(MainActivity.NUMBER_OF_QUESTIONS, null);
      FLAGS_IN_QUIZ = Integer.parseInt(numberOfQuestions);
   }


   // set up and start the next quiz
   public void resetQuiz() {

      //TODO 5: Reset all to the defaul value whenever the Quiz reset
      correctOnFirstAttempt = 0;
      score = 0;
      correctOnFirstAttempt = 0;
      tv_score.setText("Score: 0");


      // use AssetManager to get image file names for enabled regions
      AssetManager assets = getActivity().getAssets();
      fileNameList.clear(); // empty list of image file names

      try {
         // loop through each region
         for (String region : regionsSet) {
            // get a list of all flag image files in this region
            String[] paths = assets.list(region);

            for (String path : paths)
               fileNameList.add(path.replace(".png", ""));
         }
      }
      catch (IOException exception) {
         Log.e(TAG, "Error loading image file names", exception);
      }

      correctAnswers = 0; // reset the number of correct answers made
      totalGuesses = 0; // reset the total number of guesses the user made
      quizCountriesList.clear(); // clear prior list of quiz countries

      int flagCounter = 1;
      int numberOfFlags = fileNameList.size();

      // add FLAGS_IN_QUIZ random file names to the quizCountriesList
      while (flagCounter <= FLAGS_IN_QUIZ) {
         int randomIndex = random.nextInt(numberOfFlags);

         // get the random file name
         String filename = fileNameList.get(randomIndex);

         // if the region is enabled and it hasn't already been chosen
         if (!quizCountriesList.contains(filename)) {
            quizCountriesList.add(filename); // add the file to the list
            ++flagCounter;
         }
      }

      loadNextFlag(); // start the quiz by loading the first flag
   }

   // after the user guesses a correct flag, load the next flag
   private void loadNextFlag() {
      // get file name of the next flag and remove it from the list
      String nextImage = quizCountriesList.remove(0);
      correctAnswer = nextImage; // update the correct answer
      answerTextView.setText(""); // clear answerTextView

      // display current question number
      questionNumberTextView.setText(getString(
         R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

      guessCountryTextView.setText(getString(R.string.guess_country));
      // extract the region from the next image's name
      String region = nextImage.substring(0, nextImage.indexOf('-'));

      // use AssetManager to load next image from assets folder
      AssetManager assets = getActivity().getAssets();

      // get an InputStream to the asset representing the next flag
      // and try to use the InputStream
      try (InputStream stream =
              assets.open(region + "/" + nextImage + ".png")) {
         // load the asset as a Drawable and display on the flagImageView
         Drawable flag = Drawable.createFromStream(stream, nextImage);
         flagImageView.setImageDrawable(flag);

         animate(false); // animate the flag onto the screen
      }
      catch (IOException exception) {
         Log.e(TAG, "Error loading " + nextImage, exception);
      }

      Collections.shuffle(fileNameList); // shuffle file names

      // put the correct answer at the end of fileNameList
      int correct = fileNameList.indexOf(correctAnswer);
      fileNameList.add(fileNameList.remove(correct));

      // add 2, 4, 6 or 8 guess Buttons based on the value of guessRows
      for (int row = 0; row < guessRows; row++) {
         // place Buttons in currentTableRow
         for (int column = 0;
              column < guessLinearLayouts[row].getChildCount();
              column++) {
            // get reference to Button to configure
            Button newGuessButton =
               (Button) guessLinearLayouts[row].getChildAt(column);
            newGuessButton.setEnabled(true);

            // get country name and set it as newGuessButton's text
            String filename = fileNameList.get((row * 2) + column);
            newGuessButton.setText(getCountryName(filename));
         }
      }

      // randomly replace one Button with the correct answer
      int row = random.nextInt(guessRows); // pick random row
      int column = random.nextInt(2); // pick random column
      LinearLayout randomRow = guessLinearLayouts[row]; // get the row
      String countryName = getCountryName(correctAnswer);
      ((Button) randomRow.getChildAt(column)).setText(countryName);
   }

   // parses the country flag file name and returns the country name
   private String getCountryName(String name) {
      //return name.substring(name.indexOf('-') + 1).replace('_', ' ');
      String[] parts = name.split("-");
      String lastWord = parts[1];
      return lastWord;
   }

   // animates the entire quizLinearLayout on or off screen
   private void animate(boolean animateOut) {
      // prevent animation into the the UI for the first flag
      if (correctAnswers == 0)
         return;

      // calculate center x and center y
      int centerX = (quizLinearLayout.getLeft() +
         quizLinearLayout.getRight()) / 2; // calculate center x
      int centerY = (quizLinearLayout.getTop() +
         quizLinearLayout.getBottom()) / 2; // calculate center y

      // calculate animation radius
      int radius = Math.max(quizLinearLayout.getWidth(),
         quizLinearLayout.getHeight());

      Animator animator;

      // if the quizLinearLayout should animate out rather than in
      if (animateOut) {
         // create circular reveal animation
         animator = ViewAnimationUtils.createCircularReveal(
            quizLinearLayout, centerX, centerY, radius, 0);
         animator.addListener(
            new AnimatorListenerAdapter() {
               // called when the animation finishes
               @Override
               public void onAnimationEnd(Animator animation) {
                  loadNextFlag();
               }
            }
         );
      }
      else { // if the quizLinearLayout should animate in
         animator = ViewAnimationUtils.createCircularReveal(
            quizLinearLayout, centerX, centerY, 0, radius);
      }

      animator.setDuration(500); // set animation duration to 500 ms
      animator.start(); // start the animation
   }



   // called when a guess Button is touched
   private OnClickListener guessButtonListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
         Button guessButton = ((Button) v);
         String guess = guessButton.getText().toString();
         String answer = getCountryName(correctAnswer);
         ++totalGuesses; // increment number of guesses the user has made

         /*
         //TODO: 8
         String answerForCapital = getCountryCapitalName(correctAnswer);
         if(guess.equals(answerForCapital)){
            score = score + 50;
         }*/

         //TODO 5: Get the user number of time guess (Based on Button click)
         ++guessMadeByUser;

         if (guess.equals(answer)) { // if the guess is correct
            ++correctAnswers; // increment the number of correct answers


            //TODO 5: Switch statement to determine how many guesses the user has, calc the score
            //Based on first 4 guessed the user will gain points otherwise nothing also applying the same theme
            switch (guessMadeByUser){
               case 1:
                  correctOnFirstAttempt = correctOnFirstAttempt + 1;
                  score = score + 20;
                  tv_score.setText("Score: " + String.valueOf(score));
                  tv_score.setTextColor(
                          getResources().getColor(R.color.correct_answer,
                                  getContext().getTheme()));
                  break;
               case 2:
                  score = score + 10;
                  tv_score.setText("Score: " + String.valueOf(score));
                  tv_score.setTextColor(
                          getResources().getColor(R.color.correct_answer,
                                  getContext().getTheme()));
                  break;
               case 3:
                  score = score + 5;
                  tv_score.setText("Score: " + String.valueOf(score));
                  tv_score.setTextColor(
                          getResources().getColor(R.color.correct_answer,
                                  getContext().getTheme()));
                  break;
               case 4:
                  score = score + 1;
                  tv_score.setText("Score: " + String.valueOf(score));
                  tv_score.setTextColor(
                          getResources().getColor(R.color.correct_answer,
                                  getContext().getTheme()));
                  break;
               default:
                  tv_score.setText("Score: " + String.valueOf(score));
            }
            guessMadeByUser = 0; //Reset the guess to zero after calculating the score for the current Question

            // display correct answer in green text
            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(
               getResources().getColor(R.color.correct_answer,
                  getContext().getTheme()));

            disableButtons(); // disable all guess Buttons


            // if the user has correctly identified FLAGS_IN_QUIZ flags
            if (correctAnswers == FLAGS_IN_QUIZ) {

               //TODO 9: If the new score is higher than the one already saved then I advise the user using Dialog
               if(score > retrieveSavedScore()){

                  savedScore = score;// savedScore is to be shared in the SettingsActivityFragment in order to update the settings with the highest score

                  AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                  builder.setMessage(getString(R.string.high_score_achieved)  +
                          String.valueOf(score) + getString(R.string.points_high_score_achieved));
                  builder.setCancelable(true);

                  builder.setPositiveButton(R.string.dialog_ok,
                          new DialogInterface.OnClickListener() {
                             public void onClick(DialogInterface dialog,
                                                 int id) {
                                resetQuiz();
                             }
                          });
                  AlertDialog alert1One = builder.create();
                  alert1One.show();
               }

               //TODO 9: Saving the current score
               saveScore(score);

               AlertDialog.Builder builderOne = new AlertDialog.Builder(getContext());
               builderOne.setMessage(
                       getString(R.string.results,
                               totalGuesses,
                               (1000 / (double) totalGuesses), correctOnFirstAttempt, score));
               builderOne.setCancelable(true);

               builderOne.setPositiveButton(R.string.reset_quiz,
                       new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog,
                                              int id) {
                             resetQuiz();
                          }
                       });
               AlertDialog alert1One = builderOne.create();
               alert1One.show();



            }
            else {
               // answer is correct but quiz is not over
               /*load the next flag after a 2-second delay
               handler.postDelayed(
                       new Runnable() {
                          @Override
                          public void run() {
                             animate(true); // animate the flag off the screen
                          }
                       }, 2000); // 2000 milliseconds for 2-second delay */
               bonusQuestion(); //TODO 8:
            }

         }
         else { // answer was incorrect
            flagImageView.startAnimation(shakeAnimation); // play shake

            // display "Incorrect!" in red
            answerTextView.setText(R.string.incorrect_answer);
            answerTextView.setTextColor(getResources().getColor(
               R.color.incorrect_answer, getContext().getTheme()));


            //TODO 5: Using same theme as if the user has incorrect answer, it will change the color to red
            tv_score.setTextColor(
                    getResources().getColor(R.color.incorrect_answer,
                            getContext().getTheme()));


            guessButton.setEnabled(false); // disable incorrect answer
         }
      }
   };

   // utility method that disables all answer Buttons
   private void disableButtons() {
      for (int row = 0; row < guessRows; row++) {
         LinearLayout guessRow = guessLinearLayouts[row];
         for (int i = 0; i < guessRow.getChildCount(); i++)
            guessRow.getChildAt(i).setEnabled(false);
      }
   }

//TODO 8: Bonus Question
   private void bonusQuestion() {

      guessCountryTextView.setText(R.string.guess_the_capital);

      // get file name of the next flag and remove it from the list
      String nextImage = quizCountriesList.remove(0);
      correctAnswer = nextImage; // update the correct answer
      answerTextView.setText(""); // clear answerTextView

      // put the correct answer at the end of fileNameList
      int correct = fileNameList.indexOf(correctAnswer);
      fileNameList.add(fileNameList.remove(correct));

      // add 2, 4, 6 or 8 guess Buttons based on the value of guessRows
      for (int row = 0; row < guessRows; row++) {
         // place Buttons in currentTableRow
         for (int column = 0;
              column < guessLinearLayouts[row].getChildCount();
              column++) {
            // get reference to Button to configure
            Button newGuessButton =
                    (Button) guessLinearLayouts[row].getChildAt(column);
            newGuessButton.setEnabled(true);

            // get country capital name and set it as newGuessButton's text
            String filename = fileNameList.get((row * 2) + column);
            newGuessButton.setText(getCountryCapitalName(filename));
         }
      }

      // randomly replace one Button with the correct answer
      int row = random.nextInt(guessRows); // pick random row
      int column = random.nextInt(2); // pick random column
      LinearLayout randomRow = guessLinearLayouts[row]; // get the row
      String countryCapName = getCountryCapitalName(correctAnswer);
      ((Button) randomRow.getChildAt(column)).setText(countryCapName);

   }

   //TODO 8: Get the country capital name
   // parses the country capital flag file name and returns the country capital name
   private String getCountryCapitalName(String name) {
      String[] parts = name.split("-");
      String lastWord = parts[parts.length - 1];
      return lastWord;
   }



   //TODO 9: Methods for Saving and retrieve the user score using shared preferences
   private void saveScore(int score){
      SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = preferences.edit();
      editor.putInt("UserScore", score);
      editor.commit();
   }
   private int retrieveSavedScore(){
      SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
      int score = preferences.getInt("UserScore", 50); //Minimum score to be achieved 50 in order to get ranked
      return score;
   }


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
