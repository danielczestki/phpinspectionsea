package com.kalessil.phpStorm.phpInspectionsEA.types;

import com.intellij.codeInsight.intention.IntentionAction;
import com.kalessil.phpStorm.phpInspectionsEA.PhpCodeInsightFixtureTestCase;
import com.kalessil.phpStorm.phpInspectionsEA.inspectors.apiUsage.arrays.TypeUnsafeArraySearchInspector;

final public class TypeUnsafeArraySearchInspectorTest extends PhpCodeInsightFixtureTestCase {
    public void testIfFindsAllPatterns() {
        myFixture.enableInspections(new TypeUnsafeArraySearchInspector());

        myFixture.configureByFile("fixtures/api/strict-array-search.php");
        myFixture.testHighlighting(true, false, true);

        for (final IntentionAction fix : myFixture.getAllQuickFixes()) {
            myFixture.launchAction(fix);
        }
        myFixture.setTestDataPath(".");
        myFixture.checkResultByFile("fixtures/api/strict-array-search.fixed.php");
    }
}
