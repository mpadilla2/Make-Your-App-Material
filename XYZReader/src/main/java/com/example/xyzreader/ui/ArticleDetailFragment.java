package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final String TAG = "ArticleDetailFragment";
    private static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    private CollapsingToolbarLayout mCollapsingToolbar;
    private ImageView mPhotoView;
    private Date mArticlePublishedDate;
    private String mArticleAuthor;
    private String mArticleTitle;
    private Typeface mRobotoRegular;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.getDefault());
    // Use default locale format
    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private int mDefaultColor = 0xDD4B92C4;
    private int mVibrantColor;
    private int mDarkVibrantColor;
    private int mLightVibrantColor;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }


    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Objects.requireNonNull(getArguments()).containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mCollapsingToolbar = mRootView.findViewById(R.id.fragment_article_detail_collapsing_toolbar);
        // Reference: https://goo.gl/n33LTr
        Typeface mRobotoBlack = Typeface.createFromAsset(Objects.requireNonNull(getActivity()).getAssets(), "font/roboto_black.ttf");
        mRobotoRegular = Typeface.createFromAsset(getActivity().getAssets(), "font/roboto_regular.ttf");

        mCollapsingToolbar.setExpandedTitleTypeface(mRobotoBlack);
        mCollapsingToolbar.setCollapsedTitleTypeface(mRobotoRegular);

        mPhotoView = mRootView.findViewById(R.id.thumbnail);

        FloatingActionButton mFloatingActionButton = mRootView.findViewById(R.id.fragment_article_detail_share_fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(mArticleTitle + " by " + mArticleAuthor + ", published on "
                                + DateUtils.getRelativeTimeSpanString(
                                mArticlePublishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString())
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        Toolbar mToolbar = mRootView.findViewById(R.id.fragment_article_detail_toolbar);
        // Reference: https://stackoverflow.com/q/42502519/10151438
        ((AppCompatActivity)getActivity()).setSupportActionBar(mToolbar);
        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setDisplayShowHomeEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        return mRootView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }


    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }


    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView bylineView = mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = mRootView.findViewById(R.id.article_body);

        bylineView.setTypeface(mRobotoRegular);
        bodyView.setTypeface(mRobotoRegular);

        if (mCursor != null) {
            mArticlePublishedDate = parsePublishedDate();
            mArticleAuthor = mCursor.getString(ArticleLoader.Query.AUTHOR);
            mArticleTitle = mCursor.getString(ArticleLoader.Query.TITLE);
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mCollapsingToolbar.setTitle(mArticleTitle);
            bylineView.setVisibility(View.VISIBLE);
            if (!mArticlePublishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                mArticlePublishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by " + mArticleAuthor));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(mArticlePublishedDate) + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));

            }

            // Requirements:
            // Examined html and determined that \r\n should be a space and \r\n\r\n should be newline based on textual context.
            // 1. Use negative lookbehind and negative lookahead to isolate the pattern that should be a space first.
            //      So the space pattern cannot have \r\n on either side of \r\n. It must be \r\n by itself.
            // 2. Use negative lookbehind and positive lookahead to isolate the \r\n\r\n pattern to replace with one break.
            // 3. Finally, replace \n with break.
            // Reference: https://www.regular-expressions.info/lookaround.html
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)
                    .substring(0,1000)
                    .replaceAll("(?<!\\r\\n)(\\r\\n)(?!\\r\\n)", " ")
                    .replaceAll("(?<!\\r\\n)(\\r\\n)(?=\\r\\n)|(\\n)", "<br />")));

            // Reference: https://android.jlelse.eu/dynamic-colors-with-glide-library-and-android-palette-5be407049d97
            Glide.with(this)
                    .asBitmap()
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .apply(new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_image_loading)
                            .error(R.drawable.ic_broken_image))
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            if (resource != null) {
                                Palette.Builder builder = Palette.from(resource);

                                // Use PaletteAsyncListener to move generation off MainThread
                                builder.generate(new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(@NonNull Palette palette) {
                                        mVibrantColor = palette.getVibrantColor(mDefaultColor);
                                        mDarkVibrantColor = palette.getDarkVibrantColor(mDefaultColor);

                                        mRootView.findViewById(R.id.meta_bar).setBackgroundColor(mVibrantColor);
                                        mCollapsingToolbar.setBackgroundColor(mVibrantColor);
                                        //mCollapsingToolbar.setStatusBarScrimColor(mDarkVibrantColor);
                                        mCollapsingToolbar.setContentScrimColor(mVibrantColor);
                                    }
                                });
                            }
                            return false;
                        }
                    })
                    .into(mPhotoView);
        } else {
            bylineView.setVisibility(View.INVISIBLE);
            bodyView.setText(R.string.bodyViewText);
        }
    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }


    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }


    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mCursor = null;
        bindViews();
    }


}
