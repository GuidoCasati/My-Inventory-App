package com.example.android.myinventoryapp;

/**
 * Created by guido on 21/07/2017.
 */

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android.myinventoryapp.data.InventoryContract.InventoryEntry;

import java.io.File;

import static com.example.android.myinventoryapp.R.drawable.picture_icon;


public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // Global variables
    public static final String TAG = EditorActivity.class.getSimpleName();
    public static final int PICK_PHOTO_REQUEST = 20;
    public static final int EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE = 21;

    /**
     * Identifier for the item data loader
     */
    private static final int EXISTING_ITEM_LOADER = 0;

    //General Product QUERY PROJECTION
    public final String[] PRODUCT_COLS = {
            InventoryEntry._ID,
            InventoryEntry.COLUMN_ITEM_NAME,
            InventoryEntry.COLUMN_ITEM_IN_STOCK_QUANTITY,
            InventoryEntry.COLUMN_ITEM_PRICE,
            InventoryEntry.COLUMN_ITEM_CATEGORY,
            InventoryEntry.COLUMN_ITEM_SUPPLIER_CONTACT,
            InventoryEntry.COLUMN_ITEM_PICTURE
    };
    /**
     * Content URI for the existing item (null if it's a new item)
     */
    private Uri mCurrentItemUri;
    /**
     * ImageView field to enter the picture of the item
     **/
    private ImageView mItemLoadImage;

    /**
     * EditText field to enter the item name, price, quantity
     */
    private EditText mNameEditText;
    private EditText mPriceEditText;
    private EditText mQuantityEditText;
    private EditText mSupplierEditText;
    private EditText mCategoryEditText;

    //Buttons
    private Button mPlusButton;
    private Button mMinusButton;
    private ImageButton deleteButton;
    private ImageButton orderButton;
    private ImageButton saveButton;

    // photo uri
    private String mCurrentPhotoUri = "NO PIC";

    // variables used to update edittext view for quantity
    private int mQuantity;


    /**
     * Boolean flag that keeps track of whether the item has been edited (true) or not (false)
     */
    private boolean mItemHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mItemHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_item_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_item_price);
        mItemLoadImage = (ImageView) findViewById(R.id.edit_item_image);
        mSupplierEditText = (EditText) findViewById(R.id.edit_item_supplier);
        mCategoryEditText = (EditText) findViewById(R.id.edit_item_category);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mItemLoadImage.setOnTouchListener(mTouchListener);
        mCategoryEditText.setOnTouchListener(mTouchListener);

        //find all buttons
        mPlusButton = (Button) findViewById(R.id.button_plus);
        mMinusButton = (Button) findViewById(R.id.button_minus);
        deleteButton = (ImageButton) findViewById(R.id.delete_button);
        orderButton = (ImageButton) findViewById(R.id.order_button);
        saveButton = (ImageButton) findViewById(R.id.save_button);

        //setup listener on the picture
        mItemLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPhotoProductUpdate(view);
            }
        });

        //setup listeners on the buttons

        mMinusButton.setOnTouchListener(mTouchListener);
        mPlusButton.setOnTouchListener(mTouchListener);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProduct();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteConfirmationDialog();
            }
        });

        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                orderSupplier();
            }
        });

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new item or editing an existing one.
        mCurrentItemUri = getIntent().getData();

        // If the intent DOES NOT contain an item content URI, then we know that we are
        // creating a new item.
        if (mCurrentItemUri == null) {
            // This is a new item, so change the app bar to say "add item"
            setTitle(getString(R.string.editor_activity_title_add_item));

            //hide order and delete buttons for new items
            orderButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);

        } else {
            // Otherwise this is an existing item, so change app bar to say "Edit Item"
            setTitle(getString(R.string.editor_activity_title_edit_item));

            //show all buttons
            orderButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);

            // Initialize a loader to read the item data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }
    }

    /**
     * This method will inflate the menu options from the res/menu/menu_editor.xml file.
     * This adds menu items to the app bar.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;

    }

    /**
     * This method is called when the user clicked on a menu option in the app bar overflow menu
     **/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // to be implemented
            case R.id.settings:
                return true;

            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the item hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;

        }

        return super.onOptionsItemSelected(item);

    }


    public void onPhotoProductUpdate(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //We are on M or above so we need to ask for runtime permissions
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                invokeGetPhoto();
            } else {
                // we are here if we do not all ready have permissions
                String[] permisionRequest = {Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(permisionRequest, EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE);
            }
        } else {
            //We are on an older devices so we dont have to ask for runtime permissions
            invokeGetPhoto();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //permission is granted
            invokeGetPhoto();
        } else {
            // need to update permissions
            Toast.makeText(this, R.string.error_msg_missing_permissions, Toast.LENGTH_LONG).show();
        }
    }

    private void invokeGetPhoto() {
        // invoke the image gallery using an implict intent.
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);

        // where do we want to find the data?
        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String pictureDirectoryPath = pictureDirectory.getPath();
        // finally, get a URI representation
        Uri data = Uri.parse(pictureDirectoryPath);

        // set the data and type.  Get all image types.
        photoPickerIntent.setDataAndType(data, "image/*");

        // we will invoke this activity, and get something back from it.
        startActivityForResult(photoPickerIntent, PICK_PHOTO_REQUEST);
    }

    @Override
    public void onBackPressed() {
        //Go back if we have no changes
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        //otherwise Protect user from loosing info
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_PHOTO_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                //If we are here, everything processed successfully and we have an Uri data
                Uri mProductPhotoUri = data.getData();
                mCurrentPhotoUri = mProductPhotoUri.toString();
                Log.d(TAG, "Selected images " + mProductPhotoUri);

                //We use Glide to import photo images
                Glide.with(this).load(mProductPhotoUri)
                        .placeholder(picture_icon)
                        .crossFade()
                        .fitCenter()
                        .into(mItemLoadImage);
            }
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(this,
                mCurrentItemUri,
                PRODUCT_COLS,
                null,
                null,
                null);
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {

            int i_ID = 0;
            int i_COL_NAME = 1;
            int i_COL_QUANTITY = 2;
            int i_COL_PRICE = 3;
            int i_COL_CATEGORY = 4;
            int i_COL_SUPPLIER = 5;
            int i_COL_PICTURE = 6;

            // Extract values from current cursor
            String name = cursor.getString(i_COL_NAME);
            int quantity = cursor.getInt(i_COL_QUANTITY);
            mQuantity = quantity;
            float price = cursor.getFloat(i_COL_PRICE);
            String category = cursor.getString(i_COL_CATEGORY);
            String supplier = cursor.getString(i_COL_SUPPLIER);
            String photo = cursor.getString(i_COL_PICTURE);
            mCurrentPhotoUri = cursor.getString(i_COL_PICTURE);


            //We updates fields to values on DB
            mNameEditText.setText(name);
            mPriceEditText.setText(String.valueOf(price));
            mQuantityEditText.setText(String.valueOf(quantity));
            mCategoryEditText.setText(category);
            mSupplierEditText.setText(supplier);
            //Update photo using Glide
            Glide.with(this).load(mCurrentPhotoUri)
                    .placeholder(R.drawable.load_picture)
                    .error(picture_icon)
                    .crossFade()
                    .fitCenter()
                    .into(mItemLoadImage);
        }

    }


    /**
     * Get user input from editor and save/update product into database.
     */
    private void saveProduct() {
        //Read Values from text field
        String nameString = mNameEditText.getText().toString().trim();
        String categoryString = mCategoryEditText.getText().toString().trim();
        String inventoryString = mQuantityEditText.getText().toString().toString();
        String priceString = mPriceEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();

        //Check if is new or if an update
        if (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(categoryString)
                || TextUtils.isEmpty(inventoryString) || TextUtils.isEmpty(priceString) || TextUtils.isEmpty(supplierString)) {

            Toast.makeText(this, R.string.error_msg_empty_fields, Toast.LENGTH_SHORT).show();
            // No change has been made so we can return
            return;
        }

        //We set values for insert update
        ContentValues values = new ContentValues();

        values.put(InventoryEntry.COLUMN_ITEM_NAME, nameString);
        values.put(InventoryEntry.COLUMN_ITEM_CATEGORY, categoryString);
        values.put(InventoryEntry.COLUMN_ITEM_IN_STOCK_QUANTITY, inventoryString);
        values.put(InventoryEntry.COLUMN_ITEM_PRICE, priceString);
        values.put(InventoryEntry.COLUMN_ITEM_SUPPLIER_CONTACT, supplierString);
        values.put(InventoryEntry.COLUMN_ITEM_PICTURE, mCurrentPhotoUri);

        if (mCurrentItemUri == null) {

            Uri insertedRow = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            if (insertedRow == null) {
                Toast.makeText(this, R.string.error_msg_wrong_insert, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.success_msg_update, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, CatalogActivity.class);
                startActivity(intent);
            }
        } else {
            // We are Updating
            int rowUpdated = getContentResolver().update(mCurrentItemUri, values, null, null);

            if (rowUpdated == 0) {
                Toast.makeText(this, R.string.error_msg_wrong_insert, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.success_msg_update, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, CatalogActivity.class);
                startActivity(intent);

            }
        }
    }

    public void increment(View view) {
        mQuantity++;
        displayQuantity();
    }

    public void decrement(View view) {
        if (mQuantity == 0) {
            Toast.makeText(this, R.string.no_negative, Toast.LENGTH_SHORT).show();
        } else {
            mQuantity--;
            displayQuantity();
        }
    }

    public void displayQuantity() {
        mQuantityEditText.setText(String.valueOf(mQuantity));
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mCategoryEditText.setText("");
        mSupplierEditText.setText("");

    }


    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_msg_inventory_incomplete);
        builder.setPositiveButton(R.string.discard_msg, discardButtonClickListener);
        builder.setNegativeButton(R.string.stay_msg, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    /**
     * Prompt the user to confirm that they want to delete this item.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_msg_delete);
        builder.setPositiveButton(R.string.msg_delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.msg_cancel, new DialogInterface.OnClickListener() {
            // User clicked the "Cancel" button, so dismiss the dialog
            // and continue editing the item.
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the item in the database.
     */
    private void deleteItem() {
        // Only perform the delete if this is an existing item
        if (mCurrentItemUri != null) {
            // Call the ContentResolver to delete the item at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentItemUri
            // content URI already identifies the item that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.error_msg_delete),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.success_msg_delete),
                        Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
    }

    /**
     * Create the intent to send order for new items
     */
    private void orderSupplier() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + mSupplierEditText.getText().toString().trim()));
        intent.setType("text/plain");
        String name = mNameEditText.getText().toString().trim();
        intent.putExtra(Intent.EXTRA_SUBJECT, "New order of " + name);
        String message = "Hi, we would like to order a new shipment of " + name + " in quantity " + mQuantityEditText.getText().toString().trim() + " items ";
        intent.putExtra(android.content.Intent.EXTRA_TEXT, message);

        try {
            startActivity(Intent.createChooser(intent, "Sending email..."));
        } catch (android.content.ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }

}

