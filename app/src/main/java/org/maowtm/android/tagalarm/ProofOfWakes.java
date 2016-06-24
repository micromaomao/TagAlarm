package org.maowtm.android.tagalarm;

import android.content.res.Resources;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProofOfWakes {
    private ProofRequirement[] prs;
    public ProofOfWakes(JSONArray json) throws JSONException {
        this.prs = new ProofRequirement[json.length()];
        for (int i = 0; i < this.prs.length; i ++) {
            this.prs[i] = ProofRequirement.fromJSONObject(json.getJSONObject(i));
        }
    }
    public JSONArray toJSON() throws JSONException {
        JSONArray ja = new JSONArray();
        for (ProofRequirement pr : prs) {
            if (pr != null)
                ja.put(pr.toJSON());
        }
        return ja;
    }
    public ProofRequirement get(int i) {
        return this.prs[i];
    }
    public int length() {
        return this.prs.length;
    }
    public void set(int i, @Nullable ProofRequirement pr) {
        this.prs[i] = pr;
    }
    public Set<Integer> types() {
        HashSet<Integer> set = new HashSet<>();
        for (ProofRequirement pr : prs) {
            set.add(pr.type);
        }
        return set;
    }
    public int getWaitTime() {
        int minTime = Integer.MAX_VALUE;
        for (ProofRequirement pr : prs) {
            if (pr.type == ProofRequirement.TYPE_WAIT && minTime > pr.amount) {
                minTime = pr.amount;
            }
        }
        return minTime;
    }
    protected Map<Integer, Integer> getAmountMap(int type) {
        Map<Integer, Integer> map = new HashMap<>();
        for (ProofRequirement pr : prs) {
            if (pr.type == type) {
                Integer currentMin = map.get(pr.difficulty);
                if (currentMin == null || pr.amount < currentMin) {
                    map.put(pr.difficulty, pr.amount);
                }
            }
        }
        return map;
    }
    public Map<Integer, Integer> getShakeTimes() {
        return getAmountMap(ProofRequirement.TYPE_SHAKE);
    }
    public Map<Integer, Integer> getMathAmounts() {
        return getAmountMap(ProofRequirement.TYPE_MATH);
    }
    public boolean matchQRCode(String data) {
        for (ProofRequirement pr : prs) {
            if (pr.type == ProofRequirement.TYPE_QRCODE && pr.qrMatch.equals(data)) {
                return true;
            }
        }
        return false;
    }
    public boolean supportQRCode() {
        for (ProofRequirement pr : prs) {
            if (pr.type == ProofRequirement.TYPE_QRCODE)
                return true;
        }
        return false;
    }
    public static class ProofRequirement {
        public static final int TYPE_SHAKE = 1;
        public static final int TYPE_QRCODE = 2;
        public static final int TYPE_MATH = 3;
        public static final int TYPE_LOCATION = 4;
        public static final int TYPE_WAIT = 5;

        public static final int DIFFICULTY_LIGHT = 0;
        public static final int DIFFICULTY_EASY = 1;
        public static final int DIFFICULTY_NORMAL = 2;
        public static final int DIFFICULTY_HARD = 3;
        public static final int DIFFICULTY_CHALLENGING = 4;

        protected final int type;

        public int getAmount() {
            return this.amount;
        }

        public void setAmount(int amount) {
            if (amount <= 0) {
                if (this.type == TYPE_WAIT) {
                    throw new IllegalArgumentException("Seconds must > 0");
                }
                throw new IllegalArgumentException("Amount must be positive.");
            }
            this.amount = amount;
        }

        public int getDifficulty() {
            return this.difficulty;
        }

        public void setDifficulty(int difficulty) {
            if (difficulty < DIFFICULTY_LIGHT || difficulty > DIFFICULTY_CHALLENGING) {
                throw new IllegalArgumentException("Difficulty not in range.");
            }
            this.difficulty = difficulty;
        }

        private int amount;
        private int difficulty;

        public String getQrMatch() {
            return this.qrMatch;
        }

        public void setQrMatch(String qrMatch) {
            if (qrMatch.length() <= 0)
                throw new IllegalArgumentException("qrMatch must not be empty if your type is QRCODE.");
            this.qrMatch = qrMatch;
        }

        private String qrMatch;
        protected ProofRequirement(JSONObject obj) throws JSONException {
            this.type = obj.getInt("type");
            switch (this.type) {
                case TYPE_SHAKE:
                case TYPE_MATH:
                    this.setAmount(obj.getInt("amount"));
                    this.setDifficulty(obj.getInt("difficulty"));
                    break;
                case TYPE_QRCODE:
                    this.setQrMatch(obj.getString("qrMatch"));
                    break;
                case TYPE_WAIT:
                    this.setAmount(obj.getInt("second"));
                    break;
                case TYPE_LOCATION:
                    throw new UnsupportedOperationException("TODO"); // TODO
                default:
                    throw new IllegalArgumentException("Type not recognized.");
            }
        }
        public static ProofRequirement fromJSONObject(JSONObject object) throws JSONException {
            return new ProofRequirement(object);
        }
        public JSONObject toJSON() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("type", this.type);
            switch (this.type) {
                case TYPE_SHAKE:
                case TYPE_MATH:
                    obj.put("amount", this.amount);
                    obj.put("difficulty", this.difficulty);
                    break;
                case TYPE_QRCODE:
                    obj.put("qrMatch", this.qrMatch);
                    break;
                case TYPE_WAIT:
                    obj.put("second", this.amount);
                    break;
                case TYPE_LOCATION:
                    throw new UnsupportedOperationException("TODO"); // TODO
                default:
                    throw new IllegalArgumentException("Type not recognized.");
            }
            return obj;
        }
        public ProofRequirement(ProofRequirement clone) {
            this.type = clone.type;
            this.amount = clone.amount;
            this.difficulty = clone.difficulty;
            this.qrMatch = clone.qrMatch;
        }

        public String toString(Resources resources) {
            String typeTemplate = resources.getStringArray(R.array.pow_types)[this.type];
            switch (this.type) {
                case TYPE_SHAKE:
                    return String.format(typeTemplate, this.amount, this.difficulty + 1);
                case TYPE_WAIT:
                    return String.format(typeTemplate, (this.amount % 60 == 0 ? this.amount / 60 : Float.toString((this.amount / 6) / 10f)));
                default:
                    return typeTemplate;
            }
        }
    }
}
