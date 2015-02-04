package com.articulate.sigma;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 */
public class PredVarInstIntegrationTest extends IntegrationTestBase  {

    @Test
    public void testInstantiatePredVars1()     {
            String stmt1 = "(<=> (instance ?REL TransitiveRelation) " +
                    "(forall (?INST1 ?INST2 ?INST3) " +
                    "(=> (and (?REL ?INST1 ?INST2) " +
                    "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
            Formula f = new Formula();
            f.read(stmt1);

        ArrayList<Formula> actual = PredVarInst.instantiatePredVars(f, SigmaTestBase.kb);

        String expectedStr = "(<=>\n" +
                "  (instance time TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (time ?INST1 ?INST2)\n" +
                "        (time ?INST2 ?INST3))\n" +
                "      (time ?INST1 ?INST3)))), (<=>\n" +
                "  (instance finishes TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (finishes ?INST1 ?INST2)\n" +
                "        (finishes ?INST2 ?INST3))\n" +
                "      (finishes ?INST1 ?INST3)))), (<=>\n" +
                "  (instance telecomCode2 TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (telecomCode2 ?INST1 ?INST2)\n" +
                "        (telecomCode2 ?INST2 ?INST3))\n" +
                "      (telecomCode2 ?INST1 ?INST3)))), (<=>\n" +
                "  (instance nephew TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (nephew ?INST1 ?INST2)\n" +
                "        (nephew ?INST2 ?INST3))\n" +
                "      (nephew ?INST1 ?INST3)))), (<=>\n" +
                "  (instance uncle TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (uncle ?INST1 ?INST2)\n" +
                "        (uncle ?INST2 ?INST3))\n" +
                "      (uncle ?INST1 ?INST3)))), (<=>\n" +
                "  (instance side TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (side ?INST1 ?INST2)\n" +
                "        (side ?INST2 ?INST3))\n" +
                "      (side ?INST1 ?INST3)))), (<=>\n" +
                "  (instance member TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (member ?INST1 ?INST2)\n" +
                "        (member ?INST2 ?INST3))\n" +
                "      (member ?INST1 ?INST3)))), (<=>\n" +
                "  (instance systemPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (systemPart ?INST1 ?INST2)\n" +
                "        (systemPart ?INST2 ?INST3))\n" +
                "      (systemPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance equivalentContentInstance TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (equivalentContentInstance ?INST1 ?INST2)\n" +
                "        (equivalentContentInstance ?INST2 ?INST3))\n" +
                "      (equivalentContentInstance ?INST1 ?INST3)))), (<=>\n" +
                "  (instance aunt TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (aunt ?INST1 ?INST2)\n" +
                "        (aunt ?INST2 ?INST3))\n" +
                "      (aunt ?INST1 ?INST3)))), (<=>\n" +
                "  (instance routeInSystem TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (routeInSystem ?INST1 ?INST2)\n" +
                "        (routeInSystem ?INST2 ?INST3))\n" +
                "      (routeInSystem ?INST1 ?INST3)))), (<=>\n" +
                "  (instance component TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (component ?INST1 ?INST2)\n" +
                "        (component ?INST2 ?INST3))\n" +
                "      (component ?INST1 ?INST3)))), (<=>\n" +
                "  (instance fathersSistersSon TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (fathersSistersSon ?INST1 ?INST2)\n" +
                "        (fathersSistersSon ?INST2 ?INST3))\n" +
                "      (fathersSistersSon ?INST1 ?INST3)))), (<=>\n" +
                "  (instance streamOutfall TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (streamOutfall ?INST1 ?INST2)\n" +
                "        (streamOutfall ?INST2 ?INST3))\n" +
                "      (streamOutfall ?INST1 ?INST3)))), (<=>\n" +
                "  (instance exactlyLocated TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (exactlyLocated ?INST1 ?INST2)\n" +
                "        (exactlyLocated ?INST2 ?INST3))\n" +
                "      (exactlyLocated ?INST1 ?INST3)))), (<=>\n" +
                "  (instance mothersBrothersDaughter TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (mothersBrothersDaughter ?INST1 ?INST2)\n" +
                "        (mothersBrothersDaughter ?INST2 ?INST3))\n" +
                "      (mothersBrothersDaughter ?INST1 ?INST3)))), (<=>\n" +
                "  (instance powerPlant TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (powerPlant ?INST1 ?INST2)\n" +
                "        (powerPlant ?INST2 ?INST3))\n" +
                "      (powerPlant ?INST1 ?INST3)))), (<=>\n" +
                "  (instance fathersBrothersDaughter TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (fathersBrothersDaughter ?INST1 ?INST2)\n" +
                "        (fathersBrothersDaughter ?INST2 ?INST3))\n" +
                "      (fathersBrothersDaughter ?INST1 ?INST3)))), (<=>\n" +
                "  (instance associateInOrganization TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (associateInOrganization ?INST1 ?INST2)\n" +
                "        (associateInOrganization ?INST2 ?INST3))\n" +
                "      (associateInOrganization ?INST1 ?INST3)))), (<=>\n" +
                "  (instance maternalUncle TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (maternalUncle ?INST1 ?INST2)\n" +
                "        (maternalUncle ?INST2 ?INST3))\n" +
                "      (maternalUncle ?INST1 ?INST3)))), (<=>\n" +
                "  (instance primaryGeopoliticalSubdivision TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (primaryGeopoliticalSubdivision ?INST1 ?INST2)\n" +
                "        (primaryGeopoliticalSubdivision ?INST2 ?INST3))\n" +
                "      (primaryGeopoliticalSubdivision ?INST1 ?INST3)))), (<=>\n" +
                "  (instance properlyFills TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (properlyFills ?INST1 ?INST2)\n" +
                "        (properlyFills ?INST2 ?INST3))\n" +
                "      (properlyFills ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subset TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subset ?INST1 ?INST2)\n" +
                "        (subset ?INST2 ?INST3))\n" +
                "      (subset ?INST1 ?INST3)))), (<=>\n" +
                "  (instance keyName TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (keyName ?INST1 ?INST2)\n" +
                "        (keyName ?INST2 ?INST3))\n" +
                "      (keyName ?INST1 ?INST3)))), (<=>\n" +
                "  (instance telecomCoreNumber TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (telecomCoreNumber ?INST1 ?INST2)\n" +
                "        (telecomCoreNumber ?INST2 ?INST3))\n" +
                "      (telecomCoreNumber ?INST1 ?INST3)))), (<=>\n" +
                "  (instance postNeighborhood TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (postNeighborhood ?INST1 ?INST2)\n" +
                "        (postNeighborhood ?INST2 ?INST3))\n" +
                "      (postNeighborhood ?INST1 ?INST3)))), (<=>\n" +
                "  (instance sibling TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (sibling ?INST1 ?INST2)\n" +
                "        (sibling ?INST2 ?INST3))\n" +
                "      (sibling ?INST1 ?INST3)))), (<=>\n" +
                "  (instance grandfather TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (grandfather ?INST1 ?INST2)\n" +
                "        (grandfather ?INST2 ?INST3))\n" +
                "      (grandfather ?INST1 ?INST3)))), (<=>\n" +
                "  (instance partiallyFills TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (partiallyFills ?INST1 ?INST2)\n" +
                "        (partiallyFills ?INST2 ?INST3))\n" +
                "      (partiallyFills ?INST1 ?INST3)))), (<=>\n" +
                "  (instance administrativeCenter TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (administrativeCenter ?INST1 ?INST2)\n" +
                "        (administrativeCenter ?INST2 ?INST3))\n" +
                "      (administrativeCenter ?INST1 ?INST3)))), (<=>\n" +
                "  (instance bottom TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (bottom ?INST1 ?INST2)\n" +
                "        (bottom ?INST2 ?INST3))\n" +
                "      (bottom ?INST1 ?INST3)))), (<=>\n" +
                "  (instance capitalCity TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (capitalCity ?INST1 ?INST2)\n" +
                "        (capitalCity ?INST2 ?INST3))\n" +
                "      (capitalCity ?INST1 ?INST3)))), (<=>\n" +
                "  (instance equivalentContentClass TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (equivalentContentClass ?INST1 ?INST2)\n" +
                "        (equivalentContentClass ?INST2 ?INST3))\n" +
                "      (equivalentContentClass ?INST1 ?INST3)))), (<=>\n" +
                "  (instance typicallyContainsTemporalPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (typicallyContainsTemporalPart ?INST1 ?INST2)\n" +
                "        (typicallyContainsTemporalPart ?INST2 ?INST3))\n" +
                "      (typicallyContainsTemporalPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance successorAttributeClosure TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (successorAttributeClosure ?INST1 ?INST2)\n" +
                "        (successorAttributeClosure ?INST2 ?INST3))\n" +
                "      (successorAttributeClosure ?INST1 ?INST3)))), (<=>\n" +
                "  (instance developmentalForm TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (developmentalForm ?INST1 ?INST2)\n" +
                "        (developmentalForm ?INST2 ?INST3))\n" +
                "      (developmentalForm ?INST1 ?INST3)))), (<=>\n" +
                "  (instance angleOfFigure TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (angleOfFigure ?INST1 ?INST2)\n" +
                "        (angleOfFigure ?INST2 ?INST3))\n" +
                "      (angleOfFigure ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subOrganization TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subOrganization ?INST1 ?INST2)\n" +
                "        (subOrganization ?INST2 ?INST3))\n" +
                "      (subOrganization ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subsumesContentInstance TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subsumesContentInstance ?INST1 ?INST2)\n" +
                "        (subsumesContentInstance ?INST2 ?INST3))\n" +
                "      (subsumesContentInstance ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subField TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subField ?INST1 ?INST2)\n" +
                "        (subField ?INST2 ?INST3))\n" +
                "      (subField ?INST1 ?INST3)))), (<=>\n" +
                "  (instance teacher TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (teacher ?INST1 ?INST2)\n" +
                "        (teacher ?INST2 ?INST3))\n" +
                "      (teacher ?INST1 ?INST3)))), (<=>\n" +
                "  (instance quarter TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (quarter ?INST1 ?INST2)\n" +
                "        (quarter ?INST2 ?INST3))\n" +
                "      (quarter ?INST1 ?INST3)))), (<=>\n" +
                "  (instance paternalUncle TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (paternalUncle ?INST1 ?INST2)\n" +
                "        (paternalUncle ?INST2 ?INST3))\n" +
                "      (paternalUncle ?INST1 ?INST3)))), (<=>\n" +
                "  (instance physicalEnd TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (physicalEnd ?INST1 ?INST2)\n" +
                "        (physicalEnd ?INST2 ?INST3))\n" +
                "      (physicalEnd ?INST1 ?INST3)))), (<=>\n" +
                "  (instance onboard TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (onboard ?INST1 ?INST2)\n" +
                "        (onboard ?INST2 ?INST3))\n" +
                "      (onboard ?INST1 ?INST3)))), (<=>\n" +
                "  (instance sideOfFigure TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (sideOfFigure ?INST1 ?INST2)\n" +
                "        (sideOfFigure ?INST2 ?INST3))\n" +
                "      (sideOfFigure ?INST1 ?INST3)))), (<=>\n" +
                "  (instance surface TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (surface ?INST1 ?INST2)\n" +
                "        (surface ?INST2 ?INST3))\n" +
                "      (surface ?INST1 ?INST3)))), (<=>\n" +
                "  (instance superficialPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (superficialPart ?INST1 ?INST2)\n" +
                "        (superficialPart ?INST2 ?INST3))\n" +
                "      (superficialPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance maternalAunt TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (maternalAunt ?INST1 ?INST2)\n" +
                "        (maternalAunt ?INST2 ?INST3))\n" +
                "      (maternalAunt ?INST1 ?INST3)))), (<=>\n" +
                "  (instance telecomAreaCode TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (telecomAreaCode ?INST1 ?INST2)\n" +
                "        (telecomAreaCode ?INST2 ?INST3))\n" +
                "      (telecomAreaCode ?INST1 ?INST3)))), (<=>\n" +
                "  (instance postDistrict TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (postDistrict ?INST1 ?INST2)\n" +
                "        (postDistrict ?INST2 ?INST3))\n" +
                "      (postDistrict ?INST1 ?INST3)))), (<=>\n" +
                "  (instance mothersSistersDaughter TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (mothersSistersDaughter ?INST1 ?INST2)\n" +
                "        (mothersSistersDaughter ?INST2 ?INST3))\n" +
                "      (mothersSistersDaughter ?INST1 ?INST3)))), (<=>\n" +
                "  (instance brother TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (brother ?INST1 ?INST2)\n" +
                "        (brother ?INST2 ?INST3))\n" +
                "      (brother ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subPlan TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subPlan ?INST1 ?INST2)\n" +
                "        (subPlan ?INST2 ?INST3))\n" +
                "      (subPlan ?INST1 ?INST3)))), (<=>\n" +
                "  (instance mothersBrothersWife TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (mothersBrothersWife ?INST1 ?INST2)\n" +
                "        (mothersBrothersWife ?INST2 ?INST3))\n" +
                "      (mothersBrothersWife ?INST1 ?INST3)))), (<=>\n" +
                "  (instance chamberOfLegislature TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (chamberOfLegislature ?INST1 ?INST2)\n" +
                "        (chamberOfLegislature ?INST2 ?INST3))\n" +
                "      (chamberOfLegislature ?INST1 ?INST3)))), (<=>\n" +
                "  (instance grandmother TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (grandmother ?INST1 ?INST2)\n" +
                "        (grandmother ?INST2 ?INST3))\n" +
                "      (grandmother ?INST1 ?INST3)))), (<=>\n" +
                "  (instance located TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (located ?INST1 ?INST2)\n" +
                "        (located ?INST2 ?INST3))\n" +
                "      (located ?INST1 ?INST3)))), (<=>\n" +
                "  (instance parent TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (parent ?INST1 ?INST2)\n" +
                "        (parent ?INST2 ?INST3))\n" +
                "      (parent ?INST1 ?INST3)))), (<=>\n" +
                "  (instance initialPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (initialPart ?INST1 ?INST2)\n" +
                "        (initialPart ?INST2 ?INST3))\n" +
                "      (initialPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance geographicSubregion TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (geographicSubregion ?INST1 ?INST2)\n" +
                "        (geographicSubregion ?INST2 ?INST3))\n" +
                "      (geographicSubregion ?INST1 ?INST3)))), (<=>\n" +
                "  (instance postPostcodeArea TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (postPostcodeArea ?INST1 ?INST2)\n" +
                "        (postPostcodeArea ?INST2 ?INST3))\n" +
                "      (postPostcodeArea ?INST1 ?INST3)))), (<=>\n" +
                "  (instance before TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (before ?INST1 ?INST2)\n" +
                "        (before ?INST2 ?INST3))\n" +
                "      (before ?INST1 ?INST3)))), (<=>\n" +
                "  (instance typicalTemporalPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (typicalTemporalPart ?INST1 ?INST2)\n" +
                "        (typicalTemporalPart ?INST2 ?INST3))\n" +
                "      (typicalTemporalPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance typicallyContainsPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (typicallyContainsPart ?INST1 ?INST2)\n" +
                "        (typicallyContainsPart ?INST2 ?INST3))\n" +
                "      (typicallyContainsPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance fills TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (fills ?INST1 ?INST2)\n" +
                "        (fills ?INST2 ?INST3))\n" +
                "      (fills ?INST1 ?INST3)))), (<=>\n" +
                "  (instance starts TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (starts ?INST1 ?INST2)\n" +
                "        (starts ?INST2 ?INST3))\n" +
                "      (starts ?INST1 ?INST3)))), (<=>\n" +
                "  (instance typicalPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (typicalPart ?INST1 ?INST2)\n" +
                "        (typicalPart ?INST2 ?INST3))\n" +
                "      (typicalPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance interiorPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (interiorPart ?INST1 ?INST2)\n" +
                "        (interiorPart ?INST2 ?INST3))\n" +
                "      (interiorPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance sister TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (sister ?INST1 ?INST2)\n" +
                "        (sister ?INST2 ?INST3))\n" +
                "      (sister ?INST1 ?INST3)))), (<=>\n" +
                "  (instance groupMember TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (groupMember ?INST1 ?INST2)\n" +
                "        (groupMember ?INST2 ?INST3))\n" +
                "      (groupMember ?INST1 ?INST3)))), (<=>\n" +
                "  (instance inString TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (inString ?INST1 ?INST2)\n" +
                "        (inString ?INST2 ?INST3))\n" +
                "      (inString ?INST1 ?INST3)))), (<=>\n" +
                "  (instance crosses TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (crosses ?INST1 ?INST2)\n" +
                "        (crosses ?INST2 ?INST3))\n" +
                "      (crosses ?INST1 ?INST3)))), (<=>\n" +
                "  (instance engineeringSubcomponent TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (engineeringSubcomponent ?INST1 ?INST2)\n" +
                "        (engineeringSubcomponent ?INST2 ?INST3))\n" +
                "      (engineeringSubcomponent ?INST1 ?INST3)))), (<=>\n" +
                "  (instance son TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (son ?INST1 ?INST2)\n" +
                "        (son ?INST2 ?INST3))\n" +
                "      (son ?INST1 ?INST3)))), (<=>\n" +
                "  (instance third TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (third ?INST1 ?INST2)\n" +
                "        (third ?INST2 ?INST3))\n" +
                "      (third ?INST1 ?INST3)))), (<=>\n" +
                "  (instance most TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (most ?INST1 ?INST2)\n" +
                "        (most ?INST2 ?INST3))\n" +
                "      (most ?INST1 ?INST3)))), (<=>\n" +
                "  (instance geopoliticalSubdivision TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (geopoliticalSubdivision ?INST1 ?INST2)\n" +
                "        (geopoliticalSubdivision ?INST2 ?INST3))\n" +
                "      (geopoliticalSubdivision ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subString TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subString ?INST1 ?INST2)\n" +
                "        (subString ?INST2 ?INST3))\n" +
                "      (subString ?INST1 ?INST3)))), (<=>\n" +
                "  (instance cousin TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (cousin ?INST1 ?INST2)\n" +
                "        (cousin ?INST2 ?INST3))\n" +
                "      (cousin ?INST1 ?INST3)))), (<=>\n" +
                "  (instance niece TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (niece ?INST1 ?INST2)\n" +
                "        (niece ?INST2 ?INST3))\n" +
                "      (niece ?INST1 ?INST3)))), (<=>\n" +
                "  (instance mother TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (mother ?INST1 ?INST2)\n" +
                "        (mother ?INST2 ?INST3))\n" +
                "      (mother ?INST1 ?INST3)))), (<=>\n" +
                "  (instance initialList TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (initialList ?INST1 ?INST2)\n" +
                "        (initialList ?INST2 ?INST3))\n" +
                "      (initialList ?INST1 ?INST3)))), (<=>\n" +
                "  (instance mothersSistersSon TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (mothersSistersSon ?INST1 ?INST2)\n" +
                "        (mothersSistersSon ?INST2 ?INST3))\n" +
                "      (mothersSistersSon ?INST1 ?INST3)))), (<=>\n" +
                "  (instance postCountry TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (postCountry ?INST1 ?INST2)\n" +
                "        (postCountry ?INST2 ?INST3))\n" +
                "      (postCountry ?INST1 ?INST3)))), (<=>\n" +
                "  (instance father TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (father ?INST1 ?INST2)\n" +
                "        (father ?INST2 ?INST3))\n" +
                "      (father ?INST1 ?INST3)))), (<=>\n" +
                "  (instance telecomCountryCode TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (telecomCountryCode ?INST1 ?INST2)\n" +
                "        (telecomCountryCode ?INST2 ?INST3))\n" +
                "      (telecomCountryCode ?INST1 ?INST3)))), (<=>\n" +
                "  (instance tributary TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (tributary ?INST1 ?INST2)\n" +
                "        (tributary ?INST2 ?INST3))\n" +
                "      (tributary ?INST1 ?INST3)))), (<=>\n" +
                "  (instance piece TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (piece ?INST1 ?INST2)\n" +
                "        (piece ?INST2 ?INST3))\n" +
                "      (piece ?INST1 ?INST3)))), (<=>\n" +
                "  (instance paternalAunt TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (paternalAunt ?INST1 ?INST2)\n" +
                "        (paternalAunt ?INST2 ?INST3))\n" +
                "      (paternalAunt ?INST1 ?INST3)))), (<=>\n" +
                "  (instance daughter TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (daughter ?INST1 ?INST2)\n" +
                "        (daughter ?INST2 ?INST3))\n" +
                "      (daughter ?INST1 ?INST3)))), (<=>\n" +
                "  (instance properPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (properPart ?INST1 ?INST2)\n" +
                "        (properPart ?INST2 ?INST3))\n" +
                "      (properPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance fathersBrothersSon TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (fathersBrothersSon ?INST1 ?INST2)\n" +
                "        (fathersBrothersSon ?INST2 ?INST3))\n" +
                "      (fathersBrothersSon ?INST1 ?INST3)))), (<=>\n" +
                "  (instance geneticSubstrateOfVirus TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (geneticSubstrateOfVirus ?INST1 ?INST2)\n" +
                "        (geneticSubstrateOfVirus ?INST2 ?INST3))\n" +
                "      (geneticSubstrateOfVirus ?INST1 ?INST3)))), (<=>\n" +
                "  (instance fathersSistersDaughter TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (fathersSistersDaughter ?INST1 ?INST2)\n" +
                "        (fathersSistersDaughter ?INST2 ?INST3))\n" +
                "      (fathersSistersDaughter ?INST1 ?INST3)))), (<=>\n" +
                "  (instance headquartersOfOrganization TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (headquartersOfOrganization ?INST1 ?INST2)\n" +
                "        (headquartersOfOrganization ?INST2 ?INST3))\n" +
                "      (headquartersOfOrganization ?INST1 ?INST3)))), (<=>\n" +
                "  (instance half TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (half ?INST1 ?INST2)\n" +
                "        (half ?INST2 ?INST3))\n" +
                "      (half ?INST1 ?INST3)))), (<=>\n" +
                "  (instance student TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (student ?INST1 ?INST2)\n" +
                "        (student ?INST2 ?INST3))\n" +
                "      (student ?INST1 ?INST3)))), (<=>\n" +
                "  (instance postCity TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (postCity ?INST1 ?INST2)\n" +
                "        (postCity ?INST2 ?INST3))\n" +
                "      (postCity ?INST1 ?INST3)))), (<=>\n" +
                "  (instance ancestor TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (ancestor ?INST1 ?INST2)\n" +
                "        (ancestor ?INST2 ?INST3))\n" +
                "      (ancestor ?INST1 ?INST3)))), (<=>\n" +
                "  (instance top TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (top ?INST1 ?INST2)\n" +
                "        (top ?INST2 ?INST3))\n" +
                "      (top ?INST1 ?INST3)))), (<=>\n" +
                "  (instance powerComponent TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (powerComponent ?INST1 ?INST2)\n" +
                "        (powerComponent ?INST2 ?INST3))\n" +
                "      (powerComponent ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subCollection TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subCollection ?INST1 ?INST2)\n" +
                "        (subCollection ?INST2 ?INST3))\n" +
                "      (subCollection ?INST1 ?INST3)))), (<=>\n" +
                "  (instance flows TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (flows ?INST1 ?INST2)\n" +
                "        (flows ?INST2 ?INST3))\n" +
                "      (flows ?INST1 ?INST3)))), (<=>\n" +
                "  (instance completelyFills TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (completelyFills ?INST1 ?INST2)\n" +
                "        (completelyFills ?INST2 ?INST3))\n" +
                "      (completelyFills ?INST1 ?INST3)))), (<=>\n" +
                "  (instance initiallyContainsPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (initiallyContainsPart ?INST1 ?INST2)\n" +
                "        (initiallyContainsPart ?INST2 ?INST3))\n" +
                "      (initiallyContainsPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance mothersSistersHusband TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (mothersSistersHusband ?INST1 ?INST2)\n" +
                "        (mothersSistersHusband ?INST2 ?INST3))\n" +
                "      (mothersSistersHusband ?INST1 ?INST3)))), (<=>\n" +
                "  (instance fathersBrothersWife TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (fathersBrothersWife ?INST1 ?INST2)\n" +
                "        (fathersBrothersWife ?INST2 ?INST3))\n" +
                "      (fathersBrothersWife ?INST1 ?INST3)))), (<=>\n" +
                "  (instance dependentGeopoliticalArea TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (dependentGeopoliticalArea ?INST1 ?INST2)\n" +
                "        (dependentGeopoliticalArea ?INST2 ?INST3))\n" +
                "      (dependentGeopoliticalArea ?INST1 ?INST3)))), (<=>\n" +
                "  (instance mothersBrothersSon TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (mothersBrothersSon ?INST1 ?INST2)\n" +
                "        (mothersBrothersSon ?INST2 ?INST3))\n" +
                "      (mothersBrothersSon ?INST1 ?INST3)))), (<=>\n" +
                "  (instance pointOfFigure TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (pointOfFigure ?INST1 ?INST2)\n" +
                "        (pointOfFigure ?INST2 ?INST3))\n" +
                "      (pointOfFigure ?INST1 ?INST3)))), (<=>\n" +
                "  (instance during TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (during ?INST1 ?INST2)\n" +
                "        (during ?INST2 ?INST3))\n" +
                "      (during ?INST1 ?INST3)))), (<=>\n" +
                "  (instance fathersSistersHusband TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (fathersSistersHusband ?INST1 ?INST2)\n" +
                "        (fathersSistersHusband ?INST2 ?INST3))\n" +
                "      (fathersSistersHusband ?INST1 ?INST3)))), (<=>\n" +
                "  (instance immediateSubclass TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (immediateSubclass ?INST1 ?INST2)\n" +
                "        (immediateSubclass ?INST2 ?INST3))\n" +
                "      (immediateSubclass ?INST1 ?INST3)))), (<=>\n" +
                "  (instance grandparent TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (grandparent ?INST1 ?INST2)\n" +
                "        (grandparent ?INST2 ?INST3))\n" +
                "      (grandparent ?INST1 ?INST3)))), (<=>\n" +
                "  (instance pathInSystem TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (pathInSystem ?INST1 ?INST2)\n" +
                "        (pathInSystem ?INST2 ?INST3))\n" +
                "      (pathInSystem ?INST1 ?INST3)))), (<=>\n" +
                "  (instance realization TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (realization ?INST1 ?INST2)\n" +
                "        (realization ?INST2 ?INST3))\n" +
                "      (realization ?INST1 ?INST3))))";


        List<String> expectedList = Lists.newArrayList(expectedStr);

        assertEquals(117, actual.size());
        assertEquals(expectedList.toString(), actual.toString());
    }

}