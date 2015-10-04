package com.articulate.sigma.utils;

/*
Copyright 2014-2015 IPsoft

Author: Andrei Holub andrei.holub@ipsoft.com

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

public class ProgressPrinter {

    int progress = 0;
    final int ticksToSkip;

    public ProgressPrinter(int ticksToSkip) {

        this.ticksToSkip = ticksToSkip;
    }

    public void tick() {
        progress = ++progress % ticksToSkip;
        if (progress == 0) {
            System.out.print(".");
        }
    }
}
