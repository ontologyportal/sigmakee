/*
 * Copyright 2014-2015 IPsoft
 *
 * Author: Andrei Holub andrei.holub@ipsoft.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program ; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA  02111-1307 USA
 */

package com.articulate.sigma.semRewrite.substitutor;

import com.google.common.base.Preconditions;

import java.util.Map;

/**
 * Created by aholub on 4/2/15.
 */
public class SimpleSubstitutorStorage implements ClauseSubstitutor {

    Map<String, String> groups;

    public void setGroups(Map<String, String> groups) {

        this.groups = groups;
    }

    /** ***************************************************************
     */
    @Override
    public boolean containsGroup(String key) {

        Preconditions.checkNotNull(groups);
        return groups.containsKey(key);
    }

    /** ***************************************************************
     */
    @Override
    public String getGrouped(String key) {

        Preconditions.checkNotNull(groups);
        String value = groups.get(key);
        return value == null ? key : value;
    }

}
