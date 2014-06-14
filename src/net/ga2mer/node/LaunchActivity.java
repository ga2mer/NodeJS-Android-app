package net.ga2mer.node;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import net.ga2mer.chinesechess.R;
import net.ga2mer.node.NodeJsService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

public/* abstract */class LaunchActivity extends Activity implements ServiceConnection
{
    private WebView webView;

    @Override
    protected void onDestroy()
    {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    public static final String LaunchIcon = "LaunchIcon";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        
        Log.i("LaunchActivity", "Intent = " + intent.getAction());
        
        if (intent.getAction().equals(Intent.ACTION_MAIN))
        {
            deploy();

            launchService();
            setContentView(R.layout.web);
            webView = (WebView) findViewById(R.id.webView1);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl("http://127.0.0.1:8888");
        }
        else
        {
            {
                Properties props = new Properties();
                FileInputStream in;
                try
                {
                    in = new FileInputStream(getFilesDir() + "/config.props");
                    props.load(in);
                    in.close();
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }

                String port = props.getProperty("port");
            
                String ipAddress = NetUtil.getLocalIpAddress(this);
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://" + ipAddress + ":" + port + "/exit.html?exit=true"));
                startActivity(intent);
                stopService(new Intent(NodeService.LAUNCH_NODE));
            }
        }
    }

    private void deploy()
    {
        File filedir = getFilesDir();
        File config = new File(filedir, "config.props");
        if (config.exists())
        {
            return;
        }

        int resId = getResources().getIdentifier("web", "raw", getPackageName());
        InputStream webStream = getResources().openRawResource(resId);
        if (webStream != null)
        {
            try
            {
                JarInputStream jin = new JarInputStream(webStream);
                JarEntry entry;
                while ((entry = jin.getNextJarEntry()) != null)
                {
                    String entryName = entry.getName();
                    File file = new File(filedir, entryName);
                    if (entry.isDirectory())
                    {
                        if (!file.exists())
                        {
                            file.mkdirs();
                        }
                    }
                    else
                    {
                        File dir = new File(file.getParent());
                        if (!dir.exists())
                        {
                            dir.mkdirs();
                        }
                        FileOutputStream fout = null;
                        try
                        {
                            fout = new FileOutputStream(file);
                            NetUtil.copyIO(jin, fout);
                        }
                        finally
                        {
                            fout.close();
                        }
                    }
                }
            }
            catch(Exception e)
            {

            }
            
        }
    }

    private void launchService()
    {
        Intent intent = new Intent(NodeService.LAUNCH_NODE);
        startService(intent);
        boolean ret = bindService(intent, this, Context.BIND_AUTO_CREATE);
        if (!ret)
        {
            finish();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service)
    {
        mNodejs = NodeJsService.Stub.asInterface(service);

        Properties props = new Properties();
        try
        {
            String configName = getFilesDir() + "/config.props";
            FileInputStream fin = new FileInputStream(configName);
            props.load(fin);
            fin.close();

            final String index = props.getProperty("index");
            String mainfile = props.getProperty("main");
            final String port = props.getProperty("port");
            final String folderName = props.getProperty("sdfolder");
            boolean debugable = Boolean.parseBoolean(props.getProperty("debug", "false"));
            File externalSD = SdUtil.getSdCard();
            Log.i("node", "externalSd = " + externalSD.getAbsolutePath());
            File folder = new File(externalSD, folderName);
            if (!folder.exists() && !folder.mkdirs() && !folder.isDirectory())
            {
                Toast.makeText(this, "Folder " + folder.getAbsolutePath() + " can't be created", Toast.LENGTH_LONG).show();
                Log.i("node", "folder can't be created");
                finish();
                return;
            }
            
            props.setProperty("apk", SdUtil.getPackageFile(this));
            props.setProperty("sdcard", folder.getAbsolutePath());
            FileOutputStream fout = new FileOutputStream(configName);
            props.store(fout, null);
            fout.close();
            runNodeJs(mainfile, debugable);
            
           /* mTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    Intent notiIntent = new Intent(Intent.ACTION_VIEW);
                    String ipAddress = NetUtil.getLocalIpAddress(LaunchActivity.this);
                    notiIntent.setData(Uri.parse("http://yandex.ru"));
                    startActivity(notiIntent);
                }
            };

            mTimer.schedule(mTask, 3000);*/
        }
        catch(FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            props = null;
        }
        unbindService(this);
        //finish();
    }

    @Override
    public void onServiceDisconnected(ComponentName name)
    {

    }

    protected void runNodeJs(String mainfile, boolean debug)
    {
        File js = new File(getFilesDir(), mainfile);
        if (js.exists() && mNodejs != null)
        {
            try
            {
                if (!debug)
                {
                    mNodejs.launchInstance(js.getAbsolutePath());
                }
                else
                {
                    mNodejs.debugInstance(js.getAbsolutePath());
                }
            }
            catch(RemoteException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
        }
    }

    // protected abstract int getResourceId(String resourceName);

    private static NodeJsService mNodejs = null;
    //private final Timer mTimer = new Timer();
    //private TimerTask mTask;
    private Handler mHandler;
}
