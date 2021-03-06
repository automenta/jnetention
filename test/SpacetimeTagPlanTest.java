
import java.util.List;
import jnetention.gui.demo.SpacetimeTagPlanDemo;
import jnetention.possibility.SpacetimeTagPlan;
import jnetention.possibility.SpacetimeTagPlan.PlanResult;
import jnetention.possibility.SpacetimeTagPlan.Possibility;
import org.junit.Test;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author me
 */
public class SpacetimeTagPlanTest {
 
    @Test 
    public void testPlanSpaceTime() {
        for (int i = 0; i < 2; i++) {
            boolean space = i==1 ? true : false;
            testPlanSpaceTime(5, space);
            testPlanSpaceTime(10, space);
            testPlanSpaceTime(20, space);
        }
    }
    
    
    public void testPlanSpaceTime(int numObjects, boolean space) {
        int numTags = numObjects/2;
        int numCentroids = numObjects/3;
        double fuzziness = 1.5;
        int iterations = 1024;
        
        SpacetimeTagPlan s = SpacetimeTagPlanDemo.newExampleSpacetimeTagPlan(numObjects, numTags, space);
        
        int baseTags = space ? 3 : 1;
        
        assert(s.mapping.size() > baseTags);
        assert(s.mapping.size() <= (baseTags+numTags));

        s.update(numCentroids, iterations, fuzziness, new PlanResult() {

            @Override
            public void onFinished(SpacetimeTagPlan s, List<Possibility> possibilities) {
                assert(possibilities.size() == numCentroids);
                
                for (Possibility p : possibilities) {
                    //System.out.println(p);

                    assert(p.value.size() >= 2);

                    //System.out.println("Center: " + StringUtil.arrayToString(p.getCenter()));
                    //System.out.println("  Points (" + c.getPoints().size() + "): " + c.getPoints());
                }
            }

            @Override
            public void onError(SpacetimeTagPlan plan, Exception e) {
                assert(false);
            }
            
        });
        
        
        
        
         
    }
}
