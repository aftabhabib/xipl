/*
 * Copyright 2017 Isaac Pateau
 *
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

package com.zaclimon.xipl.ui.main;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.PageRow;
import android.support.v17.leanback.widget.Row;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;

import com.zaclimon.xipl.R;
import com.zaclimon.xipl.ui.search.ProviderSearchActivity;

import java.util.Map;


/**
 * Main fragment used for the Android TV variant of the application
 *
 * @author zaclimon
 * Creation date: 20/06/17
 */

public abstract class ProviderTvFragment extends BrowseFragment {

    private ArrayObjectAdapter mRowsAdapter;
    private SparseArray<RowsFragment> mFragmentSparseArray;

    /**
     * Gets the application name
     *
     * @return the application name
     */
    protected abstract String getAppName();

    /**
     * Returns a map containing one or multiple {@link RowsFragment} alongside their respective
     * header title(s).
     *
     * @return the list of RowsFragment mapped by title
     */
    protected abstract Map<String, RowsFragment> getFragmentMap();

    protected abstract Class<? extends ProviderSearchActivity> getSearchActivity();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mFragmentSparseArray = new SparseArray<>();
            setupUI();
            showRows();
            getMainFragmentRegistry().registerFragment(PageRow.class, new TvFragmentFactory());

            if (getSearchActivity() != null) {
                TypedValue value = new TypedValue();
                TypedArray array = getActivity().obtainStyledAttributes(value.data, new int[] {android.R.attr.colorAccent});
                setSearchAffordanceColor(array.getColor(0, 0));
                setOnSearchClickedListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), getSearchActivity());
                        startActivity(intent);
                    }
                });
                array.recycle();
            }
        }
    }

    /**
     * Sets the user interface (UI) for the Fragment.
     */
    private void setupUI() {
        setTitle(getAppName());
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
    }

    /**
     * Shows the different rows of the Fragment.
     */
    private void showRows() {
        Map<String, RowsFragment> tempMap = getFragmentMap();
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        int i = 0;

        for (Map.Entry<String, RowsFragment> entry : tempMap.entrySet()) {
            HeaderItem header = new HeaderItem(i, entry.getKey());
            PageRow row = new PageRow(header);
            mRowsAdapter.add(row);
            mFragmentSparseArray.append(i, entry.getValue());
            i++;
        }

        setAdapter(mRowsAdapter);
    }

    /**
     * Private class acting as a Fragment factory in order to implement custom fragments
     * into Leanback.
     *
     * @author zaclimon
     * Creation date: 02/07/17
     */
    private class TvFragmentFactory extends FragmentFactory {

        @Override
        public Fragment createFragment(Object row) {
            Row tempRow = (Row) row;
            Fragment fragment = mFragmentSparseArray.get((int) tempRow.getId());

            if (fragment != null) {
                return (fragment);
            } else {
                throw new IllegalArgumentException("Invalid row: " + row);
            }
        }

    }
}
