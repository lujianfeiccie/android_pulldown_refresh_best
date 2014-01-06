package com.example.wbd;

import java.util.Date;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 自定义GridView
 * @author Administrator
 *
 */
public class PullListView extends ListView implements OnScrollListener,android.view.View.OnClickListener
{

	String TAG = getClass().getSimpleName();
	
    private final static int RELEASE_To_REFRESH = 0;//
    
    private final static int PULL_To_REFRESH = 1;
    
    private final static int REFRESHING = 2;
    
    private final static int DONE = 3;
    
    private final static int LOADING = 4;
    
    // 实际的padding的距离与界面上偏移距离的比例
    private final static int RATIO = 2;
    private final static String tips1 = "下拉刷新";
    private final static String tips2 = "更多...";
    private final static String tips3 = "加载中...";
    private LayoutInflater inflater;
    
    private RotateAnimation animation;
    
    private RotateAnimation reverseAnimation;
    
    private Boolean isLastIndex = false;
    
    private Boolean isfirstIndex = false;
    
    // 用于保证startY的值在一个完整的touch事件中只被记录一次
    private boolean isRecored=false;
    
    
    private int startY= 0;
    
    private int firstItemIndex=-1;
    
    private int state;
    
    private boolean isBack=false;
    
    private OnRefreshListener refreshListener;
    
    private boolean isRefreshable=true;
    
    private TextView lastUpdatedTextView;//上次更新时间
    
    public boolean headerfooter = false; // false means head, true means footer	
    
    boolean hasAddHeader = false;
    boolean hasAddFooter = false;
    
    class HeaderClass{
    	public ImageView arrowImageView; //箭头
    	public TextView tipsTextview;//头视图文本框
    	public LinearLayout headView;//头视图
    	public ProgressBar progressBar;
    	public int headContentHeight;
    	public int headContentWidth;
    	public boolean enableHeader = false;
    }
    class FooterClass{
    	public TextView fTipsTextview;//脚视图文本框
    	public LinearLayout footerView;//脚视图
    	public ProgressBar fProgressBar;
    	public int footerContentHeight;
        public int footerContentWidth;    	
        public boolean enableFooter = false;
    }
    HeaderClass mHeaderClass = new HeaderClass();
    FooterClass mFooterClass = new FooterClass();
    
    public PullListView(Context context)
    {
        this(context,null);
    }
    public PullListView(Context context, AttributeSet attrs)
    {
    	this(context, attrs,0);
    	TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PullListView);
    	mHeaderClass.enableHeader = array.getBoolean(R.styleable.PullListView_enableHeader, false); // 脚视图(加载更多)
    	mFooterClass.enableFooter = array.getBoolean(R.styleable.PullListView_enableFooter, false); // 头视图(刷新)
		updateHeaderFooterState(mHeaderClass.enableHeader, mFooterClass.enableFooter);
    }
    public PullListView(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    	init(context);
    }
    
    public PullListView(Context context,boolean enableHeader, boolean enableFooter)
    {
    	this(context);
    	mHeaderClass.enableHeader = enableHeader;
    	mFooterClass.enableFooter = enableFooter;
    	updateHeaderFooterState(enableHeader, enableFooter);
    }
	private void updateHeaderFooterState(boolean enableHeader,
			boolean enableFooter) {
		if(enableHeader){
			showHeader();
		}
		if(enableFooter){
			showFooter();
		}
	}
    
	
    
    public View getHeadView()
    {
        return mHeaderClass.headView;
    }
    
    public View getFooterView()
    {
        return mFooterClass.footerView;
    }
    
    
    private void init(Context context)
    {
        setCacheColorHint(context.getResources().getColor(R.drawable.transparent)); // set the background color
        
        //动态添加视图布局文件。
        inflater = LayoutInflater.from(context);
        
        mHeaderClass.headView = (LinearLayout)inflater.inflate(R.layout.layout_pulllistview_head, null);
        
        mFooterClass.footerView = (LinearLayout)inflater.inflate(R.layout.layout_pulllistview_footer, this, false);
        mFooterClass.footerView.setOnClickListener(this);
        
        setHeadViews(mHeaderClass.headView);
        setFooterViews(mFooterClass.footerView);
        
        setOnScrollListener(this);
        
        animation =
            new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        animation.setInterpolator(new LinearInterpolator());
        animation.setDuration(250);
        animation.setFillAfter(true);
        
        reverseAnimation =
            new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        reverseAnimation.setInterpolator(new LinearInterpolator());
        reverseAnimation.setDuration(200);
        reverseAnimation.setFillAfter(true);
        
        log("init====state = DONE");
        //初始化 上下拉的 状态
        state = DONE;
        
        isRefreshable = false; //默认不启动刷新
    }
    //要在setAdapter之前调用
    public void showHeader(){
    	if(!hasAddHeader){
    	addHeaderView(mHeaderClass.headView);
    		hasAddHeader = true;
    	}
    }
    //要在setAdapter之前调用
    public void showFooter(){
	    if(!hasAddFooter){
	      addFooterView(mFooterClass.footerView);
	      //加入正在刷新
		  mFooterClass.footerView.setPadding(0, 0, 0, 0);
	      mFooterClass.fProgressBar.setVisibility(View.INVISIBLE);
	      mFooterClass.fTipsTextview.setText(tips2);
	      	hasAddFooter = true; 
	    }
    }
    private void hideHeader(){
    	removeHeaderView(mHeaderClass.headView);
    }
    private void hideFooter(){
    	removeFooterView(mFooterClass.footerView);
    }
    
    public void setFooterText(String msg){
    	mFooterClass.fTipsTextview.setText(msg);
    }
    private void setHeadViews(LinearLayout headView)
    {
        mHeaderClass.arrowImageView = (ImageView)headView.findViewById(R.id.head_arrowImageView);
        mHeaderClass.arrowImageView.setMinimumWidth(70);
        mHeaderClass.arrowImageView.setMinimumHeight(50);
        mHeaderClass.progressBar = (ProgressBar)headView.findViewById(R.id.head_progressBar);
        mHeaderClass.tipsTextview = (TextView)headView.findViewById(R.id.head_tipsTextView);
        lastUpdatedTextView = (TextView)headView.findViewById(R.id.head_lastUpdatedTextView);
        
        measureView(headView);
        mHeaderClass.headContentHeight = headView.getMeasuredHeight();
         mHeaderClass.headContentWidth = headView.getMeasuredWidth();
        
        headView.setPadding(0, -1 * mHeaderClass.headContentHeight, 0, 0); // 0,-1 * headContentHeight,0,0
        headView.invalidate();
    }
    
    public void showHeaderViewForUpdating(){
    	state = REFRESHING;
    	changeHeaderViewByState();
    }
    public void showFooterViewForUpdating(){
    	state = REFRESHING;
    	changeFooterViewByState();
    }
    public void setFooterViews(View footerView)
    {
    	mFooterClass.fProgressBar = (ProgressBar)footerView.findViewById(R.id.footer_progressBar);
    	mFooterClass.fTipsTextview = (TextView)footerView.findViewById(R.id.footer_tipsTextView);
    	mFooterClass.fTipsTextview.setText(tips2);
    	
    	measureView(footerView);
    	mFooterClass.footerContentHeight = footerView.getMeasuredHeight();
    	mFooterClass.footerContentWidth = footerView.getMeasuredWidth();
    	
    	//footerView.setPadding(0, 0, 0, -1 * footerContentHeight); // 0,-1 * headContentHeight,0,0
    	footerView.setPadding(0, -1*mFooterClass.footerContentHeight, 0, 0); // 0,-1 * headContentHeight,0,0
    	footerView.invalidate();
    }
    
    public void onScroll(AbsListView arg0, int firstVisibleItem, int visibleItemCount, int totalItemCount)
    {
        firstItemIndex = firstVisibleItem;
        
        log(String.format("onScroll==firstVisibleItem=%s,visibleItemCount=%s,totalItemCount=%s,headerfooter=%s",
        		firstVisibleItem,
        		visibleItemCount,
        		totalItemCount,
        		headerfooter));
        if (firstItemIndex == 0)//最顶上那一项位于listview的index=0的地方
        {
        	
            isfirstIndex = true;
            isLastIndex = false;
        }
        else if(firstVisibleItem + visibleItemCount == totalItemCount){ //滑到最底下了
        	isLastIndex = true;
        	
        	if(refreshListener!=null && state!= REFRESHING){
				state = REFRESHING;
				changeFooterViewByState();
				refreshListener.onMore();
			}
        }
    }
    
    public void onScrollStateChanged(AbsListView view, int scrollState)
    {
        if (firstItemIndex > 0)
        {
        	log("onScrollStateChanged===is Not firstIndex = false");
            isfirstIndex = false;
        }
        if(isLastIndex && headerfooter){
        }
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        
        if (isRefreshable)
        {
            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    if (firstItemIndex == 0 && !isRecored)
                    {
                        isRecored = true;
                        startY = (int)event.getY();
                    }
                    break;
                
                case MotionEvent.ACTION_UP:
                    
                    if (state != REFRESHING && state != LOADING)
                    {
                        if (state == DONE)
                        {
                            // 什么都不做
                        	//log("ACTION_UP===DONE");
                        }else
                        if (state == PULL_To_REFRESH)
                        {
                        	log("ACTION_UP===PULL_To_REFRESH state = DONE");
                            state = DONE;
                            if (headerfooter)
                            {
                                changeFooterViewByState();
                            }
                            else
                            {
                            	changeHeaderViewByState();
                            }
                            
                        }else
                        if (state == RELEASE_To_REFRESH)
                        {
                        	log("ACTION_UP===RELEASE_To_REFRESH state = REFRESHING");
                        	
                            state = REFRESHING;
                            
                            // 向下拉
                            if (headerfooter)
                            {
                                changeFooterViewByState();
                                onMore();
                            }
                            else
                            {
                                changeHeaderViewByState();
                                onRefresh();
                            }
                        }
                    }
                    
                    isRecored = false;
                    isBack = false;
                    
                    break;
                
                case MotionEvent.ACTION_MOVE:
                    int tempY = (int)event.getY();
                    // Log.v("tempY", tempY + "");
                    if (tempY > startY) // pull down
                    {
                    	refreshListener.onDown();
                        headerfooter = false;
                        if (!isRecored && firstItemIndex == 0)
                        {
                            // Log.v(TAG, "在move时候记录下位置");
                            isRecored = true;
                            startY = tempY;
                        }
                        if (state != REFRESHING && isRecored && state != LOADING)
                        {
                            
                            // 保证在设置padding的过程中，当前的位置一直是在head，否则如果当列表超出屏幕的话，当在上推的时候，列表会同时进行滚动
                            
                            // 可以松手去刷新了
                            if (state == RELEASE_To_REFRESH)
                            {
                                
                                // setSelection(0);
                                
                                // 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
                                if (((tempY - startY) / RATIO < mHeaderClass.headContentHeight) && (tempY - startY) > 0)
                                {
                                	log("ACTION_MOVE=======state = PULL_To_REFRESH");
                                    state = PULL_To_REFRESH;
                                    changeHeaderViewByState();
                                    
                                    // Log.v(TAG, "由松开刷新状态转变到下拉刷新状态");
                                }
                                // 一下子推到顶了
                                else if (tempY - startY <= 0)
                                {
                                	log("ACTION_MOVE DOWN=======state = DONE");
                                    state = DONE;
                                    changeHeaderViewByState();
                                    
                                    // Log.v(TAG, "由松开刷新状态转变到done状态");
                                }
                                // 往下拉了，或者还没有上推到屏幕顶部掩盖head的地步
                                else
                                {
                                    // 不用进行特别的操作，只用更新paddingTop的值就行了
                                }
                            }
                            // 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态
                            if (state == PULL_To_REFRESH && isfirstIndex)
                            {
                                
                                // setSelection(0);
                                
                                // 下拉到可以进入RELEASE_TO_REFRESH的状态
                                if ((tempY - startY) / RATIO >= mHeaderClass.headContentHeight)
                                {
                                	log("ACTION_MOVE DOWN=======state = RELEASE_To_REFRESH");;
                                    state = RELEASE_To_REFRESH;
                                    isBack = true;
                                    changeHeaderViewByState();
                                    
                                    // Log.v(TAG, "由done或者下拉刷新状态转变到松开刷新");
                                }
                                // 上推到顶了
                                else if (tempY - startY <= 0)
                                {
                                	log("ACTION_MOVE DOWN =======state = DONE");
                                    state = DONE;
                                    changeHeaderViewByState();
                                    
                                }
                            }
                            
                            // done状态下
                            if (state == DONE)
                            {
                                if (tempY - startY > 0)
                                {
                                	log("ACTION_MOVE DOWN=======state=PULL_To_REFRESH");
                                    state = PULL_To_REFRESH;
                                    changeHeaderViewByState();
                                }
                            }
                            
                            {
                                // 更新headView的size
                            	if (isfirstIndex)
                                if (state == PULL_To_REFRESH)
                                {
                                	
                                    mHeaderClass.headView.setPadding(0, -1 * mHeaderClass.headContentHeight + (tempY - startY) / RATIO, 0, 0);
                                }
                                
                                // 更新headView的paddingTop
                                if (state == RELEASE_To_REFRESH)
                                {
                                    mHeaderClass.headView.setPadding(0, (tempY - startY) / RATIO - mHeaderClass.headContentHeight, 0, 0);
                                }
                            }
                            
                        }
                    }
                    
                    // 上拉
                    // else if (false)
                    else if (tempY < startY)
                    {
                    	refreshListener.onUp();
                        headerfooter = true;
                        if (!isRecored && isLastIndex)
                        {
                            // Log.v(TAG, "在move时候记录下位置");
                            isRecored = true;
                            startY = tempY;
                            // Log.v("StartY:", startY + "");
                        }
                        
                        if (state != REFRESHING && isRecored && state != LOADING)
                        {
                            
                            // 保证在设置padding的过程中，当前的位置一直是在head，否则如果当列表超出屏幕的话，当在上推的时候，列表会同时进行滚动
                            
                            // 可以松手去刷新了
                            if (state == RELEASE_To_REFRESH)
                            {
                                
                                // 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
                                if (((startY - tempY) / RATIO < mFooterClass.footerContentHeight) && (startY - tempY) > 0)
                                {
                                	log("ACTION_MOVE UP====state = PULL_To_REFRESH");
                                    state = PULL_To_REFRESH;
                                    changeFooterViewByState();
                                    
                                    // Log.v(TAG, "由松开刷新状态转变到下拉刷新状态");
                                }
                                // 一下子推到顶了
                                else if (startY - tempY <= 0)
                                {
                                	log("ACTION_MOVE UP======state=DONE");
                                    state = DONE;
                                    changeFooterViewByState();
                                    
                                    // Log.v(TAG, "由松开刷新状态转变到done状态");
                                }
                                // 往下拉了，或者还没有上推到屏幕顶部掩盖head的地步
                                else
                                {
                                    // 不用进行特别的操作，只用更新paddingTop的值就行了
                                }
                            }
                            // 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态
                            if (state == PULL_To_REFRESH && isLastIndex)
                            {
                                
                                // setSelection(getCount() - 1);
                                
                                // 下拉到可以进入RELEASE_TO_REFRESH的状态
                                if ((startY - tempY) / RATIO >= mFooterClass.footerContentHeight)
                                {
                                	log("ACTION_MOVE UP======state = RELEASE_To_REFRESH");
                                    state = RELEASE_To_REFRESH;
                                    isBack = true;
                                    changeFooterViewByState();
                                    
                                    // Log.v(TAG, "由done或者下拉刷新状态转变到松开刷新");
                                }
                                // 上推到顶了
                                else if (startY - tempY <= 0)
                                {
                                	log("ACTION_MOVE UP state = DONE");
                                    state = DONE;
                                    changeFooterViewByState();
                                    
                                    // Log.v(TAG, "由DOne或者下拉刷新状态转变到done状态");
                                }
                            }
                            // done状态下
                            if (state == DONE)
                            {
                                if (startY - tempY > 0)
                                {
                                	log("line472=== state = PULL_To_REFRESH");
                                    state = PULL_To_REFRESH;
                                    changeFooterViewByState();
                                }
                            }
                        }
                    }
                    
                    break;
            }
        }
        
        return super.onTouchEvent(event);
    }
    
    private void changeFooterViewByState()
    {
        // TODO Auto-generated method stub
        switch (state)
        {
            case RELEASE_To_REFRESH:
            	log("RELEASE_To_REFRESH");
                mFooterClass.fProgressBar.setVisibility(View.GONE);
                mFooterClass.fProgressBar.setVisibility(View.VISIBLE);
                mFooterClass.fTipsTextview.setText("松开刷新");
                
                break;
            case PULL_To_REFRESH:
            	log("PULL_To_REFRESH");
                mFooterClass.fProgressBar.setVisibility(View.GONE);
                mFooterClass.fTipsTextview.setVisibility(View.VISIBLE);
                // 是由RELEASE_To_REFRESH状态转变来的
                if (isBack)
                {
                    isBack = false;
                    mFooterClass.fTipsTextview.setText(tips2);
                }
                else
                {
                    mFooterClass.fTipsTextview.setText(tips2);
                }
                break;
            
            case REFRESHING:
            	log("REFRESHING");
                mFooterClass.footerView.setPadding(0, 0, 0, 0);
                mFooterClass.fProgressBar.setVisibility(View.VISIBLE);
                mFooterClass.fTipsTextview.setText(tips3);
                break;
            case DONE:
            	log("DONE");
            	mFooterClass.footerView.setPadding(0, 0, 0, 0);
                mFooterClass.fProgressBar.setVisibility(View.GONE);
                mFooterClass.fTipsTextview.setText(tips2);
                break;
        }
    }
    
    // 当状态改变时候，调用该方法，以更新界面
    private void changeHeaderViewByState()
    {
    	log("changeHeaderViewByState");
        switch (state)
        {
            case RELEASE_To_REFRESH:
            	log("=======RELEASE_To_REFRESH====");
                Log.e("test", "RELEASE_To_REFRESH");
                mHeaderClass.arrowImageView.setVisibility(View.VISIBLE);
                mHeaderClass.progressBar.setVisibility(View.GONE);
                mHeaderClass.tipsTextview.setVisibility(View.VISIBLE);
                lastUpdatedTextView.setVisibility(View.VISIBLE);
                
                mHeaderClass.arrowImageView.clearAnimation();
                mHeaderClass.arrowImageView.startAnimation(animation);
                
                mHeaderClass.tipsTextview.setText("松开刷新");
                
                // Log.v(TAG, "当前状态，松开刷新");
                break;
            case PULL_To_REFRESH:
                log("=======PULL_To_REFRESH====");
                mHeaderClass.progressBar.setVisibility(View.GONE);
                mHeaderClass.tipsTextview.setVisibility(View.VISIBLE);
                lastUpdatedTextView.setVisibility(View.VISIBLE);
                mHeaderClass.arrowImageView.clearAnimation();
                mHeaderClass.arrowImageView.setVisibility(View.VISIBLE);
                // 是由RELEASE_To_REFRESH状态转变来的
                if (isBack)
                {
                    isBack = false;
                    mHeaderClass.arrowImageView.clearAnimation();
                    mHeaderClass.arrowImageView.startAnimation(reverseAnimation);
                    
                    mHeaderClass.tipsTextview.setText(tips1);
                }
                else
                {
                    mHeaderClass.tipsTextview.setText(tips1);
                }
                // Log.v(TAG, "当前状态，下拉刷新");
                break;
            
            case REFRESHING:
                //Log.e("test", "REFRESHING");
            	log("=======REFRESHING====");
                mHeaderClass.headView.setPadding(0, 0, 0, 0);
                
                mHeaderClass.progressBar.setVisibility(View.VISIBLE);
                mHeaderClass.arrowImageView.clearAnimation();
                mHeaderClass.arrowImageView.setVisibility(View.GONE);
                mHeaderClass.tipsTextview.setText(tips3);
                lastUpdatedTextView.setVisibility(View.VISIBLE);
                
                // Log.v(TAG, "当前状态,tips3");
                break;
            case DONE:
                mHeaderClass.headView.setPadding(0, -1 * mHeaderClass.headContentHeight, 0, 0);
                
                mHeaderClass.progressBar.setVisibility(View.GONE);
                mHeaderClass.arrowImageView.clearAnimation();
                mHeaderClass.arrowImageView.setImageResource(R.drawable.ic_pulltorefresh_arrow);
                mHeaderClass.tipsTextview.setText(tips1);
                lastUpdatedTextView.setVisibility(View.VISIBLE);
                
                log("=======DONE========");
                
                break;
        }
    }
    
    public void setonRefreshListener(OnRefreshListener refreshListener)
    {
        this.refreshListener = refreshListener;
        isRefreshable = true;
    }
    
    public interface OnRefreshListener
    {
        public void onRefresh();
        
        public void onMore();
        
        public void onDown();

        public void onUp();
    }
    
    public void onRefreshComplete()
    {
    	log("line658====state=DONE");
        state = DONE;
        lastUpdatedTextView.setText("最近更新:" + new Date().toLocaleString());
        changeHeaderViewByState();
        changeFooterViewByState();
    }
    
    private void onRefresh()
    {
        if (refreshListener != null)
        {
            refreshListener.onRefresh();
        }
    }
    
    private void onMore()
    {
        if (refreshListener != null)
        {
            refreshListener.onMore();
        }
    }
    
    // 此方法直接照搬自网络上的一个下拉刷新的demo，此处是“估计”headView的width以及height
    private void measureView(View child)
    {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null)
        {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            // set the width and height of the child of the view
        }
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0)
        {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        }
        else
        {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }
    
    public void setAdapter(BaseAdapter adapter)
    {
        lastUpdatedTextView.setText("最近更新:" + new Date().toLocaleString());
        super.setAdapter(adapter);
    }

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v==mFooterClass.footerView){
			if(refreshListener!=null){
				state = REFRESHING;
				changeFooterViewByState();
				refreshListener.onMore();
			}
		}
	}
	void log(String msg){
		Log.d(TAG, msg);
	}
}
