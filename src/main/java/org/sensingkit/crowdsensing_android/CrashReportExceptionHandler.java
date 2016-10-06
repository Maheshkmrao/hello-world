package org.sensingkit.crowdsensing_android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/*import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;*/

public class CrashReportExceptionHandler implements UncaughtExceptionHandler, Runnable {

    private final String TAG = CrashReportExceptionHandler.class.getCanonicalName();

    public static final String ExceptionReportFilename = "ChickenBuzz.trace";
    private static final String MSG_SUBJECT_TAG = "Crash Exception Report"; //email subject
    private static final String MSG_SENDTO = "mahesh.kmrao@gmail.com";    //email will be sent to this account
    //the following may be something you wish to consider localizing
    private static final String MSG_BODY = "Please help by sending this email. " +
            "No personal information is being sent (you can check by reading the rest of the email).";
    private int SENDING_OPTION = 0; //Initially this should be configured before released the build.
    // 0 --> Send Through Mail (Need user intervention)
    // 1 --> Sending through Web Service without user intervention or confirmation
    // 2 --> Sending through SMS (Need User Intervention)
    private static final String BUG_TRACKER_URL = "http://mobiletrack.stg.valuelabs.net/wsdl.php";
    private static final String PROJECT_NAME = "CrowdSense";
    private static final String PROJECT_TITLE = "CrowdSense";
    private static final String PLATFORM_NAME = "Android";

    private UncaughtExceptionHandler mDefaultUEH;
    private Activity mActivity = null;

    public CrashReportExceptionHandler(Activity aAct) {
        mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        mActivity = aAct;
    }

    /**
     * Call this method after creation to start protecting all code thereafter.
     */
    public void initialize() {
        if (mActivity == null)
            throw new NullPointerException();
        sendDebugReportToAuthor(); //in case a previous error did not get sent to the email app
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * Call this method at the end of the protected code, usually in {@link ()}.
     */
    public void restoreOriginalHandler() {
        if (Thread.getDefaultUncaughtExceptionHandler().equals(this))
            Thread.setDefaultUncaughtExceptionHandler(mDefaultUEH);
    }

    @Override
    protected void finalize() throws Throwable {
        restoreOriginalHandler();
        super.finalize();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        submit(e);
        //do not forget to pass this exception through up the chain
        bubbleUncaughtException(t, e);
    }

    /**
     * Send the Exception up the chain, skipping other handlers of this type so only 1 report is sent.
     *
     * @param t - thread object
     * @param e - exception being handled
     */
    protected void bubbleUncaughtException(Thread t, Throwable e) {
        if (mDefaultUEH != null) {
            if (mDefaultUEH instanceof CrashReportExceptionHandler)
                ((CrashReportExceptionHandler) mDefaultUEH).bubbleUncaughtException(t, e);
            else
                mDefaultUEH.uncaughtException(t, e);
        }
    }

    /**
     * Return a string containing the device environment.
     *
     * @return Returns a string with the device info used for debugging.
     */
    public String getDeviceEnvironment() {
        //app environment
        PackageManager pm = mActivity.getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(mActivity.getPackageName(), 0);
        } catch (NameNotFoundException nnfe) {
            //doubt this will ever run since we want info about our own package
            pi = new PackageInfo();
            pi.versionName = "unknown";
            pi.versionCode = 69;
        }
        Date theDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss_zzz");
        String s = "-------- Environment --------\n";
        s += "Time\t= " + sdf.format(theDate) + "\n";
        s += "Device\t= " + Build.FINGERPRINT + "\n";
        try {
            Field theMfrField = Build.class.getField("MANUFACTURER");
            s += "Make\t=" + theMfrField.get(null) + "\n";
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }
        s += "Model\t= " + Build.MODEL + "\n";
        s += "Product\t= " + Build.PRODUCT + "\n";
        s += "App\t\t= " + mActivity.getPackageName() + ", version " + pi.versionName + " (build " + pi.versionCode + ")\n";
        s += "Locale\t= " + mActivity.getResources().getConfiguration().locale.getDisplayName() + "\n";
        s += "Res\t\t= " + mActivity.getResources().getDisplayMetrics().toString() + "\n"; //toString() useful in Android 1.6+
        s += "-----------------------------\n\n";
        return s;
    }

    /**
     * Return the application's friendly name.
     *
     * @return Returns the application name as defined by the android:name attribute.
     */
    public CharSequence getAppName() {
        PackageManager pm = mActivity.getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(mActivity.getPackageName(), 0);
            return pi.applicationInfo.loadLabel(pm);
        } catch (NameNotFoundException nnfe) {
            //doubt this will ever run since we want info about our own package
            return mActivity.getPackageName();
        }
    }

    /**
     * If subactivities create their own report handler, report all Activities as a trace list.
     * A separate line is included if a calling activity/package is detected with the Intent it supplied.
     *
     * @param aTrace - pass in null to force a new list to be created
     * @return Returns the list of Activities in the handler chain.
     */
    public LinkedList<CharSequence> getActivityTrace(LinkedList<CharSequence> aTrace) {
        if (aTrace == null)
            aTrace = new LinkedList<CharSequence>();
        aTrace.add(mActivity.getLocalClassName() + " (" + mActivity.getTitle() + ")");
        if (mActivity.getCallingActivity() != null)
            aTrace.add(mActivity.getCallingActivity().toString() + " (" + mActivity.getIntent().toString() + ")");
        else if (mActivity.getCallingPackage() != null)
            aTrace.add(mActivity.getCallingPackage().toString() + " (" + mActivity.getIntent().toString() + ")");
        if (mDefaultUEH != null && mDefaultUEH instanceof CrashReportExceptionHandler)
            ((CrashReportExceptionHandler) mDefaultUEH).getActivityTrace(aTrace);
        return aTrace;
    }

    /**
     * Create a report based on the given exception.
     *
     * @param aException - exception to report on
     * @return Returns a string with a lot of debug information.
     */
    public String getDebugReport(Throwable aException) {
        NumberFormat theFormatter = new DecimalFormat("#0.");
        String theErrReport = "";

        theErrReport += getAppName() + " generated the following exception:\n";
        theErrReport += aException.toString() + "\n\n";

        //activity stack trace
        List<CharSequence> theActivityTrace = getActivityTrace(null);
        if (theActivityTrace != null && theActivityTrace.size() > 0) {
            theErrReport += "--------- Activity Stack Trace ---------\n";
            for (int i = 0; i < theActivityTrace.size(); i++) {
                theErrReport += theFormatter.format(i + 1) + "\t" + theActivityTrace.get(i) + "\n";
            }//for
            theErrReport += "----------------------------------------\n\n";
        }

        if (aException != null) {
            //instruction stack trace
            StackTraceElement[] theStackTrace = aException.getStackTrace();
            if (theStackTrace.length > 0) {
                theErrReport += "--------- Instruction Stack trace ---------\n";
                for (int i = 0; i < theStackTrace.length; i++) {
                    theErrReport += theFormatter.format(i + 1) + "\t" + theStackTrace[i].toString() + "\n";
                }//for
                theErrReport += "-------------------------------------------\n\n";
            }

            //if the exception was thrown in a background thread inside
            //AsyncTask, then the actual exception can be found with getCause
            Throwable theCause = aException.getCause();
            if (theCause != null) {
                theErrReport += "----------- Cause -----------\n";
                theErrReport += theCause.toString() + "\n\n";
                theStackTrace = theCause.getStackTrace();
                for (int i = 0; i < theStackTrace.length; i++) {
                    theErrReport += theFormatter.format(i + 1) + "\t" + theStackTrace[i].toString() + "\n";
                }//for
                theErrReport += "-----------------------------\n\n";
            }
        }

        theErrReport += getDeviceEnvironment();
        theErrReport += "END REPORT.";
        return theErrReport;
    }

    /**
     * Write the given debug report to the file system.
     *
     * @param aReport - the debug report
     */
    protected void saveDebugReport(String aReport) {
        //save report to file
        try {
            FileOutputStream theFile = mActivity.openFileOutput(ExceptionReportFilename, Context.MODE_PRIVATE);
            theFile.write(aReport.getBytes());
            theFile.close();
        } catch (IOException ioe) {
            //error during error report needs to be ignored, do not wish to start infinite loop
        }
    }

    /**
     * Read in saved debug report and send to email app.
     */
    public void sendDebugReportToAuthor() {
        String theLine = "";
        String theTrace = "";
        try {
            BufferedReader theReader = new BufferedReader(
                    new InputStreamReader(mActivity.openFileInput(ExceptionReportFilename)));
            while ((theLine = theReader.readLine()) != null) {
                theTrace += theLine + "\n";
            }
            /*final String crashReport = theTrace;
            //Crash report will send based on User Selection.
            mAct.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayCrashSendSelectionAlert(mAct, crashReport);
                }
            });*/

            if (sendDebugReportToAuthor(theTrace)) {
                mActivity.deleteFile(ExceptionReportFilename);
            }
        } catch (FileNotFoundException eFnf) {
            // nothing to do
        } catch (IOException eIo) {
            // not going to report
        }
    }

    /**
     * Send the given report to email app.
     *
     * @param aReport - the debug report to send
     * @return Returns true if the email app was launched regardless if the email was sent.
     */
    public Boolean sendDebugReportToAuthor(String aReport) {
        if (aReport != null) {
            String theSubject = getAppName() + " " + MSG_SUBJECT_TAG;
            String theBody = "\n" + MSG_BODY + "\n\n" + aReport + "\n\n";
            switch (SENDING_OPTION) {
                case 0:
                    Intent theIntent = new Intent(Intent.ACTION_SEND);

                    theIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{MSG_SENDTO});
                    theIntent.putExtra(Intent.EXTRA_TEXT, theBody);
                    theIntent.putExtra(Intent.EXTRA_SUBJECT, theSubject);
                    theIntent.setType("message/rfc822");
                    Boolean hasSendRecipients = (mActivity.getPackageManager().queryIntentActivities(theIntent, 0).size() > 0);
                    if (hasSendRecipients) {
                        mActivity.startActivity(theIntent);

                        return true;
                    } else {
//					if(!Constants.IS_APP_IN_DEVICE) Log.d(TAG, "Crash Report String ::" + aReport);
//					new AlertDialog.Builder(mActivity).setTitle("Sorry for the incovenience caused").setPositiveButton("OK", null).create().show();
                        return false;
                    }
                    //break;
                case 1:
                /*if(Utils.checkInternetConnection(mAct)){
				//Sending through webservices
				InputStream is = postStage(BUG_TRACKER_URL, "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:mobiletrack\"><soapenv:Header/><soapenv:Body><urn:createLog soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><Title xsi:type=\"xsd:string\" xs:type=\"type:string\" xmlns:xs=\"http://www.w3.org/2000/XMLSchema-instance\">"+PROJECT_TITLE+"</Title><Description xsi:type=\"xsd:string\" xs:type=\"type:string\" xmlns:xs=\"http://www.w3.org/2000/XMLSchema-instance\">"+aReport+"</Description><Pname xsi:type=\"xsd:string\" xs:type=\"type:string\" xmlns:xs=\"http://www.w3.org/2000/XMLSchema-instance\">"+PROJECT_NAME+"</Pname><Platform xsi:type=\"xsd:string\" xs:type=\"type:string\" xmlns:xs=\"http://www.w3.org/2000/XMLSchema-instance\">"+PLATFORM_NAME+"</Platform></urn:createLog></soapenv:Body></soapenv:Envelope>","urn:mobiletrack#createLog");
				int ch;
				String s = "";
				try {
					while((ch = is.read())!=-1)
					{
						s+=(char)ch;
					}
					Log.d("Crash Report:::","::"+s);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				}*/

//                Utils.displayDialog(mActivity, "Not yet implemented this functionality to send Crash report throigh Web Service. Will be available soon...");
                    return true;
                //break;

                case 2:

//                    Utils.sendSMSMessage(mAct, "919014536390", theBody);
                    Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                    sendIntent.putExtra("address", new String("919014536390"));
                    sendIntent.putExtra("sms_body", theBody);
                    sendIntent.setType("vnd.android-dir/mms-sms");
                    mActivity.startActivity(sendIntent);

//                    SmsManager smsManager = SmsManager.getDefault();
//                    smsManager.sendTextMessage("919014536390", null, theBody, null, null);

                    return true;
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void run() {
//        if(!Constants.IS_APP_IN_DEVICE) Log.d(TAG, "Sending Crash Reporrt as ::>>" + ((SENDING_OPTION == 0) ? "Email" : "Web Service"));
        sendDebugReportToAuthor();
    }

    /**
     * Create an exception report and start an email with the contents of the report.
     *
     * @param e - the exception
     */
    public void submit(Throwable e) {
        String theErrReport = getDebugReport(e);
//        if(!Constants.IS_APP_IN_DEVICE) Log.d(TAG, "Crash Reporrt ::>>" + theErrReport);
        saveDebugReport(theErrReport);
//        if(!Constants.IS_APP_IN_DEVICE) Log.d(TAG, "Saved the Crash Reporrt into File ::>>" + ExceptionReportFilename);
        //try to send file contents via email (need to do so via the UI thread)
        mActivity.runOnUiThread(this);
    }

//	public static InputStream postStage(String url, String reqData,String soapAction) {
//		// TODO Auto-generated method stub
//		InputStream is = null;
//		String ret = null;
////		HttpClient httpClient = new DefaultHttpClient();
////		httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);
//		HttpPost httpPost = new HttpPost(url);
//		HttpResponse response = null;
//		StringEntity tmp = null;
//		if(!Constants.IS_APP_IN_DEVICE)Log.d("Sending Req:::URL>>"+url,":::SoapAction>>"+soapAction+":::Req Data>>"+reqData);
//		httpPost.setHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux " +
//		"i686; en-US; rv:1.8.1.6) Gecko/20061201 Firefox/2.0.0.6 (Ubuntu-feisty)");
//
//		httpPost.setHeader("Accept", "text/html,application/xml," +
//		"application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
//
//		httpPost.setHeader("Content-Type", "text/xml; charset=utf-8");
//		httpPost.setHeader("SOAPAction", soapAction);
//		try {
//			tmp = new StringEntity(reqData,"UTF-8");
//		} catch (UnsupportedEncodingException e) {
//			if(!Constants.IS_APP_IN_DEVICE)Log.d("postStage()-HTTPHelp : UnsupportedEncodingException : ",""+e);
//		}
//		httpPost.setEntity(tmp);
//
//		HttpParams httpParameters = new BasicHttpParams();
//		HttpConnectionParams.setConnectionTimeout(httpParameters, Constants.HTTP_TIMEOUT);
//		HttpConnectionParams.setSoTimeout(httpParameters, Constants.SOCKET_TIMEOUT);
//		try {
//			DefaultHttpClient httpClient = new  DefaultHttpClient(httpParameters);
//			response =(BasicHttpResponse) httpClient.execute(httpPost);
//		}catch(UnknownHostException e){
//			if(!Constants.IS_APP_IN_DEVICE)Log.d("postStage()-HTTPHelp : UnknownHostException : ",""+e);
//			e.printStackTrace();
//		}catch(ConnectTimeoutException e){
//			if(!Constants.IS_APP_IN_DEVICE)Log.d("postStage()-HTTPHelp : ConnectTimeoutException : ",""+e);
//			e.printStackTrace();
//		}catch (ClientProtocolException e) {
//			if(!Constants.IS_APP_IN_DEVICE)Log.d("postStage()-HTTPHelp : ClientProtocolException : ",""+e);
//			e.printStackTrace();
//		}catch (IOException e) {
//			if(!Constants.IS_APP_IN_DEVICE)Log.d("postStage()-HTTPHelp : IOException : ",""+e);
//			e.printStackTrace();
//		}catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
//			return null;
//		}
//		try {
//			ret = response.getStatusLine().toString();
//			HttpEntity he = response.getEntity();
//			InputStream inStream = he.getContent();
//			is = inStream;
//			if(!Constants.IS_APP_IN_DEVICE)Log.d("postStage()-input stream is",""+inStream);
//		} catch (IllegalStateException e) {
//			// TODO Auto-generated catch block
//			if(!Constants.IS_APP_IN_DEVICE)Log.d("postStage()-HTTPHelp : IllegalStateException : ",""+e);
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			if(!Constants.IS_APP_IN_DEVICE)Log.d("postStage()-HTTPHelp : IOException : ",""+e);
//			e.printStackTrace();
//		} catch (Exception e) {
//			// TODO: handle exception
//			if(!Constants.IS_APP_IN_DEVICE)Log.d("postStage()-HTTPHelp : Exception : ",""+e);
//			return null;
//		}
//
//		return is;
//	}//postStage()

//    private void displayCrashSendSelectionAlert(Activity mActivity, final String theTrace){
//        try {
//            String message = mActivity.getResources().getString(R.string.crash_selection_alert_message);
//            // Custom Dialog
//            final Dialog dialog = new Dialog(mActivity, R.style.Theme_Dialog);
//            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//            dialog.setContentView(R.layout.custom_alert_dialog_layout);
//            dialog.setCancelable(true);
//            // dialog.getWindow().setBackgroundDrawable(new
//            // ColorDrawable(Color.TRANSPARENT));
//            // ScrollView dialogLayout = (ScrollView)
//            // dialog.findViewById(R.id.dialogLayout);
//            // dialogLayout.getBackground().setAlpha(15);
//
//            // dialog.setTitle(mActivity.getResources().getString(R.string.result));
//            TextView text = (TextView) dialog.findViewById(R.id.textView1);
//            text.setText(message);
//
//            Button emailSendButton = (Button) dialog.findViewById(R.id.button1);
//            emailSendButton.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View arg0) {
//                    SENDING_OPTION = 0; //Send through Email
//                    dialog.dismiss();
//
//                    if (sendDebugReportToAuthor(theTrace)) {
////                        if(!Constants.IS_APP_IN_DEVICE) Log.d(TAG, "Sent the Crash Reporrt ::>>" );
//                        CrashReportExceptionHandler.this.mActivity.deleteFile(ExceptionReportFilename);
////                        if(!Constants.IS_APP_IN_DEVICE) Log.d(TAG, "Delete Crash Reporrt File::>>" );
//                    }
//                }
//            });
//
//            Button webServiceSendButton = (Button) dialog.findViewById(R.id.button2);
//            webServiceSendButton.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View arg0) {
//                    SENDING_OPTION = 1; //Send through Web Service
//                    dialog.dismiss();
//
//                    if (sendDebugReportToAuthor(theTrace)) {
////                        if(!Constants.IS_APP_IN_DEVICE) Log.d(TAG, "Sent the Crash Reporrt ::>>" );
//                        CrashReportExceptionHandler.this.mActivity.deleteFile(ExceptionReportFilename);
////                        if(!Constants.IS_APP_IN_DEVICE) Log.d(TAG, "Delete Crash Reporrt File::>>" );
//                    }
//                }
//            });
//
//            Button smsSendButton = (Button) dialog.findViewById(R.id.button3);
//            smsSendButton.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View arg0) {
//                    SENDING_OPTION = 2; //Send through SMS
//                    dialog.dismiss();
//
//                    if (sendDebugReportToAuthor(theTrace)) {
////                        if(!Constants.IS_APP_IN_DEVICE) Log.d(TAG, "Sent the Crash Reporrt ::>>" );
//                        CrashReportExceptionHandler.this.mActivity.deleteFile(ExceptionReportFilename);
////                        if(!Constants.IS_APP_IN_DEVICE) Log.d(TAG, "Delete Crash Reporrt File::>>" );
//                    }
//                }
//            });
//
//            dialog.show();
//        } catch (Exception ex) {
////            if (!Constants.IS_APP_IN_DEVICE)
////                Log.d(TAG, "Exception :" + ex.getMessage());
//        }
//
//    }
}
