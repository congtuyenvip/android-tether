package og.android.tether;

import og.android.tether.R;

import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetManager;
import android.app.PendingIntent;
import android.widget.RemoteViews;
import android.view.View;
import android.util.Log;
import android.os.AsyncTask;
import android.os.Handler;


public class WidgetProvider extends AppWidgetProvider {

	public static final String MSG_TAG = "TETHER -> WidgetProvider";
    public static final StateTracker stateTracker = new StateTracker();
    static final ComponentName THIS_APPWIDGET = new ComponentName("og.android.tether", "og.android.tether.WidgetProvider");
    Context ctx;
    static Handler animateHandler = new Handler();
    static WidgetAnimator widgetAnimator = new WidgetAnimator();
    static int FRAME_DELAY = 300;
    
    static RemoteViews buildUpdate(Context context) {
        
        Intent intent = new Intent(context, WidgetProvider.class);
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        
        // Get the layout for the App Widget and attach an on-click listener
        // to the button
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_provider_layout);
        views.setOnClickPendingIntent(R.id.button, pendingIntent);
        updateWidgetButtons(views, context);
        
        return views;
    }
    
    public static void updateWidget(Context context) {
    		RemoteViews views = buildUpdate(context);
    		AppWidgetManager.getInstance(context).updateAppWidget(THIS_APPWIDGET, views);
    }
    
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    		final int N = appWidgetIds.length;
        RemoteViews views = buildUpdate(context);
        
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
           
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        ctx = context;
    }

    public void onReceive(Context context, Intent intent) {    		
    		super.onReceive(context, intent);
    		Log.d(MSG_TAG, "onReceive: " + intent );
    		if(TetherService.singleton != null)
    			Log.d(MSG_TAG, "getState(): "+TetherService.singleton.getState());
    		
    		if(intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
    			stateTracker.sendBroadcastChange(context);
    		} else if(intent.getAction().equals(TetherService.INTENT_STATE)) {
    			int stateArg = intent.getIntExtra("state", TetherService.MANAGE_STOPPED);
    			stateTracker.currentState = stateArg;
    			updateWidget(context);
    			Log.d(MSG_TAG, "currentState: "+stateArg);
    		}

    }
    
    public static void updateWidgetButtons(RemoteViews remoteViews, Context context) {
    		switch(stateTracker.currentState) {
    		case TetherService.STATE_RUNNING :
    			Log.d("!!!WidgetAnimator!!!", " view: " + remoteViews);
    			animateHandler.removeCallbacks(widgetAnimator);
    			remoteViews.setImageViewResource(R.id.button, R.drawable.widgeton);
    			break;
    		case TetherService.STATE_IDLE :
    			Log.d("!!!WidgetAnimator!!!", " view: " + remoteViews);
    			animateHandler.removeCallbacks(widgetAnimator);
    			remoteViews.setImageViewResource(R.id.button, R.drawable.widgetoff);
    			break;
    		default :
    			Log.d("!!!WidgetAnimator!!!", " view: " + remoteViews);
    			remoteViews.setImageViewResource(R.id.button, R.drawable.widgetwait);
    			widgetAnimator.views = remoteViews;
    			widgetAnimator.context = context;
    			animateHandler.removeCallbacks(widgetAnimator);
    			animateHandler.postDelayed(widgetAnimator, FRAME_DELAY);
    			//remoteViews.setImageViewResource(R.id.button, R.drawable.widgetwait);
    			break;
    		}
    }
    		 	
    public void launchApp(View v)
    {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        ctx.startActivity(i);
    }
}


class StateTracker {
	
	int currentState;
	boolean isChanging = false;
		
	public StateTracker() {
		if(TetherService.singleton != null)
			currentState = TetherService.singleton.getState();
		else
			currentState = TetherService.STATE_IDLE;
	}
	
	void changeState()
	{ 	
	}

	void sendBroadcastChange(Context context) {
		Log.d(WidgetProvider.MSG_TAG, "SendChange: " + currentState);
		int newState = (currentState == TetherService.STATE_RUNNING ? TetherService.MANAGE_STOP : TetherService.MANAGE_START);
		new IntentAsyncTask(context, newState).execute(new Void[0]);
	
	}
	
}

class IntentAsyncTask extends AsyncTask<Void, Void, Void> {

	Context context;
	int state;
	
	public IntentAsyncTask(Context context, int state) {
		this.context = context;
		this.state = state;
	}
	
	 protected Void doInBackground(Void... arg) {
		Intent intent = new Intent();
		intent.setAction(TetherService.INTENT_MANAGE);
		intent.putExtra("state", state);
		context.sendBroadcast(intent);
		Log.d(WidgetProvider.MSG_TAG, "Async MANAGE SEND: "+state);
		return null;
	}
}

class WidgetAnimator implements Runnable {
	
	int currentFrame = 1;
	RemoteViews views;
	Context context;
	
	public void run() {
		Log.d("!!!WidgetAnimator!!!", "currentFrame: " + currentFrame + " view: " + views);
		views.setImageViewResource(R.id.button, getImageId(currentFrame));
		AppWidgetManager.getInstance(context).updateAppWidget(WidgetProvider.THIS_APPWIDGET, views);
		if(++currentFrame > 4)  currentFrame = 1;
		WidgetProvider.animateHandler.postDelayed(WidgetProvider.widgetAnimator, WidgetProvider.FRAME_DELAY);
	}
	
	int getImageId(int index) {
		switch(index) {
		case 1 : return R.drawable.widget1;
		case 2 : return R.drawable.widget2;
		case 3 : return R.drawable.widget3;
		case 4 : default : return R.drawable.widget4;
		
		}
	}

}

