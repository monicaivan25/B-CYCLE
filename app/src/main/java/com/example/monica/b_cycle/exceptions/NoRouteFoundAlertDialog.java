package com.example.monica.b_cycle.exceptions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class NoRouteFoundAlertDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Cannot find a route to the given destination")
               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                   }
               });
        return builder.create();
    }
}