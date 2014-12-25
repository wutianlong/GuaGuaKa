package com.example.guaguaka.view;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.imooc.guaguaka.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class GuaGuaKa extends View {
	private static final String TAG = "GuaGuaKa";

	private ExecutorService mExecutor = null;
	private Paint mOutterPaint;
	private Path mPath;
	private Canvas mCanvas;
	private Bitmap mBitmap;

	private int mLastX;
	private int mLastY;

	private Bitmap mOutterBitmap;

	private String mText;
	private Paint mBackPaint;

	private Rect mTextBound;
	private int mTextSize;
	private int mTextColor;

	private volatile boolean mComplete = false;

	public interface OnGuaGuaKaCompleteListener {
		void complete();
	}

	private OnGuaGuaKaCompleteListener mListener;

	public void setOnGuaGuaKaCompleteListener(
			OnGuaGuaKaCompleteListener mListener) {
		this.mListener = mListener;
	}

	public GuaGuaKa(Context context) {
		this(context, null);
	}

	public GuaGuaKa(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public GuaGuaKa(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
		TypedArray a = null;
		try {
			a = context.getTheme().obtainStyledAttributes(attrs,
					R.styleable.GuaGuaKa, defStyle, 0);

			int n = a.getIndexCount();

			for (int i = 0; i < n; i++) {
				int attr = a.getIndex(i);

				switch (attr) {
				case R.styleable.GuaGuaKa_text:
					mText = a.getString(attr);
					break;

				case R.styleable.GuaGuaKa_textSize:
					mTextSize = (int) a.getDimension(attr, TypedValue
							.applyDimension(TypedValue.COMPLEX_UNIT_SP, 22,
									getResources().getDisplayMetrics()));
					break;
				case R.styleable.GuaGuaKa_textColor:
					mTextColor = a.getColor(attr, 0x000000);
					break;
				}

			}

		} finally {
			if (a != null)
				a.recycle();
		}

	}

	public void setText(String mText) {
		this.mText = mText;
		mBackPaint.getTextBounds(mText, 0, mText.length(), mTextBound);
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int width = getMeasuredWidth();
		int height = getMeasuredHeight();

		Log.d(TAG, "----onMeasure" + "  width=" + width + "   height=" + height);
		// 初始化Bitmap
		mBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		// Canvas绘图绘制在Bitmap上面
		mCanvas = new Canvas(mBitmap);

		setupOutPaint();
		setUpBackPaint();

		mCanvas.drawRoundRect(new RectF(0, 0, width, height), 30, 30,
				mOutterPaint);
		mCanvas.drawBitmap(mOutterBitmap, null, new Rect(0, 0, width, height),
				null);

	}

	private void setUpBackPaint() {
		mBackPaint.setColor(mTextColor);
		mBackPaint.setStyle(Style.FILL);
		mBackPaint.setTextSize(mTextSize);
		mBackPaint.getTextBounds(mText, 0, mText.length(), mTextBound);

	}

	/**
	 * 绘制Path画笔一些属性
	 */
	private void setupOutPaint() {
		mOutterPaint.setColor(Color.parseColor("#c0c0c0"));
		mOutterPaint.setAntiAlias(true);
		mOutterPaint.setDither(true);
		// 画笔为圆角
		mOutterPaint.setStrokeJoin(Paint.Join.ROUND);
		mOutterPaint.setStrokeCap(Paint.Cap.ROUND);
		mOutterPaint.setStyle(Style.FILL);
		mOutterPaint.setStrokeWidth(20);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();

		int x = (int) event.getX();
		int y = (int) event.getY();

		switch (action) {
		case MotionEvent.ACTION_DOWN:

			mLastX = x;
			mLastY = y;
			mPath.moveTo(mLastX, mLastY);
			break;
		case MotionEvent.ACTION_MOVE:

			int dx = Math.abs(x - mLastX);
			int dy = Math.abs(y - mLastY);

			// 只有款或高移动超过三像素后才开始画path,防止一直画，浪费内存
			if (dx > 3 || dy > 3) {
				mPath.lineTo(x, y);
			}

			mLastX = x;
			mLastY = y;

			break;
		case MotionEvent.ACTION_UP:
			if (!mComplete)
				mExecutor.execute(mRunnable);
			break;
		}
		if (!mComplete)
			invalidate();
		return true;

	}

	private Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			Log.d(TAG, "````````thread id=" + Thread.currentThread().getId());
			int w = getWidth();
			int h = getHeight();

			float wipeArea = 0;
			float totalArea = w * h;
			Bitmap bitmap = mBitmap;
			int[] mPixels = new int[w * h];

			bitmap.getPixels(mPixels, 0, w, 0, 0, w, h);

			for (int i = 0; i < w; i++) {
				for (int j = 0; j < h; j++) {
					int index = i + j * w;
					if (mPixels[index] == 0) {
						wipeArea++;
					}
				}
			}

			if (wipeArea > 0 && totalArea > 0) {
				int percent = (int) (wipeArea * 100 / totalArea);

				Log.e("TAG", percent + "");

				// 滑动比例超过60%，则全部消失
				if (percent > 60) {
					mComplete = true;
					postInvalidate();
				}
			}
		}
	};

	@Override
	protected void onDraw(Canvas canvas) {
		Log.d(TAG, "======onDraw" + mComplete);
		canvas.drawText(mText, getWidth() / 2 - mTextBound.width() / 2,
				getHeight() / 2 + mTextBound.height() / 2, mBackPaint);

		if (!mComplete) {
			drawPath();
			canvas.drawBitmap(mBitmap, 0, 0, null);
		}

		if (mComplete) {
			if (mListener != null) {
				mListener.complete();
			}
		}
	}

	// 画Path到mBitmap上面
	private void drawPath() {
		mOutterPaint.setStyle(Style.STROKE);
		mOutterPaint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
		mCanvas.drawPath(mPath, mOutterPaint);
	}

	private void init() {
		// 复用单线程
		mExecutor = Executors.newSingleThreadExecutor();
		mOutterPaint = new Paint();
		mPath = new Path();
		mOutterBitmap = BitmapFactory.decodeResource(getResources(),
				com.imooc.guaguaka.R.drawable.fg_guaguaka);
		mText = "5000万RMB";
		mTextBound = new Rect();
		mBackPaint = new Paint();
		mTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
				22, getResources().getDisplayMetrics());
	}
}
