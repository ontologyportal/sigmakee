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

import edu.stanford.nlp.ling.CoreLabel;

import java.util.Optional;

public interface ClauseSubstitutor {

    /** ***************************************************************
     * Returns true if substitutor contains key with given label
     */
    boolean containsKey(CoreLabel key);

    /** ***************************************************************
     * Returns true if substitutor contains key with given label
     * E.g. "was-3"
     */
    boolean containsKey(String keyLabel);

    /** ***************************************************************
     * Searches for referenced group by only first label in the key
     */
    Optional<CoreLabelSequence> getGroupedByFirstLabel(CoreLabel label);

    /** ***************************************************************
     * Searches for referenced group by any label included into the key
     */
    CoreLabelSequence getGrouped(CoreLabel key);

    /** ***************************************************************
     * Searches for referenced group by any label included into the key
     * E.g. "training-7"
     */
    CoreLabelSequence getGrouped(String keyLabel);
}
