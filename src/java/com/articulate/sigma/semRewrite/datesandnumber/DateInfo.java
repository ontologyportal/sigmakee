package com.articulate.sigma.semRewrite.datesandnumber;

public class DateInfo {
	
	private String year = null;
	private String month = null;
	private String day = null;
	
	public DateInfo(){
		
	}
	public DateInfo(DateInfo info){
		this.year = info.year;
		this.day = info.day;
		this.month = info.month;
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
	
	public void clear() {
		this.day = null;
		this.month = null;
		this.year = null;
	}
}
