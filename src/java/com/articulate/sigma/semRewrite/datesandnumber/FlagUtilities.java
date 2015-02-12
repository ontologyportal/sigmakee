package com.articulate.sigma.semRewrite.datesandnumber;

public class FlagUtilities {
	boolean monthFlag = false;
	boolean yearFlag = false;
	boolean dayFlag = false;
	
	public boolean isMonthFlag() {
		return monthFlag;
	}
	public void setMonthFlag(boolean monthFlag) {
		this.monthFlag = monthFlag;
	}
	public boolean isYearFlag() {
		return yearFlag;
	}
	public void setYearFlag(boolean yearFlag) {
		this.yearFlag = yearFlag;
	}
	public boolean isDayFlag() {
		return dayFlag;
	}
	public void setDayFlag(boolean dayFlag) {
		this.dayFlag = dayFlag;
	}
	
	public void resetFlags() {
		this.monthFlag = false;
		this.dayFlag = false;
		this.yearFlag = false;
	}

}
