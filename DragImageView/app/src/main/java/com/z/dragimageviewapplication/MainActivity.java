package com.z.dragimageviewapplication;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.z.dragimageviewapplication.ui.ImgDetailFragment;
import com.z.dragimageviewapplication.util.UiUtils;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    private static final String IMAGE_CACHE_DIR = "thumbs_big";


    private ViewPager mViewPager;
    private int iIndex;

    private ImageView[] dots;
    private ArrayList<Integer> imgLists;
    private PictureSlidePagerAdapter viewPagerAdapter;

    private int imgIds[]={
            R.drawable.first,
            R.drawable.second,
            R.drawable.third
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        imgLists=new ArrayList<>();

        for (Integer id:imgIds){
            imgLists.add(id);
        }

        iIndex=0;

        mViewPager=(ViewPager)findViewById(R.id.vp_img_prev_container);
        viewPagerAdapter=new PictureSlidePagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(viewPagerAdapter);


        initDots();

        pageViewListener();

    }

    @Override
    public void onResume(){
        super.onResume();
    }
    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    private void pageViewListener() {

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int position) {
                setCurrentDot(position);
            }

            @Override
            public void onPageScrollStateChanged(int position) {

            }
        });
    }

    class PictureSlidePagerAdapter extends FragmentStatePagerAdapter {

        public PictureSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return ImgDetailFragment.newInstance(imgLists.get(i));
        }

        @Override
        public int getCount() {
            return imgLists.size();
        }
    }

    private void initDots() {


        if (imgLists.size()<=1){
            return;
        }

        LinearLayout ll = (LinearLayout) findViewById(R.id.indicator);

        dots = new ImageView[imgLists.size()];

        int dip5= UiUtils.dip2px(this, 5);

        LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(dip5*2,dip5,1);
        layoutParams.setMargins(dip5/2,0,dip5/2,0);



        // 循环取得小点图片
        for (int i = 0; i < imgLists.size(); i++) {

            ImageView imageView = new ImageView(this);
            dots[i] = imageView;
            dots[i].setBackgroundColor(Color.WHITE);// 都设为灰色
            ll.addView(dots[i],layoutParams);
        }

        dots[iIndex].setBackgroundColor(Color.RED);// 设置为白色，即选中状态
    }

    private void setCurrentDot(int position) {

        if (position < 0 || position > imgLists.size() - 1||imgLists.size()<=1
                || iIndex == position) {
            return;
        }

        dots[position].setBackgroundColor(Color.RED);
        dots[iIndex].setBackgroundColor(Color.WHITE);

        iIndex = position;
    }
}
