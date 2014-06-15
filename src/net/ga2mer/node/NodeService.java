package net.ga2mer.node;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import net.ga2mer.node.NodeJsService;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class NodeService extends Service implements NodeContext
{
    public static final String Android_Context = "Android_Context";
    public static final String Files_Dir = "Files_Dir";

    @Override
    public void onStart(Intent intent, int startId)
    {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        android.util.Log.i("NodeService", "onDestroy");
        
        stopForeground(true);
    }
    
    @Override
    public void onCreate()
    {
        super.onCreate();

        mData = new HashMap<String, Object>();
        setData(Android_Context, this);
        setData(Files_Dir, this.getFilesDir().getAbsolutePath());
        
        try
        {
            Class.forName("net.ga2mer.node.NodeBroker");

            Properties props = new Properties();
            String configName = getFilesDir() + "/config.props";
            props.load(new FileInputStream(configName));

            FileOutputStream fout = new FileOutputStream(configName);
            props.store(fout, null);
            fout.close();
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
        catch(ClassNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static final String LAUNCH_NODE = "net.ga2mer.node.launch";

    @Override
    public IBinder onBind(Intent arg0)
    {
        return mBinder;
    }

    private final NodeJsService.Stub mBinder = new NodeJsService.Stub()
    {
        public void launchInstance(String file)
        {
            final String jsfile = file;
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    NodeBroker.runNodeJs(NodeService.this, jsfile);
                    NodeService.this.stopForeground(true);
                    NodeService.this.stopSelf();
                    android.util.Log.i("NodeService", "node.js stopped");
                }

            }).start();
        }

        public void debugInstance(String file)
        {
            final String jsfile = file;
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    // TODO Auto-generated method stub
                    NodeBroker.debugNodeJs(NodeService.this, jsfile);
                    NodeService.this.stopForeground(true);
                    NodeService.this.stopSelf();
                    android.util.Log.i("NodeService", "node.js debug stopped2.");
                }

            }).start();
        }
    };

    @Override
    public Object getData(String name)
    {
        return mData.get(name);
    }

    @Override
    public void setData(String name, Object value)
    {
        mData.put(name, value);
    }

    private HashMap<String, Object> mData;
    
}
