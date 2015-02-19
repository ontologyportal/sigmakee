package com.articulate.sigma.semRewrite.datesandnumber;

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
	private int wordIndex = -1;
	
	public DateInfo(){
		
	}
	public DateInfo(DateInfo info){
		this.year = info.year;
		this.day = info.day;
		this.month = info.month;
		this.wordIndex = info.wordIndex;
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
		return wordIndex;
	}
	public void setWordIndex(int wordIndex) {
		this.wordIndex = wordIndex;
	}
	public void clear() {
		this.day = null;
		this.month = null;
		this.year = null;
	}

}
