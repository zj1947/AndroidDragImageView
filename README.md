## AndroidDragImageView
重写android里的ImageView控件，支持缩放拖曳图片，支持ViewPager
###使用
DragImageView继承ImageView类，直接在layout文件中引用，但要设置scaleType属性为"matrix"，使用Matrix实现图片的缩放和平移。
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

重写imageview的onTouchMove方法，根据MotionEvent的不同action来设置Matrix的缩放移动数据，最后再调用imageview的setImageMatrix(matrix)使图片发生变化
#####关于Matrix
在Android图形API中提供了一个Matrix矩形类，该类具有一个3×3的矩阵坐标。通过该类可以实现图形的旋转、平移和缩放<br>

这里用到的Matrix方法：<br>
void set(Matrix src) 	复制一个源矩阵，与构造方法Matrix(Matrix src)一样<br>
void postScale(float sx, float sy, float px, float py) 	以坐标（px,py）进行缩放<br>
void postTranslate(float dx, float dy) 	平移<br>
getValues(float[] values)  复制matrix里的矩阵值到一个长度为9的浮点数组里<br>


```JAVA
@Override
    public boolean onTouchEvent(MotionEvent event) {

        /** 处理单点、多点触摸 **/
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event);//处理单指按下时的事件，设置为移动模式
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                onPointerDown(event);//处理多指按下时的事件，设置为缩放模式
                break;
            case MotionEvent.ACTION_MOVE:
                //处理手指移动时的事件
               onTouchMove(event);
                if (isNeedIntercept) {
//                    返回false，让父类控件处理
                    isNeedIntercept=false;
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
                    isScaleRestore=false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        //执行图片变化
        setImageMatrix(matrix);
        return true;
    }
```
####图片的移动
当手指触摸屏幕时，用PointF start保存按下的点坐标及用savedMatrix保存矩阵值；当手指移动时用MotionEvent中的来取得移动点的坐标，以此计算移动距离。通过mmatrix.postTranslate(dx,dy)更改移动矩阵值，最后调用imageview的setImageMatrix(matrix)实现图片平移
```JAVA
        PointF start = new PointF();//记录单指触摸屏幕点
        Matrix savedMatrix = new Matrix();//保存手指按下时的Matrix
        
        //在onTouchDown(event)事件中，保存手指触摸屏幕时的坐标点及矩阵值
          matrix.set(getImageMatrix());
        savedMatrix.set(matrix);
        savedMatrix.getValues(saveMatrixValues);//保存移动前的数据到saveMatrixValues数组
        start.set(event.getX(), event.getY());
        
        //在onTouchMove(event)事件中，计算移动距离，还原触摸时时的矩阵值，并设置其移动偏移量
        float dx=event.getX() - start.x;//计算x轴的偏移
        float dy=event.getY()- start.y;

        matrix.set(savedMatrix);//还原拖动前的值，这里的移动值是相对值，不是绝对坐标值
        matrix.postTranslate(dx,dy);
        //最后
         setImageMatrix(matrix);//使matrix生效
```
具体代码，主要针对图片的越界处理
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
    void onTouchMove(MotionEvent event) {

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
                isNeedIntercept=true;

            }

        }
    }
```
####图片的缩放
当两只手指触摸屏幕且手指间隙大于10f时，设置为缩放模式，用savedMatrix保存矩阵值；为防止抖动，当两只手指移动变化长度大于5f时，才应用缩放。放大倍数为，两指之间的即时距离与刚触摸屏幕是的距离之比。通过postScale(float sx, float sy, float px, float py)更改缩放矩阵值，最后调用imageview的setImageMatrix(matrix)实现图片平移。
```JAVA
    float beforeDistance;// 保存手指触摸屏幕时，两触点距离
    Matrix savedMatrix = new Matrix();//保存手指按下时的Matrix
    
    //在onPointerDown事件中，当两只手指触摸屏幕且手指间隙大于10f时，设置为缩放模式，用savedMatrix保存矩阵值
    
        beforeDistance = getDistance(event);// 获取两点的距离
        //两只手指，且指间隙大于10f
        if (event.getPointerCount() == 2 && beforeDistance > 10f) {
            savedMatrix.set(matrix);
            savedMatrix.getValues(saveMatrixValues);//保存移动前的数据到saveMatrixValues数组
            mode = MODE.ZOOM;
        }
        
    //在onTouchMove(event)事件中，为防止抖动，当两只手指移动变化长度大于5f时，才应用缩放。<br>                       //放大倍数为，两指之间的即时距离与刚触摸屏幕是的距离之比。<br> 
    //通过postScale(float sx, float sy, float px, float py)更改缩放矩阵值
            
            afterDistance = getDistance(event);// 获取两点的距离
            float gapLenght = afterDistance - beforeDistance;// 变化的长度
            
            //为防止抖动，当两只手指移动变化长度大于5f时，才应用缩放
            if (Math.abs(gapLenght) > 5f) {
                scale_temp = afterDistance / beforeDistance;// 求的缩放的比例
                 matrix.set(savedMatrix);//还原放大前的值，这里的放大倍数是绝对值，不是相对值
                 matrix.postScale(scale, scale, xCenterPoint, yCenterPoint);
                 getAfterMatrixValues();
            }
    //最后
         setImageMatrix(matrix);//使matrix生效
```
具体代码，主要针对图片缩放时的居中处理、缩小图片时的还原以及限定图片的缩放范围
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
    void onTouchMove(MotionEvent event) {

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
###存在的问题
在viewpager切换时，imageview跳跃过大，还没优化
