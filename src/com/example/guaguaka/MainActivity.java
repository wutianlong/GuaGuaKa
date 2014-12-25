package com.example.guaguaka;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.guaguaka.view.GuaGuaKa;
import com.example.guaguaka.view.GuaGuaKa.OnGuaGuaKaCompleteListener;
import com.imooc.guaguaka.R;

public class MainActivity extends Activity {
	GuaGuaKa mGuaGuaKa;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mGuaGuaKa = (GuaGuaKa) findViewById(R.id.id_guaguaka);
		mGuaGuaKa
				.setOnGuaGuaKaCompleteListener(new OnGuaGuaKaCompleteListener() {
					@Override
					public void complete() {
						Toast.makeText(getApplicationContext(), "刮奖结束",
								Toast.LENGTH_SHORT).show();
					}
				});

		// 代码动态设置内部文字
		// mGuaGuaKa.setText("Android新技能");
	}

}
