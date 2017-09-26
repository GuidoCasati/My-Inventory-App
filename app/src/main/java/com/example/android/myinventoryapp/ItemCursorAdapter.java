package com.example.android.myinventoryapp;

/**
 * Created by guido on 21/07/2017.
 */

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import static com.example.android.myinventoryapp.R.drawable.picture_icon;
import static com.example.android.myinventoryapp.data.InventoryContract.InventoryEntry;

/**
 * {@link ItemCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of item data as its data source. This adapter knows
 * how to create list items for each row of item data in the {@link Cursor}.
 */
public class ItemCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ItemCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    protected ItemCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.inventory_item, parent, false);

    }

    /**
     * This method binds the item data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current item can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        // Find individual views that we want to modify in the list item layout
        TextView nameTV = (TextView) view.findViewById(R.id.list_item_name);
        TextView quantityTV = (TextView) view.findViewById(R.id.list_item_quantity);
        TextView priceTV = (TextView) view.findViewById(R.id.list_item_price);
        ImageView picImageView = (ImageView) view.findViewById(R.id.list_item_pic);
        ImageView saleImageView = (ImageView) view.findViewById(R.id.list_item_sale);

        // Find the columns of item attributes that we're interested in
        // and read the attributes from the Cursor for the current item
        int itemName = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_NAME);
        int itemQuantity = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_IN_STOCK_QUANTITY);
        int itemPrice = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_PRICE);
        int itemPic = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_PICTURE);
        int itemId = cursor.getInt(cursor.getColumnIndex(InventoryEntry._ID));
        final String sItemName = cursor.getString(itemName);
        final int iQuantity = cursor.getInt(itemQuantity);

        // create details to show on display
        String sItemPrice = "Price £" + cursor.getString(itemPrice);
        Uri thumbUri = Uri.parse(cursor.getString(itemPic));
        String sItemQuantity = String.valueOf(iQuantity) + " items";
        final Uri currentProductUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, itemId);

        // Update the TextViews with the attributes for the current item
        nameTV.setText(sItemName);
        quantityTV.setText(sItemQuantity);
        priceTV.setText(sItemPrice);

        //Import pictures
        Glide.with(context).load(thumbUri)
                .placeholder(R.drawable.load_picture)
                .error(picture_icon)
                .crossFade()
                .centerCrop()
                .into(picImageView);

        saleImageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ContentResolver resolver = view.getContext().getContentResolver();
                ContentValues values = new ContentValues();
                if (iQuantity > 0) {
                    int qq = iQuantity;
                    values.put(InventoryEntry.COLUMN_ITEM_IN_STOCK_QUANTITY, --qq);
                    resolver.update(
                            currentProductUri,
                            values,
                            null,
                            null
                    );
                    context.getContentResolver().notifyChange(currentProductUri, null);
                } else {
                    Toast.makeText(context, "Item out of stock.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
