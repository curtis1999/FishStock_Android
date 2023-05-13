package com.example.fishstock;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class GameOverDialog extends Dialog {
  int result;
  OnGameOverMoveListener listener;
  Button playAgain;
  Button exit;
  TextView gameResult;
  public interface OnGameOverMoveListener {
    void onGameOver();
  }
  public GameOverDialog(@NonNull Context context, int result) {
    super(context);
    this.result = result;
    setContentView(R.layout.dialog_game_over);
    playAgain = findViewById(R.id.playAgain);
    exit = findViewById(R.id.exit);
    gameResult = findViewById(R.id.winnerMessage);
    if (result == 0) {
      gameResult.setText("DRAW :|");
    } else if(result == 1) {
      gameResult.setText("YOU WIN!! :)"); //TODO: MAKE ISWHITE VARIABLE
    } else {
      gameResult.setText("YOU LOSE  :(");
    }

    playAgain.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(getContext(), GameManager.class); //HELP
        intent.putExtra("agentType", "FishStock");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        // Clear the activity stack and start the game again
        getContext().startActivity(intent);
        dismiss(); // Close the dialog
      }
    });
    exit.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.putExtra("Result", "result"); //TODO:
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        // Clear the activity stack and start the game again
        getContext().startActivity(intent);
        dismiss(); // Close the dialog
      }
    });

  }
  public void setOnGameOverListener(OnGameOverMoveListener listener) {
    this.listener = listener;
  }
}
