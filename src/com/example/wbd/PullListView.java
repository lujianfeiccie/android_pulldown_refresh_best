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
	
    private final static int RELEASE_To_REFRESH = 0;
    
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
    
    private LinearLayout headView;
    
    private LinearLayout footerView;
    
    private TextView tipsTextview;
    
    private TextView fTipsTextview;
    
    private TextView lastUpdatedTextView;
    
    private ImageView arrowImageView;
    
   // private ImageView fArrowImageView;
    
    private ProgressBar progressBar;
    
    private ProgressBar fProgressBar;
    
    private RotateAnimation animation;
    
    private RotateAnimation reverseAnimation;
    
    private Boolean isLastIndex = false;
    
    private Boolean isfirstIdex = false;
    
    // 用于保证startY的值在一个完整的touch事件中只被记录一次
    private boolean isRecored=false;
    
     private int headContentWidth;
    
     private int footerContentWidth;
    
    private int headContentHeight;
    
    private int footerContentHeight;
    
    private int startY= 0;
    
    private int firstItemIndex=-1;
    
    private int state;
    
    private boolean isBack=false;
    
    private OnRefreshListener refreshListener;
    
    private boolean isRefreshable=true;
    
    private boolean headorfooter = false; // false means head, true means footer
    
    private boolean enableHeader = false;
    private boolean enableFooter = false;
    public PullListView(Context context)
    {
        this(context,null);
    }
    public PullListView(Context context, AttributeSet attrs)
    {
    	this(context, attrs,0);
    	TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PullListView);
    	enableHeader = array.getBoolean(R.styleable.PullListView_enableHeader, false); // 脚视图(加载更多)
		enableFooter = array.getBoolean(R.styleable.PullListView_enableFooter, false); // 头视图(刷新)
		updateHeaderFooterState(enableHeader, enableFooter);
    }
    public PullListView(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    	init(context);
    }
    
    public PullListView(Context context,boolean enableHeader, boolean enableFooter)
    {
    	this(context);
    	this.enableHeader = enableHeader;
    	this.enableFooter = enableFooter;
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
        return headView;
        // return
    }
    
    public View getFooterView()
    {
        return footerView;
    }
    
    
    private void init(Context context)
    {
        setCacheColorHint(context.getResources().getColor(R.drawable.transparent)); // set the background color
        
        //动态添加视图布局文件。
        inflater = LayoutInflater.from(context);
        
        headView = (LinearLayout)inflater.inflate(R.layout.layout_pulllistview_head, null);
        
        footerView = (LinearLayout)inflater.inflate(R.layout.layout_pulllistview_footer, this, false);
        footerView.setOnClickListener(this);
        
        setHeadViews(headView);
        setFooterViews(footerView);
        
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
    private void showHeader(){
    	addHeaderView(headView);
    }
    //要在setAdapter之前调用
    private void showFooter(){
    	 //footerView.setPadding(0, 0, 0, 0);
      addFooterView(footerView);
      //加入正在刷新
	  footerView.setPadding(0, 0, 0, 0);
      fProgressBar.setVisibility(View.VISIBLE);
      fTipsTextview.setText(tips3);
    }
    
    private void hideHeader(){
    	removeHeaderView(headView);
    }
    private void hideFooter(){
    	removeFooterView(footerView);
    }
    
    public void setFooterText(String msg){
    	fTipsTextview.setText(msg);
    }
    private void setHeadViews(LinearLayout headView)
    {
        arrowImageView = (ImageView)headView.findViewById(R.id.head_arrowImageView);
        arrowImageView.setMinimumWidth(70);
        arrowImageView.setMinimumHeight(50);
        progressBar = (ProgressBar)headView.findViewById(R.id.head_progressBar);
        tipsTextview = (TextView)headView.findViewById(R.id.head_tipsTextView);
        lastUpdatedTextView = (TextView)headView.findViewById(R.id.head_lastUpdatedTextView);
        
        measureView(headView);
        headContentHeight = headView.getMeasuredHeight();
         headContentWidth = headView.getMeasuredWidth();
        
        headView.setPadding(0, -1 * headContentHeight, 0, 0); // 0,-1 * headContentHeight,0,0
        headView.invalidate();
    }
    
    /*public void setFooterViews(LinearLayout footerView)
    {
        //fArrowImageView = (ImageView)footerView.findViewById(R.id.footer_arrowImageView);
       // fArrowImageView.setMinimumWidth(70);
       // fArrowImageView.setMinimumHeight(50);
        fProgressBar = (ProgressBar)footerView.findViewById(R.id.footer_progressBar);
        fTipsTextview = (TextView)footerView.findViewById(R.id.footer_tipsTextView);
        fTipsTextview.setText(tips2);
        
        measureView(footerView);
        footerContentHeight = footerView.getMeasuredHeight();
        footerContentWidth = footerView.getMeasuredWidth();
        
        //footerView.setPadding(0, 0, 0, -1 * footerContentHeight); // 0,-1 * headContentHeight,0,0
        footerView.setPadding(0, 0, 0, 0); // 0,-1 * headContentHeight,0,0
        footerView.invalidate();
    }*/
    public void setFooterViews(View footerView)
    {
    	fProgressBar = (ProgressBar)footerView.findViewById(R.id.footer_progressBar);
    	fTipsTextview = (TextView)footerView.findViewById(R.id.footer_tipsTextView);
    	fTipsTextview.setText(tips2);
    	
    	measureView(footerView);
    	footerContentHeight = footerView.getMeasuredHeight();
    	footerContentWidth = footerView.getMeasuredWidth();
    	
    	//footerView.setPadding(0, 0, 0, -1 * footerContentHeight); // 0,-1 * headContentHeight,0,0
    	footerView.setPadding(0, -1*footerContentHeight, 0, 0); // 0,-1 * headContentHeight,0,0
    	footerView.invalidate();
    }
    
    public void onScroll(AbsListView arg0, int firstVisiableItem, int arg2, int arg3)
    {
        firstItemIndex = firstVisiableItem;
        
        if (firstItemIndex == 0)
        {
        	log("onScroll======isfirstIdex = true");
            isfirstIdex = true;
            isLastIndex = false;
        }
    }
    
    public void onScrollStateChanged(AbsListView view, int scrollState)
    {
        if (firstItemIndex > 0)
        {
        	log("onScrollStateChanged1======isfirstIdex = false");
            isfirstIdex = false;
        }
        
       /* if (view.getLastVisiblePosition() == view.getCount() - 1)
        {
        	log("onScrollStateChanged2======isfirstIdex = false");
            isLastIndex = true;
            isfirstIdex = false;
        }
        */
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
                            if (headorfooter)
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
                        	log("ACTION_UP===state = REFRESHING");
                        	
                            state = REFRESHING;
                            
                            // 向下拉
                            if (headorfooter)
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
                        headorfooter = false;
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
                                if (((tempY - startY) / RATIO < headContentHeight) && (tempY - startY) > 0)
                                {
                                	log("ACTION_MOVE=======state = PULL_To_REFRESH");
                                    state = PULL_To_REFRESH;
                                    changeHeaderViewByState();
                                    
                                    // Log.v(TAG, "由松开刷新状态转变到下拉刷新状态");
                                }
                                // 一下子推到顶了
                                else if (tempY - startY <= 0)
                                {
                                	log("ACTION_MOVE=======state = DONE");
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
                            if (state == PULL_To_REFRESH && isfirstIdex)
                            {
                                
                                // setSelection(0);
                                
                                // 下拉到可以进入RELEASE_TO_REFRESH的状态
                                if ((tempY - startY) / RATIO >= headContentHeight)
                                {
                                	log("ACTION_MOVE=======state = RELEASE_To_REFRESH");;
                                    state = RELEASE_To_REFRESH;
                                    isBack = true;
                                    changeHeaderViewByState();
                                    
                                    // Log.v(TAG, "由done或者下拉刷新状态转变到松开刷新");
                                }
                                // 上推到顶了
                                else if (tempY - startY <= 0)
                                {
                                	log("line 352 state = DONE");
                                    state = DONE;
                                    changeHeaderViewByState();
                                    
                                }
                            }
                            
                            // done状态下
                            if (state == DONE)
                            {
                                if (tempY - startY > 0)
                                {
                                	log("ACTION_MOVE=======state=PULL_To_REFRESH");
                                    state = PULL_To_REFRESH;
                                    changeHeaderViewByState();
                                }
                            }
                            
                            if (isfirstIdex)
                            {
                            	log("line377=============isfirstIdex");
                                // 更新headView的size
                                if (state == PULL_To_REFRESH)
                                {
                                	
                                    headView.setPadding(0, -1 * headContentHeight + (tempY - startY) / RATIO, 0, 0);
                                }
                                
                                // 更新headView的paddingTop
                                if (state == RELEASE_To_REFRESH)
                                {
                                    headView.setPadding(0, (tempY - startY) / RATIO - headContentHeight, 0, 0);
                                }
                            }
                            
                        }
                    }
                    
                    // 上拉
                    // else if (false)
                    else if (tempY < startY)
                    {
                        headorfooter = true;
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
                                
                                // setSelection(getCount() - 1);
                                
                                // 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
                                if (((startY - tempY) / RATIO < footerContentHeight) && (startY - tempY) > 0)
                                {
                                	log("line419====state = PULL_To_REFRESH");
                                    state = PULL_To_REFRESH;
                                    changeFooterViewByState();
                                    
                                    // Log.v(TAG, "由松开刷新状态转变到下拉刷新状态");
                                }
                                // 一下子推到顶了
                                else if (startY - tempY <= 0)
                                {
                                	log("ACTION_MOVE======state=DONE");
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
                                if ((startY - tempY) / RATIO >= footerContentHeight)
                                {
                                	log("line449======state = RELEASE_To_REFRESH");
                                    state = RELEASE_To_REFRESH;
                                    isBack = true;
                                    changeFooterViewByState();
                                    
                                    // Log.v(TAG, "由done或者下拉刷新状态转变到松开刷新");
                                }
                                // 上推到顶了
                                else if (startY - tempY <= 0)
                                {
                                	log("LINE 453 state = DONE");
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
                            
                            if (isLastIndex)
                            {
                                // 更新footerView的size
                                if (state == PULL_To_REFRESH)
                                {
                                	log(String.format("state == PULL_To_REFRESH===%s",-1 * footerContentHeight + (startY - tempY) / RATIO));
                                //    footerView.setPadding(0, 0, 0, -1 * footerContentHeight + (startY - tempY) / RATIO);
                                }
                                
                                // 更新footerView的paddingTop
                                if (state == RELEASE_To_REFRESH)
                                {
                                	log(String.format("state == RELEASE_To_REFRESH===%s",(startY - tempY) / RATIO - footerContentHeight));
                                    //footerView.setPadding(0, 0, 0, (startY - tempY) / RATIO - footerContentHeight);
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
                fProgressBar.setVisibility(View.GONE);
                fTipsTextview.setVisibility(View.VISIBLE);
                
                fTipsTextview.setText("松开刷新");
                
                break;
            case PULL_To_REFRESH:
            	log("PULL_To_REFRESH");
                fProgressBar.setVisibility(View.GONE);
                fTipsTextview.setVisibility(View.VISIBLE);
                // 是由RELEASE_To_REFRESH状态转变来的
                if (isBack)
                {
                    isBack = false;
                    fTipsTextview.setText(tips2);
                }
                else
                {
                    fTipsTextview.setText(tips2);
                }
                break;
            
            case REFRESHING:
            	log("REFRESHING");
                footerView.setPadding(0, 0, 0, 0);
                fProgressBar.setVisibility(View.VISIBLE);
                fTipsTextview.setText(tips3);
                break;
            case DONE:
            	log("DONE");
            	footerView.setPadding(0, 0, 0, 0);
                fProgressBar.setVisibility(View.GONE);
                fTipsTextview.setText(tips2);
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
                arrowImageView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                tipsTextview.setVisibility(View.VISIBLE);
                lastUpdatedTextView.setVisibility(View.VISIBLE);
                
                arrowImageView.clearAnimation();
                arrowImageView.startAnimation(animation);
                
                tipsTextview.setText("松开刷新");
                
                // Log.v(TAG, "当前状态，松开刷新");
                break;
            case PULL_To_REFRESH:
                log("=======PULL_To_REFRESH====");
                progressBar.setVisibility(View.GONE);
                tipsTextview.setVisibility(View.VISIBLE);
                lastUpdatedTextView.setVisibility(View.VISIBLE);
                arrowImageView.clearAnimation();
                arrowImageView.setVisibility(View.VISIBLE);
                // 是由RELEASE_To_REFRESH状态转变来的
                if (isBack)
                {
                    isBack = false;
                    arrowImageView.clearAnimation();
                    arrowImageView.startAnimation(reverseAnimation);
                    
                    tipsTextview.setText(tips1);
                }
                else
                {
                    tipsTextview.setText(tips1);
                }
                // Log.v(TAG, "当前状态，下拉刷新");
                break;
            
            case REFRESHING:
                //Log.e("test", "REFRESHING");
            	log("=======REFRESHING====");
                headView.setPadding(0, 0, 0, 0);
                
                progressBar.setVisibility(View.VISIBLE);
                arrowImageView.clearAnimation();
                arrowImageView.setVisibility(View.GONE);
                tipsTextview.setText(tips3);
                lastUpdatedTextView.setVisibility(View.VISIBLE);
                
                // Log.v(TAG, "当前状态,tips3");
                break;
            case DONE:
                headView.setPadding(0, -1 * headContentHeight, 0, 0);
                
                progressBar.setVisibility(View.GONE);
                arrowImageView.clearAnimation();
                arrowImageView.setImageResource(R.drawable.ic_pulltorefresh_arrow);
                tipsTextview.setText(tips1);
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
		if(v==footerView){
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
