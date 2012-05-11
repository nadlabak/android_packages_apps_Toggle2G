/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mb.toggle2g.locale;

import com.mb.toggle2g.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class LocaleEditActivity extends Activity implements OnClickListener, OnCheckedChangeListener
{
	static final String INTENT_EXTRA_SETTING = "com.mb.Toggle2g.locale.setting";
	static final String INTENT_EXTRA_SETTING_NETWORK = "com.mb.Toggle2g.locale.setting.network";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.locale_edit);

		final String breadcrumbString = getIntent().getStringExtra(com.twofortyfouram.Intent.EXTRA_STRING_BREADCRUMB);
		if (breadcrumbString != null)
		{
			setTitle(String.format("%s%s%s", breadcrumbString, com.twofortyfouram.Intent.BREADCRUMB_SEPARATOR, getString(R.string.app_name)));
		}
		
		findViewById(R.id.ok).setOnClickListener(this);

		((RadioGroup)findViewById(R.id.settings)).setOnCheckedChangeListener(this);
		
		//findViewById(R.id.auto).setOnClickListener(this);
		//findViewById(R.id.set2g).setOnCheckedChangeListener(this);
		//findViewById(R.id.set3g).setOnCheckedChangeListener(this);
		
		//((LinearLayout) findViewById(R.id.frame)).setBackgroundDrawable(SharedResources.getDrawableResource(getPackageManager(), SharedResources.DRAWABLE_LOCALE_BORDER));

		if (savedInstanceState == null)
		{
			final Bundle forwardedBundle = getIntent().getBundleExtra(com.twofortyfouram.Intent.EXTRA_BUNDLE);
			/*
			 * the forwardedBundle would be null if this was a new setting
			 */
			long setting = -1;
			if (forwardedBundle != null)
			{
				setting = getIntent().getLongExtra(INTENT_EXTRA_SETTING, 0);

				if (setting == 1 )
				{
					//auto
					RadioButton rb = (RadioButton) findViewById(R.id.auto);
					rb.setChecked(true);
				}
				else if (setting == 2 )
				{
					//2g
					RadioButton rb = (RadioButton) findViewById(R.id.set2g);
					rb.setChecked(true);
				}
				else if (setting == 3 )
				{
					//3g
					RadioButton rb = (RadioButton) findViewById(R.id.set3g);
					rb.setChecked(true);
				}
				else if (setting == 4 )
				{
					//Set network
					RadioButton rb = (RadioButton) findViewById(R.id.set_network);
					rb.setChecked(true);
					
					int net = getIntent().getIntExtra(INTENT_EXTRA_SETTING_NETWORK, -1);
					((Spinner)findViewById(R.id.pick_network)).setSelection(net);
				}
			}
			findViewById(R.id.pick_network_label).setVisibility((setting == 4?View.VISIBLE:View.GONE) );
			findViewById(R.id.pick_network).setVisibility((setting == 4?View.VISIBLE:View.GONE));
		}
	}

	@Override
	public void onClick(View v)
	{
		Intent returnIntent = new Intent();
		final Bundle storeAndForwardExtras = new Bundle();
		
		RadioButton rb = (RadioButton) findViewById(R.id.auto);
		if ( rb.isChecked() )
		{
			returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BLURB, getString( R.string.tasker_auto ));
			storeAndForwardExtras.putLong(INTENT_EXTRA_SETTING, 1);
		}

		rb = (RadioButton) findViewById(R.id.set2g);
		if ( rb.isChecked() )
		{
			returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BLURB, getString( R.string.tasker_2g ));
			storeAndForwardExtras.putLong(INTENT_EXTRA_SETTING, 2);
		}

		rb = (RadioButton) findViewById(R.id.set3g);
		if ( rb.isChecked() )
		{
			returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BLURB, getString( R.string.tasker_3g ));
			storeAndForwardExtras.putLong(INTENT_EXTRA_SETTING, 3);
		}

		rb = (RadioButton) findViewById(R.id.set_network);
		if ( rb.isChecked() )
		{
			Spinner net = (Spinner) findViewById(R.id.pick_network);
			String item = (String) net.getSelectedItem();
			int pos = net.getSelectedItemPosition();

			returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BLURB, getString( R.string.tasker_network, item));
			storeAndForwardExtras.putLong(INTENT_EXTRA_SETTING, 4);
			storeAndForwardExtras.putInt(INTENT_EXTRA_SETTING_NETWORK, pos);
		}

		returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_BUNDLE, storeAndForwardExtras);

		setResult(RESULT_OK, returnIntent);
		super.finish();
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId)
	{
		boolean showNetwork = (checkedId == R.id.set_network );
		
		if ( showNetwork )
		{
			Spinner net = (Spinner) findViewById(R.id.pick_network);
			findViewById(R.id.ok).setEnabled( net.getSelectedItemPosition() >= 0  );	
		}
		else
		{
			findViewById(R.id.ok).setEnabled( true );	
		}

		findViewById(R.id.pick_network_label).setVisibility((showNetwork?View.VISIBLE:View.GONE) );
		findViewById(R.id.pick_network).setVisibility((showNetwork?View.VISIBLE:View.GONE));

	}
}
