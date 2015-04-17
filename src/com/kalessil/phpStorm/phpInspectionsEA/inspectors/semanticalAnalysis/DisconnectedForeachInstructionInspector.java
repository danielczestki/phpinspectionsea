package com.kalessil.phpStorm.phpInspectionsEA.inspectors.semanticalAnalysis;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.StatementImpl;
import com.jetbrains.php.lang.psi.elements.impl.UnaryExpressionImpl;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import com.kalessil.phpStorm.phpInspectionsEA.utils.ExpressionSemanticUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;

public class DisconnectedForeachInstructionInspector extends BasePhpInspection {
    /**
     * TODO: document examples with cloning options
     */
    private static final String strProblemDescription = "This statement seems to be not connected with parent foreach (use clone for objects reset)";

    @NotNull
    public String getShortName() {
        return "DisconnectedForeachInstructionInspection";
    }

    private static enum ExpressionType { IF, INCREMENT, DECREMENT, CLONE, NEW, REASSIGN, OTHER }

    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, final boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            public void visitPhpForeach(ForeachStatement foreach) {
                Variable value             = foreach.getValue();
                GroupStatement foreachBody = ExpressionSemanticUtil.getGroupStatement(foreach);
                /* ensure foreach structure is ready for inspection */
                if (null != foreachBody && null != value && null != value.getName()) {
                    /* pre-collect introduced and internally used variables */
                    HashSet<String> allModifiedVariables = new HashSet<String>();
                    allModifiedVariables.add(value.getName());
                    Variable key = foreach.getKey();
                    if (null != key && null != key.getName()) {
                        allModifiedVariables.add(key.getName());
                    }

                    HashMap<PsiElement, HashSet<String>> instructionDependencies = new HashMap<PsiElement, HashSet<String>>();
                    /* iteration 1 - investigate what are dependencies and influence */
                    for (PsiElement oneInstruction : foreachBody.getStatements()) {
                        if (oneInstruction instanceof PhpPsiElement && !(oneInstruction instanceof PsiComment)) {
                            HashSet<String> individualDependencies = new HashSet<String>();
                            individualDependencies.add("this");

                            instructionDependencies.put(oneInstruction, individualDependencies);
                            investigateInfluence((PhpPsiElement) oneInstruction, individualDependencies, allModifiedVariables);
                        }
                    }

                    /* iteration 2 - analyse dependencies */
                    for (PsiElement oneInstruction : foreachBody.getStatements()) {
                        if (oneInstruction instanceof PhpPsiElement) {
                            boolean isDependOnModifiedVariables = false;
                            boolean hasDependencies             = false;

                            /* check if any dependency is overridden */
                            HashSet<String> individualDependencies = instructionDependencies.get(oneInstruction);
                            if (null != individualDependencies && individualDependencies.size() > 1) {
                                hasDependencies = true;
                                /* contains not only this */
                                for (String dependencyName : individualDependencies) {
                                    if (allModifiedVariables.contains(dependencyName)) {
                                        isDependOnModifiedVariables = true;
                                        break;
                                    }
                                }
                            }

                            /* verify and report if violation detected */
                            if (!isDependOnModifiedVariables && hasDependencies) {
                                ExpressionType target = getExpressionType(oneInstruction);
                                /**
                                 * TODO: hint using clone instead of '$var = \DOMDocument::createElement(...)';
                                 * TODO: hint using clone instead of '$var = new ...';
                                 */
                                if (
                                    ExpressionType.IF        != target &&
                                    ExpressionType.REASSIGN  != target &&
                                    ExpressionType.CLONE     != target &&
                                    ExpressionType.INCREMENT != target &&
                                    ExpressionType.DECREMENT != target
                                ) {
                                    holder.registerProblem(oneInstruction, strProblemDescription, ProblemHighlightType.WEAK_WARNING);
                                }
                            }

                            /* cleanup dependencies details */
                            if (null != individualDependencies) {
                                individualDependencies.clear();
                            }
                        }
                    }
                    /* empty dependencies details container */
                    instructionDependencies.clear();
                }
            }

            private void investigateInfluence(
                PhpPsiElement oneInstruction,
                HashSet<String> individualDependencies,
                HashSet<String> allModifiedVariables
            ) {
                for (PsiElement variable : PsiTreeUtil.findChildrenOfType(oneInstruction, Variable.class)) {
                    Variable castedVariable = (Variable) variable;
                    String variableName = castedVariable.getName();
                    if (null != variableName) {
                        if (variable.getParent() instanceof AssignmentExpression) {
                            AssignmentExpression assignment = (AssignmentExpression) variable.getParent();
                            if (assignment.getVariable() == variable) {
                                allModifiedVariables.add(variableName);
                                continue;
                            }
                        }

                        /* increment/decrement are also write operations */
                        ExpressionType type = getExpressionType(variable.getParent());
                        if (ExpressionType.INCREMENT == type || ExpressionType.DECREMENT == type) {
                            allModifiedVariables.add(variableName);
                            continue;
                        }
                        /* TODO: lookup for array access and property access */

                        individualDependencies.add(variableName);
                    }
                }
            }

            private ExpressionType getExpressionType(PsiElement expression) {
                if (expression instanceof If) {
                    return ExpressionType.IF;
                }

                if (expression instanceof StatementImpl) {
                    return getExpressionType(((StatementImpl) expression).getFirstPsiChild());
                }

                if (expression instanceof UnaryExpressionImpl) {
                    PsiElement operation = ((UnaryExpressionImpl) expression).getOperation();
                    if (null != operation && PhpTokenTypes.opINCREMENT == operation.getNode().getElementType()) {
                        return ExpressionType.INCREMENT;

                    }
                    if (null != operation && PhpTokenTypes.opDECREMENT == operation.getNode().getElementType()) {
                        return ExpressionType.DECREMENT;
                    }
                    if (null != operation && PhpTokenTypes.kwCLONE == operation.getNode().getElementType()) {
                        return ExpressionType.CLONE;
                    }
                }

                if (expression instanceof AssignmentExpression) {
                    AssignmentExpression assignment = (AssignmentExpression) expression;
                    if (assignment.getVariable() instanceof Variable) {
                        PsiElement value = assignment.getValue();
                        if (value instanceof NewExpression) {
                            return ExpressionType.NEW;
                        }
                        if (value instanceof Variable) {
                            return ExpressionType.REASSIGN;
                        }
                    }
                }

                return ExpressionType.OTHER;
            }
        };
    }
}