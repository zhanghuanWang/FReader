package com.feng.freader.view.activity;

import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.feng.freader.R;
import com.feng.freader.base.BaseActivity;
import com.feng.freader.constant.EventBusCode;
import com.feng.freader.constract.IReadContract;
import com.feng.freader.db.DatabaseManager;
import com.feng.freader.entity.data.BookshelfNovelDbData;
import com.feng.freader.entity.data.DetailedChapterData;
import com.feng.freader.entity.eventbus.Event;
import com.feng.freader.http.UrlObtainer;
import com.feng.freader.presenter.ReadPresenter;
import com.feng.freader.test.TestActivity;
import com.feng.freader.util.EventBusUtil;
import com.feng.freader.util.StatusBarUtil;
import com.feng.freader.widget.PageView;

import java.util.Collections;
import java.util.List;

/**
 * 小说阅读界面
 *
 * @author Feng Zhaohao
 * Created on 2019/11/25
 */
public class ReadActivity extends BaseActivity<ReadPresenter>
        implements IReadContract.View, View.OnClickListener {
    private static final String TAG = "ReadActivity";
    private static final String LOADING_TEXT = "正在加载中…";

    public static final String KEY_NOVEL_URL = "read_key_novel_url";
    public static final String KEY_NAME = "read_key_name";
    public static final String KEY_COVER = "read_key_cover";
    public static final String KEY_CHAPTER_INDEX = "read_key_chapter_index";
    public static final String KEY_POSITION = "read_key_position";
    public static final String KEY_PAGE_INDEX = "read_key_page_index";
    public static final String KEY_IS_REVERSE = "read_key_is_reverse";

    private PageView mPageView;
    private TextView mNovelTitleTv;
    private TextView mNovelProgressTv;
    private TextView mStateTv;

    private RelativeLayout mTopSettingBarRv;
    private ConstraintLayout mBottomSettingBarCv;
    private ImageView mBackIv;
    private ImageView mMenuIv;
    private TextView mPreviousChapterTv;
    private SeekBar mNovelProcessSb;
    private TextView mNextChapterTv;
    private ImageView mCatalogIv;
    private ImageView mBrightnessIv;
    private ImageView mDayAndNightModeIv;
    private ImageView mSettingIv;
    private TextView mCatalogTv;
    private TextView mBrightnessTv;
    private TextView mDayAndNightModeTv;
    private TextView mSettingTv;

    // 章节 url 列表（通过网络请求获取）
    private List<String> mChapterUrlList;
    // 以下内容通过 Intent 传入
    private String mNovelUrl;   // 小说 url
    private String mName;   // 小说名
    private String mCover;  // 小说封面
    private int mChapterIndex;   // 当前阅读的章节索引
    private int mPosition;  // 文本开始读取位置
    private int mPageIndex; // 文本开始读取的页
    private boolean mIsReverse; // 是否需要将章节列表倒序

    private DatabaseManager mDbManager;
    private boolean mIsLoadingChapter = false;  // 是否正在加载具体章节
    private boolean mIsShowingOrHidingSettingBar = false;  // 是否正在显示或隐藏设置栏

    @Override
    protected void doBeforeSetContentView() {
        StatusBarUtil.setLightColorStatusBar(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_read;
    }

    @Override
    protected ReadPresenter getPresenter() {
        return new ReadPresenter();
    }

    @Override
    protected void initData() {
        mNovelUrl = getIntent().getStringExtra(KEY_NOVEL_URL);
        mName = getIntent().getStringExtra(KEY_NAME);
        mCover = getIntent().getStringExtra(KEY_COVER);
        mChapterIndex = getIntent().getIntExtra(KEY_CHAPTER_INDEX, 0);
        mPosition = getIntent().getIntExtra(KEY_POSITION, 0);
        mPageIndex = getIntent().getIntExtra(KEY_PAGE_INDEX, 0);
        mIsReverse = getIntent().getBooleanExtra(KEY_IS_REVERSE, false);
        Log.d(TAG, "initData: mNovelUrl = " + mNovelUrl +
                ", mName = " + mName + ", mCover = " + mCover + ", mPosition = " + mPosition);

        mDbManager = DatabaseManager.getInstance();
    }

    @Override
    protected void initView() {
        mTopSettingBarRv = findViewById(R.id.rv_read_top_bar);
        mBottomSettingBarCv = findViewById(R.id.cv_read_bottom_bar);

        mPageView = findViewById(R.id.pv_read_page_view);
        mPageView.setPageViewListener(new PageView.PageViewListener() {
            @Override
            public void updateProgress(String progress) {
                mNovelProgressTv.setText(progress);
            }

            @Override
            public void next() {
                if (mChapterIndex == mChapterUrlList.size() - 1) {
                    showShortToast("已经到最后了");
                    return;
                }
                // 加载下一章节
                mChapterIndex++;
                showChapter();
            }

            @Override
            public void pre() {
                if (mChapterIndex == 0) {
                    showShortToast("已经到最前了");
                    return;
                }
                // 加载上一章节
                mChapterIndex--;
                showChapter();
            }

            @Override
            public void showOrHideSettingBar() {
                if (mIsShowingOrHidingSettingBar) {
                    return;
                }
                mIsShowingOrHidingSettingBar = true;
                if (mTopSettingBarRv.getVisibility() != View.VISIBLE) {
                    // 显示设置栏
                    showSettingBar();
                } else {
                    // 隐藏设置栏
                    hideSettingBar();
                }
            }
        });

        mNovelTitleTv = findViewById(R.id.tv_read_novel_title);
        mNovelProgressTv = findViewById(R.id.tv_read_novel_progress);
        mStateTv = findViewById(R.id.tv_read_state);

        mBackIv = findViewById(R.id.iv_read_back);
        mBackIv.setOnClickListener(this);
        mMenuIv = findViewById(R.id.iv_read_menu);
        mMenuIv.setOnClickListener(this);
        mPreviousChapterTv = findViewById(R.id.tv_read_previous_chapter);
        mPreviousChapterTv.setOnClickListener(this);
        mNextChapterTv = findViewById(R.id.tv_read_next_chapter);
        mNextChapterTv.setOnClickListener(this);
        mCatalogIv = findViewById(R.id.iv_read_catalog);
        mCatalogIv.setOnClickListener(this);
        mBrightnessIv = findViewById(R.id.iv_read_brightness);
        mBrightnessIv.setOnClickListener(this);
        mDayAndNightModeIv = findViewById(R.id.iv_read_day_and_night_mode);
        mDayAndNightModeIv.setOnClickListener(this);
        mSettingIv = findViewById(R.id.iv_read_setting);
        mSettingIv.setOnClickListener(this);
        mCatalogTv = findViewById(R.id.tv_read_catalog);
        mCatalogTv.setOnClickListener(this);
        mBrightnessTv = findViewById(R.id.tv_read_brightness);
        mBrightnessTv.setOnClickListener(this);
        mDayAndNightModeTv = findViewById(R.id.tv_read_day_and_night_mode);
        mDayAndNightModeTv.setOnClickListener(this);
        mSettingTv = findViewById(R.id.tv_read_setting);
        mSettingTv.setOnClickListener(this);

        mNovelProcessSb = findViewById(R.id.sb_read_novel_progress);
    }

    @Override
    protected void doAfterInit() {
        // 先通过小说 url 获取所有章节 url 信息
        mPresenter.getChapterUrlList(UrlObtainer.getCatalogInfo(mNovelUrl));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 将书籍信息存入数据库
        mDbManager.deleteBookshelfNovel(mNovelUrl);
        if (mIsReverse) {   // 如果倒置了目录的话，需要倒置章节索引
            mChapterIndex = mChapterUrlList.size() - 1 - mChapterIndex;
        }
        BookshelfNovelDbData dbData = new BookshelfNovelDbData(mNovelUrl, mName,
                mCover, mChapterIndex, mPageView.getPosition(), mPageView.getPageIndex());
        mDbManager.insertBookshelfNovel(dbData);

        Log.d(TAG, "dbData = " + mDbManager.queryAllBookshelfNovel());

        // 更新书架页面数据
        Event event = new Event(EventBusCode.BOOKSHELF_UPDATE_LIST);
        EventBusUtil.sendEvent(event);
    }

    @Override
    protected boolean isRegisterEventBus() {
        return false;
    }

    /**
     * 获取章节目录成功
     */
    @Override
    public void getChapterUrlListSuccess(List<String> chapterUrlList) {
        if (chapterUrlList == null || chapterUrlList.isEmpty()) {
            mStateTv.setText("获取章节目录信息失败");
            return;
        }
        mChapterUrlList = chapterUrlList;
        if (mIsReverse) {
            Collections.reverse(mChapterUrlList);
        }
        // 获取具体章节信息
        if (mChapterUrlList.get(mChapterIndex) != null) {
            mIsLoadingChapter = true;
            mPresenter.getDetailedChapterData(UrlObtainer
                    .getDetailedChapter(mChapterUrlList.get(mChapterIndex)));
        } else {
            mStateTv.setText("获取章节信息失败");
        }
    }

    /**
     * 获取章节目录失败
     */
    @Override
    public void getChapterUrlListError(String errorMsg) {
        mStateTv.setText(errorMsg);
    }

    /**
     * 获取具体章节信息成功
     */
    @Override
    public void getDetailedChapterDataSuccess(DetailedChapterData data) {
        mIsLoadingChapter = false;
        if (data == null) {
            mStateTv.setText("获取不到相关数据");
            Log.d(TAG, "getDetailedChapterDataSuccess: data = null");
            return;
        }
//        Log.d(TAG, "getDetailedChapterDataSuccess: mChapterUrlList = " + mChapterUrlList);
//        Log.d(TAG, "getDetailedChapterDataSuccess: data = " + data);
        mStateTv.setVisibility(View.GONE);
        mPageView.init(data.getContent(), mPosition, mPageIndex);
        mNovelTitleTv.setText(data.getName());
    }

    /**
     * 获取具体章节信息失败
     */
    @Override
    public void getDetailedChapterDataError(String errorMsg) {
        mIsLoadingChapter = false;
        mStateTv.setText(errorMsg);
    }

    /**
     * 点击上一页/下一页后加载具体章节
     */
    private void showChapter() {
        if (mIsLoadingChapter) {    // 已经在加载了
            return;
        }
        mPosition = mPageIndex = 0;     // 归零
        mPageView.clear();              // 清除当前文字
        mStateTv.setVisibility(View.VISIBLE);
        mStateTv.setText(LOADING_TEXT);
        mIsLoadingChapter = true;
        mPresenter.getDetailedChapterData(UrlObtainer.getDetailedChapter(
                mChapterUrlList.get(mChapterIndex)));
    }

    /**
     * 显示设置栏
     */
    private void showSettingBar() {
        Animation topAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_top_enter);
        topAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                StatusBarUtil.setDarkColorStatusBar(ReadActivity.this);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // 结束时重置标记
                mIsShowingOrHidingSettingBar = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        Animation bottomAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_bottom_enter);
        mTopSettingBarRv.startAnimation(topAnim);
        mBottomSettingBarCv.setAnimation(bottomAnim);
        mTopSettingBarRv.setVisibility(View.VISIBLE);
        mBottomSettingBarCv.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏设置栏
     */
    private void hideSettingBar() {
        Animation topExitAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_top_exit);
        topExitAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mTopSettingBarRv.setVisibility(View.GONE);
                mIsShowingOrHidingSettingBar = false;
                StatusBarUtil.setLightColorStatusBar(ReadActivity.this);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        Animation bottomExitAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_bottom_exit);
        bottomExitAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mBottomSettingBarCv.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mTopSettingBarRv.startAnimation(topExitAnim);
        mBottomSettingBarCv.setAnimation(bottomExitAnim);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_read_back:
                break;
            case R.id.iv_read_menu:
                break;
            case R.id.tv_read_previous_chapter:
                break;
            case R.id.tv_read_next_chapter:
                break;
            case R.id.iv_read_catalog:
            case R.id.tv_read_catalog:
                showShortToast("目录");
                break;
            case R.id.iv_read_brightness:
            case R.id.tv_read_brightness:
                showShortToast("亮度");
                break;
            case R.id.iv_read_day_and_night_mode:
            case R.id.tv_read_day_and_night_mode:
                showShortToast("日间夜间模式");
                break;
            case R.id.iv_read_setting:
            case R.id.tv_read_setting:
                showShortToast("设置");
                break;
            default:
                break;
        }
    }
}
