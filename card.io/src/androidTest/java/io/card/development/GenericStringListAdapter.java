package io.card.development;

/* GenericStringListAdapter.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * TODO document this class
 */
public class GenericStringListAdapter extends ArrayAdapter<String> {
    protected Activity m_activity;
    public List<String> m_list;

    public GenericStringListAdapter(Activity activity, int resource, int textViewResourceId,
                                    List<String> x) {
        super(activity, resource, textViewResourceId, x);
        m_activity = activity;
        m_list = x;
    }

    /**
     * this is for the popup item views
     */
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = m_activity.getLayoutInflater().inflate(R.layout.generic_list_item, null);
        }

        v.setTag(position);

        String s = m_list.get(position);
        ((TextView) v.findViewById(R.id.text)).setText(s);

        return v;
    }

    /**
     * this is for the main non-popped up view
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = m_activity.getLayoutInflater().inflate(R.layout.generic_spinner, null);
        }

        v.setTag(position);

        String s = m_list.get(position);
        ((TextView) v.findViewById(R.id.text)).setText(s);
        return v;
    }

    public int getIndexForName(String name) {
        int found = m_list.indexOf(name);
        if (0 > found || found > m_list.size()) {
            throw new IllegalArgumentException("Could not find '" + name + "' in list");
        }
        return found;
    }

}
