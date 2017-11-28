package com.zlata.drawing;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class DrawingActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int RESULT_LOAD_IMAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION = 100;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION = 101;

    private static final String KEY_COLOR = "key_color";
    private static final String KEY_TOOL = "key_tool";
    private static final String KEY_IMAGE = "key_image";

    public DrawingView paintView;
    private MenuItem colorPickerItem;
    private AlertDialog colorPickerDialog;

    private String currentPhotoPath;

    private Integer color = Color.BLACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        paintView = (DrawingView) findViewById(R.id.paintView);

        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri imageUri = intent.getData();
            if (imageUri != null) {
                String path = imageUri.getPath();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;

                Bitmap image = BitmapFactory.decodeFile(path, options);
                paintView.setBackground(image);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int color = savedInstanceState.getInt(KEY_COLOR);
        paintView.setColor(color);
        this.color = color;
        Bitmap image = savedInstanceState.getParcelable(KEY_IMAGE);
        paintView.setBackground(image);
        int tool = savedInstanceState.getInt(KEY_TOOL);
        paintView.setTool(tool);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(KEY_COLOR, paintView.getColor());
        savedInstanceState.putInt(KEY_TOOL, paintView.getTool());
        savedInstanceState.putParcelable(KEY_IMAGE, paintView.getImage());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.paint, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_undo:
                paintView.undo();
                return true;
            case R.id.action_clear:
                paintView.clear();
                return true;

            case R.id.tool_color:
                if (color != null) {
                    Drawable icon = item.getIcon();
                }
                //ColorPicker Dialog
                ColorPickerDialogBuilder
                        .with(this)
                        .setTitle("Choose color")
                        .initialColor(color)
                        .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                        .density(12)
                        .setOnColorSelectedListener(new OnColorSelectedListener() {
                            @Override
                            public void onColorSelected(int selectedColor) {
                                System.out.println(selectedColor);
                            }
                        })
                        .setPositiveButton("ok", new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                color = selectedColor;
                                paintView.setColor(color);
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .build()
                        .show();

               colorPickerItem = item;
                return true;
            case R.id.tool_stroke_width:
                showChangeWidthDialog(item);
                return true;
            case R.id.action_open_image:
                dispatchOpenPictureIntent();
                return true;
            case R.id.action_take_photo:
                dispatchTakePictureIntent();
                return true;
            case R.id.action_save:
                if (saveImage() != null) {
                    displayInfoToast(getString(R.string.message_saved));
                } else {
                    displayInfoToast(getString(R.string.message_error_saving));
                }
                return true;
            case R.id.action_share:
                dispatchSharePictureIntent();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            File imageFile = new File(picturePath);

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inMutable = true;

            Bitmap image = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), o);

            paintView.setBackground(image);
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inMutable = true;

            Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath, o);

            paintView.setBackground(imageBitmap);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.tool_move:
                paintView.setTool(DrawingTools.MOVE);
                break;
            case R.id.tool_brush:
                paintView.setTool(DrawingTools.BRUSH);
                break;
            case R.id.tool_rectangle:
                paintView.setTool(DrawingTools.RECTANGLE);
                break;
            case R.id.tool_oval:
                paintView.setTool(DrawingTools.OVAL);
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public String saveImage() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_PERMISSION);
        } else {
            final String name = "paint_" + System.currentTimeMillis() + ".jpg";
            Bitmap image = paintView.getImage();
            String location = MediaStore.Images.Media.insertImage(
                    this.getContentResolver(), image, name,
                    name);
            return location;
        }
        return null;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                displayInfoToast(getString(R.string.error_share_message));
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(

                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void showChangeWidthDialog(final MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getText(R.string.stroke_width_dialog_title));

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(30);
        seekBar.setProgress(paintView.getStrokeWidth());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i < 5) i = 5;
                paintView.setStrokeWidth(i);
                item.setTitle(String.valueOf(i) + "px");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        builder.setView(seekBar);

        colorPickerDialog = builder.create();
        colorPickerDialog.show();
    }

    private void dispatchOpenPictureIntent() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_EXTERNAL_STORAGE_PERMISSION);
        } else {
            Intent i = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, RESULT_LOAD_IMAGE);
        }
    }

    private void dispatchSharePictureIntent() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        String path = saveImage();
        if (path != null) {
            Uri uri = Uri.parse(path);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType("image/jpeg");
            startActivity(shareIntent);
        }
    }

    private void displayInfoToast(String message) {
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(this, message, duration);
        toast.show();
    }
}
