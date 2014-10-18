package company.eugene.blogreaderrevised;


import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class MainListActivity extends ListActivity {

    // Recall that protected means only available inside this class, subclasses and packages
    // we are setting the array from the strings.xml resource

    public static final int NUMBER_OF_POSTS = 20;
    public static final String TAG = MainListActivity.class.getSimpleName();
    protected JSONObject mBlogData;
    protected ProgressBar mProgressBar;
    private final String KEY_TITLE = "title";
    private final String KEY_AUTHOR = "author";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);


        if (isNetworkAvailable()) {
            mProgressBar.setVisibility(View.VISIBLE);
            GetBlogPostsTask getBlogPostsTask = new GetBlogPostsTask();
            getBlogPostsTask.execute();
        }
        else {
            Toast.makeText(this, "Network is unavailable", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        JSONArray jsonPosts = null;
        try {
            jsonPosts = mBlogData.getJSONArray("posts");
            JSONObject jsonPost = jsonPosts.getJSONObject(position);
            String blogURL = jsonPost.getString("url");

            Intent intent = new Intent(this, BlogWebViewActivity.class);
            intent.setData(Uri.parse(blogURL));
            startActivity(intent);
        } catch (JSONException e) {
            logException(e);
        }

    }

    private void logException(JSONException e) {
        Log.e(TAG, "Exception caught!", e);
    }


    private boolean isNetworkAvailable() {
        // getSystemService returns generic Object, so have to typecast
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isAvailable = false;
        if(networkInfo !=null && networkInfo.isConnected()) {
            isAvailable = true;
        }

        return isAvailable;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleBlogResponse() {
        mProgressBar.setVisibility(View.INVISIBLE);
        
        if(mBlogData == null) {
            updateDisplayForError();

        }
        else {
            try {
               JSONArray jsonPosts = mBlogData.getJSONArray("posts");
                ArrayList<HashMap<String, String>> blogPosts =
                        new ArrayList<HashMap<String, String>>();

                for (int i = 0; i< jsonPosts.length();i++) {
                    JSONObject post = jsonPosts.getJSONObject(i);
                    String title = post.getString(KEY_TITLE);
                    title = Html.fromHtml(title).toString();

                    String author = post.getString(KEY_AUTHOR);
                    author = Html.fromHtml(author).toString();

                    HashMap<String, String> blogPost = new HashMap<String, String>();
                    blogPost.put(KEY_TITLE, title);
                    blogPost.put(KEY_AUTHOR, author);

                    blogPosts.add(blogPost);

                }

               String[] keys = {KEY_TITLE, KEY_AUTHOR};
                int[] ids = {android.R.id.text1, android.R.id.text2};
                SimpleAdapter adapter = new SimpleAdapter(this, blogPosts,
                        android.R.layout.simple_expandable_list_item_2,
                        keys, ids);
                setListAdapter(adapter);
            } catch (JSONException e) {
                logException(e);
            }
        }
    }

    private void updateDisplayForError() {
        // Builder is a nested class in AlertDialog
        // We can access the nested class because its public
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.error_title));
        builder.setMessage(getString(R.string.error_message));
        // adding null for the positive button will just close the dialog and do nothing
        builder.setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();

        TextView emptyTextView = (TextView) getListView().getEmptyView();
        emptyTextView.setText(getString(R.string.no_data));
    }

    /*
    AsyncTask has three arguments:
    1. Params: The type of parameters sent to task upon execution
    2. Progress: The type of progress units sent during the background computation
    3. Result: The type of result of the background computation
     */
    private class GetBlogPostsTask extends AsyncTask<Object, Void, JSONObject> {
        // this is the method that will be called to do in Background.
        @Override
        protected JSONObject doInBackground(Object[] objects) {
            // Want to initialize with -1 so we can tell if something is wrong with our code
            int responseCode = -1;
            JSONObject jsonResponse = null;
            try {
                URL blogfeedURL = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count=" + NUMBER_OF_POSTS);

                // openConnection() returns an object of type URLConnection, so have to typecast.
                HttpURLConnection connection = (HttpURLConnection) blogfeedURL.openConnection();
                connection.connect();

                // responseCode is an indication of whether the connection with the URL was successful or not
                responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    // reads the input stream
                    Reader reader = new InputStreamReader(inputStream);
                    int contentLength = connection.getContentLength();
                    char[] charArray = new char[contentLength];
                    reader.read(charArray);
                    String responseData = new String(charArray);

                    // JSON Object from the JSON response data
                    jsonResponse = new JSONObject(responseData);
                                    }
                else {
                    Log.i(TAG, "Unuccesful HTTP Response Code: " + responseCode);
                }
                Log.i(TAG, "Code: " + responseCode);



            } catch (MalformedURLException e) {
                Log.e(TAG, "Exception caught: ", e);
            } catch (IOException e) {
                Log.e(TAG, "Exception caught: ", e);
            } catch (Exception e) {
                // this is to catch any exceptions we did not see.  Technically speaking, we should just use this as the only catch block?
                Log.e(TAG, "Exception caught: ", e);
            }

            return jsonResponse;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            mBlogData = result;
            handleBlogResponse();
        }
    }

    
}
