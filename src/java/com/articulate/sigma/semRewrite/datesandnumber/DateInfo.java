package com.articulate.sigma.semRewrite.datesandnumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
Copyright 2014-2015 IPsoft

Author: Nagaraj Bhat nagaraj.bhat@ipsoft.com
        Rashmi Rao

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

public class DateInfo {
	
	private String year = null;
	private String month = null;
	private String day = null;
	private int startIndex = -1;
	private int endIndex = -1;
	private String weekDay = null;
	private int timeCount = -1;
	private boolean durationFlag = false;
	
	public DateInfo(){
		
	}
	public DateInfo(DateInfo info){
		this.year = info.year;
		this.day = info.day;
		this.month = info.month;
		this.wordIndexes = new ArrayList<Integer>();
		this.wordIndexes = info.wordIndexes;
		processWordIndexes();
	}
	
	public void setDurationFlag(boolean durationFlag) {
		this.durationFlag = durationFlag;
	}
	
	public String getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	public String getMonth() {
		return month;
	}
	public void setMonth(String month) {
		this.month = month;
	}
	public String getDay() {
		return day;
	}
	public void setDay(String day) {
		this.day = day;
	}
	public int getWordIndex() {
		processWordIndexes();
		return startIndex;
	}
	
	public void clear() {
		this.day = null;
		this.month = null;
		this.year = null;
	}
	
	public int getTimeCount() {
		return timeCount;
	}

	public void setTimeCount(int timeCount) {
		this.timeCount = timeCount;
	}

	public String getWeekDay() {
		return weekDay;
	}

	public void setWeekDay(String weekDay) {
		this.weekDay = weekDay;
	}

	private List<Integer> wordIndexes = new ArrayList<Integer>();
	
	public void addWordIndex(int index) {
		this.wordIndexes.add(index);
	}
	
	public void processWordIndexes() {
		this.endIndex = Collections.max(this.wordIndexes);
		this.startIndex = Collections.min(this.wordIndexes);
	}
	
	public List<Integer> getWordIndexes() {
		return wordIndexes;
	}
	
	public int getEndIndex() {
		processWordIndexes();
		return endIndex;
	}
	
	public void print() {
		System.out.println("Day, " + this.day + " Month, " + this.month + " Year, " + this.year + " Start Index, " + this.startIndex + " EndIndex, " + this.endIndex);
	}
	
	public boolean equals(DateInfo dateInfo) {
		boolean dayFlag, monthFlag,yearFlag, weekDayFlag;
		dayFlag = monthFlag = yearFlag = weekDayFlag = true;
		if ((dateInfo.day != null) && (this.day != null) && !(dateInfo.day.equals(day))) {
			dayFlag = false;
		}
		if ((dateInfo.month != null) && (this.month != null) && !(dateInfo.month.equals(month))) {
			monthFlag = false;
		}
		if ((dateInfo.year != null) && (this.year != null) && !(dateInfo.year.equals(year))) {
			yearFlag = false;
		}
		if ((dateInfo.weekDay != null) && (this.weekDay != null) && !(dateInfo.weekDay.equals(weekDay))) {
			weekDayFlag = false;
		}
		return (dayFlag && monthFlag && yearFlag && weekDayFlag);
	}

	 public int hashCode() {
	        int hash = 3;
	        hash = 53 * hash + (this.day != null  ? this.day.hashCode() : 0);
	        hash = 53 * hash + (this.month != null  ? this.month.hashCode() : 0);
	        hash = 53 * hash + (this.year != null  ? this.year.hashCode() : 0);
	        hash = 53 * hash + (this.weekDay != null  ? this.weekDay.hashCode() : 0);
	        return hash;
	    }
	 
	 public boolean isEmpty() {
		 if(this.day == null && this.month == null && this.year == null && this.weekDay == null)
			 return true;
		 return false;
	 }
	 
	 public boolean isDuration() {
		 return durationFlag;
	 }

}



