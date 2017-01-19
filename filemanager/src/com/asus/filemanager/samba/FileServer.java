package com.asus.filemanager.samba;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.apache.http.HttpResponse;
import org.apache.http.entity.InputStreamEntity;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import android.text.TextUtils;
import android.util.Log;

import com.asus.filemanager.samba.http.HTTPRequest;
import com.asus.filemanager.samba.http.HTTPResponse;
import com.asus.filemanager.samba.http.HTTPServerList;
import com.asus.filemanager.samba.http.HTTPStatus;
import com.asus.service.cloudstorage.CloudStorageService;
import com.asus.service.cloudstorage.common.MsgObj;
import com.asus.service.cloudstorage.dumgr.HttpHelper;
 

 

public class FileServer extends Thread implements com.asus.filemanager.samba.http.HTTPRequestListener
{
	
	public static final String CONTENT_EXPORT_URI = "/smb";
	public static final String CONTENT_CLOUD_URI = "/cloud";
	public static final String TAG = "FileServer";
	private HTTPServerList httpServerList = new HTTPServerList();

	private int HTTPPort = 2222;

	private String bindIP = null;

	
	public String getBindIP()
	{
		return bindIP;
	}

	public void setBindIP(String bindIP)
	{
		this.bindIP = bindIP;
	}

	public HTTPServerList getHttpServerList()
	{
		return httpServerList;
	}

	public void setHttpServerList(HTTPServerList httpServerList)
	{
		this.httpServerList = httpServerList;
	}

	public int getHTTPPort()
	{
		return HTTPPort;
	}

	public void setHTTPPort(int hTTPPort)
	{
		HTTPPort = hTTPPort;
	}

	@Override
	public void run()
	{
		super.run();

		int retryCnt = 0;

		int bindPort = getHTTPPort();

		HTTPServerList hsl = getHttpServerList();
		while (hsl.open(bindPort) == false)
		{
			retryCnt++;

			if (100 < retryCnt)
			{
				return;
			}
			setHTTPPort(bindPort + 1);
			bindPort = getHTTPPort();
		}
		hsl.addRequestListener(this);

		hsl.start(); 

		if(hsl.size() != 0){
		    SambaFileUtility.HTTP_IP = hsl.getHTTPServer(0).getBindAddress();
		    SambaFileUtility.HTTP_PORT = hsl.getHTTPServer(0).getBindPort();
		}
		 
	}

	@Override
	public void httpRequestRecieved(HTTPRequest httpReq)
	{

        Log.d(TAG,"httpRequestRecieved" + httpReq);
		String uri = httpReq.getURI();
		String contentType = "";
		InputStream contentIn = null;
		long contentLen;
		
        boolean bIsRangeRequest = false;
        long iFrom =0;
        long iTo = 0;

		if(uri.startsWith(CONTENT_EXPORT_URI)){
			String filePaths = SambaFileUtility.SelectFilePath;

			int indexOf = filePaths.indexOf("&");
			
	        if (indexOf != -1)
	        {
	        	filePaths = filePaths.substring(0, indexOf);
	        }
			
			try
			{
				SmbFile file = new SmbFile(filePaths);
				contentLen = file.length();
				contentType = SambaFileUtility.getInstance(null).getFileType(filePaths);
				contentIn = file.getInputStream();
			}
			catch (MalformedURLException e)
			{
				httpReq.returnBadRequest();
				return;
			}
			catch (SmbException e)
			{
				httpReq.returnBadRequest();
				return;
			}
			catch (IOException e)
			{
				httpReq.returnBadRequest();
				return;
			}
			
            String RangeRequest = httpReq.getStringHeaderValue("Range");
            if (!TextUtils.isEmpty(RangeRequest)){

                String []parts=RangeRequest.split("=");
                if (null==parts || parts[0].compareToIgnoreCase("bytes")!=0){
                    Log.d(TAG,"didn't find bytes tag?");
                }else{
                    String []ContentRanges=parts[1].split("-");
                    if (ContentRanges.length == 2){
                        try{
                            iFrom = Long.parseLong(ContentRanges[0]);
                            iTo = Long.parseLong(ContentRanges[1]);
                            bIsRangeRequest = true;
                        }catch(Throwable e){
                            Log.d(TAG,"can't parse content range?");
                            //response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        }
                    }else if (ContentRanges.length == 1){
                        iFrom = Long.parseLong(ContentRanges[0]);
                        iTo = -1;
                        bIsRangeRequest = true;
                    }else{
                        Log.d(TAG,"can't parse content range?");
                    }
                }
            }
		}else if(uri.startsWith(CONTENT_CLOUD_URI)){
			if(SambaFileUtility.mSelectMsgObj == null){
				return;
			}
			
			MsgObj reqMsgObj = SambaFileUtility.mSelectMsgObj;
			String filePath = reqMsgObj.getFileObjPath().getSourceUri();
			contentIn = null;
			contentLen = (long) reqMsgObj.getFileObjPath().getFileSize();
				
			contentType = SambaFileUtility.getInstance(null).getFileType(reqMsgObj.getFileObjPath().getFileName());
			if(reqMsgObj.getStorageObj().getStorageType() == MsgObj.TYPE_GOOGLE_DRIVE_STORAGE){
				contentIn = CloudStorageService.getGoogleDriveInputStream(SambaFileUtility.getInstance(null).getActivity(), reqMsgObj.getStorageObj().getStorageName(), filePath);
			}else {
				HttpHelper httpHelper = new HttpHelper();
				try {
					HttpResponse response = httpHelper.doGet(filePath);
					if(response.getStatusLine().getStatusCode() == HTTPStatus.OK){
					    contentIn = response.getEntity().getContent();//new BufferedInputStream(
					    Log.d(TAG,"----status good ok----");
					}else{
						Log.d(TAG,"---status-no ok----");
						contentIn = null;
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}else{
			httpReq.returnBadRequest();
			return;
		}
		
		if (contentLen <= 0 || contentType.length() <= 0 || contentIn == null) {
			httpReq.returnBadRequest();
			return;
		} 
		
		HTTPResponse httpRes = new HTTPResponse();
		httpRes.setContentType(contentType);
		httpRes.setStatusCode(HTTPStatus.OK);
		httpRes.setContentLength(contentLen);
		httpRes.setContentInputStream(contentIn);
        if (bIsRangeRequest){
            if (iTo == -1){
                iTo = contentLen-1;
                httpRes.setContentRange(iFrom,iTo,iTo-iFrom +1);
            }else{
                httpRes.setContentRange(iFrom,iTo,iTo-iFrom +1);
            }
        }
		httpReq.post(httpRes);

			try {
				contentIn.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}
	

  

}
