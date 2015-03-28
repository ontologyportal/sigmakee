package com.articulate.sigma.semRewrite.datesandnumber;

/*
Copyright 2014-2015 IPsoft

Author: Rashmi Rao rashmi.rao@ipsoft.com
        Nagaraj Bhat

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA 
 */

public class TimeInfo {

	private String hour = null;
	private String minute = null;
	private String second = null;
	private int wordIndex = -1;

	public String getHour() {
		return hour;
	}
	public void setHour(String hour) {
		this.hour = hour;
	}
	public String getMinute() {
		return minute;
	}
	public void setMinute(String minute) {
		this.minute = minute;
	}
	public String getSecond() {
		return second;
	}
	public void setSecond(String second) {
		this.second = second;
	}
	public int getWordIndex() {
		return wordIndex;
	}
	public void setWordIndex(int wordindex) {
		this.wordIndex = wordindex;
	}

	/** ***************************************************************
     */
	public boolean equals(TimeInfo timeInfo) {
	    
		boolean hourFlag,minuteFlag,secondFlag;
		hourFlag = minuteFlag = secondFlag = true;
		if ((this.hour == null) && (this.minute == null) && (this.second == null)) {
			hourFlag = minuteFlag = secondFlag = false;
		}
		if ((timeInfo.hour != null) && (this.hour != null) && !(timeInfo.hour.equals(hour))) {
			hourFlag = false;
		}
		if ((timeInfo.minute != null) && (this.minute != null) && !(timeInfo.minute.equals(minute))) {
			minuteFlag = false;
		}
		if ((timeInfo.second != null) && (this.second != null) && !(timeInfo.second.equals(second))) {
			secondFlag = false;
		}
		return (hourFlag && minuteFlag && secondFlag);
	}
	
	/** ***************************************************************
     */
	public int hashCode() {
	    
        int hash = 3;
        hash = 53 * hash + (this.hour != null  ? this.hour.hashCode() : 0);
        hash = 53 * hash + (this.minute != null  ? this.minute.hashCode() : 0);
        hash = 53 * hash + (this.second != null  ? this.second.hashCode() : 0);
        return hash;
    }
}
