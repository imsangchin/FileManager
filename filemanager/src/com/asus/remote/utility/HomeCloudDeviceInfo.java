package com.asus.remote.utility;

public class HomeCloudDeviceInfo {
	private String userId;
	private String deviceId;
	private String deviceName;
	private int deviceStatus;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public int getDeviceStatus() {
		return deviceStatus;
	}

	public void setDeviceStatus(int deviceStatus) {
		this.deviceStatus = deviceStatus;
	}

/*	public HomeCloudDeviceInfo(String userId, String deviceId, String deviceName) {
		super();
		this.userId = userId;
		this.deviceId = deviceId;
		this.deviceName = deviceName;
	}*/
	public HomeCloudDeviceInfo(String userId, String deviceId, String deviceName,int deviceStatus ) {
		super();
		this.userId = userId;
		this.deviceId = deviceId;
		this.deviceName = deviceName;
		this.deviceStatus = deviceStatus;
	}

	public HomeCloudDeviceInfo() {
		super();

	}

}
