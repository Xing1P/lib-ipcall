package vn.rta.cpms.services;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.widget.RemoteViews;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import vn.rta.cpms.application.Params;
import vn.rta.cpms.receivers.ConnectionChangeReceiver;
import vn.rta.cpms.receivers.RTASurveyActionReceiver;
import vn.rta.cpms.receivers.RTASurveyWorkingReceiver;
import vn.rta.cpms.receivers.ReloadAppReceiver;
import vn.rta.cpms.services.model.Environment;
import vn.rta.cpms.services.model.Form;
import vn.rta.cpms.services.model.Schedule;
import vn.rta.cpms.services.model.ValidLocation;
import vn.rta.cpms.tasks.ConnectionTask;
import vn.rta.cpms.timers.MessageCheckTimer;
import vn.rta.cpms.timers.TraceDataTimer;
import vn.rta.cpms.timers.TraceLocationTimer;
import vn.rta.cpms.timers.UploadTimer;
import vn.rta.cpms.ui.ntfaction.FloatingNtfActionService;
import vn.rta.cpms.utils.Common;
import vn.rta.cpms.utils.StringUtil;
import vn.rta.survey.android.BuildConfig;
import vn.rta.survey.android.R;
import vn.rta.survey.android.application.RTASurvey;
import vn.rta.survey.android.manager.NotificationCreator;

/**
 * @author VietDung <dungvu@rta.vn>
 * 
 * @modifiedBy VietDung (version 4.9_108, May04 2015): 
 *  register more action in RTA Survey Action receiver.
 * 
 * @modifiedBy VietDung (version 4.9_105, Mar09 2015): more detail logging for SmartSchedule.
 * 
 * @modifiedBy VietDung (version 4.9_103, Feb14 2015): unregisterReceiver() for 
 * surveyWorkingReceiver.
 * 
 * @modifiedBy VietDung (version 4.9_99, Feb06 2015): upgrade SmartSchedule mechanism,
 * compatible with RTASurvey from version 1.6.7(63)
 * 
 * @modifiedBy VietDung (Jan9, 2015): 
 * ignore checking expired date of schedule
 *
 */
public class ManagerService extends Service
		implements LocationListener, RTASurveyWorkingReceiver.RTASurveyWorkingListener {
	public static final int REQUEST_CODE_RELOAD = 2506;
	public static final int REQUEST_CODE_OPENCALC= 1407;

	public static final String ACTION_START_ALLSERVICE = "startservice";
	public static final String ACTION_STOP_RCM = "stoprcm";
	public static final String ACTION_START_RCM = "startrcm";
	public static final String ACTION_UPDATE_SCHEDULE = "updateschedule";

	private static boolean isCollectingInIdleMode = false;
	private static boolean isCollectingInRealTimeMode = false;
	private static boolean isLocationProviderUnavailable = false;

	private static boolean movementLocker = false;
	private static boolean workingLocker = false;

    private final Logger log = Logger.getLogger(ManagerService.class);

	private DBService db;
	private ConnectionService connection;
	private LocationManager locManager;
	private ContentResolver contentResolver;
	private RTASurveyActionReceiver surveyActionReceiver;
	private RTASurveyWorkingReceiver surveyWorkingReceiver;
	private ConnectionChangeReceiver connectionChangeReceiver;
	private ReloadAppReceiver reloadReceiver;

	private TraceDataTimer traceDataTimer;
	private TraceLocationTimer traceLocationTimer;
	private UploadTimer uploadTimer;
	//private SoundCheckTimer soundCheckTimer;
	private MessageCheckTimer rcmTimer;
	private Schedule schedule;
	private Location currentLocation;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		log.info("Start background service");

		// Create service
		connection = ConnectionService.getInstance();
		db = DBService.getInstance();

		// Register receiver for RTASurvey actions
		final IntentFilter filter = new IntentFilter();
		filter.addAction(RTASurveyActionReceiver.ACTION_SUBMIT_INSTANCE);
		filter.addAction(RTASurveyActionReceiver.ACTION_FILL_BLANK_FORM);
		filter.addAction(RTASurveyActionReceiver.ACTION_SEND_INSTANCE_INFO);
		filter.addAction(RTASurveyActionReceiver.ACTION_GET_BLANK_FORM);
		surveyActionReceiver = new RTASurveyActionReceiver();
		registerReceiver(surveyActionReceiver, filter);

		// Register receiver for RTASurvey working track
		final IntentFilter filter2 = new IntentFilter();
		for (String action : RTASurveyWorkingReceiver.ACTIONS) {
			filter2.addAction(action);
		}
		surveyWorkingReceiver = new RTASurveyWorkingReceiver(this);
		registerReceiver(surveyWorkingReceiver, filter2);

		// Register receiver for RTASurvey working track
		final IntentFilter filter3 = new IntentFilter();
		filter3.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		connectionChangeReceiver = new ConnectionChangeReceiver();
		registerReceiver(connectionChangeReceiver, filter3);

		// Register receiver for RTASurvey working track
		final IntentFilter filter4 = new IntentFilter();
		filter4.addAction(ReloadAppReceiver.ACTION_RELOAD);
		reloadReceiver = new ReloadAppReceiver();
		registerReceiver(reloadReceiver, filter4);

		requestLocationUpdates();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if ("rthome".equals(BuildConfig.FLAVOR)) {
			return START_REDELIVER_INTENT;
		}

		String action = intent.getAction();
		if (StringUtil.isEmptyOrNull(action)) {
			return START_REDELIVER_INTENT;
		}

		if (action.equals(ACTION_START_ALLSERVICE)) {
			if (!"rthome".equals(BuildConfig.FLAVOR)) {
				// make this service become foreground
				Intent appIntent = new Intent(ReloadAppReceiver.ACTION_RELOAD);
				PendingIntent reloadAppIntent = PendingIntent.getBroadcast(this,
						REQUEST_CODE_RELOAD, appIntent, 0);

				Intent calcIntent = new Intent(this, OverlayService.class);
				PendingIntent startCalcIntent = PendingIntent.getService(this,
						REQUEST_CODE_OPENCALC, calcIntent, 0);

				RemoteViews ntfContentView = new RemoteViews(getPackageName(),
						R.layout.notification_bar_layout);
				ntfContentView.setOnClickPendingIntent(R.id.app_icon,
						reloadAppIntent);
				ntfContentView.setOnClickPendingIntent(R.id.calc_icon,
						startCalcIntent);

				NotificationCreator.setNotificationTitle(getString(R.string.rta_app_name));
				NotificationCreator.setContentView(ntfContentView);

				startForeground(NotificationCreator.getNotificationId(),  NotificationCreator.getNotification(this));
			}

			if (!RTASurvey.getInstance().isTrackingServiceLocked() &&
					!isCollectingInIdleMode && !isCollectingInRealTimeMode) {
				collectDeviceStatusInIdleMode();
			}

			if (!RTASurvey.getInstance().isGCMRegisteredFlag() &&
					Common.isConnect(RTASurvey.getInstance().getApplicationContext())) {
				if (rcmTimer != null) {
					rcmTimer.cancel();
					rcmTimer.purge();
					log.info("rcmTimer has been stopped");
				}
				rcmTimer = new MessageCheckTimer();
				rcmTimer.start();
			}

			//start floating ntfAction counter
            startService(new Intent(getApplicationContext(), FloatingNtfActionService.class));

		} else if(action.equals(ACTION_STOP_RCM)) {
			if (rcmTimer != null) {
				rcmTimer.cancel();
				rcmTimer.purge();
				rcmTimer = null;
                log.info("rcmTimer has been stopped");
			}
		} else if (action.equals(ACTION_START_RCM)) {
			if (rcmTimer != null) {
				rcmTimer.cancel();
				rcmTimer.purge();
				rcmTimer = null;
			}
			rcmTimer = new MessageCheckTimer();
			rcmTimer.start();

		} else if (action.equals(ACTION_UPDATE_SCHEDULE)) {
            if (!RTASurvey.getInstance().isTrackingServiceLocked()) {
                new ConnectionTask(getApplicationContext()){
                    @Override
                    protected Boolean doInBackground(String... params) {
                        Schedule schedule = updateSchedule();
                        if (!isCollectingInIdleMode && !isCollectingInRealTimeMode) {
                            collectDeviceStatusInIdleMode();
                        } else if (isCollectingInRealTimeMode) {
                            collectDeviceStatus(schedule);
                        }
                        return true;
                    }
                }.execute();
            }
		}

		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        stopService(new Intent(getApplicationContext(), FloatingNtfActionService.class));
        stopService(new Intent(getApplicationContext(), OverlayService.class));

		// Stop collect and upload trace data
		isCollectingInIdleMode = false;
		isCollectingInRealTimeMode = false;
		if (traceDataTimer != null) {
			traceDataTimer.cancel();
		}

		if (traceLocationTimer != null) {
			traceLocationTimer.cancel();
		}

		if (uploadTimer != null) {
			uploadTimer.cancel();
		}

		if (rcmTimer != null) {
			rcmTimer.cancel();
			rcmTimer.purge();
		}

		// unregister RTASurvey actions receiver
		if (surveyActionReceiver != null) {
			unregisterReceiver(surveyActionReceiver);
			surveyActionReceiver = null;
		}
		if (surveyWorkingReceiver != null) {
			unregisterReceiver(surveyWorkingReceiver);
			surveyWorkingReceiver = null;
		}
        if (connectionChangeReceiver != null) {
            unregisterReceiver(connectionChangeReceiver);
            connectionChangeReceiver = null;
        }
        if (reloadReceiver != null) {
            unregisterReceiver(reloadReceiver);
            reloadReceiver = null;
        }

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locManager.removeUpdates(this);
        }

        db.getDbHelper().close();

        log.info("ManagerService is stopped");
	}

	/**
	 * Collect device status with idle period
	 */
	public void collectDeviceStatusInIdleMode() {
		Schedule idle = db.getNewestSchedule();

		if (idle==null) {
			log.error("Has not schedule yet");
			return;
		}
		
		int idleFrequency = RTASurvey.getInstance().getIdleFrequency();
		idle.setPeriodCollect(idleFrequency);
		idle.setPeriodUpload(idleFrequency);
		idle.setPeriodCollect_gps(idleFrequency);
		idle.setStatus(Params.DETECT_ACTIVE);

		isCollectingInIdleMode = true;
		isCollectingInRealTimeMode = false;

		log.info("IDLE MODE");
		collectDeviceStatus(idle);
	}

	/**
	 * Collect device status
	 */
	public void collectDeviceStatus(Schedule sched) {
		// Case get schedule fail, using old schedule
		if (sched == null) {
			// try to get old schedule from database
			schedule = db.getNewestSchedule();

			// Stop processing if schedule is null
			if (schedule == null) {
				log.info("Has not schedule yet");
                return;
			}
		}

		// Case schedule is empty for device - currently, never come here
		else if (Params.DETECT_ACTIVE != sched.getStatus()) {
			log.info("Has not schedule yet");

            // Stop processing and reset data
			schedule = null;

			// Stop collect data timer if exist
			if (traceDataTimer != null) {
				traceDataTimer.cancel();
			}

			// Stop sound checking timer if exist
			/*if (soundCheckTimer != null) {
				soundCheckTimer.cancel();
			}*/

			// Stop detecting sound timer if exist
			/*if (detectTimer != null) {
				detectTimer.cancel();
			}*/

			// Stop upload trace data if exist
			if (uploadTimer != null) {
				uploadTimer.cancel();
			}
			return;
		}


		log.info("Start collecting device status");
        if (sched!=null) {
			this.schedule = sched;
		}

		// Stop collect data timer if exist
		if (traceDataTimer != null) {
			traceDataTimer.cancel();
		}
		// Create new timers running new task
		traceDataTimer = new TraceDataTimer();
		traceDataTimer.schedule(schedule, this, db);

		// Stop collect data timer if exist
		if (traceLocationTimer != null) {
			traceLocationTimer.cancel();
		}
		// Create new timers running new task
		traceLocationTimer = new TraceLocationTimer();
		traceLocationTimer.schedule(schedule, this, db);

		// Stop sound checking timer if exist
		/*if (soundCheckTimer != null) {
			soundCheckTimer.cancel();
		}

		// Check expired time
		if (Common.hasNotExpired(schedule.getEndTime())) {
			soundCheckTimer = new SoundCheckTimer();
			soundCheckTimer.schedule(schedule, this, recorder);
		}*/

		// Stop upload trace data if exist
		if (uploadTimer != null) {
			uploadTimer.cancel();
		}
		uploadTimer = new UploadTimer();
		uploadTimer.schedule(schedule, connection, db);

	}

	private Schedule updateSchedule() {
		// Get synchronize info in server
		String formIdForUpdate = RTASurvey.getInstance().getFormId();
		String res;
		if (formIdForUpdate.equals("")) {
			res = connection.synchronize(
                    this, RTASurvey.getInstance().getServerUrl(),
                    RTASurvey.getInstance().getServerKey(), RTASurvey.getInstance().getDeviceId());
		} else {
			res = connection.updateSchedule(
                    this, RTASurvey.getInstance().getServerUrl(),
                    RTASurvey.getInstance().getServerKey(), 
                    RTASurvey.getInstance().getDeviceId(), formIdForUpdate);
			if (res==null) {
				res = connection.synchronize(
						this, RTASurvey.getInstance().getServerUrl(),
						RTASurvey.getInstance().getServerKey(), RTASurvey.getInstance().getDeviceId());
			}
		}

		// Parse JSON result to Schedule
		Schedule schedule = (Schedule) StringUtil.json2Object(res,
				Schedule.class);

		// Default value for status
		if (schedule!=null) {
			schedule.setStatus(Params.DETECT_ACTIVE);
            log.info("" + schedule);

			// Add sample data for Reliable rate checking (temporary)
			List<Environment> envs = new ArrayList<Environment>();
			List<ValidLocation> fields = new ArrayList<ValidLocation>();
			List<Form> forms = new ArrayList<Form>();

			schedule.setEnvironments(envs);
			schedule.setFields(fields);
			schedule.setForms(forms);

			// save to fadata.db
			db.saveSchedule(schedule);
			this.schedule = schedule;
		}

		return schedule;
	}

	@Override
	public void onLocationChanged(Location location) {
		float activeDistance = RTASurvey.getInstance().getVeclocityLength();
		if (currentLocation == null) {
			currentLocation = location;
		} else if ( currentLocation.distanceTo(location)>=activeDistance ) {
			// collecting in real time activate
			log.debug("try to start real-time mode from movementLocker" +
                    "\nactivaDistance=" + activeDistance + ", current=" +
                    currentLocation.distanceTo(location));
			currentLocation = location;
			startRealTimeMode(null);
		} else {
			if (isCollectingInRealTimeMode) {
				log.debug("movementLocker has been set");
				movementLocker = true;
				stopRealTimeMode();
			}
		}

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// nothing to do here
	}

	@Override
	public void onProviderEnabled(String provider) {
		// nothing to do here
	}

	@Override
	public void onProviderDisabled(String provider) {
		if (locManager.getProviders(true).contains(LocationManager.GPS_PROVIDER) 
				|| locManager.getProviders(true).contains(LocationManager.NETWORK_PROVIDER) ) {
			requestLocationUpdates();
		}
	}

	private boolean requestLocationUpdates() {
		if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            String provider = getActiveLocationProvider();
            if (provider==null) {
                isLocationProviderUnavailable = true;
                return false;
            }

            locManager.requestLocationUpdates(provider,
                    RTASurvey.getInstance().getVeclocityTime(), 0,
                    this, Looper.getMainLooper());
            return true;
        } else {
            log.error("GPS Permission isn't granted.");
            return false;
        }
	}

	private String getActiveLocationProvider() {
		if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) 
			return LocationManager.GPS_PROVIDER;
		if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			return LocationManager.NETWORK_PROVIDER;
		return null;
	}

	private void stopRealTimeMode() {
		if (isLocationProviderUnavailable) {
			if (workingLocker) {
				collectDeviceStatusInIdleMode();
				workingLocker = movementLocker = false;
				log.debug("RealtimeMode stopped, IdleMode started!");
            }
		} else {
			if (movementLocker && workingLocker) {
				collectDeviceStatusInIdleMode();
				workingLocker = movementLocker = false;
				log.debug("RealtimeMode stopped, IdleMode started!");
            }
		}
	}

	private void startRealTimeMode(Schedule schedule) {
		log.debug("start RealtimeMode");
        isCollectingInIdleMode = false;
		isCollectingInRealTimeMode = true;
		movementLocker = workingLocker = false;

        log.info("REAL-TIME MODE");
		collectDeviceStatus(schedule);
	}

	@Override
	public void isWorking() {
		// since version 4.5_68
		if (!isCollectingInRealTimeMode) {
			log.debug("Try to start real-time mode from workingLocker");
			startRealTimeMode(null);
		}
	}

	@Override
	public void stopWorking() {
		log.debug("WorkingLocker has been set");
		workingLocker = true;
		stopRealTimeMode();
		DownloadMediaFilesService.requestService(getApplicationContext(),DownloadMediaFilesService.ACTION_BROADCAST_SERVICE_RECHECK_MEDIA);
	}
}
