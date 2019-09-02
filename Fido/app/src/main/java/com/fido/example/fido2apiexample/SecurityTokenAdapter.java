/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.fido.example.fido2apiexample;

import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Manager for security key token layout in UI */
public class SecurityTokenAdapter extends RecyclerView.Adapter<SecurityTokenAdapter.ViewHolder> {

    private final List<Map<String, String>> tokens;
    private final Map<Integer, Boolean> checked;
    private final int rowLayout;
    private final Fido2DemoActivity activity;

    public SecurityTokenAdapter(
            List<Map<String, String>> tokens, int rowLayout, Fido2DemoActivity activity) {
        this.tokens = tokens;
        this.checked = new HashMap<>();
        this.rowLayout = rowLayout;
        this.activity = activity;
    }

    public void clearSecurityTokens() {
        int size = this.tokens.size();
        if (size > 0) {
            tokens.subList(0, size).clear();
            this.notifyItemRangeRemoved(0, size);
        }
    }

    public void addSecurityToken(List<Map<String, String>> tokens) {
        this.tokens.addAll(tokens);
        this.notifyItemRangeInserted(0, tokens.size() - 1);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(rowLayout, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int item) {
        Map<String, String> token = tokens.get(item);
        if (token.containsKey("handle")) {
            viewHolder.content.setText("Handle: " + token.get("handle") + "\n");
        } else {
            viewHolder.content.setText(buildFullContent(token));
        }
        viewHolder.content.setOnClickListener(
                view -> new AlertDialog.Builder(activity).setMessage(buildFullContent(token)).show());
        viewHolder.removeButton.setOnClickListener(
                v -> {
                    // Confirm to remove it
                    new AlertDialog.Builder(activity)
                            .setTitle("confirm to remove security token")
                            .setMessage("Are you sure to delete this security token?")
                            .setPositiveButton("Yes", (dialog, which) -> activity.removeTokenByIndexInList(item))
                            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                            .show();
                });
        checked.put(item, viewHolder.checkBox.isChecked());
        viewHolder.checkBox.setOnCheckedChangeListener(
                (compoundButton, b) -> checked.put(item, viewHolder.checkBox.isChecked()));
    }

    @Override
    public int getItemCount() {
        return tokens == null ? 0 : tokens.size();
    }

    public ImmutableList<Map<String, String>> getCheckedItems() {
        ImmutableList.Builder<Map<String, String>> result = ImmutableList.builder();
        for (int i = 0; i < tokens.size(); i++) {
            // Default to treating the token entry as checked if a value isn't stored.
            if (!checked.containsKey(i) || checked.get(i)) {
                result.add(tokens.get(i));
            }
        }
        return result.build();
    }

    private static String buildFullContent(Map<String, String> token) {
        return Joiner.on('\n').withKeyValueSeparator(": ").join(token);
    }

    /** Customed ViewHolder implementation. */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView content;
        public final Button removeButton;
        public final CheckBox checkBox;

        public ViewHolder(View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.information);
            removeButton = itemView.findViewById(R.id.btn_remove);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }
}
