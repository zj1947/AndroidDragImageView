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

        initialize();

    }

    private void initialize() {
        initData();
        initView();
        initListener();
    }

    private void initData() {

        imgLists=new ArrayList<>();

        for (Integer id:imgIds){
            imgLists.add(id);
        }

        iIndex=0;
    }

    private void initView() {

        mViewPager=(ViewPager)findViewById(R.id.vp_img_prev_container);
        viewPagerAdapter=new PictureSlidePagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(viewPagerAdapter);

        initDots();
    }

    private void initListener() {
        //viewPager的页面切换监听
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

    /**
     * 初始化指示点，根据图片数组数量创建指示点
     */
    private void initDots() {

        //只有一张图片，或者没有图片，不创建指示点
        if (imgLists.size()<=1){
            return;
        }
        //指示点的容器，横向linearLayout
        LinearLayout ll = (LinearLayout) findViewById(R.id.indicator);

        dots = new ImageView[imgLists.size()];

        //两个指示点之间间隙
        int dip5= UiUtils.dip2px(this, 5);

        LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(dip5*2,dip5,1);
        layoutParams.setMargins(dip5/2,0,dip5/2,0);



        // 循环取得小点图片
        for (int i = 0; i < imgLists.size(); i++) {

            ImageView imageView = new ImageView(this);
            dots[i] = imageView;
            dots[i].setBackgroundColor(Color.WHITE);// 都设为白色
            ll.addView(dots[i],layoutParams);
        }

        dots[iIndex].setBackgroundColor(Color.RED);// 设置为白色，即选中状态
    }

    /**
     * 设置指示点的选中位置
     * @param position
     */
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
