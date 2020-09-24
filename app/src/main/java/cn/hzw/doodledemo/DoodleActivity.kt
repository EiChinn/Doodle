package cn.hzw.doodledemo

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.text.TextUtils
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cn.forward.androids.utils.ImageUtils
import cn.forward.androids.utils.LogUtil
import cn.forward.androids.utils.StatusBarUtil
import cn.forward.androids.utils.Util
import cn.hzw.doodle.*
import cn.hzw.doodle.core.IDoodle
import cn.hzw.doodle.core.IDoodlePen
import cn.hzw.doodle.core.IDoodleSelectableItem
import cn.hzw.doodle.core.IDoodleTouchDetector
import cn.hzw.doodle.dialog.DialogController
import cn.hzw.doodle.imagepicker.ImageSelectorView.ImageSelectorListener
import cn.hzw.doodledemo.databinding.DoodleLayoutBinding
import cn.hzw.doodledemo.utils.ImgUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * 涂鸦界面，根据DoodleView的接口，提供页面交互
 * （这边代码和ui比较粗糙，主要目的是告诉大家DoodleView的接口具体能实现什么功能，实际需求中的ui和交互需另提别论）
 * Created by huangziwei(154330138@qq.com) on 2016/9/3.
 */
class DoodleActivity : AppCompatActivity(), DoodleContract.View {
    private lateinit var binding: DoodleLayoutBinding
    private var mPresenter: DoodlePresenter? = null
    private lateinit var mDoodle: IDoodle
    private lateinit var mDoodleView: DoodleView
    private var isPanelHide = false
    private var isLegend = false
    private var mViewShowAnimation: AlphaAnimation? = null
    private var mViewHideAnimation: AlphaAnimation? = null // view隐藏和显示时用到的渐变动画
    private var mDoodleParams: DoodleParams? = null
    private var mTouchGestureListener: DoodleOnTouchGestureListener? = null
    private val mPenSizeMap: MutableMap<IDoodlePen, Float> = HashMap() //保存每个画笔对应的最新大小
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_PARAMS, mDoodleParams)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle, persistentState: PersistableBundle) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
        mDoodleParams = savedInstanceState.getParcelable(KEY_PARAMS)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtil.setStatusBarTranslucent(this, true, false)
        EventBus.getDefault().register(this)
        mPresenter = DoodlePresenter(this)
        if (mDoodleParams == null) {
            mDoodleParams = intent.extras.getParcelable(KEY_PARAMS)
        }
        if (mDoodleParams == null) {
            LogUtil.e("TAG", "mDoodleParams is null!")
            finish()
            return
        }
        val imagePath = mDoodleParams!!.mImagePath
        LogUtil.d("TAG", imagePath)
        if (mDoodleParams!!.mIsFullScreen) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        val bitmap = if (imagePath.isEmpty()) {
            ImgUtils.createBitmapFromScratch(this)
        } else {
            ImageUtils.createBitmapFromPath(imagePath, this)
        }
        if (bitmap == null) {
            LogUtil.e("TAG", "bitmap is null!")
            finish()
            return
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DoodleLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
        resetState()

        /*
        Whether or not to optimize drawing, it is suggested to open, which can optimize the drawing speed and performance.
        Note: When item is selected for editing after opening, it will be drawn at the top level, and not at the corresponding level until editing is completed.
        是否优化绘制，建议开启，可优化绘制速度和性能.
        注意：开启后item被选中编辑时时会绘制在最上面一层，直到结束编辑后才绘制在相应层级
         */
        mDoodleView = DoodleView(this, bitmap, mDoodleParams!!.mOptimizeDrawing, object : IDoodleListener {
            override fun onSaved(doodle: IDoodle, bitmap: Bitmap, callback: Runnable) { // 保存图片为jpg格式
                var doodleFile: File? = null
                var file: File? = null
                val savePath = mDoodleParams!!.mSavePath
                val isDir = mDoodleParams!!.mSavePathIsDir
                if (TextUtils.isEmpty(savePath)) {
                    val dcimFile = File(Environment.getExternalStorageDirectory(), "DCIM")
                    doodleFile = File(dcimFile, "Doodle")
                    //　保存的路径
                    file = File(doodleFile, System.currentTimeMillis().toString() + ".jpg")
                } else {
                    if (isDir) {
                        doodleFile = File(savePath)
                        //　保存的路径
                        file = File(doodleFile, System.currentTimeMillis().toString() + ".jpg")
                    } else {
                        file = File(savePath)
                        doodleFile = file.parentFile
                    }
                }
                doodleFile!!.mkdirs()
                var outputStream: FileOutputStream? = null
                try {
                    outputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    ImageUtils.addImage(contentResolver, file.absolutePath)
                    val intent = Intent()
                    intent.putExtra(KEY_IMAGE_PATH, file.absolutePath)
                    setResult(RESULT_OK, intent)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    onError(DoodleView.ERROR_SAVE, e.message)
                } finally {
                    Util.closeQuietly(outputStream)
                    callback.run()
                }
            }

            fun onError(i: Int, msg: String?) {
                setResult(RESULT_ERROR)
                finish()
            }

            override fun onReady(doodle: IDoodle) {
                var size: Float = if (mDoodleParams!!.mPaintUnitSize > 0) {
                    mDoodleParams!!.mPaintUnitSize * mDoodleView.unitSize
                } else { 0.0f }

                if (size <= 0) {
                    size = if (mDoodleParams!!.mPaintPixelSize > 0) mDoodleParams!!.mPaintPixelSize else mDoodleView.size
                }
                // 设置初始值
                mDoodleView.size = size
                // 选择画笔
                setPen(DoodlePen.BRUSH)
                mDoodleView.color = DoodleColor(mDoodleParams!!.mPaintColor)
                mDoodleView.zoomerScale = mDoodleParams!!.mZoomerScale
                mTouchGestureListener!!.isSupportScaleItem = mDoodleParams!!.mSupportScaleItem

                // 每个画笔的初始值
                mPenSizeMap[DoodlePen.BRUSH] = mDoodleView.size
                mPenSizeMap[DoodlePen.ERASER] = mDoodleView.size
                mPenSizeMap[DoodlePen.TEXT] = DEFAULT_TEXT_SIZE * mDoodleView.unitSize
                mPenSizeMap[DoodlePen.BITMAP] = DEFAULT_BITMAP_SIZE * mDoodleView.unitSize
            }

            override fun onAddItem() {
                if (mDoodleView.redoItemCount > 0) {
                    binding.btnRedo.visibility = DoodleView.VISIBLE
                } else {
                    binding.btnRedo.visibility = DoodleView.GONE
                }

            }
        })
        mDoodle = mDoodleView
        mTouchGestureListener = DoodleOnTouchGestureListener(mDoodleView, object : DoodleOnTouchGestureListener.ISelectionListener {

            override fun onSelectedItem(doodle: IDoodle, selectableItem: IDoodleSelectableItem, selected: Boolean) {
                if (selected) {
                    setEditMode(true)
                    binding.doodleSelectableEditContainer.visibility = View.VISIBLE
                } else {
                    if (mTouchGestureListener!!.selectedItem == null) { // nothing is selected. 当前没有选中任何一个item。 点击空白处或者删除了所有图形
                        binding.doodleSelectableEditContainer.visibility = View.GONE
                    }
                }
            }

            override fun onCreateSelectableItem(doodle: IDoodle, x: Float, y: Float) {
                if (mDoodle.pen === DoodlePen.TEXT) {
                    // if legend draw it
                    if (isLegend) {
                        val item = DoodleLegendSymbolText(mDoodle, currentLegend!!.symbol, mDoodleView.size, mDoodleView.color.copy(), x, y)
                        item.legend = currentLegend
                        mDoodle.addItem(item)
                        //                        mTouchGestureListener.setSelectedItem(item);
                        refreshLegendIndex()
                        refreshLegends()
                        mDoodle.refresh()
                    } else {
                        createDoodleText(null, x, y)
                    }
                } else if (mDoodle.pen === DoodlePen.BITMAP) {
                    createDoodleBitmap(null, x, y)
                }
            }
        })
        val detector: IDoodleTouchDetector = DoodleTouchDetector(applicationContext, mTouchGestureListener)
        mDoodleView.defaultTouchDetector = detector
        mDoodleView.setIsDrawableOutside(mDoodleParams!!.mIsDrawableOutside)
        binding.doodleContainer.addView(mDoodleView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        mDoodleView.doodleMinScale = mDoodleParams!!.mMinScale
        mDoodleView.doodleMaxScale = mDoodleParams!!.mMaxScale
        initView()
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_doodle, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        resetState()
        when (item.itemId) {
            R.id.menu_rotate -> {
                // 旋转图片
                if (mRotateAnimator == null) {
                    mRotateAnimator = ValueAnimator()
                    mRotateAnimator!!.addUpdateListener { animation ->
                        val value = animation.animatedValue as Int
                        mDoodle!!.doodleRotation = value
                    }
                    mRotateAnimator!!.duration = 250
                }
                if (!mRotateAnimator!!.isRunning) {
                    mRotateAnimator!!.setIntValues(mDoodle!!.doodleRotation, mDoodle!!.doodleRotation + 90)
                    mRotateAnimator!!.start()
                }
            }
            R.id.menu_hide_tool_bar -> isPanelHide = if (isPanelHide) {
                showView(binding.doodlePanel)
                false
            } else {
                hideView(binding.doodlePanel)
                true
            }
            R.id.menu_save -> mDoodle!!.save()
            R.id.menu_legend -> {
                val legendDialogFragment = LegendDialogFragment()
                legendDialogFragment.show(supportFragmentManager, "LegendDialogFragment")
            }
            R.id.menu_test_point_list -> {
                displayTestPointList()
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun refreshLegendIndex() {
        val sortedLegends = mDoodle.allItem
                .filterIsInstance<DoodleLegendSymbolText>()
                .filter { it.legend.symbol == currentLegend!!.symbol }
                .sortedBy { it.legend.createTime }
                .toMutableList()

        sortedLegends.forEachIndexed { index, doodleLegendSymbolText ->
            if (index == 0) {
                if (doodleLegendSymbolText.text != doodleLegendSymbolText.legend.symbol) {
                    doodleLegendSymbolText.text = doodleLegendSymbolText.legend.symbol
                    mDoodleView.markItemToOptimizeDrawing(doodleLegendSymbolText)
                    mDoodleView.notifyItemFinishedDrawing(doodleLegendSymbolText)
                }

            } else {
                if (doodleLegendSymbolText.text != "${doodleLegendSymbolText.legend.symbol}#$index") {
                    doodleLegendSymbolText.text = "${doodleLegendSymbolText.legend.symbol}#$index"
                    mDoodleView.markItemToOptimizeDrawing(doodleLegendSymbolText)
                    mDoodleView.notifyItemFinishedDrawing(doodleLegendSymbolText)
                }
            }


        }
    }
    private var legendText: DoodleLegendText? = null
    private fun refreshLegends() {
        val legendStrs= mDoodle.allItem
                .filterIsInstance<DoodleLegendSymbolText>()
                .distinctBy { it.legend.symbol }
                .sortedBy { it.legend.symbol }
                .joinToString(separator = "\n") { it.legend.symbol + ": " + it.legend.name}
        val items = mDoodle.allItem
        val legends: MutableSet<String> = HashSet()
        for (item in items) {
            if (item is DoodleLegendSymbolText) {
                val legend = item.legend
                if (legend != null) {
                    legends.add(legend.symbol + ": " + legend.name)
                }
            }
        }
        if (legendText == null) {
            val maxWidth = mDoodleView.toX(600.0f).toInt()
            legendText = DoodleLegendText(
                    mDoodle,
                    legendStrs,
                    mDoodle.size,
                    mDoodle.color.copy(),
                    0.0f,
                    0.0f,
                    maxWidth
            )
            legendText!!.text = legendStrs
            val locationX = mDoodleView.toX(mDoodleView.width - 600.0f)
            // 不管图片怎么缩放，仍然需要同样高度的像素
            val locationY = mDoodleView.toY(mDoodleView.height.toFloat()) - legendText!!.bounds.height().toFloat()
            legendText!!.setLocation(locationX, locationY)
            mDoodle.addItem(legendText)
            //                        mTouchGestureListener.setSelectedItem(item);
        } else {
            legendText!!.text = legendStrs
            val locationX = mDoodleView.toX(mDoodleView.width - 600.0f)
            // 不管图片怎么缩放，仍然需要同样高度的像素
            val locationY = mDoodleView.toY(mDoodleView.height.toFloat()) - legendText!!.bounds.height().toFloat()
            legendText!!.setLocation(locationX, locationY)
            mDoodleView.markItemToOptimizeDrawing(legendText)
            mDoodleView.notifyItemFinishedDrawing(legendText)
        }
    }

    private var testPointListText: DoodleLegendText? = null
    private val testPointListTextMargin = 10.0f
    private fun displayTestPointList() {
        val testPointListStr = mDoodle.allItem
                .filterIsInstance<DoodleLegendSymbolText>()
                .sortedWith(compareBy({ it.legend.symbol }, { it.legend.createTime }))
                .joinToString(separator = "\n") { it.text }
        if (testPointListText == null) {
            val maxWidth = mDoodleView.toX(600.0f).toInt()
            testPointListText = DoodleLegendText(
                    mDoodle,
                    testPointListStr,
                    mDoodle.size,
                    mDoodle.color.copy(),
                    0.0f,
                    0.0f,
                    maxWidth
            )
            testPointListText!!.text = testPointListStr
            val locationX = testPointListTextMargin
            // 不管图片怎么缩放，仍然需要同样高度的像素
            val locationY = mDoodleView.toY(mDoodleView.height.toFloat()) - testPointListText!!.bounds.height().toFloat()
            testPointListText!!.setLocation(locationX, locationY)
            mDoodle.addItem(testPointListText)
            //                        mTouchGestureListener.setSelectedItem(item);
        } else {
            testPointListText!!.text = testPointListStr
            val locationX = testPointListTextMargin
            // 不管图片怎么缩放，仍然需要同样高度的像素
            val locationY = mDoodleView.toY(mDoodleView.height.toFloat()) - testPointListText!!.bounds.height().toFloat()
            testPointListText!!.setLocation(locationX, locationY)
            mDoodleView.markItemToOptimizeDrawing(testPointListText)
            mDoodleView.notifyItemFinishedDrawing(testPointListText)
        }
    }

    // 添加文字
    private fun createDoodleText(doodleText: DoodleText?, x: Float, y: Float) {
        if (isFinishing) {
            return
        }
        DialogController.showInputTextDialog(this, doodleText?.text, View.OnClickListener { v ->
            val text = (v.tag.toString() + "").trim { it <= ' ' }
            if (TextUtils.isEmpty(text)) {
                return@OnClickListener
            }
            if (doodleText == null) {
                val item: IDoodleSelectableItem = DoodleText(mDoodle, text, mDoodle!!.size, mDoodle!!.color.copy(), x, y)
                mDoodle!!.addItem(item)
                mTouchGestureListener!!.selectedItem = item
            } else {
                doodleText.text = text
            }
            mDoodle!!.refresh()
        }, null)
    }

    private var assetImgList: List<String>? = null
    // 添加贴图
    private fun createDoodleBitmap(doodleBitmap: DoodleBitmap?, x: Float, y: Float) {
        if (assetImgList == null) {
            assetImgList = assets.list("")?.filter { it.endsWith(".jpg") || it.endsWith(".png")}?.map { "assets/$it" }
        }
        DialogController.showSelectImageDialog(this, assetImgList, object : ImageSelectorListener {
            override fun onCancel() {}
            override fun onEnter(pathList: List<String>) {
                val bitmap = ImgUtils.createBitmapFromPath(
                        pathList[0],
                        mDoodleView.width / 4,
                        mDoodleView.height / 4,
                        this@DoodleActivity
                )
                if (doodleBitmap == null) {
                    val item: IDoodleSelectableItem = DoodleBitmap(mDoodle, bitmap, mDoodle!!.size, x, y)
                    mDoodle!!.addItem(item)
                    mTouchGestureListener!!.selectedItem = item
                } else {
                    doodleBitmap.bitmap = bitmap
                }
                mDoodle!!.refresh()
            }
        })
    }

    //++++++++++++++++++以下为一些初始化操作和点击监听+++++++++++++++++++++++++++++++++++++++++
    //
    private fun initView() {
        binding.doodleSelectableEditContainer.visibility = View.GONE
        mViewShowAnimation = AlphaAnimation(0.0f, 1.0f)
        mViewShowAnimation!!.duration = 150
        mViewHideAnimation = AlphaAnimation(1.0f, 0.0f)
        mViewHideAnimation!!.duration = 150
    }

    private var mRotateAnimator: ValueAnimator? = null
    private fun resetState() {
        binding.polylineGroup.visibility = View.GONE
        isLegend = false
    }

    fun onClick(v: View) {
        resetState()
        if (v.id == R.id.btn_pen_hand) {
            setPen(DoodlePen.BRUSH)
        } else if (v.id == R.id.btn_pen_eraser) {
            setPen(DoodlePen.ERASER)
        } else if (v.id == R.id.btn_pen_text) {
            setPen(DoodlePen.TEXT)
        } else if (v.id == R.id.btn_pen_bitmap) {
            setPen(DoodlePen.BITMAP)
        } else if (v.id == R.id.doodle_btn_brush_edit) {
            setEditMode(!mDoodleView.isEditMode)
        } else if (v.id == R.id.btn_undo) {
            mTouchGestureListener!!.selectedItem = null
            mDoodle.undo()
            if (mDoodle.redoItemCount > 0) {
                binding.btnRedo.visibility = DoodleView.VISIBLE
            } else {
                binding.btnRedo.visibility = DoodleView.GONE
            }
        } else if (v.id == R.id.btn_set_color_container) {
            val penSettingDialogFragment = PenSettingDialogFragment.newInstance(mDoodleView.size, mDoodleView.lineType.ordinal)
            penSettingDialogFragment.show(supportFragmentManager, "PenSettingDialogFragment")
        } else if (v.id == R.id.doodle_selectable_edit) {
            if (mTouchGestureListener!!.selectedItem is DoodleText) {
                createDoodleText(mTouchGestureListener!!.selectedItem as DoodleText, -1f, -1f)
            } else {
                Toast.makeText(this, "当前图形不支持编辑内容", Toast.LENGTH_SHORT).show()
            }
        } else if (v.id == R.id.doodle_selectable_remove) {
            val needRefreshLegendIndex = mTouchGestureListener!!.selectedItem is DoodleLegendSymbolText
            mDoodle.removeItem(mTouchGestureListener!!.selectedItem)
            mTouchGestureListener!!.selectedItem = null
            if (needRefreshLegendIndex) {
                refreshLegendIndex()
            }
        } else if (v.id == R.id.btn_hand_write) {
            setShape(DoodleShape.HAND_WRITE)
        } else if (v.id == R.id.btn_arrow) {
            setShape(DoodleShape.ARROW)
        } else if (v.id == R.id.btn_line) {
            setShape(DoodleShape.LINE)
        } else if (v.id == R.id.btn_holl_circle) {
            setShape(DoodleShape.HOLLOW_CIRCLE)
        } else if (v.id == R.id.btn_oval) {
            setShape(DoodleShape.OVAL)
        } else if (v.id == R.id.btn_holl_rect) {
            setShape(DoodleShape.HOLLOW_RECT)
        } else if (v.id == R.id.btn_polyline) {
            setShape(DoodleShape.POLYLINE)
        } else if (v.id == R.id.btn_polygon) {
            val selectPolygonDialogFragment = SelectPolygonDialogFragment.newInstance()
            selectPolygonDialogFragment.show(supportFragmentManager, "SelectPolygonDialogFragment")
        } else if (v.id == R.id.btn_redo) {
            if (!mDoodleView.redo(1)) {
                binding.btnRedo.visibility = View.GONE
            }
        } else if (v.id == R.id.btn_clean) {
            AlertDialog.Builder(this)
                    .setTitle(getString(R.string.doodle_clear_screen))
                    .setMessage(getString(R.string.doodle_cant_undo_after_clearing))
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定") { _, _ ->
                        mDoodle.clear()
                        mTouchGestureListener!!.selectedItem = null
                        binding.btnRedo.visibility = DoodleView.GONE
                    }
                    .create().show()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun selectPolygon(selectedPolygonEvent: SelectPolygonEvent) {
        setShape(selectedPolygonEvent.selectedPolygon)
    }

    override fun onBackPressed() { // 返回键监听
        if (mDoodleView.isEditMode) {
            setEditMode(false)
            return
        }
        if (mDoodle.allItem == null || mDoodle.itemCount == 0) {
            finish()
            return
        }
        if (!(DoodleParams.getDialogInterceptor() != null
                        && DoodleParams.getDialogInterceptor().onShow(this@DoodleActivity, mDoodle, DoodleParams.DialogType.SAVE))) {
            DialogController.showMsgDialog(this@DoodleActivity, getString(R.string.doodle_saving_picture), null, getString(R.string.doodle_cancel),
                    getString(R.string.doodle_save), { mDoodle!!.save() }) { finish() }
        }
    }

    private fun showView(view: View?) {
        if (view!!.visibility == View.VISIBLE) {
            return
        }
        view.clearAnimation()
        view.startAnimation(mViewShowAnimation)
        view.visibility = View.VISIBLE
    }

    private fun hideView(view: View?) {
        if (view!!.visibility != View.VISIBLE) {
            return
        }
        view.clearAnimation()
        view.startAnimation(mViewHideAnimation)
        view.visibility = View.GONE
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun changePenAttrs(newAttrs: ChangePenAttrEvent) {
        mDoodleView.size = newAttrs.paintSize
        mDoodleView.lineType = newAttrs.paintLineType
    }

    private fun setPen(newPen: DoodlePen) {
        val oldPen = mDoodleView.pen
        mPenSizeMap[oldPen] = mDoodleView.size // save current pen size
        mDoodleView.pen = newPen
        mPenSizeMap[newPen]?.let {
            mDoodleView.size = it // restore new pen size in history
        }
        resetPen()
        when (newPen) {
            DoodlePen.BRUSH -> setPenBrush()
            DoodlePen.ERASER -> setPenEraser()
            DoodlePen.TEXT -> setPenText()
            DoodlePen.BITMAP -> setPenBitmap()
        }
    }

    private fun resetPen() {
        binding.btnPenHand.isSelected = false
        binding.btnPenEraser.isSelected = false
        binding.btnPenText.isSelected = false
        binding.btnPenBitmap.isSelected = false
        binding.shapeContainer.visibility = View.GONE
    }

    private fun setPenBrush() {
        binding.btnPenHand.isSelected = true
        binding.shapeContainer.visibility = View.VISIBLE
        setShape(DoodleShape.HAND_WRITE)
    }
    private fun setPenEraser() {
        binding.btnPenEraser.isSelected = true
        setShape(DoodleShape.HAND_WRITE)
    }
    private fun setPenText() {
        binding.btnPenText.isSelected = true
    }
    private fun setPenBitmap() {
        binding.btnPenBitmap.isSelected = true
    }

    private fun setShape(shape: DoodleShape) {
        check(mDoodle.pen == DoodlePen.BRUSH || mDoodle.pen == DoodlePen.ERASER) {
            "only DoodlePen.BRUSH or DoodlePen.ERASER can set shape"
        }
        resetShape()
        when (shape) {
            DoodleShape.HAND_WRITE -> setShapeHandWrite()
            DoodleShape.ARROW -> setShapeArrow()
            DoodleShape.LINE -> setShapeLine()
            DoodleShape.POLYLINE -> setShapePolyLine()
            DoodleShape.HOLLOW_CIRCLE -> setShapeCircle()
            DoodleShape.OVAL -> setShapeOval()
            DoodleShape.HOLLOW_RECT -> setShapeRectangle()
            DoodleShape.TRIANGLE -> setShapePolygon()
        }
        mDoodle.shape = shape
    }

    private fun resetShape() {
        binding.btnHandWrite.isSelected = false
        binding.btnArrow.isSelected = false
        binding.btnLine.isSelected = false
        binding.btnPolyline.isSelected = false
        binding.btnHollCircle.isSelected = false
        binding.btnOval.isSelected = false
        binding.btnHollRect.isSelected = false
        binding.btnPolygon.isSelected = false
        binding.polylineGroup.visibility = View.GONE

    }
    private fun setShapeHandWrite() {
        binding.btnHandWrite.isSelected = true
    }
    private fun setShapeArrow() {
        binding.btnArrow.isSelected = true
    }
    private fun setShapeLine() {
        binding.btnLine.isSelected = true
    }
    private fun setShapePolyLine() {
        binding.btnPolyline.isSelected = true
        binding.polylineGroup.visibility = View.VISIBLE
    }
    private fun setShapeCircle() {
        binding.btnHollCircle.isSelected = true
    }
    private fun setShapeOval() {
        binding.btnOval.isSelected = true
    }
    private fun setShapeRectangle() {
        binding.btnHollRect.isSelected = true
    }
    private fun setShapePolygon() {
        binding.btnPolygon.isSelected = true
    }

    private var mLastIsDrawableOutside: Boolean? = null
    private fun setEditMode(editMode: Boolean) {
        if (editMode == mDoodleView.isEditMode) {
            return
        }
        mDoodleView.isEditMode = editMode
        binding.doodleBtnBrushEdit.isSelected = editMode
        if (editMode) {
            Toast.makeText(this@DoodleActivity, R.string.doodle_edit_mode, Toast.LENGTH_SHORT).show()
            mLastIsDrawableOutside = mDoodle.isDrawableOutside // save
            mDoodle.setIsDrawableOutside(true)
            binding.llPenShape.visibility = View.GONE
            binding.btnSetColorContainer.visibility = View.GONE
            binding.btnUndo.visibility = View.GONE
        } else {
            if (mLastIsDrawableOutside != null) { // restore
                mDoodle.setIsDrawableOutside(mLastIsDrawableOutside!!)
            }
            mTouchGestureListener!!.center() // center picture
            /*if (mTouchGestureListener!!.selectedItem == null) { // restore
                pen = pen
            }*/
            mTouchGestureListener!!.selectedItem = null
            binding.llPenShape.visibility = View.VISIBLE
            binding.btnUndo.visibility = DoodleView.VISIBLE
            binding.btnSetColorContainer.visibility = View.VISIBLE
        }
    }

    fun closeCurrentPolylinePath(view: View?) {
        mTouchGestureListener!!.closeCurrentPath()
    }

    fun finishCurrentPolylinePath(view: View?) {
        mTouchGestureListener!!.finishCurrentPath()
    }

    private var currentLegend: Legend? = null
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSetLegend(legend: Legend?) {
        currentLegend = legend
        setPen(DoodlePen.TEXT)
        isLegend = true
        // setPen
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    companion object {
        const val TAG = "Doodle"
        const val DEFAULT_TEXT_SIZE = 18 // 默认文字大小
        const val DEFAULT_BITMAP_SIZE = 80 // 默认贴图大小
        const val RESULT_ERROR = -111 // 出现错误

        /**
         * 启动涂鸦界面
         *
         * @param activity
         * @param params      涂鸦参数
         * @param requestCode startActivityForResult的请求码
         * @see DoodleParams
         */
        @JvmStatic
        fun startActivityForResult(activity: Activity, params: DoodleParams?, requestCode: Int) {
            val intent = Intent(activity, DoodleActivity::class.java)
            intent.putExtra(KEY_PARAMS, params)
            activity.startActivityForResult(intent, requestCode)
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
        @Deprecated("")
        fun startActivityForResult(activity: Activity, imagePath: String?, savePath: String?, isDir: Boolean, requestCode: Int) {
            val params = DoodleParams()
            params.mImagePath = imagePath
            params.mSavePath = savePath
            params.mSavePathIsDir = isDir
            startActivityForResult(activity, params, requestCode)
        }

        /**
         * [DoodleActivity.startActivityForResult]
         */
        @Deprecated("")
        fun startActivityForResult(activity: Activity, imagePath: String?, requestCode: Int) {
            val params = DoodleParams()
            params.mImagePath = imagePath
            startActivityForResult(activity, params, requestCode)
        }

        const val KEY_PARAMS = "key_doodle_params"
        const val KEY_IMAGE_PATH = "key_image_path"
    }
}