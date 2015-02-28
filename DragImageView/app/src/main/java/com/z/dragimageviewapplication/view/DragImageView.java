package com.z.dragimageviewapplication.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;


/**
 * Created by Administrator on 14-5-8.
 */
public class DragImageView extends ImageView {

    private static final String TAG = "mViewPager——Touch";
    private float MAX_SCALE = 3f;
    private float MIN_SCALE = 0.5f;
    private float NORMAL_SCALE=1f;

    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    PointF start = new PointF();

    private int screen_W, screen_H;// 可见屏幕的宽高度

    private int bitmap_W, bitmap_H;// 当前图片宽高

    private boolean isScaleAnim = false;// 缩放动画

    private float beforeLenght, afterLenght;// 两触点距离

    private float scale_temp;// 缩放比例
    private float xCenterPoint;//缩放中心
    private float yCenterPoint;//缩放中心

    private float afterScale;
    private float xAfterCoordinate;
    private float yAfterCoordinate;

    private float beforeScale;//缩放前的比例
    private float xBeforeCoordinate;
    private float yBeforeCoordinate;

    /**
     * 模式 NONE：无 DRAG：拖拽. ZOOM:缩放
     *
     * @author zhangjia
     */
    private enum MODE {
        NONE, DRAG, ZOOM
    };

    private MODE mode = MODE.NONE;// 默认模式


    private float beforeMatrixValues[] = new float[9];
    private float afterMatrixValues[] = new float[9];
    private float saveMatrixValues[] = new float[9];

    /**
     * 构造方法 *
     */
    public DragImageView(Context context) {
        super(context);
    }

//    public void setmActivity(Activity mActivity) {
//        this.mActivity = mActivity;
//    }

    /**
     * 可见屏幕宽度 *
     */
    public void setScreen_W(int screen_W) {
        this.screen_W = screen_W;
        this.xCenterPoint = screen_W / 2;
    }

    /**
     * 可见屏幕高度 *
     */
    public void setScreen_H(int screen_H) {
        this.screen_H = screen_H;
        this.yCenterPoint = screen_H / 2;
    }

    public DragImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 设置显示图片
     */
    public void setImageBitmap(Bitmap bm,float normalScale) {

        super.setImageBitmap(bm);
        /** 获取图片宽高 **/
        bitmap_W = bm.getWidth();
        bitmap_H = bm.getHeight();
        if (screen_W > 0) {
            NORMAL_SCALE =   normalScale;
            MIN_SCALE = NORMAL_SCALE / 2;
            MAX_SCALE = NORMAL_SCALE * 3;
        }
    }


    /**
     * touch 事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        dumpEvent(event);
        boolean isOneselfDeal = true;

        /** 处理单点、多点触摸 **/
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                onPointerDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event);

                if (mode == MODE.DRAG) {

                    doDragBack();

                    xAfterCoordinate = afterMatrixValues[2];
                    yAfterCoordinate = afterMatrixValues[5];
                    afterScale = afterMatrixValues[0];
                    if (//左拖动，且处于超过屏幕左边缘
                            (xAfterCoordinate >= 0 && xAfterCoordinate - xBeforeCoordinate >= 0) ||
                                    //右拖动，且处于超过屏幕右边缘
                                    (bitmap_W * afterScale + xAfterCoordinate <= screen_W && xAfterCoordinate - xBeforeCoordinate < 0)
                            ) {

//                        matrix.getValues(afterMatrixValues);
//                        afterScale = afterMatrixValues[0];
//                        float scale = 1 / afterScale * NORMAL_SCALE;
//                        matrix.postScale(scale, scale, xCenterPoint, yCenterPoint);
//                        matrix.getValues(afterMatrixValues);
//                        doDragBack();

//                        if(afterScale<=NORMAL_SCALE){
                            setImageMatrix(matrix);

                            getParent().requestDisallowInterceptTouchEvent(false);
                            return false;
//                        }

                    } else {
//                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
                /** 执行拖曳还原 **/
//                doDragBack();
                mode = MODE.NONE;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = MODE.NONE;
                /** 执行缩放还原 **/
                if (isScaleAnim) {
                    doScaleAnim();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }

        setImageMatrix(matrix);
        return isOneselfDeal;
    }


    private void dumpEvent(MotionEvent event) {
        String names[] = {"DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
                "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?"};
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);
        if (actionCode == MotionEvent.ACTION_POINTER_DOWN
                || actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid ").append(
                    action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")");
        }
        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++) {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";");
        }
        sb.append("]");
//        Log.d(TAG, sb.toString());
    }

    /**
     * 按下 *
     */
    void onTouchDown(MotionEvent event) {
        mode = MODE.DRAG;

        getParent().requestDisallowInterceptTouchEvent(true);
        matrix.set(getImageMatrix());
        savedMatrix.set(matrix);
        savedMatrix.getValues(saveMatrixValues);
        start.set(event.getX(), event.getY());
    }

    /**
     * 两个手指 只能放大缩小 *
     */
    void onPointerDown(MotionEvent event) {


        beforeLenght = getDistance(event);// 获取两点的距离
        if (event.getPointerCount() == 2 && beforeLenght > 10f) {
            savedMatrix.set(matrix);
            savedMatrix.getValues(saveMatrixValues);
            mode = MODE.ZOOM;
        }
    }

    /**
     * 移动的处理 *
     */
    void onTouchMove(MotionEvent event) {

        matrix.getValues(beforeMatrixValues);
        beforeScale = beforeMatrixValues[0];
        xBeforeCoordinate = beforeMatrixValues[2];
        yBeforeCoordinate = beforeMatrixValues[5];
        /** 处理拖动 **/
        if (mode == MODE.DRAG) {

            /** 在这里要进行判断处理，防止在drag时候越界 **/

//            if(xBeforeCoordinate>=0&&){};

            if (beforeScale * bitmap_W >= screen_W || beforeScale * bitmap_H > screen_H) {//图片宽度超过屏幕宽度才可以移动

                matrix.set(savedMatrix);
                matrix.postTranslate(event.getX() - start.x, event.getY()
                        - start.y);

            }

        }
        /** 处理缩放 **/
        else if (mode == MODE.ZOOM) {

            afterLenght = getDistance(event);// 获取两点的距离

            float gapLenght = afterLenght - beforeLenght;// 变化的长度

            if (Math.abs(gapLenght) > 5f) {

                scale_temp = afterLenght / beforeLenght;// 求的缩放的比例
                this.setScale(scale_temp);

            }
        }
        matrix.getValues(afterMatrixValues);

    }

    /**
     * 获取两点的距离 *
     */
    float getDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);

        return FloatMath.sqrt(x * x + y * y);
    }

    /**
     * 处理缩放 *
     */
    void setScale(float scale) {

        boolean isCanScale = false;
        // 放大
        if (scale > 1f && beforeScale <= MAX_SCALE) {
            isCanScale = true;
//            matrix.postTranslate(dx,dy);
        }
        // 缩小
        else if (scale < 1f && beforeScale >= MIN_SCALE) {
            isCanScale = true;
            isScaleAnim = true;
        }
        if (!isCanScale)
            return;

//        scale=scale*NORMAL_SCALE;
        matrix.set(savedMatrix);

        Log.d(TAG, "savedMatrix:" + matrix.toString());

        matrix.postScale(scale, scale, xCenterPoint, yCenterPoint);

        Log.d(TAG, "matrix:" + matrix.toString() + "scale:" + scale + "NORMAL_SCALE:" + NORMAL_SCALE);
        matrix.getValues(afterMatrixValues);
        doDragBack();

//        Log.d(TAG,"matrix:"+matrix.toString());
    }

    /**
     * 缩小还原大小
     */
    public void doScaleAnim() {
        afterScale = afterMatrixValues[0];
        if (afterScale < NORMAL_SCALE) {
            //放大1/afterScale倍
            float scale = 1 / afterScale * screen_W/bitmap_W;
            matrix.postScale(scale, scale, xCenterPoint, yCenterPoint);
        }
    }

    /**
     * 位置处理，图片超过边缘，则返回边缘，图片大小小于屏幕，则返回中间
     */
    public void doDragBack() {


        afterScale = afterMatrixValues[0];

        xAfterCoordinate = afterMatrixValues[2];//图片左上顶点x坐标
        yAfterCoordinate = afterMatrixValues[5];//图片左上顶点y坐标


        float imgWidth = bitmap_W * afterScale;//图片宽度=图片原始宽度x缩放倍数
        float imgHeight = bitmap_H * afterScale;//图片宽度=图片原始宽度x缩放倍数

        if (mode == MODE.DRAG) {
            //如果图片大于屏幕，，不处理返回
            boolean isCanDrag = imgWidth >= screen_W || imgHeight >= screen_H;
            if (!isCanDrag) {
                return;
            }
        }


        boolean isDragBackHorizontal = false;
        boolean isDragBackVertical = false;


        float xCenterCoordinate = (screen_W - imgWidth) / 2;
        float yCenterCoordinate = (screen_H - imgHeight) / 2;

        float dx = 0;
        float dy = 0;

        /** 水平进行判断 **/
        //是否超过右移超过左边屏幕
        if (xAfterCoordinate > 0) {
            dx = -xAfterCoordinate;
//            dx=screen_W/2-xAfterCoordinate;
            isDragBackHorizontal = true;

        }
        //是否超过左移超过右边屏幕
        else if ((xAfterCoordinate + imgWidth) < screen_W) {
            dx = screen_W - xAfterCoordinate - imgWidth;
            isDragBackHorizontal = true;

        }
        //如果图片高度小于屏幕高度，返回中间
        if (imgWidth < screen_W) {
            dx = xCenterCoordinate - xAfterCoordinate;
            isDragBackHorizontal = true;
        }


        /** 垂直进行判断 **/
        //是否超过下移超过上边屏幕
        if (yAfterCoordinate > 0) {
            dy = -yAfterCoordinate;
            isDragBackVertical = true;

        }
        //是否超过上移超过下边屏幕
        else if ((yAfterCoordinate + imgHeight) < screen_H) {
            dy = screen_H - yAfterCoordinate - imgHeight;
            isDragBackVertical = true;

        }
        //如果图片高度小于屏幕高度，返回中间
        if (imgHeight < screen_H) {
            dy = yCenterCoordinate - yAfterCoordinate;
            isDragBackVertical = true;
        }
//            Log.d(TAG+"isDragBack","dx:"+dx+"screen_W:"+screen_W+"xAfterCoordinate:"+xAfterCoordinate+"bitmap_W:"+bitmap_W);
        if (isDragBackHorizontal || isDragBackVertical)
            matrix.postTranslate(dx, dy);

    }


}
