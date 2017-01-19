package com.asus.remote.utility;

import com.asus.service.cloudstorage.common.MsgObj;

public class MsgObjBackUpEntry {
   private MsgObj msgObj;
   private int what;

public MsgObjBackUpEntry(MsgObj msgObj, int what) {
	super();
	this.msgObj = msgObj;
	this.what = what;
}
public MsgObj getMsgObj() {
	return msgObj;
}
public void setMsgObj(MsgObj msgObj) {
	this.msgObj = msgObj;
}
public int getWhat() {
	return what;
}
public void setWhat(int what) {
	this.what = what;
}
@Override
public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + what;
	return result;
}
@Override
public boolean equals(Object obj) {
	if (this == obj)
		return true;
	if (obj == null)
		return false;
	if (getClass() != obj.getClass())
		return false;
	MsgObjBackUpEntry other = (MsgObjBackUpEntry) obj;
	if (other.getMsgObj().getMsgId() == null || this.getMsgObj().getMsgId() == null) {
		return false;
	}
	if (other.getMsgObj().getMsgId() != this.getMsgObj().getMsgId()) {
		return false;
	}
	if (what != other.what)
		return false;
	return true;
}

}
