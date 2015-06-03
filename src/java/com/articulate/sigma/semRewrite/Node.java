package com.articulate.sigma.semRewrite;

import java.util.HashSet;
import java.util.Set;

/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com

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

/*************************************************************
 */
public class Node {
    public String label = "";
    public float value = (float) 0.0;
    public Set<Arc> arcs = new HashSet<Arc>();  // n1 of the arc is always this node

    public String toString() {
        return label + ":" + Float.toString(value) + " [" + arcs + "]";
    }
}
