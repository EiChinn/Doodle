package cn.hzw.doodledemo;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.forward.androids.utils.ImageUtils;
import cn.forward.androids.utils.LogUtil;
import cn.forward.androids.utils.StatusBarUtil;
import cn.forward.androids.utils.Util;
import cn.hzw.doodle.DoodleBitmap;
import cn.hzw.doodle.DoodleColor;
import cn.hzw.doodle.DoodleOnTouchGestureListener;
import cn.hzw.doodle.DoodleParams;
import cn.hzw.doodle.DoodlePath;
import cn.hzw.doodle.DoodlePen;
import cn.hzw.doodle.DoodleShape;
import cn.hzw.doodle.DoodleText;
import cn.hzw.doodle.DoodleTouchDetector;
import cn.hzw.doodle.DoodleView;
import cn.hzw.doodle.IDoodleListener;
import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodle.core.IDoodleColor;
import cn.hzw.doodle.core.IDoodleItem;
import cn.hzw.doodle.core.IDoodleItemListener;
import cn.hzw.doodle.core.IDoodlePen;
import cn.hzw.doodle.core.IDoodleSelectableItem;
import cn.hzw.doodle.core.IDoodleShape;
import cn.hzw.doodle.core.IDoodleTouchDetector;
import cn.hzw.doodle.dialog.DialogController;
import cn.hzw.doodle.imagepicker.ImageSelectorView;
import cn.hzw.doodledemo.databinding.DoodleLayoutBinding;
import cn.hzw.doodledemo.utils.ImgUtils;

/**
 * 涂鸦界面，根据DoodleView的接口，提供页面交互
 * （这边代码和ui比较粗糙，主要目的是告诉大家DoodleView的接口具体能实现什么功能，实际需求中的ui和交互需另提别论）
 * Created by huangziwei(154330138@qq.com) on 2016/9/3.
 */
public class DoodleActivity extends AppCompatActivity implements DoodleContract.View {

    private DoodleLayoutBinding binding;

    private DoodlePresenter mPresenter;

    public static final String TAG = "Doodle";
    public final static int DEFAULT_MOSAIC_SIZE = 20; // 默认马赛克大小
    public final static int DEFAULT_COPY_SIZE = 20; // 默认仿制大小
    public final static int DEFAULT_TEXT_SIZE = 18; // 默认文字大小
    public final static int DEFAULT_BITMAP_SIZE = 80; // 默认贴图大小

    public static final int RESULT_ERROR = -111; // 出现错误

    /**
     * 启动涂鸦界面
     *
     * @param activity
     * @param params      涂鸦参数
     * @param requestCode startActivityForResult的请求码
     * @see DoodleParams
     */
    public static void startActivityForResult(Activity activity, DoodleParams params, int requestCode) {
        Intent intent = new Intent(activity, DoodleActivity.class);
        intent.putExtra(DoodleActivity.KEY_PARAMS, params);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 启动涂鸦界面
     *
     * @param activity
     * @param imagePath   　图片路径
     * @param savePath    　保存路径
     * @param isDir       　保存路径是否为目录
     * @param requestCode 　startActivityForResult的请求码
     */
    @Deprecated
    public static void startActivityForResult(Activity activity, String imagePath, String savePath, boolean isDir, int requestCode) {
        DoodleParams params = new DoodleParams();
        params.mImagePath = imagePath;
        params.mSavePath = savePath;
        params.mSavePathIsDir = isDir;
        startActivityForResult(activity, params, requestCode);
    }

    /**
     * {@link DoodleActivity#startActivityForResult(Activity, String, String, boolean, int)}
     */
    @Deprecated
    public static void startActivityForResult(Activity activity, String imagePath, int requestCode) {
        DoodleParams params = new DoodleParams();
        params.mImagePath = imagePath;
        startActivityForResult(activity, params, requestCode);
    }

    public static final String KEY_PARAMS = "key_doodle_params";
    public static final String KEY_IMAGE_PATH = "key_image_path";

    private String mImagePath;

    private FrameLayout mFrameLayout;
    private IDoodle mDoodle;
    private DoodleView mDoodleView;

    private boolean isPanelHide = false;
    private boolean isLegend = false;
    private View mSettingsPanel;
    private View mSelectedEditContainer;
    private TextView mItemScaleTextView;
    private View mBtnColor, mColorContainer;
    private View mShapeContainer, mPenContainer;
    private View mBtnUndo;
    private View mMosaicMenu;
    private View mEditBtn;
    private View mRedoBtn;

    private AlphaAnimation mViewShowAnimation, mViewHideAnimation; // view隐藏和显示时用到的渐变动画

    private DoodleParams mDoodleParams;

    private DoodleOnTouchGestureListener mTouchGestureListener;
    private Map<IDoodlePen, Float> mPenSizeMap = new HashMap<>(); //保存每个画笔对应的最新大小

    private int mMosaicLevel = -1;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_PARAMS, mDoodleParams);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        mDoodleParams = savedInstanceState.getParcelable(KEY_PARAMS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setStatusBarTranslucent(this, true, false);
        EventBus.getDefault().register(this);
        mPresenter = new DoodlePresenter(this);

        if (mDoodleParams == null) {
            mDoodleParams = getIntent().getExtras().getParcelable(KEY_PARAMS);
        }
        if (mDoodleParams == null) {
            LogUtil.e("TAG", "mDoodleParams is null!");
            this.finish();
            return;
        }

        mImagePath = mDoodleParams.mImagePath;

        LogUtil.d("TAG", mImagePath);
        if (mDoodleParams.mIsFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        Bitmap bitmap;
        if (mImagePath.isEmpty()) {
            bitmap = ImgUtils.createBitmapFromScratch(this);
        } else {
            bitmap = ImageUtils.createBitmapFromPath(mImagePath, this);
        }
        if (bitmap == null) {
            LogUtil.e("TAG", "bitmap is null!");
            this.finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DoodleLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initToolbar();
        resetState();
        mFrameLayout = (FrameLayout) findViewById(cn.hzw.doodle.R.id.doodle_container);

        /*
        Whether or not to optimize drawing, it is suggested to open, which can optimize the drawing speed and performance.
        Note: When item is selected for editing after opening, it will be drawn at the top level, and not at the corresponding level until editing is completed.
        是否优化绘制，建议开启，可优化绘制速度和性能.
        注意：开启后item被选中编辑时时会绘制在最上面一层，直到结束编辑后才绘制在相应层级
         */
        mDoodle = mDoodleView = new DoodleViewWrapper(this, bitmap, mDoodleParams.mOptimizeDrawing, new IDoodleListener() {
            @Override
            public void onSaved(IDoodle doodle, Bitmap bitmap, Runnable callback) { // 保存图片为jpg格式
                File doodleFile = null;
                File file = null;
                String savePath = mDoodleParams.mSavePath;
                boolean isDir = mDoodleParams.mSavePathIsDir;
                if (TextUtils.isEmpty(savePath)) {
                    File dcimFile = new File(Environment.getExternalStorageDirectory(), "DCIM");
                    doodleFile = new File(dcimFile, "Doodle");
                    //　保存的路径
                    file = new File(doodleFile, System.currentTimeMillis() + ".jpg");
                } else {
                    if (isDir) {
                        doodleFile = new File(savePath);
                        //　保存的路径
                        file = new File(doodleFile, System.currentTimeMillis() + ".jpg");
                    } else {
                        file = new File(savePath);
                        doodleFile = file.getParentFile();
                    }
                }
                doodleFile.mkdirs();

                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                    ImageUtils.addImage(getContentResolver(), file.getAbsolutePath());
                    Intent intent = new Intent();
                    intent.putExtra(KEY_IMAGE_PATH, file.getAbsolutePath());
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                    onError(DoodleView.ERROR_SAVE, e.getMessage());
                } finally {
                    Util.closeQuietly(outputStream);
                    callback.run();
                }
            }

            public void onError(int i, String msg) {
                setResult(RESULT_ERROR);
                finish();
            }

            @Override
            public void onReady(IDoodle doodle) {

                float size = mDoodleParams.mPaintUnitSize > 0 ? mDoodleParams.mPaintUnitSize * mDoodle.getUnitSize() : 0;
                if (size <= 0) {
                    size = mDoodleParams.mPaintPixelSize > 0 ? mDoodleParams.mPaintPixelSize : mDoodle.getSize();
                }
                // 设置初始值
                mDoodle.setSize(size);
                // 选择画笔
                mDoodle.setPen(DoodlePen.BRUSH);
                mDoodle.setShape(DoodleShape.HAND_WRITE);
                mDoodle.setColor(new DoodleColor(mDoodleParams.mPaintColor));
                if (mDoodleParams.mZoomerScale <= 0) {
                    findViewById(cn.hzw.doodle.R.id.btn_zoomer).setVisibility(View.GONE);
                }
                mDoodle.setZoomerScale(mDoodleParams.mZoomerScale);
                mTouchGestureListener.setSupportScaleItem(mDoodleParams.mSupportScaleItem);

                // 每个画笔的初始值
                mPenSizeMap.put(DoodlePen.BRUSH, mDoodle.getSize());
                mPenSizeMap.put(DoodlePen.MOSAIC, DEFAULT_MOSAIC_SIZE * mDoodle.getUnitSize());
                mPenSizeMap.put(DoodlePen.COPY, DEFAULT_COPY_SIZE * mDoodle.getUnitSize());
                mPenSizeMap.put(DoodlePen.ERASER, mDoodle.getSize());
                mPenSizeMap.put(DoodlePen.TEXT, DEFAULT_TEXT_SIZE * mDoodle.getUnitSize());
                mPenSizeMap.put(DoodlePen.BITMAP, DEFAULT_BITMAP_SIZE * mDoodle.getUnitSize());
            }
        });

        mTouchGestureListener = new DoodleOnTouchGestureListener(mDoodleView, new DoodleOnTouchGestureListener.ISelectionListener() {
            // save states before being selected
            IDoodlePen mLastPen = null;
            IDoodleColor mLastColor = null;
            Float mSize = null;

            IDoodleItemListener mIDoodleItemListener = new IDoodleItemListener() {
                @Override
                public void onPropertyChanged(int property) {
                    if (mTouchGestureListener.getSelectedItem() == null) {
                        return;
                    }
                    if (property == IDoodleItemListener.PROPERTY_SCALE) {
                        mItemScaleTextView.setText(
                                (int) (mTouchGestureListener.getSelectedItem().getScale() * 100 + 0.5f) + "%");
                    }
                }
            };

            @Override
            public void onSelectedItem(IDoodle doodle, IDoodleSelectableItem selectableItem, boolean selected) {
                if (selected) {
                    if (mLastPen == null) {
                        mLastPen = mDoodle.getPen();
                    }
                    if (mLastColor == null) {
                        mLastColor = mDoodle.getColor();
                    }
                    if (mSize == null) {
                        mSize = mDoodle.getSize();
                    }
                    mDoodleView.setEditMode(true);
                    mDoodle.setPen(selectableItem.getPen());
                    mDoodle.setColor(selectableItem.getColor());
                    mDoodle.setSize(selectableItem.getSize());
                    mSelectedEditContainer.setVisibility(View.VISIBLE);
                    mItemScaleTextView.setText((int) (selectableItem.getScale() * 100 + 0.5f) + "%");
                    selectableItem.addItemListener(mIDoodleItemListener);
                } else {
                    selectableItem.removeItemListener(mIDoodleItemListener);

                    if (mTouchGestureListener.getSelectedItem() == null) { // nothing is selected. 当前没有选中任何一个item
                        if (mLastPen != null) {
                            mDoodle.setPen(mLastPen);
                            mLastPen = null;
                        }
                        if (mLastColor != null) {
                            mDoodle.setColor(mLastColor);
                            mLastColor = null;
                        }
                        if (mSize != null) {
                            mDoodle.setSize(mSize);
                            mSize = null;
                        }
                        mSelectedEditContainer.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCreateSelectableItem(IDoodle doodle, float x, float y) {
                if (mDoodle.getPen() == DoodlePen.TEXT) {
                    // if legend draw it
                    if (isLegend) {
                        DoodleLegendSymbolText item = new DoodleLegendSymbolText(mDoodle, currentLegend.symbol, mDoodle.getSize(), mDoodle.getColor().copy(), x, y);
                        item.setLegend(currentLegend);
                        mDoodle.addItem(item);
//                        mTouchGestureListener.setSelectedItem(item);
                        refreshLegends(false);
                        mDoodle.refresh();
                    } else {
                        createDoodleText(null, x, y);
                    }
                } else if (mDoodle.getPen() == DoodlePen.BITMAP) {
                    createDoodleBitmap(null, x, y);
                }
            }
        }) {
            @Override
            public void setSupportScaleItem(boolean supportScaleItem) {
                super.setSupportScaleItem(supportScaleItem);
                if (supportScaleItem) {
                    mItemScaleTextView.setVisibility(View.VISIBLE);
                } else {
                    mItemScaleTextView.setVisibility(View.GONE);
                }
            }
        };

        IDoodleTouchDetector detector = new DoodleTouchDetector(getApplicationContext(), mTouchGestureListener);
        mDoodleView.setDefaultTouchDetector(detector);

        mDoodle.setIsDrawableOutside(mDoodleParams.mIsDrawableOutside);
        mFrameLayout.addView(mDoodleView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mDoodle.setDoodleMinScale(mDoodleParams.mMinScale);
        mDoodle.setDoodleMaxScale(mDoodleParams.mMaxScale);

        initView();
    }

    private void initToolbar() {
        setSupportActionBar(binding.toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_doodle, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        resetState();
        switch (item.getItemId()) {
            case R.id.menu_rotate:
                // 旋转图片
                if (mRotateAnimator == null) {
                    mRotateAnimator = new ValueAnimator();
                    mRotateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            int value = (int) animation.getAnimatedValue();
                            mDoodle.setDoodleRotation(value);
                        }
                    });
                    mRotateAnimator.setDuration(250);
                }
                if (!mRotateAnimator.isRunning()) {
                    mRotateAnimator.setIntValues(mDoodle.getDoodleRotation(), mDoodle.getDoodleRotation() + 90);
                    mRotateAnimator.start();
                }

                break;
            case R.id.menu_hide_tool_bar:
                if (isPanelHide) {
                    showView(mSettingsPanel);
                    isPanelHide = false;
                } else {
                    hideView(mSettingsPanel);
                    isPanelHide = true;
                }
                break;
            case R.id.menu_save:
                mDoodle.save();
                break;
            case R.id.menu_legend:
                LegendDialogFragment legendDialogFragment = new LegendDialogFragment();
                legendDialogFragment.show(getSupportFragmentManager(), "LegendDialogFragment");

                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean canChangeColor(IDoodlePen pen) {
        return pen != DoodlePen.ERASER
                && pen != DoodlePen.BITMAP
                && pen != DoodlePen.COPY
                && pen != DoodlePen.MOSAIC;
    }

    private DoodleLegendText legendText;
    private void refreshLegends(boolean refresh) {
        List<IDoodleItem> items = mDoodle.getAllItem();
        Set<String> legends = new HashSet<>();
        for (IDoodleItem item : items) {
            if (item instanceof DoodleLegendSymbolText) {
                Legend legend = ((DoodleLegendSymbolText) item).getLegend();
                if (legend != null) {
                    legends.add(legend.symbol + ": " + legend.name);
                }

            }
        }

        StringBuilder legendStrs = new StringBuilder();
        for (String legend : legends) {
            legendStrs.append(legend + "\n");
        }

        if (legendText == null) {

            legendText = new DoodleLegendText(
                    mDoodle,
                    legendStrs.toString(),
                    mDoodle.getSize(),
                    mDoodle.getColor().copy(),
                    0,
                    0
            );
            legendText.setLocation(mDoodleView.getWidth() - legendText.getBounds().width(),
                    mDoodleView.getHeight() - legendText.getBounds().height());
            mDoodle.addItem(legendText);
//                        mTouchGestureListener.setSelectedItem(item);

        } else {
            legendText.setText(legendStrs.toString());
            legendText.setLocation(mDoodleView.getWidth() - legendText.getBounds().width(),
                    mDoodleView.getHeight() - legendText.getBounds().height());
            mDoodleView.markItemToOptimizeDrawing(legendText);
            mDoodleView.notifyItemFinishedDrawing(legendText);
        }


    }

    // 添加文字
    private void createDoodleText(final DoodleText doodleText, final float x, final float y) {
        if (isFinishing()) {
            return;
        }

        DialogController.showInputTextDialog(this, doodleText == null ? null : doodleText.getText(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = (v.getTag() + "").trim();
                if (TextUtils.isEmpty(text)) {
                    return;
                }
                if (doodleText == null) {
                    IDoodleSelectableItem item = new DoodleText(mDoodle, text, mDoodle.getSize(), mDoodle.getColor().copy(), x, y);
                    mDoodle.addItem(item);
                    mTouchGestureListener.setSelectedItem(item);
                } else {
                    doodleText.setText(text);
                }
                mDoodle.refresh();
            }
        }, null);
    }

    // 添加贴图
    private void createDoodleBitmap(final DoodleBitmap doodleBitmap, final float x, final float y) {
        DialogController.showSelectImageDialog(this, new ImageSelectorView.ImageSelectorListener() {
            @Override
            public void onCancel() {
            }

            @Override
            public void onEnter(List<String> pathList) {
                Bitmap bitmap = ImageUtils.createBitmapFromPath(pathList.get(0), mDoodleView.getWidth() / 4, mDoodleView.getHeight() / 4);

                if (doodleBitmap == null) {
                    IDoodleSelectableItem item = new DoodleBitmap(mDoodle, bitmap, mDoodle.getSize(), x, y);
                    mDoodle.addItem(item);
                    mTouchGestureListener.setSelectedItem(item);
                } else {
                    doodleBitmap.setBitmap(bitmap);
                }
                mDoodle.refresh();
            }
        });
    }

    //++++++++++++++++++以下为一些初始化操作和点击监听+++++++++++++++++++++++++++++++++++++++++

    //
    private void initView() {
        mBtnUndo = findViewById(cn.hzw.doodle.R.id.btn_undo);
        mBtnUndo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!(DoodleParams.getDialogInterceptor() != null
                        && DoodleParams.getDialogInterceptor().onShow(DoodleActivity.this, mDoodle, DoodleParams.DialogType.CLEAR_ALL))) {
                    DialogController.showEnterCancelDialog(DoodleActivity.this,
                            getString(cn.hzw.doodle.R.string.doodle_clear_screen), getString(cn.hzw.doodle.R.string.doodle_cant_undo_after_clearing),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mDoodle.clear();
                                }
                            }, null
                    );
                }
                return true;
            }
        });
        mSelectedEditContainer = findViewById(cn.hzw.doodle.R.id.doodle_selectable_edit_container);
        mSelectedEditContainer.setVisibility(View.GONE);
        mItemScaleTextView = (TextView) findViewById(cn.hzw.doodle.R.id.item_scale);
        mItemScaleTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mTouchGestureListener.getSelectedItem() != null) {
                    mTouchGestureListener.getSelectedItem().setScale(1);
                }
                return true;
            }
        });

        mSettingsPanel = findViewById(cn.hzw.doodle.R.id.doodle_panel);


        mShapeContainer = findViewById(cn.hzw.doodle.R.id.shape_container);
        mPenContainer = findViewById(cn.hzw.doodle.R.id.pen_container);
        mMosaicMenu = findViewById(cn.hzw.doodle.R.id.mosaic_menu);
        mEditBtn = findViewById(cn.hzw.doodle.R.id.doodle_selectable_edit);
        mRedoBtn = findViewById(cn.hzw.doodle.R.id.btn_redo);

        mBtnColor = DoodleActivity.this.findViewById(cn.hzw.doodle.R.id.btn_set_color);
        mColorContainer = DoodleActivity.this.findViewById(cn.hzw.doodle.R.id.btn_set_color_container);

        mViewShowAnimation = new AlphaAnimation(0, 1);
        mViewShowAnimation.setDuration(150);
        mViewHideAnimation = new AlphaAnimation(1, 0);
        mViewHideAnimation.setDuration(150);
    }

    private ValueAnimator mRotateAnimator;

    private void resetState() {
        binding.polylineGroup.setVisibility(View.GONE);
        isLegend = false;
    }

    public void onClick(final View v) {
        resetState();
        if (v.getId() == cn.hzw.doodle.R.id.btn_pen_hand) {
            mDoodle.setPen(DoodlePen.BRUSH);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_pen_mosaic) {
            mDoodle.setPen(DoodlePen.MOSAIC);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_pen_copy) {
            mDoodle.setPen(DoodlePen.COPY);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_pen_eraser) {
            mDoodle.setPen(DoodlePen.ERASER);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_pen_text) {
            mDoodle.setPen(DoodlePen.TEXT);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_pen_bitmap) {
            mDoodle.setPen(DoodlePen.BITMAP);
        } else if (v.getId() == cn.hzw.doodle.R.id.doodle_btn_brush_edit) {
            mDoodleView.setEditMode(!mDoodleView.isEditMode());
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_undo) {
            mDoodle.undo();
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_zoomer) {
            mDoodleView.enableZoomer(!mDoodleView.isEnableZoomer());
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_set_color_container) {
            showPopup(mBtnColor);
        } else if (v.getId() == cn.hzw.doodle.R.id.doodle_selectable_edit) {
            if (mTouchGestureListener.getSelectedItem() instanceof DoodleText) {
                createDoodleText((DoodleText) mTouchGestureListener.getSelectedItem(), -1, -1);
            } else if (mTouchGestureListener.getSelectedItem() instanceof DoodleBitmap) {
                createDoodleBitmap((DoodleBitmap) mTouchGestureListener.getSelectedItem(), -1, -1);
            }
        } else if (v.getId() == cn.hzw.doodle.R.id.doodle_selectable_remove) {
            mDoodle.removeItem(mTouchGestureListener.getSelectedItem());
            mTouchGestureListener.setSelectedItem(null);
        } else if (v.getId() == cn.hzw.doodle.R.id.doodle_selectable_top) {
            mDoodle.topItem(mTouchGestureListener.getSelectedItem());
        } else if (v.getId() == cn.hzw.doodle.R.id.doodle_selectable_bottom) {
            mDoodle.bottomItem(mTouchGestureListener.getSelectedItem());
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_hand_write) {
            mDoodle.setShape(DoodleShape.HAND_WRITE);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_arrow) {
            mDoodle.setShape(DoodleShape.ARROW);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_line) {
            mDoodle.setShape(DoodleShape.POLYLINE);
            binding.polylineGroup.setVisibility(View.VISIBLE);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_holl_circle) {
            mDoodle.setShape(DoodleShape.HOLLOW_CIRCLE);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_fill_circle) {
            mDoodle.setShape(DoodleShape.TRIANGLE);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_holl_rect) {
            mDoodle.setShape(DoodleShape.HOLLOW_RECT);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_fill_rect) {
            mDoodle.setShape(DoodleShape.PENTAGON);
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_mosaic_level1) {
            if (v.isSelected()) {
                return;
            }

            mMosaicLevel = DoodlePath.MOSAIC_LEVEL_1;
            mDoodle.setColor(DoodlePath.getMosaicColor(mDoodle, mMosaicLevel));
            v.setSelected(true);
            mMosaicMenu.findViewById(cn.hzw.doodle.R.id.btn_mosaic_level2).setSelected(false);
            mMosaicMenu.findViewById(cn.hzw.doodle.R.id.btn_mosaic_level3).setSelected(false);
            if (mTouchGestureListener.getSelectedItem() != null) {
                mTouchGestureListener.getSelectedItem().setColor(mDoodle.getColor().copy());
            }
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_mosaic_level2) {
            if (v.isSelected()) {
                return;
            }

            mMosaicLevel = DoodlePath.MOSAIC_LEVEL_2;
            mDoodle.setColor(DoodlePath.getMosaicColor(mDoodle, mMosaicLevel));
            v.setSelected(true);
            mMosaicMenu.findViewById(cn.hzw.doodle.R.id.btn_mosaic_level1).setSelected(false);
            mMosaicMenu.findViewById(cn.hzw.doodle.R.id.btn_mosaic_level3).setSelected(false);
            if (mTouchGestureListener.getSelectedItem() != null) {
                mTouchGestureListener.getSelectedItem().setColor(mDoodle.getColor().copy());
            }
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_mosaic_level3) {
            if (v.isSelected()) {
                return;
            }

            mMosaicLevel = DoodlePath.MOSAIC_LEVEL_3;
            mDoodle.setColor(DoodlePath.getMosaicColor(mDoodle, mMosaicLevel));
            v.setSelected(true);
            mMosaicMenu.findViewById(cn.hzw.doodle.R.id.btn_mosaic_level1).setSelected(false);
            mMosaicMenu.findViewById(cn.hzw.doodle.R.id.btn_mosaic_level2).setSelected(false);
            if (mTouchGestureListener.getSelectedItem() != null) {
                mTouchGestureListener.getSelectedItem().setColor(mDoodle.getColor().copy());
            }
        } else if (v.getId() == cn.hzw.doodle.R.id.btn_redo) {
            if (!mDoodle.redo(1)) {
                mRedoBtn.setVisibility(View.GONE);
            }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mDoodleView.isEditMode()) {
                mDoodleView.setEditMode(false);
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() { // 返回键监听
        if (mDoodle.getAllItem() == null || mDoodle.getItemCount() == 0) {
            finish();
            return;
        }
        if (!(DoodleParams.getDialogInterceptor() != null
                && DoodleParams.getDialogInterceptor().onShow(DoodleActivity.this, mDoodle, DoodleParams.DialogType.SAVE))) {
            DialogController.showMsgDialog(DoodleActivity.this, getString(cn.hzw.doodle.R.string.doodle_saving_picture), null, getString(cn.hzw.doodle.R.string.doodle_cancel),
                    getString(cn.hzw.doodle.R.string.doodle_save), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mDoodle.save();
                        }
                    }, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
        }
    }

    private void showView(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }

        view.clearAnimation();
        view.startAnimation(mViewShowAnimation);
        view.setVisibility(View.VISIBLE);
    }

    private void hideView(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            return;
        }
        view.clearAnimation();
        view.startAnimation(mViewHideAnimation);
        view.setVisibility(View.GONE);
    }

    // 画笔设置
    // 显示弹出调色板
    private void showPopup(View anchor) {



        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // 创建弹出调色板
        PopupWindow popup = new PopupWindow(this);
        View contentView = getLayoutInflater().inflate(R.layout.popup_paint_setting, null, false);
        final TextView paintSizeText = contentView.findViewById(R.id.paint_size_text);
		// seekbar初始化
		SeekBar mSeekBar = contentView.findViewById(R.id.stroke_seekbar);

		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
										  boolean fromUser) {
				paintSizeText.setText(progress + "");
				if ((int) mDoodle.getSize() == progress) {
					return;
				}
				mDoodle.setSize(progress);
				if (mTouchGestureListener.getSelectedItem() != null) {
					mTouchGestureListener.getSelectedItem().setSize(progress);
				}
			}
		});
		mSeekBar.setProgress((int) mDoodleView.getSize());

		RadioGroup lineTypeGroup = contentView.findViewById(R.id.line_type_group);
		lineTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == R.id.solid_line_btn) {
					mDoodle.setLineType(IDoodle.LineType.SOLID_LINE);
				} else {
					mDoodle.setLineType(IDoodle.LineType.DOTTED_LINE);
				}
			}
		});
		if (mDoodle.getLineType() == IDoodle.LineType.SOLID_LINE) {
			lineTypeGroup.check(R.id.solid_line_btn);
		} else {
			lineTypeGroup.check(R.id.dotted_line_btn);
		}
        popup.setContentView(contentView);
        popup.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(true);
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {

            }
        });

        // 清除默认的半透明背景
        popup.setBackgroundDrawable(new BitmapDrawable());

        popup.showAsDropDown(anchor);
    }

    /**
     * 包裹DoodleView，监听相应的设置接口，以改变UI状态
     */
    private class DoodleViewWrapper extends DoodleView {

        public DoodleViewWrapper(Context context, Bitmap bitmap, boolean optimizeDrawing, IDoodleListener listener) {
            super(context, bitmap, optimizeDrawing, listener);
        }

        private Map<IDoodlePen, Integer> mBtnPenIds = new HashMap<>();

        {
            mBtnPenIds.put(DoodlePen.BRUSH, cn.hzw.doodle.R.id.btn_pen_hand);
            mBtnPenIds.put(DoodlePen.MOSAIC, cn.hzw.doodle.R.id.btn_pen_mosaic);
            mBtnPenIds.put(DoodlePen.COPY, cn.hzw.doodle.R.id.btn_pen_copy);
            mBtnPenIds.put(DoodlePen.ERASER, cn.hzw.doodle.R.id.btn_pen_eraser);
            mBtnPenIds.put(DoodlePen.TEXT, cn.hzw.doodle.R.id.btn_pen_text);
            mBtnPenIds.put(DoodlePen.BITMAP, cn.hzw.doodle.R.id.btn_pen_bitmap);
        }

        @Override
        public void setPen(IDoodlePen pen) {
            IDoodlePen oldPen = getPen();
            super.setPen(pen);

            mMosaicMenu.setVisibility(GONE);
            mEditBtn.setVisibility(View.GONE); // edit btn
            if (pen == DoodlePen.BITMAP || pen == DoodlePen.TEXT) {
                mEditBtn.setVisibility(View.VISIBLE); // edit btn
                mShapeContainer.setVisibility(GONE);
                if (pen == DoodlePen.BITMAP) {
                    mColorContainer.setVisibility(GONE);
                } else {
                    mColorContainer.setVisibility(VISIBLE);
                }
            } else if (pen == DoodlePen.MOSAIC) {
                mMosaicMenu.setVisibility(VISIBLE);
                mShapeContainer.setVisibility(VISIBLE);
                mColorContainer.setVisibility(GONE);
            } else {
                mShapeContainer.setVisibility(VISIBLE);
                if (pen == DoodlePen.COPY || pen == DoodlePen.ERASER) {
                    mColorContainer.setVisibility(GONE);
                } else {
                    mColorContainer.setVisibility(VISIBLE);
                }
            }
            setSingleSelected(mBtnPenIds.values(), mBtnPenIds.get(pen));

            if (mTouchGestureListener.getSelectedItem() == null) {
                mPenSizeMap.put(oldPen, getSize()); // save
                Float size = mPenSizeMap.get(pen); // restore
                if (size != null) {
                    mDoodle.setSize(size);
                }
                if (isEditMode()) {
                    mShapeContainer.setVisibility(GONE);
                    mColorContainer.setVisibility(GONE);
                    mMosaicMenu.setVisibility(GONE);
                }
            } else {
                mShapeContainer.setVisibility(GONE);
                return;
            }

            if (pen == DoodlePen.BRUSH) {

            } else if (pen == DoodlePen.MOSAIC) {
                if (mMosaicLevel <= 0) {
                    mMosaicMenu.findViewById(cn.hzw.doodle.R.id.btn_mosaic_level2).performClick();
                } else {
                    mDoodle.setColor(DoodlePath.getMosaicColor(mDoodle, mMosaicLevel));
                }
            } else if (pen == DoodlePen.COPY) {

            } else if (pen == DoodlePen.ERASER) {

            } else if (pen == DoodlePen.TEXT) {

            } else if (pen == DoodlePen.BITMAP) {

            }
        }

        private Map<IDoodleShape, Integer> mBtnShapeIds = new HashMap<>();

        {
            mBtnShapeIds.put(DoodleShape.HAND_WRITE, cn.hzw.doodle.R.id.btn_hand_write);
            mBtnShapeIds.put(DoodleShape.ARROW, cn.hzw.doodle.R.id.btn_arrow);
            mBtnShapeIds.put(DoodleShape.POLYLINE, cn.hzw.doodle.R.id.btn_line);
            mBtnShapeIds.put(DoodleShape.HOLLOW_CIRCLE, cn.hzw.doodle.R.id.btn_holl_circle);
            mBtnShapeIds.put(DoodleShape.TRIANGLE, cn.hzw.doodle.R.id.btn_fill_circle);
            mBtnShapeIds.put(DoodleShape.HOLLOW_RECT, cn.hzw.doodle.R.id.btn_holl_rect);
            mBtnShapeIds.put(DoodleShape.PENTAGON, cn.hzw.doodle.R.id.btn_fill_rect);

        }

        @Override
        public void setShape(IDoodleShape shape) {
            super.setShape(shape);
            setSingleSelected(mBtnShapeIds.values(), mBtnShapeIds.get(shape));
        }


        @Override
        public void setSize(float paintSize) {
            super.setSize(paintSize);
            if (mTouchGestureListener.getSelectedItem() != null) {
                mTouchGestureListener.getSelectedItem().setSize(getSize());
            }
        }

        @Override
        public void setColor(IDoodleColor color) {
            IDoodlePen pen = getPen();
            super.setColor(color);

            DoodleColor doodleColor = null;
            if (color instanceof DoodleColor) {
                doodleColor = (DoodleColor) color;
            }
            if (doodleColor != null
                    && canChangeColor(pen)) {

                if (mTouchGestureListener.getSelectedItem() != null) {
                    mTouchGestureListener.getSelectedItem().setColor(getColor().copy());
                }
            }

            if (doodleColor != null && pen == DoodlePen.MOSAIC
                    && doodleColor.getLevel() != mMosaicLevel) {
                switch (doodleColor.getLevel()) {
                    case DoodlePath.MOSAIC_LEVEL_1:
                        DoodleActivity.this.findViewById(cn.hzw.doodle.R.id.btn_mosaic_level1).performClick();
                        break;
                    case DoodlePath.MOSAIC_LEVEL_2:
                        DoodleActivity.this.findViewById(cn.hzw.doodle.R.id.btn_mosaic_level2).performClick();
                        break;
                    case DoodlePath.MOSAIC_LEVEL_3:
                        DoodleActivity.this.findViewById(cn.hzw.doodle.R.id.btn_mosaic_level3).performClick();
                        break;
                }
            }
        }

        @Override
        public void enableZoomer(boolean enable) {
            super.enableZoomer(enable);
            DoodleActivity.this.findViewById(cn.hzw.doodle.R.id.btn_zoomer).setSelected(enable);
            if (enable) {
                Toast.makeText(DoodleActivity.this, "x" + mDoodleParams.mZoomerScale, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public boolean undo() {
            mTouchGestureListener.setSelectedItem(null);
            boolean res = super.undo();
            if (getRedoItemCount() > 0) {
                mRedoBtn.setVisibility(VISIBLE);
            } else {
                mRedoBtn.setVisibility(GONE);
            }
            return res;
        }

        @Override
        public void clear() {
            super.clear();
            mTouchGestureListener.setSelectedItem(null);
            mRedoBtn.setVisibility(GONE);
        }

        @Override
        public void addItem(IDoodleItem item) {
            super.addItem(item);
            if (getRedoItemCount() > 0) {
                mRedoBtn.setVisibility(VISIBLE);
            } else {
                mRedoBtn.setVisibility(GONE);
            }
        }

        View mBtnEditMode = DoodleActivity.this.findViewById(cn.hzw.doodle.R.id.doodle_btn_brush_edit);
        Boolean mLastIsDrawableOutside = null;

        @Override
        public void setEditMode(boolean editMode) {
            if (editMode == isEditMode()) {
                return;
            }

            super.setEditMode(editMode);
            mBtnEditMode.setSelected(editMode);
            if (editMode) {
                Toast.makeText(DoodleActivity.this, cn.hzw.doodle.R.string.doodle_edit_mode, Toast.LENGTH_SHORT).show();
                mLastIsDrawableOutside = mDoodle.isDrawableOutside(); // save
                mDoodle.setIsDrawableOutside(true);
                mPenContainer.setVisibility(GONE);
                mShapeContainer.setVisibility(GONE);
                mColorContainer.setVisibility(GONE);
                mBtnUndo.setVisibility(GONE);
                mMosaicMenu.setVisibility(GONE);
            } else {
                if (mLastIsDrawableOutside != null) { // restore
                    mDoodle.setIsDrawableOutside(mLastIsDrawableOutside);
                }
                mTouchGestureListener.center(); // center picture
                if (mTouchGestureListener.getSelectedItem() == null) { // restore
                    setPen(getPen());
                }
                mTouchGestureListener.setSelectedItem(null);
                mPenContainer.setVisibility(VISIBLE);
                mBtnUndo.setVisibility(VISIBLE);
            }
        }

        private void setSingleSelected(Collection<Integer> ids, int selectedId) {
            for (int id : ids) {
                if (id == selectedId) {
                    DoodleActivity.this.findViewById(id).setSelected(true);
                } else {
                    DoodleActivity.this.findViewById(id).setSelected(false);
                }
            }
        }
    }

    public void closeCurrentPolylinePath(View view) {
        mTouchGestureListener.closeCurrentPath();
    }
    public void finishCurrentPolylinePath(View view) {
        mTouchGestureListener.finishCurrentPath();

    }

    private Legend currentLegend;
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetLegend(Legend legend) {
        currentLegend = legend;
        mDoodle.setPen(DoodlePen.TEXT);
        isLegend = true;
        // setPen
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
