package com.example.wbd;

import com.example.wbd.PullListView.OnRefreshListener;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnRefreshListener {
	private String tag = getClass().getSimpleName();
	
	private Context context;

	private ImgTxtAdapter adapter;

	PullListView listview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		context = this;

		setContentView(R.layout.activity_main);

		listview = (PullListView) findViewById(R.id.gridview);

		adapter = new ImgTxtAdapter(context);

		listview.setAdapter(adapter);
		
		listview.setonRefreshListener(MainActivity.this);

		for (int i = 0; i < 10; i++) {
			ImgTxtBean b = new ImgTxtBean();
			b.setResid(R.drawable.ic_launcher);
			b.setText("item" + (i + 1));
			adapter.addObject(b);
		}
		
	}

	@Override
	public void onRefresh() {
		log("onRefresh");
		//Toast.makeText(MainActivity.this, "top", Toast.LENGTH_SHORT).show();
		mHandler.postDelayed(taskFinish, 1000);
	}

	Handler mHandler = new Handler();
	
	Runnable taskFinish = new Runnable(){
		@Override
		public void run() {
			// TODO Auto-generated method stub
			listview.onRefreshComplete();
			Toast.makeText(MainActivity.this, "新增了5条记录",200).show();
			

			int startIndex = adapter.getCount();
			for (int i = startIndex; i < startIndex + 5; i++) {
				ImgTxtBean b = new ImgTxtBean();
				b.setResid(R.drawable.ic_launcher);
				b.setText("item" + (i + 1));
				adapter.addObject(b);
			}
		}
	};
	@Override
	public void onMore() {
		log("onMore");
		mHandler.postDelayed(taskFinish, 1000);
	}

	@Override
	public void onDown() {
		//log("downing");
	}

	@Override
	public void onUp() {
		//log("uping");
	}
	void log(String msg){
		Log.d(tag, msg);
	}
}