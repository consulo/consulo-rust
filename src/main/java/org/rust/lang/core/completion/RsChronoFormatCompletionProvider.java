/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.RsCallable;
import org.rust.lang.core.types.ty.TyFunctionDef;
import org.rust.lang.core.types.ty.Ty;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;
import static org.rust.lang.core.PsiElementPatternExtUtil.withElementType;
import org.rust.lang.core.psi.ext.RsValueParameterUtil;

public class RsChronoFormatCompletionProvider extends RsCompletionProvider {
    public static final RsChronoFormatCompletionProvider INSTANCE = new RsChronoFormatCompletionProvider();

    private RsChronoFormatCompletionProvider() {
    }

    @NotNull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return withElementType(psiElement(PsiElement.class), RsElementTypes.STRING_LITERAL)
            .with(new PatternCondition<PsiElement>("isChronoFormatArgument") {
                @Override
                public boolean accepts(@NotNull PsiElement psi, @Nullable ProcessingContext ctx) {
                    PsiElement parent = psi.getParent();
                    if (!(parent instanceof RsLitExpr)) return false;
                    RsLitExpr argument = (RsLitExpr) parent;
                    PsiElement argumentListParent = argument.getParent();
                    if (!(argumentListParent instanceof RsValueArgumentList)) return false;
                    RsValueArgumentList argumentList = (RsValueArgumentList) argumentListParent;
                    RsFunction resolvedFunction = resolveFunction(argumentList.getParent());
                    if (resolvedFunction == null) return false;
                    if (RsElementUtil.getContainingCargoPackage(resolvedFunction) == null) return false;
                    if (!"chrono".equals(RsElementUtil.getContainingCargoPackage(resolvedFunction).getName())) return false;
                    int i = argumentList.getExprList().indexOf(argument);
                    java.util.List<RsValueParameter> params = RsFunctionUtil.getValueParameters(resolvedFunction);
                    RsValueParameter param = i >= 0 && i < params.size() ? params.get(i) : null;
                    if (param == null) return false;
                    String patText = RsValueParameterUtil.getPatText(param);
                    return "fmt".equals(patText);
                }
            });
    }

    private static RsFunction resolveFunction(PsiElement o) {
        if (o instanceof RsCallExpr) {
            Ty type = RsTypesUtil.getType(((RsCallExpr) o).getExpr());
            if (type instanceof TyFunctionDef) {
                Object def = ((TyFunctionDef) type).getDef();
                if (def instanceof RsCallable.Function) {
                    return ((RsCallable.Function) def).getFn();
                }
            }
        } else if (o instanceof RsMethodCall) {
            PsiElement resolved = ((RsMethodCall) o).getReference().resolve();
            if (resolved instanceof RsFunction) {
                return (RsFunction) resolved;
            }
        }
        return null;
    }

    private static Map<String, String> getDateTimeFormatCharactersWithDescriptions() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("%Y", RsBundle.message("the.full.proleptic.gregorian.year.zero.padded.to.4.digits.2021"));
        map.put("%C", RsBundle.message("the.proleptic.gregorian.year.divided.by.100.zero.padded.to.2.digits.20"));
        map.put("%y", RsBundle.message("the.proleptic.gregorian.year.modulo.100.zero.padded.to.2.digits.01"));
        map.put("%m", RsBundle.message("month.number.01.12.zero.padded.to.2.digits.07"));
        map.put("%b", RsBundle.message("abbreviated.month.name.always.3.letters.jul"));
        map.put("%B", RsBundle.message("full.month.name.july"));
        map.put("%h", RsBundle.message("abbreviated.month.name.always.3.letters.jul"));
        map.put("%d", RsBundle.message("day.number.01.31.zero.padded.to.2.digits"));
        map.put("%e", RsBundle.message("same.as.d.but.space.padded.8"));
        map.put("%a", RsBundle.message("abbreviated.weekday.name.always.3.letters.sun"));
        map.put("%A", RsBundle.message("full.weekday.name.sunday"));
        map.put("%w", RsBundle.message("sunday.0.monday.1.saturday.6"));
        map.put("%u", RsBundle.message("monday.1.tuesday.2.sunday.7.iso.8601"));
        map.put("%U", RsBundle.message("week.number.starting.with.sunday.00.53.zero.padded.to.2.digits"));
        map.put("%W", RsBundle.message("same.as.u.but.week.1.starts.with.the.first.monday.in.that.year.instead"));
        map.put("%G", RsBundle.message("same.as.y.but.uses.the.year.number.in.iso.8601.week.date.2001"));
        map.put("%g", RsBundle.message("same.as.y.but.uses.the.year.number.in.iso.8601.week.date"));
        map.put("%V", RsBundle.message("same.as.u.but.uses.the.week.number.in.iso.8601.week.date.01.53"));
        map.put("%j", RsBundle.message("day.of.the.year.001.366.zero.padded.to.3.digits"));
        map.put("%D", RsBundle.message("month.day.year.format.same.as.m.d.y"));
        map.put("%x", RsBundle.message("locale.s.date.representation.12.31.99"));
        map.put("%F", RsBundle.message("year.month.day.format.iso.8601.same.as.y.m.d"));
        map.put("%v", RsBundle.message("day.month.year.format.same.as.e.b.y"));
        map.put("%H", RsBundle.message("hour.number.00.23.zero.padded.to.2.digits"));
        map.put("%k", RsBundle.message("same.as.h.but.space.padded.same.as.h"));
        map.put("%I", RsBundle.message("hour.number.in.12.hour.clocks.01.12.zero.padded.to.2.digits"));
        map.put("%l", RsBundle.message("same.as.i.but.space.padded.same.as.i"));
        map.put("%P", RsBundle.message("am.or.pm.in.12.hour.clocks2"));
        map.put("%p", RsBundle.message("am.or.pm.in.12.hour.clocks"));
        map.put("%M", RsBundle.message("minute.number.00.59.zero.padded.to.2.digits"));
        map.put("%S", RsBundle.message("second.number.00.60.zero.padded.to.2.digits"));
        map.put("%f", RsBundle.message("the.fractional.seconds.in.nanoseconds.since.last.whole.second.026490000"));
        map.put("%.f", RsBundle.message("similar.to.f.but.left.aligned.these.all.consume.the.leading.dot.026490"));
        map.put("%.3f", RsBundle.message("similar.to.f.but.left.aligned.but.fixed.to.a.length.of.3.026"));
        map.put("%.6f", RsBundle.message("similar.to.f.but.left.aligned.but.fixed.to.a.length.of.6.026490"));
        map.put("%.9f", RsBundle.message("similar.to.f.but.left.aligned.but.fixed.to.a.length.of.9.026490000"));
        map.put("%3f", RsBundle.message("similar.to.3f.but.without.the.leading.dot.026"));
        map.put("%6f", RsBundle.message("similar.to.6f.but.without.the.leading.dot.026490"));
        map.put("%9f", RsBundle.message("similar.to.9f.but.without.the.leading.dot.026490000"));
        map.put("%R", RsBundle.message("hour.minute.format.same.as.h.m"));
        map.put("%T", RsBundle.message("hour.minute.second.format.same.as.h.m.s"));
        map.put("%X", RsBundle.message("locale.s.time.representation.23.13.48"));
        map.put("%r", RsBundle.message("hour.minute.second.format.in.12.hour.clocks.same.as.i.m.s.p"));
        map.put("%Z", RsBundle.message("local.time.zone.name.skips.all.non.whitespace.characters.during.parsing.acst"));
        map.put("%z", RsBundle.message("offset.from.the.local.time.to.utc.with.utc.being.0000"));
        map.put("%:z", RsBundle.message("same.as.z.but.with.a.colon"));
        map.put("%::z", RsBundle.message("offset.from.the.local.time.to.utc.with.seconds.09.30.00"));
        map.put("%:::z", RsBundle.message("offset.from.the.local.time.to.utc.without.minutes.09"));
        map.put("%#z", RsBundle.message("parsing.only.same.as.z.but.allows.minutes.to.be.missing.or.present"));
        map.put("%c", RsBundle.message("locale.s.date.and.time.thu.mar.3.23.05.25.2005"));
        map.put("%+", RsBundle.message("iso.8601.rfc.3339.date.time.format.2001.07.08t00.34.60.026490.09.30"));
        map.put("%s", RsBundle.message("unix.timestamp.the.number.of.seconds.since.1970.01.01.00.00.utc.994518299"));
        map.put("%t", RsBundle.message("literal.tab.t"));
        map.put("%n", RsBundle.message("literal.newline.n"));
        map.put("%%", RsBundle.message("literal.percent.sign"));
        return map;
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        PsiElement element = parameters.getPosition();
        int offsetInElement = element.getTextRange().getStartOffset() - parameters.getOffset();
        CompletionResultSet r;
        String text = element.getText();
        if (offsetInElement > 0 && offsetInElement <= text.length() && text.charAt(offsetInElement - 1) == '%') {
            r = result.withPrefixMatcher("%");
        } else {
            r = result;
        }
        for (Map.Entry<String, String> entry : getDateTimeFormatCharactersWithDescriptions().entrySet()) {
            r.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(entry.getValue(), true));
        }
    }
}
