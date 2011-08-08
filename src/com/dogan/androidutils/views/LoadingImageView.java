/*
 * @author Burcu Dogan
 */

package com.dogan.androidutils.views;

import java.io.IOException;
import java.net.MalformedURLException;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.util.AttributeSet;
import android.widget.ImageView;

// TODO: provide a DefaultDrawableCacheImpl
/**
 * LoadingImageView helps set an image url directly to 
 * an image view. It asynchronously downloads, caches and 
 * shows the image.
 */
public class LoadingImageView extends ImageView {

	private static final int COMPLETE = 0;
	private static final int FAILED = 1;
	
	private static DrawableCache mCache;
	private Drawable mDrawable;
	private DownloadHandler mDownloadHandler;
	
	
	/**
	 * Constructs a new loading image view.
	 *
	 * @param context the context
	 */
	public LoadingImageView(final Context context) {
		super(context);
	}

	/**
	 * If you will create an instance in layout XML, insert a "src"
	 * attribute to the LoadingImageView tag and set it to the URL
	 * of the remote image.
	 * E.g.: <com.dogan.androidutils.LoadingImageView src="http://.../images/...png" />
	 *
	 * @param context the context
	 * @param attrSet the attr set
	 */
	public LoadingImageView(final Context context, final AttributeSet attrSet) {
		super(context, attrSet);
		final String url = attrSet.getAttributeValue(null, "src");
		setImageUrl(url);
	}

	/**
	 * Constructs a new loading image view with a remote
	 * image url.
	 *
	 * @param context the Activity context
	 * @param url the Image URL you wish to load
	 */
	public LoadingImageView(final Context context, final String url) {
		super(context);
		setImageUrl(url);        
	}

	
	/**
	 * Download handler includes callback methods.
	 *
	 * @param downloadHandlerImpl the new download handler
	 */
	public void setDownloadHandler(DownloadHandler downloadHandlerImpl){
		this.mDownloadHandler = downloadHandlerImpl;
	}
	
	/**
	 * Sets a new cache instance, if no cache is presented
	 * caching wont perform.
	 *
	 * @param cache the new cache
	 */
	public void setCache(DrawableCache cache){
		mCache = cache;
	}

	/**
	 * Set's the view's drawable, this uses the internet to retrieve the image
	 * don't forget to add the correct permissions to your manifest.
	 *
	 * @param imageUrl the url of the image you wish to load
	 */
	public void setImageUrl(final String imageUrl) {
		mDrawable = null;
		new Thread(){
			public void run() {
				try {
					mDrawable = getDrawableFromUrl(imageUrl);
					handler.sendEmptyMessage(COMPLETE);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					if(mDownloadHandler != null){
						mDownloadHandler.onException(e);
					}
					handler.sendEmptyMessage(FAILED);
				} catch (IOException e) {
					e.printStackTrace();
					if(mDownloadHandler != null){
						mDownloadHandler.onException(e);
					}
					handler.sendEmptyMessage(FAILED);
				}
			};
		}.start();
	}

	/**
	 * Once the image is dowloaded this handler will call the
	 * downloadHandler you provided if exists, and set the image
	 * to the image view.
	 *  */
	private final Handler handler = new Handler(new Callback() {
		public boolean handleMessage(Message msg) {
			
			if(mDownloadHandler != null){
				mDownloadHandler.onDrawableDownloaded(mDrawable);
			}
			
			switch (msg.what) {
			case COMPLETE:
				setImageDrawable(mDrawable); break;
			}
			return true;
		}              
	});
	
	
	/**
	 * Gets the drawable with the given url from cache.
	 * If cache doesnt have the image, it returns null.
	 *
	 * @param url The url of the image
	 * @return drawable from the cache
	 */
	public static Drawable getFromCache(String url){
		if(mCache != null){
			return mCache.get(url);
		}
		return null;
	}
	
	/**
	 * Puts the drawable to the cache. If cache doesnt exist,
	 * it doesnt perform. If we recieve an OutOfMemoryException
	 * we clear the cache.
	 *
	 * @param url The url of the image
	 * @param drawable The drawable fetched from the url, cant be null
	 */
	public static void putToCache(String url, Drawable drawable){
		if(mCache != null && url != null && drawable != null){
			try {
				mCache.put(url, drawable);
			} catch(OutOfMemoryError e){ 
				// ignores the current put
				// TODO: may implement retry here              
				e.printStackTrace();
				mCache.clear();
			}
		}
	}

	/**
	 * Creates a new drawable from the given url. Checks the
	 * cache before hitting to url. If cache doesnt have the
	 * image already, it's fetched from the URL. Once IO
	 * is completed downloaded image is put to cache.
	 *
	 * @param url the url of the image
	 * @return a drawable
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private static Drawable getDrawableFromUrl(final String url) throws IOException, MalformedURLException {

		if(url == null){
			return null;
		}
		
		Drawable image = getFromCache(url);
		if(image == null){
			image = Drawable.createFromStream(((java.io.InputStream)new java.net.URL(url).getContent()), "name");
			putToCache(url, image);
		}
		return image;
	}
	
	
	/**
	 * DrawableCache interface forces you to implement a 
	 * drawable cache gets and puts images with a url.
	 */
	public interface DrawableCache {
		
		/**
		 * Gets a drawable by a url.
		 *
		 * @param url The image's url.
		 * @return the cached drawable if exists.
		 */
		Drawable get(String url);
		
		/**
		 * Puts a drawable to the cache.
		 *
		 * @param url The image's url
		 * @param imageDrawable The downloaded drawable.
		 */
		void put(String url, Drawable imageDrawable);
		
		/**
		 * Performs a full clean on the cache.
		 */
		void clear();
	}
	
	/**
	 * If you would like to notified when image download 
	 * is either finished or failed, you can set a DownloadHandler
	 * to subscribe onDrawableDownloaded and onException
	 * states.
	 */
	public interface DownloadHandler {
		
		/**
		 * Will be called once a drawable is downloaded
		 * You may retrieve the drawable, manipulate it
		 * and set it again.
		 *
		 * @param drawable The downloaded drawable.
		 */
		void onDrawableDownloaded(Drawable drawable);
		
		/**
		 * On exception.
		 *
		 * @param e The thrown exception during the download.
		 */
		void onException(Exception e);
	}
}