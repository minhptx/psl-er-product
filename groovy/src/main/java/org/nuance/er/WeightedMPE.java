package org.nuance.er;

import org.linqs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE;
import org.linqs.psl.database.Database;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.Rule;
import org.linqs.psl.model.rule.WeightedGroundRule;

import java.util.List;

public class WeightedMPE extends MaxLikelihoodMPE {
    public WeightedMPE(List<Rule> rules, Database rvDB, Database observedDB) {
        super(rules, rvDB, observedDB);
    }

    public WeightedMPE(Model model, Database rvDB, Database observedDB){
        super(model.getRules(), rvDB, observedDB);
    }

    @Override
    public void computeObservedIncompatibility(){
        setLabeledRandomVariables();

        // Zero out the observed incompatibility first.
        for (int i = 0; i < observedIncompatibility.length; i++) {
            observedIncompatibility[i] = 0.0;
        }

        // Sums up the incompatibilities.
        for (int i = 0; i < mutableRules.size(); i++) {
            for (GroundRule groundRule : groundRuleStore.getGroundRules(mutableRules.get(i))) {
                observedIncompatibility[i] += ((WeightedGroundRule)groundRule).getIncompatibility();
            }
        }
    }

}
