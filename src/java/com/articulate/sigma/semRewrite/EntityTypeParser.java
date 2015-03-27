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
package com.articulate.sigma.semRewrite;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

import java.util.Collection;
import java.util.List;

public class EntityTypeParser {
    public static final EntityTypeParser NULL_PARSER = new EntityTypeParser();
    Multimap<String, EntityType> parsedEntities = HashMultimap.create();

    private EntityTypeParser(){

    }

    public EntityTypeParser(Annotation document) {

        List<CoreMap> coreMaps = document.get(CoreAnnotations.MentionsAnnotation.class);
        if (coreMaps == null) {
            System.out.println("ERROR: MentionsAnnotation map not found");
        } else {
            for (CoreMap coreMap : coreMaps) {
                Optional<EntityType> type = Enums.getIfPresent(EntityType.class, coreMap.get(CoreAnnotations.EntityTypeAnnotation.class));
                if (type.isPresent()) {
                    String entity = coreMap.get(CoreAnnotations.TextAnnotation.class).replace(" ", "");
                    int idx = 1 + coreMap.get(CoreAnnotations.TokenBeginAnnotation.class).intValue();
                    parsedEntities.put(entity + "-" + idx, type.get());
                }
            }
        }
    }

    public Collection<EntityType> getEntityTypes(String entity) {

        return parsedEntities.get(entity);
    }

    public boolean equalsToEntityType(String entity, EntityType type) {

        return parsedEntities.get(entity).contains(type);
    }
}
