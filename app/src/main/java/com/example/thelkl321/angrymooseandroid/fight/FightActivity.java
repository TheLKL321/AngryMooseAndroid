package com.example.thelkl321.angrymooseandroid.fight;

import android.support.v4.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.thelkl321.angrymooseandroid.MainActivity;
import com.example.thelkl321.angrymooseandroid.R;
import com.example.thelkl321.angrymooseandroid.fight.SurrenderDialogFragment.SurrenderDialogListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public abstract class FightActivity extends AppCompatActivity implements SurrenderDialogListener{

    private ProgressBar mooseHpBar, playerHpBar;
    TextView middleText, logBox;
    private Moose moose;
    private Player player;
    int turnCounter;
    private HashMap<PlayerMove, Button> moveButtons = new HashMap<>();
    private HashSet<Button> disabledButtons = new HashSet<>();
    private MooseMove lastMooseMove;
    String lastEvent;

    protected Intent intent;
    private FragmentManager fm;
    EndgameFragment endgameFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fight);

        // Get difficulty and gamemode info
        intent = getIntent();
        int startingMooseHp = intent.getIntExtra(MainActivity.MOOSE_KEY, 0);
        int startingPlayerHp = intent.getIntExtra(MainActivity.PLAYER_KEY, 0);

        // Assign views
        mooseHpBar = findViewById(R.id.mooseHealthBar);
        mooseHpBar.setMax(startingMooseHp);
        mooseHpBar.setProgress(startingMooseHp);
        playerHpBar = findViewById(R.id.playerHealthBar);
        playerHpBar.setMax(startingPlayerHp);
        playerHpBar.setProgress(startingPlayerHp);
        middleText = findViewById(R.id.middleText);

        // Assign move buttons
        moveButtons.put(PlayerMove.THROW, (Button) findViewById(R.id.throwButton));
        moveButtons.put(PlayerMove.DODGE, (Button) findViewById(R.id.dodgeButton));
        moveButtons.put(PlayerMove.LEAP, (Button) findViewById(R.id.leapButton));
        moveButtons.put(PlayerMove.KICK, (Button) findViewById(R.id.kickButton));
        moveButtons.put(PlayerMove.ATTACK, (Button) findViewById(R.id.attackButton));

        // Create scrollable log
        logBox = findViewById(R.id.logText);
        logBox.setMovementMethod(new ScrollingMovementMethod());
        logBox.setText("");

        // Assign the endgame fragment and hide it
        fm = getSupportFragmentManager();
        endgameFragment = (EndgameFragment) fm.findFragmentById(R.id.endgameFragment);
        hideFragment(endgameFragment);

        // Create characters
        moose = new Moose(startingMooseHp, this);
        player = new Player(startingPlayerHp, this, moose);

        startgame();

        // First turn
        logEvent(R.string.initial_log);
        lastMooseMove = moose.mooseTurn();
    }

    abstract void startgame();              // Called right before the first moose turn

    abstract void endgame(String outcome);  // Called on death of either moose or the player

    protected abstract void updateTurns();  // Called right after the player pick their move

    void disable(PlayerMove move){
        Button button = moveButtons.get(move);
        assert button != null;
        String text = String.valueOf(button.getText());
        button.setText(text.concat(" X"));
        button.setClickable(false);
        disabledButtons.add(button);
    }

    private void enable (Button button){
        String text = String.valueOf(button.getText());
        text = text.substring(0, text.length() - 2);
        button.setText(text);
        button.setClickable(true);
    }

    private void checkEndgame (){
        if (moose.getHealth() <= 0){
            if (player.getHealth() <= 0)
                endgame("tie");
            else
                endgame("win");
        } else if (player.getHealth() <= 0)
            endgame("loss");
    }

    void resetDisabledButtons(){
        for (Button button : disabledButtons)
            enable(button);
        disabledButtons.clear();
    }

    void resetGame() {
        moose.reset();
        player.reset();
        mooseHpBar.setProgress(moose.getHealth());
        playerHpBar.setProgress(player.getHealth());
        logBox.setText("");
        logEvent(R.string.initial_log);
        lastMooseMove = moose.mooseTurn();
    }

    static int random() {
        return ThreadLocalRandom.current().nextInt(0, 100 + 1);
    }

    private void finishTurn (PlayerMove playerMove){
        updateTurns();
        player.tradeBlows(playerMove, lastMooseMove);
        mooseHpBar.setProgress(moose.getHealth());
        playerHpBar.setProgress(player.getHealth());
        checkEndgame();
        lastMooseMove = moose.mooseTurn();
    }

    // Prints the string under resId in the logBox
    void logEvent(int resId){
        String text = getString(resId);
        logBox.append("\n" + text);
        lastEvent = text;
    }

    public void throwPressed (View view){
        finishTurn(PlayerMove.THROW);
    }

    public void dodgePressed (View view){
        finishTurn(PlayerMove.DODGE);
    }

    public void leapPressed (View view){
        finishTurn(PlayerMove.LEAP);
    }

    public void kickPressed (View view){
        finishTurn(PlayerMove.KICK);
    }

    public void attackPressed (View view){
        finishTurn(PlayerMove.ATTACK);
    }

    public void backPressed (View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void retryPressed (View view){
        resetGame();
        startgame();
        hideFragment(endgameFragment);
    }

    public void sharePressed (View view){
        //TODO: social media integration
    }

    public void hideFragment (Fragment fragment){
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.hide(fragment);
        transaction.commit();
    }

    public void showFragment (Fragment fragment){
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.show(fragment);
        transaction.commit();
    }

    @Override
    public void onBackPressed (){
        if (endgameFragment.isVisible())
            finish();
        else {
            DialogFragment dialog = new SurrenderDialogFragment();
            dialog.show(getSupportFragmentManager(), "surrender");
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        endgame("surrender");
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) { /* Do nothing */ }
}
