## AndroidDragImageView
重写android里的ImageView控件，能够缩放拖曳图片，支持ViewPager
###使用
DragImageView继承ImageView类，直接在layout文件中引用，但要设置scaleType属性为"matrix"，因为是使用Matrix实现图片的缩放和平移。<br>
```xml
    <com.z.dragimageviewapplication.view.DragImageView
        android:layout_centerInParent="true"
        android:id="@+id/iv_big_img"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone"
        android:scaleType="matrix" />
```
###关键代码
在Android图形API中提供了一个Matrix矩形类，该类具有一个3×3的矩阵坐标。通过该类可以实现图形的旋转、平移和缩放<br>

这里用到的Matrix方法：<br>
void set(Matrix src) 	复制一个源矩阵，与构造方法Matrix(Matrix src)一样<br>
void postScale(float sx, float sy, float px, float py) 	以坐标（px,py）进行缩放<br>
void postTranslate(float dx, float dy) 	平移<br>
getValues(float[] values)  复制matrix里的矩阵值到一个长度为9的浮点数组里<br>

重写imageview的onTouchMove方法，根据MotionEvent的不同action来设置Matrix的缩放移动数据，最后再调用imageview的setImageMatrix(matrix)使图片变化

```JAVA
@Override
    public boolean onTouchEvent(MotionEvent event) {

        /** 处理单点、多点触摸 **/
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event);//这里处理单指按下时的事件
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                onPointerDown(event);//这里处理多指按下时的事件
                break;
            case MotionEvent.ACTION_MOVE:
                boolean isNeedIntercept=onTouchMove(event);
                //是否需要父类组件拦截处理，目的是为了支持viewpager
                if (isNeedIntercept) {
//                    返回false，让父类控件处理
                    return false;
                }

                break;
            case MotionEvent.ACTION_UP:
                mode = MODE.NONE;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = MODE.NONE;
                /** 执行缩放还原 **/
                if (isScaleRestore) {
                    doScaleAnim();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }

        setImageMatrix(matrix);
        return true;
    }
```
####图片的移动
```JAVA
    void onTouchDown(MotionEvent event) {
        mode = MODE.DRAG;

        getParent().requestDisallowInterceptTouchEvent(true);
        matrix.set(getImageMatrix());
        savedMatrix.set(matrix);
        savedMatrix.getValues(saveMatrixValues);//保存移动前的数据到saveMatrixValues数组
        start.set(event.getX(), event.getY());
    }
        /**
     * 移动的处理 *
     */
    boolean onTouchMove(MotionEvent event) {

        matrix.getValues(beforeMatrixValues);
        beforeScale = beforeMatrixValues[0];//图片左上顶点x坐标
        xBeforeCoordinate = beforeMatrixValues[2];

        /** 处理拖动 **/
        if (mode == MODE.DRAG) {

//            在这里要进行判断处理，防止在drag时候越界
            //图片宽度超过屏幕宽度可以移动
            boolean isWidthBeyond=beforeScale * bitmap_W >= screen_W;
            //图片高度超过屏幕高度可以移动
            boolean isHeightBeyond=beforeScale * bitmap_H > screen_H;

            if (isWidthBeyond||isHeightBeyond) {

                float dx=event.getX() - start.x;
                float dy=event.getY()- start.y;

                matrix.set(savedMatrix);//还原拖动前的值，这里的移动值是相对值，不是绝对坐标值
                matrix.postTranslate(dx,dy);

            }
            getAfterMatrixValues();

            doDragBack();

            //左拖动，且处于超过屏幕左边缘
            boolean isLeftBeyond=(xAfterCoordinate >= 0 && xAfterCoordinate - xBeforeCoordinate >= 0);
            //右拖动，且处于超过屏幕右边缘
            boolean isRightBeyond=(bitmap_W * afterScale + xAfterCoordinate <= screen_W && xAfterCoordinate - xBeforeCoordinate < 0);

            if (isLeftBeyond ||isRightBeyond) {

                setImageMatrix(matrix);
                //调用父类控件进行touchEvent拦截，让父类控件处理该事件
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            }

        }
        return false;
    }
```
####图片的缩放
```JAVA
    /**
     * 两个手指操作，缩放模式
     */
    void onPointerDown(MotionEvent event) {


        beforeDistance = getDistance(event);// 获取两点的距离
        //两只手指，且指间隙大于10f
        if (event.getPointerCount() == 2 && beforeDistance > 10f) {
            savedMatrix.set(matrix);
            savedMatrix.getValues(saveMatrixValues);//保存移动前的数据到saveMatrixValues数组
            mode = MODE.ZOOM;
        }
    }
     /**
     * 移动的处理 *
     */
    boolean onTouchMove(MotionEvent event) {

        matrix.getValues(beforeMatrixValues);
        beforeScale = beforeMatrixValues[0];//图片左上顶点x坐标
        xBeforeCoordinate = beforeMatrixValues[2];

          if (mode == MODE.ZOOM) {
            /** 处理缩放 **/

            afterDistance = getDistance(event);// 获取两点的距离

            float gapLenght = afterDistance - beforeDistance;// 变化的长度

            if (Math.abs(gapLenght) > 5f) {

                scale_temp = afterDistance / beforeDistance;// 求的缩放的比例
                this.setScale(scale_temp);

            }
            matrix.getValues(afterMatrixValues);
        }
        return false;
    }
     /**
     * 处理缩放 *
     */
    void setScale(float scale) {

        boolean isCanScale = false;
        if (scale > NORMAL_SCALE && beforeScale <= MAX_SCALE) {
            // 放大
            isCanScale = true;
        }else if (scale < NORMAL_SCALE && beforeScale >= MIN_SCALE) {
            // 缩小
            isCanScale = true;
            isScaleRestore = true;
        }
        if (!isCanScale)
            return;

        matrix.set(savedMatrix);//还原放大前的值，这里的放大倍数是绝对值，不是相对值
        matrix.postScale(scale, scale, xCenterPoint, yCenterPoint);
        getAfterMatrixValues();
        doDragBack();

    }
```
