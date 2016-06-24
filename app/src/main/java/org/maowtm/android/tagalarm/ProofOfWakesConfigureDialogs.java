package org.maowtm.android.tagalarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public abstract class ProofOfWakesConfigureDialogs {
    private static ProofOfWakes.ProofRequirement praseJSONFromArgument(Bundle arguments) {
        String prJSON = arguments.getString("pr");
        if (prJSON == null)
            throw new IllegalStateException("no pr argument.");
        try {
            return ProofOfWakes.ProofRequirement.fromJSONObject(new JSONObject(prJSON));
        } catch (JSONException e) {
            throw new IllegalStateException("pr argument not valid.", e);
        }
    }
    public interface PowUpdateCallback {
        void onSet(ProofOfWakes.ProofRequirement newPr);
    }

    protected abstract static class PowConfigDialogFragment extends DialogFragment {
        protected View contentView;
        private PowUpdateCallback callback;
        public void setUpdateCallback(PowUpdateCallback updateCallback) {
            callback = updateCallback;
        }
        protected void triggerCallback(ProofOfWakes.ProofRequirement pr) {
            if (this.callback != null)
                this.callback.onSet(pr);
        }
    }

    public static class ShakeDialogFragment extends PowConfigDialogFragment {
        protected LayoutInflater inflater;
        protected ProofOfWakes.ProofRequirement pr;
        protected int current_difficulty;
        public ShakeDialogFragment() {
            super();
        }
        @Override
        public Dialog onCreateDialog(Bundle sis) {
            this.inflater = (LayoutInflater) this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.pr = praseJSONFromArgument(this.getArguments());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.powconfig_shake_title);
            View vg = inflater.inflate(R.layout.powconfigshake, null);
            ((EditText) vg.findViewById(R.id.powconfig_shake_amount))
                    .setText(String.format(Locale.getDefault(), "%d", pr.getAmount()));
            builder.setView(vg);
            this.contentView = vg;
            this.setCurrentDifficulty(pr.getDifficulty());
            builder.setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ProofOfWakes.ProofRequirement pr = ShakeDialogFragment.this.pr;
                    View vg = ShakeDialogFragment.this.contentView;
                    try {
                        pr.setAmount(Integer.parseInt(((EditText) vg.findViewById(R.id.powconfig_shake_amount))
                                .getText().toString()));
                        pr.setDifficulty(current_difficulty);
                        triggerCallback(pr);
                    } catch (IllegalArgumentException e) {}
                }
            });
            return builder.create();
        }

        protected void setCurrentDifficulty(int difficulty) {
            this.current_difficulty = difficulty;
            ((TextView) this.contentView.findViewById(R.id.powconfig_current_shake_difficulty))
                    .setText(this.getString(R.string.powconfig_current_shake_difficulty, this.current_difficulty + 1));
        }
    }
    public static class WaitDialogFragment extends PowConfigDialogFragment {
        protected LayoutInflater inflater;
        protected ProofOfWakes.ProofRequirement pr;
        public WaitDialogFragment() {
            super();
        }
        @Override
        public Dialog onCreateDialog(Bundle sis) {
            this.inflater = (LayoutInflater) this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.pr = praseJSONFromArgument(this.getArguments());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.powconfig_wait_title);
            View vg = inflater.inflate(R.layout.powconfigwait, null);
            int seconds = pr.getAmount();
            ((EditText) vg.findViewById(R.id.powconfig_wait_minutes))
                    .setText(seconds % 60 == 0 ? Integer.toString(seconds / 60) : Float.toString((seconds / 6) / 10f));
            builder.setView(vg);
            this.contentView = vg;
            builder.setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ProofOfWakes.ProofRequirement pr = WaitDialogFragment.this.pr;
                    View vg = WaitDialogFragment.this.contentView;
                    try {
                        pr.setAmount((int) (Float.parseFloat(((EditText) vg.findViewById(R.id.powconfig_wait_minutes))
                                .getText().toString()) * 60));
                        triggerCallback(pr);
                    } catch (IllegalArgumentException e) {}
                }
            });
            return builder.create();
        }
    }
}
