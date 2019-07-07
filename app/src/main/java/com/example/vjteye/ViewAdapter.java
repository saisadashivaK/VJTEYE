package com.example.vjteye;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.LinkedList;


public class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.ListViewHolder> {
    private final LinkedList<Site> detectedImages;
    private LayoutInflater layoutInflater;
    private Site current;

    public ViewAdapter(LinkedList<Site> detectedImages) {

        this.detectedImages = detectedImages;

    }



    @NonNull
    @Override
    public ViewAdapter.ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        layoutInflater = LayoutInflater.from(parent.getContext());
        View nItemView = layoutInflater.inflate(R.layout.layout_recycler_element, parent, false);
        return new ViewAdapter.ListViewHolder(nItemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewAdapter.ListViewHolder holder, int position) {
            current = detectedImages.get(position);
            holder.itemView.setText(current.getName());
            holder.description.setText(current.getInfo());
               }

    @Override
    public int getItemCount() {
        return detectedImages.size();
    }

    public class ListViewHolder extends RecyclerView.ViewHolder{
        public  TextView itemView;
        public TextView description;
        public ImageView imageView;
        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView.findViewById(R.id.title);
            this.description = itemView.findViewById(R.id.siteInfo);
            this.imageView = itemView.findViewById(R.id.button_remove);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    detectedImages.remove(getAdapterPosition());
                    notifyItemRemoved(getAdapterPosition());
                }
            });
        }

    }
}
