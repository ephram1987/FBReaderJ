/*
 * Copyright (C) 2010-2013 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader;

import java.util.LinkedList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.*;
import android.view.*;

import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.xml.ZLStringMap;
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.android.util.PackageUtil;
import org.geometerplus.android.util.ViewUtil;

public class PluginListActivity extends ListActivity {
	private final ZLResource myResource = ZLResource.resource("plugins");

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setTitle(myResource.getValue());
		final PluginListAdapter adapter = new PluginListAdapter();
		setListAdapter(adapter);
		getListView().setOnItemClickListener(adapter);
		com.tomoon.sdk.Emulator.configure(getWindow());
	}

	private static class Plugin {
		final String Id;
		final String PackageName;

		Plugin(String id, String packageName) {
			Id = id;
			PackageName = packageName;
		}
	}

	private class Reader extends ZLXMLReaderAdapter {
		final PackageManager myPackageManager = getPackageManager();
		final List<Plugin> myPlugins;

		Reader(List<Plugin> plugins) {
			myPlugins = plugins;
		}

		@Override
		public boolean dontCacheAttributeValues() {
			return true;
		}

		@Override
		public boolean startElementHandler(String tag, ZLStringMap attributes) {
			if ("plugin".equals(tag)) {
				final String id = attributes.getValue("id");
				final String packageName = attributes.getValue("package");
				try {
      				myPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
				} catch (PackageManager.NameNotFoundException e) {
					myPlugins.add(new Plugin(id, packageName));
				}
			}
			return false;
		}
	}

	private class PluginListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
		private final List<Plugin> myPlugins = new LinkedList<Plugin>();

		PluginListAdapter() {
			new Reader(myPlugins).readQuietly(ZLFile.createFileByPath("plugins.xml"));
		}

		public final int getCount() {
			return myPlugins.size();
		}

		public final Plugin getItem(int position) {
			return myPlugins.get(position);
		}

		public final long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, final ViewGroup parent) {
			final View view = convertView != null
				? convertView
				: LayoutInflater.from(parent.getContext()).inflate(R.layout.plugin_item, parent, false);
			final TextView titleView = ViewUtil.findTextView(view, R.id.plugin_item_title);
			final TextView summaryView = ViewUtil.findTextView(view, R.id.plugin_item_summary);
			final Plugin plugin = getItem(position);
			final ZLResource resource = myResource.getResource(plugin.Id);
			titleView.setText(resource.getValue());
			summaryView.setText(resource.getResource("summary").getValue());
			return view;
		}

		public final void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
			runOnUiThread(new Runnable() {
				public void run() {
					finish();
					PackageUtil.installFromMarket(PluginListActivity.this, getItem(position).PackageName);
				}
			});
		}
	}
}
