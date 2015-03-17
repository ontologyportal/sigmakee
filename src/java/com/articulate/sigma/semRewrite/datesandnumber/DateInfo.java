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


}



