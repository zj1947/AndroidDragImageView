package com.z.dragimageviewapplication.ui;


import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.z.dragimageviewapplication.R;
import com.z.dragimageviewapplication.util.BitmapUtil;
import com.z.dragimageviewapplication.util.UiUtils;
import com.z.dragimageviewapplication.view.DragImageView;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 */
public class ImgDetailFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";

    private DragImageView imageView;
    private ProgressBar progressBar;

    private int imgResourceId;//图片资源ID
    private int screenWidth;//屏幕宽度
    private int screenHeight;//屏幕高度
    private int state_height=0;//状态栏高度


    /**
     * 获取ImgDetailFragment 实例
     * @param imgResourceId 图片资源ID
     * @return
     */
    public static ImgDetailFragment newInstance(int imgResourceId) {
        ImgDetailFragment fragment = new ImgDetailFragment();
        Bundle arg = new Bundle();
        arg.putInt(ARG_PARAM1, imgResourceId);
        fragment.setArguments(arg);
        return fragment;
    }


    public ImgDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);

        if (getArguments() != null) {
            imgResourceId = getArguments().getInt(ARG_PARAM1);
        }


        screenWidth = UiUtils.getWindowWidth(this.getActivity());
        screenHeight = UiUtils.getWindowHeight(this.getActivity());

        if (null==saveInstanceState&&state_height == 0) {
            Rect frame = new Rect();
            ImgDetailFragment.this.getActivity().getWindow().getDecorView()
                    .getWindowVisibleDisplayFrame(frame);
            state_height = frame.top;
        }else {
            state_height=saveInstanceState.getInt("state_height");
        }

        screenHeight = screenHeight - state_height;

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = (View) inflater.inflate(R.layout.fragment_img_detail, container, false);

        imageView = (DragImageView) view.findViewById(R.id.iv_big_img);
        progressBar = (ProgressBar) view.findViewById(R.id.pb_img_prev_loading);

        imageView.setScreen_H(screenHeight);
        imageView.setScreen_W(screenWidth);

        return view;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);


        setBitmap(BitmapUtil.ReadBitmapById(getActivity(),imgResourceId));
    }


    @Override
    public void onSaveInstanceState(Bundle outState){
        outState.putInt("state_height", state_height);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach(){
        if (imageView != null) {
            // Cancel any pending image work
            imageView.setImageDrawable(null);
        }
        super.onDetach();
    }

    public void setBitmap( Bitmap loadedImage) {


        float bitmap_W = loadedImage.getWidth();
        float bitmap_H = loadedImage.getHeight();

        progressBar.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageBitmap(loadedImage, screenWidth / bitmap_W);


        Matrix matrix = imageView.getImageMatrix();
        float matrixValue[] = new float[9];
        matrix.getValues(matrixValue);
        float scale = 1 / matrixValue[0] * screenWidth / bitmap_W;
        matrix.postScale(scale, scale);

        matrix.getValues(matrixValue);

        float xCenterCoordinate = (screenWidth - bitmap_W * scale) / 2;
        float yCenterCoordinate = (screenHeight - bitmap_H * scale) / 2;

        float dx = xCenterCoordinate - matrixValue[2];
        float dy = yCenterCoordinate - matrixValue[5];

        matrix.postTranslate(dx, dy);

        imageView.setImageMatrix(matrix);

    }

}
