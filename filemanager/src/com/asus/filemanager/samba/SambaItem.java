package com.asus.filemanager.samba;

import android.os.Parcel;
import android.os.Parcelable;

public class SambaItem implements Parcelable{
private String pcName = "";
private String ipAddress = "";
private String account = "";
private String password = "";


public SambaItem(String pcName,String ip){
    this.pcName = pcName;
    this.ipAddress = ip;
}

public SambaItem(String pcName,String ip,String account,String password){
    this.pcName = pcName;
    this.ipAddress = ip;
    this.account = account;
    this.password = password;	
}

public String getIpAddress(){
    return this.ipAddress;
}

public String getPcName(){
    return this.pcName;
}

public String getAccount(){
    return this.account;
}

public String getPassword(){
    return this.password;
}

public void clearAccount(){
	this.account = "";
}

public void clearPassword(){
	this.password = "";
}

@Override
public int describeContents() {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public void writeToParcel(Parcel dest, int flag) {
	// TODO Auto-generated method stub
	dest.writeString(this.pcName);
	dest.writeString(this.ipAddress);
	dest.writeString(this.account);
	dest.writeString(this.password);
}

public static final Parcelable.Creator<SambaItem> CREATOR = new Parcelable.Creator<SambaItem>() {

	@Override
	public SambaItem createFromParcel(Parcel source) {
		// TODO Auto-generated method stub
		return new SambaItem(source.readString(),source.readString(),source.readString(),source.readString());
	}

	@Override
	public SambaItem[] newArray(int size) {
		// TODO Auto-generated method stub
		return new SambaItem[size];
	}
	
};



}
