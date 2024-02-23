package de.kaiserdragon.iconrequest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppAdapter extends RecyclerView.Adapter<AppViewHolder> {
    private final List<AppInfo> appList;
    private final List<AppInfo> filteredList;
    private final boolean iPackMode;
    private final boolean secondIcon;

    private RequestActivity requestActivity;

    public AppAdapter(List<AppInfo> appList,Boolean iPacksMode,Boolean SecondIcon,RequestActivity RequestActivity) {
        this.appList = appList;
        this.filteredList = new ArrayList<>(appList);
        iPackMode = iPacksMode;
        requestActivity =RequestActivity;
        secondIcon = SecondIcon;
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(appList);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            for (AppInfo app : appList) {
                if (app.getLabel().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    public int AdapterSize(){
        return this.appList.size();
    }

    public ArrayList<AppInfo> getAllSelected() {
        ArrayList<AppInfo> arrayList = new ArrayList<>();
        for (AppInfo app : appList) {
            if (app.selected) arrayList.add(app);
        }
        return arrayList;
    }

    public void setAllSelected(boolean selected) {
        for (AppInfo app : appList) {
            app.setSelected(selected);
        }
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item, parent, false);
        return new AppViewHolder(v, filteredList,iPackMode,requestActivity);
    }


    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = filteredList.get(position);
        holder.labelView.setText(app.getLabel());
        holder.packageNameView.setText(app.packageName);
        holder.classNameView.setText(app.className);
        holder.imageView.setImageDrawable(app.getIcon());
        if (app.selected) holder.checkBox.setDisplayedChild(1);
        else holder.checkBox.setDisplayedChild(0);
        if (secondIcon) { //((SecondIcon || mode == 3 || mode == 4) && IPackChoosen && !(mode == 2))
            holder.apkIconView.setVisibility(View.VISIBLE);
            holder.apkIconView.setImageDrawable(app.getIcon2());
        }

    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }
}

