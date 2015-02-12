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
