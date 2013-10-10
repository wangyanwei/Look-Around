package com.geniusgithub.lookaround.test;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.framework.utils.UIHandler;

import com.geniusgithub.lookaround.R;
import com.geniusgithub.lookaround.util.CommonLog;
import com.geniusgithub.lookaround.util.LogFactory;
import com.geniusgithub.lookaround.weibo.sdk.ShareCore;
import com.geniusgithub.lookaround.weibo.sdk.ShareItem;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler.Callback;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TestShareActivity extends Activity implements Callback , TextWatcher,
												OnClickListener, PlatformActionListener{

	private static final CommonLog log = LogFactory.createLog();
	
	private static final int MSG_TOAST = 1;
	private static final int MSG_ACTION_CCALLBACK = 2;
	private static final int MSG_CANCEL_NOTIFY = 3;
	
	
	private Button mBtnBack;
	private Button mBtnShare;
	private Button mBtnCancelImage;
	private ImageView mIVShareImage;
	
	private EditText mETContent;
	private TextView mTVTarget;
	private TextView mTVLive;
	
	private int notifyIcon;
	private String notifyTitle;
	private String sharePath;
	
	
	private Platform mPlatform;
	private HashMap<String, Object> reqMap;
	
	private View phoneFrameView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_share_layout);
		setupViews();
		initData();
	}

	@Override
	protected void onDestroy() {
		
		ShareSDK.stopSDK(this);
		
		super.onDestroy();
	}

	
	private void setupViews(){
		ShareSDK.initSDK(this);
		
		setNotification(R.drawable.logo_icon,"Look Around");
		
		mBtnBack = (Button) findViewById(R.id.btn_back);
		mBtnShare = (Button) findViewById(R.id.btn_right);
		mBtnCancelImage = (Button) findViewById(R.id.btn_cancelimage);
		mIVShareImage = (ImageView) findViewById(R.id.iv_pic);
		mBtnBack.setOnClickListener(this);
		mBtnShare.setOnClickListener(this);
		mBtnCancelImage.setOnClickListener(this);
		
		mETContent = (EditText) findViewById(R.id.et_content);
		mTVTarget = (TextView) findViewById(R.id.tv_target);
		mETContent.addTextChangedListener(this);
		mTVLive = (TextView) findViewById(R.id.tv_live);
		
		phoneFrameView = findViewById(R.id.fl_phoneframe);
	}
	
	private void initData(){
	
		reqMap = ShareItem.reqMap;
		mPlatform = ShareSDK.getPlatform(this, (String) reqMap.get("platform"));
		Object object = reqMap.get("text");
		if (object != null){
			String value = (String) object;
			mETContent.setText(value);
			mETContent.setSelection(value.length());
		}
		
		sharePath = ShareItem.getShareImagePath();
		log.e("sharePath = " + sharePath);
		if (sharePath == null){
			showShareImage(false);
		}else{
			Bitmap bitmap = BitmapFactory.decodeFile(sharePath);
			if (bitmap != null){
				mIVShareImage.setImageBitmap(bitmap);
			}
			
		}
	
		mPlatform.setPlatformActionListener(new PlatformActionListener() {
			
			@Override
			public void onError(Platform arg0, int arg1, Throwable arg2) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onComplete(Platform platform, int action, HashMap<String, Object> map) {
				
				String name = (String) map.get("name");
				if (name == null){
					name = (String) map.get("nickname");
				}
				log.e("get user info --> onComplete \nPlatform = " + platform.getName() + ",  name = " +  name);
				if (name != null){
					updateTarget(name);
				}
				
			}
			
			@Override
			public void onCancel(Platform arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
		});
		mPlatform.showUser(null);
	}
	

	public void showShareImage(boolean flag){
		if (!flag){
			phoneFrameView.setVisibility(View.GONE);
			mBtnCancelImage.setVisibility(View.GONE);
		}else{
			phoneFrameView.setVisibility(View.VISIBLE);
			mBtnCancelImage.setVisibility(View.VISIBLE);
		}
	}
	
	/** 分享时Notification的图标和文字 */
	public void setNotification(int icon, String title) {
		notifyIcon = icon;
		notifyTitle = title;
	}
	
	private void updateTarget(final String name){
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
			mTVTarget.setText(name);
				
			}
		});
	}
	
	/** 执行分享 */
	public void share(Platform plat, HashMap<String, Object> data) {
		boolean started = false;
	
			
			String name = plat.getName();
			boolean isWechat = "WechatMoments".equals(name) || "Wechat".equals(name);
			if (isWechat && !plat.isValid()) {
				Message msg = new Message();
				msg.what = MSG_TOAST;
				msg.obj = getString(R.string.wechat_client_inavailable);
				UIHandler.sendMessage(msg, this);
				return ;
			}

			boolean isQQ = "QQ".equals(name);
			if (isQQ && !plat.isValid()) {
				Message msg = new Message();
				msg.what = MSG_TOAST;
				msg.obj = getString(R.string.qq_client_inavailable);
				UIHandler.sendMessage(msg, this);
				return ;
			}

			int shareType = Platform.SHARE_TEXT;
			String imagePath = String.valueOf(data.get("imagePath"));
			if (imagePath != null && (new File(imagePath)).exists()) {
				shareType = Platform.SHARE_IMAGE;
				if (data.containsKey("url") && !TextUtils.isEmpty(data.get("url").toString())) {
					shareType = Platform.SHARE_WEBPAGE;
				}
			}
			else {
				String imageUrl = String.valueOf(data.get("imageUrl"));
				if (imageUrl != null) {
					shareType = Platform.SHARE_IMAGE;
					if (data.containsKey("url") && !TextUtils.isEmpty(data.get("url").toString())) {
						shareType = Platform.SHARE_WEBPAGE;
					}
				}
			}
			data.put("shareType", shareType);

			if (!started) {
				started = true;
				showNotification(2000, getString(R.string.sharing));
				finish();
			}
			mPlatform.setPlatformActionListener(this);
			ShareCore shareCore = new ShareCore();
			shareCore.share(plat, data);
	
	}
	
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_back:
			finish();
			break;
		case R.id.btn_right:
			share(mPlatform, reqMap);
			break;
		case R.id.btn_cancelimage:
			break;
		}
	}
	
	
	@Override
	public boolean handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_TOAST: {
				String text = String.valueOf(msg.obj);
				Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
			}
			break;
			case MSG_ACTION_CCALLBACK: {
				switch (msg.arg1) {
					case 1: { // 成功
						showNotification(2000, getString(R.string.share_completed));
					}
					break;
					case 2: { // 失败
						String expName = msg.obj.getClass().getSimpleName();
						if ("WechatClientNotExistException".equals(expName)
								|| "WechatTimelineNotSupportedException".equals(expName)) {
							showNotification(2000, getString(R.string.wechat_client_inavailable));
						}
						else if ("GooglePlusClientNotExistException".equals(expName)) {
							showNotification(2000, getString(R.string.google_plus_client_inavailable));
						}
						else if ("QQClientNotExistException".equals(expName)) {
							showNotification(2000, getString(R.string.qq_client_inavailable));
						}
						else {
							showNotification(2000, getString(R.string.share_failed));
						}
					}
					break;
					case 3: { // 取消
						showNotification(2000, getString(R.string.share_canceled));
					}
					break;
				}
			}
			break;
			case MSG_CANCEL_NOTIFY: {
				NotificationManager nm = (NotificationManager) msg.obj;
				if (nm != null) {
					nm.cancel(msg.arg1);
				}
			}
			break;
		}
		return false;
	}
		
	// 在状态栏提示分享操作
	private void showNotification(long cancelTime, String text) {
				try {
					NotificationManager nm = (NotificationManager) 
							getSystemService(Context.NOTIFICATION_SERVICE);
					final int id = Integer.MAX_VALUE / 13 + 1;
					nm.cancel(id);

					long when = System.currentTimeMillis();
					Notification notification = new Notification(notifyIcon, text, when);
					PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(), 0);
					notification.setLatestEventInfo(this, notifyTitle, text, pi);
					notification.flags = Notification.FLAG_AUTO_CANCEL;
					nm.notify(id, notification);

					if (cancelTime > 0) {
						Message msg = new Message();
						msg.what = MSG_CANCEL_NOTIFY;
						msg.obj = nm;
						msg.arg1 = id;
						UIHandler.sendMessageDelayed(msg, cancelTime, this);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
		}

	public void onComplete(Platform platform, int action,
			HashMap<String, Object> res) {
		log.e("onComplete Platform = " + platform.getName() + ", action = " + action);
		Message msg = new Message();
		msg.what = MSG_ACTION_CCALLBACK;
		msg.arg1 = 1;
		msg.arg2 = action;
		msg.obj = platform;
		UIHandler.sendMessage(msg, this);
	}

	public void onError(Platform platform, int action, Throwable t) {
		t.printStackTrace();
		log.e("onError Platform = " + platform.getName() + ", action = " + action);
		Message msg = new Message();
		msg.what = MSG_ACTION_CCALLBACK;
		msg.arg1 = 2;
		msg.arg2 = action;
		msg.obj = t;
		UIHandler.sendMessage(msg, this);

		// 分享失败的统计
		ShareSDK.logDemoEvent(4, platform);
	}

	public void onCancel(Platform platform, int action) {
		log.e("onCancel Platform = " + platform.getName() + ", action = " + action);
		Message msg = new Message();
		msg.what = MSG_ACTION_CCALLBACK;
		msg.arg1 = 3;
		msg.arg2 = action;
		msg.obj = platform;
		UIHandler.sendMessage(msg, this);
	}

	@Override
	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		int remain = 420 - mETContent.length();
		mTVLive.setText("您还可以输入" + String.valueOf(remain) + "字");
		mTVLive.setTextColor(remain > 0 ? 0xffcfcfcf : 0xffff0000);
	}


}
