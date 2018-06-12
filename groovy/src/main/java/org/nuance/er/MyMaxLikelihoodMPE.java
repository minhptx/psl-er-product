package org.nuance.er;

import org.linqs.psl.database.Database;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.rule.Rule;

import java.util.List;

public class MyMaxLikelihoodMPE extends MyVotedPerceptron {
    public MyMaxLikelihoodMPE(Model model, Database rvDB, Database observedDB) {
        this(model.getRules(), rvDB, observedDB);
    }

    public MyMaxLikelihoodMPE(List<Rule> rules, Database rvDB, Database observedDB) {
        super(rules, rvDB, observedDB, false);
    }
}
