package com.example.myapplication;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import android.util.Log;
import android.widget.ImageButton;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.ViewHolder> {
    private List<Food> foodList;
    private FoodDatabase foodDatabase;
    private static final String TAG = "FoodAdapter";

    public FoodAdapter(List<Food> foodList, FoodDatabase database) {
        this.foodList = foodList;
        this.foodDatabase = database;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.food_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {
            Food food = foodList.get(position);
            holder.nameTextView.setText(food.getName());
            holder.expiryTextView.setText("保质期至: " + food.getExpiryDate());
            holder.quantityTextView.setText("数量: " + food.getQuantity());
            
            // 检查日期并设置颜色
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date expiryDate = sdf.parse(food.getExpiryDate());
                Date currentDate = new Date();
                
                // 计算日期差
                long diff = expiryDate.getTime() - currentDate.getTime();
                long daysLeft = diff / (24 * 60 * 60 * 1000);
                
                int textColor;
                if (daysLeft < 0) {
                    textColor = Color.RED;
                } else if (daysLeft <= 1) {
                    textColor = holder.itemView.getContext().getResources().getColor(R.color.warning_yellow);
                } else {
                    textColor = holder.itemView.getContext().getResources().getColor(R.color.text_color);
                }
                
                holder.nameTextView.setTextColor(textColor);
                holder.expiryTextView.setTextColor(textColor);
                holder.quantityTextView.setTextColor(textColor);
                
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: ", e);
            }
            
            holder.editButton.setOnClickListener(v -> showEditDialog(food, position, v));
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onBindViewHolder: ", e);
        }
    }

    private void showEditDialog(Food food, int position, View view) {
        View dialogView = LayoutInflater.from(view.getContext())
                .inflate(R.layout.dialog_manual_input, null);
        
        EditText nameEdit = dialogView.findViewById(R.id.editTextName);
        EditText expiryEdit = dialogView.findViewById(R.id.editTextExpiry);
        EditText quantityEdit = dialogView.findViewById(R.id.editTextQuantity);
        
        // 设置当前值
        nameEdit.setText(food.getName());
        quantityEdit.setText(String.valueOf(food.getQuantity()));
        
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setView(dialogView)
                .setTitle("修改食材信息")
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = nameEdit.getText().toString();
                    String expiryDays = expiryEdit.getText().toString();
                    String quantityStr = quantityEdit.getText().toString();
                    
                    if (name.isEmpty()) name = "未命名";
                    int days = expiryDays.isEmpty() ? 7 : Integer.parseInt(expiryDays);
                    int quantity = quantityStr.isEmpty() ? 1 : Integer.parseInt(quantityStr);
                    
                    // 计算保质期截止日期
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_MONTH, days);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String expiryDate = sdf.format(calendar.getTime());
                    
                    // 更新数据库
                    Food updatedFood = new Food(food.getId(), name, expiryDate, quantity);
                    foodDatabase.updateFood(food.getId(), updatedFood);
                    
                    // 更新列表
                    foodList.set(position, updatedFood);
                    notifyItemChanged(position);
                    
                    Toast.makeText(view.getContext(), "修改成功", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    public void removeItem(int position) {
        Food food = foodList.get(position);
        foodDatabase.deleteFood(food.getId());  // 需要在FoodDatabase中添加此方法
        foodList.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView expiryTextView;
        public TextView quantityTextView;
        public ImageButton editButton;

        public ViewHolder(View view) {
            super(view);
            nameTextView = view.findViewById(R.id.nameTextView);
            expiryTextView = view.findViewById(R.id.expiryTextView);
            quantityTextView = view.findViewById(R.id.quantityTextView);
            editButton = view.findViewById(R.id.editButton);
        }
    }
} 