package com.rocklinker.Adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rocklinker.R;

public class ExternalArtistListAdapter extends RecyclerView.Adapter <ExternalArtistListAdapter.ViewHolder> {

    private final Activity activity;
    private final com.rocklinker.Common.Handler Handler = new com.rocklinker.Common.Handler();
    private final int R_ID;
    private boolean isBindViewHolderError;

    private final JsonArray jsonArray;
    private JsonArray filteredJsonArray;

    private int lastPosition = 0;

    public ExternalArtistListAdapter(JsonArray jsonArray, Activity activity, int R_ID) {
        this.jsonArray = jsonArray;
        this.filteredJsonArray = jsonArray;
        this.activity = activity;
        this.R_ID = R_ID;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int ViewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_external_artist_list,parent,false);
        return new ViewHolder(view);
    }

    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        try {
            JsonObject jsonObject = filteredJsonArray.get(position).getAsJsonObject();

            String quantity = jsonObject.get("quantity").getAsString()+" músicas";
            viewHolder.textViewArtist.setText(jsonObject.get("artist").getAsString());
            viewHolder.textViewQuantity.setText(quantity);

            //viewHolder.imageViewArt.setImageBitmap(Handler.ImageDecode(jsonObject.get("art").getAsString()));

        }catch (Exception e){
            if(!isBindViewHolderError) {
                Handler.ShowSnack(
                        "Houve um erro",
                        "ExternalArtistListAdapter.onBindViewHolder: " + e.getMessage()+ " on position "+ position,
                        activity,
                        R_ID
                );
                isBindViewHolderError=true;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView imageViewArt;
        TextView textViewArtist, textViewQuantity;

        public ViewHolder(View itemView) {
            super(itemView);
            imageViewArt = itemView.findViewById(R.id.itemExternalArtistList_ImageView_Art);
            textViewArtist = itemView.findViewById(R.id.itemExternalArtistList_TextView_ArtistName);
            textViewQuantity = itemView.findViewById(R.id.itemExternalArtistList_TextView_Quantity);
        }
    }

    public int getItemCount() {
        return filteredJsonArray.size();
    }

    public String getArtistName(int position){
        return filteredJsonArray.get(position).getAsJsonObject().get("artist").getAsString();
    }

    public Filter getFilter() {
        return new Filter()
        {
            protected FilterResults performFiltering(CharSequence charSequence)
            {
                FilterResults results = new FilterResults();

                if(charSequence == null || charSequence.length() == 0){
                    results.values = jsonArray;
                    results.count = jsonArray.size();
                }else{

                    JsonArray newJsonArray = new JsonArray();

                    int i = 0;
                    try {

                        while (i < jsonArray.size()) {
                            String A = jsonArray.get(i).getAsJsonObject().get("artist").toString().toLowerCase();
                            String B = charSequence.toString().toLowerCase();
                            if (A.contains(B)) {
                                newJsonArray.add(jsonArray.get(i));
                            }
                            i++;
                        }

                        results.values = newJsonArray;
                        results.count = newJsonArray.size();

                    }catch (Exception e){
                        results.values = jsonArray;
                        results.count = jsonArray.size();
                    }
                }

                return results;
            }

            protected void publishResults(CharSequence charSequence, FilterResults filterResults)
            {
                filteredJsonArray = (JsonArray) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    public void clearFilter(){
        filteredJsonArray = jsonArray;
    }

    public int getLastPosition(){
        return lastPosition;
    }

    public void setLastPosition(int lastPosition){
        this.lastPosition = lastPosition;
    }
}
