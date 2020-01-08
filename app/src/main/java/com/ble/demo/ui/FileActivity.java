package com.ble.demo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ble.demo.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 加载本地OAD文件
 */
public class FileActivity extends AppCompatActivity {
	static final String TAG = "FileActivity";

	public final static String EXTRA_FILE_PATH = "com.ble.demo.ui.FileActivity.EXTRA_FILE_PATH";

	private final File mDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	private FileAdapter mFileAdapter;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ListView listView = new ListView(this);
		listView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		setContentView(listView);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		List<String> fileList = initFileList();
		mFileAdapter = new FileAdapter(fileList);
		listView.setAdapter(mFileAdapter);
		listView.setOnItemClickListener(mOnItemClickListener);
	}

	private List<String> initFileList() {
		List<String> fileList = new ArrayList<String>();

		if (mDir.exists()) {
			getSupportActionBar().setTitle(mDir.getAbsolutePath());
			FilenameFilter textFilter = new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					String lowercaseName = name.toLowerCase(Locale.ROOT);
					return lowercaseName.endsWith(".bin");
				}
			};

			File[] files = mDir.listFiles(textFilter);

			if (files != null) {
				for (File file : files) {
					if (!file.isDirectory()) {
						fileList.add(file.getName());
					}
				}
			}

			if (fileList.size() == 0) {
				Toast.makeText(this, "No OAD images available", Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, mDir.getAbsolutePath() + " does not exist", Toast.LENGTH_LONG).show();
		}
		return fileList;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	private final AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Intent intent = new Intent();
			intent.putExtra(EXTRA_FILE_PATH, mDir.getAbsolutePath() + File.separator + mFileAdapter.getItem(position));
			setResult(RESULT_OK, intent);
			finish();
		}
	};

	private class FileAdapter extends BaseAdapter {
		List<String> mFiles;

		public FileAdapter(List<String> files) {
			mFiles = files;
		}

		@Override
		public int getCount() {
			return mFiles.size();
		}

		@Override
		public String getItem(int pos) {
			return mFiles.get(pos);
		}

		@Override
		public long getItemId(int pos) {
			return pos;
		}

		@Override
		public View getView(int pos, View view, ViewGroup parent) {
			if (view == null) {
				TextView twName = new TextView(FileActivity.this);
				int height = getDimen(R.dimen.file_list_item_height);
				AbsListView.LayoutParams params = new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, height);
				twName.setLayoutParams(params);
				twName.setGravity(Gravity.CENTER_VERTICAL);
				twName.setPadding(getDimen(R.dimen.activity_horizontal_margin), 0, 0, 0);
				view = twName;
			}

			((TextView) view).setText(mFiles.get(pos));
			return view;
		}
	}

	private int getDimen(int id) {
		return getResources().getDimensionPixelSize(id);
	}

}